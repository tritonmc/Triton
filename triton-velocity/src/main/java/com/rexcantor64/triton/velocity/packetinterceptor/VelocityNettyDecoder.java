package com.rexcantor64.triton.velocity.packetinterceptor;

import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCounted;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class VelocityNettyDecoder extends MessageToMessageDecoder<MinecraftPacket> {

    private final VelocityLanguagePlayer player;

    @Override
    protected void decode(ChannelHandlerContext ctx, MinecraftPacket packet, List<Object> out) {
        if (packet instanceof ReferenceCounted) {
            // We need to retain the packet since we're just passing them through, otherwise Netty will throw an error
            ((ReferenceCounted) packet).retain();
            out.add(packet);
            return;
        }
        // PlayerSettingsChangedEvent is not working on 1.20.2, so we are using packets instead
        if (packet instanceof ClientSettingsPacket) {
            ClientSettingsPacket cs = (ClientSettingsPacket) packet;
            player.setClientLocale(cs.getLocale());
        }
        out.add(packet);
    }

}
