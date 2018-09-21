package com.rexcantor64.triton.wrappers.items;

import com.rexcantor64.triton.banners.Banner;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

public class ItemStackParser {

    public static ItemStack bannerToItemStack(Banner banner, boolean glow) {
        ItemStack is = new ItemStack(Material.BANNER);
        if (glow)
            is.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        BannerMeta bm = (BannerMeta) is.getItemMeta();
        for (Banner.Layer layer : banner.getLayers())
            bm.addPattern(new Pattern(DyeColor.valueOf(layer.getColor().getColor()), PatternType.valueOf(layer.getPattern().getType())));
        bm.setDisplayName(banner.getDisplayName());
        bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        is.setItemMeta(bm);
        return is;
    }

}
