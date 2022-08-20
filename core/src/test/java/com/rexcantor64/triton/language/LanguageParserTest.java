package com.rexcantor64.triton.language;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class LanguageParserTest {

    @Test
    public void testGetPatternIndexArray() {
        String input = "Lorem ipsum [tag]dolor [tag]sit[/tag] amet[/tag], [tag2]consectetur[/tag2] [tag]adipiscing elit[/tag]. Nullam posuere.";

        List<Integer[]> result = LanguageParser.getPatternIndexArray(input, "tag");
        System.out.println("result = " + result);

        List<Integer[]> expected = Arrays.asList(
                new Integer[]{12, 48, 17, 42},
                new Integer[]{75, 101, 80, 95}
        );

        assertEquals(expected.size(), result.size());
        for (int i = 0; i < expected.size(); i++){
            assertArrayEquals(expected.get(i), result.get(i));
        }
    }

}
