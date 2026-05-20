package com.githubchart;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorSchemeTest {

    @Test
    void shouldHaveFiveColorsInDefaultScheme() {
        assertEquals(5, ColorScheme.DEFAULT.getColors().length);
    }

    @Test
    void shouldHaveDefaultGreenColors() {
        assertArrayEquals(
                new String[]{"#eeeeee", "#d6e685", "#8cc665", "#44a340", "#1e6823"},
                ColorScheme.DEFAULT.getColors());
    }

    @Test
    void shouldHaveHalloweenScheme() {
        assertArrayEquals(
                new String[]{"#EEEEEE", "#FFEE4A", "#FFC501", "#FE9600", "#03001C"},
                ColorScheme.HALLOWEEN.getColors());
    }

    @Test
    void shouldHaveTealScheme() {
        assertArrayEquals(
                new String[]{"#EEEEEE", "#7FFFD4", "#76EEC6", "#66CDAA", "#458B74"},
                ColorScheme.TEAL.getColors());
    }

    @Test
    void shouldDarkenColor() {
        String darkened = ColorScheme.darken("409ba5", 0.8);
        assertNotNull(darkened);
        assertTrue(darkened.startsWith("#"));
        assertEquals(7, darkened.length());
        // 0x40*0.8=0x33, 0x9b*0.8=0x7c, 0xa5*0.8=0x84
        assertEquals("#337c84", darkened);
    }

    @Test
    void shouldLightenColor() {
        String lightened = ColorScheme.lighten("409ba5", 0.3);
        assertNotNull(lightened);
        assertTrue(lightened.startsWith("#"));
        assertEquals(7, lightened.length());
    }

    @Test
    void shouldGenerateSchemeFromHex() {
        ColorScheme scheme = ColorScheme.fromHex("409ba5");
        assertEquals(5, scheme.getColors().length);
        assertEquals("#EEEEEE", scheme.getColors()[0]);  // always lightest bg
    }

    @Test
    void shouldStripHashFromInput() {
        String darkened = ColorScheme.darken("#409ba5", 0.8);
        assertEquals("#337c84", darkened);
    }

    @Test
    void fromHexShouldProduceValidHexColors() {
        ColorScheme scheme = ColorScheme.fromHex("409ba5");
        for (String color : scheme.getColors()) {
            assertTrue(color.matches("#[0-9a-fA-F]{6}"), "Invalid color: " + color);
        }
    }
}
