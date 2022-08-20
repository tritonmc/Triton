package com.rexcantor64.triton.utils;

import java.util.Arrays;
import java.util.StringJoiner;

public class StringUtils {

    public static String join(String delimiter, String... strings) {
        StringJoiner joiner = new StringJoiner(delimiter);
        Arrays.stream(strings).forEach(joiner::add);
        return joiner.toString();
    }

    public static boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }

}
