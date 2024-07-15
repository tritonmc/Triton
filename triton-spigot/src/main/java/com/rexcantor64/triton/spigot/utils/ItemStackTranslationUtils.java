package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.google.gson.JsonSyntaxException;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.wrappers.items.WrappedFilterable;
import com.rexcantor64.triton.spigot.wrappers.items.WrappedItemContainerContents;
import com.rexcantor64.triton.spigot.wrappers.items.WrappedItemLore;
import com.rexcantor64.triton.spigot.wrappers.items.WrappedPatchedDataComponentMap;
import com.rexcantor64.triton.spigot.wrappers.items.WrappedWrittenBookContent;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemStackTranslationUtils {

    private static final MethodAccessor NBT_DESERIALIZER_METHOD;

    static {
        val mojangsonParserClass = MinecraftReflection.getMinecraftClass("nbt.TagParser", "nbt.MojangsonParser", "MojangsonParser");
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
    @Contract("null, _, _ -> null; !null, _, _ -> !null")
    public static @Nullable ItemStack translateItemStack(@Nullable ItemStack item, @NotNull Localized languagePlayer, boolean translateBooks) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        if (MinecraftVersion.v1_20_5.atOrAbove()) { // 1.20.6+
            return translateItemStackPost_1_20_6(item, languagePlayer, translateBooks, true);
        } else {
            return translateItemStackPre_1_20_6(item, languagePlayer, translateBooks);
        }
    }

    private static @NotNull ItemStack translateItemStackPre_1_20_6(@NotNull ItemStack item, @NotNull Localized languagePlayer, boolean translateBooks) {
        NbtCompound compound = null;
        try {
            val nbtTagOptional = NbtFactory.fromItemOptional(item);
            if (!nbtTagOptional.isPresent()) return item;
            compound = NbtFactory.asCompound(nbtTagOptional.get());
        } catch (IllegalArgumentException ignore) {
            // This means the item is just an ItemStack and not a CraftItemStack
            // However we can still translate stuff using the Bukkit ItemMeta API instead of NBT tags
        }

        if (compound == null) {
            // If the item is not a craft item, use the Bukkit API
            return translateBukkitItemStack(item, languagePlayer);
        }

        // Translate the contents of shulker boxes
        if (compound.containsKey("BlockEntityTag")) {
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

        // try to translate name and lore
        translateNbtItem(compound, languagePlayer, true);
        // translate the content of written books
        if (translateBooks && item.getType() == Material.WRITTEN_BOOK && main().getConfig().isBooks()) {
            if (compound.containsKey("pages")) {
                NbtList<String> pages = compound.getList("pages");
                Collection<NbtBase<String>> pagesCollection = pages.asCollection();
                List<String> newPagesCollection = new ArrayList<>();
                for (NbtBase<String> page : pagesCollection) {
                    try {
                        main().getMessageParser()
                                .translateComponent(
                                        ComponentUtils.deserializeFromJson(page.getValue()),
                                        languagePlayer,
                                        main().getConfig().getItemsSyntax()
                                )
                                .map(ComponentUtils::serializeToJson)
                                .ifChanged(newPagesCollection::add)
                                .ifUnchanged(() -> newPagesCollection.add(page.getValue()));
                    } catch (JsonSyntaxException e) {
                        val pageString = page.getValue();
                        main().getMessageParser()
                                .translateString(
                                        pageString,
                                        languagePlayer,
                                        main().getConfig().getItemsSyntax()
                                )
                                .ifChanged(newPagesCollection::add)
                                .ifUnchanged(() -> newPagesCollection.add(pageString));
                    }
                }
                compound.put("pages", NbtFactory.ofList("pages", newPagesCollection));
            }
        }
        return item;
    }

    private static @NotNull ItemStack translateItemStackPost_1_20_6(@NotNull ItemStack item, @NotNull Localized languagePlayer, boolean translateBooks, boolean translateLore) {
        Optional<WrappedPatchedDataComponentMap> componentMapOpt = WrappedPatchedDataComponentMap.fromItemStack(item);
        if (!componentMapOpt.isPresent()) {
            // item stack is not a craft item stack, delegate to the bukkit translator
            return translateBukkitItemStack(item, languagePlayer);
        }
        WrappedPatchedDataComponentMap componentMap = componentMapOpt.get();

        // translate custom name
        WrappedChatComponent customName = componentMap.getCustomName();
        if (customName != null) {
            main().getMessageParser()
                    .translateComponent(
                            WrappedComponentUtils.deserialize(customName),
                            languagePlayer,
                            main().getConfig().getItemsSyntax()
                    )
                    .map(ComponentUtils::ensureNotItalic)
                    .map(WrappedComponentUtils::serialize)
                    .getResultOrToRemove(() -> null)
                    .ifPresent(componentMap::setCustomName);
        }

        if (translateLore) {
            // translate lore
            WrappedItemLore itemLore = componentMap.getLore();
            if (itemLore != null) {
                List<WrappedChatComponent> newLines = new ArrayList<>();
                for (WrappedChatComponent line : itemLore.getLines()) {
                    main().getMessageParser()
                            .translateComponent(
                                    WrappedComponentUtils.deserialize(line),
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax()
                            )
                            .map(ComponentUtils::splitByNewLine)
                            .ifChanged(result -> newLines.addAll(
                                    result.stream()
                                            .map(ComponentUtils::ensureNotItalic)
                                            .map(WrappedComponentUtils::serialize)
                                            .collect(Collectors.toList())
                            ))
                            .ifUnchanged(() -> newLines.add(line));
                }
                itemLore.setLines(newLines);
                componentMap.setLore(itemLore);
            }

            // translate tooltips of shulker boxes (and other containers)
            WrappedItemContainerContents containerContents = componentMap.getContainer();
            if (containerContents != null) {
                List<ItemStack> newItems = containerContents.getItems()
                        .stream()
                        .map(containerItem -> translateItemStackPost_1_20_6(containerItem.clone(), languagePlayer, false, false))
                        .collect(Collectors.toList());
                containerContents.setItems(newItems);
                componentMap.setContainer(containerContents);
            }
        }

        // translate contents of written books
        if (translateBooks && main().getConfig().isBooks()) {
            WrappedWrittenBookContent bookContent = componentMap.getWrittenBookContent();
            if (bookContent != null) {
                List<WrappedFilterable<WrappedChatComponent>> newPages = new ArrayList<>();
                for (WrappedFilterable<WrappedChatComponent> page : bookContent.getPages()) {
                    main().getMessageParser()
                            .translateComponent(
                                    WrappedComponentUtils.deserialize(page.getRaw()),
                                    languagePlayer,
                                    main().getConfig().getItemsSyntax()
                            )
                            .map(WrappedComponentUtils::serialize)
                            .ifChanged(pageContents -> {
                                page.setRaw(pageContents);
                                newPages.add(page);
                            })
                            .ifUnchanged(() -> newPages.add(page));
                }
                bookContent.setPages(newPages);
                componentMap.setWrittenBookContent(bookContent);
            }
        }

        return item;
    }

    private static @NotNull ItemStack translateBukkitItemStack(@NotNull ItemStack item, @NotNull Localized languagePlayer) {
        if (item.hasItemMeta()) {
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
    public static void translateNbtItem(@NotNull NbtCompound compound, @NotNull Localized languagePlayer, boolean translateLore) {
        if (!compound.containsKey("display")) {
            return;
        }

        NbtCompound display = compound.getCompoundOrDefault("display");
        if (display.containsKey("Name")) {
            String name = display.getStringOrDefault("Name");
            try {
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
            } catch (JsonSyntaxException e) {
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
                try {
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
                } catch (JsonSyntaxException e) {
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
