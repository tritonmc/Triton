package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.utils.EntityTypeUtils;
import com.rexcantor64.triton.spigot.utils.ItemStackTranslationUtils;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asSync;

public class EntitiesPacketHandler extends PacketHandler {

    private final DataWatcherHandler dataWatcherHandler;

    public EntitiesPacketHandler() {
        if (getMcVersion() < 9) {
            // MC 1.8
            this.dataWatcherHandler = new DataWatcherHandler1_8();
        } else if (getMcVersion() < 13) {
            // MC 1.9 to 1.12
            this.dataWatcherHandler = new DataWatcherHandler1_9();
        } else {
            // MC 1.13+
            this.dataWatcherHandler = new DataWatcherHandler1_13();
        }
    }

    /**
     * @param entityType The entity type to check if it should be translated or not.
     * @return Whether the plugin should attempt to translate the given entity type.
     */
    private boolean isEntityTypeDisabled(EntityType entityType) {
        return !getConfig().isHologramsAll() && !getConfig().getHolograms().contains(entityType);
    }

    /**
     * Handle a Spawn Entity packet by adding the spawned entity to cache, if
     * translation is enabled for its entity type.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handleSpawnEntity(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);

        // Try to get the entity type
        EntityType entityType;
        if (getMcVersion() >= 14) {
            // On MC 1.14+, the entity type is present on the packet
            entityType = EntityType.fromBukkit(packet.getPacket().getEntityTypeModifier().readSafely(0));
        } else if (getMcVersion() >= 9) {
            // On MC 1.9 to 1.13, we need to convert the entity type ID to an actual entity type object
            entityType = EntityTypeUtils.getEntityTypeByObjectId(packet.getPacket().getIntegers().readSafely(6));
        } else {
            // On MC 1.8, we need to convert the entity type ID to an actual entity type object
            entityType = EntityTypeUtils.getEntityTypeByObjectId(packet.getPacket().getIntegers().readSafely(9));
        }
        if (isEntityTypeDisabled(entityType)) {
            // Don't add this entity to cache if it shouldn't be translated
            return;
        }
        if (entityType == EntityType.PLAYER) {
            // Ignore human entities since they're handled separately
            // This packet should never spawn one either way
            return;
        }

        // Add entity to cache
        addEntity(languagePlayer.getEntitiesMap(), packet.getPlayer().getWorld(), entityId, Optional.empty());
    }

    /**
     * Handle a Spawn Entity Living packet by adding the spawned entity to cache, if
     * translation is enabled for its entity type. Additionally, translate any existing
     * custom names.
     * This packet was removed in MC 1.19.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handleSpawnEntityLiving(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);

        int entityTypeId = packet.getPacket().getIntegers().readSafely(1);
        EntityType entityType = EntityTypeUtils.getEntityTypeById(entityTypeId);
        if (isEntityTypeDisabled(entityType)) {
            // Don't add this entity to cache if it shouldn't be translated
            return;
        }
        if (entityType == EntityType.PLAYER) {
            // Ignore human entities since they're handled separately
            // This packet should never spawn one either way
            return;
        }
        // Add entity to cache
        addEntity(languagePlayer.getEntitiesMap(), packet.getPlayer().getWorld(), entityId, Optional.empty());

        if (getMcVersion() >= 15) {
            // DataWatcher is not sent on 1.15 anymore in this packet
            return;
        }

        // Clone the data watcher, so we don't edit the display name permanently
        val dataWatcherValues = packet.getPacket()
                .getDataWatcherModifier()
                .readSafely(0)
                .asMap()
                .values();
        val dataWatcher = new WrappedDataWatcher(new ArrayList<>(dataWatcherValues));

        val displayNameWatchableObject = dataWatcher.getWatchableObject(2);
        if (displayNameWatchableObject != null) {
            this.dataWatcherHandler.translatePlayerDisplayNameWatchableObject(
                    languagePlayer,
                    displayNameWatchableObject,
                    (displayName) -> addEntity(
                            languagePlayer.getEntitiesMap(),
                            packet.getPlayer().getWorld(),
                            entityId,
                            Optional.of(displayName)
                    ),
                    (hasCustomName) -> this.dataWatcherHandler
                            .getCustomNameVisibilityWatchableObject(hasCustomName)
                            .ifPresent(obj -> dataWatcher.setObject(3, obj))
            ).ifPresent(obj -> dataWatcher.setObject(2, obj));
        }

        packet.getPacket().getDataWatcherModifier().writeSafely(0, dataWatcher);
    }

    /**
     * Handle a Named Entity Spawn packet by adding the spawned entity to cache, if
     * translation is enabled for human entities.
     * Due to the complexity of translating human entities, it is only possible to translate
     * those stored server side by the server itself.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handleNamedEntitySpawn(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (isEntityTypeDisabled(EntityType.PLAYER)) {
            // Don't add this entity to cache if it shouldn't be translated
            return;
        }

        // TODO For now, it is only possible to translate NPCs that are saved server side
        // Fetch entity object using main thread, otherwise we'll get concurrency issues
        Triton.asSpigot()
                .callSync(() -> packet.getPacket().getEntityModifier(packet).readSafely(0))
                .ifPresent(entity -> addEntity(
                                languagePlayer.getPlayersMap(),
                                packet.getPlayer().getWorld(),
                                entity.getEntityId(),
                                entity
                        )
                );
    }

    /**
     * Handle an Entity Metadata packet by replacing the display name of the entity.
     * If the entity is a (glowing) item frame, translate the item inside it.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handleEntityMetadata(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        int entityId = packet.getPacket().getIntegers().readSafely(0);

        val worldEntitiesMap = languagePlayer.getEntitiesMap().get(packet.getPlayer().getWorld());
        if (worldEntitiesMap == null || !worldEntitiesMap.containsKey(entityId)) {
            // If the entity isn't in the cache, don't translate it
            return;
        }

        List<WrappedWatchableObject> watchableObjects = packet.getPacket().getWatchableCollectionModifier().readSafely(0);
        if (watchableObjects == null) {
            // The DataWatcher.Item List is Nullable
            // Since it's null, it doesn't have any text to translate anyway, so just ignore it
            return;
        }

        List<WrappedWatchableObject> newWatchableObjects = new ArrayList<>();
        AtomicBoolean skipHideCustomName = new AtomicBoolean(false);
        for (WrappedWatchableObject oldObject : watchableObjects) {
            if (oldObject.getIndex() == 2) {
                // Index 2 is "Custom Name" of type "OptChat"
                // https://wiki.vg/Entity_metadata#Entity
                newWatchableObjects.add(
                        this.dataWatcherHandler.translatePlayerDisplayNameWatchableObject(
                                languagePlayer,
                                oldObject,
                                (displayName) -> addEntity(
                                        languagePlayer.getEntitiesMap(),
                                        packet.getPlayer().getWorld(),
                                        entityId,
                                        Optional.of(displayName)
                                ),
                                (hasCustomName) -> this.dataWatcherHandler
                                        .getCustomNameVisibilityWatchableObject(hasCustomName)
                                        .ifPresent(obj -> {
                                            newWatchableObjects.add(obj);
                                            // Ensure the original WatchableObject, if existed, will not be added to the list
                                            skipHideCustomName.set(true);
                                        })
                        ).orElse(oldObject)
                );
            } else if (oldObject.getIndex() == 3) {
                // Index 3 is "Is custom name visible" of type "Boolean"
                // https://wiki.vg/Entity_metadata#Entity
                if (!skipHideCustomName.get()) {
                    newWatchableObjects.add(oldObject);
                }
            } else if (oldObject.getIndex() == 8 && getMcVersion() >= 13) {
                // Index 8 is "Item" of type "Slot"
                // https://wiki.vg/Entity_metadata#Entity
                // Used to translate items inside (glowing) item frames
                newWatchableObjects.add(
                        this.dataWatcherHandler.translateItemFrameItems(
                                languagePlayer,
                                oldObject,
                                (itemStack) -> addEntity(
                                        languagePlayer.getItemFramesMap(),
                                        packet.getPlayer().getWorld(),
                                        entityId,
                                        itemStack
                                )
                        ).orElse(oldObject)
                );
            } else {
                newWatchableObjects.add(oldObject);
            }
        }
        packet.getPacket().getWatchableCollectionModifier().writeSafely(0, newWatchableObjects);
    }

    /**
     * Handle an Entity Destroy packet by removing its entities from Triton's internal cache.
     * Depending on the MC version, this packet might contain one or multiple entities,
     * represented either by an integer, an integer array or an integer list.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handleEntityDestroy(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        Stream<Integer> ids;
        if (packet.getPacket().getIntegers().size() > 0) {
            ids = Stream.of(packet.getPacket().getIntegers().readSafely(0));
        } else if (packet.getPacket().getIntegerArrays().size() > 0) {
            ids = Arrays.stream(packet.getPacket().getIntegerArrays().readSafely(0)).boxed();
        } else {
            ids = ((List<Integer>) packet.getPacket().getModifier().readSafely(0)).stream();
        }

        val world = packet.getPlayer().getWorld();
        removeEntities(
                ids,
                languagePlayer.getEntitiesMap().get(world),
                languagePlayer.getPlayersMap().get(world),
                languagePlayer.getItemFramesMap().get(world)
        );
    }

    /**
     * Handle a Player Info packet by translating the name and display name of all game profiles.
     * This is more directed to translating the names of human entities, but it can also
     * translate custom TABs.
     *
     * @param packet         ProtocolLib's packet event.
     * @param languagePlayer The language player this packet is being sent to.
     */
    private void handlePlayerInfo(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (isEntityTypeDisabled(EntityType.PLAYER)) {
            return;
        }

        EnumWrappers.PlayerInfoAction infoAction = packet.getPacket().getPlayerInfoAction().readSafely(0);
        List<PlayerInfoData> dataList = packet.getPacket().getPlayerInfoDataLists().readSafely(0);
        if (infoAction == EnumWrappers.PlayerInfoAction.REMOVE_PLAYER) {
            for (PlayerInfoData data : dataList) {
                languagePlayer.getShownPlayers().remove(data.getProfile().getUUID());
            }
            return;
        }

        if (infoAction != EnumWrappers.PlayerInfoAction.ADD_PLAYER && infoAction != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
            return;

        List<PlayerInfoData> dataListNew = new ArrayList<>();
        for (PlayerInfoData data : dataList) {
            WrappedGameProfile oldGP = data.getProfile();
            languagePlayer.getShownPlayers().add(oldGP.getUUID());
            WrappedGameProfile newGP = oldGP.withName(translateAndTruncate(
                    languagePlayer,
                    oldGP.getName()
            ));
            newGP.getProperties().putAll(oldGP.getProperties());
            WrappedChatComponent msg = data.getDisplayName();
            if (msg != null) {
                BaseComponent[] result = getLanguageParser().parseComponent(
                        languagePlayer,
                        getConfig().getHologramSyntax(),
                        ComponentSerializer.parse(msg.getJson())
                );
                if (result == null) {
                    msg = null;
                } else {
                    msg.setJson(ComponentSerializer.toString(result));
                }
            }
            dataListNew.add(new PlayerInfoData(newGP, data.getLatency(), data.getGameMode(), msg));
        }
        packet.getPacket().getPlayerInfoDataLists().writeSafely(0, dataListNew);
    }

    /**
     * Resend custom names of entities when the language of a player changes.
     * This will send player info and spawn packets in case of human entities,
     * otherwise it will just send an entity metadata packet.
     *
     * @param languagePlayer The player to resend the entity packets to.
     */
    public void refreshEntities(SpigotLanguagePlayer languagePlayer) {
        if (!languagePlayer.toBukkit().isPresent()) return;
        val bukkitPlayer = languagePlayer.toBukkit().get();

        refreshNormalEntities(languagePlayer, bukkitPlayer);
        refreshHumanEntities(languagePlayer, bukkitPlayer);
        refreshItemFramesEntities(languagePlayer, bukkitPlayer);
    }

    /**
     * Resend custom names of entities that are not human entities when the language of a player changes.
     * This will send entity metadata packets, hiding the custom name if a "disabled line" is found.
     *
     * @param languagePlayer The player to resend the entity packets to.
     * @param bukkitPlayer   The bukkit handler of languagePlayer.
     */
    private void refreshNormalEntities(@NotNull SpigotLanguagePlayer languagePlayer, @NotNull Player bukkitPlayer) {
        val entitiesInCurrentWorld = languagePlayer.getEntitiesMap().get(bukkitPlayer.getWorld());
        if (entitiesInCurrentWorld == null) {
            return;
        }

        for (Map.Entry<Integer, Optional<String>> entry : entitiesInCurrentWorld.entrySet()) {
            if (!entry.getValue().isPresent()) {
                continue;
            }
            val displayName = entry.getValue().get();

            final List<WrappedWatchableObject> watchableObjects = new ArrayList<>();
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);

            // Write entity ID
            packet.getIntegers().writeSafely(0, entry.getKey());

            if (getMcVersion() >= 13) {
                // On MC 1.13+, display names are sent as text components instead of legacy text
                val result = getLanguageParser().parseComponent(
                        languagePlayer,
                        getConfig().getHologramSyntax(),
                        ComponentSerializer.parse(displayName)
                );

                this.dataWatcherHandler.getPlayerDisplayNameWatchableObject(result).ifPresent(watchableObjects::add);
                this.dataWatcherHandler.getCustomNameVisibilityWatchableObject(result != null).ifPresent(watchableObjects::add);
            } else {
                // On MC 1.8 to 1.12, display names are sent as legacy text
                val result = getLanguageParser().replaceLanguages(
                        getLanguageManager().matchPattern(displayName, languagePlayer),
                        languagePlayer,
                        getConfig().getHologramSyntax()
                );

                this.dataWatcherHandler.getPlayerDisplayNameWatchableObject(result).ifPresent(watchableObjects::add);
                this.dataWatcherHandler.getCustomNameVisibilityWatchableObject(result != null).ifPresent(watchableObjects::add);
            }

            if (watchableObjects.size() == 0) {
                // Don't send anything if there are no changes
                continue;
            }

            // Write watchable objects
            packet.getWatchableCollectionModifier().writeSafely(0, watchableObjects);
            // Send packet without passing through listeners again
            sendPacket(bukkitPlayer, packet, false);
        }
    }

