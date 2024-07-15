package com.rexcantor64.triton.spigot.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.bridge.BridgeSerializer;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.commands.handler.SpigotSender;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpigotBridgeManager implements PluginMessageListener, BridgeManager {

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        if (!channel.equals("triton:main")) {
            return;
        }

        val start = System.currentTimeMillis();

        val in = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            val action = in.readByte();
            if (action == BridgeSerializer.ActionP2S.SEND_STORAGE_AND_CONFIG.getKey()) {
                if (!(Triton.get().getStorage() instanceof LocalStorage)) {
                    Triton.get().getLogger()
                            .logWarning("You're using BungeeCord with a local storage option, but this server is " +
                                    "using non-local storage.");
                    Triton.get().getLogger()
                            .logWarning("All servers must share the same storage settings, otherwise translations " +
                                    "might not be loaded.");
                    return;
                }
                try {
                    val firstSend = in.readBoolean();
                    if (firstSend) {
                        val config = Triton.get().getConfig();
                        config.setMainLanguage(in.readUTF());
                        short languageSize = in.readShort();
                        val languages = new ArrayList<Language>();
                        for (int i = 0; i < languageSize; i++) {
                            val name = in.readUTF();
                            val displayName = in.readUTF();
                            val flag = in.readUTF();
                            val fallbackLanguages = new ArrayList<String>();
                            val fallbackSize = in.readShort();
                            for (int k = 0; k < fallbackSize; k++)
                                fallbackLanguages.add(in.readUTF());
                            val minecraftCodes = new ArrayList<String>();
                            val mcSize = in.readShort();
                            for (int k = 0; k < mcSize; k++)
                                minecraftCodes.add(in.readUTF());
                            languages.add(new Language(name, flag, minecraftCodes, displayName, fallbackLanguages, null));
                        }
                        config.setLanguages(languages);
                        val jsonObj = new JsonObject();
                        File file = new File(Triton.get().getDataFolder(), "cache.json");
                        jsonObj.addProperty("mainLanguage", config.getMainLanguage());
                        jsonObj.add("languages", gson.toJsonTree(languages));
                        Files.write(file.toPath(), gson.toJson(jsonObj)
                                        .getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    }
                    // Read language files
                    val languageItems = new ArrayList<LanguageItem>();
                    val itemsSize = in.readInt();
                    for (int i = 0; i < itemsSize; i++) {
                        val type = in.readByte();
                        val key = in.readUTF();
                        switch (type) {
                            case 0:
                            case 2:
                                val textItem = new LanguageText();
                                textItem.setKey(key);

                                val msgs = new HashMap<String, String>();
                                val langSize = in.readShort();
                                for (int k = 0; k < langSize; k++)
                                    msgs.put(in.readUTF(), in.readUTF());
                                textItem.setLanguages(msgs);

                                List<String> patterns = new ArrayList<>();
                                if (type != 0) {
                                    val matchesSize = in.readShort();
                                    for (int k = 0; k < matchesSize; k++)
                                        patterns.add(in.readUTF());
                                }
                                if (patterns.size() > 0) textItem.setPatterns(patterns);

                                languageItems.add(textItem);
                                break;
                            case 1:
                                val signItem = new LanguageSign();
                                signItem.setKey(key);

                                val signLocations = new ArrayList<SignLocation>();
                                val locSize = in.readShort();
                                for (int k = 0; k < locSize; k++)
                                    signLocations.add(new SignLocation(in.readUTF(), in.readInt(), in.readInt(), in
                                            .readInt()));
                                signItem.setLocations(signLocations);

                                val signLines = new HashMap<String, String[]>();
                                val linesSize = in.readShort();
                                for (int k = 0; k < linesSize; k++)
                                    signLines.put(in.readUTF(), new String[]{in.readUTF(), in.readUTF(), in.readUTF()
                                            , in.readUTF()});
                                signItem.setLines(signLines);

                                languageItems.add(signItem);
                                break;
                            default:
                                Triton.get().getLogger()
                                        .logError("Received invalid type language item type while reading " +
                                                "from BungeeCord: %1", type);
                                break;
                        }
                    }
                    ConcurrentHashMap<String, Collection> collections;
                    if (firstSend) {
                        collections = new ConcurrentHashMap<>();
                        val col = new Collection();
                        col.setItems(languageItems);
                        collections.put("cache", col);
                    } else {
                        collections = Triton.get().getStorage().getCollections();
                        val col = collections.containsKey("cache") ? collections.get("cache") : new Collection();
                        col.getItems().addAll(languageItems);
                    }
                    Triton.get().getStorage().setCollections(collections);
                    Triton.get().getStorage().uploadToStorage(collections);

                    Triton.get().getLogger().logDebug("Received config from BungeeCord and parsed it in %1ms!",
                            System.currentTimeMillis() - start);
                } finally {
                    Triton.get().getLanguageManager().setup();
                    Triton.get().getTranslationManager().setup();
                    Bukkit.getScheduler().runTaskLater(SpigotTriton.asSpigot().getJavaPlugin(), () -> Triton.get()
                            .refreshPlayers(), 10L);
                }
            } else if (action == BridgeSerializer.ActionP2S.SEND_PLAYER_LANGUAGE.getKey()) {
                val uuid = new UUID(in.readLong(), in.readLong());

                val lang = Triton.get().getLanguageManager().getLanguageByNameOrDefault(in.readUTF());
                val languagePlayer = (SpigotLanguagePlayer) SpigotTriton.asSpigot().getPlayerManager().get(player.getUniqueId());
                languagePlayer.setProxyUniqueId(uuid);
                Bukkit.getScheduler().runTaskLater(SpigotTriton.asSpigot().getJavaPlugin(),
                        () -> languagePlayer.setLang(lang, false),
                        10L);
            } else if (action == BridgeSerializer.ActionP2S.SEND_COMMAND_AS_CONSOLE.getKey()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), in.readUTF());
            } else if (action == BridgeSerializer.ActionP2S.SIGNAL_REFRESH_FROM_DB.getKey()) {
                val storage = Triton.get().getStorage();
                if (storage instanceof LocalStorage) {
                    Triton.get().getLogger()
                            .logWarning("You're using BungeeCord with a non-local storage option, but this server " +
                                    "is using local storage.");
                    Triton.get().getLogger()
                            .logWarning("All servers must share the same storage settings, otherwise translations " +
                                    "might not be loaded.");
                    return;
                }
                Triton.get().runAsync(() -> {
                    val col = storage.downloadFromStorage();
                    storage.setCollections(col);

                    Triton.get().getLanguageManager().setup();
                    Triton.get().getTranslationManager().setup();
                    Bukkit.getScheduler().runTaskLater(SpigotTriton.asSpigot().getJavaPlugin(), () -> Triton.get()
                            .refreshPlayers(), 10L);
                });
            } else if (action == BridgeSerializer.ActionP2S.FORWARD_TRITON_COMMAND.getKey()) {
                @Deprecated
                val uuid = new UUID(in.readLong(), in.readLong()); // TODO remove in v4

                val subCommand = in.readBoolean() ? in.readUTF() : null;
                val args = new String[in.readShort()];
                for (int i = 0; i < args.length; ++i)
                    args[i] = in.readUTF();

                Triton.get().getLogger().logTrace("Received forwarded command '%1' with args %2 for player %3",
                        subCommand, Arrays.toString(args), player.getUniqueId());

                val commandEvent = new CommandEvent(
                        new SpigotSender(player),
                        subCommand,
                        args,
                        "triton",
                        true
                );
                SpigotTriton.asSpigot().getCommandHandler().handleCommand(commandEvent);
            }
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to parse plugin message.");
        }
    }

    public void updatePlayerLanguage(SpigotLanguagePlayer lp) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(BridgeSerializer.ActionS2P.UPDATE_PLAYER_LANGUAGE.getKey());
        out.writeUTF(lp.getProxyUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        lp.toBukkit().ifPresent(player ->
                player.sendPluginMessage(SpigotTriton.asSpigot().getJavaPlugin(), "triton:main", out.toByteArray())
        );
    }

    public void updateSign(String world, int x, int y, int z, String key, Player p) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(BridgeSerializer.ActionS2P.UPDATE_SIGN_GROUP_MEMBERSHIP.getKey());
        out.writeUTF(world);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeBoolean(key != null); // Set (true) or Remove (false)
        if (key != null) // Set only
            out.writeUTF(key);
        p.sendPluginMessage(SpigotTriton.asSpigot().getJavaPlugin(), "triton:main", out.toByteArray());
    }

    @Override
    public void forwardCommand(CommandEvent commandEvent) {
        throw new UnsupportedOperationException("Cannot forward command on non-proxy platform");
    }

    @Override
    public void sendConfigToEveryone() {
        throw new UnsupportedOperationException("Cannot send config to everyone on non-proxy platform");
    }
}
