package com.rexcantor64.triton.wrappers.v1_13;

import org.bukkit.Material;

public class MaterialWrapper {

    public static Material bannerMaterial = Material.BLACK_BANNER;

    public static boolean shouldLoad(int mcVersion) {
        return mcVersion >= 13;
    }
}
