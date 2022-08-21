package com.rexcantor64.triton.bridge;

import com.rexcantor64.triton.commands.handler.CommandEvent;

public interface BridgeManager {

    /**
     * Forward a {@link CommandEvent command event} from the proxy to the player's current server.
     *
     * @param commandEvent The command event to forward.
     * @throws UnsupportedOperationException If the current platform is not a proxy.
     */
    void forwardCommand(CommandEvent commandEvent);

    /**
     * Send config and translations to every server, using plugin messaging.
     * If not using local storage, only a signal is sent for the servers to fetch from remote sources.
     *
     * @throws UnsupportedOperationException If the current platform is not a proxy.
     */
    void sendConfigToEveryone();

}
