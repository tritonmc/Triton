package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.Triton;

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
        String result = Triton.get().getLanguageParser()
                .replaceLanguages(input, Triton.get().getLanguageManager().getMainLanguage().getName(), Triton
                        .get().getConf().getChatSyntax());
        if (result != null) return result;
        return input;
    }

    public PrintStream getOriginal() {
        return original;
    }
}
