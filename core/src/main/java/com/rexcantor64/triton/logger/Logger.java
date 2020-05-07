package com.rexcantor64.triton.logger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.logging.Level;

@RequiredArgsConstructor
public class Logger {
    private final java.util.logging.Logger logger;
    @Setter
    private boolean debug;

    public void logInfo(String info, Object... arguments) {
        logger.log(Level.INFO, parseMessage(info, arguments));
    }

    public void logWarning(String warning, Object... arguments) {
        logger.log(Level.WARNING, parseMessage(warning, arguments));
    }

    public void logError(String error, Object... arguments) {
        logger.log(Level.SEVERE, parseMessage(error, arguments));
    }

    public void logDebug(String info, Object... arguments) {
        if (!debug) return;
        logger.log(Level.INFO, "[DEBUG] " + parseMessage(info, arguments));
    }

    public void logDebugWarning(String warning, Object... arguments) {
        if (!debug) return;
        logger.log(Level.WARNING, "[DEBUG] " + parseMessage(warning, arguments));
    }

    private String parseMessage(@NonNull String message, @NonNull Object... arguments) {
        for (int i = 0; i < arguments.length; ++i)
            message = message.replace("%" + (i + 1), String.valueOf(arguments[i]));
        return message;
    }

}
