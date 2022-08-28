package com.rexcantor64.triton.spigot.utils;

import com.rexcantor64.triton.utils.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NMSUtils {

    public static Object getHandle(Player target) {
        return ReflectionUtils.getMethod(target, "getHandle", new Class[0], new Object[0]);
    }

    public static Class<?> getNMSClass(String className) {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        try {
            Class<?> c = Class.forName("net.minecraft.server." + version + "." + className);
            return c;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Class<?> getCraftbukkitClass(String className) {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        try {
            Class<?> c = Class.forName("org.bukkit.craftbukkit." + version + "." + className);
            return c;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
