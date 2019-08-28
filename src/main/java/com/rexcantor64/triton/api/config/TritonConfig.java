package com.rexcantor64.triton.api.config;

import com.rexcantor64.triton.api.wrappers.EntityType;

import java.util.List;

/**
 * Represents the config.yml
 *
 * @since 1.0.0
 */
public interface TritonConfig {

    /**
     * @return The value of "run-language-commands-on-join" in the config.
     * @since 1.0.0
     */
    boolean isRunLanguageCommandsOnLogin();

    /**
     * @return The value of "force-client-locale-on-join" in the config.
     * @since 1.3.0
     */
    boolean isAlwaysCheckClientLocale();

    /**
     * Spigot only
     *
     * @return The value of "bungeecord" in the config.
     * @since 1.0.0
     */
    boolean isBungeecord();

    /**
     * @return The value of "language-creation.disabled-line" in the config.
     * @since 1.0.0
     */
    String getDisabledLine();

    /**
     * @return The value of "language-creation.chat.enabled" in the config.
     * @since 1.0.0
     */
    boolean isChat();

    /**
     * @return The value of "language-creation.actionbars.enabled" in the config.
     * @since 1.0.0
     */
    boolean isActionbars();

    /**
     * @return The value of "language-creation.titles.enabled" in the config.
     * @since 1.0.0
     */
    boolean isTitles();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.guis.enabled" in the config.
     * @since 1.0.0
     */
    boolean isGuis();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.scoreboards.enabled" in the config.
     * @since 1.0.0
     */
    boolean isScoreboards();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.scoreboards.advanced" in the config.
     * @since 1.0.0
     */
    boolean isScoreboardsAdvanced();

    /**
     * Spigot only
     * Be careful because this method returns a list of a custom wrapper of EntityType, not Bukkit's EntityType.
     *
     * @return The value of "language-creation.holograms.types" in the config.
     * @since 1.0.0
     */
    List<EntityType> getHolograms();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.holograms.allow-all" in the config.
     * @since 1.0.0
     */
    boolean isHologramsAll();

    /**
     * @return The value of "language-creation.kick.enabled" in the config.
     * @since 1.0.0
     */
    boolean isKick();

    /**
     * @return The value of "language-creation.tab.enabled" in the config.
     * @since 1.0.0
     */
    boolean isTab();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.items.enabled" in the config.
     * @since 1.0.0
     */
    boolean isItems();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.items.allow-in-inventory" in the config.
     * @since 1.0.0
     */
    boolean isInventoryItems();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.items.books" in the config.
     * @since 1.4.0
     */
    boolean isBooks();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.signs.enabled" in the config.
     * @since 1.0.0
     */
    boolean isSigns();

    /**
     * @return The value of "language-creation.bossbars.enabled" in the config.
     * @since 1.0.0
     */
    boolean isBossbars();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.chat"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getChatSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .actionbars" in the config.
     * @since 1.0.0
     */
    FeatureSyntax getActionbarSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.title"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getTitleSyntax();

    /**
     * Spigot only
     *
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.guis"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getGuiSyntax();

    /**
     * Spigot only
     *
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .scoreboards" in the config.
     * @since 1.0.0
     */
    FeatureSyntax getScoreboardSyntax();

    /**
     * Spigot only
     *
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .holograms" in the config.
     * @since 1.0.0
     */
    FeatureSyntax getHologramSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.kick"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getKickSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.tab"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getTabSyntax();

    /**
     * Spigot only
     *
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.items"
     * in the config.
     * @since 1.0.0
     */
    FeatureSyntax getItemsSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .bossbars" in the config.
     * @since 1.0.0
     */
    FeatureSyntax getBossbarSyntax();

}
