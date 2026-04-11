package com.rexcantor64.triton.bungeecord.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageBungeeEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.packetinterceptor.BungeeListener;
import com.rexcantor64.triton.bungeecord.utils.BaseComponentUtils;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.utils.SocketUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.protocol.data.NumberFormat;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ScoreboardObjective.HealthDisplay;
import net.md_5.bungee.protocol.packet.Team;
import net.md_5.bungee.protocol.util.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeLanguagePlayer extends TritonLanguagePlayer<ProxiedPlayer> {

    private final UUID uuid;
    private Connection currentConnection;
    private ProxiedPlayer parent;

    private Language language;
    private BungeeListener listener;

    private BaseComponent lastTabHeader;
    private BaseComponent lastTabFooter;
    private final HashMap<UUID, BaseComponent> bossBars = new HashMap<>();
    private boolean waitingForClientLocale = false;

    @Getter
    private Map<String, ScoreboardObjective> objectivesMap = new ConcurrentHashMap<>();
    @Getter
    private Map<String, ScoreboardTeam> teamsMap = new ConcurrentHashMap<>();

    public BungeeLanguagePlayer(UUID parent) {
        super();
        this.uuid = parent;
        this.parent = BungeeCord.getInstance().getPlayer(parent);
        this.currentConnection = this.parent;
        BungeeCord.getInstance().getScheduler().runAsync(BungeeTriton.asBungee().getPlugin(), this::load);
    }

    public BungeeLanguagePlayer(UUID uuid, Connection connection) {
        this.uuid = uuid;
        this.currentConnection = connection;
        load();
    }

    @Override
    public @NotNull Optional<ProxiedPlayer> getPlatformPlayer() {
        return Optional.of(this.parent);
    }

    public void setBossbar(UUID uuid, BaseComponent lastBossBar) {
        bossBars.put(uuid, lastBossBar.duplicate());
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
    }

    public void clearCachedBossbars() {
        bossBars.clear();
    }

    public void setLastTabHeader(BaseComponent lastTabHeader) {
        this.lastTabHeader = lastTabHeader.duplicate();
    }

    public void setLastTabFooter(BaseComponent lastTabFooter) {
        this.lastTabFooter = lastTabFooter.duplicate();
    }

    public void setScoreboardObjective(String name, BaseComponent displayName, HealthDisplay type, @Nullable NumberFormat numberFormat) {
        ScoreboardObjective objective = this.objectivesMap.computeIfAbsent(name, k -> new ScoreboardObjective());
        objective.setDisplayName(displayName);
        objective.setType(type);
        objective.setNumberFormat(numberFormat);
    }

    public void removeScoreboardObjective(String name) {
        this.objectivesMap.remove(name);
    }

    public void setScoreboardTeam(String name, ScoreboardTeam team) {
        this.teamsMap.put(name, team);
    }

    public void removeScoreboardTeam(String name) {
        this.teamsMap.remove(name);
    }

    @Override
    public boolean isWaitingForClientLocale() {
        return waitingForClientLocale;
    }

    @Override
    public void waitForClientLocale() {
        this.waitingForClientLocale = true;
    }

    public Language getLang() {
        if (language == null)
            language = Triton.get().getLanguageManager().getMainLanguage();
        return language;
    }

    public void setLang(Language language) {
        setLang(language, true);
    }

    public void setLang(Language language, boolean sendToSpigot) {
        PlayerChangeLanguageBungeeEvent event = new PlayerChangeLanguageBungeeEvent(this, this.language, language);
        BungeeCord.getInstance().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        this.language = event.getNewLanguage();
        if (this.waitingForClientLocale && getParent() != null)
            parent.sendMessage(BaseComponentUtils.serialize(Triton.get().getMessagesConfig()
                    .getMessageComponent("success.detected-language", language.getDisplayName())));
        this.waitingForClientLocale = false;

        if (sendToSpigot && getParent() != null)
            BungeeTriton.asBungee().getBridgeManager().sendPlayerLanguage(this);

        save();
        refreshAll();
        executeCommands(null);
    }

    public void refreshAll() {
        super.refreshAll();
        if (listener == null) return;
        listener.refreshTab();
        if (Triton.get().getConfig().isTab() && lastTabHeader != null && lastTabFooter != null)
            listener.refreshTabHeaderFooter(lastTabHeader, lastTabFooter);
        if (Triton.get().getConfig().isBossbars())
            for (Map.Entry<UUID, BaseComponent> entry : bossBars.entrySet())
                listener.refreshBossbar(entry.getKey(), entry.getValue());
        if (Triton.get().getConfig().isScoreboards()) {
            for (Map.Entry<String, ScoreboardObjective> entry : objectivesMap.entrySet()) {
                listener.refreshScoreboardObjective(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, ScoreboardTeam> entry : teamsMap.entrySet()) {
                listener.refreshScoreboardTeam(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public ProxiedPlayer getParent() {
        if (parent == null) {
            this.parent = BungeeCord.getInstance().getPlayer(this.uuid);
            if (this.parent != null)
                this.currentConnection = this.parent;
        }
        return parent;
    }

    public Connection getCurrentConnection() {
        return currentConnection;
    }

    private void load() {
        language = Triton.get().getStorage().getLanguage(this);
        if (currentConnection != null)
            Triton.get().getStorage()
                    .setLanguage(null, SocketUtils.getIpAddress(currentConnection.getSocketAddress()), language);
    }

    private void save() {
        BungeeCord.getInstance().getScheduler().runAsync(BungeeTriton.asBungee().getPlugin(), () -> {
            String ip = null;
            if (getParent() != null)
                ip = SocketUtils.getIpAddress(parent.getSocketAddress());
            Triton.get().getStorage().setLanguage(uuid, ip, language);
        });
    }

    public void setListener(BungeeListener listener) {
        this.listener = listener;
    }

    public void executeCommands(Server overrideServer) {
        Triton.get().getLogger().logTrace("Executing language commands for player %1", this);
        val parent = getParent();
        if (parent == null) return;
        val server = overrideServer == null ? parent.getServer() : overrideServer;
        for (val cmd : ((com.rexcantor64.triton.language.Language) language).getCmds()) {
            val cmdText = cmd.getCmd().replace("%player%", parent.getName()).replace("%uuid%", uuid.toString());

            Triton.get().getLogger().logTrace("-- Command[TYPE=%2]: %1", cmdText, cmd.getType());

            if (!cmd.isUniversal() && !cmd.getServers().contains(server.getInfo().getName())) continue;

            if (cmd.getType() == ExecutableCommand.Type.SERVER) {
                BungeeTriton.asBungee().getBridgeManager().sendExecutableCommand(cmdText, server);
            } else if (cmd.getType() == ExecutableCommand.Type.PLAYER) {
                server.unsafe().sendPacket(new Chat("/" + cmdText));
            } else if (cmd.getType() == ExecutableCommand.Type.PROXY) {
                BungeeCord.getInstance().getPluginManager().dispatchCommand(BungeeCord.getInstance().getConsole(),
                        cmdText);
            } else if (cmd.getType() == ExecutableCommand.Type.PROXY_PLAYER) {
                BungeeCord.getInstance().getPluginManager().dispatchCommand(parent, cmdText);
            }
        }
    }

    @Data
    public static class ScoreboardObjective {
        private BaseComponent displayName;
        private HealthDisplay type;
        @Nullable
        private NumberFormat numberFormat;
    }

    @Data
    @AllArgsConstructor
    public static class ScoreboardTeam {
        private BaseComponent displayName;
        private BaseComponent prefix;
        private BaseComponent suffix;

        // other data (has to be saved for refreshing packet)
        private Either<String, Team.NameTagVisibility> nameTagVisibility;
        private Either<String, Team.CollisionRule> collisionRule;
        private int color;
        private byte options;
    }

    @Override
    public String toString() {
        return "BungeeLanguagePlayer{" +
                "uuid=" + uuid +
                ", language=" + (language == null ? "null" : language.getName()) +
                '}';
    }
}
