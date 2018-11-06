package com.rexcantor64.triton.language.item;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LanguageSign extends LanguageItem {

    private List<SignLocation> locations;
    private HashMap<String, String[]> languages;

    public LanguageSign(String key, List<SignLocation> locations, HashMap<String, String[]> languages) {
        super(key);
        this.locations = locations;
        this.languages = languages;
    }

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.SIGN;
    }

    public List<SignLocation> getLocations() {
        return locations;
    }

    public boolean hasLocation(SignLocation loc) {
        return hasLocation(loc, false);
    }

    public boolean hasLocation(SignLocation loc, boolean checkServer) {
        if (loc != null)
            for (SignLocation l : locations)
                if (checkServer ? loc.equals(l) : loc.equalsNoServer(l)) return true;
        return false;
    }

    public String[] getLines(String languageName) {
        return languages.get(languageName);
    }

    public HashMap<String, String[]> getLanguages() {
        return languages;
    }

    public static class SignLocation {
        private String server;
        private String world;
        private int x;
        private int y;
        private int z;

        public SignLocation(String server, String world, int x, int y, int z) {
            this.server = server;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public SignLocation(String world, int x, int y, int z) {
            this.server = null;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
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
                    Objects.equals(server, that.server) &&
                    Objects.equals(world, that.world);
        }

        public boolean equalsNoServer(Object o) {
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
