package com.rexcantor64.triton.debug;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.player.LanguagePlayer;
import lombok.Cleanup;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle dumping messages into a file for debug purposes.
 *
 * @since 4.0.0
 */
public class DumpManager {

    private static final UUID ALL_UUID = new UUID(0L, 0L);
    private static final String DUMP_FOLDER_NAME = "dumps";
    private static final String DUMP_NAME_TEMPLATE = "dump-%s.txt";
    @Getter
    private final HashMap<String, FeatureSyntax> availableTypes = new HashMap<>();
    @Getter
    private final Map<UUID, Collection<FeatureSyntax>> filter = new HashMap<>();
    // Helper to avoid querying the map unnecessarily
    private boolean enabled = false;

    public DumpManager() {
        val config = Triton.get().getConfig();
        availableTypes.put("actionbar", config.getActionbarSyntax());
        availableTypes.put("advancements", config.getAdvancementsSyntax());
        availableTypes.put("bossbar", config.getBossbarSyntax());
        availableTypes.put("chat", config.getChatSyntax());
        availableTypes.put("deathscreen", config.getDeathScreenSyntax());
        availableTypes.put("gui", config.getGuiSyntax());
        availableTypes.put("hologram", config.getHologramSyntax());
        availableTypes.put("items", config.getItemsSyntax());
        availableTypes.put("kick", config.getKickSyntax());
        availableTypes.put("motd", config.getMotdSyntax());
        availableTypes.put("resourcepackprompt", config.getResourcePackPromptSyntax());
        availableTypes.put("scoreboard", config.getScoreboardSyntax());
        availableTypes.put("signs", config.getSignsSyntax());
        availableTypes.put("tab", config.getTabSyntax());
        availableTypes.put("title", config.getTitleSyntax());
    }

    /**
     * Enable message dumping for all messages of certain types (chat, action bars, etc.).
     *
     * @since 4.0.0
     */
    public void enableForEveryone(Collection<FeatureSyntax> enabledTypes) {
        enableForPlayer(ALL_UUID, enabledTypes);
    }

    /**
     * Enable message dumping for messages of certain types (chat, action bars, etc.)
     * sent to a given player.
     * Some messages that are not translated using the {@link com.rexcantor64.triton.player.LanguagePlayer}
     * instance might not be correctly identified.
     * <p>
     * If the message dumping is already enabled for the player, current
     * types will be merged with the given types.
     * If the message dumping is already enabled for everything, player specific settings
     * will take precedence.
     *
     * @param uuid         The UUID of the player.
     * @param enabledTypes The types to enable.
     * @since 4.0.0
     */
    public void enableForPlayer(UUID uuid, Collection<FeatureSyntax> enabledTypes) {
        filter.merge(uuid, enabledTypes, (oldTypes, newTypes) -> {
            oldTypes.addAll(newTypes);
            return oldTypes;
        });
        enabled = true;
    }

    /**
     * Disable message dumping for all messages.
     *
     * @since 4.0.0
     */
    public void disable() {
        filter.clear();
        enabled = false;
    }

    /**
     * Disable message dumping for all messages, except for player specific settings.
     *
     * @since 4.0.0
     */
    public void disableForEveryone(Collection<FeatureSyntax> enabledTypes) {
        disableForPlayer(ALL_UUID, enabledTypes);
    }

    /**
     * Disable message dumping for messages of certain types (chat, action bars, etc.)
     * sent to a given player.
     * <p>
     * If the message dumping is already enabled for the player, current
     * types will be merged with the given types, except if all types are being dumped,
     * where this will overwrite that.
     *
     * @param uuid         The UUID of the player.
     * @param enabledTypes The types to enable.
     * @since 4.0.0
     */
    public void disableForPlayer(UUID uuid, Collection<FeatureSyntax> enabledTypes) {
        synchronized (filter) {
            val currentSettings = filter.get(uuid);
            if (currentSettings == null) {
                return;
            }
            currentSettings.removeAll(enabledTypes);
            if (currentSettings.isEmpty()) {
                filter.remove(uuid);
            }
        }
    }

    /**
     * Decide whether the message should be dumped.
     * It first tries to get the player settings, if localized is a player and
     * if the player has custom settings, otherwise it checks global settings.
     *
     * @param localized The localized target for the message.
     * @param type      The type of the message.
     * @return Whether the message should be dumped.
     * @since 4.0.0
     */
    private boolean shouldDump(Localized localized, FeatureSyntax type) {
        if (!enabled) {
            // Quickly determine that dumping is disabled without querying the Map
            return false;
        }
        if (localized instanceof LanguagePlayer) {
            val uuid = ((LanguagePlayer) localized).getUUID();
            val playerSettings = filter.get(uuid);
            if (playerSettings != null) {
                // Use referential equality instead of object equality, since we want to compare if the
                // instance is the same and not if the content is the same.
                return playerSettings.stream().anyMatch(el -> el == type);
            }
        }
        val globalSettings = filter.get(ALL_UUID);
        if (globalSettings != null) {
            // Use referential equality instead of object equality, since we want to compare if the
            // instance is the same and not if the content is the same.
            return globalSettings.stream().anyMatch(el -> el == type);
        }
        return false;
    }

    private String getDumpName() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return String.format(DUMP_NAME_TEMPLATE, date);
    }

    /**
     * Attempt to dump a message. The message will only be dumped if dumping is
     * enabled for the given player and message type. This method will immediately
     * write to the dump file.
     *
     * @param message   The message to dump.
     * @param localized The locale representing a player. If it is a {@link Localized},
     *                  player-specific settings apply, otherwise whether to dump will
     *                  be decided by global configurations.
     * @param type      The type of the message.
     */
    public void dump(Component message, Localized localized, FeatureSyntax type) {
        if (!shouldDump(localized, type)) {
            return;
        }

        Path tritonFolderPath = Triton.get().getDataFolder().toPath();
        Path dumpFolderPath = tritonFolderPath.resolve(DUMP_FOLDER_NAME);
        Path dumpPath = dumpFolderPath.resolve(getDumpName());

        File dumpFolderFile = dumpFolderPath.toFile();
        if (!dumpFolderFile.isDirectory() && !dumpFolderFile.mkdirs()) {
            Triton.get().getLogger().logError("Failed to create \"%1\" folder!", dumpFolderPath.toAbsolutePath().toString());
            return;
        }

        File dumpFile = dumpPath.toFile();

        try {
            @Cleanup
            val writer = new BufferedWriter(new FileWriter(dumpFile, true));

            writer.write(GsonComponentSerializer.gson().serialize(message));
            writer.write("\n");
        } catch (IOException exception) {
            Triton.get().getLogger().logError(exception, "Failed writing to dump %1!", dumpPath.toString());
        }
    }

}
