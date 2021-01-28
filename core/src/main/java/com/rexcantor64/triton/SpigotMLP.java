package com.rexcantor64.triton;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.commands.handler.SpigotCommandHandler;
import com.rexcantor64.triton.guiapi.GuiButton;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.guiapi.ScrollableGui;
import com.rexcantor64.triton.listeners.BukkitListener;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.placeholderapi.TritonPlaceholderHook;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.plugin.SpigotPlugin;
import com.rexcantor64.triton.terminal.Log4jInjector;
import com.rexcantor64.triton.wrappers.MaterialWrapperManager;
import com.rexcantor64.triton.wrappers.items.ItemStackParser;
import lombok.Getter;
import lombok.val;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public class SpigotMLP extends Triton {

    @Getter
    private final short mcVersion;
    @Getter
    private final short minorMcVersion;
    private ProtocolLibListener protocolLibListener;
    private SpigotBridgeManager bridgeManager;
    @Getter
    private MaterialWrapperManager wrapperManager;
    @Getter
    private SpigotCommandHandler commandHandler;
    private int refreshTaskId = -1;

    public SpigotMLP(PluginLoader loader) {
        val versionSplit = Bukkit.getServer().getClass().getPackage().getName().split("_");
        mcVersion = Short.parseShort(versionSplit[1]);
        minorMcVersion = Short.parseShort(versionSplit[2].substring(1));
        super.loader = loader;
    }

    public SpigotPlugin getLoader() {
        return (SpigotPlugin) this.loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        Metrics metrics = new Metrics(getLoader(), 5606);
        metrics.addCustomChart(new Metrics.SingleLineChart("active_placeholders",
                () -> Triton.get().getLanguageManager().getItemCount()));

        // Setup custom managers
        wrapperManager = new MaterialWrapperManager();

        // Setup commands
        this.commandHandler = new SpigotCommandHandler();
        val tritonCommand = Objects.requireNonNull(getLoader().getCommand("triton"));
        tritonCommand.setAliases(getConfig().getCommandAliases());
        tritonCommand.setExecutor(this.commandHandler);
        Objects.requireNonNull(getLoader().getCommand("twin")).setExecutor(this.commandHandler);
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager = new GuiManager(), getLoader());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), getLoader());
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this));

        if (getConf().isBungeecord()) {
            getLoader().getServer().getMessenger().registerOutgoingPluginChannel(getLoader(), "triton" +
                    ":main");
            getLoader().getServer().getMessenger().registerIncomingPluginChannel(getLoader(), "triton" +
                    ":main", bridgeManager = new SpigotBridgeManager());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            new TritonPlaceholderHook(this).register();

        if (getConf().isTerminal())
            Log4jInjector.injectAppender();
    }

    @Override
    protected void startConfigRefreshTask() {
        if (refreshTaskId != -1) Bukkit.getScheduler().cancelTask(refreshTaskId);
        if (getConf().getConfigAutoRefresh() <= 0) return;
        refreshTaskId = Bukkit.getScheduler()
                .scheduleSyncDelayedTask(getLoader(), this::reload, getConf().getConfigAutoRefresh() * 20L);
    }

    public ProtocolLibListener getProtocolLibListener() {
        return protocolLibListener;
    }

    public File getDataFolder() {
        return getLoader().getDataFolder();
    }

    public SpigotBridgeManager getBridgeManager() {
        return bridgeManager;
    }

    public void openLanguagesSelectionGUI(LanguagePlayer p) {
        if (!(p instanceof SpigotLanguagePlayer)) return;

        val slp = (SpigotLanguagePlayer) p;

        val commandOverride = getConfig().getOpenSelectorCommandOverride();
        if (commandOverride != null && !commandOverride.isEmpty()) {
            slp.toBukkit().performCommand(commandOverride);
            return;
        }

        val language = Triton.get().getLanguageManager();
        val pLang = p.getLang();
        val gui = new ScrollableGui(Triton.get().getMessagesConfig().getMessage("other.selector-gui-name"));
        for (val lang : language.getAllLanguages())
            gui.addButton(new GuiButton(ItemStackParser
                    .bannerToItemStack(
                            ((com.rexcantor64.triton.language.Language) lang).getBanner(),
                            pLang.equals(lang)
                    )).setListener(event -> {
                p.setLang(lang);
                slp.toBukkit().closeInventory();
                slp.toBukkit().sendMessage(Triton.get().getMessagesConfig()
                        .getMessage("success.selector", lang.getDisplayName()));
            }));
        gui.open(slp.toBukkit());
    }

    @Override
    public String getVersion() {
        return getLoader().getDescription().getVersion();
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(getLoader(), runnable);
    }

    @Override
    public UUID getPlayerUUIDFromString(String input) {
        val player = Bukkit.getPlayer(input);
        if (player != null) return player.getUniqueId();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
