package com.rexcantor64.triton.velocity.packetinterceptor;

import com.rexcantor64.triton.velocity.packetinterceptor.packets.ChatHandler;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChat;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCounted;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class VelocityNettyEncoder extends MessageToMessageEncoder<MinecraftPacket> {

    private final VelocityLanguagePlayer player;
    private static final Map<Class<?>, BiFunction<?, VelocityLanguagePlayer, Optional<MinecraftPacket>>> handlerMap = new HashMap<>();

    static {
        val chatHandler = new ChatHandler();
        addHandler(SystemChat.class, chatHandler::handleSystemChat);
        addHandler(LegacyChat.class, chatHandler::handleLegacyChat);
    }

    private static <T extends MinecraftPacket> void addHandler(final @NotNull Class<T> type, final @NotNull BiFunction<@NotNull T, @NotNull VelocityLanguagePlayer, @NotNull Optional<MinecraftPacket>> handler) {
        handlerMap.put(type, handler);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MinecraftPacket packet, List<Object> out) {
        if (packet instanceof ReferenceCounted) {
            // We need ro retain the packet since we're just passing them through, otherwise Netty will throw an error
            ((ReferenceCounted) packet).retain();
            out.add(packet);
            return;
        }
        val handler = (BiFunction<MinecraftPacket, VelocityLanguagePlayer, Optional<MinecraftPacket>>) handlerMap.get(packet.getClass());
        if (handler != null) {
            Optional<MinecraftPacket> result = handler.apply(packet, this.player);
            result.ifPresent(out::add);
        } else {
            out.add(packet);
        }
    }

}
