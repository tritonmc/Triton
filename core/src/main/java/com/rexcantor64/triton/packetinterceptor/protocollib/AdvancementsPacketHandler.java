package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.wrappers.WrappedAdvancementDisplay;
import lombok.val;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Map;
import java.util.function.BiConsumer;

public class AdvancementsPacketHandler extends PacketHandler {

    private final Class<?> SERIALIZED_ADVANCEMENT_CLASS;
    private final FieldAccessor ADVANCEMENT_DISPLAY_FIELD;

    public AdvancementsPacketHandler() {
        SERIALIZED_ADVANCEMENT_CLASS = MinecraftReflection.getMinecraftClass("advancements.Advancement$SerializedAdvancement");
        ADVANCEMENT_DISPLAY_FIELD = Accessors.getFieldAccessor(SERIALIZED_ADVANCEMENT_CLASS, WrappedAdvancementDisplay.getWrappedClass(), true);
    }

    /**
     * @return Whether the plugin should attempt to translate advancements
     */
    private boolean areSignsDisabled() {
        return !getMain().getConf().isSigns();
    }

    private void handleAdvancements(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        val serializedAdvancementMap = packet.getPacket().getMaps(MinecraftKey.getConverter(), Converters.passthrough(SERIALIZED_ADVANCEMENT_CLASS)).readSafely(0);

        for (Object serializedAdvancement : serializedAdvancementMap.values()) {
            val advancementDisplayHandle = ADVANCEMENT_DISPLAY_FIELD.get(serializedAdvancement);
            if (advancementDisplayHandle == null) continue;

            val advancementDisplay = WrappedAdvancementDisplay.fromHandle(advancementDisplayHandle).shallowClone();

            val originalTitle = ComponentSerializer.parse(advancementDisplay.getTitle().getJson());
            val originalDescription = ComponentSerializer.parse(advancementDisplay.getDescription().getJson());

            // TODO syntax
            val translatedTitle = getLanguageParser().parseComponent(languagePlayer, getMain().getConf().getChatSyntax(), originalTitle);
            val translatedDescription = getLanguageParser().parseComponent(languagePlayer, getMain().getConf().getChatSyntax(), originalDescription);

            advancementDisplay.setTitle(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedTitle)));
            advancementDisplay.setDescription(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedDescription)));

            ADVANCEMENT_DISPLAY_FIELD.set(serializedAdvancement, advancementDisplay.getHandle());
        }
    }

    @Override
    public void registerPacketTypes(Map<PacketType, BiConsumer<PacketEvent, SpigotLanguagePlayer>> registry) {
        registry.put(PacketType.Play.Server.ADVANCEMENTS, this::handleAdvancements);
    }
}
