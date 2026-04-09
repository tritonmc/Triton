package com.rexcantor64.triton.spigot.banners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

public class BannerBuilder {

    private final static ItemFlag[] ITEM_FLAGS;

    static {
        // Enum value was renamed in MC 1.20.6
        ItemFlag hideAdditionalTooltipFlag = Stream.of("HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP")
                .map(name -> {
                    try {
                        return ItemFlag.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to get HIDE_ADDITIONAL_TOOLTIP item flag"));

        ITEM_FLAGS = new ItemFlag[]{
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                hideAdditionalTooltipFlag,
        };
    }

    private final HashMap<Language, Banner> bannerCache = new HashMap<>();

    public void flushCache() {
        this.bannerCache.clear();
    }

    public ItemStack fromLanguage(Language language, boolean active) {
        final Banner banner = bannerCache.computeIfAbsent(language, (lang) -> new Banner(lang.getFlagCode()));

        return bannerToItemStack(banner, language.getDisplayNameComponent(), active);
    }

    private ItemStack bannerToItemStack(Banner banner, Component displayName, boolean active) {
        ItemStack itemStack = new ItemStack(SpigotTriton.asSpigot().getWrapperManager().getBannerMaterial());
        BannerMeta bannerMeta = Objects.requireNonNull((BannerMeta) itemStack.getItemMeta());
        for (Banner.Layer layer : banner.getLayers()) {
            val dyeColor = layer.getColor().toDyeColor();
            val patternType = layer.getPattern().toPatternType();
            bannerMeta.addPattern(new Pattern(dyeColor, patternType));
        }
        bannerMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(displayName));
        if (active) {
            val selectedMsg = LegacyComponentSerializer.legacySection().serialize(Triton.get().getMessagesConfig().getMessageComponent("other.selected"));
            bannerMeta.setLore(Collections.singletonList(selectedMsg));
        }
        bannerMeta.addItemFlags(ITEM_FLAGS);
        itemStack.setItemMeta(bannerMeta);
        return itemStack;
    }

}
