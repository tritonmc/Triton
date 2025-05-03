package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class EntityPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;
    private final boolean allowAll;
    private final @NotNull List<EntityType> allowedEntities;

    public EntityPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config, @NotNull List<EntityType> allowedEntities) {
        this.parser = parser;
        this.syntax = config.getHologramSyntax();
        this.allowAll = config.isHologramsAll();
        this.allowedEntities = allowedEntities;
    }

    public void onSpawnEntityPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSpawnEntity(event);

        val isAllowed = this.allowAll || this.allowedEntities.contains(packet.getEntityType());
        if (isAllowed) {
            if (packet.getEntityType().equals(EntityTypes.PLAYER)) {
                val uuidOpt = packet.getUUID();
                val playerOpt = uuidOpt.flatMap(uuid -> languagePlayer.getPacketEventsRefresh().getTentativePlayer(uuid));
                if (!playerOpt.isPresent()) {
                    return;
                }
                val player = playerOpt.get();

                languagePlayer.getPacketEventsRefresh().saveSpawnedPlayer(packet.getEntityId(), player);
                player.setPosition(packet.getPosition());
                player.setPitch(packet.getPitch());
                player.setYaw(packet.getYaw());
                player.setHeadYaw(packet.getHeadYaw());
            } else {
                languagePlayer.getPacketEventsRefresh().saveEntity(packet.getEntityId());
            }
        }
    }

    public void onSpawnLivingEntityPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSpawnLivingEntity(event);

        val isAllowed = this.allowAll || this.allowedEntities.contains(packet.getEntityType());
        if (isAllowed) {
            languagePlayer.getPacketEventsRefresh().saveEntity(packet.getEntityId());
        }
    }

    public void onSpawnPlayerPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSpawnPlayer(event);

        val isAllowed = this.allowAll || this.allowedEntities.contains(EntityTypes.PLAYER);
        if (isAllowed) {
            val uuid = packet.getUUID();
            val playerOpt = languagePlayer.getPacketEventsRefresh().getTentativePlayer(uuid);
            if (!playerOpt.isPresent()) {
                return;
            }
            val player = playerOpt.get();

            languagePlayer.getPacketEventsRefresh().saveSpawnedPlayer(packet.getEntityId(), player);
            player.setPosition(packet.getPosition());
            player.setPitch(packet.getPitch());
            player.setYaw(packet.getYaw());
        }
    }

    public void onDestroyEntitiesPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerDestroyEntities(event);

        for (int id : packet.getEntityIds()) {
            languagePlayer.getPacketEventsRefresh().discardEntity(id);
            languagePlayer.getPacketEventsRefresh().discardSpawnedPlayer(id);
        }
    }

    @SuppressWarnings("unchecked")
    public void onEntityMetadataPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityMetadata(event);

        val entityCacheOpt = languagePlayer.getPacketEventsRefresh().getEntity(packet.getEntityId());
        if (!entityCacheOpt.isPresent()) {
            // If it's not cached, it means we don't want to translate it
            return;
        }
        val entityCache = entityCacheOpt.get();

        val metadata = packet.getEntityMetadata();
        val hideCustomName = new AtomicBoolean();
        for (EntityData data : metadata) {
            val index = data.getIndex();
            val value = data.getValue();
            if (index == 2) {
                // Index 2 is "Custom Name" of type "OptChat"
                // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Entity
                val customName = (Optional<Component>) value;
                if (customName.isPresent()) {
                    val name = customName.get();
                    parser.translateComponent(
                                    name,
                                    languagePlayer,
                                    syntax
                            )
                            .ifChanged(result -> {
                                entityCache.setDisplayName(name);
                                data.setValue(Optional.ofNullable(result));
                                event.markForReEncode(true);
                            })
                            .ifToRemove(() -> {
                                entityCache.setDisplayName(name);
                                data.setValue(Optional.empty());
                                hideCustomName.set(true);
                                event.markForReEncode(true);
                            })
                            .ifUnchanged(() -> entityCache.setDisplayName(null));
                } else {
                    entityCache.setDisplayName(null);
                }
            } else if (index == 3) {
                // Index 3 is "Is custom name visible" of type "Boolean"
                // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Entity
                entityCache.setShowDisplayName((boolean) data.getValue());
                if (hideCustomName.get()) {
                    data.setValue(false);
                }
            } else if (index == 8) {
                // Index 8 is "Item" of type "Slot"
                // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Entity
                // Used to translate items inside (glowing) item frames
                // TODO
            } else if ((index == 22 || index == 23) && value instanceof Component) {
                // Index 22/23 is "Text" of type "Chat"
                // https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Entity_metadata#Text_Display
                // Used to translate text display entities
                val text = (Component) value;
                parser.translateComponent(
                                text,
                                languagePlayer,
                                syntax
                        )
                        .ifUnchanged(() -> entityCache.setTextDisplayContent(null))
                        .getResultOrToRemove(Component::empty)
                        .ifPresent(result -> {
                            entityCache.setTextDisplayContent(text);
                            data.setValue(result);
                            event.markForReEncode(true);
                        });
            }
        }
    }

    public void onEntityPositionSync(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityPositionSync(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getId())
                .ifPresent(player -> {
                    val values = packet.getValues();
                    player.setPosition(values.getPosition());
                    player.setPitch(values.getPitch());
                    player.setYaw(values.getYaw());
                });
    }

    public void onEntityTeleport(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        if (event.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)) {
            // This packet doesn't do the same thing on 1.21.2+, so ignore it
            return;
        }
        val packet = new WrapperPlayServerEntityTeleport(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getEntityId())
                .ifPresent(player -> {
                    val values = packet.getValues();
                    player.setPosition(values.getPosition());
                    player.setPitch(values.getPitch());
                    player.setYaw(values.getYaw());
                });
    }

    public void onEntityRelativeMove(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityRelativeMove(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getEntityId())
                .ifPresent(player -> player.setPosition(player.getPosition().add(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ())));
    }

    public void onEntityRelativeMoveAndRotation(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getEntityId())
                .ifPresent(player -> {
                    player.setPosition(player.getPosition().add(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ()));
                    player.setPitch(packet.getPitch());
                    player.setYaw(packet.getYaw());
                });
    }

    public void onEntityRotation(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityRotation(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getEntityId())
                .ifPresent(player -> {
                    player.setPitch(packet.getPitch());
                    player.setYaw(packet.getYaw());
                });
    }

    public void onEntityHeadLook(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerEntityHeadLook(event);
        languagePlayer.getPacketEventsRefresh().getSpawnedPlayer(packet.getEntityId())
                .ifPresent(player -> player.setHeadYaw(packet.getHeadYaw()));
    }

    public void onJoinGame(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        languagePlayer.getPacketEventsRefresh().discardAllEntitiesAndPlayers();
    }
}