    /**
     * Resend custom names of human entities when the language of a player changes.
     * This will send player info and spawn packets, generating the necessary packets for so that Triton's listener
     * is able to translate the human entity's name.
     * This relies on the current entity name server side, therefore not being able to accurately
     * refresh entities that are created only through packets.
     *
     * @param languagePlayer The player to resend the entity packets to.
     * @param bukkitPlayer   The bukkit handler of languagePlayer.
     */
    private void refreshHumanEntities(@NotNull SpigotLanguagePlayer languagePlayer, @NotNull Player bukkitPlayer) {
        val humanEntitiesInCurrentWorld = languagePlayer.getPlayersMap().get(bukkitPlayer.getWorld());
        if (humanEntitiesInCurrentWorld == null) {
            return;
        }

        for (Map.Entry<Integer, Entity> entry : humanEntitiesInCurrentWorld.entrySet()) {
            val humanEntity = (Player) entry.getValue();
            if (isRealPlayer(humanEntity.getUniqueId())) {
                continue;
            }

            val playerInfoDataList = Collections.singletonList(
                    new PlayerInfoData(
                            WrappedGameProfile.fromPlayer(humanEntity),
                            50,
                            EnumWrappers.NativeGameMode.fromBukkit(humanEntity.getGameMode()),
                            WrappedChatComponent.fromText(humanEntity.getPlayerListName())
                    )
            );

            // To be able to change the name of a human entity, we must first remove its player info
            val packetRemove = createPacket(PacketType.Play.Server.PLAYER_INFO);
            packetRemove.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            packetRemove.getPlayerInfoDataLists().writeSafely(0, playerInfoDataList);

            // Destroy the current entity
            val packetDestroy = createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packetDestroy.getIntegerArrays().writeSafely(0, new int[]{humanEntity.getEntityId()});

            // Then send the player info again
            val packetAdd = createPacket(PacketType.Play.Server.PLAYER_INFO);
            packetAdd.getPlayerInfoAction().writeSafely(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packetAdd.getPlayerInfoDataLists().writeSafely(0, playerInfoDataList);

            // Spawn the entity again
            val packetSpawn = createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            packetSpawn.getIntegers().writeSafely(0, humanEntity.getEntityId());
            packetSpawn.getUUIDs().writeSafely(0, humanEntity.getUniqueId());
            if (getMcVersion() < 9) {
                // On MC 1.8, location is defined as an integer and multiplied by 32
                packetSpawn.getIntegers()
                        .writeSafely(1, (int) Math.floor(humanEntity.getLocation().getX() * 32.00D))
                        .writeSafely(2, (int) Math.floor(humanEntity.getLocation().getY() * 32.00D))
                        .writeSafely(3, (int) Math.floor(humanEntity.getLocation().getZ() * 32.00D));
            } else {
                // On MC 1.9+, location is defined as a double
                packetSpawn.getDoubles()
                        .writeSafely(0, humanEntity.getLocation().getX())
                        .writeSafely(1, humanEntity.getLocation().getY())
                        .writeSafely(2, humanEntity.getLocation().getZ());
            }
            packetSpawn.getBytes()
                    .writeSafely(0, (byte) (int) (humanEntity.getLocation().getYaw() * 256.0F / 360.0F))
                    .writeSafely(1, (byte) (int) (humanEntity.getLocation().getPitch() * 256.0F / 360.0F));
            packetSpawn.getDataWatcherModifier().writeSafely(0, WrappedDataWatcher.getEntityWatcher(humanEntity));

            // Even though this is sent in the spawn packet, we still need to send it again for some reason
            val packetLook = createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            packetLook.getIntegers().writeSafely(0, humanEntity.getEntityId());
            packetLook.getBytes().writeSafely(0, (byte) (int) (humanEntity.getLocation().getYaw() * 256.0F / 360.0F));

            val isHiddenEntity = !languagePlayer.getShownPlayers().contains(humanEntity.getUniqueId());
            sendPacket(bukkitPlayer, packetRemove, true);
            sendPacket(bukkitPlayer, packetDestroy, true);
            sendPacket(bukkitPlayer, packetAdd, true);
            sendPacket(bukkitPlayer, packetSpawn, true);
            sendPacket(bukkitPlayer, packetLook, true);
            if (isHiddenEntity) {
                // If the entity should not show up in tab, hide it again
                Bukkit.getScheduler().runTaskLater(
                        getMain().getLoader(),
                        () -> sendPacket(bukkitPlayer, packetRemove, true),
                        4L
                );
            }
        }
    }

    /**
     * Resend items of (glowing) item frames when the language of a player changes.
     * This will send entity metadata packets.
     *
     * @param languagePlayer The player to resend the entity packets to.
     * @param bukkitPlayer   The bukkit handler of languagePlayer.
     */
    private void refreshItemFramesEntities(@NotNull SpigotLanguagePlayer languagePlayer, @NotNull Player bukkitPlayer) {
        val entitiesInCurrentWorld = languagePlayer.getItemFramesMap().get(bukkitPlayer.getWorld());
        if (entitiesInCurrentWorld == null) {
            return;
        }

        for (Map.Entry<Integer, ItemStack> entry : entitiesInCurrentWorld.entrySet()) {
            val itemStack = entry.getValue();

            final List<WrappedWatchableObject> watchableObjects = new ArrayList<>();
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);

            // Write entity ID
            packet.getIntegers().writeSafely(0, entry.getKey());


            val clonedItemStack = itemStack.clone();
            val translatedItemStack = ItemStackTranslationUtils.translateItemStack(clonedItemStack, languagePlayer, false);
            this.dataWatcherHandler.getItemStackWatchableObject(translatedItemStack).ifPresent(watchableObjects::add);

            if (watchableObjects.size() == 0) {
                // Don't send anything if there are no changes
                continue;
            }

            // Write watchable objects
            packet.getWatchableCollectionModifier().writeSafely(0, watchableObjects);
            // Send packet without passing through listeners again
            sendPacket(bukkitPlayer, packet, false);
        }
    }

