package com.rexcantor64.multilanguageplugin.banners;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.List;

public class Banner {

    private final ItemStack is = new ItemStack(Material.BANNER);

    public Banner(String encoded) {
        List<String> strings = new ArrayList<>();
        int index = 0;
        while (index < encoded.length()) {
            strings.add(encoded.substring(index, Math.min(index + 2, encoded.length())));
            index += 2;
        }
        for (String s : strings) {
            if (s.length() != 2) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because it has an invalid format!", s, encoded);
                continue;
            }
            BannerMeta bm = getMeta();
            Colors color = Colors.getByCode(s.charAt(0));
            Patterns type = Patterns.getByCode(s.charAt(1));
            if (color == null) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because the color is invalid!", s, encoded);
                continue;
            }
            if (type == null) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because the pattern is invalid!", s, encoded);
                continue;
            }
            bm.addPattern(new Pattern(color.getColor(), type.getType()));
            is.setItemMeta(bm);
        }
        BannerMeta bm = getMeta();
        bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        is.setItemMeta(bm);
    }

    public BannerMeta getMeta() {
        return (BannerMeta) is.getItemMeta();
    }

    public ItemStack toBukkit() {
        return is.clone();
    }

}
