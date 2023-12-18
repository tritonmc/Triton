package com.rexcantor64.triton;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.commands.handler.SpigotCommandHandler;
import com.rexcantor64.triton.guiapi.GuiButton;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.guiapi.ScrollableGui;
import com.rexcantor64.triton.listeners.BukkitListener;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.packetinterceptor.protocollib.HandlerFunction;
import com.rexcantor64.triton.packetinterceptor.protocollib.MotdPacketHandler;
import com.rexcantor64.triton.placeholderapi.TritonPlaceholderHook;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.plugin.SpigotPlugin;
import com.rexcantor64.triton.terminal.Log4jInjector;
import com.rexcantor64.triton.utils.NMSUtils;
import com.rexcantor64.triton.wrappers.MaterialWrapperManager;
import com.rexcantor64.triton.wrappers.items.ItemStackParser;
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
    @Getter
    private boolean papiEnabled = false;
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

        if (!this.isProtocolLibAvailable()) {
            getLogger().logError("Shutting down...");
            Bukkit.getPluginManager().disablePlugin(getLoader());
            return;
        }

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

        // Setup ProtocolLib
        if (getConfig().isAsyncProtocolLib()) {
            val asyncManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
            asyncManager.registerAsyncHandler(protocolLibListener = new ProtocolLibListener(this, HandlerFunction.HandlerType.ASYNC)).start();
            asyncManager.registerAsyncHandler(new MotdPacketHandler()).start();
            ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibListener(this, HandlerFunction.HandlerType.SYNC));
        } else {
            ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener = new ProtocolLibListener(this, HandlerFunction.HandlerType.ASYNC, HandlerFunction.HandlerType.SYNC));
            ProtocolLibrary.getProtocolManager().addPacketListener(new MotdPacketHandler());
        }

        if (getConf().isBungeecord()) {
            if (!isSpigotProxyMode() && !isPaperProxyMode()) {
                getLogger().logError("DANGER! DANGER! DANGER!");
                getLogger().logError("Proxy mode is enabled on Triton but disabled on Spigot!");
                getLogger().logError("A malicious player can run ANY command as the server.");
                getLogger().logError("DANGER! DANGER! DANGER!");
            }

            getLoader().getServer().getMessenger().registerOutgoingPluginChannel(getLoader(), "triton" +
                    ":main");
            getLoader().getServer().getMessenger().registerIncomingPluginChannel(getLoader(), "triton" +
                    ":main", bridgeManager = new SpigotBridgeManager());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TritonPlaceholderHook(this).register();
            papiEnabled = true;
        }

        if (getConf().isTerminal())
            Log4jInjector.injectAppender();
    }

    @SneakyThrows
    private PluginCommand registerTritonCommand() {
        val constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        val command = (PluginCommand) constructor.newInstance("triton", getLoader());

        command.setAliases(getConf().getCommandAliases());
        command.setDescription("The main command of Triton.");

        val commandMap = (CommandMap) NMSUtils.getDeclaredField(Bukkit.getServer(), "commandMap");
        commandMap.register("triton", command);

        return command;
    }

    @Override
    protected void startConfigRefreshTask() {
        if (refreshTaskId != -1) Bukkit.getScheduler().cancelTask(refreshTaskId);
        if (getConf().getConfigAutoRefresh() <= 0) return;
        refreshTaskId = Bukkit.getScheduler()
                .scheduleSyncDelayedTask(getLoader(), this::reload, getConf().getConfigAutoRefresh() * 20L);
    }

    /**
     * Checks if ProtocolLib is enabled and if its version matches
     * the expected version.
     * Triton requires ProtocolLib 5.2.0 or later.
     *
     * @return Whether the plugin should continue loading
     * @since 3.8.2
     */
    private boolean isProtocolLibAvailable() {
        val protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (!Bukkit.getPluginManager().isPluginEnabled(protocolLib)) {
            getLogger().logError("ProtocolLib IS REQUIRED! Without ProtocolLib, Triton will not translate any messages.");
            getLogger().logError("For the plugin to work correctly, please download the latest version of ProtocolLib.");
            return false;
        }

        if (getConfig().isIKnowWhatIAmDoing()) {
            return true;
        }

        try {
            MinecraftVersion ignore = MinecraftVersion.v1_20_4;
        } catch (NoSuchFieldError ignore) {
            // Triton requires ProtocolLib 5.2.0 or later
            getLogger().logError("ProtocolLib 5.2.0 or later is required! Older versions of ProtocolLib will only partially work or not work at all, and are therefore not recommended.");
            getLogger().logError("If you want to enable the plugin anyway, add `i-know-what-i-am-doing: true` to Triton's config.yml.");
            return false;
        }

        return true;
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
        slp.toBukkit().ifPresent(player -> {
            val commandOverride = getConfig().getOpenSelectorCommandOverride();
            if (commandOverride != null && !commandOverride.isEmpty()) {
                player.performCommand(commandOverride);
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
                    player.closeInventory();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                            .getMessage("success.selector", lang.getDisplayName())));
                }));
            gui.open(player);
        });
    }

    @Override
    public String getVersion() {
        return getLoader().getDescription().getVersion();
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

    /**
     * Use reflection to check if this Spigot server has "bungeecord" mode enabled on spigot.yml.
     * This is used to show a warning if Spigot is in proxy mode, but the server is not.
     *
     * @return Whether this Spigot server has bungeecord enabled on spigot.yml.
     */
    public boolean isSpigotProxyMode() {
        try {
            Class<?> spigotConfigClass = NMSUtils.getClass("org.spigotmc.SpigotConfig");
            if (spigotConfigClass == null) {
                return false;
            }

            Object bungeeEnabled = NMSUtils.getStaticField(spigotConfigClass, "bungee");
            if (bungeeEnabled == null) {
                return false;
            }
            return (boolean) bungeeEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Use reflection to check if this Paper server has velocity modern forwarding enabled on paper-global.yml.
     * This is used to show a warning if Paper is in proxy mode, but the server is not.
     *
     * @return Whether this Spigot server has velocity forwarding enabled on paper-global.yml.
     */
    public boolean isPaperProxyMode() {
        try {
            Class<?> paperConfigClass = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");

            Object instance = paperConfigClass.getMethod("get").invoke(null);
            Object proxies = instance.getClass().getField("proxies").get(instance);
            Object velocity = proxies.getClass().getField("velocity").get(proxies);
            Object velocityEnabled = velocity.getClass().getField("enabled").get(velocity);
            if (velocityEnabled == null) {
                return false;
            }
            return (boolean) velocityEnabled;
        } catch (Exception e) {
            return false;
        }
    }
}
