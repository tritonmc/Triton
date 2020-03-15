package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.utils.AppenderRefFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;

public class Log4jInjector {

    public static void injectAppender() {
        Logger logger = (Logger) LogManager.getRootLogger();
        Configuration config = logger.getContext().getConfiguration();

        AppenderRef appenderRef = AppenderRefFactory.getAppenderRef("TerminalConsole");

        if (appenderRef != null) {
            RewriteAppender appender = RewriteAppender.createAppender("TritonTerminalTranslation",
                    "false",
                    new AppenderRef[]{appenderRef},
                    config,
                    TritonTerminalRewrite.createPolicy(),
                    null);
            appender.start();
            logger.addAppender(appender);
            logger.removeAppender(logger.getAppenders().get("TerminalConsole"));
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
