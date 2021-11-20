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
     * @return The value of "open-selector-command-override" in the config.
     * @since 3.1.0
     */
    String getOpenSelectorCommandOverride();

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
     * @return The value of "log-level" in the config.
     * @since 3.0.0
     */
    int getLogLevel();

    /**
     * Although possible, it is not recommended to change the values in this list.
     * Consider it as read-only.
     *
     * @return The value of "command-aliases" in the config.
     * @since 3.2.0
     */
    List<String> getCommandAliases();

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
     * Spigot only.
     * From v3.0.0 to v3.2.0, this always returned false because scoreboard was dropped.
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
     * @deprecated Scoreboard translation has been removed in v3.0.0 and this will always return false from now on.
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
     * This only returns whether database storage is enabled or not. It is not possible to get the MySQL credentials
     * from the API, due to security reasons.
     *
     * @return The value of "database.enabled" in the config.
     * @since 2.6.0
     * @deprecated This has been deprecated since v3.0.0 in favor of {@link #getStorageType()}
     */
    boolean isMysql();

    /**
     * Get the storage type currently in use by the server.
     *
     * @return The string value of the storage type in use. ('local' or 'mysql')
     * @since 3.0.0
     */
    String getStorageType();

    /**
     * @return The value of "language-creation.motd.enabled" in the config.
     * @since 2.6.0
     */
    boolean isMotd();

    /**
     * @return The value of "language-creation.terminal" in the config.
     * @since 2.6.0
     */
    boolean isTerminal();

    /**
     * Spigot only
     *
     * @return The value of "language-creation.prevent-placeholders-in-chat" in the config.
     * @since 3.0.0
     */
    boolean isPreventPlaceholdersInChat();

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
     * From v3.0.0 to v3.2.0, this always returned a default
     * {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax}.
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
     * Spigot only
     *
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation.signs"
     * in the config.
     * @since 2.3.0
     */
    FeatureSyntax getSignsSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .bossbars" in the config.
     * @since 1.0.0
     */
    FeatureSyntax getBossbarSyntax();

    /**
     * @return The {@link com.rexcantor64.triton.api.config.FeatureSyntax FeatureSyntax} of "language-creation
     * .motd" in the config.
     * @since 2.6.0
     */
    FeatureSyntax getMotdSyntax();

}
