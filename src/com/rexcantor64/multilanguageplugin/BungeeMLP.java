package com.rexcantor64.multilanguageplugin;

import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;

import java.io.File;

public class BungeeMLP extends MultiLanguagePlugin {

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();
    }

    public ProtocolLibListener getProtocolLibListener() {
        return null;
    }

    public File getDataFolder() {
        return loader.asBungee().getDataFolder();
    }

}
