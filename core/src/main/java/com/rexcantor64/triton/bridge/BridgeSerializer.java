package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.NonNull;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.List;

/**
 * Actions:
 * 0 -> send storage and config
 * 1 -> send player language
 * 2 -> send command to be ran as console
 * 3 -> tell server to re-fetch translations from database
 * 4 -> forward Triton command as player
 */
public class BridgeSerializer {

    public static byte[] getLanguageDataOutput() {
        val languageOut = ByteStreams.newDataOutput();
        // Action 0 (send config)
        languageOut.writeUTF(Triton.get().getLanguageManager().getMainLanguage().getName());
        val languageList = Triton.get().getLanguageManager().getAllLanguages();
        languageOut.writeShort(languageList.size());
        for (val language : languageList) {
            languageOut.writeUTF(language.getName());
            languageOut.writeUTF(language.getRawDisplayName());
            languageOut.writeUTF(language.getFlagCode());
            languageOut.writeShort(language.getFallbackLanguages().size());
            for (val fallbackLanguage : language.getFallbackLanguages())
                languageOut.writeUTF(fallbackLanguage);
            languageOut.writeShort(language.getMinecraftCodes().size());
            for (val code : language.getMinecraftCodes())
                languageOut.writeUTF(code);
        }

        return languageOut.toByteArray();
    }

    public static List<byte[]> buildTranslationData(String serverName, @NonNull byte[] languageOut) {
        List<byte[]> outList = new ArrayList<>();
        try {

            // If not using local storage, each server should fetch the translations for themselves
            if (!(Triton.get().getStorage() instanceof LocalStorage)) {
                val refreshSignalOut = ByteStreams.newDataOutput();
                // Action 3 (tell server to re-fetch from database storage)
                refreshSignalOut.writeByte(3);
                outList.add(refreshSignalOut.toByteArray());
                return outList;
            }

            val languageList = Triton.get().getLanguageManager().getAllLanguages();

            var size = 0;
            var languageItemsOut = ByteStreams.newDataOutput();
            for (val collection : Triton.get().getStorage().getCollections().values())
                for (val item : collection.getItems()) {
                    if (languageItemsOut.toByteArray().length > 29000) {
                        val out = ByteStreams.newDataOutput();
                        out.writeByte(0);
                        if (outList.size() == 0) {
                            out.writeBoolean(true);
                            out.write(languageOut);
                        } else {
                            out.writeBoolean(false);
                        }
                        out.writeInt(size);
                        out.write(languageItemsOut.toByteArray());
                        outList.add(out.toByteArray());
                        languageItemsOut = ByteStreams.newDataOutput();
                        size = 0;
                    }

                    if (item instanceof LanguageText) {
                        val text = (LanguageText) item;
                        if (!text.belongsToServer(collection.getMetadata(), serverName))
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
                        if (sign.getLocations() != null) {
                            for (val loc : sign.getLocations()) {
                                if (loc.getServer() != null && !loc.getServer().equals(serverName))
                                    continue;
                                locOut.writeUTF(loc.getWorld());
                                locOut.writeInt(loc.getX());
                                locOut.writeInt(loc.getY());
                                locOut.writeInt(loc.getZ());
                                locSize++;
                            }
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
            val firstSend = outList.size() == 0;
            out.writeBoolean(firstSend);
            if (firstSend)
                out.write(languageOut);
            out.writeInt(size);
            out.write(languageItemsOut.toByteArray());
            outList.add(out.toByteArray());
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError("Failed to send config and language items to '%1' server! Not everything might work " +
                            "as expected! Error: %2", serverName, e.getMessage());
            e.printStackTrace();
        }
        return outList;
    }

    public static byte[] buildPlayerLanguageData(LanguagePlayer lp) {
        val out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        val uuid = lp.getUUID();
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeUTF(lp.getLang().getName());
        return out.toByteArray();
    }

    public static byte[] buildExecutableCommandData(String command) {
        val out = ByteStreams.newDataOutput();
        // Action 2
        out.writeByte(2);
        out.writeUTF(command);
        return out.toByteArray();
    }

    public static byte[] buildForwardCommandData(CommandEvent commandEvent) {
        val out = ByteStreams.newDataOutput();
        // Action 4
        out.writeByte(4);

        val uuid = commandEvent.getSender().getUUID();
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());

        out.writeBoolean(commandEvent.getSubCommand() != null);
        if (commandEvent.getSubCommand() != null)
            out.writeUTF(commandEvent.getSubCommand());

        out.writeShort(commandEvent.getArgs().length);
        for (val arg : commandEvent.getArgs())
            out.writeUTF(arg);
        return out.toByteArray();
    }

}
