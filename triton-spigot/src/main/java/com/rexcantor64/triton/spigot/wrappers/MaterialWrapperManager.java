package com.rexcantor64.triton.spigot.wrappers;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Objects;

public class MaterialWrapperManager {

    private static final String[] MATERIAL_NAMES = new String[]{"BLACK_BANNER", "BANNER"};

    @Getter
    private Material bannerMaterial;

    public MaterialWrapperManager() {
        this.bannerMaterial = Arrays.stream(MATERIAL_NAMES)
                .map(Material::getMaterial)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Can't find a suitable material for banners for this Minecraft version. The plugin might not work as expected. Is it up-to-date?")
                );
    }

}
