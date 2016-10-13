package com.cloutteam.rex.multilanguage.api;

import org.bukkit.entity.Player;

import com.cloutteam.rex.multilanguage.LanguagePlayer;

/**
 * The Language class. This is the core of the language management. You can get
 * it's instance using
 * {@link com.cloutteam.rex.multilanguage.MultiLanguagePlugin#getLanguageManager()
 * MultiLanguagePlugin.get().getLanguageManager()}
 * 
 * @author Rexcantor64
 *
 */
public class Language {

	/**
	 * Get a message using the
	 * {@link com.cloutteam.rex.multilanguage.LanguagePlayer LanguagePlayer}.
	 * 
	 * @param lp
	 *            The language player. Get it by using
	 *            {@link com.cloutteam.rex.multilanguage.PlayerData#getData(Player)
	 *            PlayerData.getData(bukkitPlayer)}
	 * @param message
	 *            The message code.
	 * @param complements
	 *            (Optional) The arguments to replace on the message. If the
	 *            message (on the language file) is "Welcome %1!" and you give
	 *            "player" on the first argument, the output will be
	 *            "Welcome player!"
	 * @return The un-coded message with the arguments replaced.
	 * @author Rexcantor64
	 *
	 */
	public String get(LanguagePlayer lp, String message, Object... complements) {
		return null;
	}

	/**
	 * Get a message using the {@link org.bukkit.entity.Player Bukkit Player}.
	 * 
	 * @param p
	 *            The bukkit player. Get it by using
	 *            {@link org.bukkit.Bukkit#getPlayer(String)
	 *            Bukkit.getPlayer(name or UUID)}
	 * @param message
	 *            The message code.
	 * @param complements
	 *            (Optional) The arguments to replace on the message. If the
	 *            message (on the language file) is "Welcome %1!" and you give
	 *            "player" on the first argument, the output will be
	 *            "Welcome player!"
	 * @return The un-coded message with the arguments replaced.
	 * @author Rexcantor64
	 *
	 */
	public String get(Player p, String message, Object... complements) {
		return null;
	}

	/**
	 * @param name
	 *            The message you want to check
	 * @param langCode
	 *            The language code you want to know if the name param is part
	 *            of.
	 * @return If the langCode represents the name on any language.
	 * @author Rexcantor64
	 *
	 */
	public boolean isLangMessage(String name, String langCode) {
		return false;
	}

	/**
	 * Get a language using its name
	 * 
	 * @param name
	 *            The language name
	 * @param fallback
	 *            When true, if the language isn't found, it will return the
	 *            main language instead of null
	 * @return The {@link com.cloutteam.rex.multilanguage.api.Lang Lang} object
	 * @author Rexcantor64
	 *
	 */
	public Lang getLangByName(String name, boolean fallback) {
		return null;
	}

	/**
	 * Get a language using its code
	 * 
	 * @param minecraftCode
	 *            The language code
	 * @param fallback
	 *            When true, if the language isn't found, it will return the
	 *            main language instead of null
	 * @return The {@link com.cloutteam.rex.multilanguage.api.Lang Lang} object
	 * @author Rexcantor64
	 *
	 */
	public Lang getLangByMcCode(String minecraftCode, boolean fallback) {
		return null;
	}

}
