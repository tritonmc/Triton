package com.rexcantor64.triton.language.item;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Objects;

public class LanguageSign extends LanguageItem {

    private SignLocation location;
    private HashMap<String, String[]> languages;

    public LanguageSign(SignLocation location, HashMap<String, String[]> languages, boolean universal, boolean blacklist, JSONArray servers) {
        this.location = location;
        this.languages = languages;
        super.universal = universal;
        super.blacklist = blacklist;
        super.setServers(servers);
    }

    public LanguageSign(SignLocation location, HashMap<String, String[]> languages) {
        this.location = location;
        this.languages = languages;
        super.setServers(null);
    }

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.SIGN;
    }

    public SignLocation getLocation() {
        return location;
    }

    public String[] getLines(String languageName) {
        return languages.get(languageName);
    }

    public HashMap<String, String[]> getLanguages() {
        return languages;
    }

    public static class SignLocation {
        private String world;
        private int x;
        private int y;
        private int z;

        public SignLocation(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SignLocation that = (SignLocation) o;
            return x == that.x &&
                    y == that.y &&
                    z == that.z &&
                    Objects.equals(world, that.world);
        }

    }

}
