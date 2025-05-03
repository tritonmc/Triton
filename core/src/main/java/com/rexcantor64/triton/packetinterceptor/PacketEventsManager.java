package com.rexcantor64.triton.packetinterceptor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.NonNull;
import lombok.val;

/**
 * This class exists to avoid {@link NoClassDefFoundError} when not using PacketEvents.
 * It acts as a "buffer" so that the PacketEvents classes are not loaded by Java unless
 * PacketEvents support is enabled for Triton.
 *
 * @since 4.0.0
 */
public abstract class PacketEventsManager {

    private final @NonNull PacketEventsListener packetEventsListener = new PacketEventsListener();

    @SuppressWarnings("UnstableApiUsage")
    public void onLoad() {
        val dependencyManager = Triton.get().getLoader().getDependencyManager();
        val isPacketEventsVendored = dependencyManager.hasLoaderFlag(LoaderFlag.VENDOR_PACKET_EVENTS);
        if (isPacketEventsVendored) {
            // load packet events dependency (api is loaded in the Triton class to allow this class to load successfully)
            dependencyManager.loadDependency(Dependency.PACKET_EVENTS_NETTY_COMMON);
            dependencyManager.loadDependency(this.getPlatformPacketEventsDependency());

            initPacketEventsPlatform();
        }
        this.packetEventsListener.setupHandlers();
        PacketEvents.getAPI().load();
        if (isPacketEventsVendored) {
            // Don't check for PacketEvents updates if it's vendored (the user can't do anything)
            PacketEvents.getAPI().getSettings().checkForUpdates(false);
            // Disable re-encoding by default for performance, Triton correctly marks packets as needing re-encoding when anything is changed.
            // We can't do this when not vendored, since it might affect other plugins.
            PacketEvents.getAPI().getSettings().reEncodeByDefault(false);
        }
        PacketEvents.getAPI().getEventManager().registerListener(this.packetEventsListener, PacketListenerPriority.HIGHEST);
    }

    public void onEnable() {
        PacketEvents.getAPI().init();
    }

    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public void onReload() {
        this.packetEventsListener.setupHandlers();
    }

    /**
     * Call {@link PacketEvents#setAPI(PacketEventsAPI)} with the appropriate platform.
     *
     * @since 4.0.0
     */
    protected abstract void initPacketEventsPlatform();

    /**
     * @return Get the PacketEvents dependency to download for the current platform
     * @since 4.0.0
     */
    protected abstract @NonNull Dependency getPlatformPacketEventsDependency();
}
