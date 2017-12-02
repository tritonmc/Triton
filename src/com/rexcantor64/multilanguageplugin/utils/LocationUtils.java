package com.rexcantor64.multilanguageplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.json.JSONObject;

public class LocationUtils {

    public static Location jsonToLocation(JSONObject obj) {
        return new Location(Bukkit.getWorld(obj.optString("world", "world")), obj.optInt("x", 0), obj.optInt("y", 0), obj.optInt("z", 0));
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

    public static boolean equalsBlock(Location l1, Location l2) {
        return l1 == l2 || l1 != null && l2 != null && l2.getWorld().equals(l1.getWorld()) && l2.getBlockX() == l1.getBlockX() && l2.getBlockY() == l1.getBlockY() && l2.getBlockZ() == l1.getBlockZ();
    }

}
