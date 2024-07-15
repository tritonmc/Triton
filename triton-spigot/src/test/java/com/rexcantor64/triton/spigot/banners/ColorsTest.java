package com.rexcantor64.triton.spigot.banners;

import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorsTest {

    @Test
    public void testToDyeColor() {
        // Test converting all colors to their respective DyeColor
        // This helps keep the conversion table up-to-date, since it would fail if the DyeColor enum changes

        assertEquals(Colors.BLACK.toDyeColor(), DyeColor.BLACK);
        assertEquals(Colors.RED.toDyeColor(), DyeColor.RED);
        assertEquals(Colors.GREEN.toDyeColor(), DyeColor.GREEN);
        assertEquals(Colors.BROWN.toDyeColor(), DyeColor.BROWN);
        assertEquals(Colors.BLUE.toDyeColor(), DyeColor.BLUE);
        assertEquals(Colors.PURPLE.toDyeColor(), DyeColor.PURPLE);
        assertEquals(Colors.CYAN.toDyeColor(), DyeColor.CYAN);
        assertEquals(Colors.GRAY.toDyeColor(), DyeColor.LIGHT_GRAY);
        assertEquals(Colors.DARK_GRAY.toDyeColor(), DyeColor.GRAY);
        assertEquals(Colors.PINK.toDyeColor(), DyeColor.PINK);
        assertEquals(Colors.LIME.toDyeColor(), DyeColor.LIME);
        assertEquals(Colors.YELLOW.toDyeColor(), DyeColor.YELLOW);
        assertEquals(Colors.LIGHT_BLUE.toDyeColor(), DyeColor.LIGHT_BLUE);
        assertEquals(Colors.MAGENTA.toDyeColor(), DyeColor.MAGENTA);
        assertEquals(Colors.ORANGE.toDyeColor(), DyeColor.ORANGE);
        assertEquals(Colors.WHITE.toDyeColor(), DyeColor.WHITE);
    }
}
