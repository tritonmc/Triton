package com.rexcantor64.triton.scoreboard;

import net.md_5.bungee.api.ChatColor;

import java.util.Objects;

public class ScoreboardComponent {

    private String text;
    private ChatColor color = ChatColor.WHITE;
    private boolean bold = false;
    private boolean italic = false;
    private boolean magic = false;
    private boolean strikethrough = false;
    private boolean underline = false;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void appendText(String text) {
        this.text += text;
    }

    public ChatColor getColor() {
        return color;
    }

    public void setColor(ChatColor color) {
        this.color = color;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isMagic() {
        return magic;
    }

    public void setMagic(boolean magic) {
        this.magic = magic;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public String getFormatting() {
        StringBuilder builder = new StringBuilder();
        builder.append(getColor());
        if (isBold()) builder.append(ChatColor.BOLD);
        if (isItalic()) builder.append(ChatColor.ITALIC);
        if (isMagic()) builder.append(ChatColor.MAGIC);
        if (isStrikethrough()) builder.append(ChatColor.STRIKETHROUGH);
        if (isUnderline()) builder.append(ChatColor.UNDERLINE);
        return builder.toString();
    }

    public boolean equalsFormatting(ScoreboardComponent that) {
        return bold == that.bold &&
                italic == that.italic &&
                magic == that.magic &&
                strikethrough == that.strikethrough &&
                underline == that.underline &&
                color == that.color;
    }

    @Override
    public String toString() {
        return "ScoreboardComponent{" +
                "text='" + text + '\'' +
                ", color=" + color +
                ", bold=" + bold +
                ", italic=" + italic +
                ", magic=" + magic +
                ", strikethrough=" + strikethrough +
                ", underline=" + underline +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreboardComponent that = (ScoreboardComponent) o;
        return bold == that.bold &&
                italic == that.italic &&
                magic == that.magic &&
                strikethrough == that.strikethrough &&
                underline == that.underline &&
                Objects.equals(text, that.text) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {

        return Objects.hash(text, color, bold, italic, magic, strikethrough, underline);
    }
}
