package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.SpigotSender;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.language.item.*;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.val;
import lombok.var;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpigotBridgeManager implements PluginMessageListener {

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals("triton:main")) return;

        val start = System.currentTimeMillis();

        val in = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            val action = in.readByte();
            if (action == 0) {
                if (!(Triton.get().getStorage() instanceof LocalStorage)) {
                    Triton.get().getLogger()
                            .logWarning(0, "You're using BungeeCord with a local storage option, but this server is " +
                                    "using non-local storage.");
                    Triton.get().getLogger()
                            .logWarning(0, "All servers must share the same storage settings, otherwise translations " +
                                    "might not be loaded.");
                    return;
                }
                try {
                    val firstSend = in.readBoolean();
                    if (firstSend) {
                        val config = Triton.get().getConf();
                        config.setMainLanguage(in.readUTF());
                        short languageSize = in.readShort();
                        val languages = new ArrayList<Language>();
                        for (var i = 0; i < languageSize; i++) {
                            val name = in.readUTF();
                            val displayName = in.readUTF();
                            val flag = in.readUTF();
                            val minecraftCodes = new ArrayList<String>();
                            val mcSize = in.readShort();
                            for (var k = 0; k < mcSize; k++)
                                minecraftCodes.add(in.readUTF());
                            languages.add(new Language(name, flag, minecraftCodes, displayName, null));
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
                    for (var i = 0; i < itemsSize; i++) {
                        val type = in.readByte();
                        val key = in.readUTF();
                        switch (type) {
                            case 0:
                            case 2:
                                val textItem = new LanguageText();
                                textItem.setKey(key);

                                val msgs = new HashMap<String, String>();
                                val langSize = in.readShort();
                                for (var k = 0; k < langSize; k++)
                                    msgs.put(in.readUTF(), in.readUTF());
                                textItem.setLanguages(msgs);

                                List<String> patterns = new ArrayList<>();
                                if (type != 0) {
                                    val matchesSize = in.readShort();
                                    for (var k = 0; k < matchesSize; k++)
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
                                for (var k = 0; k < linesSize; k++)
                                    signLines.put(in.readUTF(), new String[]{in.readUTF(), in.readUTF(), in.readUTF()
                                            , in.readUTF()});
                                signItem.setLines(signLines);

                                languageItems.add(signItem);
                                break;
                            default:
                                Triton.get().getLogger()
                                        .logWarning(2, "Received invalid type language item type while reading " +
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

                    Triton.get().getLogger().logInfo(2, "Received config from BungeeCord and parsed it in %1ms!",
                            System.currentTimeMillis() - start);
                } finally {
                    Triton.get().getLanguageManager().setup();
                    Bukkit.getScheduler().runTaskLater(Triton.asSpigot().getLoader(), () -> Triton.get()
                            .refreshPlayers(), 10L);
                }
            } else if (action == 1) {
                val uuid = new UUID(in.readLong(), in.readLong());
                val lang = Triton.get().getLanguageManager().getLanguageByName(in.readUTF(), true);
                Bukkit.getScheduler().runTaskLater(Triton.asSpigot().getLoader(),
                        () -> ((SpigotLanguagePlayer) Triton.get().getPlayerManager().get(uuid)).setLang(lang, false)
                        , 10L);
            } else if (action == 2) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), in.readUTF());
            } else if (action == 3) {
                val storage = Triton.get().getStorage();
                if (storage instanceof LocalStorage) {
                    Triton.get().getLogger()
                            .logWarning(0, "You're using BungeeCord with a non-local storage option, but this server " +
                                    "is using local storage.");
                    Triton.get().getLogger()
                            .logWarning(0, "All servers must share the same storage settings, otherwise translations " +
                                    "might not be loaded.");
                    return;
                }
                Triton.get().runAsync(() -> {
                    val col = storage.downloadFromStorage();
                    storage.setCollections(col);

                    Triton.get().getLanguageManager().setup();
                    Bukkit.getScheduler().runTaskLater(Triton.asSpigot().getLoader(), () -> Triton.get()
                            .refreshPlayers(), 10L);
                });
            } else if (action == 4) {
                val uuid = new UUID(in.readLong(), in.readLong());
                val p = Bukkit.getPlayer(uuid);

                val subCommand = in.readBoolean() ? in.readUTF() : null;
                val args = new String[in.readShort()];
                for (var i = 0; i < args.length; ++i)
                    args[i] = in.readUTF();

                val commandEvent = new CommandEvent(new SpigotSender(p), subCommand, args, "triton",
                        CommandEvent.Environment.SPIGOT);
                Triton.asSpigot().getCommandHandler().handleCommand(commandEvent);
            }
        } catch (Exception e) {
            Triton.get().getLogger().logError("Failed to parse plugin message: %1", e.getMessage());
            if (Triton.get().getConf().getLogLevel() >= 1)
                e.printStackTrace();
        }
    }

    public void updatePlayerLanguage(SpigotLanguagePlayer lp) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action (0): updatePlayerLanguage
        out.writeByte(0);
        out.writeUTF(lp.getUUID().toString());
        out.writeUTF(lp.getLang().getName());
        lp.toBukkit().sendPluginMessage(Triton.asSpigot().getLoader(), "triton:main", out.toByteArray());
    }

    public void updateSign(String world, int x, int y, int z, String key, Player p) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action (1): sign management
        out.writeByte(1);
        out.writeUTF(world);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeBoolean(key != null); // Set (true) or Remove (false)
        if (key != null) // Set only
            out.writeUTF(key);
        p.sendPluginMessage(Triton.asSpigot().getLoader(), "triton:main", out.toByteArray());
    }

}
