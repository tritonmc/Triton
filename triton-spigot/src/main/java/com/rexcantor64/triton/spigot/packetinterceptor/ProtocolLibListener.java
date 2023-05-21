package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.spigot.utils.BaseComponentUtils;
import com.rexcantor64.triton.spigot.utils.ItemStackTranslationUtils;
import com.rexcantor64.triton.spigot.utils.NMSUtils;
import com.rexcantor64.triton.spigot.utils.WrappedComponentUtils;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.utils.ReflectionUtils;
import lombok.SneakyThrows;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asAsync;
import static com.rexcantor64.triton.spigot.packetinterceptor.HandlerFunction.asSync;

@SuppressWarnings({"deprecation"})
public class ProtocolLibListener implements PacketListener {
    private final Class<?> CONTAINER_PLAYER_CLASS;
    private final Class<?> MERCHANT_RECIPE_LIST_CLASS;
    private final MethodAccessor CRAFT_MERCHANT_RECIPE_FROM_BUKKIT_METHOD;
    private final MethodAccessor CRAFT_MERCHANT_RECIPE_TO_MINECRAFT_METHOD;
    private final Class<?> BOSSBAR_UPDATE_TITLE_ACTION_CLASS;
    private final Class<BaseComponent[]> BASE_COMPONENT_ARRAY_CLASS = BaseComponent[].class;
    private StructureModifier<Object> SCOREBOARD_TEAM_METADATA_MODIFIER = null;
    private final Class<Component> ADVENTURE_COMPONENT_CLASS = Component.class;
    private final Field PLAYER_ACTIVE_CONTAINER_FIELD;
    private final String MERCHANT_RECIPE_SPECIAL_PRICE_FIELD;
    private final String MERCHANT_RECIPE_DEMAND_FIELD;

    private final SignPacketHandler signPacketHandler = new SignPacketHandler();
    private final AdvancementsPacketHandler advancementsPacketHandler;
    private final EntitiesPacketHandler entitiesPacketHandler = new EntitiesPacketHandler();

    private final SpigotTriton main;
    private final List<HandlerFunction.HandlerType> allowedTypes;
    private final Map<PacketType, HandlerFunction> packetHandlers = new HashMap<>();
    private final AtomicBoolean firstRun = new AtomicBoolean(true);

    public ProtocolLibListener(SpigotTriton main, HandlerFunction.HandlerType... allowedTypes) {
        this.main = main;
        this.allowedTypes = Arrays.asList(allowedTypes);
        if (main.getMcVersion() >= 17) {
            MERCHANT_RECIPE_LIST_CLASS = ReflectionUtils.getClass("net.minecraft.world.item.trading.MerchantRecipeList");
        } else if (main.getMcVersion() >= 14) {
            MERCHANT_RECIPE_LIST_CLASS = NMSUtils.getNMSClass("MerchantRecipeList");
        } else {
            MERCHANT_RECIPE_LIST_CLASS = null;
        }
        if (main.getMcVersion() >= 14) {
            val craftMerchantRecipeClass = NMSUtils.getCraftbukkitClass("inventory.CraftMerchantRecipe");
            CRAFT_MERCHANT_RECIPE_FROM_BUKKIT_METHOD = Accessors.getMethodAccessor(craftMerchantRecipeClass, "fromBukkit", MerchantRecipe.class);
            CRAFT_MERCHANT_RECIPE_TO_MINECRAFT_METHOD = Accessors.getMethodAccessor(craftMerchantRecipeClass, "toMinecraft");
        } else {
            CRAFT_MERCHANT_RECIPE_FROM_BUKKIT_METHOD = null;
            CRAFT_MERCHANT_RECIPE_TO_MINECRAFT_METHOD = null;
        }
        CONTAINER_PLAYER_CLASS = main.getMcVersion() >= 17 ?
                ReflectionUtils.getClass("net.minecraft.world.inventory.ContainerPlayer") :
                NMSUtils.getNMSClass("ContainerPlayer");
        BOSSBAR_UPDATE_TITLE_ACTION_CLASS = main.getMcVersion() >= 17 ? ReflectionUtils.getClass("net.minecraft.network.protocol.game.PacketPlayOutBoss$e") : null;

        MERCHANT_RECIPE_SPECIAL_PRICE_FIELD = getMCVersion() >= 17 ? "g" : "specialPrice";
        MERCHANT_RECIPE_DEMAND_FIELD = getMCVersion() >= 17 ? "h" : "demand";

        val containerClass = MinecraftReflection.getMinecraftClass("world.inventory.Container", "Container");
        PLAYER_ACTIVE_CONTAINER_FIELD = Arrays.stream(MinecraftReflection.getEntityHumanClass().getDeclaredFields())
                .filter(field -> field.getType() == containerClass && !field.getName().equals("defaultContainer")).findAny().orElse(null);

        this.advancementsPacketHandler = getMCVersion() >= 12 ? new AdvancementsPacketHandler() : null;

        setupPacketHandlers();
    }

