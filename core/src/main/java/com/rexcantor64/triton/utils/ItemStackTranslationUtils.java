package com.rexcantor64.triton.utils;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemContainerContents;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.WrittenBookContent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.util.Filterable;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ItemStackTranslationUtils {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public ModifiableItemStack translateItem(ItemStack item, Localized locale) {
        val modifiableItem = new ModifiableItemStack(item);
        this.translateItem(modifiableItem, locale, true, true);
        return modifiableItem;
    }

    private void translateItem(ModifiableItemStack item, Localized locale, boolean translateLore, boolean translateBook) {
        translateItemName(item, locale);
        translateCustomName(item, locale);
        if (translateLore) {
            translateLore(item, locale);
            translateContainerContents(item, locale);
        }
        if (translateBook) {
            translateBook(item, locale);
        }
    }

    private void translateItemName(ModifiableItemStack item, Localized locale) {
        val itemNameOpt = item.getOriginal().getComponent(ComponentTypes.ITEM_NAME);
        if (!itemNameOpt.isPresent()) {
            return;
        }

        val itemName = itemNameOpt.get();
        parser.translateComponent(
                        itemName,
                        locale,
                        this.syntax
                )
                .map(ComponentUtils::ensureNotItalic)
                .ifChanged(result -> item.willModify().setComponent(ComponentTypes.ITEM_NAME, result))
                .ifToRemove(() -> item.willModify().unsetComponent(ComponentTypes.ITEM_NAME));
    }

    private void translateCustomName(ModifiableItemStack item, Localized locale) {
        val itemNameOpt = item.getOriginal().getComponent(ComponentTypes.CUSTOM_NAME);
        if (!itemNameOpt.isPresent()) {
            return;
        }

        val itemName = itemNameOpt.get();
        parser.translateComponent(
                        itemName,
                        locale,
                        this.syntax
                )
                .map(ComponentUtils::ensureNotItalic)
                .ifChanged(result -> item.willModify().setComponent(ComponentTypes.CUSTOM_NAME, result))
                .ifToRemove(() -> item.willModify().unsetComponent(ComponentTypes.CUSTOM_NAME));
    }

    private void translateLore(ModifiableItemStack item, Localized locale) {
        val loreOpt = item.getOriginal().getComponent(ComponentTypes.LORE);
        if (!loreOpt.isPresent()) {
            return;
        }

        val lore = loreOpt.get();
        AtomicBoolean loreChanged = new AtomicBoolean(false);
        List<Component> newLines = new ArrayList<>();
        for (Component line : lore.getLines()) {
            parser.translateComponent(
                            line,
                            locale,
                            this.syntax
                    )
                    .map(ComponentUtils::splitByNewLine)
                    .ifChanged(result -> {
                        newLines.addAll(
                                result.stream()
                                        .map(ComponentUtils::ensureNotItalic)
                                        .collect(Collectors.toList())
                        );
                        loreChanged.set(true);
                    })
                    .ifUnchanged(() -> newLines.add(line))
                    .ifToRemove(() -> loreChanged.set(true));
        }
        if (!loreChanged.get()) {
            return;
        }
        item.willModify().setComponent(ComponentTypes.LORE, new ItemLore(newLines));
    }

    private void translateContainerContents(ModifiableItemStack item, Localized locale) {
        val containerContentsOpt = item.getOriginal().getComponent(ComponentTypes.CONTAINER);
        if (!containerContentsOpt.isPresent()) {
            return;
        }

        boolean contentsChanged = false;
        val containerContents = containerContentsOpt.get();
        val newContents = new ArrayList<ItemStack>(containerContents.getItems().size());
        for (val itemStack : containerContents.getItems()) {
            val modifiableItem = new ModifiableItemStack(itemStack);
            translateItem(modifiableItem, locale, false, false);
            if (modifiableItem.isModified()) {
                newContents.add(modifiableItem.getModified());
                contentsChanged = true;
            } else {
                newContents.add(itemStack);
            }
        }

        if (contentsChanged) {
            item.willModify().setComponent(ComponentTypes.CONTAINER, new ItemContainerContents(newContents));
        }
    }

    private void translateBook(ModifiableItemStack item, Localized locale) {
        val bookContentOpt = item.getOriginal().getComponent(ComponentTypes.WRITTEN_BOOK_CONTENT);
        if (!bookContentOpt.isPresent()) {
            return;
        }

        boolean contentsChanged = false;
        val bookContent = bookContentOpt.get();
        val newPages = new ArrayList<Filterable<@NotNull Component>>(bookContent.getPages().size());
        for (val page : bookContent.getPages()) {
            val translationResult = parser.translateComponent(
                    page.getRaw(),
                    locale,
                    syntax
            );
            translationResult
                    .ifChanged(result -> {
                        newPages.add(new Filterable<>(result, page.getFiltered()));
                    })
                    .ifUnchanged(() -> newPages.add(page));
            contentsChanged |= !translationResult.isUnchanged();
        }

        if (contentsChanged) {
            item.willModify().setComponent(
                    ComponentTypes.WRITTEN_BOOK_CONTENT,
                    new WrittenBookContent(
                            bookContent.getTitle(),
                            bookContent.getAuthor(),
                            bookContent.getGeneration(),
                            newPages,
                            bookContent.isResolved()
                    )
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ModifiableItemStack {
        private final @NotNull ItemStack original;
        private @Nullable ItemStack modified = null;

        public boolean isModified() {
            return this.modified != null;
        }

        public @NotNull ItemStack willModify() {
            if (this.modified == null) {
                this.modified = this.original.copy();
            }
            return this.modified;
        }
    }

}
