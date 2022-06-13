package com.rexcantor64.triton.logger;

public interface TritonLogger {

    void logTrace(String message, Object... arguments);

    void logDebug(String message, Object... arguments);

    void logInfo(String message, Object... arguments);

    void logWarning(String message, Object... arguments);

    void logError(String message, Object... arguments);

    void logError(Throwable error, String message, Object... arguments);

    void setLogLevel(int level);

}
