package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.Triton;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {

    public static JSONObject applyPatches(JSONObject obj, JSONArray patches) {
        patchLoop:
        for (int i = 0; i < patches.length(); i++) {
            JSONObject patch = patches.optJSONObject(i);
            if (patch == null) continue;
            String op = patch.optString("op");
            if (op.equals("replace") || op.equals("add")) {
                String[] path = patch.optString("path").substring(1).split("/");
                Object target = getFromPath(obj, path);
                if (target == null) {
                    Triton.get().logError("Failed to apply patch to language item: array was given a non-numeric index.\nPatch: %1\nItem: %2", patch.toString(), obj.toString());
                    continue patchLoop;
                }
                if (target instanceof JSONObject) {
                    ((JSONObject) target).put(path[path.length - 1], patch.opt("value"));
                } else {
                    try {
                        int pathInt = Integer.parseInt(path[path.length - 1]);
                        ((JSONArray) target).put(pathInt, patch.opt("value"));
                    } catch (NumberFormatException e) {
                        Triton.get().logError("Failed to apply patch to language item: array was given a non-numeric index.\nPatch: %1\nItem: %2", patch.toString(), obj.toString());
                        continue patchLoop;
                    }
                }
            } else if (op.equals("remove")) {
                String[] path = patch.optString("path").substring(1).split("/");
                Object target = getFromPath(obj, path);
                if (target == null) {
                    Triton.get().logError("Failed to apply patch to language item: array was given a non-numeric index.\nPatch: %1\nItem: %2", patch.toString(), obj.toString());
                    continue patchLoop;
                }
                if (target instanceof JSONObject) {
                    ((JSONObject) target).remove(path[path.length - 1]);
                } else {
                    try {
                        int pathInt = Integer.parseInt(path[path.length - 1]);
                        ((JSONArray) target).remove(pathInt);
                    } catch (NumberFormatException e) {
                        Triton.get().logError("Failed to apply patch to language item: array was given a non-numeric index.\nPatch: %1\nItem: %2", patch.toString(), obj.toString());
                        continue patchLoop;
                    }
                }
            } else {
                Triton.get().logError("Failed to apply patch to language item: unknown operation.\nPatch: %1\nItem: %2", patch.toString(), obj.toString());
            }
        }
        return obj;
    }

    private static Object getFromPath(JSONObject obj, String[] path) {
        Object currentPath = obj;
        for (int j = 0; j < path.length - 1; j++) {
            String s = path[j];
            if (currentPath instanceof JSONObject) {
                Object target = ((JSONObject) currentPath).opt(s);
                if (target instanceof JSONObject || target instanceof JSONArray)
                    currentPath = target;
                else {
                    Object toCreate;
                    try {
                        Integer.parseInt(path[j + 1]);
                        toCreate = new JSONArray();
                    } catch (NumberFormatException ignore) {
                        toCreate = new JSONObject();
                    }
                    ((JSONObject) currentPath).put(s, toCreate);
                    currentPath = toCreate;
                }
            } else if (currentPath != null) { // Is JSONArray
                try {
                    int pathInt = Integer.parseInt(s);
                    Object target = ((JSONArray) currentPath).opt(pathInt);
                    if (target instanceof JSONObject || target instanceof JSONArray)
                        currentPath = target;
                    else {
                        Object toCreate;
                        try {
                            Integer.parseInt(path[j + 1]);
                            toCreate = new JSONArray();
                        } catch (NumberFormatException ignore) {
                            toCreate = new JSONObject();
                        }
                        ((JSONArray) currentPath).put(pathInt, toCreate);
                        currentPath = toCreate;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return currentPath;
    }

}
