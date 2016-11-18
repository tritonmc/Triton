package com.cloutteam.rex.multilanguage;

import org.bukkit.entity.Player;

import com.cloutteam.rex.multilanguage.api.Lang;

/**
 * This represents the Player. Here, only the language is saved.
 * 
 * @author Rexcantor64
 *
 */
public class LanguagePlayer {

	/**
	 * Creates a player object. Do not create an object, always get if from the
	 * {@link PlayerData#getData(Player) PlayerData}
	 * 
	 * @param p
	 *            The bukkit player.
	 */
	private LanguagePlayer(Player p) {
	}

	/**
	 * @return Returns the player's
	 *         {@link com.cloutteam.rex.multilanguage.api.Lang Language object}
	 * @author Rexcantor64
	 */
	public Lang getLang() {
		return null;
	}

	/**
	 * @param lang
	 *            The player's {@link com.cloutteam.rex.multilanguage.api.Lang
	 *            Language object}
	 * @author Rexcantor64
	 */
	public void setLang(Lang lang) {
	}

	/**
	 * @return Returns the Bukkit player object
	 * @author Rexcantor64
	 */
	public Player toBukkit() {
		return null;
	}

}
