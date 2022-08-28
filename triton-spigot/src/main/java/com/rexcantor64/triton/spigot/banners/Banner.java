package com.rexcantor64.triton.spigot.banners;

import com.rexcantor64.triton.Triton;

import java.util.ArrayList;
import java.util.List;

public class Banner {

    private final List<Layer> layers = new ArrayList<>();

    public Banner(String encoded) {
        List<String> strings = new ArrayList<>();
        int index = 0;
        while (index < encoded.length()) {
            strings.add(encoded.substring(index, Math.min(index + 2, encoded.length())));
            index += 2;
        }
        for (String s : strings) {
            if (s.length() != 2) {
                Triton.get().getLogger()
                        .logError("Can't load layer %1 for banner %2 because it has an invalid format!", s, encoded);
                continue;
            }
            Colors color = Colors.getByCode(s.charAt(0));
            Patterns type = Patterns.getByCode(s.charAt(1));
            if (color == null) {
                Triton.get().getLogger()
                        .logError("Can't load layer %1 for banner %2 because the color is invalid!", s, encoded);
                continue;
            }
            if (type == null) {
                Triton.get().getLogger()
                        .logError("Can't load layer %1 for banner %2 because the pattern is invalid!", s, encoded);
                continue;
            }
            layers.add(new Layer(color, type));
        }
    }

    public List<Layer> getLayers() {
        return layers;
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
