package com.rexcantor64.multilanguageplugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.multilanguageplugin.bridge.BungeeBridgeManager;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.language.item.LanguageItem;
import com.rexcantor64.multilanguageplugin.language.item.LanguageSign;
import com.rexcantor64.multilanguageplugin.language.item.LanguageText;
import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.multilanguageplugin.player.BungeeLanguagePlayer;
import com.rexcantor64.multilanguageplugin.plugin.PluginLoader;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BungeeMLP extends MultiLanguagePlugin {

    private HashMap<UUID, BungeeLanguagePlayer> players = new HashMap<>();

    public BungeeMLP(PluginLoader loader) {
        super.loader = loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        BungeeCord.getInstance().getPluginManager().registerListener(loader.asBungee(), new BungeeBridgeManager());
        BungeeCord.getInstance().registerChannel("MultiLanguagePlugin");

        for (ProxiedPlayer p : BungeeCord.getInstance().getPlayers())
            registerPlayer(p);

        sendConfigToEveryone();
    }

    public BungeeLanguagePlayer registerPlayer(ProxiedPlayer p) {
        BungeeLanguagePlayer lp = new BungeeLanguagePlayer(p);
        players.put(p.getUniqueId(), lp);
        return lp;
    }

    public BungeeLanguagePlayer getLanguagePlayer(ProxiedPlayer p) {
        return players.get(p.getUniqueId());
    }

    public BungeeLanguagePlayer getLanguagePlayer(UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public void reload() {
        super.reload();
        sendConfigToEveryone();
    }

    private void sendConfigToEveryone() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 0 (send config)
        out.write(0);
        out.writeUTF(MultiLanguagePlugin.get().getLanguageManager().getMainLanguage().getName());
        List<Language> languageList = MultiLanguagePlugin.get().getLanguageManager().getAllLanguages();
        out.writeShort(languageList.size());
        for (Language language : languageList) {
            out.writeUTF(language.getName());
            out.writeUTF(language.getRawDisplayName());
            out.writeUTF(language.getFlagCode());
            out.writeShort(language.getMinecraftCodes().size());
            for (String code : language.getMinecraftCodes())
                out.writeUTF(code);
        }
        // Send language files
        List<LanguageItem> languageItems = MultiLanguagePlugin.get().getLanguageConfig().getItems();
        out.writeInt(languageItems.size());
        for (LanguageItem item : languageItems) {
            switch (item.getType()) {
                case TEXT:
                    // Send type (0)
                    out.writeByte(0);
                    LanguageText text = (LanguageText) item;
                    out.writeUTF(text.getKey());
                    short langSize2 = 0;
                    ByteArrayDataOutput langOut2 = ByteStreams.newDataOutput();
                    for (Language lang : languageList) {
                        String msg = text.getMessage(lang.getName());
                        if (msg == null) continue;
                        langOut2.writeUTF(lang.getName());
                        langOut2.writeUTF(msg);
                        langSize2++;
                    }
                    out.writeShort(langSize2);
                    out.write(langOut2.toByteArray());
                    break;
                case SIGN:
                    // Send type (1)
                    out.writeByte(1);
                    LanguageSign sign = (LanguageSign) item;
                    out.writeUTF(sign.getLocation().getWorld());
                    out.writeInt(sign.getLocation().getX());
                    out.writeInt(sign.getLocation().getY());
                    out.writeInt(sign.getLocation().getZ());
                    short langSize = 0;
                    ByteArrayDataOutput langOut = ByteStreams.newDataOutput();
                    for (Language lang : languageList) {
                        String[] lines = sign.getLines(lang.getName());
                        if (lines == null) continue;
                        langOut.writeUTF(lang.getName());
                        langOut.writeUTF(lines[0]);
                        langOut.writeUTF(lines[1]);
                        langOut.writeUTF(lines[2]);
                        langOut.writeUTF(lines[3]);
                        langSize++;
                    }
                    out.writeShort(langSize);
                    out.write(langOut.toByteArray());
                    break;
            }
        }

        for (ServerInfo info : BungeeCord.getInstance().getServers().values())
            info.sendData("MultiLanguagePlugin", out.toByteArray());
    }

    public ProtocolLibListener getProtocolLibListener() {
        return null;
    }

    public File getDataFolder() {
        return loader.asBungee().getDataFolder();
    }

}
