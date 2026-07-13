package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ProtocolLibManager {

    /**
     * Checks if ProtocolLib is enabled and if its version matches
     * the expected version.
     * Triton requires ProtocolLib 5.4.0 or later.
     *
     * @return Whether the plugin should continue loading
     * @since 3.8.2
     */
    public static boolean isProtocolLibAvailable() {
        val protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        val logger = Triton.get().getLogger();
        val config = Triton.get().getConfig();
        if (!Bukkit.getPluginManager().isPluginEnabled(protocolLib)) {
            logger.logError("ProtocolLib IS REQUIRED! Without ProtocolLib, Triton will not translate any messages.");
            logger.logError("For the plugin to work correctly, please download the latest version of ProtocolLib.");
            return false;
        }

        if (config.isIKnowWhatIAmDoing()) {
            return true;
        }

        try {
            // Field known to exist in build 717 (commit e726f6e)
            boolean ignore = MinecraftVersion.v1_21_5.atOrAbove();
        } catch (NoSuchFieldError ignore) {
            // Triton requires ProtocolLib 5.4.0 or later
            logger.logError("ProtocolLib 5.4.0 or later is required! Older versions of ProtocolLib will only partially work or not work at all, and are therefore not recommended.");
            logger.logError("It is likely that you need the latest dev version, which you can download at https://triton.rexcantor64.com/protocollib");
            logger.logError("If you want to enable the plugin anyway, add `i-know-what-i-am-doing: true` to Triton's config.yml.");
            return false;
        }

        return true;
    }

    public static @NotNull ProtocolLibRefresher registerProtocolLibListeners() {
        val triton = SpigotTriton.asSpigot();
        ProtocolLibListener protocolLibListener;
        if (triton.getConfig().isAsyncProtocolLib()) {
            protocolLibListener = new ProtocolLibListener(triton, HandlerFunction.HandlerType.ASYNC);
        } else {
            protocolLibListener = new ProtocolLibListener(triton, HandlerFunction.HandlerType.ASYNC, HandlerFunction.HandlerType.SYNC);
        }

        // Use delayed task to try to be the last registered listener and therefore have the final say in packets
        triton.getScheduler().runSyncLater(() -> {
            if (triton.getConfig().isAsyncProtocolLib()) {
                val asyncManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
                asyncManager.registerAsyncHandler(protocolLibListener).start();
                asyncManager.registerAsyncHandler(new MotdPacketHandler()).start();
                ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibListener(triton, HandlerFunction.HandlerType.SYNC));
            } else {
                ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener);
                ProtocolLibrary.getProtocolManager().addPacketListener(new MotdPacketHandler());
            }
            triton.getLogger().logInfo("Registered ProtocolLib listeners");
        }, 1L);

        return protocolLibListener;
    }
}
