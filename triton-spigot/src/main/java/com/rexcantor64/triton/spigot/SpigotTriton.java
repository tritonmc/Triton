package com.rexcantor64.triton.spigot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import com.rexcantor64.triton.spigot.packetinterceptor.ProtocolLibManager;
import com.rexcantor64.triton.spigot.packetinterceptor.ProtocolLibRefresher;
import com.rexcantor64.triton.spigot.packetinterceptor.SpigotPacketEventsManager;
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
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class SpigotTriton extends Triton<SpigotLanguagePlayer, SpigotBridgeManager> {

    @Getter
    private @Nullable ProtocolLibRefresher protocolLibRefresher;
    @Getter
    private MaterialWrapperManager wrapperManager;
    @Getter
    private SpigotCommandHandler commandHandler;
    @Getter
    private boolean papiEnabled = false;
    private int refreshTaskId = -1;
    @Getter
    private GuiManager guiManager;
    @Getter
    private final BannerBuilder bannerBuilder = new BannerBuilder();

    public SpigotTriton(PluginLoader loader) {
        super(new PlayerManager<>(SpigotLanguagePlayer::new), new SpigotBridgeManager());
        super.loader = loader;
    }

    public SpigotPlugin getLoader() {
        return (SpigotPlugin) this.loader;
    }

    public JavaPlugin getJavaPlugin() {
        return this.getLoader().getPlugin();
    }

    public static SpigotTriton asSpigot() {
        return (SpigotTriton) instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (!this.getConfig().isUsePacketEvents()) {
            if (!ProtocolLibManager.isProtocolLibAvailable()) {
                getLogger().logError("Shutting down...");
                Bukkit.getPluginManager().disablePlugin(getJavaPlugin());
                return;
            }
            this.protocolLibRefresher = ProtocolLibManager.registerProtocolLibListeners();
        }

        Metrics metrics = new Metrics(getJavaPlugin(), 5606);
        metrics.addCustomChart(new SingleLineChart("active_placeholders",
                () -> this.getTranslationManager().getTranslationCount()));

        // Setup custom managers
        wrapperManager = new MaterialWrapperManager();

        // Setup commands
        this.commandHandler = new SpigotCommandHandler();
        registerTritonCommand().setExecutor(this.commandHandler);
        Objects.requireNonNull(getJavaPlugin().getCommand("twin")).setExecutor(this.commandHandler);
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager = new GuiManager(), getJavaPlugin());
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), getJavaPlugin());

        if (getConfig().isBungeecord()) {
            if (!isSpigotProxyMode() && !isPaperProxyMode()) {
                getLogger().logError("DANGER! DANGER! DANGER!");
                getLogger().logError("Proxy mode is enabled on Triton but disabled on Spigot!");
                getLogger().logError("A malicious player can run ANY command as the server.");
                getLogger().logError("DANGER! DANGER! DANGER!");
            }

            val messenger = getJavaPlugin().getServer().getMessenger();
            messenger.registerOutgoingPluginChannel(getJavaPlugin(), "triton:main");
            messenger.registerIncomingPluginChannel(getJavaPlugin(), "triton:main", getBridgeManager());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TritonPlaceholderHook(this, false).register();
            new TritonPlaceholderHook(this, true).register();
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
        val command = (PluginCommand) constructor.newInstance("triton", getJavaPlugin());

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
    protected void initPacketEventsManager() {
        this.packetEventsManager = new SpigotPacketEventsManager();
    }

    @Override
    protected void startConfigRefreshTask() {
        if (refreshTaskId != -1) Bukkit.getScheduler().cancelTask(refreshTaskId);
        if (getConfig().getConfigAutoRefresh() <= 0) return;
        refreshTaskId = Bukkit.getScheduler()
                .scheduleSyncDelayedTask(getJavaPlugin(), this::reload, getConfig().getConfigAutoRefresh() * 20L);
    }

    public File getDataFolder() {
        return getJavaPlugin().getDataFolder();
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
        return getJavaPlugin().getDescription().getVersion();
    }

    @Override
    protected String getConfigFileName() {
        return "config_spigot";
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(getJavaPlugin(), runnable);
    }

    public <T> Optional<T> callSync(Callable<T> callable) {
        try {
            if (Bukkit.getServer().isPrimaryThread()) {
                return Optional.ofNullable(callable.call());
            }
            return Optional.ofNullable(Bukkit.getScheduler().callSyncMethod(getJavaPlugin(), callable).get());
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
            Class<?> spigotConfigClass = ReflectionUtils.getClass("org.spigotmc.SpigotConfig");
            if (spigotConfigClass == null) {
                return false;
            }

            Object bungeeEnabled = ReflectionUtils.getStaticField(spigotConfigClass, "bungee");
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

    private Optional<String> getProtocolLibVersion() {
        return getPluginVersion("ProtocolLib");
    }

    private Optional<String> getPacketEventsVersion() {
        return getPluginVersion("packetevents");
    }

    private Optional<String> getPluginVersion(String name) {
        return Optional.ofNullable(Bukkit.getPluginManager().getPlugin(name))
                .map(Plugin::getDescription)
                .map(PluginDescriptionFile::getVersion);
    }

    @Override
    public @NotNull JsonElement getPlatformDebugInfo() {
        val obj = new JsonObject();
        obj.addProperty("serverName", Bukkit.getName());
        obj.addProperty("serverVersion", Bukkit.getVersion());
        getProtocolLibVersion().ifPresent(version -> obj.addProperty("protocolLibVersion", version));
        getPacketEventsVersion().ifPresent(version -> obj.addProperty("packetEventsVersion", version));
        return obj;
    }
}
