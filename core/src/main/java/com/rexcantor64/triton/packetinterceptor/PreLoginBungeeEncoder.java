package com.rexcantor64.triton.packetinterceptor;

import com.rexcantor64.triton.Triton;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.Kick;

import java.util.List;

@RequiredArgsConstructor
public class PreLoginBungeeEncoder extends MessageToMessageEncoder<DefinedPacket> {

    private final String ip;
    private String lang;

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket packet,
                          List<Object> out) {
        try {
            if (Triton.get().getConf().isKick() && packet instanceof Kick) {
                Kick kick = (Kick) packet;

                if (this.lang == null)
                    this.lang = Triton.get().getStorage().getLanguageFromIp(ip).getName();

                kick.setMessage(serializeComponent(Triton.get().getLanguageParser().parseComponent(this.lang,
                        Triton.get().getConf().getKickSyntax(), ComponentSerializer.parse(kick.getMessage()))));
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        out.add(packet);
    }

    private String serializeComponent(BaseComponent... bc) {
        if (bc == null) return ComponentSerializer.toString(new TranslatableComponent(""));
        return ComponentSerializer.toString(bc);
    }

}
