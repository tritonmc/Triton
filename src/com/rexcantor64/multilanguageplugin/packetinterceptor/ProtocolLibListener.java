package com.rexcantor64.multilanguageplugin.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.components.api.chat.BaseComponent;
import com.rexcantor64.multilanguageplugin.components.chat.ComponentSerializer;
import com.rexcantor64.multilanguageplugin.language.LanguageParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class ProtocolLibListener implements PacketListener {

    private SpigotMLP main;

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        if (packet.getPacketType() == PacketType.Play.Server.CHAT) {
            boolean ab = isActionbar(packet.getPacket());
            if (ab && main.getConf().isActionbars()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().write(0, msg);
            } else if (!ab && main.getConf().isChat()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                if (msg != null) {
                    msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
                    packet.getPacket().getChatComponents().write(0, msg);
                    return;
                }
                packet.getPacket().getModifier().write(1, toLegacy(main.getLanguageParser().parseChat(packet.getPlayer(), fromLegacy((net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().read(1)))));
            }
        } else if (packet.getPacketType() == PacketType.Play.Server.TITLE && main.getConf().isTitles()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER && main.getConf().isTab()) {
            WrappedChatComponent header = packet.getPacket().getChatComponents().read(0);
            header.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(header.getJson()))));
            packet.getPacket().getChatComponents().write(0, header);
            WrappedChatComponent footer = packet.getPacket().getChatComponents().read(1);
            footer.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(footer.getJson()))));
            packet.getPacket().getChatComponents().write(1, footer);
        } else if (packet.getPacketType() == PacketType.Play.Server.OPEN_WINDOW && main.getConf().isGuis()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        } else if (packet.getPacketType() == PacketType.Play.Server.ENTITY_METADATA && main.getConf().getHolograms().size() != 0) {
            Entity e = packet.getPacket().getEntityModifier(packet).readSafely(0);
            if (e == null || !main.getConf().getHolograms().contains(e.getType())) return;
            List<WrappedWatchableObject> dw = packet.getPacket().getWatchableCollectionModifier().read(0);
            List<WrappedWatchableObject> dwn = new ArrayList<>();
            for (WrappedWatchableObject obj : dw)
                if (obj.getIndex() == 2)
                    if (getMCVersion() < 9)
                        dwn.add(new WrappedWatchableObject(obj.getIndex(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), packet.getPlayer())));
                    else
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), packet.getPlayer())));
                else
                    dwn.add(obj);
            packet.getPacket().getWatchableCollectionModifier().write(0, dwn);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_INFO && main.getConf().getHolograms().contains(EntityType.PLAYER)) {
            EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().read(0);
            if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
                return;
            List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().read(0);
            List<PlayerInfoData> dataListNew = new ArrayList<>();
            for (PlayerInfoData data : dataList) {
                WrappedGameProfile oldGP = data.getProfile();
                WrappedGameProfile newGP = oldGP.withName(main.getLanguageParser().replaceLanguages(oldGP.getName(), packet.getPlayer()));
                newGP.getProperties().putAll(oldGP.getProperties());
                WrappedChatComponent msg = data.getDisplayName();
                if (msg != null)
                    msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
                dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
            }
            packet.getPacket().getPlayerInfoDataLists().write(0, dataListNew);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE && main.getConf().isScoreboards()) {
            packet.getPacket().getStrings().write(1, main.getLanguageParser().replaceLanguages(packet.getPacket().getStrings().read(1), packet.getPlayer()));
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE && main.getConf().isScoreboards()) {
            StructureModifier<String> strings = packet.getPacket().getStrings();
            String name = strings.read(0);
            LanguageParser utils = main.getLanguageParser();
            if (utils.hasLanguages(name)) {
                strings.write(0, utils.replaceLanguages(name, packet.getPlayer()));
                for (Team team : packet.getPlayer().getScoreboard().getTeams())
                    for (String entry : team.getEntries())
                        if (entry.equals(name))
                            team.addEntry(utils.replaceLanguages(name, packet.getPlayer()));
                return;
            }
            for (Team team : packet.getPlayer().getScoreboard().getTeams()) {
                for (String entry : team.getEntries())
                    if (entry.equals(name)) {
                        if (utils.hasLanguages(team.getPrefix() + name)) {
                            String translate = utils.replaceLanguages(team.getPrefix() + name, packet.getPlayer());
                            int i = translate.length() - (name.length() > translate.length()
                                    ? (translate.length() < 16 ? translate.length() - 1 : 16) : name.length());
                            String newTeamPrefix = translate.substring(0, i);
                            String newName = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            strings.write(0, utils.replaceLanguages(newName, packet.getPlayer()));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, team.getSuffix());
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(name + team.getSuffix())) {
                            String translate = utils.replaceLanguages(name + team.getSuffix(), packet.getPlayer());
                            int i = name.length() > translate.length() ? translate.length() - 1 : name.length();
                            String newName = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newName) + translate.substring(i);
                            strings.write(0, utils.replaceLanguages(newName, packet.getPlayer()));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    team.getPrefix(), newTeamSuffix);
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + name + team.getSuffix())) {
                            String translate = utils.replaceLanguages(team.getPrefix() + name + team.getSuffix(), packet.getPlayer());
                            int i = (translate.length() - (name.length() > translate.length()
                                    ? (translate.length() < 16 ? translate.length() - 1 : 16) : name.length())) / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newName = utils.getLastColor(newTeamPrefix)
                                    + translate.substring(i, i + name.length());
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix + newName)
                                    + translate.substring(i + name.length());
                            strings.write(0, utils.replaceLanguages(newName, packet.getPlayer()));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + team.getSuffix())) {
                            String translate = utils.replaceLanguages(team.getPrefix() + team.getSuffix(), packet.getPlayer());
                            int i = translate.length() / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + utils.removeFirstColor(team.getSuffix())) && main.getConf().isScoreboardsAdvanced()) {
                            String translate = utils.replaceLanguages(team.getPrefix() + utils.removeFirstColor(team.getSuffix()), packet.getPlayer());
                            int i = translate.length() / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            return;
                        }
                    }
            }
            strings.write(0, utils.replaceLanguages(strings.read(0), packet.getPlayer()));
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM && main.getConf().isScoreboards()) {
            StructureModifier<String> strings = packet.getPacket().getStrings();
            String prefix = strings.read(2);
            String suffix = strings.read(3);
            String checkSuffix = suffix;
            if (!main.getLanguageParser().hasLanguages(suffix) && !main.getLanguageParser().hasLanguages(prefix + suffix) && main.getConf().isScoreboardsAdvanced())
                checkSuffix = main.getLanguageParser().removeFirstColor(checkSuffix);
            strings.write(1, main.getLanguageParser().replaceLanguages(strings.read(1), packet.getPlayer()));
            if (main.getLanguageParser().hasLanguages(prefix + checkSuffix)) {
                String translate = main.getLanguageParser().replaceLanguages(prefix + checkSuffix, packet.getPlayer());
                int i = translate.length() / 2;
                String newTeamPrefix = translate.substring(0, i);
                String newTeamSuffix = main.getLanguageParser().getLastColor(newTeamPrefix)
                        + translate.substring(i);
                strings.write(2, newTeamPrefix);
                strings.write(3, newTeamSuffix);
                return;
            }
            strings.write(2, main.getLanguageParser().replaceLanguages(prefix, packet.getPlayer()));
            strings.write(3, main.getLanguageParser().replaceLanguages(suffix, packet.getPlayer()));
        } else if ((packet.getPacketType() == PacketType.Login.Server.DISCONNECT || packet.getPacketType() == PacketType.Play.Server.KICK_DISCONNECT) && main.getConf().isKick()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Server.CHAT, PacketType.Play.Server.TITLE, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.OPEN_WINDOW, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.SCOREBOARD_OBJECTIVE, PacketType.Play.Server.SCOREBOARD_SCORE, PacketType.Play.Server.SCOREBOARD_TEAM, PacketType.Login.Server.DISCONNECT, PacketType.Play.Server.KICK_DISCONNECT).highest().build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Client.SETTINGS).build();
    }

    @Override
    public Plugin getPlugin() {
        return main;
    }

    private BaseComponent[] fromLegacy(net.md_5.bungee.api.chat.BaseComponent[] components) {
        return ComponentSerializer.parse(net.md_5.bungee.chat.ComponentSerializer.toString(components));
    }

    private net.md_5.bungee.api.chat.BaseComponent[] toLegacy(BaseComponent[] components) {
        return net.md_5.bungee.chat.ComponentSerializer.parse(ComponentSerializer.toString(components));
    }

    private boolean isActionbar(PacketContainer container) {
        if (getMCVersion() >= 12)
            return container.getChatTypes().read(0) == EnumWrappers.ChatType.GAME_INFO;
        else
            return container.getBytes().read(0) == 2;
    }

    private void updateTeamPrefixSuffix(Player p, Team team, String prefix, String suffix) {
        System.out.println(team.getOption(Team.Option.NAME_TAG_VISIBILITY).name());
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.write(0, team.getName());
        strings.write(1, team.getDisplayName());
        strings.write(2, prefix);
        strings.write(3, suffix);
        strings.write(4, team.getOption(Team.Option.NAME_TAG_VISIBILITY).name());
        container.getIntegers().write(0, -1);
        container.getIntegers().write(1, 2);
        int data = 0;
        if (team.allowFriendlyFire())
            data |= 1;
        if (team.canSeeFriendlyInvisibles())
            data |= 2;
        container.getIntegers().write(2, data);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send team update packet: %1", e.getMessage());
        }
    }

    private int getMCVersion() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        return Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[1]);
    }
}
