package com.accet.projecthub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantsTest {

    @Test
    void resolveProjectImageUsesEmbeddedSystemsImageForThatCategory() {
        assertEquals("photo-1518770660439-4636190af475", Constants.resolveProjectImage("Embedded Systems"));
    }

    @Test
    void resolveProjectImageFallsBackToDefaultForUnknownCategory() {
        assertEquals(Constants.DEFAULT_IMAGE, Constants.resolveProjectImage("Unknown category"));
    }
}
