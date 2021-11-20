package com.rexcantor64.triton.api.language;

import java.util.List;

/**
 * This represents a language in config.
 *
 * @since 1.0.0
 */
public interface Language {

    /**
     * @return The name of the language.
     * @since 1.0.0
     */
    String getName();

    /**
     * @return The locale codes of the language.
     * @since 1.0.0
     */
    List<String> getMinecraftCodes();

    /**
     * @return The display name of the language in the GUIs and chat with translated color codes (§).
     * @since 1.0.0
     */
    String getDisplayName();

    /**
     * @return The display name of the language in the GUIs and chat without the translated color codes, just (&amp;).
     * @since 1.0.0
     */
    String getRawDisplayName();

    /**
     * @return The code of the flag.
     * @since 1.0.0
     */
    String getFlagCode();

}
