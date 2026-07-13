package com.rexcantor64.triton.test;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import io.github.retrooper.packetevents.impl.netty.NettyManagerImpl;

public class MockPacketEventsAPI extends PacketEventsAPI<Void> {

    private static final ServerManager SERVER_MANAGER = () -> ServerVersion.V_26_2;
    private static final NettyManager NETTY_MANAGER = new NettyManagerImpl();

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void init() {

    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public Void getPlugin() {
        return null;
    }

    @Override
    public ServerManager getServerManager() {
        return SERVER_MANAGER;
    }

    @Override
    public ProtocolManager getProtocolManager() {
        return null;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return null;
    }

    @Override
    public NettyManager getNettyManager() {
        return NETTY_MANAGER;
    }

    @Override
    public ChannelInjector getInjector() {
        return null;
    }
}
