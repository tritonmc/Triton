package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
                            val pageString = page.getValue().substring(1, page.getValue().length() - 1);
                            val pageComponent = LegacyComponentSerializer.legacySection().deserialize(pageString);
                            main().getMessageParser()
                                    .translateComponent(
                                            pageComponent,
                                            languagePlayer,
                                            main().getConfig().getItemsSyntax()
                                    )
                                    .map(ComponentUtils::serializeToJson)
                                    .ifChanged(newPagesCollection::add)
                                    .ifUnchanged(() -> newPagesCollection.add(ComponentUtils.serializeToJson(pageComponent)));
                        } else {
                            main().getMessageParser()
                                    .translateComponent(
                                            ComponentUtils.deserializeFromJson(page.getValue()),
                                            languagePlayer,
                                            main().getConfig().getItemsSyntax()
                                    )
                                    .map(ComponentUtils::serializeToJson)
                                    .ifChanged(newPagesCollection::add)
                                    .ifUnchanged(() -> newPagesCollection.add(page.getValue()));
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
                main().getMessageParser()
                        .translateString(
                                meta.getDisplayName(),
                                languagePlayer,
                                main().getConfig().getItemsSyntax()
                        )
                        .ifChanged(meta::setDisplayName)
                        .ifToRemove(() -> meta.setDisplayName(null));
            }
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore()) {
                    main().getMessageParser()
                            .translateString(lore, languagePlayer, main().getConfig().getItemsSyntax())
                            .ifChanged(result -> newLore.addAll(Arrays.asList(result.split("\n"))))
                            .ifUnchanged(() -> newLore.add(lore));
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
                main().getMessageParser()
                        .translateComponent(
                                ComponentUtils.deserializeFromJson(name),
                                languagePlayer,
                                main().getConfig().getItemsSyntax()
                        )
                        .map(ComponentUtils::ensureNotItalic)
                        .map(ComponentUtils::serializeToJson)
                        .ifChanged(result -> display.put("Name", result))
                        .ifToRemove(() -> display.remove("Name"));
            } else {
                main().getMessageParser()
                        .translateString(
                                name,
                                languagePlayer,
                                main().getConfig().getItemsSyntax()
                        )
                        .ifChanged(result -> display.put("Name", result))
                        .ifToRemove(() -> display.remove("Name"));
            }
        }

        if (translateLore && display.containsKey("Lore")) {
            NbtList<String> loreNbt = display.getListOrDefault("Lore");

            List<String> newLore = new ArrayList<>();
            for (String lore : loreNbt) {
                if (main().getMcVersion() >= 13) {
                    main().getMessageParser()
                            .translateComponent(
                                    ComponentUtils.deserializeFromJson(lore),
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax()
                            )
                            .map(ComponentUtils::splitByNewLine)
                            .ifChanged(result -> newLore.addAll(
                                    result.stream()
                                            .map(ComponentUtils::ensureNotItalic)
                                            .map(ComponentUtils::serializeToJson)
                                            .collect(Collectors.toList())
                            ))
                            .ifUnchanged(() -> newLore.add(lore));
                } else {
                    main().getMessageParser()
                            .translateString(
                                    lore,
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax()
                            )
                            .ifChanged(result -> newLore.addAll(
                                    Arrays.asList(result.split("\n"))
                            ))
                            .ifUnchanged(() -> newLore.add(lore));
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

    private static SpigotTriton main() {
        return SpigotTriton.asSpigot();
    }

}
