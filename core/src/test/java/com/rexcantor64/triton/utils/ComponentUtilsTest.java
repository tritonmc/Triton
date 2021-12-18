package com.rexcantor64.triton.utils;

import lombok.val;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComponentUtilsTest {

    @Test
    public void testSplitByNewLineWithoutExtras() {
        BaseComponent component = new TextComponent("First line\nSecond line");
        component.setColor(ChatColor.BLUE);

        val result = ComponentUtils.splitByNewLine(Collections.singletonList(component));

        assertEquals(2, result.size());

        val jsonResult = result.stream().map(ComponentSerializer::toString).collect(Collectors.toList());

        assertEquals("[{\"color\":\"blue\",\"text\":\"First line\"}]", jsonResult.get(0));
        assertEquals("[{\"color\":\"blue\",\"text\":\"Second line\"}]", jsonResult.get(1));
    }

    @Test
    public void testSplitByNewLineWithExtras() {
        BaseComponent root = new TextComponent();
        root.setItalic(true);
        BaseComponent child1 = new TextComponent("First line\nSecond ");
        child1.setColor(ChatColor.BLACK);
        root.addExtra(child1);
        BaseComponent child2 = new TextComponent("li");
        child2.setColor(ChatColor.RED);
        BaseComponent childChild = new TextComponent("ne\nThird line");
        childChild.setUnderlined(true);
        child2.addExtra(childChild);
        child2.setBold(true);
        root.addExtra(child2);

        val result = ComponentUtils.splitByNewLine(Collections.singletonList(root));

        assertEquals(3, result.size());

        val jsonResult = result.stream().map(ComponentSerializer::toString).collect(Collectors.toList());

        assertEquals("[{\"italic\":true,\"extra\":[{\"color\":\"black\",\"text\":\"First line\"}],\"text\":\"\"}]", jsonResult.get(0));
        assertEquals("[{\"italic\":true,\"extra\":[{\"color\":\"black\",\"text\":\"Second \"},{\"bold\":true,\"color\":\"red\",\"extra\":[{\"underlined\":true,\"text\":\"ne\"}],\"text\":\"li\"}],\"text\":\"\"}]", jsonResult.get(1));
        assertEquals("[{\"italic\":true,\"extra\":[{\"bold\":true,\"color\":\"red\",\"extra\":[{\"underlined\":true,\"text\":\"Third line\"}],\"text\":\"\"}],\"text\":\"\"}]", jsonResult.get(2));
    }

    @Test
    public void testSplitByNewLineWithSlashNAtTheEnd() {
        BaseComponent[] root = ComponentSerializer.parse("{\"extra\":[{\"color\":\"dark_purple\",\"text\":\"First line!\\n\"},{\"color\":\"gray\",\"text\":\"Second Line\"}],\"text\":\"\"}");

        val result = ComponentUtils.splitByNewLine(Arrays.asList(root));

        assertEquals(2, result.size());

        val jsonResult = result.stream().map(ComponentSerializer::toString).collect(Collectors.toList());

        assertEquals("[{\"extra\":[{\"color\":\"dark_purple\",\"text\":\"First line!\"}],\"text\":\"\"}]", jsonResult.get(0));
        assertEquals("[{\"extra\":[{\"color\":\"dark_purple\",\"text\":\"\"},{\"color\":\"gray\",\"text\":\"Second Line\"}],\"text\":\"\"}]", jsonResult.get(1));
    }

}
