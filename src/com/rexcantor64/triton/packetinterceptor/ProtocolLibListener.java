package com.rexcantor64.triton.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.MethodUtils;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.scoreboard.WrappedObjective;
import com.rexcantor64.triton.scoreboard.WrappedTeam;
import com.rexcantor64.triton.utils.EntityTypeUtils;
import com.rexcantor64.triton.utils.NMSUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation"})
public class ProtocolLibListener implements PacketListener, PacketInterceptor {
    private final Class<?> MERCHANT_RECIPE_LIST_CLASS;
    private final Class<?> CRAFT_MERCHANT_RECIPE_LIST_CLASS;
    private final int mcVersion;
    private final int mcVersionR;
    private Triton main;

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
        String a = Bukkit.getServer().getClass().getPackage().getName();
        String[] s = a.substring(a.lastIndexOf('.') + 1).split("_");
        mcVersion = Integer.parseInt(s[1]);
        mcVersionR = Integer.parseInt(s[2].substring(1));
        MERCHANT_RECIPE_LIST_CLASS = mcVersion >= 14 ? NMSUtils.getNMSClass("MerchantRecipeList") : null;
        CRAFT_MERCHANT_RECIPE_LIST_CLASS = mcVersion >= 14 ? NMSUtils
                .getCraftbukkitClass("inventory.CraftMerchantRecipe") : null;
    }

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());
        if (ab && main.getConf().isActionbars()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                msg.setJson(ComponentSerializer.toString(mergeComponents(main.getLanguageParser()
                        .parseComponent(languagePlayer, main.getConf().getActionbarSyntax(), ComponentSerializer
                                .parse(msg.getJson())))));
                packet.getPacket().getChatComponents().writeSafely(0, msg);
                return;
            }
            packet.getPacket().getModifier().writeSafely(1,
                    mergeComponents(main.getLanguageParser().parseComponent(languagePlayer,
                            main.getConf().getChatSyntax(),
                            (net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier()
                                    .readSafely(1))));
        } else if (!ab && main.getConf().isChat()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                msg.setJson(net.md_5.bungee.chat.ComponentSerializer.toString(main.getLanguageParser()
                        .parseComponent(languagePlayer, main.getConf()
                                .getChatSyntax(), net.md_5.bungee.chat.ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().writeSafely(0, msg);
                return;
            }
            net.md_5.bungee.api.chat.BaseComponent[] bc = main.getLanguageParser().parseComponent(languagePlayer,
                    main.getConf().getChatSyntax(),
                    (net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().readSafely(1));
            packet.getPacket().getModifier().writeSafely(1, bc);
        }
    }

    private void handleTitle(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        if (msg == null) return;
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTitleSyntax(), ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handlePlayerListHeaderFooter(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent header = packet.getPacket().getChatComponents().readSafely(0);
        String headerJson = header.getJson();
        header.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTabSyntax(), ComponentSerializer.parse(header.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, header);
        WrappedChatComponent footer = packet.getPacket().getChatComponents().readSafely(1);
        String footerJson = footer.getJson();
        footer.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTabSyntax(), ComponentSerializer.parse(footer.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(1, footer);
        languagePlayer.setLastTabHeader(headerJson);
        languagePlayer.setLastTabFooter(footerJson);
    }

    private void handleOpenWindow(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        msg.setJson(ComponentSerializer.toString(mergeComponents(main.getLanguageParser()
                .parseComponent(languagePlayer, main.getConf().getGuiSyntax(), ComponentSerializer
                        .parse(msg.getJson())))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleNamedEntitySpawn(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if ((!main.getConf().isHologramsAll() && !main.getConf().getHolograms()
                .contains(EntityType.PLAYER)))
            return;
        Entity e = packet.getPacket().getEntityModifier(packet).readSafely(0);
        addPlayer(packet.getPlayer().getWorld(), e.getEntityId(), e, languagePlayer);
    }

    private void handleSpawnEntityLiving(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);
        int type = packet.getPacket().getIntegers().readSafely(1);
        EntityType et = EntityTypeUtils.getEntityTypeById(type);
        if ((!main.getConf().isHologramsAll() && !main.getConf().getHolograms()
                .contains(et)))
            return;
        if (et == EntityType.PLAYER)
            return;
        addEntity(packet.getPlayer().getWorld(), entityId, null, languagePlayer);
        if (getMCVersion() >= 15) return; // DataWatcher is not sent on 1.15 anymore in this packet
        // Clone the data watcher, so we don't edit the display name permanently
        WrappedDataWatcher dataWatcher = new WrappedDataWatcher(new ArrayList<>(packet.getPacket()
                .getDataWatcherModifier()
                .readSafely(0).asMap().values()));
        WrappedWatchableObject watchableObject = dataWatcher.getWatchableObject(2);
        if (watchableObject == null) return;
        if (getMCVersion() >= 13) {
            Optional optional = (Optional) watchableObject.getValue();
            if (optional.isPresent()) {
                String displayName = WrappedChatComponent.fromHandle(optional.get()).getJson();
                addEntity(packet.getPlayer().getWorld(), entityId, displayName, languagePlayer);
                dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(),
                        Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                                .toString(main.getLanguageParser().parseComponent(languagePlayer, main.getConf()
                                        .getHologramSyntax(), ComponentSerializer
                                        .parse(displayName))))
                                .getHandle())));
            }
        } else if (getMCVersion() >= 9) {
            addEntity(packet.getPlayer().getWorld(), entityId, (String) watchableObject.getValue(), languagePlayer);
            dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(),
                    main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                    .matchPattern((String) watchableObject.getValue(), languagePlayer), languagePlayer,
                            main.getConf().getHologramSyntax())));
        } else {
            addEntity(packet.getPlayer().getWorld(), entityId, (String) watchableObject.getValue(), languagePlayer);
            dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getIndex(),
                    main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                    .matchPattern((String) watchableObject.getValue(), languagePlayer), languagePlayer,
                            main.getConf().getHologramSyntax())));
        }
        packet.getPacket().getDataWatcherModifier().writeSafely(0, dataWatcher);
    }

    private void handleSpawnEntity(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);
        EntityType et;
        if (getMCVersion() >= 14)
            et = EntityType.fromBukkit(packet.getPacket().getEntityTypeModifier().readSafely(0));
        else if (getMCVersion() >= 9)
            et = EntityTypeUtils.getEntityTypeByObjectId(packet.getPacket().getIntegers().readSafely(6));
        else
            et = EntityTypeUtils.getEntityTypeByObjectId(packet.getPacket().getIntegers().readSafely(9));
        if ((!main.getConf().isHologramsAll() && !main.getConf().getHolograms()
                .contains(et)))
            return;
        if (et == EntityType.PLAYER)
            return;
        addEntity(packet.getPlayer().getWorld(), entityId, null, languagePlayer);
    }

    private void handleEntityMetadata(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);

        HashMap<Integer, String> worldEntitesMap = languagePlayer.getEntitiesMap().get(packet.getPlayer().getWorld());
        if (worldEntitesMap == null || !worldEntitesMap.containsKey(entityId))
            return;

        List<WrappedWatchableObject> dw = packet.getPacket().getWatchableCollectionModifier().readSafely(0);
        List<WrappedWatchableObject> dwn = new ArrayList<>();
        for (WrappedWatchableObject obj : dw)
            if (obj.getIndex() == 2)
                if (getMCVersion() < 9) {
                    addEntity(packet.getPlayer().getWorld(), entityId, (String) obj.getValue(), languagePlayer);
                    dwn.add(new WrappedWatchableObject(obj.getIndex(),
                            main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                            .matchPattern((String) obj.getValue(), languagePlayer), languagePlayer,
                                    main.getConf().getHologramSyntax())));
                } else if (getMCVersion() < 13) {
                    addEntity(packet.getPlayer().getWorld(), entityId, (String) obj.getValue(), languagePlayer);
                    dwn.add(new WrappedWatchableObject(obj.getWatcherObject(),
                            main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                            .matchPattern((String) obj.getValue(), languagePlayer), languagePlayer,
                                    main.getConf().getHologramSyntax())));
                } else {
                    Optional optional = (Optional) obj.getValue();
                    if (optional.isPresent()) {
                        addEntity(packet.getPlayer().getWorld(), entityId, WrappedChatComponent
                                .fromHandle(optional.get())
                                .getJson(), languagePlayer);
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(),
                                Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                                        .toString(main.getLanguageParser().parseComponent(languagePlayer, main.getConf()
                                                .getHologramSyntax(), ComponentSerializer
                                                .parse(WrappedChatComponent.fromHandle(optional.get()).getJson()))))
                                        .getHandle())));
                    } else dwn.add(obj);
                }
            else
                dwn.add(obj);
        packet.getPacket().getWatchableCollectionModifier().writeSafely(0, dwn);
    }

    private void handleEntityDestroy(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int[] ids = packet.getPacket().getIntegerArrays().readSafely(0);
        removeEntities(languagePlayer.getEntitiesMap().get(languagePlayer.toBukkit().getWorld()), ids);
        removeEntities(languagePlayer.getPlayersMap().get(languagePlayer.toBukkit().getWorld()), ids);
    }

    private void handlePlayerInfo(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().readSafely(0);
        if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
            return;
        List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().readSafely(0);
        List<PlayerInfoData> dataListNew = new ArrayList<>();
        for (PlayerInfoData data : dataList) {
            WrappedGameProfile oldGP = data.getProfile();
            WrappedGameProfile newGP = oldGP.withName(translate(languagePlayer, oldGP.getName(), 16,
                    main.getConf().getHologramSyntax()));
            newGP.getProperties().putAll(oldGP.getProperties());
            WrappedChatComponent msg = data.getDisplayName();
            if (msg != null)
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                        main.getConf().getHologramSyntax(), ComponentSerializer.parse(msg.getJson()))));
            dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
        }
        packet.getPacket().getPlayerInfoDataLists().writeSafely(0, dataListNew);
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int mode = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        if (!main.getConf().isScoreboardsAdvanced()) {
            if (mode == 1) return;
            if (getMCVersion() < 13)
                packet.getPacket().getStrings().writeSafely(1, translate(languagePlayer,
                        packet.getPacket().getStrings().readSafely(1), 32, main.getConf().getScoreboardSyntax()));
            else
                packet.getPacket().getChatComponents().writeSafely(0,
                        WrappedChatComponent.fromJson(ComponentSerializer.toString(main.getLanguageParser()
                                .parseComponent(languagePlayer, main.getConf()
                                        .getScoreboardSyntax(), ComponentSerializer
                                        .parse(packet.getPacket().getChatComponents().readSafely(0).getJson())))));
        }
        if (mode == 1) {
            languagePlayer.getScoreboard().removeObjective(name);
            return;
        }
        WrappedObjective objective = null;
        if (mode == 0) {
            objective = languagePlayer.getScoreboard().createObjective(name);
        } else if (mode == 2) {
            objective = languagePlayer.getScoreboard().getObjective(name);
        }
        if (objective == null)
            return;
        if (getMCVersion() < 13) {
            objective.setTitle(packet.getPacket().getStrings().readSafely(1));
            languagePlayer.getScoreboard().getBridge().updateObjectiveTitle(translate(languagePlayer,
                    objective.getTitle(), 32, main.getConf().getScoreboardSyntax()));
        } else {
            objective.setTitleComp(ComponentSerializer
                    .parse(packet.getPacket().getChatComponents().readSafely(0).getJson()));
            languagePlayer.getScoreboard().getBridge().updateObjectiveTitle(ComponentSerializer
                    .toString(main.getLanguageParser()
                            .parseComponent(languagePlayer, main.getConf().getScoreboardSyntax(), objective
                                    .getTitleComp())));
        }
    }

    private void handleScoreboardScore(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        if (!main.getConf().isScoreboardsAdvanced()) {
            strings.writeSafely(0, translate(languagePlayer, strings.readSafely(0), 40,
                    main.getConf().getScoreboardSyntax()));
            return;
        }
        String objectiveString = strings.readSafely(1);
        if (objectiveString.isEmpty()) {
            for (WrappedObjective objective : languagePlayer.getScoreboard().getObjectives())
                objective.setScore(strings.readSafely(0),
                        packet.getPacket().getScoreboardActions()
                                .readSafely(0) == EnumWrappers.ScoreboardAction.CHANGE ? packet.getPacket()
                                .getIntegers().readSafely(0) : null);
        } else {
            WrappedObjective objective = languagePlayer.getScoreboard().getObjective(objectiveString);
            if (objective == null) {
                return;
            }
            objective.setScore(strings.readSafely(0),
                    packet.getPacket().getScoreboardActions().readSafely(0) == EnumWrappers.ScoreboardAction.CHANGE ?
                            packet.getPacket().getIntegers().readSafely(0) : null);
        }
        languagePlayer.getScoreboard().rerender(false);
    }

    @SuppressWarnings("unchecked")
    private void handleScoreboardTeam(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        StructureModifier<String> strings = packet.getPacket().getStrings();
        StructureModifier<WrappedChatComponent> components = packet.getPacket().getChatComponents();
        if (!main.getConf().isScoreboardsAdvanced()) {
            if (getMCVersion() < 13) {
                strings.writeSafely(2, translate(languagePlayer, strings.readSafely(2), 16,
                        main.getConf().getScoreboardSyntax()));
                strings.writeSafely(3, translate(languagePlayer, strings.readSafely(3), 16,
                        main.getConf().getScoreboardSyntax()));
            } else {
                components.writeSafely(1,
                        WrappedChatComponent.fromJson(ComponentSerializer.toString(main.getLanguageParser()
                                .parseComponent(languagePlayer, main.getConf()
                                        .getScoreboardSyntax(), ComponentSerializer
                                        .parse(components.readSafely(1).getJson())))));
                components.writeSafely(2,
                        WrappedChatComponent.fromJson(ComponentSerializer.toString(main.getLanguageParser()
                                .parseComponent(languagePlayer, main.getConf()
                                        .getScoreboardSyntax(), ComponentSerializer
                                        .parse(components.readSafely(2).getJson())))));
            }
            return;
        }
        StructureModifier<Integer> integers = packet.getPacket().getIntegers();
        String name = strings.readSafely(0);
        int mode = integers.readSafely(getMCVersion() < 13 ? 1 : 0);
        if (mode == 1) {
            languagePlayer.getScoreboard().removeTeam(name);
            return;
        }
        WrappedTeam team;
        if (mode == 0) team = languagePlayer.getScoreboard().createTeam(name);
        else team = languagePlayer.getScoreboard().getTeam(name);
        if (team == null)
            return;
        if (mode == 0 || mode == 2) {
            if (getMCVersion() < 13) {
                team.setPrefix(strings.readSafely(2));
                team.setSuffix(strings.readSafely(3));
            } else {
                team.setPrefixComp(ComponentSerializer.parse(components.readSafely(1).getJson()));
                team.setSuffixComp(ComponentSerializer.parse(components.readSafely(2).getJson()));
            }
        }
        if (mode == 0 || mode == 3)
            team.addEntry((Collection<String>) packet.getPacket().getSpecificModifier(Collection.class).readSafely(0));
        if (mode == 4)
            team.removeEntry((Collection<String>) packet.getPacket().getSpecificModifier(Collection.class)
                    .readSafely(0));
        languagePlayer.getScoreboard().rerender(false);
    }

    private void handleScoreboardDisplayObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConf().isScoreboardsAdvanced()) return;
        int position = packet.getPacket().getIntegers().readSafely(0);
        String name = packet.getPacket().getStrings().readSafely(0);
        if (position != 1 && position < 3) return;
        WrappedObjective obj = languagePlayer.getScoreboard().getObjective(name);
        if (obj == null) return;
        packet.setCancelled(true);
        languagePlayer.getScoreboard().setSidebarObjective(obj);
        languagePlayer.getScoreboard().rerender(false);
    }

    private void handleKickDisconnect(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getKickSyntax(), ComponentSerializer.parse(msg.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleUpdateSign(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        PacketContainer newPacket = packet.getPacket().shallowClone();
        BlockPosition pos = newPacket.getBlockPositionModifier().readSafely(0);
        StructureModifier<WrappedChatComponent[]> linesModifier = newPacket.getChatComponentArrays();
        WrappedChatComponent[] defaultLinesWrapped = linesModifier.readSafely(0);
        String[] defaultLines = new String[4];
        for (int i = 0; i < 4; i++)
            defaultLines[i] =
                    AdvancedComponent.fromBaseComponent(ComponentSerializer.parse(defaultLinesWrapped[i].getJson()))
                            .getText();
        String[] lines = main.getLanguageManager().getSign(languagePlayer,
                new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(),
                        pos.getZ()), defaultLines);
        if (lines == null) return;
        WrappedChatComponent[] comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; i++)
            comps[i] =
                    WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
        linesModifier.writeSafely(0, comps);
        packet.setPacket(newPacket);
    }

    private void handleTileEntityData(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (packet.getPacket().getIntegers().readSafely(0) == 9) {
            PacketContainer newPacket = packet.getPacket().deepClone();
            NbtCompound nbt = NbtFactory.asCompound(newPacket.getNbtModifier().readSafely(0));
            String[] defaultLines = new String[4];
            for (int i = 0; i < 4; i++)
                defaultLines[i] = AdvancedComponent
                        .fromBaseComponent(ComponentSerializer.parse(nbt.getStringOrDefault("Text" + (i + 1))))
                        .getText();
            LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(),
                    nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            String[] sign = main.getLanguageManager().getSign(languagePlayer, l, defaultLines);
            if (sign != null) {
                for (int i = 0; i < 4; i++)
                    nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
                packet.setPacket(newPacket);
            }
        }
    }

    private void handleMapChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        List<NbtBase<?>> entities = packet.getPacket().getListNbtModifier().readSafely(0);
        for (NbtBase<?> entity : entities) {
            NbtCompound nbt = NbtFactory.asCompound(entity);
            if (nbt.getString("id").equals(getMCVersion() <= 10 ? "Sign" : "minecraft:sign")) {
                String[] defaultLines = new String[4];
                for (int i = 0; i < 4; i++)
                    defaultLines[i] = AdvancedComponent
                            .fromBaseComponent(ComponentSerializer.parse(nbt.getStringOrDefault("Text" + (i + 1))))
                            .getText();
                LanguageSign.SignLocation l = new LanguageSign.SignLocation(packet.getPlayer().getWorld().getName(),
                        nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
                String[] sign = main.getLanguageManager().getSign(languagePlayer, l, defaultLines);
                if (sign != null)
                    for (int i = 0; i < 4; i++)
                        nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
            }
        }
    }

    private void handleWindowItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (NMSUtils.getNMSClass("ContainerPlayer") == NMSUtils
                .getDeclaredField(NMSUtils.getHandle(packet.getPlayer()), "activeContainer").getClass() && !main
                .getConf().isInventoryItems())
            return;

        List<ItemStack> items = getMCVersion() <= 10 ?
                Arrays.asList(packet.getPacket().getItemArrayModifier().readSafely(0)) :
                packet.getPacket().getItemListModifier().readSafely(0);
        for (ItemStack item : items)
            translateItemStack(item, languagePlayer, true);
        if (getMCVersion() <= 10)
            packet.getPacket().getItemArrayModifier().writeSafely(0, items.toArray(new ItemStack[0]));
        else
            packet.getPacket().getItemListModifier().writeSafely(0, items);
    }

    private void handleSetSlot(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (NMSUtils.getNMSClass("ContainerPlayer") == NMSUtils
                .getDeclaredField(NMSUtils.getHandle(packet.getPlayer()), "activeContainer").getClass() && !main
                .getConf().isInventoryItems())
            return;

        ItemStack item = packet.getPacket().getItemModifier().readSafely(0);
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(translate(meta.getDisplayName(), languagePlayer,
                        main.getConf().getItemsSyntax()));
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore())
                    newLore.add(translate(lore, languagePlayer,
                            main.getConf().getItemsSyntax()));
                meta.setLore(newLore);
            }
            item.setItemMeta(meta);
            if (item.getType() == Material.WRITTEN_BOOK && main.getConf().isBooks()) {
                NbtCompound compound = NbtFactory.asCompound(NbtFactory.fromItemTag(item));
                NbtList<String> pages = compound.getList("pages");
                Collection<NbtBase<String>> pagesCollection = pages.asCollection();
                List<String> newPagesCollection = new ArrayList<>();
                for (NbtBase<String> page : pagesCollection) {
                    if (page.getValue().startsWith("\""))
                        newPagesCollection.add(
                                ComponentSerializer.toString(
                                        TextComponent.fromLegacyText(
                                                translate(page.getValue().substring(1
                                                        , page.getValue().length() - 1),
                                                        languagePlayer, main.getConf().getItemsSyntax()))));
                    else {
                        newPagesCollection.add(
                                ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                                        main.getConf().getItemsSyntax(), ComponentSerializer.parse(page.getValue()))));
                    }
                }
                compound.put("pages", NbtFactory.ofList("pages", newPagesCollection));
            }
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
        bossbar.setJson(ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getBossbarSyntax(), ComponentSerializer.parse(bossbar.getJson()))));
        packet.getPacket().getChatComponents().writeSafely(0, bossbar);
    }

    private void handleMerchantItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        try {
            ArrayList<?> recipes = (ArrayList<?>) packet.getPacket()
                    .getSpecificModifier(MERCHANT_RECIPE_LIST_CLASS).readSafely(0);
            ArrayList<Object> newRecipes = (ArrayList<Object>) MERCHANT_RECIPE_LIST_CLASS.newInstance();
            for (Object recipeObject : recipes) {
                MerchantRecipe recipe = (MerchantRecipe) NMSUtils.getMethod(recipeObject, "asBukkit");
                MerchantRecipe newRecipe = new MerchantRecipe(translateItemStack(recipe.getResult()
                        .clone(), languagePlayer, false), recipe.getUses(), recipe.getMaxUses(), recipe
                        .hasExperienceReward());
                for (ItemStack ingredient : recipe.getIngredients())
                    newRecipe.addIngredient(translateItemStack(ingredient.clone(), languagePlayer, false));
                Object newCraftRecipe = MethodUtils
                        .invokeExactStaticMethod(CRAFT_MERCHANT_RECIPE_LIST_CLASS, "fromBukkit", newRecipe);
                Object newNMSRecipe = MethodUtils.invokeExactMethod(newCraftRecipe, "toMinecraft", null);
                newRecipes.add(newNMSRecipe);
            }
            packet.getPacket().getModifier().writeSafely(1, newRecipes);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer =
                    (SpigotLanguagePlayer) Triton.get().getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            Triton.get().logDebugWarning("Failed to translate packet because UUID of the player is unknown (because " +
                    "the player hasn't joined yet).");
            return;
        }
        if (languagePlayer == null) {
            Triton.get().logDebugWarning("Language Player is null on packet sending");
            return;
        }
        if (packet.getPacketType() == PacketType.Play.Server.CHAT) {
            handleChat(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.TITLE && main.getConf().isTitles()) {
            handleTitle(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER && main.getConf()
                .isTab()) {
            handlePlayerListHeaderFooter(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.OPEN_WINDOW && main.getConf().isGuis()) {
            handleOpenWindow(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().contains(EntityType.PLAYER))) {
            handleNamedEntitySpawn(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY_LIVING && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().size() != 0)) {
            handleSpawnEntityLiving(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().size() != 0)) {
            handleSpawnEntity(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.ENTITY_METADATA && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().size() != 0)) {
            handleEntityMetadata(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.ENTITY_DESTROY && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().size() != 0)) {
            handleEntityDestroy(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.PLAYER_INFO && (main.getConf()
                .isHologramsAll() || main.getConf().getHolograms().contains(EntityType.PLAYER))) {
            handlePlayerInfo(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE && main.getConf()
                .isScoreboards()) {
            handleScoreboardObjective(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_SCORE && main.getConf()
                .isScoreboards()) {
            handleScoreboardScore(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM && main.getConf().isScoreboards()) {
            handleScoreboardTeam(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE && main.getConf()
                .isScoreboards()) {
            handleScoreboardDisplayObjective(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.KICK_DISCONNECT && main.getConf().isKick()) {
            handleKickDisconnect(packet, languagePlayer);
        } else if (existsSignUpdatePacket() && packet.getPacketType() == PacketType.Play.Server.UPDATE_SIGN && main
                .getConf().isSigns()) {
            handleUpdateSign(packet, languagePlayer);
        } else if (!existsSignUpdatePacket() && packet
                .getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA && main.getConf().isSigns()) {
            handleTileEntityData(packet, languagePlayer);
        } else if (!existsSignUpdatePacket() && packet.getPacketType() == PacketType.Play.Server.MAP_CHUNK && main
                .getConf().isSigns()) {
            handleMapChunk(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS && main.getConf().isItems()) {
            handleWindowItems(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SET_SLOT && main.getConf().isItems()) {
            handleSetSlot(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.BOSS && main.getConf().isBossbars()) {
            handleBoss(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Login.Server.DISCONNECT && main.getConf().isKick()) {
            handleKickDisconnect(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.OPEN_WINDOW_MERCHANT && main.getConf().isItems()) {
            handleMerchantItems(packet, languagePlayer);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packet) {
        if (packet.isServerPacket()) return;
        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer =
                    (SpigotLanguagePlayer) Triton.get().getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            Triton.get().logDebugWarning("Failed to get SpigotLanguagePlayer because UUID of the player is unknown " +
                    "(because the player hasn't joined yet).");
            return;
        }
        if (languagePlayer == null) {
            Triton.get().logDebugWarning("Language Player is null on packet receiving");
            return;
        }
        if (packet.getPacketType() == PacketType.Play.Client.SETTINGS) {
            if (languagePlayer.isWaitingForClientLocale())
                Bukkit.getScheduler().runTask(Triton.get().getLoader().asSpigot(), () -> languagePlayer
                        .setLang(Triton.get().getLanguageManager()
                                .getLanguageByLocale(packet.getPacket().getStrings().readSafely(0), true)));
        }
    }

    @Override
    public void refreshSigns(SpigotLanguagePlayer player) {
        out:
        for (LanguageItem item : main.getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            for (LanguageSign.SignLocation location : sign.getLocations())
                if (player.toBukkit().getWorld().getName().equals(location.getWorld())) {
                    PacketContainer packet =
                            ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN);
                    String[] lines = sign.getLines(player.getLang().getName());
                    if (lines == null) lines = sign.getLines(main.getLanguageManager().getMainLanguage().getName());
                    if (lines == null) continue out;
                    String[] defaultLines = getSignLinesFromLocation(location);
                    lines = lines.clone();
                    for (int i = 0; i < 4; ++i)
                        if (lines[i].equals("%use_line_default%") && defaultLines[i] != null)
                            lines[i] = Triton.get().getLanguageParser()
                                    .replaceLanguages(Triton.get().getLanguageManager()
                                            .matchPattern(defaultLines[i], player), player, Triton.get()
                                            .getConf()
                                            .getSignsSyntax());
                    packet.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(),
                            location.getY(), location.getZ()));
                    if (existsSignUpdatePacket()) {
                        WrappedChatComponent[] comps = new WrappedChatComponent[4];
                        for (int i = 0; i < 4; i++)
                            comps[i] =
                                    WrappedChatComponent.fromJson(ComponentSerializer
                                            .toString(TextComponent.fromLegacyText(lines[i])));
                        packet.getChatComponentArrays().writeSafely(0, comps);
                    } else {
                        packet.getIntegers().writeSafely(0, 9);
                        NbtCompound compound = NbtFactory.ofCompound(null);
                        compound.put("x", location.getX());
                        compound.put("y", location.getY());
                        compound.put("z", location.getZ());
                        compound.put("id", getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
                        for (int i = 0; i < 4; i++)
                            compound.put("Text" + (i + 1),
                                    ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
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
        if (player.getEntitiesMap().containsKey(player.toBukkit().getWorld()))
            for (Map.Entry<Integer, String> entry : player.getEntitiesMap().get(player.toBukkit().getWorld())
                    .entrySet()) {
                if (entry.getValue() == null) continue;
                PacketContainer packet =
                        ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
                packet.getIntegers().writeSafely(0, entry.getKey());
                Object value;
                if (getMCVersion() >= 13)
                    value = Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                            .toString(main.getLanguageParser().parseComponent(player, main.getConf()
                                    .getHologramSyntax(), ComponentSerializer.parse(entry.getValue()))))
                            .getHandle());
                else
                    value = main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                    .matchPattern(entry.getValue(), player), player,
                            main.getConf().getHologramSyntax());
                WrappedWatchableObject watchableObject;
                if (getMCVersion() >= 9)
                    watchableObject = new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(2,
                            getMCVersion() >= 13 ? WrappedDataWatcher.Registry
                                    .getChatComponentSerializer(true) : WrappedDataWatcher.Registry
                                    .get(String.class)), value);
                else
                    watchableObject = new WrappedWatchableObject(2, value);
                packet.getWatchableCollectionModifier().writeSafely(0, Collections.singletonList(watchableObject));
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, false);
                } catch (InvocationTargetException e) {
                    main.logError("Failed to send entity update packet: %1", e.getMessage());
                }
            }

        if (player.getPlayersMap().containsKey(player.toBukkit().getWorld()))
            playerLoop:
                    for (Map.Entry<Integer, Entity> entry : player.getPlayersMap().get(player.toBukkit().getWorld())
                            .entrySet()) {
                        Player p = (Player) entry.getValue();
                        for (Player op : Bukkit.getOnlinePlayers())
                            if (op.getUniqueId().equals(p.getUniqueId())) continue playerLoop;
                        List<PlayerInfoData> dataList = new ArrayList<>();
                        dataList.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), 50,
                                EnumWrappers.NativeGameMode.fromBukkit(p.getGameMode()),
                                WrappedChatComponent.fromText(p.getPlayerListName())));
                        PacketContainer packetRemove =
                                ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                        packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        packetRemove.getPlayerInfoDataLists().writeSafely(0, dataList);

                        PacketContainer packetAdd =
                                ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                        packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                        packetRemove.getPlayerInfoDataLists().writeSafely(0, dataList);

                        PacketContainer packetDestroy =
                                ProtocolLibrary.getProtocolManager()
                                        .createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                        packetDestroy.getIntegerArrays().writeSafely(0, new int[]{p.getEntityId()});

                        PacketContainer packetSpawn =
                                ProtocolLibrary.getProtocolManager()
                                        .createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
                        packetSpawn.getIntegers().writeSafely(0, p.getEntityId());
                        packetSpawn.getUUIDs().writeSafely(0, p.getUniqueId());
                        // Location in 1.8 is integer only
                        if (getMCVersion() < 9)
                            packetSpawn.getIntegers()
                                    .writeSafely(1, (int) Math.floor(p.getLocation().getX() * 32.00D))
                                    .writeSafely(2, (int) Math.floor(p.getLocation().getY() * 32.00D))
                                    .writeSafely(3, (int) Math.floor(p.getLocation().getZ() * 32.00D));
                        else
                            packetSpawn.getDoubles().writeSafely(0, p.getLocation().getX()).writeSafely(1,
                                    p.getLocation().getY()).writeSafely(2, p.getLocation().getZ());
                        packetSpawn.getBytes().writeSafely(0, (byte) (int) (p.getLocation().getYaw() * 256.0F / 360.0F))
                                .writeSafely(1, (byte) (int) (p.getLocation().getPitch() * 256.0F / 360.0F));
                        packetSpawn.getDataWatcherModifier().writeSafely(0, WrappedDataWatcher.getEntityWatcher(p));

                        PacketContainer packetLook = ProtocolLibrary.getProtocolManager()
                                .createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                        packetLook.getIntegers().writeSafely(0, p.getEntityId());
                        packetLook.getBytes().writeSafely(0, (byte) (int) (p.getLocation().getYaw() * 256.0F / 360.0F));

                        try {
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetRemove, true);
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetDestroy, false);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetAdd, false);
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetSpawn, false);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetLook, false);
                        } catch (InvocationTargetException e) {
                            main.logError("Failed to send player entity update packet: %1", e.getMessage());
                        }
                    }

    }

    @Override
    public void refreshTabHeaderFooter(SpigotLanguagePlayer player, String header, String footer) {
        PacketContainer packet =
                ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
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
        player.getScoreboard().rerender(true);
    }

    @Override
    public void resetSign(Player p, LanguageSign.SignLocation location) {
        World world = Bukkit.getWorld(location.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
        BlockState state = block.getState();
        if (!(state instanceof Sign))
            return;
        String[] lines = ((Sign) state).getLines();
        if (existsSignUpdatePacket()) {
            PacketContainer container =
                    ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_SIGN, true);
            container.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(), location.getY(),
                    location.getZ()));
            container.getChatComponentArrays().writeSafely(0,
                    new WrappedChatComponent[]{WrappedChatComponent.fromText(lines[0]),
                            WrappedChatComponent.fromText(lines[1]), WrappedChatComponent.fromText(lines[2]),
                            WrappedChatComponent.fromText(lines[3])});
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.logError("Failed refresh sign: %1", e.getMessage());
            }
        } else {
            PacketContainer container =
                    ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TILE_ENTITY_DATA, true);
            container.getBlockPositionModifier().writeSafely(0, new BlockPosition(location.getX(), location.getY(),
                    location.getZ()));
            container.getIntegers().writeSafely(0, 9); // Action (9): Update sign text
            NbtCompound nbt = NbtFactory.asCompound(container.getNbtModifier().readSafely(0));
            for (int i = 0; i < 4; i++)
                nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
            nbt.put("name", "null").put("x", block.getX()).put("y", block.getY()).put("z", block.getZ()).put("id",
                    getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.logError("Failed refresh sign: %1", e.getMessage());
            }
        }
    }

    private void addEntity(World world, int id, String displayName, SpigotLanguagePlayer lp) {
        if (!lp.getEntitiesMap().containsKey(world))
            lp.getEntitiesMap().put(world, new HashMap<>());
        lp.getEntitiesMap().get(world).put(id, displayName);
    }

    private void removeEntities(HashMap<Integer, ?> map, int[] ids) {
        if (map == null) return;
        map.keySet().removeAll(Arrays.stream(ids).boxed().collect(Collectors.toList()));
    }

    private void addPlayer(World world, int id, Entity player, SpigotLanguagePlayer lp) {
        if (!lp.getPlayersMap().containsKey(world))
            lp.getPlayersMap().put(world, new HashMap<>());
        lp.getPlayersMap().get(world).put(id, player);
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        Collection<PacketType> types = new ArrayList<>();
        types.add(PacketType.Play.Server.CHAT);
        types.add(PacketType.Play.Server.TITLE);
        types.add(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
        types.add(PacketType.Play.Server.OPEN_WINDOW);
        types.add(PacketType.Play.Server.ENTITY_METADATA);
        types.add(PacketType.Play.Server.SPAWN_ENTITY);
        types.add(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
        types.add(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        types.add(PacketType.Play.Server.ENTITY_DESTROY);
        types.add(PacketType.Play.Server.PLAYER_INFO);
        types.add(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
        types.add(PacketType.Play.Server.SCOREBOARD_SCORE);
        types.add(PacketType.Play.Server.SCOREBOARD_TEAM);
        types.add(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);
        types.add(PacketType.Play.Server.KICK_DISCONNECT);
        if (existsSignUpdatePacket()) types.add(PacketType.Play.Server.UPDATE_SIGN);
        else {
            types.add(PacketType.Play.Server.MAP_CHUNK);
            types.add(PacketType.Play.Server.TILE_ENTITY_DATA);
        }
        types.add(PacketType.Play.Server.WINDOW_ITEMS);
        types.add(PacketType.Play.Server.SET_SLOT);
        types.add(PacketType.Login.Server.DISCONNECT);
        if (getMCVersion() >= 9) types.add(PacketType.Play.Server.BOSS);
        if (getMCVersion() >= 14) types.add(PacketType.Play.Server.OPEN_WINDOW_MERCHANT);
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(types).highest().build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Client.SETTINGS)
                .highest().build();
    }

    @Override
    public Plugin getPlugin() {
        return main.getLoader().asSpigot();
    }

    private boolean isActionbar(PacketContainer container) {
        if (getMCVersion() >= 12)
            return container.getChatTypes().readSafely(0) == EnumWrappers.ChatType.GAME_INFO;
        else
            return container.getBytes().readSafely(0) == 2;
    }

    private int getMCVersion() {
        return mcVersion;
    }

    private int getMCVersionR() {
        return mcVersionR;
    }

    private boolean existsSignUpdatePacket() {
        return getMCVersion() == 8 || (getMCVersion() == 9 && getMCVersionR() == 1);
    }

    private BaseComponent[] mergeComponents(BaseComponent... comps) {
        return new BaseComponent[]{new TextComponent(TextComponent.toLegacyText(comps))};
    }

    private String translate(LanguagePlayer lp, String s, int max, MainConfig.FeatureSyntax syntax) {
        String r = main.getLanguageParser().replaceLanguages(s, lp, syntax);
        if (r.length() > max) return r.substring(0, max);
        return r;
    }

    private String translate(String s, LanguagePlayer lp, MainConfig.FeatureSyntax syntax) {
        return main.getLanguageParser().replaceLanguages(main.getLanguageManager().matchPattern(s, lp), lp, syntax);
    }

    private ItemStack translateItemStack(ItemStack item, LanguagePlayer languagePlayer, boolean translateBooks) {
        if (item == null) return null;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(translate(meta.getDisplayName(),
                        languagePlayer, main.getConf().getItemsSyntax()));
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore())
                    newLore.add(translate(lore, languagePlayer,
                            main.getConf().getItemsSyntax()));
                meta.setLore(newLore);
            }
            item.setItemMeta(meta);
            if (translateBooks && item.getType() == Material.WRITTEN_BOOK && main.getConf().isBooks()) {
                NbtCompound compound = NbtFactory.asCompound(NbtFactory.fromItemTag(item));
                NbtList<String> pages = compound.getList("pages");
                Collection<NbtBase<String>> pagesCollection = pages.asCollection();
                List<String> newPagesCollection = new ArrayList<>();
                for (NbtBase<String> page : pagesCollection) {
                    if (page.getValue().startsWith("\""))
                        newPagesCollection.add(
                                ComponentSerializer.toString(
                                        TextComponent.fromLegacyText(
                                                translate(page.getValue().substring(1
                                                        , page.getValue().length() - 1),
                                                        languagePlayer, main.getConf().getItemsSyntax()))));
                    else {
                        newPagesCollection.add(
                                ComponentSerializer.toString(main.getLanguageParser().parseComponent(languagePlayer,
                                        main.getConf().getItemsSyntax(),
                                        ComponentSerializer.parse(page.getValue()))));
                    }
                }
                compound.put("pages", NbtFactory.ofList("pages", newPagesCollection));
            }
        }
        return item;
    }

    private String[] getSignLinesFromLocation(LanguageSign.SignLocation loc) {
        World w = Bukkit.getWorld(loc.getWorld());
        if (w == null) return new String[4];
        Location l = new Location(w, loc.getX(), loc.getY(), loc.getZ());
        BlockState state = l.getBlock().getState();
        if (!(state instanceof Sign))
            return new String[4];
        return ((Sign) state).getLines();
    }

    public enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES
    }

    public enum EnumScoreboardHealthDisplay {
        HEARTS, INTEGER
    }

}
