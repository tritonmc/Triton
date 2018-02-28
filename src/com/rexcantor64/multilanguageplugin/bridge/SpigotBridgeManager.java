package com.rexcantor64.multilanguageplugin.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.config.MainConfig;
import com.rexcantor64.multilanguageplugin.config.interfaces.Configuration;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.language.item.LanguageItem;
import com.rexcantor64.multilanguageplugin.language.item.LanguageSign;
import com.rexcantor64.multilanguageplugin.language.item.LanguageText;
import com.rexcantor64.multilanguageplugin.player.LanguagePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpigotBridgeManager implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals("MultiLanguagePlugin")) return;

        long start = System.currentTimeMillis();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            byte action = in.readByte();
            if (action == 0) {
                MainConfig config = MultiLanguagePlugin.get().getConf();
                config.setMainLanguage(in.readUTF());
                short languageSize = in.readShort();
                Configuration languages = new Configuration();
                for (int i = 0; i < languageSize; i++) {
                    String name = in.readUTF();
                    String displayName = in.readUTF();
                    String flag = in.readUTF();
                    List<String> minecraftCodes = new ArrayList<>();
                    short mcSize = in.readShort();
                    for (int k = 0; k < mcSize; k++)
                        minecraftCodes.add(in.readUTF());
                    Configuration section = languages.getSection(name);
                    section.set("flag", flag);
                    section.set("minecraft-code", minecraftCodes);
                    section.set("display-name", displayName);
                }
                config.setLanguages(languages);
                MultiLanguagePlugin.get().getLanguageManager().setup();
                // Read language files
                List<LanguageItem> languageItems = new ArrayList<>();
                int itemsSize = in.readInt();
                for (int i = 0; i < itemsSize; i++) {
                    byte type = in.readByte();
                    switch (type) {
                        case 0:
                            String key = in.readUTF();
                            HashMap<String, String> msgs = new HashMap<>();
                            short langSize2 = in.readShort();
                            for (int k = 0; k < langSize2; k++)
                                msgs.put(in.readUTF(), in.readUTF());
                            languageItems.add(new LanguageText(key, msgs));
                            break;
                        case 1:
                            LanguageSign.SignLocation loc = new LanguageSign.SignLocation(in.readUTF(), in.readInt(), in.readInt(), in.readInt());
                            HashMap<String, String[]> signLines = new HashMap<>();
                            short langSize = in.readShort();
                            for (int k = 0; k < langSize; k++)
                                signLines.put(in.readUTF(), new String[]{in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()});
                            languageItems.add(new LanguageSign(loc, signLines));
                            break;
                        default:
                            MultiLanguagePlugin.get().logDebugWarning("Received invalid type language item type while reading from BungeeCord: %1", type);
                            break;
                    }
                }
                MultiLanguagePlugin.get().getLanguageConfig().setItems(languageItems);
                MultiLanguagePlugin.get().logDebug("Received config from BungeeCord and parsed it in %1ms!", System.currentTimeMillis() - start);

                for (LanguagePlayer lp : MultiLanguagePlugin.asSpigot().getPlayerManager().getAll())
                    lp.refreshAll();

                MultiLanguagePlugin.get().getLanguageManager().setup();
            } else if (action == 1) {
                MultiLanguagePlugin.asSpigot().getPlayerManager().get(player).setLang(MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(in.readUTF(), true), false);
            }
        } catch (Exception e) {
            MultiLanguagePlugin.get().logError("Failed to parse plugin message: %1", e.getMessage());
        }
    }

    public void updatePlayerLanguage(LanguagePlayer lp) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action (0): updatePlayerLanguage
        out.writeByte(0);
        out.writeUTF(lp.toBukkit().getUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        lp.toBukkit().sendPluginMessage(MultiLanguagePlugin.get().getLoader().asSpigot(), "MultiLanguagePlugin", out.toByteArray());
    }

}
