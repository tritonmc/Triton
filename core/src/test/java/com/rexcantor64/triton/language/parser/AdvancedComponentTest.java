package com.rexcantor64.triton.language.parser;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.chat.ComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdvancedComponentTest {

    @Test
    public void testColorCodeBetweenClickEvent() {
        BaseComponent root = new TextComponent();
        BaseComponent child1 = new TextComponent("Testing");
        child1.setColor(ChatColor.GRAY);
        child1.setStrikethrough(true);
        child1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("")));
        root.addExtra(child1);
        BaseComponent child2 = new TextComponent("another test");
        child2.setColor(ChatColor.GRAY);
        child2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("")));
        root.addExtra(child2);

        AdvancedComponent advancedComponent = AdvancedComponent.fromBaseComponent(root);
        advancedComponent.setText(advancedComponent.getTextClean());

        BaseComponent[] components = advancedComponent.toBaseComponent();

        String expectedResultJson = "{\"extra\":[{\"strikethrough\":true,\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"\"},\"extra\":[{\"text\":\"Testing\"}],\"text\":\"\"},{\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"\"},\"extra\":[{\"text\":\"another test\"}],\"text\":\"\"}],\"text\":\"\"}";
        assertEquals(expectedResultJson, ComponentSerializer.toString(components));
    }
}
