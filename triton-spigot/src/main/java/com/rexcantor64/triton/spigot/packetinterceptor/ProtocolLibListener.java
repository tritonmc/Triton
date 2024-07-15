package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedNumberFormat;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.utils.BaseComponentUtils;
import com.rexcantor64.triton.spigot.utils.ItemStackTranslationUtils;
import com.rexcantor64.triton.spigot.utils.NMSUtils;
import com.rexcantor64.triton.spigot.utils.WrappedComponentUtils;
import com.rexcantor64.triton.spigot.wrappers.WrappedClientConfiguration;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.wrappers.WrappedPlayerChatMessage;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asAsync;
import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asSync;

@SuppressWarnings({"deprecation"})
public class ProtocolLibListener implements PacketListener {
    private final Class<?> CONTAINER_PLAYER_CLASS;
    private final Class<BaseComponent[]> BASE_COMPONENT_ARRAY_CLASS = BaseComponent[].class;
    private final Class<Component> ADVENTURE_COMPONENT_CLASS = Component.class;
    private final FieldAccessor PLAYER_ACTIVE_CONTAINER_FIELD;
    private final FieldAccessor PLAYER_INVENTORY_CONTAINER_FIELD;
    private final String SIGN_NBT_ID;

    private final HandlerFunction ASYNC_PASSTHROUGH = asAsync((_packet, _player) -> {
    });

    private final AdvancementsPacketHandler advancementsPacketHandler = AdvancementsPacketHandler.newInstance();
    private final BossBarPacketHandler bossBarPacketHandler = new BossBarPacketHandler();
    private final EntitiesPacketHandler entitiesPacketHandler = new EntitiesPacketHandler();
    private final SignPacketHandler signPacketHandler = new SignPacketHandler();

    private final SpigotTriton main;
    private final List<HandlerFunction.HandlerType> allowedTypes;
    private final Map<PacketType, HandlerFunction> packetHandlers = new HashMap<>();
    private final AtomicBoolean firstRun = new AtomicBoolean(true);

    public ProtocolLibListener(SpigotTriton main, HandlerFunction.HandlerType... allowedTypes) {
        this.main = main;
        this.allowedTypes = Arrays.asList(allowedTypes);
        if (MinecraftVersion.EXPLORATION_UPDATE.atOrAbove()) { // 1.11+
            SIGN_NBT_ID = "minecraft:sign";
        } else {
            SIGN_NBT_ID = "Sign";
        }

        val containerClass = MinecraftReflection.getMinecraftClass("world.inventory.Container", "world.inventory.AbstractContainerMenu", "Container");
        CONTAINER_PLAYER_CLASS = MinecraftReflection.getMinecraftClass("world.inventory.ContainerPlayer", "world.inventory.InventoryMenu", "ContainerPlayer");
        if (MinecraftVersion.v1_20_5.atOrAbove()) { // 1.20.5+
            val fuzzyHuman = FuzzyReflection.fromClass(MinecraftReflection.getEntityHumanClass());
            // We have to use this field matcher because the function in accessors matches superclasses
            PLAYER_ACTIVE_CONTAINER_FIELD = Accessors.getFieldAccessor(
                    fuzzyHuman.getField(FuzzyFieldContract.newBuilder().typeExact(containerClass).build())
            );
            PLAYER_INVENTORY_CONTAINER_FIELD = Accessors.getFieldAccessor(
                    fuzzyHuman.getField(FuzzyFieldContract.newBuilder().typeExact(CONTAINER_PLAYER_CLASS).build())
            );

            // Sanity check
            assert PLAYER_ACTIVE_CONTAINER_FIELD.getField() != PLAYER_INVENTORY_CONTAINER_FIELD.getField();
        } else {
            val activeContainerField = Arrays.stream(MinecraftReflection.getEntityHumanClass().getDeclaredFields())
                    .filter(field -> field.getType() == containerClass && !field.getName().equals("defaultContainer"))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Failed to find field for player's active container"));
            PLAYER_ACTIVE_CONTAINER_FIELD = Accessors.getFieldAccessor(activeContainerField);
            PLAYER_INVENTORY_CONTAINER_FIELD = null;
        }

        setupPacketHandlers();
    }

    @Override
    public Plugin getPlugin() {
        return main.getJavaPlugin();
    }

    private AdventureParser parser() {
        return main.getMessageParser();
    }

