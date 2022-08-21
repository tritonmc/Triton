package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.Triton;

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
        if (Triton.get().getLanguageManager().getMainLanguage() != null) {
            String result = Triton.get().getLanguageParser()
                    .replaceLanguages(superResult, Triton.get().getLanguageManager().getMainLanguage().getName(), Triton
                            .get().getConfig().getChatSyntax());
            if (result != null) return result;
        }
        return superResult;
    }
}
