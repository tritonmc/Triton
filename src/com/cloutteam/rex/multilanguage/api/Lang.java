package com.cloutteam.rex.multilanguage.api;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

/**
 * Represents a language. You can get the language info from this class.
 * 
 * @category Spigot/Bukkit only
 * 
 * @author Rexcantor64
 *
 */
public class Lang {

	/**
	 * Creates a new language object. You shouldn't create a language by
	 * yourself, as it doesn't save on the plugin languages map.
	 * 
	 * @param name
	 *            The language name
	 * @param flagCode
	 *            The flag code
	 * @param minecraftCode
	 *            The language code, on minecraft. This is used to compare
	 *            player's minecraft language. Leave empty if you don't want to
	 *            check for the player's minecraft language.
	 * @param displayName
	 *            The display name on chat and flag. Color codes compatible.
	 * 
	 * @author Rexcantor64
	 */
	public Lang(String name, String flagCode, String minecraftCode, String displayName) {
	}

	/**
	 * @return The language name
	 * 
	 * @author Rexcantor64
	 */
	public String getName() {
		return null;
	}

	/**
	 * @return The language code, on minecraft. This is used to compare player's
	 *         minecraft language. Leave empty if you don't want to check for
	 *         the player's minecraft language.
	 * 
	 * @author Rexcantor64
	 */
	public String getMinecraftCode() {
		return null;
	}

	/**
	 * @return The display name on chat and flag. Color codes compatible.
	 * 
	 * @author Rexcantor64
	 */
	public String getDisplayName() {
		return null;
	}

	/**
	 * @return The banner meta. Doesn't contain any name, lore or enchantment.
	 * 
	 * @author Rexcantor64
	 */
	public BannerMeta getMeta() {
		return null;
	}

	/**
	 * @return The flag item with the display name.
	 * 
	 * @author Rexcantor64
	 */
	public ItemStack getStack() {
		return null;
	}

	/**
	 * @return The language map. The key is the message code and the value is
	 *         the message on this language.
	 * 
	 * @author Rexcantor64
	 */
	public HashMap<String, String> getMap() {
		return null;
	}

}