    private void setupPacketHandlers() {
        if (MinecraftVersion.WILD_UPDATE.atOrAbove()) { // 1.19+
            // New chat packets on 1.19
            packetHandlers.put(PacketType.Play.Server.SYSTEM_CHAT, asAsync(this::handleSystemChat));
            if (!MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
                // Removed in 1.19.3
                packetHandlers.put(PacketType.Play.Server.CHAT_PREVIEW, asAsync(this::handleChatPreview));
            }
        }
        // In 1.19+, this packet is signed, but we can still edit it, since it might contain
        // formatting from chat plugins.
        packetHandlers.put(PacketType.Play.Server.CHAT, asAsync(this::handleChat));
        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            // Title packet split on 1.17
            packetHandlers.put(PacketType.Play.Server.SET_TITLE_TEXT, asAsync(this::handleTitle));
            packetHandlers.put(PacketType.Play.Server.SET_SUBTITLE_TEXT, asAsync(this::handleTitle));

            // New actionbar packet
            packetHandlers.put(PacketType.Play.Server.SET_ACTION_BAR_TEXT, asAsync(this::handleActionbar));

            // Combat packet split on 1.17
            packetHandlers.put(PacketType.Play.Server.PLAYER_COMBAT_KILL, asAsync(this::handleDeathScreen));
        } else {
            packetHandlers.put(PacketType.Play.Server.TITLE, asAsync(this::handleTitle));
            packetHandlers.put(PacketType.Play.Server.COMBAT_EVENT, asAsync(this::handleDeathScreen));
        }

