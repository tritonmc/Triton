package com.rexcantor64.multilanguageplugin.packetinterceptor;

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
import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.components.api.chat.BaseComponent;
import com.rexcantor64.multilanguageplugin.components.api.chat.TextComponent;
import com.rexcantor64.multilanguageplugin.components.chat.ComponentSerializer;
import com.rexcantor64.multilanguageplugin.language.LanguageParser;
import com.rexcantor64.multilanguageplugin.language.item.LanguageItem;
import com.rexcantor64.multilanguageplugin.language.item.LanguageSign;
import com.rexcantor64.multilanguageplugin.player.SpigotLanguagePlayer;
import com.rexcantor64.multilanguageplugin.wrappers.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@SuppressWarnings("deprecation")
public class ProtocolLibListener implements PacketListener, PacketInterceptor {

    private MultiLanguagePlugin main;

    private HashMap<World, HashMap<Integer, Entity>> entities = new HashMap<>();

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
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
            boolean ab = isActionbar(packet.getPacket());
            if (ab && main.getConf().isActionbars()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().write(0, msg);
            } else if (!ab && main.getConf().isChat()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                if (msg != null) {
                    msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
                    packet.getPacket().getChatComponents().write(0, msg);
                    return;
                }
                packet.getPacket().getModifier().write(1, toLegacy(main.getLanguageParser().parseChat(languagePlayer, fromLegacy((net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().read(1)))));
            }
        } else if (packet.getPacketType() == PacketType.Play.Server.TITLE && main.getConf().isTitles()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseTitle(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER && main.getConf().isTab()) {
            WrappedChatComponent header = packet.getPacket().getChatComponents().read(0);
            String headerJson = header.getJson();
            header.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(header.getJson()))));
            packet.getPacket().getChatComponents().write(0, header);
            WrappedChatComponent footer = packet.getPacket().getChatComponents().read(1);
            String footerJson = footer.getJson();
            footer.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(footer.getJson()))));
            packet.getPacket().getChatComponents().write(1, footer);
            languagePlayer.setLastTabHeader(headerJson);
            languagePlayer.setLastTabFooter(footerJson);
        } else if (packet.getPacketType() == PacketType.Play.Server.OPEN_WINDOW && main.getConf().isGuis()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        } else if (packet.getPacketType() == PacketType.Play.Server.ENTITY_METADATA && main.getConf().getHolograms().size() != 0) {
            Entity e = packet.getPacket().getEntityModifier(packet).readSafely(0);
            if (e == null || !main.getConf().getHolograms().contains(EntityType.fromBukkit(e.getType()))) return;
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
            packet.getPacket().getWatchableCollectionModifier().write(0, dwn);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_INFO && main.getConf().getHolograms().contains(EntityType.PLAYER)) {
            EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().read(0);
            if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
                return;
            List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().read(0);
            List<PlayerInfoData> dataListNew = new ArrayList<>();
            for (PlayerInfoData data : dataList) {
                WrappedGameProfile oldGP = data.getProfile();
                WrappedGameProfile newGP = oldGP.withName(main.getLanguageParser().replaceLanguages(oldGP.getName(), languagePlayer));
                newGP.getProperties().putAll(oldGP.getProperties());
                WrappedChatComponent msg = data.getDisplayName();
                if (msg != null)
                    msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
                dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
            }
            packet.getPacket().getPlayerInfoDataLists().write(0, dataListNew);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE && main.getConf().isScoreboards()) {
            packet.getPacket().getStrings().write(1, main.getLanguageParser().replaceLanguages(packet.getPacket().getStrings().read(1), languagePlayer));
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE && main.getConf().isScoreboards()) {
            StructureModifier<String> strings = packet.getPacket().getStrings();
            String name = strings.read(0);
            LanguageParser utils = main.getLanguageParser();
            if (utils.hasLanguages(name)) {
                strings.write(0, utils.replaceLanguages(name, languagePlayer));
                for (Team team : packet.getPlayer().getScoreboard().getTeams())
                    for (String entry : team.getEntries())
                        if (entry.equals(name))
                            team.addEntry(utils.replaceLanguages(name, languagePlayer));
                return;
            }
            for (Team team : packet.getPlayer().getScoreboard().getTeams()) {
                for (String entry : team.getEntries())
                    if (entry.equals(name)) {
                        if (utils.hasLanguages(team.getPrefix() + name)) {
                            String translate = utils.replaceLanguages(team.getPrefix() + name, languagePlayer);
                            int i = translate.length() - (name.length() > translate.length()
                                    ? (translate.length() < 16 ? translate.length() - 1 : 16) : name.length());
                            String newTeamPrefix = translate.substring(0, i);
                            String newName = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            strings.write(0, utils.replaceLanguages(newName, languagePlayer));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, team.getSuffix());
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(name + team.getSuffix())) {
                            String translate = utils.replaceLanguages(name + team.getSuffix(), languagePlayer);
                            int i = name.length() > translate.length() ? translate.length() - 1 : name.length();
                            String newName = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newName) + translate.substring(i);
                            strings.write(0, utils.replaceLanguages(newName, languagePlayer));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    team.getPrefix(), newTeamSuffix);
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + name + team.getSuffix())) {
                            String translate = utils.replaceLanguages(team.getPrefix() + name + team.getSuffix(), languagePlayer);
                            int i = (translate.length() - (name.length() > translate.length()
                                    ? (translate.length() < 16 ? translate.length() - 1 : 16) : name.length())) / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newName = utils.getLastColor(newTeamPrefix)
                                    + translate.substring(i, i + name.length());
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix + newName)
                                    + translate.substring(i + name.length());
                            strings.write(0, utils.replaceLanguages(newName, languagePlayer));
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            team.addEntry(newName);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + team.getSuffix())) {
                            String translate = utils.replaceLanguages(team.getPrefix() + team.getSuffix(), languagePlayer);
                            int i = translate.length() / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            return;
                        }
                        if (utils.hasLanguages(team.getPrefix() + utils.removeFirstColor(team.getSuffix())) && main.getConf().isScoreboardsAdvanced()) {
                            String translate = utils.replaceLanguages(team.getPrefix() + utils.removeFirstColor(team.getSuffix()), languagePlayer);
                            int i = translate.length() / 2;
                            String newTeamPrefix = translate.substring(0, i);
                            String newTeamSuffix = utils.getLastColor(newTeamPrefix) + translate.substring(i);
                            updateTeamPrefixSuffix(packet.getPlayer(), team,
                                    newTeamPrefix, newTeamSuffix);
                            return;
                        }
                    }
            }
            strings.write(0, utils.replaceLanguages(strings.read(0), languagePlayer));
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM && main.getConf().isScoreboards()) {
            StructureModifier<String> strings = packet.getPacket().getStrings();
            String prefix = strings.read(2);
            String suffix = strings.read(3);
            String checkSuffix = suffix;
            if (!main.getLanguageParser().hasLanguages(suffix) && !main.getLanguageParser().hasLanguages(prefix + suffix) && main.getConf().isScoreboardsAdvanced())
                checkSuffix = main.getLanguageParser().removeFirstColor(checkSuffix);
            strings.write(1, main.getLanguageParser().replaceLanguages(strings.read(1), languagePlayer));
            if (main.getLanguageParser().hasLanguages(prefix + checkSuffix)) {
                String translate = main.getLanguageParser().replaceLanguages(prefix + checkSuffix, languagePlayer);
                int i = translate.length() / 2;
                String newTeamPrefix = translate.substring(0, i);
                String newTeamSuffix = main.getLanguageParser().getLastColor(newTeamPrefix)
                        + translate.substring(i);
                strings.write(2, newTeamPrefix);
                strings.write(3, newTeamSuffix);
                return;
            }
            strings.write(2, main.getLanguageParser().replaceLanguages(prefix, languagePlayer));
            strings.write(3, main.getLanguageParser().replaceLanguages(suffix, languagePlayer));
        } else if (packet.getPacketType() == PacketType.Play.Server.KICK_DISCONNECT && main.getConf().isKick()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
            msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseSimpleBaseComponent(languagePlayer, ComponentSerializer.parse(msg.getJson()))));
            packet.getPacket().getChatComponents().write(0, msg);
        } else if (signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.UPDATE_SIGN && main.getConf().isSigns()) {
            BlockPosition pos = packet.getPacket().getBlockPositionModifier().read(0);
            String[] lines = main.getLanguageManager().getSign(languagePlayer, new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ()));
            if (lines == null) return;
            WrappedChatComponent[] comps = new WrappedChatComponent[4];
            for (int i = 0; i < 4; i++)
                comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
            packet.getPacket().getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).write(0, Arrays.asList(comps));
        } else if (!signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA && main.getConf().isSigns()) {
            if (packet.getPacket().getIntegers().read(0) == 9) {
                NbtCompound nbt = NbtFactory.asCompound(packet.getPacket().getNbtModifier().read(0));
                LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
                String[] sign = main.getLanguageManager().getSign(languagePlayer, l);
                if (sign != null)
                    for (int i = 0; i < 4; i++)
                        nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
            }
        } else if (!signUpdateExists() && packet.getPacketType() == PacketType.Play.Server.MAP_CHUNK && main.getConf().isSigns()) {
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
        } else if (packet.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS && main.getConf().isItems()) {
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
                packet.getPacket().getItemArrayModifier().write(0, items.toArray(new ItemStack[0]));
            else
                packet.getPacket().getItemListModifier().write(0, items);
        } else if (packet.getPacketType() == PacketType.Play.Server.SET_SLOT && main.getConf().isItems()) {
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
            packet.getPacket().getItemModifier().write(0, item);
        } else if (packet.getPacketType() == PacketType.Play.Server.BOSS && main.getConf().isBossbars()) {
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
    }

    @Override
    public void refreshSigns(SpigotLanguagePlayer player) {
        for (LanguageItem item : main.getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            if (player.toBukkit().getWorld().equals(sign.getLocation().getWorld())) {
                PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN);
                String[] lines = sign.getLines(player.getLang().getName());
                if (lines == null) lines = sign.getLines(main.getLanguageManager().getMainLanguage().getName());
                if (lines == null) continue;
                packet.getBlockPositionModifier().write(0, new BlockPosition(sign.getLocation().getX(), sign.getLocation().getY(), sign.getLocation().getZ()));
                if (signUpdateExists()) {
                    WrappedChatComponent[] comps = new WrappedChatComponent[4];
                    for (int i = 0; i < 4; i++)
                        comps[i] = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
                    packet.getModifier().withType(MinecraftReflection.getIChatBaseComponentArrayClass(), BukkitConverters.getArrayConverter(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter())).write(0, Arrays.asList(comps));
                } else {
                    packet.getIntegers().write(0, 9);
                    NbtCompound compound = NbtFactory.ofCompound(null);
                    compound.put("x", sign.getLocation().getX());
                    compound.put("y", sign.getLocation().getY());
                    compound.put("z", sign.getLocation().getZ());
                    compound.put("id", getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
                    for (int i = 0; i < 4; i++)
                        compound.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
                    packet.getNbtModifier().write(0, compound);
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
                packet.getIntegers().write(0, entry.getKey());
                String oldName = entry.getValue().getCustomName();
                WrappedDataWatcher dw = WrappedDataWatcher.getEntityWatcher(entry.getValue());
                dw.setObject(2, main.getLanguageParser().replaceLanguages(entry.getValue().getCustomName(), player));
                packet.getWatchableCollectionModifier().write(0, dw.getWatchableObjects());
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
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Server.CHAT, PacketType.Play.Server.TITLE, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.OPEN_WINDOW, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.SCOREBOARD_OBJECTIVE, PacketType.Play.Server.SCOREBOARD_SCORE, PacketType.Play.Server.SCOREBOARD_TEAM, PacketType.Play.Server.KICK_DISCONNECT, PacketType.Play.Server.UPDATE_SIGN, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT, getMCVersion() >= 9 ? PacketType.Play.Server.BOSS : PacketType.Play.Server.CHAT).highest().build();
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

    private int getMCVersionR() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        return Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[2].substring(1));
    }

    private boolean signUpdateExists() {
        return getMCVersion() == 8 || (getMCVersion() == 9 && getMCVersionR() == 1);
    }

    public static enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES;
    }
}
