package com.rexcantor64.triton.spigot.packetinterceptor;

import com.github.retrooper.packetevents.PacketEvents;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.packetinterceptor.PacketEventsManager;
import com.rexcantor64.triton.spigot.SpigotTriton;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.NonNull;
import lombok.val;

public class SpigotPacketEventsManager extends PacketEventsManager {
    @Override
    protected void initPacketEventsPlatform() {
        val loader = SpigotTriton.asSpigot().getLoader();
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(loader.getPlugin()));
    }

    @Override
    protected @NonNull Dependency getPlatformPacketEventsDependency() {
        return Dependency.PACKET_EVENTS_SPIGOT;
    }
}
