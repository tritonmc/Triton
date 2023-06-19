package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import com.rexcantor64.triton.utils.RegistryUtils;
import lombok.val;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rexcantor64.triton.packetinterceptor.protocollib.HandlerFunction.asAsync;

@SuppressWarnings({"deprecation"})
public class SignPacketHandler extends PacketHandler {

    private final Class<?> LEVEL_CHUNK_PACKET_DATA_CLASS;
    private final Class<?> TILE_ENTITY_TYPES_CLASS;
    private final String SIGN_TYPE_ID;
    private final String HANGING_SIGN_TYPE_ID;

    public SignPacketHandler() {
        LEVEL_CHUNK_PACKET_DATA_CLASS = getMcVersion() >= 18 ?
                NMSUtils.getClass("net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData") : null;
        TILE_ENTITY_TYPES_CLASS = getMcVersion() >= 18 ?
                NMSUtils.getClass("net.minecraft.world.level.block.entity.TileEntityTypes") : null;
        SIGN_TYPE_ID = getMcVersion() >= 11 ? "minecraft:sign" : "Sign";
        HANGING_SIGN_TYPE_ID = "minecraft:hanging_sign";
    }

    /**
     * @return Whether the plugin should attempt to translate signs
     */
    private boolean areSignsDisabled() {
        return !getMain().getConf().isSigns();
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
        val blockEntities = (List<?>) NMSUtils.getDeclaredField(chunkData, "d");

        // Each block entity is in an array and its type needs to be checked.
        for (Object blockEntity : blockEntities) {
            val nmsNbtTagCompound = NMSUtils.getDeclaredField(blockEntity, "d");
            if (nmsNbtTagCompound == null) continue;

            // Try to determine type
            val tileEntityType = NMSUtils.getDeclaredField(blockEntity, "c");
            val tileEntityTypeKey = RegistryUtils.getTileEntityTypeKey(tileEntityType);
            if (!SIGN_TYPE_ID.equals(tileEntityTypeKey) && !HANGING_SIGN_TYPE_ID.equals(tileEntityTypeKey)) continue;

            // The NBT compound below does not include the position, so we have to calculate it
            // from the chunk position and block section position
            val encodedPosition = (int) NMSUtils.getDeclaredField(blockEntity, "a");
            val sectionX = encodedPosition >> 4;
            val sectionZ = encodedPosition & 15;
            val y = (int) NMSUtils.getDeclaredField(blockEntity, "b");

            val nbt = NbtFactory.fromNMSCompound(nmsNbtTagCompound);

            val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                    chunkX * 16 + sectionX, y, chunkZ * 16 + sectionZ);
            translateSignNbtCompound(nbt, location, languagePlayer, true, tileEntityType);
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
        val tileEntityTypeKey = RegistryUtils.getTileEntityTypeKey(tileEntityType);
        if (SIGN_TYPE_ID.equals(tileEntityTypeKey) || HANGING_SIGN_TYPE_ID.equals(tileEntityTypeKey)) {
            val position = packet.getPacket().getBlockPositionModifier().readSafely(0);
            val nbt = NbtFactory.asCompound(packet.getPacket().getNbtModifier().readSafely(0));
            val location = new SignLocation(packet.getPlayer().getWorld().getName(),
                    position.getX(), position.getY(), position.getZ());

            translateSignNbtCompound(nbt, location, languagePlayer, true, tileEntityType);
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
                translateSignNbtCompound(nbt, location, languagePlayer, true, null);
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
            if (translateSignNbtCompound(nbt, location, languagePlayer, true, null)) {
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
        if (areSignsDisabled()) return;

        val newPacket = packet.getPacket().shallowClone();
        val pos = newPacket.getBlockPositionModifier().readSafely(0);
        val linesModifier = newPacket.getChatComponentArrays();
        val defaultLinesWrapped = linesModifier.readSafely(0);

        val l = new SignLocation(packet.getPlayer().getWorld().getName(), pos.getX(), pos.getY(), pos.getZ());
        String[] lines = getLanguageManager().getSign(languagePlayer, l, () -> {
            val defaultLines = new String[4];
            for (int i = 0; i < 4; i++) {
                try {
                    defaultLines[i] = AdvancedComponent
                            .fromBaseComponent(ComponentSerializer.parse(defaultLinesWrapped[i].getJson()))
                            .getTextClean();
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 at %2.", i + 1, l);
                    defaultLines[i] = "";
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

        languagePlayer.getLegacySigns().put(l, Arrays.stream(defaultLinesWrapped).map(WrappedChatComponent::getJson).toArray(String[]::new));

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

        player.getSigns().forEach(((signLocation, sign) -> {
            val compoundClone = NbtFactory.asCompound(sign.getCompound().deepClone());

            if (translateSignNbtCompound(compoundClone, signLocation, player, false, null)) {
                PacketContainer packet;
                if (getMcVersion() >= 18) {
                    packet = buildTileEntityDataPacketPost1_18(signLocation, compoundClone, sign.getTileEntityType());
                } else {
                    packet = buildTileEntityDataPacketPre1_18(signLocation, compoundClone);
                }

                ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, false);
            }
        }));

        player.getLegacySigns().forEach(((signLocation, lines) -> {
            val resultLines = getLanguageManager().getSign(player, signLocation, () -> {
                val defaultLines = new String[4];
                for (int i = 0; i < 4; i++) {
                    try {
                        defaultLines[i] = AdvancedComponent
                                .fromBaseComponent(ComponentSerializer.parse(lines[i]))
                                .getTextClean();
                    } catch (Exception e) {
                        Triton.get().getLogger().logError(e, "Failed to parse sign line %1 at %2.", i + 1, signLocation);
                        defaultLines[i] = "";
                    }
                }
                return defaultLines;
            });

            PacketContainer packet = buildUpdateSignPacket(signLocation, resultLines);
            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, false);
        }));
    }

    /**
     * Builds a Tile Entity Data packet for Minecraft 1.18 and above, used to refresh the content of a sign.
     *
     * @param location       The location of the sign
     * @param compound       The NBT Compound of the sign
     * @param tileEntityType The tile entity type of the sign (NMS Object)
     * @return The packet that was built
     */
    @SuppressWarnings({"unchecked"})
    private PacketContainer buildTileEntityDataPacketPost1_18(SignLocation location, NbtCompound compound, Object tileEntityType) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        packet.getSpecificModifier((Class<Object>) TILE_ENTITY_TYPES_CLASS).writeSafely(0, tileEntityType);

        packet.getNbtModifier().writeSafely(0, compound);

        return packet;
    }

    /**
     * Builds a Tile Entity Data packet for Minecraft 1.17 and below, used to refresh the content of a sign.
     *
     * @param location The location of the sign
     * @param compound The NBT Compound of the sign
     * @return The packet that was built
     * @deprecated Changed in Minecraft 1.18.
     */
    @Deprecated
    private PacketContainer buildTileEntityDataPacketPre1_18(SignLocation location, NbtCompound compound) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        // Update sign action ID is 9
        packet.getIntegers().writeSafely(0, 9);

        packet.getNbtModifier().writeSafely(0, compound);

        return packet;
    }

    /**
     * Builds an Update Sign packet for Minecraft 1.8 and 1.9_R1, used to refresh the content of a sign.
     *
     * @param location The location of the sign
     * @param lines    The lines of the sign
     * @return The packet that was built
     * @deprecated Removed in Minecraft 1.9_R2.
     */
    @Deprecated
    private PacketContainer buildUpdateSignPacket(SignLocation location, String[] lines) {
        val packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.UPDATE_SIGN);

        packet.getBlockPositionModifier().writeSafely(0,
                new BlockPosition(location.getX(), location.getY(), location.getZ()));

        val comps = new WrappedChatComponent[4];
        for (int i = 0; i < 4; ++i)
            comps[i] =
                    WrappedChatComponent.fromJson(ComponentSerializer
                            .toString(TextComponent.fromLegacyText(lines[i])));
        packet.getChatComponentArrays().writeSafely(0, comps);

        return packet;
    }

    /**
     * Translates the sign text using by mutating its NBT Tag Compound.
     *
     * @param compound       Sign's NBT data
     * @param location       The location of the sign
     * @param player         The language player to translate for
     * @param saveToCache    Whether to save the location and original compound to the player's cache
     * @param tileEntityType The tile entity type of the sign (NMS Object)
     * @return True if the sign was translated or false if left untouched
     */
    private boolean translateSignNbtCompound(NbtCompound compound, SignLocation location, SpigotLanguagePlayer player,
                                             boolean saveToCache, @Nullable Object tileEntityType) {
        if (MinecraftVersion.TRAILS_AND_TAILS.atOrAbove()) {
            return translateSignNbtCompoundPost1_20(compound, location, player, saveToCache, tileEntityType);
        }
        return translateSignNbtCompoundPre1_20(compound, location, player, saveToCache, tileEntityType);
    }

    /**
     * @see this#translateSignNbtCompound(NbtCompound, SignLocation, SpigotLanguagePlayer, boolean, Object)
     * @deprecated Since 3.9.0.
     */
    @Deprecated
    private boolean translateSignNbtCompoundPre1_20(NbtCompound compound, SignLocation location, SpigotLanguagePlayer player,
                                                    boolean saveToCache, @Nullable Object tileEntityType) {
        String[] sign = getLanguageManager().getSign(player, location, () -> {
            val defaultLines = new String[4];
            for (int i = 0; i < 4; i++) {
                try {
                    val nbtLine = compound.getStringOrDefault("Text" + (i + 1));
                    if (nbtLine != null)
                        defaultLines[i] = AdvancedComponent
                                .fromBaseComponent(ComponentSerializer.parse(nbtLine))
                                .getTextClean();
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 at %2.", i + 1, location);
                }
            }
            return defaultLines;
        });

        if (sign != null) {
            val compoundClone = saveToCache ? NbtFactory.asCompound(compound.deepClone()) : null;
            for (int i = 0; i < 4; i++) {
                compound.put("Text" + (i + 1), ComponentSerializer.toString(TextComponent.fromLegacyText(sign[i])));
            }
            if (compoundClone != null) {
                player.saveSign(location, tileEntityType, compoundClone);
            }
            return true;
        }
        return false;
    }

    /**
     * @see this#translateSignNbtCompound(NbtCompound, SignLocation, SpigotLanguagePlayer, boolean, Object)
     * @since 3.9.0
     */
    private boolean translateSignNbtCompoundPost1_20(NbtCompound compound, SignLocation location, SpigotLanguagePlayer player,
                                                     boolean saveToCache, @Nullable Object tileEntityType) {
        String[] sign = getLanguageManager().getSign(player, location, () -> {
            val defaultLines = new String[8];
            val frontText = compound.getCompound("front_text");
            val frontTextMessages = frontText.<String>getList("messages");
            for (int i = 0; i < 4; i++) {
                try {
                    val nbtLine = frontTextMessages.getValue(i);
                    if (nbtLine != null)
                        defaultLines[i] = AdvancedComponent
                                .fromBaseComponent(ComponentSerializer.parse(nbtLine))
                                .getTextClean();
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 (front) at %2.", i + 1, location);
                }
            }
            val backText = compound.getCompound("back_text");
            val backTextMessages = backText.<String>getList("messages");
            for (int i = 0; i < 4; i++) {
                try {
                    val nbtLine = backTextMessages.getValue(i);
                    if (nbtLine != null)
                        defaultLines[i + 4] = AdvancedComponent
                                .fromBaseComponent(ComponentSerializer.parse(nbtLine))
                                .getTextClean();
                } catch (Exception e) {
                    Triton.get().getLogger().logError(e, "Failed to parse sign line %1 (back) at %2.", i + 1, location);
                }
            }
            return defaultLines;
        });

        if (sign != null) {
            val compoundClone = saveToCache ? NbtFactory.asCompound(compound.deepClone()) : null;

            val frontText = compound.getCompound("front_text");
            val frontTextMessages = Arrays.stream(sign, 0, 4)
                    .map(TextComponent::fromLegacyText)
                    .map(ComponentSerializer::toString)
                    .collect(Collectors.toList());
            frontText.put("messages", NbtFactory.ofList("messages", frontTextMessages));

            val backText = compound.getCompound("back_text");
            val backTextMessages = Arrays.stream(sign, 4, 8)
                    .map(TextComponent::fromLegacyText)
                    .map(ComponentSerializer::toString)
                    .collect(Collectors.toList());
            backText.put("messages", NbtFactory.ofList("messages", backTextMessages));

            if (compoundClone != null) {
                player.saveSign(location, tileEntityType, compoundClone);
            }
            return true;
        }
        return false;
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
