package com.rexcantor64.triton.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class TritonLanguagePlayer<P> implements LanguagePlayer {

    @Nullable
    @Getter
    private PacketEventsRefresh packetEventsRefresh;

    /**
     * Keep track of many active connections exist for this player.
     * While it is not possible for more than one connection to exist in the PLAY phase,
     * a user can attempt to log in with the same UUID as a player that is already in-game.
     * Therefore, we need to know how many active connections there are to decide
     * whether to unregister this player once they disconnect.
     *
     * @since 4.1.0
     */
    @Getter
    private int connectionCount = 0;

    protected TritonLanguagePlayer() {
        if (Triton.get().getConfig().isUsePacketEvents()) {
            this.packetEventsRefresh = new PacketEventsRefresh(this);
        }
    }

    public abstract boolean isWaitingForClientLocale();

    public abstract void waitForClientLocale();

    public abstract @NotNull Optional<P> getPlatformPlayer();

    public void refreshAll() {
        if (packetEventsRefresh != null) {
            Triton.get().getScheduler().runAsync(() -> packetEventsRefresh.refreshAll());
        }
    }

    @Override
    public Language getLanguage() {
        return this.getLang();
    }

    /**
     * @return the UUID that is to be used for storage-related operations
     */
    public UUID getStorageUniqueId() {
        return this.getUUID();
    }

    public void increaseConnectionCount() {
        connectionCount += 1;
    }

    public void decreaseConnectionCount() {
        connectionCount -= 1;
    }
}
