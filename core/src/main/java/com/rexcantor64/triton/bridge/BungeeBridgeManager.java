package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class BungeeBridgeManager implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        // TODO
        if (!e.getTag().equals("triton:main")) return;
        val in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            val action = in.readByte();

            // Player changes language
            if (action == 0) {
                val uuid = UUID.fromString(in.readUTF());
                val language = in.readUTF();

                val player = Triton.get().getPlayerManager().get(uuid);
                if (player != null)
                    Triton.get().runAsync(() -> player
                            .setLang(Triton.get().getLanguageManager().getLanguageByName(language, true)));
            }

            // Add or remove a location from a sign group using /triton sign
            if (action == 1) {
                val server = ((Server) e.getSender()).getInfo();
                SignLocation location = new SignLocation(server.getName(), in.readUTF(), in.readInt(), in.readInt(), in
                        .readInt());

                // Whether we're adding a location to a group or removing one from a group
                boolean add = in.readBoolean();
                val key = add ? in.readUTF() : null;

                Triton.get().getStorage().toggleLocationForSignGroup(location, key);

                Triton.get().runAsync(() -> {
                    Triton.get().getLogger().logInfo(2, "Saving sign to storage...");
                    Triton.get().getStorage().uploadToStorage(Triton.get().getStorage().getCollections());
                    sendConfigToServer(server, null);
                    Triton.get().getLogger().logInfo(2, "Sign saved!");
                });
            }
        } catch (Exception e1) {
            Triton.get().getLogger().logError("Failed to read plugin message: %1", e1.getMessage());
            if (Triton.get().getConf().getLogLevel() > 0)
                e1.printStackTrace();
        }
    }

    public void sendConfigToEveryone() {
        Triton.get().getLogger().logInfo(2, "Sending config and translations to all Spigot servers...");
        try {
            val languageOut = getLanguageDataOutput();

            // Send language files
            for (val info : Triton.asBungee().getBungeeCord().getServers().values())
                sendConfigToServer(info, languageOut);
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError("Failed to send config and language items to other servers! Not everything might work " +
                            "as expected! Error: %1", e.getMessage());
            if (Triton.get().getConf().getLogLevel() > 0)
                e.printStackTrace();
        }
    }

    public ByteArrayDataOutput getLanguageDataOutput() {
        val languageOut = ByteStreams.newDataOutput();
        // Action 0 (send config)
        languageOut.writeUTF(Triton.get().getLanguageManager().getMainLanguage().getName());
        val languageList = Triton.get().getLanguageManager().getAllLanguages();
        languageOut.writeShort(languageList.size());
        for (val language : languageList) {
            languageOut.writeUTF(language.getName());
            languageOut.writeUTF(language.getRawDisplayName());
            languageOut.writeUTF(language.getFlagCode());
            languageOut.writeShort(language.getMinecraftCodes().size());
            for (val code : language.getMinecraftCodes())
                languageOut.writeUTF(code);
        }

        return languageOut;
    }

    public void sendConfigToServer(@NonNull ServerInfo info, ByteArrayDataOutput languageOut) {
        Triton.get().getLogger().logInfo(2, "Sending config and translations to '%1' server...", info.getName());
        try {

            // If not using local storage, each server should fetch the translations for themselves
            if (!(Triton.get().getStorage() instanceof LocalStorage)) {
                val refreshSignalOut = ByteStreams.newDataOutput();
                // Action 3 (tell server to re-fetch from database storage)
                refreshSignalOut.writeByte(3);
                info.sendData("triton:main", refreshSignalOut.toByteArray());
                return;
            }

            if (languageOut == null) languageOut = getLanguageDataOutput();
            val languageList = Triton.get().getLanguageManager().getAllLanguages();

            var firstSend = true;
            var size = 0;
            var languageItemsOut = ByteStreams.newDataOutput();
            for (val collection : Triton.get().getStorage().getCollections().values())
                for (val item : collection.getItems()) {
                    if (languageItemsOut.toByteArray().length > 29000) {
                        val out = ByteStreams.newDataOutput();
                        out.writeByte(0);
                        if (firstSend) {
                            firstSend = false;
                            out.writeBoolean(true);
                            out.write(languageOut.toByteArray());
                        } else {
                            out.writeBoolean(false);
                        }
                        out.writeInt(size);
                        out.write(languageItemsOut.toByteArray());
                        info.sendData("triton:main", out.toByteArray());
                        languageItemsOut = ByteStreams.newDataOutput();
                        size = 0;
                    }

                    if (item instanceof LanguageText) {
                        val text = (LanguageText) item;
                        if (!text.belongsToServer(collection.getMetadata(), info.getName()))
                            continue;
                        // Send type (2) (type 0, but with pattern data)
                        languageItemsOut.writeByte(2);
                        languageItemsOut.writeUTF(item.getKey());
                        var langSize = 0;
                        val langOut2 = ByteStreams.newDataOutput();
                        for (val lang : languageList) {
                            val msg = text.getMessage(lang.getName());
                            if (msg == null) continue;
                            langOut2.writeUTF(lang.getName());
                            langOut2.writeUTF(msg);
                            langSize++;
                        }
                        languageItemsOut.writeShort(langSize);
                        languageItemsOut.write(langOut2.toByteArray());
                        if (text.getPatterns() != null) {
                            languageItemsOut.writeShort(text.getPatterns().size());
                            for (String s : text.getPatterns())
                                languageItemsOut.writeUTF(s);
                        } else
                            languageItemsOut.writeShort(0);
                    } else if (item instanceof LanguageSign) {
                        // Send type (1)
                        val sign = (LanguageSign) item;
                        languageItemsOut.writeByte(1);
                        languageItemsOut.writeUTF(item.getKey());

                        var locSize = 0;
                        val locOut = ByteStreams.newDataOutput();
                        for (val loc : sign.getLocations()) {
                            if (loc.getServer() != null && !loc.getServer().equals(info.getName()))
                                continue;
                            locOut.writeUTF(loc.getWorld());
                            locOut.writeInt(loc.getX());
                            locOut.writeInt(loc.getY());
                            locOut.writeInt(loc.getZ());
                            locSize++;
                        }
                        languageItemsOut.writeShort(locSize);
                        languageItemsOut.write(locOut.toByteArray());
                        var langSize = 0;
                        val langOut = ByteStreams.newDataOutput();
                        for (val lang : languageList) {
                            val lines = sign.getLines(lang.getName());
                            if (lines == null) continue;
                            langOut.writeUTF(lang.getName());
                            langOut.writeUTF(lines[0]);
                            langOut.writeUTF(lines[1]);
                            langOut.writeUTF(lines[2]);
                            langOut.writeUTF(lines[3]);
                            langSize++;
                        }
                        languageItemsOut.writeShort(langSize);
                        languageItemsOut.write(langOut.toByteArray());
                    }
                    size++;
                }
            val out = ByteStreams.newDataOutput();
            out.writeByte(0);
            out.writeBoolean(firstSend);
            if (firstSend)
                out.write(languageOut.toByteArray());
            out.writeInt(size);
            out.write(languageItemsOut.toByteArray());
            info.sendData("triton:main", out.toByteArray());
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError("Failed to send config and language items to '%1' server! Not everything might work " +
                            "as expected! Error: %2", info.getName(), e.getMessage());
            if (Triton.get().getConf().getLogLevel() > 0)
                e.printStackTrace();
        }
    }

    public void sendPlayerLanguage(BungeeLanguagePlayer lp) {
        sendPlayerLanguage(lp, lp.getParent(), lp.getParent().getServer());
    }

    public void sendPlayerLanguage(@NonNull LanguagePlayer lp, @NonNull ProxiedPlayer player, @NonNull Server server) {
        val out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        server.sendData("triton:main", out.toByteArray());
    }

    public void sendExecutableCommand(String command, @NonNull Server server) {
        val out = ByteStreams.newDataOutput();
        // Action 2
        out.writeByte(2);
        out.writeUTF(command);
        server.sendData("triton:main", out.toByteArray());
    }

}
