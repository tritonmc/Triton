package com.rexcantor64.triton.player;

import com.google.common.collect.Maps;
import com.rexcantor64.triton.MultiLanguagePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private Map<UUID, LanguagePlayer> players = Maps.newHashMap();

    public LanguagePlayer get(UUID p) {
        LanguagePlayer lp = players.get(p);
        if (lp != null) return lp;
        if (MultiLanguagePlugin.isBungee())
            players.put(p, lp = new BungeeLanguagePlayer(p));
        else
            players.put(p, lp = new SpigotLanguagePlayer(p));
        return lp;
    }

    public void unregisterPlayer(UUID p) {
        players.remove(p);
    }

    public List<LanguagePlayer> getAll() {
        return new ArrayList<>(players.values());
    }


}
