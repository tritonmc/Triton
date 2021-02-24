package com.rexcantor64.triton.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.MethodUtils;
import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.utils.EntityTypeUtils;
import com.rexcantor64.triton.utils.NMSUtils;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
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
import java.util.stream.Stream;

@SuppressWarnings({"deprecation"})
public class ProtocolLibListener implements PacketListener, PacketInterceptor {
    private final Class<?> MERCHANT_RECIPE_LIST_CLASS;
    private final Class<?> CRAFT_MERCHANT_RECIPE_LIST_CLASS;
    private final Class<BaseComponent[]> BASE_COMPONENT_ARRAY_CLASS = BaseComponent[].class;
    private SpigotMLP main;

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
        MERCHANT_RECIPE_LIST_CLASS = main.getMcVersion() >= 14 ? NMSUtils.getNMSClass("MerchantRecipeList") : null;
        CRAFT_MERCHANT_RECIPE_LIST_CLASS = main.getMcVersion() >= 14 ? NMSUtils
                .getCraftbukkitClass("inventory.CraftMerchantRecipe") : null;
    }

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());
        val baseComponentModifier = packet.getPacket().getSpecificModifier(BASE_COMPONENT_ARRAY_CLASS);
        if (ab && main.getConf().isActionbars()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                BaseComponent[] result = main.getLanguageParser()
                        .parseComponent(languagePlayer, main.getConf().getActionbarSyntax(), ComponentSerializer
                                .parse(msg.getJson()));
                if (result == null) {
                    packet.setCancelled(true);
                    return;
                }
                msg.setJson(ComponentSerializer.toString(mergeComponents(result)));
                packet.getPacket().getChatComponents().writeSafely(0, msg);
                return;
            }
            BaseComponent[] bc = main.getLanguageParser().parseComponent(languagePlayer,
                    main.getConf().getActionbarSyntax(),
                    baseComponentModifier.readSafely(0));
            if (bc == null) {
                packet.setCancelled(true);
                return;
            }
            baseComponentModifier.writeSafely(0, mergeComponents(bc));
        } else if (!ab && main.getConf().isChat()) {
            WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                try {
                    BaseComponent[] result = main.getLanguageParser()
                            .parseComponent(languagePlayer, main.getConf()
                                    .getChatSyntax(), ComponentSerializer.parse(msg.getJson()));
                    if (result == null) {
                        packet.setCancelled(true);
                        return;
                    }
                    msg.setJson(ComponentSerializer.toString(result));
                    packet.getPacket().getChatComponents().writeSafely(0, msg);
                } catch (RuntimeException e) {
                    Triton.get().getLogger()
                            .logError(1, "Could not parse a chat message, so it was ignored. Message: %1", msg
                                    .getJson());
                    if (Triton.get().getConfig().getLogLevel() > 99)
                        e.printStackTrace();
                }
                return;
            }
            BaseComponent[] bc = main.getLanguageParser().parseComponent(languagePlayer,
                    main.getConf().getChatSyntax(),
                    baseComponentModifier.readSafely(0));
            if (bc == null) {
                packet.setCancelled(true);
                return;
            }
            baseComponentModifier.writeSafely(0, bc);
        }
    }

    private void handleTitle(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        if (msg == null) return;
        BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTitleSyntax(), ComponentSerializer.parse(msg.getJson()));
        if (result == null) {
            packet.setCancelled(true);
            return;
        }
        msg.setJson(ComponentSerializer.toString(result));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handlePlayerListHeaderFooter(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent header = packet.getPacket().getChatComponents().readSafely(0);
        String headerJson = header.getJson();
        BaseComponent[] resultHeader = main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTabSyntax(), ComponentSerializer.parse(headerJson));
        if (resultHeader == null)
            resultHeader = new BaseComponent[]{new TranslatableComponent("")};
        header.setJson(ComponentSerializer.toString(resultHeader));
        packet.getPacket().getChatComponents().writeSafely(0, header);
        WrappedChatComponent footer = packet.getPacket().getChatComponents().readSafely(1);
        String footerJson = footer.getJson();
        BaseComponent[] resultFooter = main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getTabSyntax(), ComponentSerializer.parse(footerJson));
        if (resultFooter == null)
            resultFooter = new BaseComponent[]{new TranslatableComponent("")};
        footer.setJson(ComponentSerializer.toString(resultFooter));
        packet.getPacket().getChatComponents().writeSafely(1, footer);
        languagePlayer.setLastTabHeader(headerJson);
        languagePlayer.setLastTabFooter(footerJson);
    }

    private void handleOpenWindow(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        BaseComponent[] result = main.getLanguageParser()
                .parseComponent(languagePlayer, main.getConf().getGuiSyntax(), ComponentSerializer
                        .parse(msg.getJson()));
        if (result == null)
            result = new BaseComponent[]{new TextComponent("")};
        msg.setJson(ComponentSerializer.toString(mergeComponents(result)));
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
        boolean forceHideCustomName = false;
        if (getMCVersion() >= 13) {
            Optional optional = (Optional) watchableObject.getValue();
            if (optional.isPresent()) {
                String displayName = WrappedChatComponent.fromHandle(optional.get()).getJson();
                addEntity(packet.getPlayer().getWorld(), entityId, displayName, languagePlayer);
                BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer, main.getConf()
                        .getHologramSyntax(), ComponentSerializer
                        .parse(displayName));
                if (result != null)
                    dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(),
                            Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                                    .toString(result))
                                    .getHandle())));
                else {
                    dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(),
                            Optional.empty()));
                    forceHideCustomName = true;
                }
            }
        } else if (getMCVersion() >= 9) {
            addEntity(packet.getPlayer().getWorld(), entityId, (String) watchableObject.getValue(), languagePlayer);
            String result = translate((String) watchableObject.getValue(), languagePlayer, main.getConf()
                    .getHologramSyntax());
            if (result != null)
                dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(), result));
            else {
                dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getWatcherObject(),
                        ""));
                forceHideCustomName = true;
            }
        } else {
            addEntity(packet.getPlayer().getWorld(), entityId, (String) watchableObject.getValue(), languagePlayer);
            String result = translate((String) watchableObject.getValue(), languagePlayer, main.getConf()
                    .getHologramSyntax());
            if (result != null)
                dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getIndex(), result));
            else {
                dataWatcher.setObject(2, new WrappedWatchableObject(watchableObject.getIndex(),
                        ""));
                forceHideCustomName = true;
            }
        }
        if (forceHideCustomName) {
            WrappedWatchableObject isCustomName = dataWatcher.getWatchableObject(3);
            if (isCustomName != null)
                if (getMCVersion() >= 9)
                    dataWatcher.setObject(3, new WrappedWatchableObject(isCustomName.getWatcherObject(), false));
                else
                    dataWatcher.setObject(3, new WrappedWatchableObject(isCustomName.getIndex(), (byte) 0));
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
        boolean forceHideCustomName = false;
        for (WrappedWatchableObject obj : dw)
            if (obj.getIndex() == 2)
                if (getMCVersion() < 9) {
                    addEntity(packet.getPlayer().getWorld(), entityId, (String) obj.getValue(), languagePlayer);
                    String result = translate((String) obj.getValue(), languagePlayer, main.getConf()
                            .getHologramSyntax());
                    if (result != null)
                        dwn.add(new WrappedWatchableObject(obj.getIndex(), result));
                    else {
                        dwn.add(new WrappedWatchableObject(obj.getIndex(), ""));
                        forceHideCustomName = true;
                    }
                } else if (getMCVersion() < 13) {
                    addEntity(packet.getPlayer().getWorld(), entityId, (String) obj.getValue(), languagePlayer);
                    String result = translate((String) obj.getValue(), languagePlayer, main.getConf()
                            .getHologramSyntax());
                    if (result != null)
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), result));
                    else {
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), ""));
                        forceHideCustomName = true;
                    }
                } else {
                    Optional optional = (Optional) obj.getValue();
                    if (optional.isPresent()) {
                        addEntity(packet.getPlayer().getWorld(), entityId, WrappedChatComponent
                                .fromHandle(optional.get())
                                .getJson(), languagePlayer);
                        BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer, main.getConf()
                                .getHologramSyntax(), ComponentSerializer
                                .parse(WrappedChatComponent.fromHandle(optional.get()).getJson()));
                        if (result != null)
                            dwn.add(new WrappedWatchableObject(obj.getWatcherObject(),
                                    Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                                            .toString(result))
                                            .getHandle())));
                        else {
                            dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), Optional.empty()));
                            forceHideCustomName = true;
                        }
                    } else dwn.add(obj);
                }
            else if (obj.getIndex() == 3) {
                if (forceHideCustomName) {
                    if (getMCVersion() >= 9)
                        dwn.add(new WrappedWatchableObject(obj.getWatcherObject(), false));
                    else dwn.add(new WrappedWatchableObject(obj.getIndex(), (byte) 0));
                } else dwn.add(obj);
            } else
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
        List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().readSafely(0);
        if (infoAction == EnumWrappers.PlayerInfoAction.REMOVE_PLAYER) {
            for (PlayerInfoData data : dataList)
                languagePlayer.getShownPlayers().remove(data.getProfile().getUUID());
            return;
        }

        if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
            return;

        List<PlayerInfoData> dataListNew = new ArrayList<>();
        for (PlayerInfoData data : dataList) {
            WrappedGameProfile oldGP = data.getProfile();
            languagePlayer.getShownPlayers().add(oldGP.getUUID());
            WrappedGameProfile newGP = oldGP.withName(translate(languagePlayer, oldGP.getName(), 16,
                    main.getConf().getHologramSyntax()));
            newGP.getProperties().putAll(oldGP.getProperties());
            WrappedChatComponent msg = data.getDisplayName();
            if (msg != null) {
                BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer,
                        main.getConf().getHologramSyntax(), ComponentSerializer.parse(msg.getJson()));
                if (result == null)
                    msg = null;
                else
                    msg.setJson(ComponentSerializer.toString(result));
            }
            dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
        }
        packet.getPacket().getPlayerInfoDataLists().writeSafely(0, dataListNew);
    }

    private void handleKickDisconnect(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        WrappedChatComponent msg = packet.getPacket().getChatComponents().readSafely(0);
        BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer,
                main.getConf().getKickSyntax(), ComponentSerializer.parse(msg.getJson()));
        if (result == null)
            result = new BaseComponent[]{new TextComponent("")};
        msg.setJson(ComponentSerializer.toString(result));
        packet.getPacket().getChatComponents().writeSafely(0, msg);
    }

    private void handleUpdateSign(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        val newPacket = packet.getPacket().shallowClone();
        val pos = newPacket.getBlockPositionModifier().readSafely(0);
        val linesModifier = newPacket.getChatComponentArrays();

        val l = new SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ());
        String[] lines = main.getLanguageManager().getSign(languagePlayer, l, () -> {
            val defaultLinesWrapped = linesModifier.readSafely(0);
            val defaultLines = new String[4];
            for (int i = 0; i < 4; i++) {
                try {
                    defaultLines[i] = AdvancedComponent
                            .fromBaseComponent(ComponentSerializer.parse(defaultLinesWrapped[i].getJson()))
                            .getTextClean();
                } catch (Exception e) {
                    Triton.get().getLogger().logError("Failed to parse sign line %1 at %2.", i + 1, l);
                    if (Triton.get().getConfig().getLogLevel() >= 1)
                        e.printStackTrace();
                }
            }
            return defaultLines;
        });
        if (lines == null) return;
        val comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; i++)
            comps[i] =
                    WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(lines[i])));
        linesModifier.writeSafely(0, comps);
        packet.setPacket(newPacket);
    }

    private void handleTileEntityData(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (packet.getPacket().getIntegers().readSafely(0) == 9) {
            val newPacket = packet.getPacket().deepClone();
            val nbt = NbtFactory.asCompound(newPacket.getNbtModifier().readSafely(0));
            val l = new SignLocation(packet.getPlayer().getWorld().getName(),
                    nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            String[] sign = main.getLanguageManager().getSign(languagePlayer, l, () -> {
                val defaultLines = new String[4];
                for (int i = 0; i < 4; i++) {
                    try {
                        val nbtLine = nbt.getStringOrDefault("Text" + (i + 1));
                        if (nbtLine != null)
                            defaultLines[i] = AdvancedComponent
                                    .fromBaseComponent(ComponentSerializer.parse(nbtLine))
                                    .getTextClean();
                    } catch (Exception e) {
                        Triton.get().getLogger().logError("Failed to parse sign line %1 at %2.", i + 1, l);
                        if (Triton.get().getConfig().getLogLevel() >= 1)
                            e.printStackTrace();
                    }
                }
                return defaultLines;
            });
            if (sign != null) {
                for (int i = 0; i < 4; i++)
                    nbt.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
                packet.setPacket(newPacket);
            }
        }
    }

    private void handleMapChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        val entities = packet.getPacket().getListNbtModifier().readSafely(0);
        for (val entity : entities) {
            val nbt = NbtFactory.asCompound(entity);
            if (nbt.getString("id").equals(getMCVersion() <= 10 ? "Sign" : "minecraft:sign")) {
                val l = new SignLocation(packet.getPlayer().getWorld().getName(),
                        nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
                String[] sign = main.getLanguageManager().getSign(languagePlayer, l, () -> {
                    val defaultLines = new String[4];
                    for (int i = 0; i < 4; i++) {
                        try {
                            val nbtLine = nbt.getStringOrDefault("Text" + (i + 1));
                            if (nbtLine != null)
                                defaultLines[i] = AdvancedComponent
                                        .fromBaseComponent(ComponentSerializer.parse(nbtLine))
                                        .getTextClean();
                        } catch (Exception e) {
                            Triton.get().getLogger().logError("Failed to parse sign line %1 at %2.", i + 1, l);
                            if (Triton.get().getConfig().getLogLevel() >= 1)
                                e.printStackTrace();
                        }
                    }
                    return defaultLines;
                });

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
        translateItemStack(item, languagePlayer, true);
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
        try {
            languagePlayer.setBossbar(uuid, bossbar.getJson());
            BaseComponent[] result = main.getLanguageParser().parseComponent(languagePlayer,
                    main.getConf().getBossbarSyntax(), ComponentSerializer.parse(bossbar.getJson()));
            if (result == null)
                result = new BaseComponent[]{new TranslatableComponent("")};
            bossbar.setJson(ComponentSerializer.toString(result));
            packet.getPacket().getChatComponents().writeSafely(0, bossbar);
        } catch (RuntimeException e) {
            // TODO Catch 1.16 Hover 'contents' not being parsed correctly
            Triton.get().getLogger()
                    .logError(1, "Could not parse a bossbar, so it was ignored. Bossbar: %1", bossbar.getJson());
        }
    }

    private void handleMerchantItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        try {
            ArrayList<?> recipes = (ArrayList<?>) packet.getPacket()
                    .getSpecificModifier(MERCHANT_RECIPE_LIST_CLASS).readSafely(0);
            ArrayList<Object> newRecipes = (ArrayList<Object>) MERCHANT_RECIPE_LIST_CLASS.newInstance();
            for (val recipeObject : recipes) {
                val recipe = (MerchantRecipe) NMSUtils.getMethod(recipeObject, "asBukkit");
                val originalSpecialPrice = NMSUtils.getDeclaredField(recipeObject, "specialPrice");
                val originalDemand = NMSUtils.getDeclaredField(recipeObject, "demand");

                val newRecipe = new MerchantRecipe(translateItemStack(recipe.getResult()
                        .clone(), languagePlayer, false), recipe.getUses(), recipe.getMaxUses(), recipe
                        .hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());

                for (val ingredient : recipe.getIngredients())
                    newRecipe.addIngredient(translateItemStack(ingredient.clone(), languagePlayer, false));
                Object newCraftRecipe = MethodUtils
                        .invokeExactStaticMethod(CRAFT_MERCHANT_RECIPE_LIST_CLASS, "fromBukkit", newRecipe);
                Object newNMSRecipe = MethodUtils.invokeExactMethod(newCraftRecipe, "toMinecraft", null);
                NMSUtils.setDeclaredField(newNMSRecipe, "specialPrice", originalSpecialPrice);
                NMSUtils.setDeclaredField(newNMSRecipe, "demand", originalDemand);
                newRecipes.add(newNMSRecipe);
            }
            packet.getPacket().getModifier().writeSafely(1, newRecipes);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void handleServerInfo(PacketEvent event) {
        val lang = Triton.get().getStorage().getLanguageFromIp(Objects
                .requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress()).getName();
        val syntax = Triton.get().getConfig().getMotdSyntax();

        val serverPing = event.getPacket().getServerPings().readSafely(0);
        serverPing.setPlayers(serverPing.getPlayers().stream().map((gp) -> {
            if (gp.getName() == null) return gp;
            val translatedName = Triton.get().getLanguageParser().replaceLanguages(gp.getName(), lang, syntax);
            if (gp.getName().equals(translatedName)) return gp;
            if (translatedName == null) return null;
            return gp.withName(translatedName);
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        val motd = serverPing.getMotD();
        val result = main.getLanguageParser()
                .parseComponent(lang, syntax, ComponentSerializer.parse(motd.getJson()));
        if (result != null)
            motd.setJson(ComponentSerializer.toString(mergeComponents(result)));
        serverPing.setMotD(motd);
    }

    private void handleScoreboardTeam(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        val teamName = packet.getPacket().getStrings().readSafely(0);
        val mode = packet.getPacket().getIntegers().readSafely(0);

        if (mode == 1) {
            languagePlayer.removeScoreboardTeam(teamName);
            return;
        }

        if (mode != 0 && mode != 2) return; // Other modes don't change text

        // Pack name tag visibility, collision rule, team color and friendly flags into list
        val modifiers = packet.getPacket().getModifier();
        List<Object> options = Stream.of(4, 5, 6, 9).map(modifiers::readSafely).collect(Collectors.toList());

        val chatComponents = packet.getPacket().getChatComponents();
        val displayName = chatComponents.readSafely(0);
        val prefix = chatComponents.readSafely(1);
        val suffix = chatComponents.readSafely(2);

        languagePlayer.setScoreboardTeam(teamName, displayName.getJson(), prefix.getJson(), suffix.getJson(), options);

        var i = 0;
        for (var component : Arrays.asList(displayName, prefix, suffix)) {
            var result = main.getLanguageParser()
                    .parseComponent(languagePlayer, main.getConf().getScoreboardSyntax(), ComponentSerializer
                            .parse(component.getJson()));
            if (result == null) result = new BaseComponent[]{new TextComponent("")};
            component.setJson(ComponentSerializer.toString(mergeComponents(result)));
            packet.getPacket().getChatComponents().writeSafely(i++, component);
        }
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        val objectiveName = packet.getPacket().getStrings().readSafely(0);
        val mode = packet.getPacket().getIntegers().readSafely(0);

        if (mode == 1) {
            languagePlayer.removeScoreboardObjective(objectiveName);
            return;
        }
        // There are only 3 modes, so no need to check for more modes

        val healthDisplay = packet.getPacket().getModifier().readSafely(2);
        val displayName = packet.getPacket().getChatComponents().readSafely(0);

        languagePlayer.setScoreboardObjective(objectiveName, displayName.getJson(), healthDisplay);

        var result = main.getLanguageParser()
                .parseComponent(languagePlayer, main.getConf().getScoreboardSyntax(), ComponentSerializer
                        .parse(displayName.getJson()));
        if (result == null) result = new BaseComponent[]{new TextComponent("")};
        displayName.setJson(ComponentSerializer.toString(mergeComponents(result)));
        packet.getPacket().getChatComponents().writeSafely(0, displayName);
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        if (packet.getPacketType() == PacketType.Status.Server.SERVER_INFO) {
            if (main.getConfig().isMotd()) handleServerInfo(packet);
            return;
        }
        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer =
                    (SpigotLanguagePlayer) Triton.get().getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            Triton.get().getLogger()
                    .logWarning(1, "Failed to translate packet because UUID of the player is unknown (because " +
                            "the player hasn't joined yet).");
            return;
        }
        if (languagePlayer == null) {
            Triton.get().getLogger().logWarning(1, "Language Player is null on packet sending");
            return;
        }
        if (packet.getPacketType() == PacketType.Login.Server.SUCCESS)
            languagePlayer.setInterceptor(this);
        else if (packet.getPacketType() == PacketType.Play.Server.CHAT) {
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
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM) {
            handleScoreboardTeam(packet, languagePlayer);
        } else if (packet.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE) {
            handleScoreboardObjective(packet, languagePlayer);
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
            Triton.get().getLogger()
                    .logWarning(1, "Failed to get SpigotLanguagePlayer because UUID of the player is unknown " +
                            "(because the player hasn't joined yet).");
            return;
        }
        if (languagePlayer == null) {
            Triton.get().getLogger().logWarning(1, "Language Player is null on packet receiving");
            return;
        }
        if (packet.getPacketType() == PacketType.Play.Client.SETTINGS) {
            if (languagePlayer.isWaitingForClientLocale())
                Bukkit.getScheduler().runTask(Triton.asSpigot().getLoader(), () -> languagePlayer
                        .setLang(Triton.get().getLanguageManager()
                                .getLanguageByLocale(packet.getPacket().getStrings().readSafely(0), true)));
        }
    }

    @Override
    public void refreshSigns(SpigotLanguagePlayer player) {
        val storage = main.getStorage();
        val filterItems = Triton.get() instanceof SpigotMLP && Triton.get().getConfig().isBungeecord() && !(Triton.get()
                .getStorage() instanceof LocalStorage);
        val serverName = Triton.get().getConfig().getServerName();

        storage.getCollections().values().forEach((collection -> collection.getItems().forEach((item -> {
            if (!(item instanceof LanguageSign)) return;
            val sign = (LanguageSign) item;

            if (sign.getLocations() == null) return;

            val lines = Optional.ofNullable(sign.getLines(player.getLang().getName()))
                    .orElse(sign.getLines(main.getLanguageManager().getMainLanguage().getName()));
            if (lines == null) return;

            sign.getLocations().stream()
                    .filter((loc) -> !filterItems || loc.getServer() == null || serverName.equals(loc.getServer()))
                    .forEach(location -> {
                        val resultLines = main.getLanguageManager()
                                .formatLines(player.getLang()
                                        .getName(), lines, () -> getSignLinesFromLocation(location));

                        val packet = ProtocolLibrary.getProtocolManager()
                                .createPacket(PacketType.Play.Server.UPDATE_SIGN);

                        packet.getBlockPositionModifier().writeSafely(0,
                                new BlockPosition(location.getX(), location.getY(), location.getZ()));

                        if (existsSignUpdatePacket()) {
                            val comps = new WrappedChatComponent[4];
                            for (var i = 0; i < 4; ++i)
                                comps[i] =
                                        WrappedChatComponent.fromJson(ComponentSerializer
                                                .toString(TextComponent.fromLegacyText(resultLines[i])));
                            packet.getChatComponentArrays().writeSafely(0, comps);
                        } else {
                            packet.getIntegers().writeSafely(0, 9);
                            val compound = NbtFactory.ofCompound(null);
                            compound.put("x", location.getX());
                            compound.put("y", location.getY());
                            compound.put("z", location.getZ());
                            compound.put("id", getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
                            for (var i = 0; i < 4; ++i)
                                compound.put("Text" + (i + 1),
                                        ComponentSerializer.toString(TextComponent.fromLegacyText(resultLines[i])));
                            packet.getNbtModifier().writeSafely(0, compound);
                        }

                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, false);
                        } catch (InvocationTargetException e) {
                            main.getLogger().logError("Failed to send sign update packet: %1", e.getMessage());
                        }
                    });
        }))));
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
                boolean forceHideCustomName = false;
                Object value;
                if (getMCVersion() >= 13) {
                    BaseComponent[] result = main.getLanguageParser().parseComponent(player, main.getConf()
                            .getHologramSyntax(), ComponentSerializer.parse(entry.getValue()));
                    if (result != null)
                        value = Optional.of(WrappedChatComponent.fromJson(ComponentSerializer
                                .toString(result))
                                .getHandle());
                    else {
                        value = Optional.empty();
                        forceHideCustomName = true;
                    }
                } else {
                    value = main.getLanguageParser().replaceLanguages(main.getLanguageManager()
                                    .matchPattern(entry.getValue(), player), player,
                            main.getConf().getHologramSyntax());
                    if (value == null) {
                        value = "";
                        forceHideCustomName = true;
                    }
                }
                List<WrappedWatchableObject> watchableObjects = new ArrayList<>();
                WrappedWatchableObject watchableObject;
                if (getMCVersion() >= 9)
                    watchableObject = new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(2,
                            getMCVersion() >= 13 ? WrappedDataWatcher.Registry
                                    .getChatComponentSerializer(true) : WrappedDataWatcher.Registry
                                    .get(String.class)), value);
                else
                    watchableObject = new WrappedWatchableObject(2, value);
                watchableObjects.add(watchableObject);
                if (forceHideCustomName) {
                    WrappedWatchableObject customNameVisibility;
                    if (getMCVersion() >= 9)
                        customNameVisibility =
                                new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(3,
                                        WrappedDataWatcher.Registry
                                                .get(Boolean.class)), false);
                    else
                        customNameVisibility = new WrappedWatchableObject(3, (byte) 0);
                    watchableObjects.add(customNameVisibility);
                }
                packet.getWatchableCollectionModifier().writeSafely(0, watchableObjects);
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, false);
                } catch (InvocationTargetException e) {
                    main.getLogger().logError("Failed to send entity update packet: %1", e.getMessage());
                }
            }

        if (player.getPlayersMap().containsKey(player.toBukkit().getWorld()))
            playerLoop:
                    for (Map.Entry<Integer, Entity> entry : player.getPlayersMap().get(player.toBukkit().getWorld())
                            .entrySet()) {
                        val p = (Player) entry.getValue();
                        for (val op : Bukkit.getOnlinePlayers())
                            if (op.getUniqueId().equals(p.getUniqueId())) continue playerLoop;

                        val dataList = new ArrayList<PlayerInfoData>();
                        dataList.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), 50,
                                EnumWrappers.NativeGameMode.fromBukkit(p.getGameMode()),
                                WrappedChatComponent.fromText(p.getPlayerListName())));

                        val packetRemove =
                                ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                        packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        packetRemove.getPlayerInfoDataLists().writeSafely(0, dataList);

                        val packetAdd =
                                ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                        packetAdd.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                        packetAdd.getPlayerInfoDataLists().writeSafely(0, dataList);

                        val packetDestroy =
                                ProtocolLibrary.getProtocolManager()
                                        .createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                        packetDestroy.getIntegerArrays().writeSafely(0, new int[]{p.getEntityId()});

                        val packetSpawn =
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

                        val packetLook = ProtocolLibrary.getProtocolManager()
                                .createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                        packetLook.getIntegers().writeSafely(0, p.getEntityId());
                        packetLook.getBytes().writeSafely(0, (byte) (int) (p.getLocation().getYaw() * 256.0F / 360.0F));

                        try {
                            val isHiddenEntity = !player.getShownPlayers().contains(p.getUniqueId());
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetRemove, true);
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetDestroy, false);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetAdd, true);
                            ProtocolLibrary.getProtocolManager()
                                    .sendServerPacket(player.toBukkit(), packetSpawn, false);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packetLook, false);
                            if (isHiddenEntity) {
                                Bukkit.getScheduler().runTaskLater(main.getLoader(), () -> {
                                    try {
                                        ProtocolLibrary
                                                .getProtocolManager()
                                                .sendServerPacket(player.toBukkit(), packetRemove, true);
                                    } catch (InvocationTargetException e) {
                                        main.getLogger().logError("Failed to send player entity update packet: %1", e
                                                .getMessage());
                                    }
                                }, 4L);
                            }
                        } catch (InvocationTargetException e) {
                            main.getLogger().logError("Failed to send player entity update packet: %1", e.getMessage());
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
            main.getLogger().logError("Failed to send tab update packet: %1", e.getMessage());
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
            main.getLogger().logError("Failed to send bossbar update packet: %1", e.getMessage());
        }
    }

    @Override
    public void refreshScoreboard(SpigotLanguagePlayer player) {
        player.getObjectivesMap().forEach((key, value) -> {
            val packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
            packet.getIntegers().writeSafely(0, 2); // Update display name mode
            packet.getStrings().writeSafely(0, key);
            packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(value.getChatJson()));
            packet.getModifier().writeSafely(2, value.getType());
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, true);
            } catch (InvocationTargetException e) {
                main.getLogger().logError("Failed to send scoreboard objective update packet: %1", e.getMessage());
            }
        });

        player.getTeamsMap().forEach((key, value) -> {
            val packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
            packet.getIntegers().writeSafely(0, 2); // Update team info mode
            packet.getStrings().writeSafely(0, key);
            packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(value.getDisplayJson()));
            packet.getChatComponents().writeSafely(1, WrappedChatComponent.fromJson(value.getPrefixJson()));
            packet.getChatComponents().writeSafely(2, WrappedChatComponent.fromJson(value.getSuffixJson()));
            var j = 0;
            for (var i : Arrays.asList(4, 5, 6, 9))
                packet.getModifier().writeSafely(i, value.getOptionData().get(j++));
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player.toBukkit(), packet, true);
            } catch (InvocationTargetException e) {
                main.getLogger().logError("Failed to send scoreboard team update packet: %1", e.getMessage());
            }
        });
    }

    @Override
    public void resetSign(Player p, SignLocation location) {
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
                main.getLogger().logError("Failed refresh sign: %1", e.getMessage());
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
                main.getLogger().logError("Failed refresh sign: %1", e.getMessage());
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
        types.add(PacketType.Login.Server.SUCCESS);
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
        types.add(PacketType.Play.Server.KICK_DISCONNECT);
        if (main.getMcVersion() >= 13) { // Scoreboard rewrite on 1.13
            types.add(PacketType.Play.Server.SCOREBOARD_TEAM);
            types.add(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
        }
        if (existsSignUpdatePacket()) types.add(PacketType.Play.Server.UPDATE_SIGN);
        else {
            types.add(PacketType.Play.Server.MAP_CHUNK);
            types.add(PacketType.Play.Server.TILE_ENTITY_DATA);
        }
        types.add(PacketType.Play.Server.WINDOW_ITEMS);
        types.add(PacketType.Play.Server.SET_SLOT);
        types.add(PacketType.Status.Server.SERVER_INFO);
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
        return main.getLoader();
    }

    private boolean isActionbar(PacketContainer container) {
        if (getMCVersion() >= 12)
            return container.getChatTypes().readSafely(0) == EnumWrappers.ChatType.GAME_INFO;
        else
            return container.getBytes().readSafely(0) == 2;
    }

    private short getMCVersion() {
        return main.getMcVersion();
    }

    private short getMCVersionR() {
        return main.getMinorMcVersion();
    }

    private boolean existsSignUpdatePacket() {
        return getMCVersion() == 8 || (getMCVersion() == 9 && getMCVersionR() == 1);
    }

    private BaseComponent[] mergeComponents(BaseComponent... comps) {
        if (main.getLanguageParser().hasTranslatableComponent(comps))
            return comps;
        return new BaseComponent[]{new TextComponent(AdvancedComponent.fromBaseComponent(true, comps).getText())};
    }

    private String translate(LanguagePlayer lp, String s, int max, MainConfig.FeatureSyntax syntax) {
        String r = translate(s, lp, syntax);
        if (r != null && r.length() > max) return r.substring(0, max);
        return r;
    }

    private String translate(String s, LanguagePlayer lp, MainConfig.FeatureSyntax syntax) {
        if (s == null) return null;
        return main.getLanguageParser().replaceLanguages(main.getLanguageManager().matchPattern(s, lp), lp, syntax);
    }

    private ItemStack translateItemStack(ItemStack item, LanguagePlayer languagePlayer, boolean translateBooks) {
        if (item == null) return null;
        NbtCompound compound;
        try {
            val nbtTagOptional = NbtFactory.fromItemOptional(item);
            if (!nbtTagOptional.isPresent()) return item;
            compound = NbtFactory.asCompound(nbtTagOptional.get());
        } catch (IllegalArgumentException e) {
            return item;
        }
        if (compound.containsKey("BlockEntityTag")) {
            NbtCompound blockEntityTag = compound.getCompoundOrDefault("BlockEntityTag");
            if (blockEntityTag.containsKey("Items")) {
                NbtBase<?> itemsBase = blockEntityTag.getValue("Items");
                if (itemsBase instanceof NbtList<?>) {
                    NbtList<?> items = (NbtList<?>) itemsBase;
                    Collection<? extends NbtBase<?>> itemsCollection = items.asCollection();
                    for (NbtBase<?> base : itemsCollection) {
                        NbtCompound invItem = NbtFactory.asCompound(base);
                        if (!invItem.containsKey("tag")) continue;
                        NbtCompound tag = invItem.getCompoundOrDefault("tag");
                        if (!tag.containsKey("display")) continue;
                        NbtCompound display = tag.getCompoundOrDefault("display");
                        if (!display.containsKey("Name")) continue;
                        String name = display.getStringOrDefault("Name");
                        if (getMCVersion() >= 13) {
                            BaseComponent[] result = main.getLanguageParser()
                                    .parseComponent(languagePlayer, main.getConf().getItemsSyntax(), ComponentSerializer
                                            .parse(name));
                            if (result == null)
                                display.remove("Name");
                            else
                                display.put("Name", ComponentSerializer.toString(result));
                        } else {
                            String result = translate(name, languagePlayer, main.getConf().getItemsSyntax());
                            if (result == null)
                                display.remove("Name");
                            else
                                display.put("Name", result);
                        }
                    }
                }
            }
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(translate(meta.getDisplayName(),
                        languagePlayer, main.getConf().getItemsSyntax()));
            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String lore : meta.getLore()) {
                    String result = translate(lore, languagePlayer,
                            main.getConf().getItemsSyntax());
                    if (result != null)
                        newLore.addAll(Arrays.asList(result.split("\n")));
                }
                meta.setLore(newLore);
            }
            item.setItemMeta(meta);
            if (translateBooks && item.getType() == Material.WRITTEN_BOOK && main.getConf().isBooks()) {
                compound = NbtFactory.asCompound(NbtFactory.fromItemTag(item));
                if (compound.containsKey("pages")) {
                    NbtList<String> pages = compound.getList("pages");
                    Collection<NbtBase<String>> pagesCollection = pages.asCollection();
                    List<String> newPagesCollection = new ArrayList<>();
                    for (NbtBase<String> page : pagesCollection) {
                        if (page.getValue().startsWith("\"")) {
                            String result = translate(page.getValue()
                                    .substring(1, page.getValue().length() - 1), languagePlayer, main.getConf()
                                    .getItemsSyntax());
                            if (result != null)
                                newPagesCollection.add(
                                        ComponentSerializer.toString(
                                                TextComponent.fromLegacyText(result)));
                        } else {
                            BaseComponent[] result = main.getLanguageParser()
                                    .parseComponent(languagePlayer, main.getConf().getItemsSyntax(), ComponentSerializer
                                            .parse(page.getValue()));
                            if (result != null)
                                newPagesCollection.add(ComponentSerializer.toString(result));
                        }
                    }
                    compound.put("pages", NbtFactory.ofList("pages", newPagesCollection));
                }
            }
        }
        return item;
    }

    private String[] getSignLinesFromLocation(SignLocation loc) {
        val w = Bukkit.getWorld(loc.getWorld());
        if (w == null) return new String[4];
        val l = new Location(w, loc.getX(), loc.getY(), loc.getZ());
        val state = l.getBlock().getState();
        if (!(state instanceof Sign))
            return new String[4];
        return ((Sign) state).getLines();
    }

    public enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES
    }

}
