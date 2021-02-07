package com.rexcantor64.triton.wrappers.items;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.banners.Banner;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.Collections;

public class ItemStackParser {

    public static ItemStack bannerToItemStack(Banner banner, boolean active) {
        ItemStack is = new ItemStack(Triton.asSpigot().getWrapperManager().getBannerMaterial());
        BannerMeta bm = (BannerMeta) is.getItemMeta();
        for (Banner.Layer layer : banner.getLayers())
            bm.addPattern(new Pattern(DyeColor.valueOf(layer.getColor().getColor()), PatternType
                    .valueOf(layer.getPattern().getType())));
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', banner.getDisplayName()));
        if (active)
            bm.setLore(Collections.singletonList(ChatColor
                    .translateAlternateColorCodes('&', Triton.get().getMessagesConfig().getMessage("other.selected"))));
        bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        is.setItemMeta(bm);
        return is;
    }

}
