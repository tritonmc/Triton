package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;

import java.util.Map;
import java.util.function.BiConsumer;

public abstract class PacketHandler {

    public abstract void registerPacketTypes(Map<PacketType, BiConsumer<PacketEvent, SpigotLanguagePlayer>> registry);

    protected SpigotMLP getMain() {
        return Triton.asSpigot();
    }

    protected TritonLogger logger() {
        return getMain().getLogger();
    }

    protected LanguageManager getLanguageManager() {
        return getMain().getLanguageManager();
    }

    protected LanguageParser getLanguageParser() {
        return getMain().getLanguageParser();
    }

    protected int getMcVersion() {
        return Triton.get().getMcVersion();
    }

}
