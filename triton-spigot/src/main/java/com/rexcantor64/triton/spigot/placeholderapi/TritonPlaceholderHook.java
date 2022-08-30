package com.rexcantor64.triton.spigot.placeholderapi;

import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TritonPlaceholderHook extends PlaceholderExpansion implements Relational {

    private final SpigotTriton triton;

    @Override
    public String getIdentifier() {
        return "triton";
    }

    @Override
    public String getAuthor() {
        return "Rexcantor64";
    }

    @Override
    public String getVersion() {
        return triton.getLoader().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (params == null) return null;
        if (p == null) return triton.getLanguageManager().getTextFromMain(params);
        // TODO support PAPI placeholders and Adventure Components
        return triton.getLanguageManager().getText(triton.getPlayerManager().get(p.getUniqueId()), params);
    }

    @Override
    public String onPlaceholderRequest(Player ignore, Player viewer, String params) {
        return onPlaceholderRequest(viewer, params);
    }
}
