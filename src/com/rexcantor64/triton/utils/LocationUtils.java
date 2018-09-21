package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.language.item.LanguageSign;
import org.json.JSONObject;

public class LocationUtils {

    public static LanguageSign.SignLocation jsonToLocation(JSONObject obj) {
        return new LanguageSign.SignLocation(obj.optString("world", "world"), obj.optInt("x", 0), obj.optInt("y", 0), obj.optInt("z", 0));
    }

    public static JSONObject locationToJSON(int x, int y, int z, String world) {
        JSONObject obj = new JSONObject();
        obj.put("x", x);
        obj.put("y", y);
        obj.put("z", z);
        obj.put("world", world);
        return obj;
    }

    public static boolean equalsJSONLocation(JSONObject obj1, JSONObject obj2) {
        return obj1 == obj2 || obj1 != null && obj2 != null && obj1.optInt("x", 0) == obj2.optInt("x", 0) && obj1.optInt("y", 0) == obj2.optInt("y", 0) && obj1.optInt("z", 0) == obj2.optInt("z", 0) && obj1.optString("world", "world").equals(obj2.optString("world", "world"));
    }

}
