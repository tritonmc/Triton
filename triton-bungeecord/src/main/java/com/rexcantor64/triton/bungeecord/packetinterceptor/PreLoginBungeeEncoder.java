package com.rexcantor64.triton.bungeecord.packetinterceptor;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.utils.ComponentUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.Kick;

import java.util.List;

@RequiredArgsConstructor
public class PreLoginBungeeEncoder extends MessageToMessageEncoder<DefinedPacket> {

    private final String ip;
    private Language lang;

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket packet,
                          List<Object> out) {
        try {
            if (Triton.get().getConfig().isKick() && packet instanceof Kick) {
                Kick kickPacket = (Kick) packet;

                if (this.lang == null) {
                    this.lang = Triton.get().getStorage().getLanguageFromIp(ip);
                }

                Triton.get().getMessageParser()
                        .translateComponent(
                                ComponentUtils.deserializeFromJson(kickPacket.getMessage()),
                                this.lang,
                                Triton.get().getConfig().getKickSyntax()
                        )
                        .map(ComponentUtils::serializeToJson)
                        .ifChanged(kickPacket::setMessage)
                        .ifToRemove(() -> kickPacket.setMessage(ComponentUtils.serializeToJson(Component.empty())));
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        out.add(packet);
    }

}
