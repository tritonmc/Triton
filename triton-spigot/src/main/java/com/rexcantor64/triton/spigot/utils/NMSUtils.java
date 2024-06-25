package com.rexcantor64.triton.spigot.utils;

import com.rexcantor64.triton.utils.ReflectionUtils;
import org.bukkit.entity.Player;

public class NMSUtils {

    public static Object getHandle(Player target) {
        return ReflectionUtils.getMethod(target, "getHandle", new Class[0], new Object[0]);
    }

}