    /**
     * @param uniqueId The player to search for.
     * @return True if the player is logged in, false otherwise.
     */
    private boolean isRealPlayer(UUID uniqueId) {
        for (val op : Bukkit.getOnlinePlayers()) {
            if (op.getUniqueId().equals(uniqueId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add an entity along with relevant data to a cache multimap.
     *
     * @param map   The multimap to add the data to.
     * @param world The world (first key) this entity belongs to.
     * @param id    The entity id (second key).
     * @param value The data to store with the entity (e.g. display name, entity object, etc).
     * @param <T>   The type of the data to store.
     */
    private <T> void addEntity(Map<World, Map<Integer, T>> map, World world, int id, T value) {
        map.computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .put(id, value);
    }

    /**
     * Remove all entries with one of the given IDs from the given maps.
     *
     * @param ids  The entity IDs to remove.
     * @param maps The maps to remove the entities from.
     */
    @SafeVarargs
    private final void removeEntities(Stream<Integer> ids, Map<Integer, ?>... maps) {
        ids.forEach(id -> Arrays.stream(maps).filter(Objects::nonNull).forEach(map -> map.keySet().remove(id)));
    }

    /**
     * Translate legacy text using Triton's language parser and truncate it to 16 characters.
     *
     * @param languagePlayer The player to translate to.
     * @param string         The legacy text to translate.
     * @return The translated legacy text, truncated to 16 characters.
     */
    private String translateAndTruncate(LanguagePlayer languagePlayer, String string) {
        if (string == null) {
            return null;
        }
        val result = getLanguageParser().replaceLanguages(
                getLanguageManager().matchPattern(string, languagePlayer),
                languagePlayer,
                getConfig().getHologramSyntax()
        );
        if (result != null && result.length() > 16) return result.substring(0, 16);
        return result;
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public void registerPacketTypes(Map<PacketType, HandlerFunction> registry) {
        registry.put(PacketType.Play.Server.SPAWN_ENTITY, asSync(this::handleSpawnEntity));
        if (getMcVersion() < 19) {
            // 1.19 removed this packet
            registry.put(PacketType.Play.Server.SPAWN_ENTITY_LIVING, asSync(this::handleSpawnEntityLiving));
        }
        registry.put(PacketType.Play.Server.NAMED_ENTITY_SPAWN, asSync(this::handleNamedEntitySpawn));
        registry.put(PacketType.Play.Server.ENTITY_METADATA, asSync(this::handleEntityMetadata));
        registry.put(PacketType.Play.Server.ENTITY_DESTROY, asSync(this::handleEntityDestroy));

        registry.put(PacketType.Play.Server.PLAYER_INFO, asSync(this::handlePlayerInfo));
    }

    private abstract static class DataWatcherHandler {

        /**
         * Get a {@link WrappedWatchableObject wrapped watchable object} for an entity's
         * display name from a text component.
         *
         * @param components The display name of the entity as a text component. Can be null.
         * @return The wrapped watchable object.
         * @since 3.8.0
         */
        abstract Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(@Nullable BaseComponent[] components);

        /**
         * Get a {@link WrappedWatchableObject wrapped watchable object} for an entity's
         * display name from a legacy text string.
         *
         * @param text The display name of the entity as a legacy text string. Can be null.
         * @return The wrapped watchable object.
         * @since 3.8.0
         */
        abstract Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(@Nullable String text);

        /**
         * Get a {@link WrappedWatchableObject wrapped watchable object} for the visibility of
         * an entity's display name from a boolean.
         *
         * @param visible Whether of not the custom name of the entity should be visible.
         * @return The wrapped watchable object.
         * @since 3.8.0
         */
        abstract Optional<WrappedWatchableObject> getCustomNameVisibilityWatchableObject(boolean visible);

        /**
         * Get a {@link WrappedWatchableObject wrapped watchable object} for the item stack
         * inside an item frame.
         *
         * @param item The item to set inside the item frame.
         * @return The wrapped watchable object.
         * @since 3.8.0
         */
        abstract Optional<WrappedWatchableObject> getItemStackWatchableObject(ItemStack item);

        /**
         * Translate the content of a WatchableObject containing a display name.
         *
         * @param languagePlayer        The player to translate for.
         * @param watchableObject       The original watchable object to translate.
         * @param saveToCache           A callback that will save to cache the display name before translating it.
         * @param hasCustomNameConsumer A callback that will be run with true or false, depending on
         *                              whether there is a custom display name.
         * @return The translated watchable object, if applicable.
         */
        abstract Optional<WrappedWatchableObject> translatePlayerDisplayNameWatchableObject(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<String> saveToCache,
                @Nullable Consumer<Boolean> hasCustomNameConsumer);

        /**
         * Translate the content of a WatchableObject containing an item.
         *
         * @param languagePlayer  The player to translate for.
         * @param watchableObject The original watchable object to translate.
         * @param saveToCache     A callback that will save to cache the item before translating it.
         * @return The translated watchable object, if applicable.
         */
        abstract Optional<WrappedWatchableObject> translateItemFrameItems(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<ItemStack> saveToCache
        );
    }

    private class DataWatcherHandler1_8 extends DataWatcherHandler {
        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(BaseComponent[] components) {
            // This watchable object uses legacy text in this version
            return Optional.empty();
        }

        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(String text) {
            val payload = text == null ? "" : text;

            // Display name has: index 2
            return Optional.of(new WrappedWatchableObject(2, payload));
        }

        @Override
        Optional<WrappedWatchableObject> getCustomNameVisibilityWatchableObject(boolean visible) {
            if (visible) {
                // If the name should be visible, we don't need to do anything
                return Optional.empty();
            }

            // On MC 1.8, this is represented by a byte instead of a boolean
            return Optional.of(new WrappedWatchableObject(3, (byte) 0));
        }

        @Override
        Optional<WrappedWatchableObject> getItemStackWatchableObject(ItemStack item) {
            return Optional.empty();
        }

        @Override
        Optional<WrappedWatchableObject> translatePlayerDisplayNameWatchableObject(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<String> saveToCache,
                @Nullable Consumer<Boolean> hasCustomNameConsumer) {
            String displayName = (String) watchableObject.getValue();
            if (displayName == null) {
                return Optional.empty();
            }

            // Save to cache before translating
            saveToCache.accept(displayName);

            val result = getLanguageParser().replaceLanguages(
                    getLanguageManager().matchPattern(displayName, languagePlayer),
                    languagePlayer,
                    getConfig().getHologramSyntax()
            );

            if (hasCustomNameConsumer != null) {
                hasCustomNameConsumer.accept(result != null);
            }
            return this.getPlayerDisplayNameWatchableObject(result);
        }

        @Override
        Optional<WrappedWatchableObject> translateItemFrameItems(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<ItemStack> saveToCache) {
            return Optional.empty();
        }
    }

    private class DataWatcherHandler1_9 extends DataWatcherHandler {
        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(BaseComponent[] components) {
            // This watchable object uses legacy text in this version
            return Optional.empty();
        }

        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(String text) {
            val payload = text == null ? "" : text;

            // Display name has: index 2 and type string
            val watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(
                    2,
                    WrappedDataWatcher.Registry.get(String.class)
            );

            return Optional.of(new WrappedWatchableObject(watcherObject, payload));
        }

        @Override
        Optional<WrappedWatchableObject> getCustomNameVisibilityWatchableObject(boolean visible) {
            if (visible) {
                // If the name should be visible, we don't need to do anything
                return Optional.empty();
            }

            // Custom name visibility has: index 3 and type boolean
            // https://wiki.vg/Entity_metadata#Entity
            val watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(
                    3,
                    WrappedDataWatcher.Registry.get(Boolean.class)
            );

            return Optional.of(new WrappedWatchableObject(watcherObject, false));
        }

        @Override
        Optional<WrappedWatchableObject> getItemStackWatchableObject(ItemStack item) {
            return Optional.empty();
        }

        @Override
        Optional<WrappedWatchableObject> translatePlayerDisplayNameWatchableObject(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<String> saveToCache,
                @Nullable Consumer<Boolean> hasCustomNameConsumer) {
            String displayName = (String) watchableObject.getValue();
            if (displayName == null) {
                return Optional.empty();
            }

            // Save to cache before translating
            saveToCache.accept(displayName);

            val result = getLanguageParser().replaceLanguages(
                    getLanguageManager().matchPattern(displayName, languagePlayer),
                    languagePlayer,
                    getConfig().getHologramSyntax()
            );

            if (hasCustomNameConsumer != null) {
                hasCustomNameConsumer.accept(result != null);
            }
            return this.getPlayerDisplayNameWatchableObject(result);
        }

        @Override
        Optional<WrappedWatchableObject> translateItemFrameItems(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<ItemStack> saveToCache) {
            return Optional.empty();
        }
    }

    private class DataWatcherHandler1_13 extends DataWatcherHandler {
        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(BaseComponent[] components) {
            Optional<Object> payload;
            if (components != null) {
                val wrappedChatComponent = WrappedChatComponent.fromJson(ComponentSerializer.toString(components));
                payload = Optional.of(wrappedChatComponent.getHandle());
            } else {
                payload = Optional.empty();
            }

            // Display name has: index 2 and type chat
            // https://wiki.vg/Entity_metadata#Entity
            val watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(
                    2,
                    WrappedDataWatcher.Registry.getChatComponentSerializer(true)
            );

            return Optional.of(new WrappedWatchableObject(watcherObject, payload));
        }

        @Override
        Optional<WrappedWatchableObject> getPlayerDisplayNameWatchableObject(String text) {
            // This watchable object does not use legacy text in this version anymore
            return Optional.empty();
        }

        @Override
        Optional<WrappedWatchableObject> getCustomNameVisibilityWatchableObject(boolean visible) {
            if (visible) {
                // If the name should be visible, we don't need to do anything
                return Optional.empty();
            }

            // Custom name visibility has: index 3 and type boolean
            // https://wiki.vg/Entity_metadata#Entity
            val watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(
                    3,
                    WrappedDataWatcher.Registry.get(Boolean.class)
            );

            return Optional.of(new WrappedWatchableObject(watcherObject, false));
        }

        @Override
        Optional<WrappedWatchableObject> getItemStackWatchableObject(ItemStack item) {
            // Display name has: index 8 and type slot (item stack)
            // https://wiki.vg/Entity_metadata#Entity
            val watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(
                    8,
                    WrappedDataWatcher.Registry.getItemStackSerializer(false)
            );

            // We cannot pass translated item stack to constructor because it doesn't get unwrapped
            val newWatchableObject = new WrappedWatchableObject(watcherObject, null);
            // The setter unwraps the Bukkit class to the NMS class
            newWatchableObject.setValue(item);
            return Optional.of(newWatchableObject);
        }

        @Override
        Optional<WrappedWatchableObject> translatePlayerDisplayNameWatchableObject(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<String> saveToCache,
                @Nullable Consumer<Boolean> hasCustomNameConsumer) {
            val displayName = (Optional<WrappedChatComponent>) watchableObject.getValue();
            if (!displayName.isPresent()) {
                return Optional.empty();
            }

            val displayNameJson = displayName.get().getJson();

            // Save to cache before translating
            saveToCache.accept(displayNameJson);

            val result = getLanguageParser().parseComponent(
                    languagePlayer,
                    getConfig().getHologramSyntax(),
                    ComponentSerializer.parse(displayNameJson)
            );

            if (hasCustomNameConsumer != null) {
                hasCustomNameConsumer.accept(result != null);
            }
            return this.getPlayerDisplayNameWatchableObject(result);
        }

        @Override
        Optional<WrappedWatchableObject> translateItemFrameItems(
                SpigotLanguagePlayer languagePlayer,
                WrappedWatchableObject watchableObject,
                Consumer<ItemStack> saveToCache) {
            val value = watchableObject.getValue();
            if (!(value instanceof ItemStack)) {
                return Optional.empty();
            }

            val itemStack = (ItemStack) value;
            if (itemStack.getType() == Material.AIR) {
                return Optional.empty();
            }

            saveToCache.accept(itemStack.clone());
            val clonedItemStack = itemStack.clone();

            val translatedItemStack = ItemStackTranslationUtils.translateItemStack(clonedItemStack, languagePlayer, false);
            return this.getItemStackWatchableObject(translatedItemStack);
        }
    }
}
