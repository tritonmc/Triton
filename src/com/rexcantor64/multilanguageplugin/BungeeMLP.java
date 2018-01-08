package com.rexcantor64.multilanguageplugin;

import com.rexcantor64.multilanguageplugin.bridge.BungeeBridgeManager;
import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.multilanguageplugin.plugin.PluginLoader;
import net.md_5.bungee.BungeeCord;

import java.io.File;

public class BungeeMLP extends MultiLanguagePlugin {

    public BungeeMLP(PluginLoader loader) {
        super.loader = loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        BungeeCord.getInstance().getPluginManager().registerListener(loader.asBungee(), new BungeeBridgeManager());
        BungeeCord.getInstance().registerChannel("MultiLanguagePlugin");
    }

    public ProtocolLibListener getProtocolLibListener() {
        return null;
    }

    public File getDataFolder() {
        return loader.asBungee().getDataFolder();
    }

}
