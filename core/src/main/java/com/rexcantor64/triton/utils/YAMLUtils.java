package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.config.interfaces.Configuration;
import lombok.val;

import java.util.HashMap;
import java.util.List;

public class YAMLUtils {

    public static List<String> getStringOrStringList(Configuration config, String index) {
        val result = config.getStringList(index);
        if (result.size() == 0)
            if (config.getString(index) != null) result.add(config.getString(index));
        return result;
    }

    public static HashMap<String, Object> deepToMap(Configuration conf, String prefix) {
        val result = new HashMap<String, Object>();
        for (val key : conf.getKeys()) {
            Object value = conf.get(key);
            if (value instanceof Configuration)
                result.putAll(deepToMap(conf, prefix + key + "."));
            else
                result.put(prefix + key, value);
        }
        return result;
    }

}
