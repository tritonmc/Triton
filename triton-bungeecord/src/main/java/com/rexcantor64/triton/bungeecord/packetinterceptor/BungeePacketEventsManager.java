package com.rexcantor64.triton.bungeecord.packetinterceptor;

import com.github.retrooper.packetevents.PacketEvents;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.packetinterceptor.PacketEventsManager;
import io.github.retrooper.packetevents.bungee.factory.BungeePacketEventsBuilder;
import lombok.NonNull;
import lombok.val;

public class BungeePacketEventsManager extends PacketEventsManager {
    @Override
    protected void initPacketEventsPlatform() {
        val loader = BungeeTriton.asBungee().getLoader();
        PacketEvents.setAPI(BungeePacketEventsBuilder.build(loader.getPlugin()));
    }

    @Override
    protected @NonNull Dependency getPlatformPacketEventsDependency() {
        return Dependency.PACKET_EVENTS_BUNGEE;
    }
}
