package com.rexcantor64.triton.terminal;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.utils.NMSUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.SimpleMessage;

@Plugin(name = "TritonTerminal", category = "Core", elementType = "rewritePolicy", printObject = false)
public final class TritonTerminalRewrite implements RewritePolicy {

    ChatColorTerminalReplacer chatColorTerminalReplacer;

    public TritonTerminalRewrite() {
        this.chatColorTerminalReplacer = new ChatColorTerminalReplacer();
    }

    @PluginFactory
    public static TritonTerminalRewrite createPolicy() {
        return new TritonTerminalRewrite();
    }

    @Override
    public LogEvent rewrite(final LogEvent event) {
        Language lang = Triton.get().getLanguageManager().getMainLanguage();
        if (lang == null) return event;
        String translated = Triton.get().getLanguageParser()
                .replaceLanguages(event.getMessage().getFormattedMessage(), lang.getName(), Triton.get().getConf()
                        .getChatSyntax());
        if (translated == null)
            return event;
        if (Triton.get().getConf().isTerminalAnsi())
            translated = chatColorTerminalReplacer.parseMessage(translated);

        try {
            NMSUtils.setDeclaredField(event, "message", new SimpleMessage(translated));
        } catch (Exception | Error ignored) {
        }
        return event;
    }
}
