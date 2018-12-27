package com.rexcantor64.triton.api.config;

/**
 * Represents how Triton should look for placeholders.
 * @since 1.0.0
 */
public interface FeatureSyntax {

    /**
     * Get the key of the tag that starts/ends the entire placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "lang".
     * @return The key of the tag that starts/end the entire placeholder
     * @since 1.0.0
     */
    String getLang();

    /**
     * Get the key of the tag that starts/ends the variables of a placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "args".
     * @return The key of the tag that starts/end the variables of a placeholder
     * @since 1.0.0
     */
    String getArgs();

    /**
     * Get the key of the tag that starts/ends a variable of a placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "arg".
     * @return The key of the tag that starts/end a variable of a placeholder
     * @since 1.0.0
     */
    String getArg();
}
