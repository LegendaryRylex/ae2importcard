package dev.rylex.ae2importcard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Ae2ImportCardTest {
    @Test
    void modIdIsValid() {
        assertTrue(Ae2ImportCard.MOD_ID.matches("[a-z][a-z0-9_]{1,63}"));
    }
}
