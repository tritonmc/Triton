package com.rexcantor64.triton.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.components.api.chat.BaseComponent;
import com.rexcantor64.triton.components.api.chat.TextComponent;
import com.rexcantor64.triton.components.chat.ComponentSerializer;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.scoreboard.TObjective;
import com.rexcantor64.triton.scoreboard.TTeam;
import com.rexcantor64.triton.utils.NMSUtils;
import com.rexcantor64.triton.wrappers.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@SuppressWarnings("deprecation")
public class ProtocolLibListener implements PacketListener, PacketInterceptor {

    private final int mcVersion;
    private final int mcVersionR;

    private MultiLanguagePlugin main;

    private HashMap<World, HashMap<Integer, Entity>> entities = new HashMap<>();

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
        String a = Bukkit.getServer().getClass().getPackage().getName();
        mcVersion = Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[1]);
        mcVersionR = Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[2].substring(1));
    }

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());
        if (ab && main.getConf().isActionbars()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()), main.getConf().getActionbarSyntax())));
            packet.getPacket().getChatComponents().writeSafely(0, msg);
        } else if (!ab && main.getConf().isChat()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, main.getConf().getChatSyntax(), ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().writeSafely(0, msg);
                return;
            }
            packet.getPacket().getModifier().writeSafely(1, toLegacy(main.getLanguageParser().parseChat(languagePlayer, main.getConf().getChatSyntax(), fromLegacy((net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().readSafely(1)))));
        }
    }

    private void handleTitle(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        if (msg == null) return;
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(languagePlayer, ComponentSerializer.parse(msg.getJson()), main.getConf().getTitleSyntax())));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handlePlayerListHeaderFooter(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent header = packet.getPacket().getChatComponents().readSafely(0);
        String headerJson = header.getJson();
        header.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(header.getJson()), main.getConf().getTabSyntax())));
        packet.getPacket().getChatComponents().writeSafely(0, header);
        WrappedChatComponent footer = packet.getPacket().getChatComponents().readSafely(1);
        String footerJson = footer.getJson();
        footer.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(footer.getJson()), main.getConf().getTabSyntax())));
        packet.getPacket().getChatComponents().writeSafely(1, footer);
        languagePlayer.setLastTabHeader(headerJson);
        languagePlayer.setLastTabFooter(footerJson);
    }

    private void handleOpenWindow(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, main.getConf().getGuiSyntax(), ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    @SuppressWarnings("unchecked")
    private void handleEntityMetadata(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        Entity e = packet.getPacket().getEntityModifier(packet).readSafely(0);
        if (e == null || (!main.getConf().isHologramsAll() && !main.getConf().getHolograms().contains(EntityType.fromBukkit(e.getType()))))
            return;
        if (e.getType() == org.bukkit.entity.EntityType.PLAYER) {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getUniqueId().equals(e.getUniqueId()))
                    return;
        }
        addEntity(packet.getPlayer().getWorld(), packet.getPacket().getIntegers().readSafely(0), e);
        List<WrappedWatchableObject> dw = packet.getPacket().getWatchableCollectionModifier().readSafely(0);
        List<WrappedWatchableObject> dwn = new ArrayList<>();
        for (WrappedWatchableObject obj : dw)
            if (obj.getIndex() == 2)
                if (getMCVersion() < 9)
                    dwn.add(new WrappedWatchableObject(obj.getIndex(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), languagePlayer, main.getConf().getHologramSyntax())));
                else if (getMCVersion() < 13)
                    dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), languagePlayer, main.getConf().getHologramSyntax())));
                else {
                    Optional optional = (Optional) obj.getValue();
                    if (optional.isPresent()) {
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), Optional.of(WrappedChatComponent.fromJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, main.getConf().getHologramSyntax(), ComponentSerializer.parse(WrappedChatComponent.fromHandle(optional.get()).getJson())))).getHandle())));
                    } else dwn.add(obj);
                }
            else
                dwn.add(obj);
        packet.getPacket().getWatchableCollectionModifier().writeSafely(0, dwn);
    }

    private void handlePlayerInfo(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().readSafely(0);
        if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
            return;
        List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().readSafely(0);
        List<PlayerInfoData> dataListNew = new ArrayList<>();
        for (PlayerInfoData data : dataList) {
            WrappedGameProfile oldGP = data.getProfile();
            WrappedGameProfile newGP = oldGP.withName(translate(languagePlayer, oldGP.getName(), 16, main.getConf().getHologramSyntax()));
            newGP.getProperties().putAll(oldGP.getProperties());
            WrappedChatComponent msg = data.getDisplayName();
            if (msg != null)
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()), main.getConf().getActionbarSyntax())));
            dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
        }
        packet.getPacket().getPlayerInfoDataLists().writeSafely(0, dataListNew);
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int mode = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        if (mode == 1) {
            languagePlayer.getScoreboard().removeObjective(name);
            return;
        }
        TObjective objective = null;
        if (mode == 0) {
            objective = new TObjective(name, "", false);
            languagePlayer.getScoreboard().addObjective(objective);
        } else if (mode == 2) {
            objective = languagePlayer.getScoreboard().getObjective(name);
        }
        if (objective == null)
            return;
        objective.setDisplayName(packet.getPacket().getStrings().readSafely(1));
        EnumScoreboardHealthDisplay criteria = packet.getPacket().getEnumModifier(EnumScoreboardHealthDisplay.class, 2).readSafely(0);
        objective.setHearts(criteria == EnumScoreboardHealthDisplay.HEARTS);
        if (objective.getDisplayName() != null && !objective.getDisplayName().isEmpty()) {
            String translatedDisplayName = translate(languagePlayer, objective.getDisplayName(), 32, main.getConf().getScoreboardSyntax());
            if (!translatedDisplayName.equals(objective.getDisplayName()))
                packet.getPacket().getStrings().writeSafely(1, translatedDisplayName);
        }
    }

    private void handleScoreboardObjectiveNew(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int mode = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        if (mode == 1) {
            languagePlayer.getScoreboard().removeObjective(name);
            return;
        }
        TObjective objective = null;
        if (mode == 0) {
            objective = new TObjective(name, "", false);
            languagePlayer.getScoreboard().addObjective(objective);
        } else if (mode == 2) {
            objective = languagePlayer.getScoreboard().getObjective(name);
        }
        if (objective == null)
            return;
        objective.setDisplayChat(ComponentSerializer.parse(packet.getPacket().getChatComponents().readSafely(0).getJson()));
        EnumScoreboardHealthDisplay criteria = packet.getPacket().getEnumModifier(EnumScoreboardHealthDisplay.class, 2).readSafely(0);
        objective.setHearts(criteria == EnumScoreboardHealthDisplay.HEARTS);
        if (objective.getDisplayChat() != null) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, main.getConf().getScoreboardSyntax(), objective.getDisplayChat())));
            packet.getPacket().getChatComponents().writeSafely(0, msg);
        }
    }

    private void handleScoreboardScore(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        TObjective objective = languagePlayer.getScoreboard().getObjective(strings.readSafely(1));
        String entry = strings.readSafely(0);
        EnumWrappers.ScoreboardAction action = packet.getPacket().getScoreboardActions().readSafely(0);
        if (action == EnumWrappers.ScoreboardAction.CHANGE) {
            if (objective == null)
                return;
            objective.setScore(entry, packet.getPacket().getIntegers().readSafely(0));
            TTeam team = languagePlayer.getScoreboard().getEntryTeam(entry);
            if ((team == null || !main.getConf().isScoreboardsAdvanced()) && main.getLanguageParser().hasLanguages(entry, main.getConf().getScoreboardSyntax())) {
                LanguageParser parser = main.getLanguageParser();
                if (!parser.hasLanguages(entry, main.getConf().getScoreboardSyntax()))
                    return;
                String[] translated = parser.toPacketFormatting(parser.removeDummyColors(parser.toScoreboardComponents(translate(languagePlayer, entry, main.getConf().getScoreboardSyntax()))), objective.getScore(entry));
                if (team != null) {
                    changeTeamEntries(packet.getPlayer(), team, true, entry);
                    changeTeamEntries(packet.getPlayer(), team, false, translated[1]);
                    updateTeamPrefixSuffix(packet.getPlayer(), team, translated[0], translated[2]);
                } else if (translated[0].length() != 0) {
                    team = new TTeam("MLPT" + languagePlayer.getLastTeamId(), "MLPT" + languagePlayer.increaseTeamId(), "", "", "always", "always", -1, Collections.singletonList(translated[1]), 0);
                    PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
                    StructureModifier<String> packetStrings = container.getStrings();
                    packetStrings.writeSafely(0, team.getName());
                    packetStrings.writeSafely(1, team.getDisplayName());
                    packetStrings.writeSafely(2, translated[0]);
                    packetStrings.writeSafely(3, translated[2]);
                    packetStrings.writeSafely(4, team.getVisibility());
                    packetStrings.writeSafely(5, team.getCollision());
                    StructureModifier<Integer> integers = container.getIntegers();
                    integers.writeSafely(0, team.getColor());
                    integers.writeSafely(1, 0);
                    integers.writeSafely(2, team.getOptionData());
                    container.getSpecificModifier(Collection.class).writeSafely(0, team.getEntries());
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(packet.getPlayer(), container, false);
                    } catch (Exception e) {
                        main.logError("Failed to send create team packet: %1", e.getMessage());
                    }
                }
                removeEntryScore(packet.getPlayer(), objective, entry);
                strings.writeSafely(0, translated[1]);
                objective.addTranslatedScore(translated[1]);
            } else if (team != null && main.getConf().isScoreboardsAdvanced()) {
                LanguageParser parser = main.getLanguageParser();
                String text = parser.scoreboardComponentToString(parser.removeDummyColors(parser.toScoreboardComponents(team.getPrefix() + entry + team.getSuffix())));
                if (!parser.hasLanguages(text, main.getConf().getScoreboardSyntax()))
                    return;
                String[] translated = parser.toPacketFormatting(parser.toScoreboardComponents(translate(languagePlayer, text, main.getConf().getScoreboardSyntax())), objective.getScore(entry));
                changeTeamEntries(packet.getPlayer(), team, true, entry);
                if (translated[0].length() != 0)
                    changeTeamEntries(packet.getPlayer(), team, false, translated[1]);
                updateTeamPrefixSuffix(packet.getPlayer(), team, translated[0], translated[2]);
                strings.writeSafely(0, translated[1]);
                objective.addTranslatedScore(translated[1]);
            }
        } else if (objective == null) {
            for (TObjective obj : languagePlayer.getScoreboard().getAllObjectives())
                obj.removeScore(entry);
            refreshScoreboard(languagePlayer);
        } else {
            objective.removeScore(entry);
            refreshScoreboard(languagePlayer);
        }
    }

    private void handleScoreboardScoreNew(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        TObjective objective = languagePlayer.getScoreboard().getObjective(strings.readSafely(1));
        String entry = strings.readSafely(0);
        EnumWrappers.ScoreboardAction action = packet.getPacket().getScoreboardActions().readSafely(0);
        if (action == EnumWrappers.ScoreboardAction.CHANGE) {
            if (objective == null)
                return;
            objective.setScore(entry, packet.getPacket().getIntegers().readSafely(0));
            //TODO check if has changed before refreshing
        } else if (objective == null)
            for (TObjective obj : languagePlayer.getScoreboard().getAllObjectives())
                obj.removeScore(entry);
        else
            objective.removeScore(entry);
        refreshScoreboard(languagePlayer);
    }

    @SuppressWarnings("unchecked")
    private void handleScoreboardTeam(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        StructureModifier<Integer> integers = packet.getPacket().getIntegers();
        String name = strings.readSafely(0);
        int mode = integers.readSafely(1);
        TTeam team = null;
        if (mode != 0 && mode != 1) {
            team = languagePlayer.getScoreboard().getTeam(name);
            if (team == null)
                return;
        }
        switch (mode) {
            case 1:
                languagePlayer.getScoreboard().removeTeam(name);
                break;
            case 0:
                team = new TTeam(name, (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0));
                languagePlayer.getScoreboard().addTeam(team);
            case 2:
                team.setDisplayName(strings.readSafely(1));
                team.setPrefix(strings.readSafely(2));
                team.setSuffix(strings.readSafely(3));
                team.setVisibility(strings.readSafely(4));
                team.setCollision(strings.readSafely(5));
                team.setColor(integers.readSafely(0));
                team.setOptionData(integers.readSafely(2));
                if (!main.getConf().isScoreboardsAdvanced()) {
                    strings.writeSafely(1, translate(languagePlayer, strings.readSafely(1), 32, main.getConf().getScoreboardSyntax()));
                    strings.writeSafely(2, translate(languagePlayer, strings.readSafely(2), 16, main.getConf().getScoreboardSyntax()));
                    strings.writeSafely(3, translate(languagePlayer, strings.readSafely(3), 16, main.getConf().getScoreboardSyntax()));
                }
                break;
            case 3:
                for (String entry : (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0))
                    team.addEntry(entry);
                break;
            case 4:
                for (String entry : (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0))
                    team.removeEntry(entry);
                break;
        }
        if (main.getConf().isScoreboardsAdvanced()) {
            if (mode != 1) {
                TObjective objective = languagePlayer.getScoreboard().getVisibleObjective();
                if (objective != null && team.getEntries().size() == 1) {
                    String entry = team.getEntries().get(0);
                    if (objective.getScore(entry) != null) {
                        LanguageParser utils = main.getLanguageParser();
                        String text = utils.scoreboardComponentToString(utils.removeDummyColors(utils.toScoreboardComponents(team.getPrefix() + entry + team.getSuffix())));
                        if (!utils.hasLanguages(text, main.getConf().getScoreboardSyntax()))
                            return;
                        String[] translated = utils.toPacketFormatting(utils.toScoreboardComponents(translate(languagePlayer, text, main.getConf().getScoreboardSyntax())), objective.getScore(entry));
                        if (!entry.equals(translated[1])) {
                            if (mode == 0 || mode == 3)
                                packet.getPacket().getSpecificModifier(Collection.class).writeSafely(0, Collections.singletonList(translated[1]));
                            if (mode == 4 || mode == 2) {
                                changeTeamEntries(packet.getPlayer(), team, true, entry);
                                changeTeamEntries(packet.getPlayer(), team, false, translated[1]);
                            }
                            removeEntryScore(packet.getPlayer(), objective, entry);
                            setEntryScore(packet.getPlayer(), objective, translated[1], objective.getScore(entry));
                            objective.addTranslatedScore(translated[1]);
                        }
                        if (mode == 0 || mode == 2) {
                            strings.writeSafely(2, translated[0]);
                            strings.writeSafely(3, translated[1]);
                        } else
                            updateTeamPrefixSuffix(packet.getPlayer(), team, translated[0], translated[2]);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleScoreboardTeamNew(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        StructureModifier<WrappedChatComponent> chat = packet.getPacket().getChatComponents();
        StructureModifier<Integer> integers = packet.getPacket().getIntegers();
        String name = strings.readSafely(0);
        int mode = integers.readSafely(0);
        TTeam team = null;
        if (mode != 0 && mode != 1) {
            team = languagePlayer.getScoreboard().getTeam(name);
            if (team == null)
                return;
        }
        switch (mode) {
            case 1:
                languagePlayer.getScoreboard().removeTeam(name);
                break;
            case 0:
                team = new TTeam(name, (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0));
                languagePlayer.getScoreboard().addTeam(team);
            case 2:
                team.setDisplayNameChat(ComponentSerializer.parse(chat.readSafely(0).getJson()));
                team.setPrefixChat(ComponentSerializer.parse(chat.readSafely(1).getJson()));
                team.setSuffixChat(ComponentSerializer.parse(chat.readSafely(2).getJson()));
                team.setVisibility(strings.readSafely(1));
                team.setCollision(strings.readSafely(2));
                team.setColor(packet.getPacket().getEnumModifier(TeamColor.class, 6).readSafely(0).id);
                team.setOptionData(integers.readSafely(1));
                break;
            case 3:
                for (String entry : (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0))
                    team.addEntry(entry);
                break;
            case 4:
                for (String entry : (Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0))
                    team.removeEntry(entry);
                break;
        }
        refreshScoreboard(languagePlayer);
    }

    private void handleScoreboardDisplayObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int position = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        for (TObjective obj : new ArrayList<>(languagePlayer.getScoreboard().getAllObjectives())) {
            if (obj.getName().equals(name))
                obj.setDisplayPosition(position);
            else if (obj.getDisplayPosition() == position)
                obj.setDisplayPosition(-1);
        }
        if (position == 1) {
            packet.setCancelled(true);
            refreshScoreboard(languagePlayer);
        }
    }

    private void handleKickDisconnect(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()), main.getConf().getKickSyntax())));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleUpdateSign(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        BlockPosition pos = packet.getPacket().getBlockPositionModifier().readSafely(0);
        String[] lines = main.getLanguageManager().getSign(languagePlayer, new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ()));
        if (lines == null) return;
        WrappedChatComponent[] comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; i++)
            comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
        packet.getPacket().getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).writeSafely(0, Arrays.asList(comps));
    }

    private void handleTileEntityData(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (packet.getPacket().getIntegers().readSafely(0) == 9) {
            NbtCompound nbt = NbtFactory.asCompound(packet.getPacket().getNbtModifier().readSafely(0));
            LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            String[] sign = main.getLanguageManager().getSign(languagePlayer, l);
            if (sign != null)
                for (int i = 0; i < 4; i++)
                    nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
        }
    }

    private void handleMapChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        List<NbtBase<?>> entities = packet.getPacket().getListNbtModifier().readSafely(0);
        for (NbtBase<?> entity : entities) {
            NbtCompound nbt = NbtFactory.asCompound(entity);
            if (nbt.getString("id").equals(getMCVersion() <= 10 ? "Sign" : "minecraft:sign")) {
                LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
                String[] sign = main.getLanguageManager().getSign(languagePlayer, l);
                if (sign != null)
                    for (int i = 0; i < 4; i++)
                        nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
            }
        }
    }

    private void handleWindowItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (NMSUtils.getNMSClass("ContainerPlayer") == NMSUtils.getDeclaredField(NMSUtils.getHandle(packet.getPlayer()), "activeContainer").getClass() && !main.getConf().isInventoryItems())
            return;

        List<ItemStack> items = getMCVersion() <= 10 ? Arrays.asList(packet.getPacket().getItemArrayModifier().readSafely(0)) : packet.getPacket().getItemListModifier().readSafely(0);
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName())
                    meta.setDisplayName(main.getLanguageParser().replaceLanguages(meta.getDisplayName(), languagePlayer, main.getConf().getItemsSyntax()));
                if (meta.hasLore()) {
                    List<String> newLore = new ArrayList<>();
                    for (String lore : meta.getLore())
                        newLore.add(main.getLanguageParser().replaceLanguages(lore, languagePlayer, main.getConf().getItemsSyntax()));
                    meta.setLore(newLore);
                }
                item.setItemMeta(meta);
            }
        }
        if (getMCVersion() <= 10)
            packet.getPacket().getItemArrayModifier().writeSafely(0, items.toArray(new ItemStack[0]));
        else
            packet.getPacket().getItemListModifier().writeSafely(0, items);
    }

    private void handleSetSlot(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (NMSUtils.getNMSClass("ContainerPlayer") == NMSUtils.getDeclaredField(NMSUtils.getHandle(packet.getPlayer()), "activeContainer").getClass() && !main.getConf().isInventoryItems())
            return;

        ItemStack item = packet.getPacket().getItemModifier().readSafely(0);
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(main.getLanguageParser().replaceLanguages(meta.getDisplayName(), languagePlayer, main.getConf().getItemsSyntax()));
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore())
                    newLore.add(main.getLanguageParser().replaceLanguages(lore, languagePlayer, main.getConf().getItemsSyntax()));
                meta.setLore(newLore);
            }
            item.setItemMeta(meta);
        }
        packet.getPacket().getItemModifier().writeSafely(0, item);
    }

    private void handleBoss(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        UUID uuid = packet.getPacket().getUUIDs().readSafely(0);
        Action action = packet.getPacket().getEnumModifier(Action.class, 1).readSafely(0);
        if (action == Action.REMOVE) {
            languagePlayer.removeBossbar(uuid);
            return;
        }
        if (action != Action.ADD && action != Action.UPDATE_NAME) return;
        WrappedChatComponent bossbar = packet.getPacket().getChatComponents().readSafely(0);
        languagePlayer.setBossbar(uuid, bossbar.getJson());
        bossbar.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(languagePlayer, ComponentSerializer.parse(bossbar.getJson()), main.getConf().getBossbarSyntax())));
        packet.getPacket().getChatComponents().writeSafely(0, bossbar);
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer = (SpigotLanguagePlayer) MultiLanguagePlugin.get().getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            MultiLanguagePlugin.get().logDebugWarning("Failed to translate packet because UUID of the player is unknown (because the player hasn't joined yet).");
            return;
        }
        if (languagePlayer == null) {
            MultiLanguagePlugin.get().logDebugWarning("Language Player is null on packet sending");
            return;
        }
        if (packet.getPacketType() == PacketType.Play.Server.CHAT) {
            handleChat(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.TITLE && main.getConf().isTitles()) {
            handleTitle(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER && main.getConf().isTab()) {
            handlePlayerListHeaderFooter(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.OPEN_WINDOW && main.getConf().isGuis()) {
            handleOpenWindow(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.ENTITY_METADATA && (main.getConf().isHologramsAll() || main.getConf().getHolograms().size() != 0)) {
            handleEntityMetadata(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_INFO && (main.getConf().isHologramsAll() || main.getConf().getHolograms().contains(EntityType.PLAYER))) {
            handlePlayerInfo(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE && main.getConf().isScoreboards()) {
            if (getMCVersion() < 13)
                handleScoreboardObjective(packet, languagePlayer);
            else
                handleScoreboardObjectiveNew(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE && main.getConf().isScoreboards()) {
            if (getMCVersion() < 13)
                handleScoreboardScore(packet, languagePlayer);
            else
                handleScoreboardScoreNew(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM && main.getConf().isScoreboards()) {
            if (getMCVersion() < 13)
                handleScoreboardTeam(packet, languagePlayer);
            else
                handleScoreboardTeamNew(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE && main.getConf().isScoreboards()) {
            handleScoreboardDisplayObjective(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.KICK_DISCONNECT && main.getConf().isKick()) {
            handleKickDisconnect(packet, languagePlayer);
        } else if (signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.UPDATE_SIGN && main.getConf().isSigns()) {
            handleUpdateSign(packet, languagePlayer);
        } else if (!signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA && main.getConf().isSigns()) {
            handleTileEntityData(packet, languagePlayer);
        } else if (!signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.MAP_CHUNK && main.getConf().isSigns()) {
            handleMapChunk(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS && main.getConf().isItems()) {
            handleWindowItems(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SET_SLOT && main.getConf().isItems()) {
            handleSetSlot(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.BOSS && main.getConf().isBossbars()) {
            handleBoss(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Login.Server.DISCONNECT && main.getConf().isKick()) {
            handleKickDisconnect(packet, languagePlayer);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {

    }

    @Override
    public void refreshSigns(SpigotLanguagePlayer player) {
        out:
        for (LanguageItem item : main.getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            for (LanguageSign.SignLocation location : sign.getLocations())
                if (player.toBukkit().getWorld().getName().equals(location.getWorld())) {
                    PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN);
                    String[] lines = sign.getLines(player.getLang().getName());
                    if (lines == null) lines = sign.getLines(main.getLanguageManager().getMainLanguage().getName());
                    if (lines == null) continue out;
                    packet.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(), location.getY(), location.getZ()));
                    if (signUpdateExists()) {
                        WrappedChatComponent[] comps = new WrappedChatComponent[4];
                        for (int i = 0; i < 4; i++)
                            comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
                        packet.getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).writeSafely(0, Arrays.asList(comps));
                    } else {
                        packet.getIntegers().writeSafely(0, 9);
                        NbtCompound compound = NbtFactory.ofCompound(null);
                        compound.put("x", location.getX());
                        compound.put("y", location.getY());
                        compound.put("z", location.getZ());
                        compound.put("id", getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
                        for (int i = 0; i < 4; i++)
                            compound.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
                        packet.getNbtModifier().writeSafely(0, compound);
                    }
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, false);
                    } catch (InvocationTargetException e) {
                        main.logError("Failed to send sign update packet: %1", e.getMessage());
                    }
                }
        }
    }

    @Override
    public void refreshEntities(SpigotLanguagePlayer player) {
        if (entities.containsKey(player.toBukkit().getWorld()))
            entityLoop:for (Map.Entry<Integer, Entity> entry : entities.get(player.toBukkit().getWorld()).entrySet()) {
                if (entry.getValue().getType() == org.bukkit.entity.EntityType.PLAYER) {
                    Player p = (Player) entry.getValue();
                    for (Player op : Bukkit.getOnlinePlayers())
                        if (op.getUniqueId().equals(p.getUniqueId())) continue entityLoop;
                    List<PlayerInfoData> dataList = new ArrayList<>();
                    dataList.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), 50, EnumWrappers.NativeGameMode.fromBukkit(p.getGameMode()), WrappedChatComponent.fromText(p.getPlayerListName())));
                    PacketContainer packetRemove = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                    packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                    packetRemove.getPlayerInfoDataLists().writeSafely(0, dataList);

                    PacketContainer packetAdd = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                    packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                    packetRemove.getPlayerInfoDataLists().writeSafely(0, dataList);

                    PacketContainer packetDestroy = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    packetDestroy.getIntegerArrays().writeSafely(0, new int[]{p.getEntityId()});

                    PacketContainer packetSpawn = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
                    packetSpawn.getIntegers().writeSafely(0, p.getEntityId());
                    packetSpawn.getUUIDs().writeSafely(0, p.getUniqueId());
                    packetSpawn.getDoubles().writeSafely(0, p.getLocation().getX()).writeSafely(1, p.getLocation().getY()).writeSafely(2, p.getLocation().getZ());
                    packetSpawn.getBytes().writeSafely(0, (byte) (int) (p.getLocation().getYaw() * 256.0F / 360.0F)).writeSafely(1, (byte) (int) (p.getLocation().getPitch() * 256.0F / 360.0F));
                    packetSpawn.getDataWatcherModifier().writeSafely(0, WrappedDataWatcher.getEntityWatcher(p));

                    PacketContainer packetRotation = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                    packetRotation.getIntegers().writeSafely(0, p.getEntityId());
                    packetRotation.getBytes().writeSafely(0, (byte) p.getLocation().getYaw());

                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetRemove, true);
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetAdd, false);
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetDestroy, true);
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetSpawn, true);
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetRotation, true);
                    } catch (InvocationTargetException e) {
                        main.logError("Failed to send player entity update packet: %1", e.getMessage());
                    }
                    continue;
                }
                if (entry.getValue().getCustomName() == null) continue;
                PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
                packet.getIntegers().writeSafely(0, entry.getKey());
                WrappedDataWatcher dw = WrappedDataWatcher.getEntityWatcher(entry.getValue());
                packet.getWatchableCollectionModifier().writeSafely(0, dw.getWatchableObjects());
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, true);
                } catch (InvocationTargetException e) {
                    main.logError("Failed to send entity update packet: %1", e.getMessage());
                }
            }
    }

    @Override
    public void refreshTabHeaderFooter(SpigotLanguagePlayer player, String header, String footer) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
        packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(header));
        packet.getChatComponents().writeSafely(1, WrappedChatComponent.fromJson(footer));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, true);
        } catch (InvocationTargetException e) {
            main.logError("Failed to send tab update packet: %1", e.getMessage());
        }
    }

    @Override
    public void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String json) {
        if (getMCVersion() <= 8) return;
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.BOSS);
        packet.getUUIDs().writeSafely(0, uuid);
        packet.getEnumModifier(Action.class, 1).writeSafely(0, Action.UPDATE_NAME);
        packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(json));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, true);
        } catch (InvocationTargetException e) {
            main.logError("Failed to send bossbar update packet: %1", e.getMessage());
        }
    }

    @Override
    public void refreshScoreboard(SpigotLanguagePlayer player) {
        if (getMCVersion() < 13)
            refreshScoreboardPre13(player);
        else
            refreshScoreboardPos13(player);
    }

    private void refreshScoreboardPre13(SpigotLanguagePlayer player) {
        for (TObjective objective : player.getScoreboard().getAllObjectives()) {
            if (objective.getDisplayPosition() != 1) continue;
            if (objective.getDisplayName() != null) {
                String translatedDisplayName = translate(player, objective.getDisplayName(), 32, main.getConf().getScoreboardSyntax());
                if (!translatedDisplayName.equals(objective.getDisplayName())) {

                    PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, true);
                    StructureModifier<String> strings = container.getStrings();
                    strings.writeSafely(0, objective.getName());
                    strings.writeSafely(1, translatedDisplayName);
                    container.getEnumModifier(EnumScoreboardHealthDisplay.class, 2).writeSafely(0, objective.isHearts() ? EnumScoreboardHealthDisplay.HEARTS : EnumScoreboardHealthDisplay.INTEGER);
                    container.getIntegers().writeSafely(0, 2);
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, false);
                    } catch (Exception e) {
                        main.logError("Failed to send refreshObjective packet: %1", e.getMessage());
                    }
                }
            }
            for (String entry : objective.getTranslatedScores()) removeEntryScore(player.toBukkit(), objective, entry);
            objective.clearTranslatedScores();
            for (Map.Entry<String, Integer> entry : objective.getScores()) {
                PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
                StructureModifier<String> strings = container.getStrings();
                strings.writeSafely(0, entry.getKey());
                strings.writeSafely(1, objective.getName());
                container.getIntegers().writeSafely(0, entry.getValue());
                container.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.CHANGE);
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, true);
                } catch (Exception e) {
                    main.logError("Failed to send refreshScore packet: %1", e.getMessage());
                }
            }
        }
    }

    private void refreshScoreboardPos13(SpigotLanguagePlayer player) {
        TObjective objective = player.getScoreboard().getVisibleObjective();
        if (objective == null) return;

        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, "TritonObj");
        StructureModifier<WrappedChatComponent> chats = container.getChatComponents();
        chats.writeSafely(0, WrappedChatComponent.fromJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(player, main.getConf().getScoreboardSyntax(), objective.getDisplayChat()))));
        container.getEnumModifier(EnumScoreboardHealthDisplay.class, 2).writeSafely(0, EnumScoreboardHealthDisplay.INTEGER);
        container.getIntegers().writeSafely(0, player.isScoreboardSetup() ? 2 : 0);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, false);
        } catch (Exception e) {
            main.logError("Failed to setup scoreboard objective (packet): %1", e.getMessage());
        }

        if (!player.isScoreboardSetup()) {
            container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, true);
            container.getStrings().writeSafely(0, "TritonObj");
            container.getIntegers().writeSafely(0, 1);
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, false);
            } catch (Exception e) {
                main.logError("Failed to setup scoreboard objective (packet display): %1", e.getMessage());
            }
            for (int i = 0; i < 15; i++)
                createTeam(player.toBukkit(), i);
            player.setScoreboardSetup(true);
        }

        List<String> scores = objective.getTopScores();
        for (int i = 0; i < 15; i++) {
            if (scores.size() > i) {
                container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
                strings = container.getStrings();
                strings.writeSafely(0, "§6§4" + getColorSuffix(i));
                strings.writeSafely(1, "TritonObj");
                container.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.CHANGE);
                container.getIntegers().writeSafely(0, 15 - i);
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, false);
                } catch (Exception e) {
                    main.logError("Failed to update scoreboard (packet update score): %1", e.getMessage());
                }
                BaseComponent[] component;
                TTeam team = player.getScoreboard().getEntryTeam(scores.get(i));
                if (team != null)
                    component = concatenate(concatenate(team.getPrefixChat(), TextComponent.fromLegacyText(scores.get(i))), team.getSuffixChat());
                else
                    component = TextComponent.fromLegacyText(scores.get(i));
                updateTeamPrefix(player.toBukkit(), i, main.getLanguageParser().parseChat(player, main.getConf().getScoreboardSyntax(), component));
            } else {
                container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
                strings = container.getStrings();
                strings.writeSafely(0, "§6§4" + getColorSuffix(i));
                strings.writeSafely(1, "TritonObj");
                container.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.REMOVE);
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), container, false);
                } catch (Exception e) {
                    main.logError("Failed to update scoreboard (packet remove score): %1", e.getMessage());
                }
            }
        }
    }

    @Override
    public void resetSign(Player p, LanguageSign.SignLocation location) {
        World world = Bukkit.getWorld(location.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
        if (block.getType() != Material.SIGN && block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN)
            return;
        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();
        if (signUpdateExists()) {
            PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN, true);
            container.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(), location.getY(), location.getZ()));
            container.getChatComponentArrays().writeSafely(0, new WrappedChatComponent[]{WrappedChatComponent.fromText(lines[0]), WrappedChatComponent.fromText(lines[1]), WrappedChatComponent.fromText(lines[2]), WrappedChatComponent.fromText(lines[3])});
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.logError("Failed refresh sign: %1", e.getMessage());
            }
        } else {
            PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TILE_ENTITY_DATA, true);
            container.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(), location.getY(), location.getZ()));
            container.getIntegers().writeSafely(0, 9); // Action (9): Update sign text
            NbtCompound nbt = NbtFactory.asCompound(container.getNbtModifier().readSafely(0));
            for (int i = 0; i < 4; i++)
                nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
            nbt.put("name", "null").put("x", block.getX()).put("y", block.getY()).put("z", block.getZ()).put("id", getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.logError("Failed refresh sign: %1", e.getMessage());
            }
        }
    }

    private void addEntity(World world, int id, Entity entity) {
        if (!entities.containsKey(world))
            entities.put(world, new HashMap<>());
        entities.get(world).put(id, entity);
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Server.CHAT, PacketType.Play.Server.TITLE, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.OPEN_WINDOW, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.SCOREBOARD_OBJECTIVE, PacketType.Play.Server.SCOREBOARD_SCORE, PacketType.Play.Server.SCOREBOARD_TEAM, PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, PacketType.Play.Server.KICK_DISCONNECT, PacketType.Play.Server.UPDATE_SIGN, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT, PacketType.Login.Server.DISCONNECT, getMCVersion() >= 9 ? PacketType.Play.Server.BOSS : PacketType.Play.Server.CHAT).highest().build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return main.getLoader().asSpigot();
    }

    private BaseComponent[] fromLegacy(net.md_5.bungee.api.chat.BaseComponent... components) {
        return ComponentSerializer.parse(net.md_5.bungee.chat.ComponentSerializer.toString(components));
    }

    private net.md_5.bungee.api.chat.BaseComponent[] toLegacy(BaseComponent... components) {
        return net.md_5.bungee.chat.ComponentSerializer.parse(ComponentSerializer.toString(components));
    }

    private boolean isActionbar(PacketContainer container) {
        if (getMCVersion() >= 12)
            return container.getChatTypes().readSafely(0) == EnumWrappers.ChatType.GAME_INFO;
        else
            return container.getBytes().readSafely(0) == 2;
    }

    private void createTeam(Player p, int i) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, "tritonteam_" + i);
        StructureModifier<WrappedChatComponent> chats = container.getChatComponents();
        for (int k = 0; k < 3; k++) {
            WrappedChatComponent field = chats.readSafely(k);
            field.setJson("{\"text\": \"\"}");
            chats.writeSafely(k, field);
        }
        strings.writeSafely(1, "always");
        strings.writeSafely(2, "always");
        StructureModifier<Integer> integers = container.getIntegers();
        container.getEnumModifier(TeamColor.class, 6).writeSafely(0, TeamColor.getById(15));
        integers.writeSafely(0, 0);
        container.getSpecificModifier(Collection.class).writeSafely(0, Collections.singleton("§6§4" + getColorSuffix(i)));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to refresh scoreboard (packet setup teams): %1", e.getMessage());
        }
    }

    private void updateTeamPrefix(Player p, int i, BaseComponent[] prefix) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, "tritonteam_" + i);
        StructureModifier<WrappedChatComponent> chats = container.getChatComponents();
        for (int k = 0; k < 3; k++) {
            WrappedChatComponent field = chats.readSafely(k);
            field.setJson(k == 1 ? ComponentSerializer.toString(prefix) : "{\"text\": \"\"}");
            chats.writeSafely(k, field);
        }
        strings.writeSafely(1, "always");
        strings.writeSafely(2, "always");
        StructureModifier<Integer> integers = container.getIntegers();
        container.getEnumModifier(TeamColor.class, 6).writeSafely(0, TeamColor.getById(15));
        integers.writeSafely(0, 2);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to refresh scoreboard (packet setup teams): %1", e.getMessage());
        }
    }

    private void updateTeamPrefixSuffix(Player p, TTeam team, String prefix, String suffix) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, team.getName());
        if (getMCVersion() < 13) {
            strings.writeSafely(1, team.getDisplayName());
            strings.writeSafely(2, prefix);
            strings.writeSafely(3, suffix);
        } else {
            StructureModifier<WrappedChatComponent> chats = container.getChatComponents();
            String[] a = new String[]{team.getDisplayName(), prefix, suffix};
            for (int i = 0; i < 3; i++) {
                WrappedChatComponent field = chats.readSafely(i);
                field.setJson(ComponentSerializer.toString(TextComponent.fromLegacyText(a[i])));
                chats.writeSafely(i, field);
            }
        }
        strings.writeSafely(getMCVersion() < 13 ? 4 : 1, team.getVisibility());
        strings.writeSafely(getMCVersion() < 13 ? 5 : 2, team.getCollision());
        StructureModifier<Integer> integers = container.getIntegers();
        if (getMCVersion() < 13)
            integers.writeSafely(0, team.getColor());
        else container.getEnumModifier(TeamColor.class, 6).writeSafely(0, TeamColor.getById(team.getColor()));
        integers.writeSafely(getMCVersion() < 13 ? 1 : 0, 2);
        integers.writeSafely(getMCVersion() < 13 ? 2 : 1, team.getOptionData());
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send updateTeamPrefixSuffix packet: %1", e.getMessage());
        }
    }

    private void changeTeamEntries(Player p, TTeam team, boolean remove, String... entries) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        container.getIntegers().writeSafely(getMCVersion() < 13 ? 1 : 0, remove ? 4 : 3);
        container.getStrings().writeSafely(0, team.getName());
        container.getSpecificModifier(Collection.class).writeSafely(0, Arrays.asList(entries));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send changeTeamEntries packet: %1", e.getMessage());
        }
    }

    private void removeEntryScore(Player p, TObjective objective, String entry) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, entry);
        strings.writeSafely(1, objective.getName());
        container.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.REMOVE);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send removeEntryScore packet: %1", e.getMessage());
        }
    }

    private void setEntryScore(Player p, TObjective objective, String entry, int score) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, entry);
        strings.writeSafely(1, objective.getName());
        container.getIntegers().writeSafely(0, score);
        container.getScoreboardActions().writeSafely(0, EnumWrappers.ScoreboardAction.CHANGE);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send setEntryScore packet: %1", e.getMessage());
        }
    }

    private int getMCVersion() {
        return mcVersion;
    }

    private int getMCVersionR() {
        return mcVersionR;
    }

    private boolean signUpdateExists() {
        return getMCVersion() == 8 || (getMCVersion() == 9 && getMCVersionR() == 1);
    }

    private String translate(LanguagePlayer lp, String s, int max, MainConfig.FeatureSyntax syntax) {
        String r = main.getLanguageParser().replaceLanguages(s, lp, syntax);
        if (r.length() > max) return r.substring(0, max);
        return r;
    }

    private String translate(LanguagePlayer lp, String s, MainConfig.FeatureSyntax syntax) {
        return main.getLanguageParser().replaceLanguages(s, lp, syntax);
    }

    public enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES
    }

    public enum EnumScoreboardHealthDisplay {
        HEARTS, INTEGER
    }

    private String getColorSuffix(int score) {
        if (score < 0)
            return "§l";
        if (score < 10)
            return "§" + score;
        if (score == 10)
            return "§a";
        if (score == 11)
            return "§b";
        if (score == 12)
            return "§c";
        if (score == 13)
            return "§d";
        if (score == 14)
            return "§e";
        if (score == 15)
            return "§f";
        return "§l";
    }

    private <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;
        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public enum TeamColor {
        BLACK(0),
        DARK_BLUE(1),
        DARK_GREEN(2),
        DARK_AQUA(3),
        DARK_RED(4),
        DARK_PURPLE(5),
        GOLD(6),
        GRAY(7),
        DARK_GRAY(8),
        BLUE(9),
        GREEN(10),
        AQUA(11),
        RED(12),
        LIGHT_PURPLE(13),
        YELLOW(14),
        WHITE(15),
        OBFUSCATED(16),
        BOLD(17),
        STRIKETHROUGH(18),
        UNDERLINE(19),
        ITALIC(20),
        RESET(21);

        private final int id;

        TeamColor(int id) {
            this.id = id;
        }

        static TeamColor getById(int id) {
            for (TeamColor c : values())
                if (c.id == id) return c;
            return RESET;
        }
    }

}
