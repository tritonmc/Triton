package com.rexcantor64.triton.spigot.terminal;

import com.rexcantor64.triton.Triton;
import lombok.val;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class SpigotTerminalFormatter extends SimpleFormatter {

    @Override
    public synchronized String format(LogRecord record) {
        return handleString(super.format(record));
    }

    @Override
    public synchronized String formatMessage(LogRecord record) {
        return handleString(super.formatMessage(record));
    }

    private String handleString(String superResult) {
        val mainLanguage = Triton.get().getLanguageManager().getMainLanguage();
        if (mainLanguage != null) {
            return Triton.get().getMessageParser()
                    .translateString(superResult, mainLanguage, Triton.get().getConfig().getChatSyntax())
                    .getResult()
                    .orElse(superResult);
        }
        return superResult;
    }
}
