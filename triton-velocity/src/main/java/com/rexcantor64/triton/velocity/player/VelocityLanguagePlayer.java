package com.rexcantor64.triton.velocity.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.utils.SocketUtils;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.rexcantor64.triton.velocity.packetinterceptor.VelocityNettyEncoder;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityLanguagePlayer extends TritonLanguagePlayer<Player> {
    @NotNull
    private final UUID uuid;
    @Nullable
    @Setter
    private Player parent;

    private Language language;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PUBLIC)
    private Component lastTabHeader;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PUBLIC)
    private Component lastTabFooter;
    private final Map<UUID, Component> bossBars = new HashMap<>();
    private final Map<UUID, Component> playerListItemCache = new ConcurrentHashMap<>();
    private boolean waitingForClientLocale = false;
    private String clientLocale;
    private final RefreshFeatures refresher;

    public VelocityLanguagePlayer(@NotNull UUID uuid) {
        super();
        Objects.requireNonNull(uuid, "cannot build VelocityLanguagePlayer from null UUID");
        this.uuid = uuid;
        this.refresher = new RefreshFeatures(this);
        Triton.get().runAsync(this::load);
    }

    @Override
    public @NotNull Optional<Player> getPlatformPlayer() {
        if (this.parent == null) {
            return VelocityTriton.asVelocity().getLoader().getServer().getPlayer(this.uuid);
        } else {
            return Optional.of(this.parent);
        }
    }

    public void setBossbar(UUID uuid, Component lastBossBar) {
        bossBars.put(uuid, lastBossBar);
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
    }

    Map<UUID, Component> getCachedBossBars() {
        return Collections.unmodifiableMap(bossBars);
    }

    public void clearCachedBossbars() {
        bossBars.clear();
    }

    public void cachePlayerListItem(UUID uuid, Component lastDisplayName) {
        playerListItemCache.put(uuid, lastDisplayName);
    }

    public void deleteCachedPlayerListItem(UUID uuid) {
        playerListItemCache.remove(uuid);
    }

    Map<UUID, Component> getCachedPlayerListItems() {
        return Collections.unmodifiableMap(playerListItemCache);
    }

    @Override
    public boolean isWaitingForClientLocale() {
        return waitingForClientLocale;
    }

    @Override
    public void waitForClientLocale() {
        this.waitingForClientLocale = true;
    }

    public void setClientLocale(String locale) {
        if (this.isWaitingForClientLocale()) {
            this.setLang(Triton.get().getLanguageManager().getLanguageByLocaleOrDefault(locale));
        }
        this.clientLocale = locale;
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
        // TODO fire Triton's API change language event
        val player = getPlatformPlayer();

        this.language = language;
        if (this.waitingForClientLocale) {
            player.ifPresent(parent -> parent.sendMessage(Triton.get().getMessagesConfig()
                    .getMessageComponent("success.detected-language", language.getDisplayName())));
        }
        this.waitingForClientLocale = false;

        if (sendToSpigot) {
            player.ifPresent(p -> VelocityTriton.asVelocity().getBridgeManager().sendPlayerLanguage(this));
        }

        save();
        refreshAll();
        executeCommands(null);
    }

    public void refreshAll() {
        super.refreshAll();
        this.refresher.refreshAll();
    }

    public void injectNettyPipeline() {
        if (Triton.get().getConfig().isUsePacketEvents()) {
            // PacketEvents handler covers all packets translated by Velocity,
            // so no need to inject ourselves into netty anymore.
            Triton.get().getLogger().logDebug("Skipped injecting into netty pipeline for player %1 because PacketEvents is in use", getUUID());
            return;
        }
        val player = this.getPlatformPlayer();
        player.ifPresent(parent -> {
            ConnectedPlayer connectedPlayer = (ConnectedPlayer) parent;
            connectedPlayer.getConnection().getChannel().pipeline()
                    .addAfter(Connections.MINECRAFT_ENCODER, "triton-custom-encoder", new VelocityNettyEncoder(this));
        });
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public @NotNull ProtocolVersion getProtocolVersion() {
        return this.getPlatformPlayer().map(Player::getProtocolVersion).orElse(ProtocolVersion.UNKNOWN);
    }

    private void load() {
        val player = getPlatformPlayer();
        this.language = Triton.get().getStorage().getLanguage(this);
        if (this.clientLocale != null && this.isWaitingForClientLocale()) {
            this.waitingForClientLocale = false;
            this.language = Triton.get().getLanguageManager().getLanguageByLocaleOrDefault(this.clientLocale);
            player.ifPresent(parent -> parent.sendMessage(Triton.get().getMessagesConfig()
                    .getMessageComponent("success.detected-language", language.getDisplayName())));
        }
        player.ifPresent(parent -> Triton.get().getStorage()
                .setLanguage(null, SocketUtils.getIpAddress(parent.getRemoteAddress()), language));
    }

    private void save() {
        Triton.get().runAsync(() -> {
            val ip = getPlatformPlayer()
                    .map(player -> SocketUtils.getIpAddress(player.getRemoteAddress()))
                    .orElse(null);
            Triton.get().getStorage().setLanguage(getUUID(), ip, language);
        });
    }

    public void executeCommands(RegisteredServer overrideServer) {
        val playerOpt = getPlatformPlayer();
        if (!playerOpt.isPresent()) {
            return;
        }
        val player = playerOpt.get();

        val currentServer = player.getCurrentServer();
        if (overrideServer == null && !currentServer.isPresent()) return;
        val server = overrideServer == null ? currentServer.get().getServer() : overrideServer;
        for (val cmd : ((com.rexcantor64.triton.language.Language) language).getCmds()) {
            val cmdText = cmd.getCmd().replace("%player%", player.getUsername())
                    .replace("%uuid%", player.getUniqueId().toString());

            if (!cmd.isUniversal() && !cmd.getServers().contains(server.getServerInfo().getName())) {
                continue;
            }

            val velocity = VelocityTriton.asVelocity().getLoader().getServer();

            if (cmd.getType() == ExecutableCommand.Type.SERVER) {
                VelocityTriton.asVelocity().getBridgeManager().sendExecutableCommand(cmdText, server);
            } else if (cmd.getType() == ExecutableCommand.Type.PLAYER) {
                player.spoofChatInput("/" + cmdText);
            } else if (cmd.getType() == ExecutableCommand.Type.BUNGEE) {
                velocity.getCommandManager().executeAsync(velocity.getConsoleCommandSource(), cmdText);
            } else if (cmd.getType() == ExecutableCommand.Type.BUNGEE_PLAYER) {
                velocity.getCommandManager().executeAsync(player, cmdText);
            }
        }
    }

    @Override
    public String toString() {
        return "VelocityLanguagePlayer{" +
                "uuid=" + this.getUUID() +
                ", language=" + Optional.ofNullable(language).map(Language::getName).orElse("null") +
                '}';
    }
}