    @Override
    public Plugin getPlugin() {
        return main.getLoader();
    }

    private MessageParser parser() {
        return main.getMessageParser();
    }

    private void setupPacketHandlers() {
        if (main.getMcVersion() >= 19) {
            // New chat packets on 1.19
            packetHandlers.put(PacketType.Play.Server.SYSTEM_CHAT, asAsync(this::handleSystemChat));
            packetHandlers.put(PacketType.Play.Server.CHAT_PREVIEW, asAsync(this::handleChatPreview));
        } else {
            // In 1.19+, this packet is signed, so we cannot edit it.
            // It's sent by the player anyway, so there's nothing to translate.
            packetHandlers.put(PacketType.Play.Server.CHAT, asAsync(this::handleChat));
        }
        if (main.getMcVersion() >= 17) {
            // Title packet split on 1.17
            packetHandlers.put(PacketType.Play.Server.SET_TITLE_TEXT, asAsync(this::handleTitle));
            packetHandlers.put(PacketType.Play.Server.SET_SUBTITLE_TEXT, asAsync(this::handleTitle));

            // New actionbar packet
            packetHandlers.put(PacketType.Play.Server.SET_ACTION_BAR_TEXT, asAsync(this::handleActionbar));
        } else {
            packetHandlers.put(PacketType.Play.Server.TITLE, asAsync(this::handleTitle));
        }

        packetHandlers.put(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, asAsync(this::handlePlayerListHeaderFooter));
        packetHandlers.put(PacketType.Play.Server.OPEN_WINDOW, asAsync(this::handleOpenWindow));
        packetHandlers.put(PacketType.Play.Server.KICK_DISCONNECT, asSync(this::handleKickDisconnect));
        if (main.getMcVersion() >= 13) {
            // Scoreboard rewrite on 1.13
            // It allows unlimited length team prefixes and suffixes
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_TEAM, asAsync(this::handleScoreboardTeam));
            packetHandlers.put(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, asAsync(this::handleScoreboardObjective));
        }
        packetHandlers.put(PacketType.Play.Server.WINDOW_ITEMS, asAsync(this::handleWindowItems));
        packetHandlers.put(PacketType.Play.Server.SET_SLOT, asAsync(this::handleSetSlot));
        if (getMCVersion() >= 9) {
            // Bossbars were only added on MC 1.9
            packetHandlers.put(PacketType.Play.Server.BOSS, asAsync(this::handleBoss));
        }
        if (getMCVersion() >= 14) {
            // Villager merchant interface redesign on 1.14
            packetHandlers.put(PacketType.Play.Server.OPEN_WINDOW_MERCHANT, asAsync(this::handleMerchantItems));
        }


