package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.AppenderRefFactory;
import lombok.var;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;

public class Log4jInjector {

    public static void injectAppender() {
        Logger logger = (Logger) LogManager.getRootLogger();
        Configuration config = logger.getContext().getConfiguration();

        var originalAppender = logger.getAppenders().get("TerminalConsole");
        if (originalAppender == null) originalAppender = logger.getAppenders().get("rewrite");
        if (originalAppender == null) {
            Triton.get().getLogger().logError("Failed to inject rewrite policy into Log4j. Terminal translation won't work properly.");
            return;
        }

        AppenderRef appenderRef = AppenderRefFactory.getAppenderRef(originalAppender.getName());

        if (appenderRef != null) {
            RewriteAppender appender = RewriteAppender.createAppender("TritonTerminalTranslation",
                    "false",
                    new AppenderRef[]{appenderRef},
                    config,
                    TritonTerminalRewrite.createPolicy(),
                    null);
            appender.start();
            logger.addAppender(appender);
            logger.removeAppender(originalAppender);
        }
    }

    public static void uninjectAppender() {
        Logger logger = (Logger) LogManager.getRootLogger();
        Configuration config = logger.getContext().getConfiguration();
        if (logger.getAppenders().containsKey("TritonTerminalTranslation")) {
            logger.removeAppender(logger.getAppenders().get("TritonTerminalTranslation"));
            logger.addAppender(config.getAppenders().get("TerminalConsole"));
        }
    }

}
