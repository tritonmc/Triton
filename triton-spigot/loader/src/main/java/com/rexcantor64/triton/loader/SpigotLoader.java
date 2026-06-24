package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.CommonLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotLoader extends JavaPlugin {
    private static final String PLATFORM_JAR_NAME = "triton-spigot.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    public SpigotLoader() {
        val builder = CommonLoader.builder()
                .jarInJarName(PLATFORM_JAR_NAME)
                .bootstrapClassName(BOOTSTRAP_CLASS)
                .constructorType(JavaPlugin.class)
                .constructorValue(this);

        if (shouldVendorAdventure()) {
            builder.flag(LoaderFlag.VENDOR_ADVENTURE);
            builder.flag(LoaderFlag.VENDOR_ADVENTURE_NBT);
            builder.flag(LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS);
        } else {
            if (shouldVendorAdventureNbt()) {
                builder.flag(LoaderFlag.VENDOR_ADVENTURE_NBT);
            }
            if (shouldVendorAdventureSerializers()) {
                builder.flag(LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS);
            }
        }
        // paper does not include the bungee md5 library serializer
        builder.flag(LoaderFlag.VENDOR_ADVENTURE_BUNGEE_SERIALIZER);

        builder.flag(LoaderFlag.VENDOR_PACKET_EVENTS);

        this.plugin = builder
                .build()
                .loadUserLoaderFlags(this.getDataFolder().toPath())
                .loadPlugin();
    }

    private boolean shouldVendorAdventure() {
        try {
            // Method only available on adventure 4.25.0+
            // Finding a method instead of a class, since plugins can incorrectly shade newer versions than what is installed in the server
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            componentClass.getMethod("object");

            try {
                Class<?> objectComponentClass = Class.forName("net.kyori.adventure.text.ObjectComponent");
                objectComponentClass.getMethod("fallback");
                // Adventure v5+ is present and we do not support it yet!
                return true;
            } catch (ClassNotFoundException | NoSuchMethodException ignore) {
                // A modern version of adventure v4 is already present
                return false;
            }
        } catch (ClassNotFoundException | NoSuchMethodException ignore) {
            // Adventure is not present or an outdated version is present
            return true;
        }
    }

    private boolean shouldVendorAdventureNbt() {
        try {
            // Class from adventure nbt
            Class.forName("net.kyori.adventure.nbt.BinaryTag");

            // Adventure NBT is already present
            return false;
        } catch (ClassNotFoundException ignore) {
            // Adventure NBT is not present or an outdated version is present
            return true;
        }
    }

    private boolean shouldVendorAdventureSerializers() {
        try {
            // Get a class from all the wanted packages
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Class.forName("net.kyori.adventure.text.serializer.plain.PlainComponentSerializer");

            // A modern version of adventure serializers is already present
            return false;
        } catch (ClassNotFoundException ignore) {
            // Adventure serializers are not present or outdated versions are present
            return true;
        }
    }

    @Override
    public void onLoad() {
        this.plugin.onLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onEnable();
    }

    @Override
    public void onDisable() {
        this.plugin.onDisable();
    }
}
