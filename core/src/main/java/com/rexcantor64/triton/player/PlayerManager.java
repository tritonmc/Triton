package com.rexcantor64.triton.player;

import com.rexcantor64.triton.Triton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PlayerManager<T extends LanguagePlayer> implements com.rexcantor64.triton.api.players.PlayerManager {

    private final Map<UUID, T> players = new ConcurrentHashMap<>();

    private final Function<UUID, T> languagePlayerSupplier;

    public PlayerManager(Function<UUID, T> languagePlayerSupplier) {
        this.languagePlayerSupplier = languagePlayerSupplier;
    }

    public T get(UUID uuid) {
        T languagePlayer = players.get(uuid);
        if (languagePlayer != null) {
            return languagePlayer;
        }

        Triton.get().getLogger().logTrace("[Player Manager] Tried to get an unregistered language player, so registering a new one for UUID %1", uuid);

        players.put(uuid, languagePlayer = languagePlayerSupplier.apply(uuid));
        return languagePlayer;
    }

    public boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }

    public void unregisterPlayer(UUID uuid) {
        Triton.get().getLogger().logTrace("[Player Manager] Unregistering language player with UUID %1", uuid);
        players.remove(uuid);
    }

    public void registerPlayer(T languagePlayer) {
        Triton.get().getLogger().logTrace("[Player Manager] Registering language player %1", languagePlayer);
        players.put(languagePlayer.getUUID(), languagePlayer);
    }

    public List<T> getAll() {
        return new ArrayList<>(players.values());
    }

}