        // External Packet Handlers
        signPacketHandler.registerPacketTypes(packetHandlers);
        if (advancementsPacketHandler != null) {
            advancementsPacketHandler.registerPacketTypes(packetHandlers);
        }
        entitiesPacketHandler.registerPacketTypes(packetHandlers);
    }

    /* PACKET HANDLERS */

    private void handleChat(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        boolean ab = isActionbar(packet.getPacket());

        // Don't bother parsing anything else if it's disabled on config
        if ((ab && !main.getConfig().isActionbars()) || (!ab && !main.getConfig().isChat())) return;

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
                        ab ? main.getConfig().getActionbarSyntax() : main.getConfig().getChatSyntax()
                )
                .ifChanged(result -> {
                    if (adventureModifier.size() > 0) {
                        // On a Paper or fork, so we can directly set the Adventure Component
                        adventureModifier.writeSafely(0, result);
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

        Component message = null;

        val adventureModifier = packet.getPacket().getSpecificModifier(ADVENTURE_COMPONENT_CLASS);

        if (adventureModifier.readSafely(0) != null) {
            message = adventureModifier.readSafely(0);
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

        List<ItemStack> items = getMCVersion() <= 10 ?
                Arrays.asList(packet.getPacket().getItemArrayModifier().readSafely(0)) :
                packet.getPacket().getItemListModifier().readSafely(0);
        for (ItemStack item : items) {
            ItemStackTranslationUtils.translateItemStack(item, languagePlayer, true);
        }
        if (getMCVersion() <= 10) {
            packet.getPacket().getItemArrayModifier().writeSafely(0, items.toArray(new ItemStack[0]));
        } else {
            packet.getPacket().getItemListModifier().writeSafely(0, items);
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

    @SneakyThrows
    private void handleBoss(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isBossbars()) return;

        val uuid = packet.getPacket().getUUIDs().readSafely(0);
        WrappedChatComponent bossbar;
        Object actionObj = null;

        if (getMCVersion() >= 17) {
            actionObj = packet.getPacket().getModifier().readSafely(1);
            val method = actionObj.getClass().getMethod("a");
            method.setAccessible(true);
            val actionEnum = ((Enum<?>) method.invoke(actionObj)).ordinal();
            if (actionEnum == 1) {
                languagePlayer.removeBossbar(uuid);
                return;
            }
            if (actionEnum != 0 && actionEnum != 3) return;

            bossbar = WrappedChatComponent.fromHandle(ReflectionUtils.getDeclaredField(actionObj, "a"));
        } else {
            Action action = packet.getPacket().getEnumModifier(Action.class, 1).readSafely(0);
            if (action == Action.REMOVE) {
                languagePlayer.removeBossbar(uuid);
                return;
            }
            if (action != Action.ADD && action != Action.UPDATE_NAME) return;

            bossbar = packet.getPacket().getChatComponents().readSafely(0);
        }

        languagePlayer.setBossbar(uuid, bossbar.getJson());

        final Object finalActionObj = actionObj; // required for lambda
        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(bossbar),
                        languagePlayer,
                        main.getConfig().getBossbarSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(WrappedComponentUtils::serialize)
                .ifPresent(result -> {
                    if (getMCVersion() >= 17) {
                        ReflectionUtils.setDeclaredField(finalActionObj, "a", result.getHandle());
                    } else {
                        packet.getPacket().getChatComponents().writeSafely(0, result);
                    }
                });
    }

    @SuppressWarnings({"unchecked"})
    private void handleMerchantItems(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isItems()) return;

        try {
            ArrayList<?> recipes = (ArrayList<?>) packet.getPacket()
                    .getSpecificModifier(MERCHANT_RECIPE_LIST_CLASS).readSafely(0);
            ArrayList<Object> newRecipes = (ArrayList<Object>) MERCHANT_RECIPE_LIST_CLASS.newInstance();
            for (val recipeObject : recipes) {
                val recipe = (MerchantRecipe) ReflectionUtils.getMethod(recipeObject, "asBukkit");
                val originalSpecialPrice = ReflectionUtils.getDeclaredField(recipeObject, MERCHANT_RECIPE_SPECIAL_PRICE_FIELD);
                val originalDemand = ReflectionUtils.getDeclaredField(recipeObject, MERCHANT_RECIPE_DEMAND_FIELD);

                val newRecipe = new MerchantRecipe(ItemStackTranslationUtils.translateItemStack(recipe.getResult()
                        .clone(), languagePlayer, false), recipe.getUses(), recipe.getMaxUses(), recipe
                        .hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());

                for (val ingredient : recipe.getIngredients()) {
                    newRecipe.addIngredient(ItemStackTranslationUtils.translateItemStack(ingredient.clone(), languagePlayer, false));
                }

                Object newCraftRecipe = CRAFT_MERCHANT_RECIPE_FROM_BUKKIT_METHOD.invoke(null, newRecipe);
                Object newNMSRecipe = CRAFT_MERCHANT_RECIPE_TO_MINECRAFT_METHOD.invoke(newCraftRecipe);
                ReflectionUtils.setDeclaredField(newNMSRecipe, MERCHANT_RECIPE_SPECIAL_PRICE_FIELD, originalSpecialPrice);
                ReflectionUtils.setDeclaredField(newNMSRecipe, MERCHANT_RECIPE_DEMAND_FIELD, originalDemand);
                newRecipes.add(newNMSRecipe);
            }
            packet.getPacket().getModifier().writeSafely(1, newRecipes);
        } catch (IllegalAccessException | InstantiationException e) {
            Triton.get().getLogger().logError(e, "Failed to translate merchant items.");
        }
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

        // Pack name tag visibility, collision rule, team color and friendly flags into list
        val modifiers = packet.getPacket().getModifier();
        List<Object> options;
        WrappedChatComponent displayName, prefix, suffix;
        StructureModifier<WrappedChatComponent> chatComponents;

        if (getMCVersion() >= 17) {
            Optional<?> meta = (Optional<?>) modifiers.readSafely(3);
            if (!meta.isPresent()) return;

            val obj = meta.get();

            if (SCOREBOARD_TEAM_METADATA_MODIFIER == null)
                SCOREBOARD_TEAM_METADATA_MODIFIER = new StructureModifier<>(obj.getClass());
            val structure = SCOREBOARD_TEAM_METADATA_MODIFIER.withTarget(obj);

            options = Stream.of(3, 4, 5, 6).map(structure::readSafely).collect(Collectors.toList());

            chatComponents = structure.withType(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter());
            displayName = chatComponents.read(0);
            prefix = chatComponents.read(1);
            suffix = chatComponents.read(2);
        } else {
            options = Stream.of(4, 5, 6, 9).map(modifiers::readSafely).collect(Collectors.toList());

            chatComponents = packet.getPacket().getChatComponents();
            displayName = chatComponents.readSafely(0);
            prefix = chatComponents.readSafely(1);
            suffix = chatComponents.readSafely(2);
        }

        languagePlayer.setScoreboardTeam(teamName, displayName.getJson(), prefix.getJson(), suffix.getJson(), options);

        int i = 0;
        for (WrappedChatComponent component : Arrays.asList(displayName, prefix, suffix)) {
            final int currentIndex = i++;
            parser()
                    .translateComponent(
                            WrappedComponentUtils.deserialize(component),
                            languagePlayer,
                            main.getConfig().getScoreboardSyntax()
                    )
                    .getResultOrToRemove(Component::empty)
                    .map(WrappedComponentUtils::serialize)
                    .ifPresent(result -> chatComponents.writeSafely(currentIndex, result));
        }
    }

    private void handleScoreboardObjective(PacketEvent packet, SpigotLanguagePlayer languagePlayer) {
        if (!main.getConfig().isScoreboards()) return;

        val objectiveName = packet.getPacket().getStrings().readSafely(0);
        val mode = packet.getPacket().getIntegers().readSafely(0);

        if (mode == 1) {
            languagePlayer.removeScoreboardObjective(objectiveName);
            return;
        }
        // There are only 3 modes, so no need to check for more modes

        val chatComponentsModifier = packet.getPacket().getChatComponents();

        val healthDisplay = packet.getPacket().getModifier().readSafely(2);
        val displayName = chatComponentsModifier.readSafely(0);

        languagePlayer.setScoreboardObjective(objectiveName, displayName.getJson(), healthDisplay);

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
                    .logWarning("Failed to get SpigotLanguagePlayer because UUID of the player is unknown " +
                            "(possibly because the player hasn't joined yet).");
            return;
        }
        if (packet.getPacketType() != PacketType.Play.Client.SETTINGS) {
            return;
        }
        if (!languagePlayer.isWaitingForClientLocale()) {
            return;
        }
        Bukkit.getScheduler().runTask(
                main.getLoader(),
                () -> languagePlayer.setLang(
                        main.getLanguageManager()
                                .getLanguageByLocaleOrDefault(packet.getPacket().getStrings().readSafely(0))
                )
        );
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
        return ListeningWhitelist.newBuilder()
                .gamePhase(GamePhase.PLAYING)
                .types(PacketType.Play.Client.SETTINGS)
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

    @SneakyThrows
    public void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String json) {
        if (getMCVersion() <= 8) return;
        val bukkitPlayerOpt = player.toBukkit();
        if (!bukkitPlayerOpt.isPresent()) return;
        val bukkitPlayer = bukkitPlayerOpt.get();

        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.BOSS);
        packet.getUUIDs().writeSafely(0, uuid);
        if (getMCVersion() >= 17) {
            val msg = WrappedChatComponent.fromJson(json);
            val constructor = BOSSBAR_UPDATE_TITLE_ACTION_CLASS.getDeclaredConstructor(msg.getHandleType());
            constructor.setAccessible(true);
            val action = constructor.newInstance(msg.getHandle());
            packet.getModifier().writeSafely(1, action);
        } else {
            packet.getEnumModifier(Action.class, 1).writeSafely(0, Action.UPDATE_NAME);
            packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(json));
        }
        ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, true);
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
            packet.getModifier().writeSafely(2, value.getType());
            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, packet, true);
        });

        player.getTeamsMap().forEach((key, value) -> {
            val packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
            packet.getIntegers().writeSafely(0, 2); // Update team info mode
            packet.getStrings().writeSafely(0, key);
            if (getMCVersion() >= 17) {
                Optional<?> meta = (Optional<?>) packet.getModifier().readSafely(3);
                if (!meta.isPresent()) {
                    Triton.get().getLogger().logError("Triton was not able to refresh a scoreboard team, probably due to changes in ProtocolLib!");
                    return;
                }

                val obj = meta.get();

                if (SCOREBOARD_TEAM_METADATA_MODIFIER == null)
                    SCOREBOARD_TEAM_METADATA_MODIFIER = new StructureModifier<>(obj.getClass());
                val structure = SCOREBOARD_TEAM_METADATA_MODIFIER.withTarget(obj);

                int j = 0;
                for (int i : Arrays.asList(3, 4, 5, 6))
                    structure.writeSafely(i, value.getOptionData().get(j++));

                val chatComponents = structure.withType(
                        MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter());
                chatComponents.writeSafely(0, WrappedChatComponent.fromJson(value.getDisplayJson()));
                chatComponents.writeSafely(1, WrappedChatComponent.fromJson(value.getPrefixJson()));
                chatComponents.writeSafely(2, WrappedChatComponent.fromJson(value.getSuffixJson()));
            } else {
                packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(value.getDisplayJson()));
                packet.getChatComponents().writeSafely(1, WrappedChatComponent.fromJson(value.getPrefixJson()));
                packet.getChatComponents().writeSafely(2, WrappedChatComponent.fromJson(value.getSuffixJson()));
                int j = 0;
                for (int i : Arrays.asList(4, 5, 6, 9))
                    packet.getModifier().writeSafely(i, value.getOptionData().get(j++));
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
            nbt.put("name", "null").put("x", block.getX()).put("y", block.getY()).put("z", block.getZ()).put("id",
                    getMCVersion() <= 10 ? "Sign" : "minecraft:sign");
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, container, false);
            } catch (Exception e) {
                main.getLogger().logError("Failed refresh sign.");
            }
        }
    }

    /* UTILITIES */

    private boolean isActionbar(PacketContainer container) {
        if (getMCVersion() >= 19) {
            val booleans = container.getBooleans();
            if (booleans.size() > 0) {
                return booleans.readSafely(0);
            }
            return container.getIntegers().readSafely(0) == 2;
        } else if (getMCVersion() >= 12) {
            return container.getChatTypes().readSafely(0) == EnumWrappers.ChatType.GAME_INFO;
        } else {
            return container.getBytes().readSafely(0) == 2;
        }
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

    private boolean isPlayerInventoryOpen(Player player) {
        val nmsHandle = NMSUtils.getHandle(player);

        try {
            return Objects.requireNonNull(PLAYER_ACTIVE_CONTAINER_FIELD).get(nmsHandle).getClass() == CONTAINER_PLAYER_CLASS;
        } catch (IllegalAccessException | NullPointerException e) {
            return false;
        }
    }

    /**
     * BossBar packet Action wrapper
     */
    public enum Action {
        ADD, REMOVE, UPDATE_PCT, UPDATE_NAME, UPDATE_STYLE, UPDATE_PROPERTIES
    }

}