        packetHandlers.put(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, asAsync(this::handlePlayerListHeaderFooter));
        packetHandlers.put(PacketType.Play.Server.OPEN_WINDOW, asAsync(this::handleOpenWindow));
        packetHandlers.put(PacketType.Play.Server.KICK_DISCONNECT, asSync(this::handleKickDisconnect));
        if (MinecraftVersion.AQUATIC_UPDATE.atOrAbove()) { // 1.13+
            // Scoreboard rewrite on 1.13
            // It allows unlimited length team prefixes and suffixes
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_TEAM, asAsync(this::handleScoreboardTeam));
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, asAsync(this::handleScoreboardObjective));
            // Register the packets below so their order is kept between all scoreboard packets
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, ASYNC_PASSTHROUGH);
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_SCORE, ASYNC_PASSTHROUGH);
            if (MinecraftVersion.v1_20_4.atOrAbove()) {
                packetHandlers.put(PacketType.Play.Server.RESET_SCORE, ASYNC_PASSTHROUGH);
            }
        }
        packetHandlers.put(PacketType.Play.Server.WINDOW_ITEMS, asAsync(this::handleWindowItems));
        packetHandlers.put(PacketType.Play.Server.SET_SLOT, asAsync(this::handleSetSlot));
        if (MinecraftVersion.CAVES_CLIFFS_2.atOrAbove()) { // 1.18+
            // While the villager merchant interface redesign was on 1.14, the Bukkit API only has all fields on 1.18
            packetHandlers.put(PacketType.Play.Server.OPEN_WINDOW_MERCHANT, asAsync(this::handleMerchantItems));
        }

        // External Packet Handlers
        if (advancementsPacketHandler != null) {
            advancementsPacketHandler.registerPacketTypes(packetHandlers);
        }
        bossBarPacketHandler.registerPacketTypes(packetHandlers);
        entitiesPacketHandler.registerPacketTypes(packetHandlers);
        signPacketHandler.registerPacketTypes(packetHandlers);
    }

    /* PACKET HANDLERS */

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean isSigned = MinecraftVersion.WILD_UPDATE.atOrAbove(); // MC 1.19+
        if (isSigned && !main.getConfig().isSignedChat()) return;
        // action bars are not sent here on 1.19+ anymore
        boolean ab = !isSigned && isActionbar(packet.getPacket());

        // Don't bother parsing anything else if it's disabled on config
        if ((ab && !main.getConfig().isActionbars()) || (!ab && !main.getConfig().isChat())) return;

        val chatModifier = packet.getPacket().getChatComponents();
        val baseComponentModifier = packet.getPacket().getSpecificModifier(BASE_COMPONENT_ARRAY_CLASS);
        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);
        boolean hasPlayerChatMessageRecord = isSigned && !MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove(); // MC 1.19-1.19.2
        WrappedPlayerChatMessage wrappedPlayerChatMessage = null;

        Component message = null;

        if (hasPlayerChatMessageRecord) {
            // The message is wrapped in a PlayerChatMessage record
            val playerChatModifier = packet.getPacket().getModifier().withType(WrappedPlayerChatMessage.getWrappedClass(), WrappedPlayerChatMessage.CONVERTER);
            wrappedPlayerChatMessage = playerChatModifier.readSafely(0);
            if (wrappedPlayerChatMessage != null) {
                Optional<WrappedChatComponent> msg = wrappedPlayerChatMessage.getMessage();
                if (msg.isPresent()) {
                    message = WrappedComponentUtils.deserialize(msg.get());
                }
            }
        } else if (adventureModifier.readSafely(0) != null) {
            message = adventureModifier.readSafely(0);
        } else if (baseComponentModifier.readSafely(0) != null) {
            message = BaseComponentUtils.deserialize(baseComponentModifier.readSafely(0));
        } else {
            val msg = chatModifier.readSafely(0);
            if (msg != null) {
                message = WrappedComponentUtils.deserialize(msg);
            }
        }

        // Something went wrong while getting data from the packet, or the packet is empty...?
        if (message == null) {
            return;
        }

        // Translate the message
        val wrappedPlayerChatMessageFinal = wrappedPlayerChatMessage;
        parser()
                .translateComponent(
                        message,
                        languagePlayer,
                        ab ? main.getConfig().getActionbarSyntax() : main.getConfig().getChatSyntax()
                )
                .ifChanged(result -> {
                    if (adventureModifier.size() > 0) {
                        // On a Paper or fork, so we can directly set the Adventure Component
                        adventureModifier.writeSafely(0, result);
                    } else if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) { // MC 1.19.3+
                        // While chat is signed, we can still mess around with formatting and prefixes
                        chatModifier.writeSafely(0, WrappedComponentUtils.serialize(result));
                    } else if (hasPlayerChatMessageRecord) { // MC 1.19-1.19.2
                        // While chat is signed, we can still mess around with formatting and prefixes
                        wrappedPlayerChatMessageFinal.setMessage(Optional.of(WrappedComponentUtils.serialize(result)));
                    } else {
                        BaseComponent[] resultComponent;
                        if (ab && !MinecraftVersion.EXPLORATION_UPDATE.atOrAbove()) {
                            // The Notchian client does not support true JSON messages on actionbars
                            // on 1.10 and below. Therefore, we must convert to a legacy string inside
                            // a TextComponent.
                            resultComponent = new BaseComponent[]{new TextComponent(LegacyComponentSerializer.legacySection().serialize(result))};
                        } else {
                            resultComponent = BaseComponentUtils.serialize(result);
                        }
                        baseComponentModifier.writeSafely(0, resultComponent);
                    }
                })
                .ifToRemove(() -> packet.setCancelled(true));
    }

    /**
     * Handle a system chat outbound packet, added in Minecraft 1.19.
     * Apparently most chat messages and actionbars are sent through here in Minecraft 1.19+.
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @since 3.8.0 (Minecraft 1.19)
     */
    private void handleSystemChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());

        // Don't bother parsing anything else if it's disabled on config
        if ((ab && !main.getConfig().isActionbars()) || (!ab && !main.getConfig().isChat())) return;

        val stringModifier = packet.getPacket().getStrings();
        val chatModifier = packet.getPacket().getChatComponents();

        Component message = null;

        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);

        if (adventureModifier.readSafely(0) != null) {
            message = adventureModifier.readSafely(0);
        } else if (chatModifier.readSafely(0) != null) {
            message = WrappedComponentUtils.deserialize(chatModifier.readSafely(0));
        } else {
            val msgJson = stringModifier.readSafely(0);
            if (msgJson != null) {
                message = ComponentUtils.deserializeFromJson(msgJson);
            }
        }

        // Packet is empty
        if (message == null) {
            return;
        }

        // Translate the message
        parser()
                .translateComponent(
                        message,
                        languagePlayer,
                        ab ? main.getConfig().getActionbarSyntax() : main.getConfig().getChatSyntax()
                )
                .ifChanged(result -> {
                    if (adventureModifier.size() > 0) {
                        // On a Paper or fork, so we can directly set the Adventure Component
                        adventureModifier.writeSafely(0, result);
                    } else if (chatModifier.size() > 0) {
                        // Starting on MC 1.20.3 this packet takes a chat component instead of a json string
                        chatModifier.writeSafely(0, WrappedComponentUtils.serialize(result));
                    } else {
                        stringModifier.writeSafely(0, ComponentUtils.serializeToJson(result));
                    }
                })
                .ifToRemove(() -> packet.setCancelled(true));
    }

    /**
     * Handle a chat preview outbound packet, added in Minecraft 1.19.
     * This changes the preview of the message to translate placeholders there
     *
     * @param packet         ProtocolLib's packet event
     * @param languagePlayer The language player this packet is being sent to
     * @since 3.8.2 (Minecraft 1.19)
     */
    private void handleChatPreview(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isChat()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();

        Component message = null;

        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);

        if (adventureModifier.readSafely(0) != null) {
            message = adventureModifier.readSafely(0);
        } else {
            val msg = chatComponentsModifier.readSafely(0);
            if (msg != null) {
                message = WrappedComponentUtils.deserialize(msg);
            }
        }

        // Packet is empty
        if (message == null) {
            return;
        }

        // Translate the message
        parser()
                .translateComponent(
                        message,
                        languagePlayer,
                        main.getConfig().getChatSyntax()
                )
                .ifChanged(result -> {
                    if (adventureModifier.size() > 0) {
                        // On a Paper or fork, so we can directly set the Adventure Component
                        adventureModifier.write(0, result);
                    } else {
                        chatComponentsModifier.writeSafely(0, WrappedComponentUtils.serialize(result));
                    }
                })
                .ifToRemove(() -> {
                    adventureModifier.writeSafely(0, null);
                    chatComponentsModifier.writeSafely(0, null);

                });
    }

    private void handleActionbar(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isActionbars()) return;

        val baseComponentModifier = packet.getPacket().getSpecificModifier(BASE_COMPONENT_ARRAY_CLASS);
        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);

        Component message = null;

        if (adventureModifier.readSafely(0) != null) {
            message = adventureModifier.readSafely(0);
        } else if (baseComponentModifier.readSafely(0) != null) {
            message = BaseComponentUtils.deserialize(baseComponentModifier.readSafely(0));
        } else {
            val msg = packet.getPacket().getChatComponents().readSafely(0);
            if (msg != null) {
                message = WrappedComponentUtils.deserialize(msg);
            }
        }

        // Something went wrong while getting data from the packet, or the packet is empty...?
        if (message == null) {
            return;
        }

        // Translate the message
        parser()
                .translateComponent(
                        message,
                        languagePlayer,
                        main.getConfig().getActionbarSyntax()
                )
                .ifChanged(result -> {
                    if (adventureModifier.size() > 0) {
                        // We're on a Paper or fork, so we can directly set the Adventure Component
                        adventureModifier.writeSafely(0, result);
                    } else {
                        baseComponentModifier.writeSafely(0, BaseComponentUtils.serialize(result));
                    }
                })
                .ifToRemove(() -> packet.setCancelled(true));
    }

    private void handleTitle(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isTitles()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();
        WrappedChatComponent msg = chatComponentsModifier.readSafely(0);
        if (msg == null) {
            return;
        }

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(msg),
                        languagePlayer,
                        main.getConfig().getTitleSyntax()
                )
                .map(WrappedComponentUtils::serialize)
                .ifChanged(newTitle -> chatComponentsModifier.writeSafely(0, newTitle))
                .ifToRemove(() -> packet.setCancelled(true));
    }

    private void handlePlayerListHeaderFooter(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isTab()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();
        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);

        Component header = adventureModifier.optionRead(0)
                .orElseGet(() ->
                        chatComponentsModifier.optionRead(0)
                                .map(WrappedComponentUtils::deserialize)
                                .orElse(null)
                );
        Component footer = adventureModifier.optionRead(1)
                .orElseGet(() ->
                        chatComponentsModifier.optionRead(1)
                                .map(WrappedComponentUtils::deserialize)
                                .orElse(null)
                );

        if (header == null || footer == null) {
            Triton.get().getLogger().logWarning("Could not translate player list header footer because content is null.");
            return;
        }

        parser()
                .translateComponent(header, languagePlayer, main.getConfig().getTabSyntax())
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    /* FIXME
                    if (resultHeader.length == 1 && resultHeader[0] instanceof TextComponent) {
                        // This is needed because the Notchian client does not render the header/footer
                        // if the content of the header top level component is an empty string.
                        val textComp = (TextComponent) resultHeader[0];
                        if (textComp.getText().length() == 0 && !headerJson.equals("{\"text\":\"\"}"))
                            textComp.setText("§0§1§2§r");
                    }
                    */
                    if (adventureModifier.size() > 0) {
                        // We're on Paper or a fork, so use the Adventure field
                        adventureModifier.writeSafely(0, result);
                    } else {
                        chatComponentsModifier.writeSafely(0, WrappedComponentUtils.serialize(result));
                    }
                });
        parser()
                .translateComponent(footer, languagePlayer, main.getConfig().getTabSyntax())
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    if (adventureModifier.size() > 1) {
                        // We're on Paper or a fork, so use the Adventure field
                        adventureModifier.writeSafely(1, result);
                    } else {
                        chatComponentsModifier.writeSafely(1, WrappedComponentUtils.serialize(result));
                    }
                });

        languagePlayer.setLastTabHeader(header);
        languagePlayer.setLastTabFooter(footer);
    }

    private void handleOpenWindow(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isGuis()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();

        val chatComponent = chatComponentsModifier.readSafely(0);
        if (chatComponent == null) {
            return;
        }

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(chatComponent),
                        languagePlayer,
                        main.getConfig().getGuiSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(WrappedComponentUtils::serialize)
                .ifPresent(result -> chatComponentsModifier.writeSafely(0, result));
    }

    private void handleKickDisconnect(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isKick()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();

        val chatComponent = chatComponentsModifier.readSafely(0);
        if (chatComponent == null) {
            return;
        }

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(chatComponent),
                        languagePlayer,
                        main.getConfig().getKickSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(WrappedComponentUtils::serialize)
                .ifPresent(result -> chatComponentsModifier.writeSafely(0, result));
    }

    private void handleWindowItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isItems()) return;

        if (!main.getConfig().isInventoryItems() && isPlayerInventoryOpen(packet.getPlayer()))
            return;

        if (MinecraftVersion.EXPLORATION_UPDATE.atOrAbove()) { // 1.11+
            List<ItemStack> items = packet.getPacket().getItemListModifier().readSafely(0);
            for (ItemStack item : items) {
                ItemStackTranslationUtils.translateItemStack(item, languagePlayer, true);
            }
            packet.getPacket().getItemListModifier().writeSafely(0, items);

            if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
                ItemStack carriedItem = packet.getPacket().getItemModifier().readSafely(0);
                carriedItem = ItemStackTranslationUtils.translateItemStack(carriedItem, languagePlayer, false);
                packet.getPacket().getItemModifier().writeSafely(0, carriedItem);
            }
        } else {
            ItemStack[] items = packet.getPacket().getItemArrayModifier().readSafely(0);
            for (ItemStack item : items) {
                ItemStackTranslationUtils.translateItemStack(item, languagePlayer, true);
            }
            packet.getPacket().getItemArrayModifier().writeSafely(0, items);
        }
    }

    private void handleSetSlot(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isItems()) return;

        if (!main.getConfig().isInventoryItems() && isPlayerInventoryOpen(packet.getPlayer()))
            return;

        ItemStack item = packet.getPacket().getItemModifier().readSafely(0);
        ItemStackTranslationUtils.translateItemStack(item, languagePlayer, true);
        packet.getPacket().getItemModifier().writeSafely(0, item);
    }

    private void handleMerchantItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isItems()) return;

        val recipes = packet.getPacket().getMerchantRecipeLists().readSafely(0);
        val newRecipes = new ArrayList<MerchantRecipe>();

        for (val recipe : recipes) {
            // Unfortunately this constructor does not exist in older Bukkit versions
            // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/5dca4a4b8455ba1ee8d3e4e36894f6dcc4b04555
            val newRecipe = new MerchantRecipe(
                    ItemStackTranslationUtils.translateItemStack(recipe.getResult().clone(), languagePlayer, false),
                    recipe.getUses(),
                    recipe.getMaxUses(),
                    recipe.hasExperienceReward(),
                    recipe.getVillagerExperience(),
                    recipe.getPriceMultiplier(),
                    recipe.getDemand(),
                    recipe.getSpecialPrice()
            );

            for (val ingredient : recipe.getIngredients()) {
                newRecipe.addIngredient(ItemStackTranslationUtils.translateItemStack(ingredient.clone(), languagePlayer, false));
            }

            newRecipes.add(newRecipe);
        }

        packet.getPacket().getMerchantRecipeLists().writeSafely(0, newRecipes);
    }

    private void handleScoreboardTeam(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isScoreboards()) return;

        val teamName = packet.getPacket().getStrings().readSafely(0);
        val mode = packet.getPacket().getIntegers().readSafely(0);

        if (mode == 1) {
            languagePlayer.removeScoreboardTeam(teamName);
            return;
        }

        if (mode != 0 && mode != 2) return; // Other modes don't change text

        WrappedChatComponent displayName, prefix, suffix;
        SpigotLanguagePlayer.ScoreboardTeam team;

        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            Optional<WrappedTeamParameters> paramsOpt = packet.getPacket().getOptionalTeamParameters().readSafely(0);
            if (!paramsOpt.isPresent()) return;

            val parameters = paramsOpt.get();

            displayName = parameters.getDisplayName();
            prefix = parameters.getPrefix();
            suffix = parameters.getSuffix();

            team = new SpigotLanguagePlayer.ScoreboardTeam(
                    displayName.getJson(),
                    prefix.getJson(),
                    suffix.getJson(),
                    parameters.getNametagVisibility(),
                    parameters.getCollisionRule(),
                    parameters.getColor(),
                    parameters.getOptions()
            );
        } else {
            val chatComponents = packet.getPacket().getChatComponents();
            displayName = chatComponents.readSafely(0);
            prefix = chatComponents.readSafely(1);
            suffix = chatComponents.readSafely(2);

            team = new SpigotLanguagePlayer.ScoreboardTeam(
                    displayName.getJson(),
                    prefix.getJson(),
                    suffix.getJson(),
                    packet.getPacket().getStrings().readSafely(1),
                    packet.getPacket().getStrings().readSafely(2),
                    packet.getPacket().getChatFormattings().readSafely(0),
                    packet.getPacket().getIntegers().readSafely(1)
            );
        }

        languagePlayer.setScoreboardTeam(teamName, team);

        for (WrappedChatComponent component : Arrays.asList(displayName, prefix, suffix)) {
            parser()
                    .translateComponent(
                            WrappedComponentUtils.deserialize(component),
                            languagePlayer,
                            main.getConfig().getScoreboardSyntax()
                    )
                    .getResultOrToRemove(Component::empty)
                    .map(ComponentUtils::serializeToJson)
                    .ifPresent(component::setJson);
        }

        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
            val parameters = WrappedTeamParameters.newBuilder()
                    .displayName(displayName)
                    .prefix(prefix)
                    .suffix(suffix)
                    .nametagVisibility(team.getNameTagVisibility())
                    .collisionRule(team.getCollisionRule())
                    .color(team.getColor())
                    .options(team.getOptions())
                    .build();

            packet.getPacket().getOptionalTeamParameters().writeSafely(0, Optional.of(parameters));
        } else {
            val chatComponents = packet.getPacket().getChatComponents();
            chatComponents.writeSafely(0, displayName);
            chatComponents.writeSafely(1, prefix);
            chatComponents.writeSafely(2, suffix);
        }
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isScoreboards()) return;

        val objectiveName = packet.getPacket().getStrings().readSafely(0);
        val mode = packet.getPacket().getIntegers().readSafely(0);

        if (mode == 1) { // Mode 1 is REMOVE
            languagePlayer.removeScoreboardObjective(objectiveName);
            return;
        }
        // There are only 3 modes, so no need to check for more modes

        val chatComponentsModifier = packet.getPacket().getChatComponents();

        val displayName = chatComponentsModifier.readSafely(0);
        val renderType = packet.getPacket().getRenderTypes().readSafely(0);
        WrappedNumberFormat numberFormat = null;
        if (WrappedNumberFormat.isSupported()) {
            if (MinecraftVersion.v1_20_5.atOrAbove()) {
                // on MC 1.20.5+ this field became an Optional
                numberFormat = packet.getPacket()
                        .getOptionals(BukkitConverters.getWrappedNumberFormatConverter())
                        .readSafely(0)
                        .orElse(null);
            } else {
                numberFormat = packet.getPacket().getNumberFormats().readSafely(0);
            }
        }

        languagePlayer.setScoreboardObjective(objectiveName, displayName.getJson(), renderType, numberFormat);

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(displayName),
                        languagePlayer,
                        main.getConfig().getScoreboardSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(WrappedComponentUtils::serialize)
                .ifPresent(result -> chatComponentsModifier.writeSafely(0, result));
    }

    private void handleDeathScreen(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isDeathScreen()) return;

        val chatComponentsModifier = packet.getPacket().getChatComponents();
        val component = chatComponentsModifier.readSafely(0);
        if (component == null) {
            // Likely it's MC 1.16 or below and type of packet is not ENTITY_DIED.
            // Alternatively, this will always be null on 1.8.8 since it uses a String, but there's nothing interesting to translate anyway.
            return;
        }

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(component),
                        languagePlayer,
                        main.getConfig().getDeathScreenSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(WrappedComponentUtils::serialize)
                .ifPresent(result -> chatComponentsModifier.writeSafely(0, result));
    }

    /* PROTOCOL LIB */

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) {
            return;
        }

        if (firstRun.compareAndSet(true, false) && !Bukkit.getServer().isPrimaryThread()) {
            Thread.currentThread().setName("Triton Async Packet Handler");
        }

        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer = main.getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logWarning("Failed to translate packet because UUID of the player is unknown (possibly " +
                            "because the player hasn't joined yet).");
            if (Triton.get().getConfig().getLogLevel() >= 1) {
                e.printStackTrace();
            }
            return;
        }

        val handler = packetHandlers.get(packet.getPacketType());
        if (handler != null) {
            handler.getHandlerFunction().accept(packet, languagePlayer);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packet) {
        if (packet.isServerPacket()) return;
        SpigotLanguagePlayer languagePlayer;
        try {
            languagePlayer = main.getPlayerManager().get(packet.getPlayer().getUniqueId());
        } catch (Exception ignore) {
            Triton.get().getLogger()
                    .logTrace("Failed to get SpigotLanguagePlayer because UUID of the player is unknown " +
                            "(possibly because the player hasn't joined yet).");
            return;
        }
        if (!languagePlayer.isWaitingForClientLocale()) {
            return;
        }
        if (packet.getPacketType() == PacketType.Play.Client.SETTINGS) {
            Bukkit.getScheduler().runTask(
                    main.getJavaPlugin(),
                    () -> languagePlayer.setLang(
                            main.getLanguageManager()
                                    .getLanguageByLocaleOrDefault(packet.getPacket().getStrings().readSafely(0))
                    )
            );
        } else if (packet.getPacketType().getProtocol() == PacketType.Protocol.CONFIGURATION) {
            val clientConfigurations = packet.getPacket().getStructures().withType(WrappedClientConfiguration.getWrappedClass(), WrappedClientConfiguration.CONVERTER);
            val locale = clientConfigurations.readSafely(0).getLocale();
            val language = main.getLanguageManager().getLanguageByLocaleOrDefault(locale);
            Bukkit.getScheduler().runTaskLater(main.getJavaPlugin(), () -> languagePlayer.setLang(language), 2L);
        }
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        val types = packetHandlers.entrySet().stream()
                .filter(entry -> this.allowedTypes.contains(entry.getValue().getHandlerType()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return ListeningWhitelist.newBuilder()
                .gamePhase(GamePhase.PLAYING)
                .types(types)
                .mergeOptions(ListenerOptions.ASYNC)
                .highest()
                .build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        val types = new ArrayList<PacketType>();
        if (this.allowedTypes.contains(HandlerFunction.HandlerType.SYNC)) {
            // only listen for these packets in the sync handler
            types.add(PacketType.Play.Client.SETTINGS);
            if (MinecraftVersion.CONFIG_PHASE_PROTOCOL_UPDATE.atOrAbove()) { // MC 1.20.2
                types.add(PacketType.Configuration.Client.CLIENT_INFORMATION);
            }
        }

        return ListeningWhitelist.newBuilder()
                .gamePhase(GamePhase.PLAYING)
                .types(types)
                .mergeOptions(ListenerOptions.ASYNC)
                .highest()
                .build();
    }

    /* REFRESH */

    public void refreshSigns(SpigotLanguagePlayer player) {
        signPacketHandler.refreshSignsForPlayer(player);
    }

    public void refreshEntities(SpigotLanguagePlayer player) {
        entitiesPacketHandler.refreshEntities(player);
    }

    public void refreshTabHeaderFooter(SpigotLanguagePlayer player, Component header, Component footer) {
        player.toBukkit().ifPresent(bukkitPlayer -> {
            PacketContainer packet =
                    ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);

            val adventureModifier = packet.getSpecificModifier(ADVENTURE_COMPONENT_CLASS);
            if (adventureModifier.size() > 0) {
                adventureModifier.writeSafely(0, header);
                adventureModifier.writeSafely(1, footer);
            } else {
                val chatComponentModifier = packet.getChatComponents();
                chatComponentModifier.writeSafely(0, WrappedComponentUtils.serialize(header));
                chatComponentModifier.writeSafely(1, WrappedComponentUtils.serialize(footer));
            }

            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, true);
        });
    }

    public void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String json) {
        bossBarPacketHandler.refreshBossbar(player, uuid, json);
    }

    public void refreshScoreboard(SpigotLanguagePlayer player) {
        val bukkitPlayerOpt = player.toBukkit();
        if (!bukkitPlayerOpt.isPresent()) return;
        val bukkitPlayer = bukkitPlayerOpt.get();

        player.getObjectivesMap().forEach((key, value) -> {
            val packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
            packet.getIntegers().writeSafely(0, 2); // Update display name mode
            packet.getStrings().writeSafely(0, key);
            packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(value.getChatJson()));
            packet.getRenderTypes().writeSafely(0, value.getType());
            if (WrappedNumberFormat.isSupported()) {
                if (MinecraftVersion.v1_20_5.atOrAbove()) {
                    // on MC 1.20.5+ this field became an Optional
                    packet.getOptionals(BukkitConverters.getWrappedNumberFormatConverter())
                            .writeSafely(0, Optional.ofNullable(value.getNumberFormat()));
                } else {
                    packet.getNumberFormats().writeSafely(0, value.getNumberFormat());
                }
            }
            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, true);
        });

        player.getTeamsMap().forEach((key, value) -> {
            val packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
            packet.getIntegers().writeSafely(0, 2); // Update team info mode
            packet.getStrings().writeSafely(0, key);
            if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) { // 1.17+
                val parameters = WrappedTeamParameters.newBuilder()
                        .displayName(WrappedChatComponent.fromJson(value.getDisplayJson()))
                        .prefix(WrappedChatComponent.fromJson(value.getPrefixJson()))
                        .suffix(WrappedChatComponent.fromJson(value.getSuffixJson()))
                        .nametagVisibility(value.getNameTagVisibility())
                        .collisionRule(value.getCollisionRule())
                        .color(value.getColor())
                        .options(value.getOptions())
                        .build();

                packet.getOptionalTeamParameters().writeSafely(0, Optional.of(parameters));
            } else {
                packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(value.getDisplayJson()));
                packet.getChatComponents().writeSafely(1, WrappedChatComponent.fromJson(value.getPrefixJson()));
                packet.getChatComponents().writeSafely(2, WrappedChatComponent.fromJson(value.getSuffixJson()));

                packet.getStrings().writeSafely(1, value.getNameTagVisibility());
                packet.getStrings().writeSafely(2, value.getCollisionRule());
                packet.getChatFormattings().writeSafely(0, value.getColor());
                packet.getIntegers().writeSafely(1, value.getOptions());
            }

            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, true);
        });
    }

    public void refreshAdvancements(SpigotLanguagePlayer languagePlayer) {
        if (this.advancementsPacketHandler == null) return;

        this.advancementsPacketHandler.refreshAdvancements(languagePlayer);
    }

    public void resetSign(Player p, SignLocation location) {
        World world = Bukkit.getWorld(location.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
        BlockState state = block.getState();
        if (!(state instanceof Sign))
            return;
        String[] lines = ((Sign) state).getLines();
        if (MinecraftReflection.signUpdateExists()) {
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
                main.getLogger().logError(e, "Failed refresh sign.");
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
            nbt.put("name", "null")
                    .put("x", block.getX())
                    .put("y", block.getY())
                    .put("z", block.getZ())
                    .put("id", SIGN_NBT_ID);
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.getLogger().logError("Failed refresh sign.");
            }
        }
    }

    /* UTILITIES */

    private boolean isActionbar(PacketContainer container) {
        if (MinecraftVersion.WILD_UPDATE.atOrAbove()) { // 1.19+
            val booleans = container.getBooleans();
            if (booleans.size() > 0) {
                return booleans.readSafely(0);
            }
            return container.getIntegers().readSafely(0) == 2;
        } else if (MinecraftVersion.COLOR_UPDATE.atOrAbove()) { // 1.12+
            return container.getChatTypes().readSafely(0) == EnumWrappers.ChatType.GAME_INFO;
        } else {
            return container.getBytes().readSafely(0) == 2;
        }
    }

    private boolean isPlayerInventoryOpen(Player player) {
        val nmsHandle = NMSUtils.getHandle(player);

        if (MinecraftVersion.v1_20_5.atOrAbove()) { // 1.20.5+
            return PLAYER_ACTIVE_CONTAINER_FIELD.get(nmsHandle) == PLAYER_INVENTORY_CONTAINER_FIELD.get(nmsHandle);
        } else {
            return PLAYER_ACTIVE_CONTAINER_FIELD.get(nmsHandle).getClass() == CONTAINER_PLAYER_CLASS;
        }
    }

}
