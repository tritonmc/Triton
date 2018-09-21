package com.rexcantor64.triton.components.api.chat;

import com.rexcantor64.triton.components.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseComponent {
    BaseComponent parent;
    private ChatColor color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;
    private String insertion;
    private List<BaseComponent> extra;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;

    public void setColor(ChatColor color) {
        this.color = color;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }

    public void setItalic(Boolean italic) {
        this.italic = italic;
    }

    public void setUnderlined(Boolean underlined) {
        this.underlined = underlined;
    }

    public void setStrikethrough(Boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public void setObfuscated(Boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public void setInsertion(String insertion) {
        this.insertion = insertion;
    }

    public void setClickEvent(ClickEvent clickEvent) {
        this.clickEvent = clickEvent;
    }

    public void setHoverEvent(HoverEvent hoverEvent) {
        this.hoverEvent = hoverEvent;
    }

    public String toString() {
        return "BaseComponent(color=" + getColor() + ", bold=" + this.bold + ", italic=" + this.italic + ", underlined=" + this.underlined + ", strikethrough=" + this.strikethrough + ", obfuscated=" + this.obfuscated + ", insertion=" + getInsertion() + ", extra=" + getExtra() + ", clickEvent=" + getClickEvent() + ", hoverEvent=" + getHoverEvent() + ")";
    }

    public String getInsertion() {
        return this.insertion;
    }

    public List<BaseComponent> getExtra() {
        return this.extra;
    }

    public ClickEvent getClickEvent() {
        return this.clickEvent;
    }

    public HoverEvent getHoverEvent() {
        return this.hoverEvent;
    }

    BaseComponent(BaseComponent old) {
        setColor(old.getColorRaw());
        setBold(old.isBoldRaw());
        setItalic(old.isItalicRaw());
        setUnderlined(old.isUnderlinedRaw());
        setStrikethrough(old.isStrikethroughRaw());
        setObfuscated(old.isObfuscatedRaw());
        setInsertion(old.getInsertion());
        setClickEvent(old.getClickEvent());
        setHoverEvent(old.getHoverEvent());
        if (old.getExtra() != null) {
            for (BaseComponent component : old.getExtra()) {
                addExtra(component.duplicate());
            }
        }
    }

    public static String toLegacyText(BaseComponent... components) {
        StringBuilder builder = new StringBuilder();
        for (BaseComponent msg : components) {
            builder.append(msg.toLegacyText());
        }
        return builder.toString();
    }

    public static String toPlainText(BaseComponent... components) {
        StringBuilder builder = new StringBuilder();
        for (BaseComponent msg : components) {
            builder.append(msg.toPlainText());
        }
        return builder.toString();
    }

    public ChatColor getColor() {
        if (this.color == null) {
            if (this.parent == null) {
                return ChatColor.WHITE;
            }
            return this.parent.getColor();
        }
        return this.color;
    }

    public ChatColor getTrueColor() {
        if (this.color == null) {
            if (this.parent == null)
                return null;
            return this.parent.getTrueColor();
        }
        return this.color;
    }

    public ChatColor getColorRaw() {
        return this.color;
    }

    public boolean isBold() {
        if (this.bold == null) {
            return (this.parent != null) && (this.parent.isBold());
        }
        return this.bold.booleanValue();
    }

    public Boolean isTrueBold() {
        if (this.bold == null) {
            if (this.parent == null) return null;
            this.parent.isTrueBold();
        }
        return this.bold;
    }

    public Boolean isBoldRaw() {
        return this.bold;
    }

    public boolean isItalic() {
        if (this.italic == null) {
            return (this.parent != null) && (this.parent.isItalic());
        }
        return this.italic.booleanValue();
    }

    public Boolean isTrueItalic() {
        if (this.italic == null) {
            if (this.parent == null) return null;
            this.parent.isTrueItalic();
        }
        return this.italic;
    }

    public Boolean isItalicRaw() {
        return this.italic;
    }

    public boolean isUnderlined() {
        if (this.underlined == null) {
            return (this.parent != null) && (this.parent.isUnderlined());
        }
        return this.underlined.booleanValue();
    }

    public Boolean isTrueUnderlined() {
        if (this.underlined == null) {
            if (this.parent == null) return null;
            this.parent.isTrueUnderlined();
        }
        return this.underlined;
    }

    public Boolean isUnderlinedRaw() {
        return this.underlined;
    }

    public boolean isStrikethrough() {
        if (this.strikethrough == null) {
            return (this.parent != null) && (this.parent.isStrikethrough());
        }
        return this.strikethrough.booleanValue();
    }

    public Boolean isTrueStrikethrough() {
        if (this.strikethrough == null) {
            if (this.parent == null) return null;
            this.parent.isTrueStrikethrough();
        }
        return this.strikethrough;
    }

    public Boolean isStrikethroughRaw() {
        return this.strikethrough;
    }

    public boolean isObfuscated() {
        if (this.obfuscated == null) {
            return (this.parent != null) && (this.parent.isObfuscated());
        }
        return this.obfuscated.booleanValue();
    }

    public Boolean isTrueObfuscated() {
        if (this.obfuscated == null) {
            if (this.parent == null) return null;
            this.parent.isTrueObfuscated();
        }
        return this.obfuscated;
    }

    public Boolean isObfuscatedRaw() {
        return this.obfuscated;
    }

    public void setExtra(List<BaseComponent> components) {
        for (BaseComponent component : components) {
            component.parent = this;
        }
        this.extra = components;
    }

    public void addExtra(String text) {
        addExtra(new TextComponent(text));
    }

    public void addExtra(BaseComponent component) {
        if (this.extra == null) {
            this.extra = new ArrayList();
        }
        component.parent = this;
        this.extra.add(component);
    }

    public boolean hasFormatting() {
        return (this.color != null) || (this.bold != null) || (this.italic != null) || (this.underlined != null) || (this.strikethrough != null) || (this.obfuscated != null) || (this.insertion != null) || (this.hoverEvent != null) || (this.clickEvent != null);
    }

    public String toPlainText() {
        StringBuilder builder = new StringBuilder();
        toPlainText(builder);
        return builder.toString();
    }

    void toPlainText(StringBuilder builder) {
        if (this.extra != null) {
            for (BaseComponent e : this.extra) {
                e.toPlainText(builder);
            }
        }
    }

    public String toLegacyText() {
        StringBuilder builder = new StringBuilder();
        toLegacyText(builder);
        return builder.toString();
    }

    void toLegacyText(StringBuilder builder) {
        if (this.extra != null) {
            for (BaseComponent e : this.extra) {
                e.toLegacyText(builder);
            }
        }
    }

    public abstract BaseComponent duplicate();

    public BaseComponent() {
    }
}
