package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.TranslationManager;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.spigot.SpigotTriton;
import org.bukkit.entity.Player;

import java.util.Map;

public abstract class PacketHandler {

    public abstract void registerPacketTypes(Map<PacketType, HandlerFunction> registry);

    protected SpigotTriton getMain() {
        return SpigotTriton.asSpigot();
    }

    protected MainConfig getConfig() {
        return getMain().getConfig();
    }

    protected TritonLogger logger() {
        return getMain().getLogger();
    }

    protected LanguageManager getLanguageManager() {
        return getMain().getLanguageManager();
    }

    protected TranslationManager getTranslationManager() {
        return getMain().getTranslationManager();
    }

    protected MessageParser parser() {
        return getMain().getMessageParser();
    }

    /**
     * @deprecated Use {@link MinecraftVersion#atOrAbove()} instead.
     */
    @Deprecated
    protected int getMcVersion() {
        return getMain().getMcVersion();
    }

    /**
     * Wrapper for {@link com.comphenix.protocol.ProtocolManager#sendServerPacket(Player, PacketContainer, boolean)}.
     *
     * @param bukkitPlayer The player to send the packet to.
     * @param packet       The packet itself.
     * @param filters      Whether to pass the packet through registered packet listeners.
     * @since 3.8.0
     */
    protected void sendPacket(Player bukkitPlayer, PacketContainer packet, boolean filters) {
        ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, filters);
    }

    /**
     * Wrapper for {@link com.comphenix.protocol.ProtocolManager#createPacket(PacketType)}.
     *
     * @param packetType The type of the packet to create.
     * @return The created packet.
     * @since 3.8.0
     */
    protected PacketContainer createPacket(PacketType packetType) {
        return ProtocolLibrary.getProtocolManager().createPacket(packetType);
    }

}
