package com.rexcantor64.triton.spigot;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.spigot.banners.BannerBuilder;
import com.rexcantor64.triton.spigot.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.spigot.commands.handler.SpigotCommandHandler;
import com.rexcantor64.triton.spigot.guiapi.GuiButton;
import com.rexcantor64.triton.spigot.guiapi.GuiManager;
import com.rexcantor64.triton.spigot.guiapi.ScrollableGui;
import com.rexcantor64.triton.spigot.listeners.BukkitListener;
import com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction;
import com.rexcantor64.triton.spigot.packetinterceptor.MotdPacketHandler;
import com.rexcantor64.triton.spigot.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.spigot.placeholderapi.TritonPlaceholderHook;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.plugin.SpigotPlugin;
import com.rexcantor64.triton.spigot.wrappers.MaterialWrapperManager;
import com.rexcantor64.triton.terminal.Log4jInjector;
import com.rexcantor64.triton.utils.ReflectionUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class SpigotTriton extends Triton<SpigotLanguagePlayer, SpigotBridgeManager> {

    @Getter
    private final short mcVersion;
    @Getter
    private final short minorMcVersion;
    private ProtocolLibListener protocolLibListener;
    @Getter
    private MaterialWrapperManager wrapperManager;
    @Getter
    private SpigotCommandHandler commandHandler;
    @Getter
    private boolean papiEnabled = false;
    private int refreshTaskId = -1;
    private GuiManager guiManager;
    @Getter
    private final BannerBuilder bannerBuilder = new BannerBuilder();

    public SpigotTriton(PluginLoader loader) {
        super(new PlayerManager<>(SpigotLanguagePlayer::new), new SpigotBridgeManager());
        val versionSplit = Bukkit.getServer().getClass().getPackage().getName().split("_");
        mcVersion = Short.parseShort(versionSplit[1]);
        minorMcVersion = Short.parseShort(versionSplit[2].substring(1));
        super.loader = loader;
    }

    public SpigotPlugin getLoader() {
        return (SpigotPlugin) this.loader;
    }

    public static SpigotTriton asSpigot() {
        return (SpigotTriton) instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        Metrics metrics = new Metrics(getLoader(), 5606);
        metrics.addCustomChart(new SingleLineChart("active_placeholders",
                () -> Triton.get().getLanguageManager().getItemCount()));

        // Setup custom managers
        wrapperManager = new MaterialWrapperManager();

        // Setup commands
        this.commandHandler = new SpigotCommandHandler();
        registerTritonCommand().setExecutor(this.commandHandler);
        Objects.requireNonNull(getLoader().getCommand("twin")).setExecutor(this.commandHandler);
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager = new GuiManager(), getLoader());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), getLoader());
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (getConfig().isAsyncProtocolLib()) {
                val asyncManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
                asyncManager.registerAsyncHandler(protocolLibListener = new ProtocolLibListener(this, HandlerFunction.HandlerType.ASYNC)).start();
                asyncManager.registerAsyncHandler(new MotdPacketHandler()).start();
                ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibListener(this, HandlerFunction.HandlerType.SYNC));
            } else {
                ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this, HandlerFunction.HandlerType.ASYNC, HandlerFunction.HandlerType.SYNC));
                ProtocolLibrary.getProtocolManager().addPacketListener(new MotdPacketHandler());
            }
        } else {
            getLogger().logError("Could not setup packet interceptor because ProtocolLib is not available!");
        }

        if (getConfig().isBungeecord()) {
            val messenger = getLoader().getServer().getMessenger();
            messenger.registerOutgoingPluginChannel(getLoader(), "triton:main");
            messenger.registerIncomingPluginChannel(getLoader(), "triton:main", getBridgeManager());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TritonPlaceholderHook(this).register();
            papiEnabled = true;
        }

        if (getConfig().isTerminal()) {
            Log4jInjector.injectAppender();
        }
    }

    @SneakyThrows
    private PluginCommand registerTritonCommand() {
        val constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        val command = (PluginCommand) constructor.newInstance("triton", getLoader());

        command.setAliases(getConfig().getCommandAliases());
        command.setDescription("The main command of Triton.");

        val commandMap = (CommandMap) ReflectionUtils.getDeclaredField(Bukkit.getServer(), "commandMap");
        commandMap.register("triton", command);

        return command;
    }

    @Override
    public void reload() {
        super.reload();
        this.bannerBuilder.flushCache();
    }

    @Override
    protected void startConfigRefreshTask() {
        if (refreshTaskId != -1) Bukkit.getScheduler().cancelTask(refreshTaskId);
        if (getConfig().getConfigAutoRefresh() <= 0) return;
        refreshTaskId = Bukkit.getScheduler()
                .scheduleSyncDelayedTask(getLoader(), this::reload, getConfig().getConfigAutoRefresh() * 20L);
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

    @Override
    public void openLanguagesSelectionGUI(LanguagePlayer genericLanguagePlayer) {
        SpigotLanguagePlayer languagePlayer = (SpigotLanguagePlayer) genericLanguagePlayer;

        languagePlayer.toBukkit().ifPresent(player -> {
            val commandOverride = getConfig().getOpenSelectorCommandOverride();
            if (commandOverride != null && !commandOverride.isEmpty()) {
                player.performCommand(commandOverride);
                return;
            }

            val language = Triton.get().getLanguageManager();
            val pLang = languagePlayer.getLang();
            val gui = new ScrollableGui(Triton.get().getMessagesConfig().getMessage("other.selector-gui-name"));
            for (val lang : language.getAllLanguages()) {
                val isLanguageActive = pLang.equals(lang);
                val languageItem = this.getBannerBuilder().fromLanguage(lang, isLanguageActive);
                gui.addButton(new GuiButton(languageItem).setListener(event -> {
                    languagePlayer.setLang(lang);
                    player.closeInventory();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                            .getMessage("success.selector", lang.getDisplayName())));
                }));
            }
            gui.open(player);
        });
    }

    @Override
    public String getVersion() {
        return getLoader().getDescription().getVersion();
    }

    @Override
    protected String getConfigFileName() {
        return "config";
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(getLoader(), runnable);
    }

    public <T> Optional<T> callSync(Callable<T> callable) {
        try {
            if (Bukkit.getServer().isPrimaryThread()) {
                return Optional.ofNullable(callable.call());
            }
            return Optional.ofNullable(Bukkit.getScheduler().callSyncMethod(getLoader(), callable).get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
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
