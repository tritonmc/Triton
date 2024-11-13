package com.rexcantor64.triton.velocity.packetinterceptor;

import com.github.retrooper.packetevents.PacketEvents;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.packetinterceptor.PacketEventsManager;
import com.rexcantor64.triton.velocity.VelocityTriton;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import lombok.NonNull;
import lombok.val;

public class VelocityPacketEventsManager extends PacketEventsManager {
    @Override
    protected void initPacketEventsPlatform() {
        val loader = VelocityTriton.asVelocity().getLoader();
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(
                loader.getServer(),
                loader.getPluginContainer(),
                loader.getVelocityLogger(),
                loader.getDataDirectory()
        ));
    }

    @Override
    protected @NonNull Dependency getPlatformPacketEventsDependency() {
        return Dependency.PACKET_EVENTS_VELOCITY;
    }
}
