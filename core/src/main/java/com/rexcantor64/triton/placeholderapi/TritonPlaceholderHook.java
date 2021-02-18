package com.rexcantor64.triton.placeholderapi;

import com.rexcantor64.triton.SpigotMLP;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TritonPlaceholderHook extends PlaceholderExpansion {

    private final SpigotMLP triton;

    public TritonPlaceholderHook(SpigotMLP triton) {
        this.triton = triton;
    }

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
        return triton.getLanguageManager().getText(triton.getPlayerManager().get(p.getUniqueId()), params);
    }
}
