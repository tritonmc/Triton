package com.rexcantor64.triton.logger;

public interface TritonLogger {

    void logInfo(String info, Object... arguments);

    void logWarning(String warning, Object... arguments);

    void logError(String error, Object... arguments);

    void logInfo(int level, String info, Object... arguments);

    void logWarning(int level, String warning, Object... arguments);

    void logError(int level, String error, Object... arguments);

    void setLogLevel(int level);

}
