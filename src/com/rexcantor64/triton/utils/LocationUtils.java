package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.item.LanguageSign;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocationUtils {

    private static LanguageSign.SignLocation jsonToLocation(JSONObject obj) {
        return new LanguageSign.SignLocation(obj.optString("server", null), obj.optString("world", "world"), obj.optInt("x", 0), obj.optInt("y", 0), obj.optInt("z", 0));
    }

    public static List<LanguageSign.SignLocation> jsonToLocationArray(JSONArray arr) {
        List<LanguageSign.SignLocation> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) {
                MultiLanguagePlugin.get().logWarning("Found invalid location while reading sign! Make sure all locations are a JSONObject.");
                continue;
            }
            result.add(jsonToLocation(obj));
        }
        return result;
    }

    public static JSONObject locationToJSON(int x, int y, int z, String world) {
        return new JSONObject().put("world", world).put("x", x).put("y", y).put("z", z);
    }

    public static boolean equalsJSONLocation(JSONObject obj1, JSONObject obj2) {
        return obj1 == obj2 || obj1 != null && obj2 != null && obj1.optInt("x", 0) == obj2.optInt("x", 0) && obj1.optInt("y", 0) == obj2.optInt("y", 0) && obj1.optInt("z", 0) == obj2.optInt("z", 0) && obj1.optString("world", "world").equals(obj2.optString("world", "world"));
    }

}
