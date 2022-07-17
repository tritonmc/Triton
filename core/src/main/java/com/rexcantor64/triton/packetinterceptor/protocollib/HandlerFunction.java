package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.events.PacketEvent;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;

/**
 * Wrapper for a {@link PacketEvent} handler along whether it can be
 * handled async or not.
 *
 * @since 3.8.1
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class HandlerFunction {
    private final BiConsumer<PacketEvent, SpigotLanguagePlayer> handlerFunction;
    private final HandlerType handlerType;

    /**
     * Build a handler function that can be run both synchronously and asynchronously.
     *
     * @param handlerFunction The handler function.
     * @return A {@link HandlerFunction} wrapping the handler function.
     * @since 3.8.1
     */
    public static HandlerFunction asAsync(BiConsumer<PacketEvent, SpigotLanguagePlayer> handlerFunction) {
        return new HandlerFunction(handlerFunction, HandlerType.ASYNC);
    }

    /**
     * Build a handler function that can only be run synchronously.
     *
     * @param handlerFunction The handler function.
     * @return A {@link HandlerFunction} wrapping the handler function.
     * @since 3.8.1
     */
    public static HandlerFunction asSync(BiConsumer<PacketEvent, SpigotLanguagePlayer> handlerFunction) {
        return new HandlerFunction(handlerFunction, HandlerType.SYNC);
    }

    public enum HandlerType {
        SYNC, ASYNC
    }
}
