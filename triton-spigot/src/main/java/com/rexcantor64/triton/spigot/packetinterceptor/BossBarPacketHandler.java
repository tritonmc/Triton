package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.utils.WrappedComponentUtils;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asAsync;

public class BossBarPacketHandler extends PacketHandler {

    private final Class<?> ACTION_ENUM_CLASS;
    private final Class<?> OPERATION_INTERFACE;
    private final MethodAccessor GET_OPERATION_ACTION_METHOD;
    private final FieldAccessor ADD_COMPONENT_FIELD;
    private final FieldAccessor UPDATE_COMPONENT_FIELD;
    private final ConstructorAccessor UPDATE_CONSTRUCTOR;
    private final EquivalentConverter<Action> ACTION_CONVERTER;
    private final EquivalentConverter<WrappedChatComponent> COMPONENT_CONVERTER = BukkitConverters.getWrappedChatComponentConverter();

    public BossBarPacketHandler() {
        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            val innerClasses = PacketType.Play.Server.BOSS.getPacketClass().getDeclaredClasses();
            ACTION_ENUM_CLASS = Arrays.stream(innerClasses)
                    .filter(Enum.class::isAssignableFrom)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Cannot find boss bar action enum class"));

            // the operation interface has a method that returns the operation type
            OPERATION_INTERFACE = Arrays.stream(innerClasses)
                    .filter(Class::isInterface)
                    .filter(c -> !FuzzyReflection.fromClass(c).getMethodListByParameters(ACTION_ENUM_CLASS).isEmpty())
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Cannot find boss bar operation interface class"));
            GET_OPERATION_ACTION_METHOD = Accessors.getMethodAccessor(
                    FuzzyReflection.fromClass(OPERATION_INTERFACE)
                            .getMethodByReturnTypeAndParameters("getType", ACTION_ENUM_CLASS)
            );

            // the add operation class has a chat component field, but does not have a chat component constructor
            val addOperationClass = Arrays.stream(innerClasses)
                    .filter(OPERATION_INTERFACE::isAssignableFrom)
                    .filter(c -> !FuzzyReflection.fromClass(c, true).getFieldListByType(MinecraftReflection.getIChatBaseComponentClass()).isEmpty())
                    .filter(c -> FuzzyReflection.fromClass(c, true)
                            .getConstructorList(
                                    FuzzyMethodContract.newBuilder()
                                            .parameterExactType(MinecraftReflection.getIChatBaseComponentClass())
                                            .build()
                            )
                            .isEmpty()
                    )
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Cannot find boss bar add operation class"));
            ADD_COMPONENT_FIELD = Accessors.getFieldAccessor(addOperationClass, MinecraftReflection.getIChatBaseComponentClass(), true);

            // the update operation class has a chat component field, and also has a chat component constructor
            val updateOperationClass = Arrays.stream(innerClasses)
                    .filter(OPERATION_INTERFACE::isAssignableFrom)
                    .filter(c -> !FuzzyReflection.fromClass(c, true).getFieldListByType(MinecraftReflection.getIChatBaseComponentClass()).isEmpty())
                    .filter(c -> !FuzzyReflection.fromClass(c, true)
                            .getConstructorList(
                                    FuzzyMethodContract.newBuilder()
                                            .parameterExactType(MinecraftReflection.getIChatBaseComponentClass())
                                            .build()
                            )
                            .isEmpty()
                    )
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Cannot find boss bar add operation class"));
            UPDATE_COMPONENT_FIELD = Accessors.getFieldAccessor(updateOperationClass, MinecraftReflection.getIChatBaseComponentClass(), true);

            UPDATE_CONSTRUCTOR = Accessors.getConstructorAccessor(
                    FuzzyReflection.fromClass(updateOperationClass, true)
                            .getConstructor(
                                    FuzzyMethodContract.newBuilder()
                                            .parameterExactType(MinecraftReflection.getIChatBaseComponentClass())
                                            .build()
                            )
            );

            ACTION_CONVERTER = new EnumWrappers.EnumConverter<>(ACTION_ENUM_CLASS, Action.class);
        } else {
            ACTION_ENUM_CLASS = null;
            OPERATION_INTERFACE = null;
            GET_OPERATION_ACTION_METHOD = null;
            ADD_COMPONENT_FIELD = null;
            UPDATE_COMPONENT_FIELD = null;
            UPDATE_CONSTRUCTOR = null;
            ACTION_CONVERTER = null;
        }
    }

    /**
     * @return Whether the plugin should attempt to translate boss bars
     */
    private boolean areBossBarsDisabled() {
        return !getConfig().isBossbars();
    }

    private void handleBoss_1_9(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areBossBarsDisabled()) return;

        val uuid = packet.getPacket().getUUIDs().readSafely(0);
        Action action = packet.getPacket().getEnumModifier(Action.class, 1).readSafely(0);
        if (action == Action.REMOVE) {
            languagePlayer.removeBossbar(uuid);
            return;
        }
        if (action != Action.ADD && action != Action.UPDATE_NAME) return;

        WrappedChatComponent component = packet.getPacket().getChatComponents().readSafely(0);
        try {
            translateAndSaveBossBar(languagePlayer, uuid, component);
            packet.getPacket().getChatComponents().writeSafely(0, component);
        } catch (RuntimeException e) {
            // Catch 1.16 Hover 'contents' not being parsed correctly
            // Has been fixed in newer versions of Spigot 1.16
            logger().logError(e, "Could not parse a bossbar, so it was ignored. Bossbar: %1", component.getJson());
        }
    }

    private void handleBoss_1_17(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (areBossBarsDisabled()) return;

        val uuid = packet.getPacket().getUUIDs().readSafely(0);
        val operation = packet.getPacket().getSpecificModifier(OPERATION_INTERFACE).readSafely(0);
        Action action = ACTION_CONVERTER.getSpecific(GET_OPERATION_ACTION_METHOD.invoke(operation));

        if (action == Action.REMOVE) {
            languagePlayer.removeBossbar(uuid);
            return;
        }

        WrappedChatComponent component;
        if (action == Action.ADD) {
            component = COMPONENT_CONVERTER.getSpecific(ADD_COMPONENT_FIELD.get(operation));
        } else if (action == Action.UPDATE_NAME) {
            component = COMPONENT_CONVERTER.getSpecific(UPDATE_COMPONENT_FIELD.get(operation));
        } else {
            return;
        }

        translateAndSaveBossBar(languagePlayer, uuid, component);

        if (action == Action.ADD) {
            ADD_COMPONENT_FIELD.set(operation, COMPONENT_CONVERTER.getGeneric(component));
        } else {
            UPDATE_COMPONENT_FIELD.set(operation, COMPONENT_CONVERTER.getGeneric(component));
        }
    }

    private void translateBossBar(SpigotLanguagePlayer languagePlayer, WrappedChatComponent component) {
        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(component),
                        languagePlayer,
                        getConfig().getBossbarSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(ComponentUtils::serializeToJson)
                .ifPresent(component::setJson);
    }

    private void translateAndSaveBossBar(SpigotLanguagePlayer languagePlayer, UUID uuid, WrappedChatComponent component) {
        languagePlayer.setBossbar(uuid, component.getJson());
        translateBossBar(languagePlayer, component);
    }

    public void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String json) {
        if (!MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
            // bossbar only works on 1.9+
            return;
        }

        val bukkitPlayerOpt = player.toBukkit();
        if (!bukkitPlayerOpt.isPresent()) return;
        val bukkitPlayer = bukkitPlayerOpt.get();

        PacketContainer packet = createPacket(PacketType.Play.Server.BOSS);
        packet.getUUIDs().writeSafely(0, uuid);
        val msg = WrappedChatComponent.fromJson(json);
        translateBossBar(player, msg);
        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            Object operation = UPDATE_CONSTRUCTOR.invoke(COMPONENT_CONVERTER.getGeneric(msg));
            // noinspection unchecked
            packet.getSpecificModifier((Class<Object>) OPERATION_INTERFACE).writeSafely(0, operation);
        } else {
            packet.getEnumModifier(Action.class, 1).writeSafely(0, Action.UPDATE_NAME);
            packet.getChatComponents().writeSafely(0, msg);
        }
        sendPacket(bukkitPlayer, packet, false);
    }

    @Override
    public void registerPacketTypes(Map<PacketType, HandlerFunction> registry) {
        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            // Rework of packet to use subclasses instead of having all data on packet itself
            registry.put(PacketType.Play.Server.BOSS, asAsync(this::handleBoss_1_17));
        } else if (MinecraftVersion.COMBAT_UPDATE.atOrAbove()) { // 1.9+
            // Bossbars were only added on MC 1.9
            registry.put(PacketType.Play.Server.BOSS, asAsync(this::handleBoss_1_9));
        }
    }

    /**
     * BossBar packet Action wrapper
     */
    @Getter
    public enum Action implements EnumWrappers.AliasedEnum {
        ADD, REMOVE, UPDATE_PROGRESS("UPDATE_PCT"), UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES;

        private final String[] aliases;

        Action(String... aliases) {
            this.aliases = aliases;
        }
    }
}
