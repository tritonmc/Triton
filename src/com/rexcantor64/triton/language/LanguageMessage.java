package com.rexcantor64.triton.language;

import com.rexcantor64.triton.components.api.chat.BaseComponent;
import com.rexcantor64.triton.components.api.chat.TextComponent;
import com.rexcantor64.triton.components.api.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageMessage {

    private final Pattern format = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private LanguageMessage parent;
    private BaseComponent component;

    public LanguageMessage(LanguageMessage parent, BaseComponent component) {
        this.parent = parent;
        this.component = component;
    }

    public LanguageMessage getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public BaseComponent getComponent() {
        return component;
    }

    public boolean isTranslatableComponent() {
        return component instanceof TranslatableComponent;
    }

    public String getText() {
        if (component instanceof TextComponent)
            return ((TextComponent) component).getText();
        else if (component instanceof TranslatableComponent) {
            TranslatableComponent comp = (TranslatableComponent) component;
            StringBuilder builder = new StringBuilder();
            String trans;
            try {
                trans = comp.getLocales().getString(comp.getTranslate());
            } catch (MissingResourceException e) {
                trans = comp.getTranslate();
            }
            Matcher matcher = this.format.matcher(trans);
            int position = 0;
            int i = 0;
            while (matcher.find(position)) {
                int pos = matcher.start();
                if (pos != position) {
                    builder.append(trans.substring(position, pos));
                }
                position = matcher.end();

                String formatCode = matcher.group(2);
                switch (formatCode.charAt(0)) {
                    case 'd':
                    case 's':
                        String withIndex = matcher.group(1);
                        builder.append(comp.getWith().get(withIndex != null ? Integer.parseInt(withIndex) - 1 : i++).toPlainText());
                        break;
                    case '%':
                        builder.append('%');
                }
            }
            if (trans.length() != position) {
                builder.append(trans.substring(position, trans.length()));
            }
            return builder.toString();
        }
        return "";
    }

    public static List<LanguageMessage> fromBaseComponentArray(BaseComponent... components) {
        List<LanguageMessage> messages = new ArrayList<>();
        for (BaseComponent comp : components)
            addToList(messages, null, comp);
        return messages;
    }

    private static void addToList(List<LanguageMessage> languageMessages, LanguageMessage parent, BaseComponent comp) {
        LanguageMessage message = new LanguageMessage(parent, comp);
        languageMessages.add(message);
        if (comp.getExtra() != null)
            for (BaseComponent comp1 : comp.getExtra())
                addToList(languageMessages, message, comp1);
    }

    @Override
    public String toString() {
        return "LanguageMessage{" +
                "component=" + component +
                '}';
    }
}
