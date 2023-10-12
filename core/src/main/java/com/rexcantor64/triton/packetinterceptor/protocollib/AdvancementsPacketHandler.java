package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import com.rexcantor64.triton.wrappers.WrappedAdvancementDisplay;
import com.rexcantor64.triton.wrappers.WrappedAdvancementHolder;
import lombok.val;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.rexcantor64.triton.packetinterceptor.protocollib.HandlerFunction.asAsync;

public class AdvancementsPacketHandler extends PacketHandler {
    private final Class<?> SERIALIZED_ADVANCEMENT_CLASS;
    private final FieldAccessor ADVANCEMENT_DISPLAY_FIELD;
    private final FieldAccessor ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD;
    private final MethodAccessor ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD;
    private final MethodAccessor CRAFT_SERVER_GET_SERVER_METHOD;
    private final MethodAccessor MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD;
    private final MethodAccessor ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD;

    private AdvancementsPacketHandler() {
        if (!MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE.atOrAbove()) {
            SERIALIZED_ADVANCEMENT_CLASS = MinecraftReflection.getMinecraftClass(
                    "advancements.Advancement$SerializedAdvancement",
                    "Advancement$SerializedAdvancement"
            );
            ADVANCEMENT_DISPLAY_FIELD = Accessors.getFieldAccessor(
                    SERIALIZED_ADVANCEMENT_CLASS,
                    WrappedAdvancementDisplay.getWrappedClass(),
                    true
            );
        } else {
            SERIALIZED_ADVANCEMENT_CLASS = null;
            ADVANCEMENT_DISPLAY_FIELD = null;
        }
        val advancementDataPlayerClass = MinecraftReflection.getMinecraftClass("server.AdvancementDataPlayer", "AdvancementDataPlayer");
        ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD = Accessors.getFieldAccessor(MinecraftReflection.getEntityPlayerClass(), advancementDataPlayerClass, true);
        ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD = Accessors.getMethodAccessor(advancementDataPlayerClass, "b", MinecraftReflection.getEntityPlayerClass());

        val advancementDataWorldClass = MinecraftReflection.getMinecraftClass("server.AdvancementDataWorld", "AdvancementDataWorld");
        CRAFT_SERVER_GET_SERVER_METHOD = Accessors.getMethodAccessor(MinecraftReflection.getCraftBukkitClass("CraftServer"), "getServer");
        MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD = Accessors.getMethodAccessor(Arrays.stream(MinecraftReflection.getMinecraftServerClass().getMethods())
                .filter(m -> m.getReturnType() == advancementDataWorldClass).findAny()
                .orElseThrow(() -> new RuntimeException("Unable to find method getAdvancementData([])")));

        if (getMcVersion() < 16) {
            // MC 1.12-1.15
            // Loading of achievements only needs the method to be called without any parameters
            ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD = Accessors.getMethodAccessor(advancementDataPlayerClass, "b");
        } else {
            // MC 1.16+
            // Loading of achievements requires an AdvancementDataWorld method
            ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD = Accessors.getMethodAccessor(advancementDataPlayerClass, "a", advancementDataWorldClass);
        }
    }

    /**
     * Build a new instance of {@link AdvancementsPacketHandler} if supported by the current
     * Minecraft version (1.12 and above).
     *
     * @return A new instance of this class.
     * @since 4.0.0
     */
    public static @Nullable AdvancementsPacketHandler newInstance() {
        try {
            if (MinecraftVersion.COLOR_UPDATE.atOrAbove()) {
                // MC 1.12+
                return new AdvancementsPacketHandler();
            }
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to hook into advancements packets. Advancements translation will not work.");
        }
        return null;
    }

    /**
     * @return Whether the plugin should attempt to translate advancements
     */
    private boolean areAdvancementsDisabled() {
        return !getMain().getConf().isAdvancements();
    }

    /**
     * @return Whether the plugin should attempt to refresh translated advancements
     */
    private boolean areAdvancementsRefreshDisabled() {
        return areAdvancementsDisabled() || !getMain().getConfig().isAdvancementsRefresh();
    }

    private void handleAdvancementsPre1_20_2(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areAdvancementsDisabled()) return;

        val serializedAdvancementMap = packet.getPacket().getMaps(MinecraftKey.getConverter(), Converters.passthrough(SERIALIZED_ADVANCEMENT_CLASS)).readSafely(0);

