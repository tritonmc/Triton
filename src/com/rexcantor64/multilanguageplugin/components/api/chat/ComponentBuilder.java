package com.rexcantor64.multilanguageplugin.components.api.chat;

import java.util.ArrayList;
import java.util.List;

import com.rexcantor64.multilanguageplugin.components.api.ChatColor;

public class ComponentBuilder {
    private TextComponent current;
    private final List<BaseComponent> parts = new ArrayList<>();

    public ComponentBuilder(ComponentBuilder original) {
        this.current = new TextComponent(original.current);
        for (BaseComponent baseComponent : original.parts) {
            this.parts.add(baseComponent.duplicate());
        }
    }

    public ComponentBuilder(String text) {
        this.current = new TextComponent(text);
    }

    public ComponentBuilder append(String text) {
        return append(text, FormatRetention.ALL);
    }

    public ComponentBuilder append(String text, FormatRetention retention) {
        this.parts.add(this.current);

        this.current = new TextComponent(this.current);
        this.current.setText(text);
        retain(retention);

        return this;
    }

    public ComponentBuilder color(ChatColor color) {
        this.current.setColor(color);
        return this;
    }

    public ComponentBuilder bold(boolean bold) {
        this.current.setBold(Boolean.valueOf(bold));
        return this;
    }

    public ComponentBuilder italic(boolean italic) {
        this.current.setItalic(Boolean.valueOf(italic));
        return this;
    }

    public ComponentBuilder underlined(boolean underlined) {
        this.current.setUnderlined(Boolean.valueOf(underlined));
        return this;
    }

    public ComponentBuilder strikethrough(boolean strikethrough) {
        this.current.setStrikethrough(Boolean.valueOf(strikethrough));
        return this;
    }

    public ComponentBuilder obfuscated(boolean obfuscated) {
        this.current.setObfuscated(Boolean.valueOf(obfuscated));
        return this;
    }

    public ComponentBuilder insertion(String insertion) {
        this.current.setInsertion(insertion);
        return this;
    }

    public ComponentBuilder event(ClickEvent clickEvent) {
        this.current.setClickEvent(clickEvent);
        return this;
    }

    public ComponentBuilder event(HoverEvent hoverEvent) {
        this.current.setHoverEvent(hoverEvent);
        return this;
    }

    public ComponentBuilder reset() {
        return retain(FormatRetention.NONE);
    }

    public ComponentBuilder retain(FormatRetention retention) {
        BaseComponent previous = this.current;
        switch (retention) {
            case NONE:
                this.current = new TextComponent(this.current.getText());
                break;
            case ALL:
                break;
            case EVENTS:
                this.current = new TextComponent(this.current.getText());
                this.current.setInsertion(previous.getInsertion());
                this.current.setClickEvent(previous.getClickEvent());
                this.current.setHoverEvent(previous.getHoverEvent());
                break;
            case FORMATTING:
                this.current.setClickEvent(null);
                this.current.setHoverEvent(null);
        }
        return this;
    }

    public BaseComponent[] create() {
        BaseComponent[] result = (BaseComponent[]) this.parts.toArray(new BaseComponent[this.parts.size() + 1]);
        result[this.parts.size()] = this.current;
        return result;
    }

    public enum FormatRetention {
        NONE, FORMATTING, EVENTS, ALL;
    }
}