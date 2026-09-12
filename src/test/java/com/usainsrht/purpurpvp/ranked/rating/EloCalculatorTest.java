package com.usainsrht.purpurpvp.ranked.rating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EloCalculatorTest {

    private EloCalculator eloCalculator;

    @BeforeEach
    void setUp() {
        eloCalculator = new EloCalculator(32);
    }

    @Test
    void testExpectedScoreEqualRatings() {
        double expected = eloCalculator.expectedScore(1000.0, 1000.0);
        assertEquals(0.5, expected, 0.0001, "Expected score for equal ratings should be 0.5");
    }

    @Test
    void testExpectedScoreHigherRating() {
        double expected = eloCalculator.expectedScore(1200.0, 1000.0);
        assertTrue(expected > 0.5, "Higher rated player should have expected score > 0.5");
        assertEquals(0.7597, expected, 0.001);
    }

    @Test
    void testWinIncreasesRating() {
        double newRating = eloCalculator.calculateNewRating(1000.0, 1000.0, 1.0);
        assertEquals(1016.0, newRating, 0.0001, "Win against equal opponent with K=32 should increase rating by 16");
    }

    @Test
    void testLossDecreasesRating() {
        double newRating = eloCalculator.calculateNewRating(1000.0, 1000.0, 0.0);
        assertEquals(984.0, newRating, 0.0001, "Loss against equal opponent with K=32 should decrease rating by 16");
    }

    @Test
    void testDrawEqualRatings() {
        double newRating = eloCalculator.calculateNewRating(1000.0, 1000.0, 0.5);
        assertEquals(1000.0, newRating, 0.0001, "Draw against equal opponent should leave rating unchanged");
    }

    @Test
    void testUpsetWinYieldsLargerGain() {
        double normalWin = eloCalculator.calculateNewRating(1200.0, 1000.0, 1.0) - 1200.0;
        double upsetWin = eloCalculator.calculateNewRating(1000.0, 1200.0, 1.0) - 1000.0;
        assertTrue(upsetWin > normalWin, "Upset win should give more points than expected win");
    }
}
