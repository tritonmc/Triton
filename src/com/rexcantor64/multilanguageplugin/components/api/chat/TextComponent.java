package com.rexcantor64.multilanguageplugin.components.api.chat;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rexcantor64.multilanguageplugin.components.api.ChatColor;

public class TextComponent
        extends BaseComponent {
    public void setText(String text) {
        this.text = text;
    }

    @ConstructorProperties({"text"})
    public TextComponent(String text) {
        this.text = text;
    }

    private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");
    private String text;

    public static BaseComponent[] fromLegacyText(String message) {
        ArrayList<BaseComponent> components = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        TextComponent component = new TextComponent();
        Matcher matcher = url.matcher(message);
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§') {
                i++;
                c = message.charAt(i);
                if ((c >= 'A') && (c <= 'Z')) {
                    c = (char) (c + ' ');
                }
                ChatColor format = ChatColor.getByChar(c);
                if (format != null) {
                    if (builder.length() > 0) {
                        TextComponent old = component;
                        component = new TextComponent(old);
                        old.setText(builder.toString());
                        builder = new StringBuilder();
                        components.add(old);
                    }
                    switch (format) {
                        case BOLD:
                            component.setBold(Boolean.TRUE);
                            break;
                        case ITALIC:
                            component.setItalic(Boolean.TRUE);
                            break;
                        case UNDERLINE:
                            component.setUnderlined(Boolean.TRUE);
                            break;
                        case STRIKETHROUGH:
                            component.setStrikethrough(Boolean.TRUE);
                            break;
                        case MAGIC:
                            component.setObfuscated(Boolean.TRUE);
                            break;
                        case RESET:
                            format = ChatColor.WHITE;
                        default:
                            component = new TextComponent();
                            component.setColor(format);
                            break;
                    }
                }
            } else {
                int pos = message.indexOf(' ', i);
                if (pos == -1) {
                    pos = message.length();
                }
                if (matcher.region(i, pos).find()) {
                    if (builder.length() > 0) {
                        TextComponent old = component;
                        component = new TextComponent(old);
                        old.setText(builder.toString());
                        builder = new StringBuilder();
                        components.add(old);
                    }
                    TextComponent old = component;
                    component = new TextComponent(old);
                    String urlString = message.substring(i, pos);
                    component.setText(urlString);
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                            (urlString.startsWith("http") || urlString.startsWith("https")) ? urlString : ("http://" + urlString)));
                    components.add(component);
                    i += pos - i - 1;
                    component = old;
                } else {
                    builder.append(c);
                }
            }
        }
        if (builder.length() > 0) {
            component.setText(builder.toString());
            components.add(component);
        } else if (component.hasFormatting()) {
            component.setText("");
            components.add(component);
        }
        if (components.isEmpty()) {
            components.add(new TextComponent(""));
        }
        return (BaseComponent[]) components.toArray(new BaseComponent[components.size()]);
    }

    public String getText() {
        return this.text;
    }

    public TextComponent() {
        this.text = "";
    }

    public TextComponent(TextComponent textComponent) {
        super(textComponent);
        setText(textComponent.getText());
    }

    public TextComponent(BaseComponent... extras) {
        setText("");
        setExtra(new ArrayList<>(Arrays.asList(extras)));
    }

    public BaseComponent duplicate() {
        return new TextComponent(this);
    }

    protected void toPlainText(StringBuilder builder) {
        builder.append(this.text);
        super.toPlainText(builder);
    }

    protected void toLegacyText(StringBuilder builder) {
        builder.append(getColor());
        if (isBold()) {
            builder.append(ChatColor.BOLD);
        }
        if (isItalic()) {
            builder.append(ChatColor.ITALIC);
        }
        if (isUnderlined()) {
            builder.append(ChatColor.UNDERLINE);
        }
        if (isStrikethrough()) {
            builder.append(ChatColor.STRIKETHROUGH);
        }
        if (isObfuscated()) {
            builder.append(ChatColor.MAGIC);
        }
        builder.append(this.text);
        super.toLegacyText(builder);
    }

    public String toString() {
        return String.format("TextComponent{text=%s, %s}", new Object[]{this.text, super.toString()});
    }
}