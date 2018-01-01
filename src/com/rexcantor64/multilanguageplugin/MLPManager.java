package com.rexcantor64.multilanguageplugin;

public class MLPManager {

    private static MultiLanguagePlugin instance;

    static void setInstance(MultiLanguagePlugin ins) {
        instance = ins;
    }

    public static SpigotMLP getSpigot() {
        if (instance instanceof SpigotMLP) return (SpigotMLP) instance;
        return null;
    }

    public static BungeeMLP getBungee() {
        if (instance instanceof BungeeMLP) return (BungeeMLP) instance;
        return null;
    }

    public static MultiLanguagePlugin get() {
        return instance;
    }

}
