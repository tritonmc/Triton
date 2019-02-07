package com.rexcantor64.triton;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.commands.MainCMD;
import com.rexcantor64.triton.commands.TwinCMD;
import com.rexcantor64.triton.guiapi.Gui;
import com.rexcantor64.triton.guiapi.GuiButton;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.guiapi.ScrollableGui;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.listeners.BukkitListener;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.placeholderapi.TritonPlaceholderHook;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.wrappers.items.ItemStackParser;
import org.bukkit.Bukkit;

import java.io.File;

public class SpigotMLP extends Triton {

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
        loader.asSpigot().getCommand("triton").setExecutor(new MainCMD());
        loader.asSpigot().getCommand("twin").setExecutor(new TwinCMD());
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager = new GuiManager(), loader.asSpigot());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), loader.asSpigot());
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this));

        if (getConf().isBungeecord()) {
            loader.asSpigot().getServer().getMessenger().registerOutgoingPluginChannel(loader.asSpigot(), "triton:main");
            loader.asSpigot().getServer().getMessenger().registerIncomingPluginChannel(loader.asSpigot(), "triton:main", bridgeManager = new SpigotBridgeManager());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            new TritonPlaceholderHook(this).register();

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

    public void openLanguagesSelectionGUI(LanguagePlayer p) {
        if (!(p instanceof SpigotLanguagePlayer)) return;

        LanguageManager language = Triton.get().getLanguageManager();
        Language pLang = p.getLang();
        Gui gui = new ScrollableGui(Triton.get().getMessage("other.selector-gui-name", "&aSelect a language"));
        for (Language lang : language.getAllLanguages())
            gui.addButton(new GuiButton(ItemStackParser.bannerToItemStack(((com.rexcantor64.triton.language.Language) lang).getBanner(), pLang.equals(lang))).setListener(event -> {
                p.setLang(lang);
                ((SpigotLanguagePlayer) p).toBukkit().closeInventory();
                ((SpigotLanguagePlayer) p).toBukkit().sendMessage(Triton.get().getMessage("success.selector", "&aLanguage changed to %1", lang.getDisplayName()));
            }));
        gui.open(((SpigotLanguagePlayer) p).toBukkit());
    }
}
