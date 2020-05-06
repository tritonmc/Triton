package com.rexcantor64.triton.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageBungeeEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.packetinterceptor.BungeeListener;
import net.md_5.bungee.BungeeCord;
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
    private HashMap<UUID, String> bossBars = new HashMap<>();
    private boolean waitingForClientLocale = false;

    public BungeeLanguagePlayer(UUID parent) {
        this.uuid = parent;
        this.parent = BungeeCord.getInstance().getPlayer(parent);
        this.currentConnection = this.parent;
        BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), this::load);
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
        PlayerChangeLanguageBungeeEvent event = new PlayerChangeLanguageBungeeEvent(this, this.language, language);
        BungeeCord.getInstance().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (this.waitingForClientLocale && getParent() != null)
            parent.sendMessage(TextComponent.fromLegacyText(Triton.get().getMessage("success.detected-language",
                    "&aYour language has been automatically set to %1", language.getDisplayName())));
        this.language = event.getNewLanguage();
        this.waitingForClientLocale = false;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(uuid.toString());
        out.writeUTF(language.getName());
        if (getParent() != null)
            getParent().getServer().sendData("triton:main", out.toByteArray());
        save();
        refreshAll();
        executeCommands(null);
    }

    public void refreshAll() {
        if (listener == null) return;
        listener.refreshTab();
        if (Triton.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
            listener.refreshTabHeaderFooter(lastTabHeader, lastTabFooter);
        if (Triton.get().getConf().isBossbars())
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
        language = Triton.get().getPlayerStorage().getLanguage(this);
        if (currentConnection != null)
            Triton.get().getPlayerStorage()
                    .setLanguage(null, currentConnection.getAddress().getAddress().getHostAddress(), language);
    }

    private void save() {
        BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), () -> {
            String ip = null;
            if (parent != null)
                ip = parent.getAddress().getAddress().getHostAddress();
            Triton.get().getPlayerStorage().setLanguage(uuid, ip, language);
        });
    }

    public void setListener(BungeeListener listener) {
        this.listener = listener;
    }

    public void executeCommands(Server overrideServer) {
        Server server = overrideServer == null ? getParent().getServer() : overrideServer;
        for (ExecutableCommand cmd : ((com.rexcantor64.triton.language.Language) language).getCmds()) {
            String cmdText = cmd.getCmd().replace("%player%", getParent().getName()).replace("%uuid%", uuid.toString());
            if (!cmd.isUniversal() && !cmd.getServers().contains(server.getInfo().getName())) continue;
            if (cmd.getType() == ExecutableCommand.Type.SERVER) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                // Action 2
                out.writeByte(2);
                out.writeUTF(cmdText);
                server.sendData("triton:main", out.toByteArray());
            } else if (cmd.getType() == ExecutableCommand.Type.PLAYER)
                server.unsafe().sendPacket(new Chat("/" + cmdText));
            else if (cmd.getType() == ExecutableCommand.Type.BUNGEE)
                BungeeCord.getInstance().getPluginManager().dispatchCommand(BungeeCord.getInstance().getConsole(),
                        cmdText);
            else if (cmd.getType() == ExecutableCommand.Type.BUNGEE_PLAYER)
                BungeeCord.getInstance().getPluginManager().dispatchCommand(getParent(), cmdText);
        }
    }
}
