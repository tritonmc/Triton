package com.rexcantor64.multilanguageplugin.player;

import com.google.common.collect.Maps;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private Map<UUID, LanguagePlayer> players = Maps.newHashMap();

    public LanguagePlayer get(Player p) {
        LanguagePlayer lp = players.get(p.getUniqueId());
        if (lp != null) return lp;
        players.put(p.getUniqueId(), lp = new LanguagePlayer(p));
        return lp;
    }

    public void unregisterPlayer(Player p) {
        players.remove(p.getUniqueId());
    }

    public List<LanguagePlayer> getAll() {
        return new ArrayList<>(players.values());
    }


}
