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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    private static final String DUMP_NAME_TEMPLATE = "dump_%d_%d_%d.txt";
    @Getter
    private final HashMap<String, FeatureSyntax> availableTypes = new HashMap<>();
    @Getter
    private final Map<UUID, Collection<FeatureSyntax>> filter = new HashMap<>();

    public DumpManager() {
        val config = Triton.get().getConfig();
        availableTypes.put("chat", config.getChatSyntax());
        availableTypes.put("actionbar", config.getActionbarSyntax());
        availableTypes.put("bossbar", config.getBossbarSyntax());
        availableTypes.put("gui", config.getGuiSyntax());
        availableTypes.put("advancements", config.getAdvancementsSyntax());
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
    }

    /**
     * Disable message dumping for all messages.
     *
     * @since 4.0.0
     */
    public void disable() {
        filter.clear();
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
            if (currentSettings.size() == 0) {
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
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return String.format(DUMP_NAME_TEMPLATE, year, month, day);
    }

    public void dump(Component message, Localized localized, FeatureSyntax type) {
        if (!shouldDump(localized, type)) {
            return;
        }

        Path tritonFolderPath = Triton.get().getDataFolder().toPath();
        Path dumpFolderPath = tritonFolderPath.resolve(DUMP_FOLDER_NAME);
        Path dumpPath = dumpFolderPath.resolve(getDumpName());

        File dumpFolderFile = dumpFolderPath.toFile();
        if (!dumpFolderFile.isDirectory() && !dumpFolderFile.mkdirs()) {
            Triton.get().getLogger().logError("Failed to create \"dumps\" folder!");
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
