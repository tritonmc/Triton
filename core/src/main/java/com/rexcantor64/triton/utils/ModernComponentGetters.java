package com.rexcantor64.triton.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.KeybindComponent;

import java.util.Optional;

/**
 * Avoid loading "modern" (as in, not 1.8.8) md_5's chat library classes on old Spigot versions.
 * This will be removed in Triton v4 (where Adventure is used instead).
 */
public class ModernComponentGetters {

    public static Optional<String> getKeybind(BaseComponent component) {
        if (component instanceof KeybindComponent) {
            return Optional.ofNullable(((KeybindComponent) component).getKeybind());
        }
        return Optional.empty();
    }

    public static BaseComponent newKeybindComponent(String keybind) {
        return new KeybindComponent(keybind);
    }

}
