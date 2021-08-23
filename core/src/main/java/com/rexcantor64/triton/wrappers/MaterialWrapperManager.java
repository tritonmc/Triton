package com.rexcantor64.triton.wrappers;

import com.rexcantor64.triton.Triton;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Method;

public class MaterialWrapperManager {

    private static final String[] AVAILABLE_WRAPPERS = new String[]{"legacy", "v1_13"};

    private Material bannerMaterial;

    public MaterialWrapperManager() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        int mcVersion = Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[1]);

        for (String p : AVAILABLE_WRAPPERS) {
            try {
                Class<?> clazz = Class.forName("com.rexcantor64.triton.wrappers." + p + ".MaterialWrapper");
                Method method = clazz.getMethod("shouldLoad", int.class);
                boolean shouldLoad = (boolean) method.invoke(null, mcVersion);
                if (shouldLoad) {
                    bannerMaterial = (Material) clazz.getField("bannerMaterial").get(null);
                    return;
                }
            } catch (Exception | Error ignore) {
            }
        }
        Triton.get().getLogger()
                .logError("Couldn't find a wrapper for this version! The plugin might not work as expected. Is it up-to-date?");
    }

    public Material getBannerMaterial() {
        return bannerMaterial;
    }

}
