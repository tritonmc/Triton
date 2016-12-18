package com.cloutteam.rex.multilanguage.plugin;

import java.util.List;

import com.cloutteam.rex.multilanguage.plugin.MultiLanguagePlugin.MLPInterface;
import com.cloutteam.rex.multilanguage.plugin.MultiLanguagePlugin.PluginType;

import net.md_5.bungee.api.plugin.Plugin;

/**
 * 
 * Main class of MultiLanguagePlugin (Bungee side). Get an instance using
 * {@link com.cloutteam.rex.multilanguage.plugin.MultiLanguagePlugin#get()
 * MultiLanguagePlugin.get()}
 * 
 * @category Bungee only
 * 
 * @since 0.1.5
 * 
 * @author Rexcantor64
 *
 */
public class BungeeMLP extends Plugin implements MLPInterface {

	/**
	 * 
	 * @param messageCode
	 *            The message code on your .language file.
	 * @param args
	 *            The arguments to insert on the string. Leave empty
	 * @return
	 *
	 * @author Rexcantor64
	 */
	public String getMessage(String messageCode, String... args) {
		return null;
	}

	/**
	 * This reloads the plugin configuration file.
	 *
	 * @author Rexcantor64
	 */
	public void reloadConfigValues() {
	}

	/**
	 * 
	 * @return Returns {@link MultiLanguagePlugin.PluginType#BUNGEE
	 *         PluginType.BUNGEE}
	 * 
	 * @author Rexcantor64
	 */
	public PluginType getType() {
		return PluginType.BUNGEE;
	}

	/**
	 * Get the syntax. For example, if the syntax is "lang", the syntax in game
	 * will be [lang]messageCode[/lang]
	 * 
	 * @return The syntax to use in-game to start language messages.
	 * 
	 * @author Rexcantor64
	 */
	public String getSyntax() {
		return null;
	}

	/**
	 * Get the args syntax. For example, if the syntax is "lang" and the args
	 * syntax "args", the syntax in game will be
	 * [lang]messageCode[args]args[/args][/lang]
	 * 
	 * @return The syntax to use in-game to start the arguments list.
	 * 
	 * @author Rexcantor64
	 */
	public String getSyntaxArgs() {
		return null;
	}

	/**
	 * Get the arg syntax. For example, if the syntax is "lang", the args syntax
	 * "args" and the arg syntax "arg", the syntax in game will be
	 * [lang]messageCode[args][arg]arg1[/arg][arg]arg2[/arg][/args][/lang]
	 * 
	 * @return The syntax to use in-game to start the arguments.
	 * 
	 * @author Rexcantor64
	 */
	public String getSyntaxArg() {
		return null;
	}

	/**
	 * 
	 * @return If you are checking for language regex on chat or not.
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateChat() {
		return false;
	}

	/**
	 * 
	 * @return If you are checking for language regex on titles/subtitles or
	 *         not.
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateTitles() {
		return false;
	}

	/**
	 * 
	 * @return If you are checking for language regex on actionbars or not.
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateActionBars() {
		return false;
	}

	/**
	 * Not implemented on BungeeCord yet.
	 * 
	 * @return Always false
	 * 
	 * @deprecated
	 * 
	 * @author Rexcantor64
	 * 
	 */
	public boolean translateScoreboards() {
		return false;
	}

	/**
	 * Not implemented on BungeeCord yet.
	 * 
	 * @return If you are checking for language regex on kick messages or not.
	 * 
	 * @deprecated
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateKick() {
		return false;
	}

	/**
	 * 
	 * @return If you are checking for language regex on tab header and footer or not.
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateTab() {
		return false;
	}

	/**
	 * 
	 * @return If you are checking for language regex on boss bars or not.
	 * 
	 * @author Rexcantor64
	 */
	public boolean translateBossBars() {
		return false;
	}

	/**
	 * 
	 * @return A list containing all servers with MLP installed.
	 * 
	 * @author Rexcantor64
	 */
	public List<String> translateServers() {
		return null;
	}

}
