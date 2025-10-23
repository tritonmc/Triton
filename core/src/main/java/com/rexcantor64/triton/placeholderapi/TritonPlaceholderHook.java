package com.rexcantor64.triton.placeholderapi;

import com.rexcantor64.triton.SpigotMLP;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TritonPlaceholderHook extends PlaceholderExpansion implements Relational {

    private final SpigotMLP triton;
    // whether the "viewer" in a %rel_% placeholder should be swapped
    private final boolean swapRel;

    @Override
    public String getIdentifier() {
        return swapRel ? "triton2" : "triton";
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
        return triton.getLanguageManager().getText(triton.getPlayerManager().get(p.getUniqueId()), params);
    }

    @Override
    public String onPlaceholderRequest(Player viewer2, Player viewer1, String params) {
        return onPlaceholderRequest(swapRel ? viewer2 : viewer1, params);
    }
}
