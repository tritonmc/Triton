package com.rexcantor64.triton.packetinterceptor;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.ClientSettings;

import java.util.List;

public class BungeeDecoder extends MessageToMessageDecoder<PacketWrapper> {

    private final BungeeLanguagePlayer lp;

    public BungeeDecoder(BungeeLanguagePlayer lp) {
        this.lp = lp;
    }

    @Override
    protected void decode(ChannelHandlerContext chx, PacketWrapper wrapper, List<Object> out) throws Exception {
        try {
            if (wrapper.packet instanceof ClientSettings) {
                ClientSettings packet = (ClientSettings) wrapper.packet;
                if (lp.isWaitingForClientLocale())
                    lp.setLang(Triton.get().getLanguageManager().getLanguageByLocale(packet.getLocale(), true));
            }
            out.add(wrapper);
        } catch (NullPointerException e) {
            Triton.get().getLogger().logError(e, "Failed to fetch client locale for player %1!", this.lp.getUUID());
        }
    }
}