        for (Object serializedAdvancement : serializedAdvancementMap.values()) {
            val advancementDisplayHandle = ADVANCEMENT_DISPLAY_FIELD.get(serializedAdvancement);
            if (advancementDisplayHandle == null) continue;

            val advancementDisplay = WrappedAdvancementDisplay.fromHandle(advancementDisplayHandle).shallowClone();
            translateAdvancementDisplay(advancementDisplay, languagePlayer);
            if (advancementDisplay.hasChangedAndReset()) {
                ADVANCEMENT_DISPLAY_FIELD.set(serializedAdvancement, advancementDisplay.getHandle());
            }
        }
    }

    private void handleAdvancementsPost1_20_2(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areAdvancementsDisabled()) return;

        val advancementHolders = packet.getPacket().getLists(WrappedAdvancementHolder.CONVERTER).readSafely(0);

        for (WrappedAdvancementHolder advancementHolder : advancementHolders) {
            val advancement = advancementHolder.getAdvancement();
            val advancementDisplayOpt = advancement.getAdvancementDisplay();
            if (!advancementDisplayOpt.isPresent()) {
                continue;
            }

            val advancementDisplay = advancementDisplayOpt.get().shallowClone();
            translateAdvancementDisplay(advancementDisplay, languagePlayer);
            if (advancementDisplay.hasChangedAndReset()) {
                val advancementClone = advancement.shallowClone();
                advancementClone.setAdvancementDisplay(Optional.of(advancementDisplay));
                advancementHolder.setAdvancement(advancementClone);
            }
        }
    }

    private void translateAdvancementDisplay(WrappedAdvancementDisplay advancementDisplay, Localized locale) {
        val originalTitle = ComponentSerializer.parse(advancementDisplay.getTitle().getJson());
        val originalDescription = ComponentSerializer.parse(advancementDisplay.getDescription().getJson());

        val translatedTitle = getLanguageParser().parseComponent(locale, getMain().getConf().getAdvancementsSyntax(), originalTitle);
        val translatedDescription = getLanguageParser().parseComponent(locale, getMain().getConf().getAdvancementsSyntax(), originalDescription);

        advancementDisplay.setTitle(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedTitle)));
        advancementDisplay.setDescription(WrappedChatComponent.fromJson(ComponentSerializer.toString(translatedDescription)));
    }

    /**
     * Forcefully refresh the advancements for a given player.
     * To achieve this, achievements are loaded from the server's state onto the Player,
     * and then sent. This is what NMS does under the hood, we're just doing the same thing here manually.
     *
     * @param languagePlayer The player to refresh the advancements for
     */
    public void refreshAdvancements(SpigotLanguagePlayer languagePlayer) {
        if (areAdvancementsRefreshDisabled()) return;

        languagePlayer.toBukkit().ifPresent(bukkitPlayer -> {
            val nmsPlayer = NMSUtils.getHandle(bukkitPlayer);

            val advancementDataPlayer = ENTITY_PLAYER_ADVANCEMENT_DATA_PLAYER_FIELD.get(nmsPlayer);

            Bukkit.getScheduler().runTask(getMain().getLoader(), () -> {
                // These are the same methods that are called from org.bukkit.craftbukkit.<version>.util.CraftMagicNumbers#loadAdvancement
                if (getMcVersion() < 16) {
                    // MC 1.12-1.15
                    ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD.invoke(advancementDataPlayer);
                } else {
                    // MC 1.16+
                    val minecraftServer = CRAFT_SERVER_GET_SERVER_METHOD.invoke(Bukkit.getServer());
                    val advancementDataWorld = MINECRAFT_SERVER_GET_ADVANCEMENT_DATA_METHOD.invoke(minecraftServer);
                    ADVANCEMENT_DATA_PLAYER_LOAD_FROM_ADVANCEMENT_DATA_WORLD_METHOD.invoke(advancementDataPlayer, advancementDataWorld);
                }
                ADVANCEMENT_DATA_PLAYER_REFRESH_METHOD.invoke(advancementDataPlayer, nmsPlayer);
            });
        });
    }

    @Override
    public void registerPacketTypes(Map<PacketType, HandlerFunction> registry) {
        if (MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE.atOrAbove()) {
            // MC 1.20.2+
            registry.put(PacketType.Play.Server.ADVANCEMENTS, asAsync(this::handleAdvancementsPost1_20_2));
        } else {
            registry.put(PacketType.Play.Server.ADVANCEMENTS, asAsync(this::handleAdvancementsPre1_20_2));
        }
    }
}
