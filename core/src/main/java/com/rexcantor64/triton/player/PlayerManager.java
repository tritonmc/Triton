package com.rexcantor64.triton.player;

import com.google.common.collect.Maps;
import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.VelocityMLP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager implements com.rexcantor64.triton.api.players.PlayerManager {

    private Map<UUID, LanguagePlayer> players = Maps.newHashMap();

    public LanguagePlayer get(UUID p) {
        LanguagePlayer lp = players.get(p);
        if (lp != null) return lp;

        Triton.get().getLogger().logTrace("[Player Manager] Tried to get an unregistered language player, so registering a new one for UUID %1", p);

        if (Triton.get() instanceof BungeeMLP)
            players.put(p, lp = new BungeeLanguagePlayer(p));
        else if (Triton.get() instanceof SpigotMLP)
            players.put(p, lp = new SpigotLanguagePlayer(p));
        else if (Triton.get() instanceof VelocityMLP)
            players.put(p, lp = VelocityLanguagePlayer.fromUUID(p));

        return lp;
    }

    public boolean hasPlayer(UUID p) {
        return players.containsKey(p);
    }

    public void unregisterPlayer(UUID p) {
        Triton.get().getLogger().logTrace("[Player Manager] Unregistering language player with UUID %1", p);
        players.remove(p);
    }

    public void registerPlayer(LanguagePlayer lp) {
        Triton.get().getLogger().logTrace("[Player Manager] Registering language player %1", lp);
        players.put(lp.getUUID(), lp);
    }

    public List<LanguagePlayer> getAll() {
        return new ArrayList<>(players.values());
    }

}
