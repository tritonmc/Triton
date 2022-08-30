package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.utils.WrappedComponentUtils;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.spigot.utils.NMSUtils;
import com.rexcantor64.triton.spigot.utils.RegistryUtils;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.utils.ReflectionUtils;
import lombok.val;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asAsync;

@SuppressWarnings({"deprecation"})
public class SignPacketHandler extends PacketHandler {

    private final Class<?> LEVEL_CHUNK_PACKET_DATA_CLASS;
    private final Class<?> TILE_ENTITY_TYPES_CLASS;
    private final String SIGN_TYPE_ID;

    public SignPacketHandler() {
        LEVEL_CHUNK_PACKET_DATA_CLASS = getMcVersion() >= 18 ?
                ReflectionUtils.getClass("net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData") : null;
        TILE_ENTITY_TYPES_CLASS = getMcVersion() >= 18 ?
                ReflectionUtils.getClass("net.minecraft.world.level.block.entity.TileEntityTypes") : null;
        SIGN_TYPE_ID = getMcVersion() >= 11 ? "minecraft:sign" : "Sign";
    }

    /**
     * @return Whether the plugin should attempt to translate signs
     */
    private boolean areSignsDisabled() {
        return !getMain().getConfig().isSigns();
    }

    /**
     * Handle a level chunk packet, added in Minecraft 1.18.
     * Looks for signs in the chunk and translates them.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @since 3.6.0 (Minecraft 1.18)
     */
    private void handleLevelChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areSignsDisabled() || LEVEL_CHUNK_PACKET_DATA_CLASS == null) return;

        val ints = packet.getPacket().getIntegers();
        val chunkX = ints.readSafely(0);
        val chunkZ = ints.readSafely(1);

        val chunkData = packet.getPacket().getSpecificModifier(LEVEL_CHUNK_PACKET_DATA_CLASS).readSafely(0);
        val blockEntities = (List<?>) ReflectionUtils.getDeclaredField(chunkData, "d");

        // Each block entity is in an array and its type needs to be checked.
        for (Object blockEntity : blockEntities) {
            val nmsNbtTagCompound = ReflectionUtils.getDeclaredField(blockEntity, "d");
            if (nmsNbtTagCompound == null) continue;

            // Try to determine type
            val tileEntityType = ReflectionUtils.getDeclaredField(blockEntity, "c");
            if (!SIGN_TYPE_ID.equals(RegistryUtils.getTileEntityTypeKey(tileEntityType))) continue;

            // The NBT compound below does not include the position, so we have to calculate it
            // from the chunk position and block section position
            val encodedPosition = (int) ReflectionUtils.getDeclaredField(blockEntity, "a");
            val sectionX = encodedPosition >> 4;
            val sectionZ = encodedPosition & 15;
            val y = (int) ReflectionUtils.getDeclaredField(blockEntity, "b");

            val nbt = NbtFactory.fromNMSCompound(nmsNbtTagCompound);

            val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                    chunkX * 16 + sectionX, y, chunkZ * 16 + sectionZ);
            translateSignNbtCompound(nbt, location, languagePlayer);
        }
    }

    /**
     * Handle a Tile Entity Data packet, changed in Minecraft 1.18.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @since 3.6.0 (Minecraft 1.18)
     */
    private void handleTileEntityDataPost1_18(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areSignsDisabled()) return;

        val tileEntityType = packet.getPacket().getSpecificModifier(TILE_ENTITY_TYPES_CLASS).readSafely(0);
        if (SIGN_TYPE_ID.equals(RegistryUtils.getTileEntityTypeKey(tileEntityType))) {
            val position = packet.getPacket().getBlockPositionModifier().readSafely(0);
            val nbt = NbtFactory.asCompound(packet.getPacket().getNbtModifier().readSafely(0));
            val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                    position.getX(), position.getY(), position.getZ());

            translateSignNbtCompound(nbt, location, languagePlayer);
        }
    }

    /**
     * Handle a map chunk packet, added in Minecraft 1.9_R2 and removed in Minecraft 1.18.
     * Looks for signs in the chunk and translates them.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @deprecated Removed in Minecraft 1.18.
     */
    @Deprecated
    private void handleMapChunk(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areSignsDisabled()) return;

        val entities = packet.getPacket().getListNbtModifier().readSafely(0);
        for (val entity : entities) {
            val nbt = NbtFactory.asCompound(entity);
            if (nbt.getString("id").equals(SIGN_TYPE_ID)) {
                val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                        nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
                translateSignNbtCompound(nbt, location, languagePlayer);
            }
        }
    }

    /**
     * Handle a Tile Entity Data packet, added in Minecraft 1.9_R2.
     * Its internal structure has changed in 1.18.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @deprecated Changed in Minecraft 1.18.
     */
    @Deprecated
    private void handleTileEntityDataPre1_18(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areSignsDisabled()) return;

        // Action 9 is "Update Sign"
        if (packet.getPacket().getIntegers().readSafely(0) == 9) {
            val newPacket = packet.getPacket().deepClone();
            val nbt = NbtFactory.asCompound(newPacket.getNbtModifier().readSafely(0));
            val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                    nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            if (translateSignNbtCompound(nbt, location, languagePlayer)) {
                packet.setPacket(newPacket);
            }
        }
    }

    /**
     * Handle an Update Sign packet, which was removed in Minecraft 1.9_R2.
     * This packet includes the sign text in a ChatComponent array.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @deprecated Removed in Minecraft 1.9_R2.
     */
    @Deprecated
    private void handleUpdateSign(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areSignsDisabled()) {
            return;
        }

        val newPacket = packet.getPacket().shallowClone();
        val pos = newPacket.getBlockPositionModifier().readSafely(0);
        val linesModifier = newPacket.getChatComponentArrays();

        val location = new SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ());
        val lines = getTranslationManager().getSignComponents(languagePlayer, location, () -> {
            val defaultLinesWrapped = linesModifier.readSafely(0);
            val defaultLines = new Component[4];
            for (int i = 0; i < 4; i++) {
                try {
                    defaultLines[i] = WrappedComponentUtils.deserialize(defaultLinesWrapped[i]);
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 at %2.", i + 1, location);
                }
            }
            return defaultLines;
        });

        if (!lines.isPresent()) {
            return;
        }

        val components = Arrays.stream(lines.get())
                .map(WrappedComponentUtils::serialize)
                .toArray(WrappedChatComponent[]::new);
        linesModifier.writeSafely(0, components);
        packet.setPacket(newPacket);
    }

    /**
     * Forcefully refresh the sign content of all signs in the same world as the given player.
     * Sends an update packet for each sign.
     *
     * @param player The player to refresh the signs for
     */
    public void refreshSignsForPlayer(SpigotLanguagePlayer player) {
        val bukkitPlayerOpt = player.toBukkit();
        if (!bukkitPlayerOpt.isPresent()) return;
        val bukkitPlayer = bukkitPlayerOpt.get();

        val storage = getMain().getStorage();
        val filterItems = Triton.get().getConfig().isBungeecord() && !(Triton.get().getStorage() instanceof LocalStorage);
        val serverName = Triton.get().getConfig().getServerName();

        storage.getCollections().values().forEach(collection ->
                collection.getItems().forEach(item -> {
                    if (!(item instanceof LanguageSign)) {
                        return;
                    }
                    val sign = (LanguageSign) item;

                    if (sign.getLocations() == null) {
                        return;
                    }

                    val lines = Optional.ofNullable(sign.getLines(player.getLang().getName()))
                            .orElse(sign.getLines(getLanguageManager().getMainLanguage().getName()));
                    if (lines == null) {
                        return;
                    }

                    sign.getLocations().stream()
                            .filter((loc) -> !filterItems || loc.getServer() == null || serverName.equals(loc.getServer()))
                            .forEach(location -> {
                                val resultLines = getTranslationManager()
                                        .formatLines(
                                                player.getLanguage(),
                                                lines,
                                                () -> getSignLinesFromLocation(location).orElse(new Component[0])
                                        );

                                PacketContainer packet;
                                if (getMcVersion() >= 18) {
                                    packet = buildTileEntityDataPacketPost1_18(location, resultLines);
                                } else if (!MinecraftReflection.signUpdateExists()) {
                                    packet = buildTileEntityDataPacketPre1_18(location, resultLines);
                                } else {
                                    packet = buildUpdateSignPacket(location, resultLines);
                                }

                                ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, false);
                            });
                })
        );
    }

    /**
     * Builds a Tile Entity Data packet for Minecraft 1.18 and above, used to refresh the content of a sign.
     *
     * @param location The location of the sign
     * @param lines    The lines of the sign
     * @return The packet that was built
     */
    @SuppressWarnings({"unchecked"})
    private PacketContainer buildTileEntityDataPacketPost1_18(SignLocation location, Component[] lines) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        // We need to pass the instance of a Sign Tile Entity, which is available in the IRegistry
        val type = RegistryUtils.getTileEntityTypeFromKey(new MinecraftKey("sign"));
        packet.getSpecificModifier((Class<Object>) TILE_ENTITY_TYPES_CLASS).writeSafely(0, type);

        val compound = NbtFactory.ofCompound(null);
        for (int i = 0; i < 4; ++i) {
            compound.put("Text" + (i + 1), ComponentUtils.serializeToJson(lines[i]));
        }
        packet.getNbtModifier().writeSafely(0, compound);

        return packet;
    }

    /**
     * Builds a Tile Entity Data packet for Minecraft 1.17 and below, used to refresh the content of a sign.
     *
     * @param location The location of the sign
     * @param lines    The lines of the sign
     * @return The packet that was built
     * @deprecated Changed in Minecraft 1.18.
     */
    @Deprecated
    private PacketContainer buildTileEntityDataPacketPre1_18(SignLocation location, Component[] lines) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        // Update sign action ID is 9
        packet.getIntegers().writeSafely(0, 9);

        val compound = NbtFactory.ofCompound(null);
        compound.put("x", location.getX());
        compound.put("y", location.getY());
        compound.put("z", location.getZ());
        compound.put("id", SIGN_TYPE_ID);
        for (int i = 0; i < 4; ++i) {
            compound.put("Text" + (i + 1), ComponentUtils.serializeToJson(lines[i]));
        }
        packet.getNbtModifier().writeSafely(0, compound);

        return packet;
    }

    /**
     * Builds an Update Sign packet for Minecraft 1.8 and 1.9_R2, used to refresh the content of a sign.
     *
     * @param location The location of the sign
     * @param lines    The lines of the sign
     * @return The packet that was built
     * @deprecated Removed in Minecraft 1.9_R2.
     */
    @Deprecated
    private PacketContainer buildUpdateSignPacket(SignLocation location, Component[] lines) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.UPDATE_SIGN);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        val comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; ++i) {
            comps[i] = WrappedComponentUtils.serialize(lines[i]);
        }
        packet.getChatComponentArrays().writeSafely(0, comps);

        return packet;
    }

    /**
     * Translates the sign text using by mutating its NBT Tag Compound.
     *
     * @param compound Sign's NBT data
     * @param location The location of the sign
     * @param player   The language player to translate for
     * @return True if the sign was translated or false if left untouched
     */
    private boolean translateSignNbtCompound(NbtCompound compound, SignLocation location, LanguagePlayer player) {
        val sign = getTranslationManager().getSignComponents(player, location, () -> {
            val defaultLines = new Component[4];
            for (int i = 0; i < 4; i++) {
                try {
                    val nbtLine = compound.getStringOrDefault("Text" + (i + 1));
                    if (nbtLine != null) {
                        defaultLines[i] = ComponentUtils.deserializeFromJson(nbtLine);
                    }
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 at %2.", i + 1, location);
                }
            }
            return defaultLines;
        });

        if (sign.isPresent()) {
            val lines = sign.get();
            for (int i = 0; i < 4; i++) {
                compound.put("Text" + (i + 1), ComponentUtils.serializeToJson(lines[i]));
            }
            return true;
        }
        return false;
    }

    /**
     * Gets lines from a sign given its location.
     * If the sign cannot be found, an empty {@link Component} array is located.
     *
     * @param location The location of the sign
     * @return An array with length 4, representing each line of the sign
     */
    private Optional<Component[]> getSignLinesFromLocation(SignLocation location) {
        return SpigotTriton.asSpigot().callSync(() -> {
            val world = Bukkit.getWorld(location.getWorld());
            if (world == null) {
                return new Component[0];
            }

            val bukkitLocation = new Location(world, location.getX(), location.getY(), location.getZ());

            val state = bukkitLocation.getBlock().getState();
            if (!(state instanceof Sign)) {
                return new Component[0];
            }

            return Arrays.stream(((Sign) state).getLines())
                    .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                    .toArray(Component[]::new);
        });
    }

    @Override
    public void registerPacketTypes(Map<PacketType, HandlerFunction> registry) {
        if (getMcVersion() >= 18) {
            registry.put(PacketType.Play.Server.MAP_CHUNK, asAsync(this::handleLevelChunk));
            registry.put(PacketType.Play.Server.TILE_ENTITY_DATA, asAsync(this::handleTileEntityDataPost1_18));
        } else if (!MinecraftReflection.signUpdateExists()) {
            registry.put(PacketType.Play.Server.MAP_CHUNK, asAsync(this::handleMapChunk));
            registry.put(PacketType.Play.Server.TILE_ENTITY_DATA, asAsync(this::handleTileEntityDataPre1_18));
        } else {
            registry.put(PacketType.Play.Server.UPDATE_SIGN, asAsync(this::handleUpdateSign));
        }
    }
}
