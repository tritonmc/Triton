package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import com.rexcantor64.triton.wrappers.WrappedAdvancementDisplay;
import lombok.val;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

public class AdvancementsPacketHandler extends PacketHandler {

    private final Class<?> SERIALIZED_ADVANCEMENT_CLASS;
    private final FieldAccessor ADVANCEMENT_DISPLAY_FIELD;
    private final FieldAccessor ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD;
    private final MethodAccessor ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD;
    private final MethodAccessor CRAFT_SERVER_GET_SERVER_METHOD;
    private final MethodAccessor MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD;
    private final MethodAccessor ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD;

    public AdvancementsPacketHandler() {
        SERIALIZED_ADVANCEMENT_CLASS = MinecraftReflection.getMinecraftClass("advancements.Advancement$SerializedAdvancement");
        ADVANCEMENT_DISPLAY_FIELD = Accessors.getFieldAccessor(SERIALIZED_ADVANCEMENT_CLASS, WrappedAdvancementDisplay.getWrappedClass(), true);
        val advancementDataPlayerClass = MinecraftReflection.getMinecraftClass("server.AdvancementDataPlayer", "AdvancementDataPlayer");
        ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD = Accessors.getFieldAccessor(MinecraftReflection.getEntityPlayerClass(), advancementDataPlayerClass, true);
        ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD = Accessors.getMethodAccessor(advancementDataPlayerClass, "b", MinecraftReflection.getEntityPlayerClass());

        val advancementDataWorldClass = MinecraftReflection.getMinecraftClass("server.AdvancementDataWorld", "AdvancementDataWorld");
        CRAFT_SERVER_GET_SERVER_METHOD = Accessors.getMethodAccessor(MinecraftReflection.getCraftBukkitClass("CraftServer"), "getServer");
        MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD = Accessors.getMethodAccessor(Arrays.stream(MinecraftReflection.getMinecraftServerClass().getMethods())
                .filter(m -> m.getReturnType() == advancementDataWorldClass).findAny()
                .orElseThrow(() -> new RuntimeException("Unable to find method getAdvancementData([])")));

        ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD = Accessors.getMethodAccessor(advancementDataPlayerClass, "a", advancementDataWorldClass);
    }

    /**
     * @return Whether the plugin should attempt to translate advancements
     */
    private boolean areAdvancementsDisabled() {
        return !getMain().getConf().isAdvancements();
    }

    private void handleAdvancements(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areAdvancementsDisabled()) return;

        val serializedAdvancementMap = packet.getPacket().getMaps(MinecraftKey.getConverter(), Converters.passthrough(SERIALIZED_ADVANCEMENT_CLASS)).readSafely(0);

        for (Object serializedAdvancement : serializedAdvancementMap.values()) {
            val advancementDisplayHandle = ADVANCEMENT_DISPLAY_FIELD.get(serializedAdvancement);
            if (advancementDisplayHandle == null) continue;

            val advancementDisplay = WrappedAdvancementDisplay.fromHandle(advancementDisplayHandle).shallowClone();

            val originalTitle = ComponentSerializer.parse(advancementDisplay.getTitle().getJson());
            val originalDescription = ComponentSerializer.parse(advancementDisplay.getDescription().getJson());

            val translatedTitle = getLanguageParser().parseComponent(languagePlayer, getMain().getConf().getAdvancementsSyntax(), originalTitle);
            val translatedDescription = getLanguageParser().parseComponent(languagePlayer, getMain().getConf().getAdvancementsSyntax(), originalDescription);

            advancementDisplay.setTitle(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedTitle)));
            advancementDisplay.setDescription(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedDescription)));

            ADVANCEMENT_DISPLAY_FIELD.set(serializedAdvancement, advancementDisplay.getHandle());
        }
    }

    /**
     * Forcefully refresh the advancements for a given player.
     * To achieve this, achievements are loaded from the server's state onto the Player,
     * and then sent. This is what NMS does under the hood, we're just doing the same thing here manually.
     *
     * @param languagePlayer The player to refresh the advancements for
     */
    public void refreshAdvancements(SpigotLanguagePlayer languagePlayer) {
        if (areAdvancementsDisabled()) return;

        val bukkitPlayer = languagePlayer.toBukkit();
        val nmsPlayer = NMSUtils.getHandle(bukkitPlayer);

        val advancementDataPlayer = ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD.get(nmsPlayer);
        val minecraftServer = CRAFT_SERVER_GET_SERVER_METHOD.invoke(Bukkit.getServer());
        val advancementDataWorld = MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD.invoke(minecraftServer);

        ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD.invoke(advancementDataPlayer, advancementDataWorld);
        ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD.invoke(advancementDataPlayer, nmsPlayer);
    }

    @Override
    public void registerPacketTypes(Map<PacketType, BiConsumer<PacketEvent, SpigotLanguagePlayer>> registry) {
        registry.put(PacketType.Play.Server.ADVANCEMENTS, this::handleAdvancements);
    }
}
