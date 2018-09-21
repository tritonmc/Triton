package com.rexcantor64.triton;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.commands.MainCMD;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.listeners.BukkitListener;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.plugin.PluginLoader;
import org.bukkit.Bukkit;

import java.io.File;

public class SpigotMLP extends MultiLanguagePlugin {

    private ProtocolLibListener protocolLibListener;
    private SpigotBridgeManager bridgeManager;

    public SpigotMLP(PluginLoader loader) {
        super.loader = loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();
        // Setup commands
        loader.asSpigot().getCommand("multilanguageplugin").setExecutor(new MainCMD());
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager = new GuiManager(), loader.asSpigot());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), loader.asSpigot());
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this));

        if (getConf().isBungeecord()) {
            loader.asSpigot().getServer().getMessenger().registerOutgoingPluginChannel(loader.asSpigot(), "MultiLanguagePlugin");
            loader.asSpigot().getServer().getMessenger().registerIncomingPluginChannel(loader.asSpigot(), "MultiLanguagePlugin", bridgeManager = new SpigotBridgeManager());
        }
    }

    public ProtocolLibListener getProtocolLibListener() {
        return protocolLibListener;
    }

    public File getDataFolder() {
        return loader.asSpigot().getDataFolder();
    }

    public SpigotBridgeManager getBridgeManager() {
        return bridgeManager;
    }
}
