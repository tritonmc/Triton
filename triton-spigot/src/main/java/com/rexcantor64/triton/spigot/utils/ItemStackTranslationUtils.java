package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ItemStackTranslationUtils {

    private static final MethodAccessor NBT_DESERIALIZER_METHOD;

    static {
        val mojangsonParserClass = MinecraftReflection.getMinecraftClass("nbt.MojangsonParser", "MojangsonParser");
        FuzzyReflection fuzzy = FuzzyReflection.fromClass(mojangsonParserClass);
        val method = fuzzy.getMethodByReturnTypeAndParameters("deserializeNbtCompound", MinecraftReflection.getNBTCompoundClass(), String.class);
        NBT_DESERIALIZER_METHOD = Accessors.getMethodAccessor(method);
    }

    /**
     * Translates an item stack in one of two ways:
     * - if the item has a CraftBukkit handler, the item is translated through its NBT tag;
     * - otherwise, Bukkit's ItemMeta API is used instead.
     * <p>
     * Special attention is given to Shulker Boxes (the names of the items inside them are also translated for preview purposes)
     * and to Written Books (their text is also translated).
     *
     * @param item           The item to translate. Might be mutated
     * @param languagePlayer The language player to translate for
     * @param translateBooks Whether it should translate written books
     * @return The translated item stack, which may or may not be the same as the given parameter
     */
    public static ItemStack translateItemStack(ItemStack item, Localized languagePlayer, boolean translateBooks) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        NbtCompound compound = null;
        try {
            val nbtTagOptional = NbtFactory.fromItemOptional(item);
            if (!nbtTagOptional.isPresent()) return item;
            compound = NbtFactory.asCompound(nbtTagOptional.get());
        } catch (IllegalArgumentException ignore) {
            // This means the item is just an ItemStack and not a CraftItemStack
            // However we can still translate stuff using the Bukkit ItemMeta API instead of NBT tags
        }
        // Translate the contents of shulker boxes
        if (compound != null && compound.containsKey("BlockEntityTag")) {
            NbtCompound blockEntityTag = compound.getCompoundOrDefault("BlockEntityTag");
            if (blockEntityTag.containsKey("Items")) {
                NbtBase<?> itemsBase = blockEntityTag.getValue("Items");
                if (itemsBase instanceof NbtList<?>) {
                    NbtList<?> items = (NbtList<?>) itemsBase;
                    Collection<? extends NbtBase<?>> itemsCollection = items.asCollection();
                    for (NbtBase<?> base : itemsCollection) {
                        NbtCompound invItem = NbtFactory.asCompound(base);
                        if (!invItem.containsKey("tag")) continue;
                        NbtCompound tag = invItem.getCompoundOrDefault("tag");
                        translateNbtItem(tag, languagePlayer, false);
                    }
                }
            }
        }
        // If the compound is null, the item is not a CraftItemStack, therefore it doesn't have NBT data
        if (compound != null) {
            // try to translate name and lore
            translateNbtItem(compound, languagePlayer, true);
            // translate the content of written books
            if (translateBooks && item.getType() == Material.WRITTEN_BOOK && main().getConfig().isBooks()) {
                if (compound.containsKey("pages")) {
                    NbtList<String> pages = compound.getList("pages");
                    Collection<NbtBase<String>> pagesCollection = pages.asCollection();
                    List<String> newPagesCollection = new ArrayList<>();
                    for (NbtBase<String> page : pagesCollection) {
                        if (page.getValue().startsWith("\"")) {
                            String result = translate(
                                    page.getValue().substring(1, page.getValue().length() - 1),
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax()
                            );
                            if (result != null) {
                                newPagesCollection.add(
                                        ComponentSerializer.toString(
                                                TextComponent.fromLegacyText(result)));
                            }
                        } else {
                            BaseComponent[] result = main().getLanguageParser()
                                    .parseComponent(
                                            languagePlayer,
                                            main().getConfig().getItemsSyntax(),
                                            ComponentSerializer.parse(page.getValue())
                                    );
                            if (result != null) {
                                newPagesCollection.add(ComponentSerializer.toString(result));
                            }
                        }
                    }
                    compound.put("pages", NbtFactory.ofList("pages", newPagesCollection));
                }
            }
        }
        // If the item is not a craft item, use the Bukkit API
        if (compound == null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                meta.setDisplayName(translate(meta.getDisplayName(),
                        languagePlayer, main().getConfig().getItemsSyntax()));
            }
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore()) {
                    String result = translate(lore, languagePlayer,
                            main().getConfig().getItemsSyntax());
                    if (result != null)
                        newLore.addAll(Arrays.asList(result.split("\n")));
                }
                meta.setLore(newLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Translates an item's name (and optionally lore) by their NBT tag, mutating the given compound
     *
     * @param compound       The NBT tag of the item
     * @param languagePlayer The language player to translate for
     * @param translateLore  Whether to attempt to translate the lore of the item
     */
    public static void translateNbtItem(NbtCompound compound, Localized languagePlayer, boolean translateLore) {
        if (!compound.containsKey("display")) {
            return;
        }

        NbtCompound display = compound.getCompoundOrDefault("display");
        if (display.containsKey("Name")) {
            String name = display.getStringOrDefault("Name");
            if (main().getMcVersion() >= 13) {
                BaseComponent[] result = main().getLanguageParser()
                        .parseComponent(
                                languagePlayer,
                                main().getConfig().getItemsSyntax(),
                                ComponentSerializer.parse(name)
                        );
                if (result == null) {
                    display.remove("Name");
                } else {
                    display.put("Name", ComponentSerializer.toString(ComponentUtils.ensureNotItalic(Arrays.stream(result))));
                }
            } else {
                String result = translate(name, languagePlayer, main().getConfig().getItemsSyntax());
                if (result == null) {
                    display.remove("Name");
                } else {
                    display.put("Name", result);
                }
            }
        }

        if (translateLore && display.containsKey("Lore")) {
            NbtList<String> loreNbt = display.getListOrDefault("Lore");

            List<String> newLore = new ArrayList<>();
            for (String lore : loreNbt) {
                if (main().getMcVersion() >= 13) {
                    BaseComponent[] result = main().getLanguageParser()
                            .parseComponent(
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax(),
                                    ComponentSerializer.parse(lore)
                            );
                    if (result != null) {
                        List<List<BaseComponent>> splitLoreLines = ComponentUtils.splitByNewLine(Arrays.asList(result));
                        newLore.addAll(splitLoreLines.stream()
                                .map(comps -> ComponentUtils.ensureNotItalic(comps.stream()))
                                .map(ComponentSerializer::toString)
                                .collect(Collectors.toList()));
                    }
                } else {
                    String result = translate(
                            lore,
                            languagePlayer,
                            main().getConfig().getItemsSyntax()
                    );
                    if (result != null) {
                        newLore.addAll(Arrays.asList(result.split("\n")));
                    }
                }
            }
            display.put(NbtFactory.ofList("Lore", newLore));
        }
    }

    public static String translateNbtString(String nbt, Localized localized) {
        val compound = deserializeItemTagNbt(nbt);
        translateNbtItem(compound, localized, true);
        return serializeItemTagNbt(compound);
    }

    private static NbtCompound deserializeItemTagNbt(String nbt) {
        val nmsCompound = NBT_DESERIALIZER_METHOD.invoke(null, nbt);
        return NbtFactory.fromNMSCompound(nmsCompound);
    }

    private static String serializeItemTagNbt(NbtCompound nbt) {
        return nbt.getHandle().toString();
    }

    private static String translate(String string, Localized localized, MainConfig.FeatureSyntax featureSyntax) {
        if (string == null) {
            return null;
        }
        return main().getLanguageParser().replaceLanguages(
                main().getLanguageManager().matchPattern(string, localized),
                localized,
                featureSyntax
        );
    }

    private static SpigotMLP main() {
        return Triton.asSpigot();
    }

}
