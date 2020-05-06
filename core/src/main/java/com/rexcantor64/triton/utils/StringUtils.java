package com.rexcantor64.triton.utils;

public class StringUtils {

    public static String join(String delimiter, String... strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i != 0) builder.append(delimiter);
            builder.append(strings[i]);
        }
        return builder.toString();
    }

}
