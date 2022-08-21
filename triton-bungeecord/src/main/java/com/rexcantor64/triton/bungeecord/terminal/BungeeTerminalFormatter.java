package com.rexcantor64.triton.bungeecord.terminal;

import com.rexcantor64.triton.Triton;
import net.md_5.bungee.log.ConciseFormatter;

import java.util.logging.LogRecord;

public class BungeeTerminalFormatter extends ConciseFormatter {

    public BungeeTerminalFormatter() {
        super(true);
    }

    @Override
    public String format(LogRecord record) {
        String superResult = super.format(record);
        if (Triton.get().getLanguageManager().getMainLanguage() != null) {
            String result = Triton.get().getLanguageParser()
                    .replaceLanguages(superResult, Triton.get().getLanguageManager().getMainLanguage().getName(), Triton
                            .get().getConfig().getChatSyntax());
            if (result != null) return result;
        }
        return superResult;
    }
}
