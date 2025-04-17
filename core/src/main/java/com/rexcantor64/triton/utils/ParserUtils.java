package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.language.parser.TranslationConfiguration;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * Methods used in implementations of {@link com.rexcantor64.triton.api.language.MessageParser}.
 *
 * @since 4.0.0
 */
public class ParserUtils {

    /**
     * Find the indexes of all root "[pattern][/pattern]" tags in the given string.
     * <p>
     * Only the root tags are included, that is, nested tags are ignored.
     * For example, <code>[pattern][pattern][/pattern][/pattern]</code> would only
     * return the indexes for the outer tags.
     * <p>
     * Each array in the returned list corresponds to a different set of opening and closing tags,
     * and has size 4.
     * Indexes have the following meaning:
     * <ul>
     *     <li>0: the first character of the opening tag</li>
     *     <li>1: the character after the last character of the closing tag</li>
     *     <li>2: the character after the last character of the opening tag</li>
     *     <li>3: the first character of the closing tag</li>
     * </ul>
     *
     * @param input   The string to search for opening and closing tags.
     * @param pattern The tags to search for (i.e. "lang" will search for "[lang]" and "[/lang]").
     * @return A list of indexes of all the found tags, as specified by the method description.
     */
    public static List<Integer[]> getPatternIndexArray(String input, String pattern) {
        List<Integer[]> result = new ArrayList<>();
        int start = -1;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1,
                    i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1,
                    i + 3 + pattern.length()).equals("/" + pattern + "]")) {
                openedAmount--;
                if (openedAmount == 0) {
                    result.add(new Integer[]{start, i + 3 + pattern.length(), start + pattern.length() + 2, i});
                    start = -1;
                }
            }
        }
        return result;
    }

    /**
     * Removes legacy <code>[args][/args]</code> tags from (the end of) translation keys.
     * Since v4.0.0, these tags are no longer needed and are therefore deprecated.
     * For backwards compatibility, ignore them.
     *
     * @param key The key, potentially ending in <code>[args]</code>, <code>[/args]</code>, or both.
     * @param configuration The settings being applied while translating the placeholder with this key.
     * @return The key with the <code>[args][/args]</code> removed.
     */
    public static String normalizeTranslationKey(String key, TranslationConfiguration<?> configuration) {
        val syntax = configuration.getFeatureSyntax().getArgs();
        // The [args] tag is optional since v4.0.0, so strip it if it's present
        if (key.endsWith("[/" + syntax + "]")) {
            key = key.substring(0, key.length() - syntax.length() - 3);
        }
        if (key.endsWith("[" + configuration.getFeatureSyntax().getArgs() + "]")) {
            key = key.substring(0, key.length() - syntax.length() - 2);
        }
        return key;
    }
}
