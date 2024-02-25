package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class BridgeSerializer {

    /**
     * Actions from proxy to server
     */
    @RequiredArgsConstructor
    @Getter
    public enum ActionP2S {
        /** Send storage and config **/
        SEND_STORAGE_AND_CONFIG(0),
        /** Send a player's language **/
        SEND_PLAYER_LANGUAGE(1),
        /** Send command to be run as console **/
        SEND_COMMAND_AS_CONSOLE(2),
        /** Signal server to re-fetch translations from database **/
        SIGNAL_REFRESH_FROM_DB(3),
        /** Forward a Triton command to be run as the player themselves **/
        FORWARD_TRITON_COMMAND(4),
        ;

        private final int key;
    }

    /**
     * Actions from server to proxy
     */
    @RequiredArgsConstructor
    @Getter
    public enum ActionS2P {
        /** Update player's language **/
        UPDATE_PLAYER_LANGUAGE(0),
        /** Add or remove a sign (location) from a sign group **/
        UPDATE_SIGN_GROUP_MEMBERSHIP(1),
        ;

        private final int key;
    }

    public static byte[] getLanguageDataOutput() {
        val languageOut = ByteStreams.newDataOutput();
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
                refreshSignalOut.writeByte(ActionP2S.SIGNAL_REFRESH_FROM_DB.getKey());
                outList.add(refreshSignalOut.toByteArray());
                return outList;
            }

            val languageList = Triton.get().getLanguageManager().getAllLanguages();

            int size = 0;
            ByteArrayDataOutput languageItemsOut = ByteStreams.newDataOutput();
            for (val collection : Triton.get().getStorage().getCollections().values())
                for (val item : collection.getItems()) {
                    if (languageItemsOut.toByteArray().length > 29000) {
                        val out = ByteStreams.newDataOutput();
                        out.writeByte(ActionP2S.SEND_STORAGE_AND_CONFIG.getKey());
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
                        int langSize = 0;
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

                        int locSize = 0;
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
                        int langSize = 0;
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
            out.writeByte(ActionP2S.SEND_STORAGE_AND_CONFIG.getKey());
            val firstSend = outList.size() == 0;
            out.writeBoolean(firstSend);
            if (firstSend)
                out.write(languageOut);
            out.writeInt(size);
            out.write(languageItemsOut.toByteArray());
            outList.add(out.toByteArray());
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError(e, "Failed to send config and language items to '%1' server! Not everything might work " +
                            "as expected!", serverName);
        }
        return outList;
    }

    public static byte[] buildPlayerLanguageData(LanguagePlayer lp) {
        val out = ByteStreams.newDataOutput();
        out.writeByte(ActionP2S.SEND_PLAYER_LANGUAGE.getKey());
        val uuid = lp.getUUID();
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeUTF(lp.getLang().getName());
        return out.toByteArray();
    }

    public static byte[] buildExecutableCommandData(String command) {
        val out = ByteStreams.newDataOutput();
        out.writeByte(ActionP2S.SEND_COMMAND_AS_CONSOLE.getKey());
        out.writeUTF(command);
        return out.toByteArray();
    }

    public static byte[] buildForwardCommandData(CommandEvent commandEvent) {
        val out = ByteStreams.newDataOutput();
        out.writeByte(ActionP2S.FORWARD_TRITON_COMMAND.getKey());

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
