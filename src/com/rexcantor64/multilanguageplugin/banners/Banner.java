package com.rexcantor64.multilanguageplugin.banners;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;

import java.util.ArrayList;
import java.util.List;

public class Banner {

    private List<Layer> layers = new ArrayList<>();
    private String displayName;

    public Banner(String encoded, String displayName) {
        this.displayName = displayName;
        List<String> strings = new ArrayList<>();
        int index = 0;
        while (index < encoded.length()) {
            strings.add(encoded.substring(index, Math.min(index + 2, encoded.length())));
            index += 2;
        }
        for (String s : strings) {
            if (s.length() != 2) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because it has an invalid format!", s, encoded);
                continue;
            }
            Colors color = Colors.getByCode(s.charAt(0));
            Patterns type = Patterns.getByCode(s.charAt(1));
            if (color == null) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because the color is invalid!", s, encoded);
                continue;
            }
            if (type == null) {
                MultiLanguagePlugin.get().logError("Can't load layer %1 for banner %2 because the pattern is invalid!", s, encoded);
                continue;
            }
            layers.add(new Layer(color, type));
        }
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public String getDisplayName() {
        return displayName;
    }

    public class Layer {
        private final Colors color;
        private final Patterns pattern;

        private Layer(Colors color, Patterns pattern) {
            this.color = color;
            this.pattern = pattern;
        }

        public Colors getColor() {
            return color;
        }

        public Patterns getPattern() {
            return pattern;
        }
    }

}
