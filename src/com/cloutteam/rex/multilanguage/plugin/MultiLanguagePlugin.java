package com.cloutteam.rex.multilanguage.plugin;

/**
 * 
 * Main class of MultiLanguagePlugin. Get an instance of {@link MLPInterface
 * MLPInterface} using {@link #get() get()}
 * 
 * @category Both Spigot/Bukkit and Bungee
 * 
 * @since 0.1.5
 * 
 * @author Rexcantor64
 *
 */
public class MultiLanguagePlugin {

	/**
	 * 
	 * @return The current MLPInterface in execution. It can be a
	 *         {@link BungeeMLP Bungee Instance} or {@link SpigotMLP Spigot
	 *         Instance}
	 *
	 * @author Rexcantor64
	 */
	public static MLPInterface get() {
		return null;
	}

	/**
	 * The interface that connects the two plugin types (Spigot and Bungee)
	 * 
	 * @author Rexcantor64
	 *
	 */
	public static interface MLPInterface {

		/**
		 * 
		 * @return The plugin type that's running ({@link PluginType#SPIGOT
		 *         spigot} or {@link PluginType#BUNGEE bungee})
		 *
		 * @author Rexcantor64
		 */
		public PluginType getType();

		/**
		 * Get the syntax. For example, if the syntax is "lang", the syntax in
		 * game will be [lang]messageCode[/lang]
		 * 
		 * @return The syntax to use in-game to start language messages.
		 * 
		 * @author Rexcantor64
		 */
		public String getSyntax();

		/**
		 * Get the args syntax. For example, if the syntax is "lang" and the
		 * args syntax "args", the syntax in game will be
		 * [lang]messageCode[args]args[/args][/lang]
		 * 
		 * @return The syntax to use in-game to start the arguments list.
		 * 
		 * @author Rexcantor64
		 */
		public String getSyntaxArgs();

		/**
		 * Get the arg syntax. For example, if the syntax is "lang", the args
		 * syntax "args" and the arg syntax "arg", the syntax in game will be
		 * [lang]messageCode[args][arg]arg1[/arg][arg]arg2[/arg][/args][/lang]
		 * 
		 * @return The syntax to use in-game to start the arguments.
		 * 
		 * @author Rexcantor64
		 */
		public String getSyntaxArg();

		/**
		 * 
		 * @return If you are checking for language regex on chat or not.
		 * 
		 * @author Rexcantor64
		 */
		public boolean translateChat();

		/**
		 * 
		 * @return If you are checking for language regex on actionbars or not.
		 * 
		 * @author Rexcantor64
		 */
		public boolean translateActionBars();

		/**
		 * 
		 * @return If you are checking for language regex on titles/subtitles or
		 *         not.
		 * 
		 * @author Rexcantor64
		 */
		public boolean translateTitles();

		/**
		 * 
		 * @return If you are checking for language regex on scoreboards or not.
		 * 
		 * @author Rexcantor64
		 */
		public boolean translateScoreboards();

	}

	/**
	 * An enum that contains the two possible plugin types
	 * ({@link PluginType#SPIGOT spigot} and {@link PluginType#BUNGEE bungee})
	 * 
	 * @author Rexcantor64
	 *
	 */
	public enum PluginType {
		SPIGOT, BUNGEE;
	}

}
