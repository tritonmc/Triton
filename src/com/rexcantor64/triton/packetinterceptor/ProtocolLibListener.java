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
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.scoreboard.TObjective;
import com.rexcantor64.triton.scoreboard.TTeam;
import com.rexcantor64.triton.wrappers.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@SuppressWarnings("deprecation")
public class ProtocolLibListener implements PacketListener, PacketInterceptor {

    private MultiLanguagePlugin main;

    private HashMap<World, HashMap<Integer, Entity>> entities = new HashMap<>();

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
    }

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());
        if (ab && main.getConf().isActionbars()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().writeSafely(0, msg);
        } else if (!ab && main.getConf().isChat()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            if (msg != null) {
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().writeSafely(0, msg);
                return;
            }
            packet.getPacket().getModifier().writeSafely(1, toLegacy(main.getLanguageParser().parseChat(languagePlayer, fromLegacy((net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().read(1)))));
        }
    }

    private void handleTitle(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handlePlayerListHeaderFooter(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent header = packet.getPacket().getChatComponents().read(0);
        String headerJson = header.getJson();
        header.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(header.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, header);
        WrappedChatComponent footer = packet.getPacket().getChatComponents().read(1);
        String footerJson = footer.getJson();
        footer.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(footer.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(1, footer);
        languagePlayer.setLastTabHeader(headerJson);
        languagePlayer.setLastTabFooter(footerJson);
    }

    private void handleOpenWindow(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleEntityMetadata(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        Entity e = packet.getPacket().getEntityModifier(packet).readSafely(0);
        if (e == null || (!main.getConf().isHologramsAll() && !main.getConf().getHolograms().contains(EntityType.fromBukkit(e.getType()))))
            return;
        addEntity(packet.getPlayer().getWorld(), packet.getPacket().getIntegers().read(0), e);
        List<WrappedWatchableObject> dw = packet.getPacket().getWatchableCollectionModifier().read(0);
        List<WrappedWatchableObject> dwn = new ArrayList<>();
        for (WrappedWatchableObject obj : dw)
            if (obj.getIndex() == 2)
                if (getMCVersion() < 9)
                    dwn.add(new WrappedWatchableObject(obj.getIndex(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), languagePlayer)));
                else
                    dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), main.getLanguageParser().replaceLanguages((String) obj.getValue(), languagePlayer)));
            else
                dwn.add(obj);
        packet.getPacket().getWatchableCollectionModifier().writeSafely(0, dwn);
    }

    private void handlePlayerInfo(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().read(0);
        if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
            return;
        List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().read(0);
        List<PlayerInfoData> dataListNew = new ArrayList<>();
        for (PlayerInfoData data : dataList) {
            WrappedGameProfile oldGP = data.getProfile();
            WrappedGameProfile newGP = oldGP.withName(translate(languagePlayer, oldGP.getName(), 16));
            newGP.getProperties().putAll(oldGP.getProperties());
            WrappedChatComponent msg = data.getDisplayName();
            if (msg != null)
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
            dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
        }
        packet.getPacket().getPlayerInfoDataLists().writeSafely(0, dataListNew);
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        packet.getPacket().getStrings().writeSafely(1, main.getLanguageParser().replaceLanguages(packet.getPacket().getStrings().read(1), languagePlayer));
        StructureModifier<String> strings = packet.getPacket().getStrings();
        int mode = packet.getPacket().getIntegers().readSafely(0);
        String name = strings.readSafely(0);
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
        objective.setDisplayName(strings.readSafely(1));
        EnumScoreboardHealthDisplay criteria = packet.getPacket().getEnumModifier(EnumScoreboardHealthDisplay.class, 2).readSafely(0);
        objective.setHearts(criteria == EnumScoreboardHealthDisplay.HEARTS);
        if (objective.getDisplayName() != null) {
            String translatedDisplayName = translate(languagePlayer, objective.getDisplayName(), 32);
            if (!translatedDisplayName.equals(objective.getDisplayName()))
                strings.writeSafely(1, translatedDisplayName);
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
            if ((team == null || !main.getConf().isScoreboardsAdvanced()) && main.getLanguageParser().hasLanguages(entry)) {
                LanguageParser parser = main.getLanguageParser();
                if (!parser.hasLanguages(entry))
                    return;
                String[] translated = parser.toPacketFormatting(parser.removeDummyColors(parser.toScoreboardComponents(translate(languagePlayer, entry))), objective.getScore(entry));
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
                if (!parser.hasLanguages(text))
                    return;
                String[] translated = parser.toPacketFormatting(parser.toScoreboardComponents(translate(languagePlayer, text)), objective.getScore(entry));
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
                    strings.writeSafely(1, translate(languagePlayer, strings.readSafely(1), 32));
                    strings.writeSafely(2, translate(languagePlayer, strings.readSafely(2), 16));
                    strings.writeSafely(3, translate(languagePlayer, strings.readSafely(3), 16));
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
                        if (!utils.hasLanguages(text))
                            return;
                        String[] translated = utils.toPacketFormatting(utils.toScoreboardComponents(translate(languagePlayer, text)), objective.getScore(entry));
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

    private void handleScoreboardDisplayObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int position = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        for (TObjective obj : languagePlayer.getScoreboard().getAllObjectives()) {
            if (obj.getName().equals(name))
                obj.setDisplayPosition(position);
            else if (obj.getDisplayPosition() == position)
                obj.setDisplayPosition(-1);
        }
    }

    private void handleKickDisconnect(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleUpdateSign(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        BlockPosition pos = packet.getPacket().getBlockPositionModifier().read(0);
        String[] lines = main.getLanguageManager().getSign(languagePlayer, new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ()));
        if (lines == null) return;
        WrappedChatComponent[] comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; i++)
            comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
        packet.getPacket().getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).writeSafely(0, Arrays.asList(comps));
    }

    private void handleTileEntityData(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (packet.getPacket().getIntegers().read(0) == 9) {
            NbtCompound nbt = NbtFactory.asCompound(packet.getPacket().getNbtModifier().read(0));
            LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            String[] sign = main.getLanguageManager().getSign(languagePlayer, l);
            if (sign != null)
                for (int i = 0; i < 4; i++)
                    nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
        }
    }

    private void handleMapChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        List<NbtBase<?>> entities = packet.getPacket().getListNbtModifier().read(0);
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
        List<ItemStack> items = getMCVersion() <= 10 ? Arrays.asList(packet.getPacket().getItemArrayModifier().read(0)) : packet.getPacket().getItemListModifier().read(0);
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName())
                    meta.setDisplayName(main.getLanguageParser().replaceLanguages(meta.getDisplayName(), languagePlayer));
                if (meta.hasLore()) {
                    List<String> newLore = new ArrayList<>();
                    for (String lore : meta.getLore())
                        newLore.add(main.getLanguageParser().replaceLanguages(lore, languagePlayer));
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
        ItemStack item = packet.getPacket().getItemModifier().read(0);
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(main.getLanguageParser().replaceLanguages(meta.getDisplayName(), languagePlayer));
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore())
                    newLore.add(main.getLanguageParser().replaceLanguages(lore, languagePlayer));
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
        bossbar.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(languagePlayer, ComponentSerializer.parse(bossbar.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, bossbar);
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        SpigotLanguagePlayer languagePlayer = null;
        try {
            languagePlayer = (SpigotLanguagePlayer) MultiLanguagePlugin.get().getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            MultiLanguagePlugin.get().logDebugWarning("Failed to translate packet because UUID of the player is unknown (because the player hasn't joined yet).");
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
            handleScoreboardObjective(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE && main.getConf().isScoreboards()) {
            handleScoreboardScore(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM && main.getConf().isScoreboards()) {
            handleScoreboardTeam(packet, languagePlayer);
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
        }
    }

    @Override
    public void refreshSigns(SpigotLanguagePlayer player) {
        for (LanguageItem item : main.getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            if (player.toBukkit().getWorld().getName().equals(sign.getLocation().getWorld())) {
                PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN);
                String[] lines = sign.getLines(player.getLang().getName());
                if (lines == null) lines = sign.getLines(main.getLanguageManager().getMainLanguage().getName());
                if (lines == null) continue;
                packet.getBlockPositionModifier().writeSafely(0, new BlockPosition(sign.getLocation().getX(), sign.getLocation().getY(), sign.getLocation().getZ()));
                if (signUpdateExists()) {
                    WrappedChatComponent[] comps = new WrappedChatComponent[4];
                    for (int i = 0; i < 4; i++)
                        comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
                    packet.getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).writeSafely(0, Arrays.asList(comps));
                } else {
                    packet.getIntegers().writeSafely(0, 9);
                    NbtCompound compound = NbtFactory.ofCompound(null);
                    compound.put("x", sign.getLocation().getX());
                    compound.put("y", sign.getLocation().getY());
                    compound.put("z", sign.getLocation().getZ());
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
            for (Map.Entry<Integer, Entity> entry : entities.get(player.toBukkit().getWorld()).entrySet()) {
                if (entry.getValue().getType() == org.bukkit.entity.EntityType.PLAYER) {
                    Player p = (Player) entry.getValue();
                    if (Bukkit.getOnlinePlayers().contains(p)) continue;
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
                String oldName = entry.getValue().getCustomName();
                WrappedDataWatcher dw = WrappedDataWatcher.getEntityWatcher(entry.getValue());
                dw.setObject(2, main.getLanguageParser().replaceLanguages(entry.getValue().getCustomName(), player));
                packet.getWatchableCollectionModifier().writeSafely(0, dw.getWatchableObjects());
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, false);
                    dw.setObject(2, oldName);
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
        for (TObjective objective : player.getScoreboard().getAllObjectives()) {
            if (objective.getDisplayPosition() != 1) continue;
            if (objective.getDisplayName() != null) {
                String translatedDisplayName = translate(player, objective.getDisplayName(), 32);
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

    private void addEntity(World world, int id, Entity entity) {
        if (!entities.containsKey(world))
            entities.put(world, new HashMap<>());
        entities.get(world).put(id, entity);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Server.CHAT, PacketType.Play.Server.TITLE, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.OPEN_WINDOW, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.SCOREBOARD_OBJECTIVE, PacketType.Play.Server.SCOREBOARD_SCORE, PacketType.Play.Server.SCOREBOARD_TEAM, PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, PacketType.Play.Server.KICK_DISCONNECT, PacketType.Play.Server.UPDATE_SIGN, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT, getMCVersion() >= 9 ? PacketType.Play.Server.BOSS : PacketType.Play.Server.CHAT).highest().build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Client.SETTINGS).build();
    }

    @Override
    public Plugin getPlugin() {
        return main.getLoader().asSpigot();
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

    private void updateTeamPrefixSuffix(Player p, TTeam team, String prefix, String suffix) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, team.getName());
        strings.writeSafely(1, team.getDisplayName());
        strings.writeSafely(2, prefix);
        strings.writeSafely(3, suffix);
        strings.writeSafely(4, team.getVisibility());
        strings.writeSafely(5, team.getCollision());
        StructureModifier<Integer> integers = container.getIntegers();
        integers.writeSafely(0, team.getColor());
        integers.writeSafely(1, 2);
        integers.writeSafely(2, team.getOptionData());
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
        } catch (Exception e) {
            main.logError("Failed to send updateTeamPrefixSuffix packet: %1", e.getMessage());
        }
    }

    private void changeTeamEntries(Player p, TTeam team, boolean remove, String... entries) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        container.getIntegers().writeSafely(1, remove ? 4 : 3);
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
        String a = Bukkit.getServer().getClass().getPackage().getName();
        return Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[1]);
    }

    private int getMCVersionR() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        return Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[2].substring(1));
    }

    private boolean signUpdateExists() {
        return getMCVersion() == 8 || (getMCVersion() == 9 && getMCVersionR() == 1);
    }

    private String translate(LanguagePlayer lp, String s, int max) {
        String r = main.getLanguageParser().replaceLanguages(s, lp);
        if (r.length() > max) return r.substring(0, max);
        return r;
    }

    private String translate(LanguagePlayer lp, String s) {
        return main.getLanguageParser().replaceLanguages(s, lp);
    }

    public enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES
    }

    public enum EnumScoreboardHealthDisplay {
        HEARTS, INTEGER
    }

}
