package com.rexcantor64.triton;

import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.plugin.VelocityPlugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.val;

import java.io.File;
import java.util.UUID;

public class VelocityMLP extends Triton {

    public VelocityMLP(PluginLoader loader) {
        super.loader = loader;
    }

    public VelocityPlugin getLoader() {
        return (VelocityPlugin) this.loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();
    }

    @Override
    public void reload() {
        super.reload();
    }

    @Override
    protected void startConfigRefreshTask() {
    }


    public File getDataFolder() {
        return getLoader().getDataDirectory().toFile();
    }

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public void runAsync(Runnable runnable) {
        getLoader().getServer().getScheduler().buildTask(getLoader(), runnable).schedule();
    }

    public ProxyServer getVelocity() {
        return getLoader().getServer();
    }

    @Override
    public UUID getPlayerUUIDFromString(String input) {
        val player = getVelocity().getPlayer(input);
        if (player.isPresent()) return player.get().getUniqueId();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public short getMcVersion() {
        return 0;
    }

    @Override
    public short getMinorMcVersion() {
        return 0;
    }
}
