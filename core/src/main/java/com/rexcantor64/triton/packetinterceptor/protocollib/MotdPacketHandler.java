package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MotdPacketHandler extends PacketAdapter {

    public MotdPacketHandler() {
        super(Triton.asSpigot().getLoader(), ListenerPriority.HIGHEST,
                Collections.singleton(PacketType.Status.Server.SERVER_INFO), ListenerOptions.ASYNC);
    }

    /**
     * @return Whether the plugin should attempt to translate the MOTD
     */
    private boolean isMotdEnabled() {
        return Triton.get().getConf().isMotd();
    }

    /**
     * Handle a Server Info (MOTD) packet.
     * Placeholders are searched in the text itself, as well as in the ping message.
     * The resulting components are flattened as legacy text using {@link ComponentUtils#mergeComponents(BaseComponent...)}.
     *
     * @param event ProtocolLib's packet event
     */
    private void handleServerInfo(PacketEvent event) {
        val ipAddr = getPlayerIpAddress(event.getPlayer());
        if (!ipAddr.isPresent()) {
            Triton.get().getLogger().logWarning("Failed to get IP address for player, could not translate MOTD");
            return;
        }
        val lang = Triton.get().getStorage().getLanguageFromIp(ipAddr.get()).getName();
        val syntax = Triton.get().getConfig().getMotdSyntax();

        val serverPing = event.getPacket().getServerPings().readSafely(0);
        serverPing.setPlayers(serverPing.getPlayers().stream().flatMap((gp) -> {
            if (gp.getName() == null) return Stream.of(gp);
            val translatedName = Triton.get().getLanguageParser().replaceLanguages(gp.getName(), lang, syntax);
            if (gp.getName().equals(translatedName)) return Stream.of(gp);
            if (translatedName == null) return null;
            val translatedNameSplit = translatedName.split("\n", -1);
            if (translatedNameSplit.length > 1) {
                return Arrays.stream(translatedNameSplit).map(name -> new WrappedGameProfile(UUID.randomUUID(), name));
            } else {
                return Stream.of(gp.withName(translatedName));
            }
        }).collect(Collectors.toList()));

        serverPing.setVersionName(Triton.get().getLanguageParser().replaceLanguages(serverPing.getVersionName(), lang, syntax));

        val motd = serverPing.getMotD();
        val result = Triton.get().getLanguageParser()
                .parseComponent(lang, syntax, ComponentSerializer.parse(motd.getJson()));
        if (result != null)
            motd.setJson(ComponentSerializer.toString(result));
        serverPing.setMotD(motd);
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        if (packet.getPacketType() == PacketType.Status.Server.SERVER_INFO && isMotdEnabled()) {
            handleServerInfo(packet);
        }
    }

    public Optional<String> getPlayerIpAddress(Player player) {
        return Optional.ofNullable(player)
                .map(Player::getAddress)
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress);
    }
}
