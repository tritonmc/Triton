package com.rexcantor64.triton.logger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class SLF4JLogger implements TritonLogger {
    private final Logger logger;
    @Setter
    private int logLevel = 1000;

    public void logTrace(String message, Object... arguments) {
        if (logLevel < 2) return;
        logger.info("[TRACE] " + parseMessage(message, arguments));
    }

    public void logDebug(String message, Object... arguments) {
        if (logLevel < 1) return;
        logger.info("[DEBUG] " + parseMessage(message, arguments));
    }

    public void logInfo(String message, Object... arguments) {
        logger.info(parseMessage(message, arguments));
    }

    public void logWarning(String message, Object... arguments) {
        logger.warn(parseMessage(message, arguments));
    }

    public void logError(String message, Object... arguments) {
        logger.error(parseMessage(message, arguments));
    }

    public void logError(Throwable error, String message, Object... arguments) {
        logger.error(parseMessage(message, arguments), error);
    }

    private String parseMessage(@NonNull String message, @NonNull Object... arguments) {
        for (int i = 0; i < arguments.length; ++i)
            message = message.replace("%" + (i + 1), String.valueOf(arguments[i]));
        return message;
    }

}
