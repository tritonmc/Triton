package com.rexcantor64.multilanguageplugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.multilanguageplugin.commands.MainCMD;
import com.rexcantor64.multilanguageplugin.listeners.BukkitListener;
import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.multilanguageplugin.plugin.PluginLoader;
import org.bukkit.Bukkit;

import java.io.File;

public class SpigotMLP extends MultiLanguagePlugin {

    private ProtocolLibListener protocolLibListener;

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
        Bukkit.getPluginManager().registerEvents(guiManager, loader.asSpigot());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), loader.asSpigot());
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this));
    }

    public ProtocolLibListener getProtocolLibListener() {
        return protocolLibListener;
    }

    public File getDataFolder() {
        return loader.asSpigot().getDataFolder();
    }
}
