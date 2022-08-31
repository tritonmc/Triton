package com.rexcantor64.triton.velocity.packetinterceptor;

import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCounted;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class VelocityNettyEncoder extends MessageToMessageEncoder<MinecraftPacket> {

    private final VelocityLanguagePlayer player;

    @Override
    protected void encode(ChannelHandlerContext ctx, MinecraftPacket packet, List<Object> out) {
        if (packet instanceof ReferenceCounted) {
            System.out.println("ctx = " + ctx);
            // We need ro retain the packet since we're just passing them through, otherwise Netty will throw an error
            ((ReferenceCounted) packet).retain();
        }
        System.out.println("packet = " + packet);
        out.add(packet);
    }
}
