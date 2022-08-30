package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import lombok.val;

import java.io.PrintStream;

public class TranslatablePrintStream extends PrintStream {

    private PrintStream original;

    public TranslatablePrintStream(PrintStream out) {
        super(out, true);
        this.original = out;
    }

    @Override
    public void print(Object obj) {
        super.print(translate(String.valueOf(obj)));
    }

    @Override
    public void print(String s) {
        super.print(translate(s));
    }

    private String translate(String input) {
        Language mainLanguage = Triton.get().getLanguageManager().getMainLanguage();
        if (mainLanguage != null) {
            return Triton.get().getMessageParser()
                    .translateString(
                            input,
                            mainLanguage,
                            Triton.get().getConfig().getChatSyntax()
                    )
                    .getResult()
                    .orElse(input);
        }
        return input;
    }

    public PrintStream getOriginal() {
        return original;
    }
}
