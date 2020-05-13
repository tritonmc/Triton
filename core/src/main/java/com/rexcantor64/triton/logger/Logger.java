package com.rexcantor64.triton.logger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.logging.Level;

@RequiredArgsConstructor
public class Logger {
    private final java.util.logging.Logger logger;
    @Setter
    private int logLevel = 0;

    public void logInfo(String info, Object... arguments) {
        logInfo(0, info, arguments);
    }

    public void logWarning(String warning, Object... arguments) {
        logWarning(0, warning, arguments);
    }

    public void logError(String error, Object... arguments) {
        logError(0, error, arguments);
    }

    public void logInfo(int level, String info, Object... arguments) {
        if (level > logLevel) return;
        logger.log(Level.INFO, getPrefix(level) + parseMessage(info, arguments));
    }

    public void logWarning(int level, String warning, Object... arguments) {
        if (level > logLevel) return;
        logger.log(Level.WARNING, getPrefix(level) + parseMessage(warning, arguments));
    }

    public void logError(int level, String error, Object... arguments) {
        if (level > logLevel) return;
        logger.log(Level.SEVERE, getPrefix(level) + parseMessage(error, arguments));
    }

    private String parseMessage(@NonNull String message, @NonNull Object... arguments) {
        for (int i = 0; i < arguments.length; ++i)
            message = message.replace("%" + (i + 1), String.valueOf(arguments[i]));
        return message;
    }

    private String getPrefix(int level) {
        return level > 1 ? "[DEBUG] " : "";
    }

}
