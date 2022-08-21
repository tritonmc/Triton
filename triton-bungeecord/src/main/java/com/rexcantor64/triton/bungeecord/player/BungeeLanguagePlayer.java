package com.rexcantor64.triton.bungeecord.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageBungeeEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.packetinterceptor.BungeeListener;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.utils.SocketUtils;
import lombok.val;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.protocol.packet.Chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BungeeLanguagePlayer implements LanguagePlayer {

    private final UUID uuid;
    private Connection currentConnection;
    private ProxiedPlayer parent;

    private Language language;
    private BungeeListener listener;

    private String lastTabHeader;
    private String lastTabFooter;
    private final HashMap<UUID, String> bossBars = new HashMap<>();
    private boolean waitingForClientLocale = false;

    public BungeeLanguagePlayer(UUID parent) {
        this.uuid = parent;
        this.parent = BungeeCord.getInstance().getPlayer(parent);
        this.currentConnection = this.parent;
        BungeeCord.getInstance().getScheduler().runAsync(BungeeTriton.asBungee().getLoader(), this::load);
    }

    public BungeeLanguagePlayer(UUID uuid, Connection connection) {
        this.uuid = uuid;
        this.currentConnection = connection;
        load();
    }

    public void setBossbar(UUID uuid, String lastBossBar) {
        bossBars.put(uuid, lastBossBar);
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
    }

    public void setLastTabHeader(String lastTabHeader) {
        this.lastTabHeader = lastTabHeader;
    }

    public void setLastTabFooter(String lastTabFooter) {
        this.lastTabFooter = lastTabFooter;
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
        if (this.waitingForClientLocale && getParent() != null)
            parent.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                    .getMessage("success.detected-language", language.getDisplayName()))));
        this.language = event.getNewLanguage();
        this.waitingForClientLocale = false;

        if (sendToSpigot && getParent() != null)
            BungeeTriton.asBungee().getBridgeManager().sendPlayerLanguage(this);

        save();
        refreshAll();
        executeCommands(null);
    }

    public void refreshAll() {
        if (listener == null) return;
        listener.refreshTab();
        if (Triton.get().getConfig().isTab() && lastTabHeader != null && lastTabFooter != null)
            listener.refreshTabHeaderFooter(lastTabHeader, lastTabFooter);
        if (Triton.get().getConfig().isBossbars())
            for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                listener.refreshBossbar(entry.getKey(), entry.getValue());
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
        BungeeCord.getInstance().getScheduler().runAsync(BungeeTriton.asBungee().getLoader(), () -> {
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
            } else if (cmd.getType() == ExecutableCommand.Type.BUNGEE) {
                BungeeCord.getInstance().getPluginManager().dispatchCommand(BungeeCord.getInstance().getConsole(),
                        cmdText);
            } else if (cmd.getType() == ExecutableCommand.Type.BUNGEE_PLAYER) {
                BungeeCord.getInstance().getPluginManager().dispatchCommand(parent, cmdText);
            }
        }
    }

    @Override
    public String toString() {
        return "BungeeLanguagePlayer{" +
                "uuid=" + uuid +
                ", language=" + (language == null ? "null" : language.getName()) +
                '}';
    }
}
