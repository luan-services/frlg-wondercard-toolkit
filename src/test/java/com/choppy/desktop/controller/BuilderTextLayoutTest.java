package com.choppy.desktop.controller;

import com.choppy.desktop.view.wondercard.WonderCardTextLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuilderTextLayoutTest {
    private final WonderCardTextLayout layout=new WonderCardTextLayout();
    @Test void enforcesBothTheFieldLengthAndStockWindowWidth() {
        assertTrue(layout.fits("title","MYSTERY EVENT",40));
        assertTrue(layout.fits("subtitle","CHOPPY'S CUSTOM EVENT",40));
        assertFalse(layout.fits("body1","i".repeat(41),40));
        assertFalse(layout.fits("subtitle","W".repeat(30),40));
        assertTrue(layout.width("W".repeat(30))>layout.maxWidth("subtitle"));
        assertEquals(200,layout.maxWidth("title")); assertEquals(224,layout.maxWidth("footer1"));
    }
    @Test void identifiesCharactersWithoutStockGlyphs() {
        assertTrue(layout.supported("Pokémon's gift?"));
        assertFalse(layout.supported("@😀"));
        assertEquals(layout.width("?"),layout.width("😀"));
    }
}
