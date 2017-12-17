package com.rexcantor64.multilanguageplugin.utils;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class YAMLUtils {

    public static List<String> getStringOrStringList(ConfigurationSection config, String index) {
        List<String> result = config.getStringList(index);
        if (result.size() == 0)
            if (config.getString(index) != null) result.add(config.getString(index));
        return result;
    }

}
