package com.rexcantor64.triton.spigot.player;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedNumberFormat;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageSpigotEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpigotLanguagePlayer implements LanguagePlayer {

    private final UUID uuid;
    /**
     * UUID of this player as seen by the proxy; some servers might have different player UUIDs on proxy and servers
     **/
    @Getter
    private UUID proxyUniqueId;
    private Player bukkit;

    private Language lang;

    @Setter
    private Component lastTabHeader;
    @Setter
    private Component lastTabFooter;
    private final Map<UUID, String> bossBars = new ConcurrentHashMap<>();
    private boolean waitingForClientLocale = false;

    @Getter
    private final Map<World, Map<Integer, Optional<String>>> entitiesMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<World, Map<Integer, Entity>> playersMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<World, Map<Integer, ItemStack>> itemFramesMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<World, Map<Integer, String>> textDisplayEntitiesMap = new ConcurrentHashMap<>();
    @Getter
    private final Set<UUID> shownPlayers = new HashSet<>();
    @Getter
    private final Map<String, ScoreboardObjective> objectivesMap = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, ScoreboardTeam> teamsMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<SignLocation, Sign> signs = new ConcurrentHashMap<>();
    @Deprecated
    @Getter
    private final Map<SignLocation, Component[]> legacySigns = new ConcurrentHashMap<>(); // until 1.19_R1 only

    public SpigotLanguagePlayer(UUID p) {
        uuid = p;
        proxyUniqueId = this.uuid;
        load();
    }

    public void setScoreboardObjective(String name, String chatJson, EnumWrappers.RenderType type, @Nullable WrappedNumberFormat numberFormat) {
        ScoreboardObjective objective = this.objectivesMap.computeIfAbsent(name, k -> new ScoreboardObjective());
        objective.setChatJson(chatJson);
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

    public void saveSign(SignLocation location, MinecraftKey tileEntityType, NbtCompound nbtCompound) {
        this.signs.put(location, new Sign(tileEntityType, nbtCompound));
    }

    public void setProxyUniqueId(UUID proxyUniqueId) {
        if (Objects.equals(proxyUniqueId, this.proxyUniqueId)) {
            return;
        }
        this.proxyUniqueId = proxyUniqueId;
        val language = Triton.get().getStorage().getLanguage(this);
        setLang(language, false);
    }

    public Language getLang() {
        if (lang == null)
            lang = Triton.get().getLanguageManager().getMainLanguage();
        return lang;
    }

    public void setLang(Language lang) {
        setLang(lang, true);
    }

    public void setLang(Language lang, boolean sendToBungee) {
        PlayerChangeLanguageSpigotEvent event = new PlayerChangeLanguageSpigotEvent(this, this.lang, lang);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (this.waitingForClientLocale) {
            try {
                if (toBukkit().isPresent()) {
                    bukkit.sendMessage(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                            .getMessage("success.detected-language", lang.getDisplayName())));
                } else {
                    Triton.get().getLogger()
                            .logWarning("Could not automatically set language for %1 because Bukkit Player instance " +
                                    "is unknown", uuid);
                }
            } catch (Exception e) {
                Triton.get().getLogger().logError(e, "Failed to send \"language changed\" message.");
            }
        }
        boolean hasChanged = !Objects.equals(event.getNewLanguage(), this.lang);
        this.lang = event.getNewLanguage();
        this.waitingForClientLocale = false;
        if (hasChanged) {
            refreshAll();
            if (SpigotTriton.asSpigot().getBridgeManager() == null || Triton.get().getStorage() instanceof LocalStorage)
                save();
            if (sendToBungee && SpigotTriton.asSpigot().getBridgeManager() != null && Triton.get().getConfig().isBungeecord())
                SpigotTriton.asSpigot().getBridgeManager().updatePlayerLanguage(this);
            executeCommands();
        }
    }

    @Override
    public boolean isWaitingForClientLocale() {
        return waitingForClientLocale;
    }

    public void waitForClientLocale() {
        this.waitingForClientLocale = true;
    }

    private Optional<ProtocolLibListener> getInterceptor() {
        return Optional.ofNullable(SpigotTriton.asSpigot().getProtocolLibListener());
    }

    /**
     * Asynchronously refreshes entities, signs, inventory items, tab header/footer, bossbars and scoreboards
     * for the given player, that is, packets are sent to ensure they're updated with the player's language.
     */
    public void refreshAll() {
        Triton.get().runAsync(() -> toBukkit().ifPresent(player -> {
            refreshEntities();
            refreshSigns();
            player.updateInventory();
            getInterceptor().ifPresent((interceptor) -> {
                if (Triton.get().getConfig().isTab() && lastTabHeader != null && lastTabFooter != null)
                    interceptor.refreshTabHeaderFooter(this, lastTabHeader, lastTabFooter);
                if (Triton.get().getConfig().isBossbars())
                    for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                        interceptor.refreshBossbar(this, entry.getKey(), entry.getValue());
                if (Triton.get().getConfig().isScoreboards())
                    interceptor.refreshScoreboard(this);
                interceptor.refreshAdvancements(this);
            });
        }));
    }

    private void refreshSigns() {
        if (!Triton.get().getConfig().isSigns())
            return;
        getInterceptor().ifPresent(interceptor -> interceptor.refreshSigns(this));
    }

    private void refreshEntities() {
        if (Triton.get().getConfig().getHolograms().size() == 0 && !Triton.get().getConfig().isHologramsAll())
            return;
        getInterceptor().ifPresent(interceptor -> interceptor.refreshEntities(this));
    }

    /**
     * Signal this player that it has changed worlds.
     * Used to clean cache.
     */
    public void onWorldChange() {
        this.signs.clear();
        this.legacySigns.clear();
    }

    public void setBossbar(UUID uuid, String lastBossBar) {
        bossBars.put(uuid, lastBossBar);
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
    }

    private void load() {
        lang = Triton.get().getStorage().getLanguage(this);
        toBukkit().ifPresent(player -> {
            if (player.getAddress() != null) {
                Triton.get().getStorage()
                        .setLanguage(null, player.getAddress().getAddress().getHostAddress(), lang);
            }
        });
        if (Triton.get().getConfig().isRunLanguageCommandsOnLogin())
            executeCommands();
    }

    private void save() {
        Triton.get().runAsync(() -> {
            String ip = null;
            if (toBukkit().isPresent()) {
                val player = toBukkit().get();
                if (player.getAddress() != null) {
                    ip = player.getAddress().getAddress().getHostAddress();
                }
            }
            Triton.get().getStorage().setLanguage(this.getStorageUniqueId(), ip, lang);
        });
    }

    /**
     * Get this LanguagePlayer's Bukkit Player instance.
     * The value is stored in this object and therefore not recalculated.
     * If the stored value is null, this tries to get the Player instance by its UUID.
     * <p>
     * An empty Optional is returned if the player cannot be found (e.g. either it's offline or it's still joining).
     *
     * @return Bukkit's player instance of this Language Player
     */
    public Optional<Player> toBukkit() {
        if (bukkit != null && !bukkit.isOnline())
            bukkit = null;
        if (bukkit == null)
            bukkit = Bukkit.getPlayer(uuid);
        return Optional.ofNullable(bukkit);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    private void executeCommands() {
        toBukkit().ifPresent(bukkit -> {
            for (ExecutableCommand cmd : ((com.rexcantor64.triton.language.Language) lang).getCmds()) {
                String cmdText = cmd.getCmd().replace("%player%", bukkit.getName()).replace("%uuid%",
                        bukkit.getUniqueId().toString());
                if (cmd.getType() == ExecutableCommand.Type.SERVER)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdText);
                else if (cmd.getType() == ExecutableCommand.Type.PLAYER)
                    Bukkit.dispatchCommand(bukkit, cmdText);
            }
        });
    }

    @Override
    public String toString() {
        return "SpigotLanguagePlayer{" +
                "uuid=" + uuid +
                ", lang=" + (lang == null ? "null" : lang.getName()) +
                '}';
    }

    @Data
    public static class ScoreboardObjective {
        private String chatJson;
        private EnumWrappers.RenderType type;
        @Nullable
        private WrappedNumberFormat numberFormat;
    }

    @Data
    @AllArgsConstructor
    public static class ScoreboardTeam {
        private String displayJson;
        private String prefixJson;
        private String suffixJson;

        // other data (has to be saved for refreshing packet)
        private String nameTagVisibility;
        private String collisionRule;
        private EnumWrappers.ChatFormatting color;
        private int options;
    }

    @Data
    public static class Sign {
        private final MinecraftKey tileEntityType;
        private final NbtCompound compound;
    }

}
