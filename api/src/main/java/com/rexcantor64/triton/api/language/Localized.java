package com.rexcantor64.triton.api.language;

/**
 * Represents something that has a language.
 * It can be a player or even a language itself.
 *
 * @since 3.8.0
 */
public interface Localized {

    /**
     * Get the string identifier of the language of this object.
     * Depending on the underlying implementation, it can get it from a
     * player's current language, a language object or even a string itself.
     *
     * @return The string identifier of the language of this object.
     * @since 3.8.0
     */
    default String getLanguageId() {
        final Language language = this.getLanguage();
        if (language == null) {
            return null;
        }
        return this.getLanguage().getName();
    }

    /**
     * Get the language of this object.
     * Depending on the underlying implementation, it can get it from a
     * player's current language, a language object or derive it from its string id.
     *
     * @return The language of this object.
     * @since 3.8.0
     */
    Language getLanguage();

}
