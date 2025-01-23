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
            Triton.get().runAsync(() -> packetEventsRefresh.refreshAll());
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
}
