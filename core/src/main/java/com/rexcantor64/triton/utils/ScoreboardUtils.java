package com.rexcantor64.triton.utils;

public class ScoreboardUtils {

    public static String getEntrySuffix(int index) {
        if (index < 0)
            return "§k§l";
        if (index < 10)
            return "§k§" + index;
        if (index == 10)
            return "§k§a";
        if (index == 11)
            return "§k§b";
        if (index == 12)
            return "§k§c";
        if (index == 13)
            return "§k§d";
        if (index == 14)
            return "§k§e";
        if (index == 15)
            return "§k§f";
        return "§k§l";
    }

}
