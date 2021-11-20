package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.Triton;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.AppenderRef;

import java.lang.reflect.Method;

public class AppenderRefFactory {

    public static AppenderRef getAppenderRef(String appender) {
        try {
            return AppenderRef.createAppenderRef(appender, null, null);
        } catch (NoSuchMethodError e) {
            try {
                Method method = AppenderRef.class
                        .getMethod("createAppenderRef", String.class, String.class, Filter.class);
                return (AppenderRef) method.invoke(null, appender, null, null);
            } catch (Exception | Error ignore) {
                Triton.get().getLogger().logError("Failed to inject terminal translations!");
                return null;
            }
        }
    }

}
