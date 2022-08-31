package com.rexcantor64.triton.spigot.placeholderapi;

import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TritonPlaceholderHook extends PlaceholderExpansion implements Relational {

    private final SpigotTriton triton;

    @Override
    public @NotNull String getIdentifier() {
        return "triton";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Rexcantor64";
    }

    @Override
    public @NotNull String getVersion() {
        return triton.getLoader().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player p, @NotNull String params) {
        Localized locale;
        if (p == null) {
            locale = triton.getLanguageManager().getMainLanguage();
        } else {
            locale = triton.getPlayerManager().get(p.getUniqueId());
        }
        val component = triton.getTranslationManager().getTextComponentOr404(locale, params);
        val text = LegacyComponentSerializer.legacySection().serialize(component);

        return PlaceholderAPI.setPlaceholders(p, text);
    }



    @Override
    public String onPlaceholderRequest(Player ignore, Player viewer, String params) {
        return onPlaceholderRequest(viewer, params);
    }
}
