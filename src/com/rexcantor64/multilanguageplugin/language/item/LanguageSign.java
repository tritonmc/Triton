package com.rexcantor64.multilanguageplugin.language.item;

import org.bukkit.Location;

import java.util.HashMap;

public class LanguageSign extends LanguageItem {

    private SignLocation location;
    private HashMap<String, String[]> languages;

    public LanguageSign(SignLocation location, HashMap<String, String[]> languages) {
        super.type = LanguageItemType.SIGN;
        this.location = location;
        this.languages = languages;
    }

    public SignLocation getLocation() {
        return location;
    }

    public String[] getLines(String languageName) {
        return languages.get(languageName);
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
    }

}
