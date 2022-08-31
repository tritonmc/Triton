package com.rexcantor64.triton.bungeecord.terminal;

import com.rexcantor64.triton.Triton;
import lombok.val;
import net.md_5.bungee.log.ConciseFormatter;

import java.util.logging.LogRecord;

public class BungeeTerminalFormatter extends ConciseFormatter {

    public BungeeTerminalFormatter() {
        super(true);
    }

    @Override
    public String format(LogRecord record) {
        val mainLanguage = Triton.get().getLanguageManager().getMainLanguage();

        String superResult = super.format(record);
        if (mainLanguage != null) {
            return Triton.get().getMessageParser()
                    .translateString(
                            superResult,
                            mainLanguage,
                            Triton.get().getConfig().getChatSyntax()
                    )
                    .getResult()
                    .orElse(superResult);
        }
        return superResult;
    }
}
