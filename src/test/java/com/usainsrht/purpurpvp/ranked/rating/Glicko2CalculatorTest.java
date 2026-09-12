package com.usainsrht.purpurpvp.ranked.rating;

import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Glicko2CalculatorTest {

    private Glicko2Calculator glicko2Calculator;
    private PlayerProfile player;

    @BeforeEach
    void setUp() {
        glicko2Calculator = new Glicko2Calculator();
        player = new PlayerProfile(UUID.randomUUID());
    }

    @Test
    void testWinIncreasesRatingAndDecreasesRd() {
        double initialRating = player.getGlickoRating();
        double initialRd = player.getGlickoRd();

        glicko2Calculator.updateRating(player, 1500.0, 200.0, 1.0);

        assertTrue(player.getGlickoRating() > initialRating, "Rating should increase after a win");
        assertTrue(player.getGlickoRd() < initialRd, "RD should decrease (confidence increase) after a match");
    }

    @Test
    void testLossDecreasesRatingAndDecreasesRd() {
        double initialRating = player.getGlickoRating();
        double initialRd = player.getGlickoRd();

        glicko2Calculator.updateRating(player, 1500.0, 200.0, 0.0);

        assertTrue(player.getGlickoRating() < initialRating, "Rating should decrease after a loss");
        assertTrue(player.getGlickoRd() < initialRd, "RD should decrease after a match");
    }

    @Test
    void testConsecutiveWinsAccumulate() {
        double r0 = player.getGlickoRating();
        glicko2Calculator.updateRating(player, 1500.0, 100.0, 1.0);
        double r1 = player.getGlickoRating();
        glicko2Calculator.updateRating(player, 1550.0, 100.0, 1.0);
        double r2 = player.getGlickoRating();

        assertTrue(r1 > r0, "First win increases rating");
        assertTrue(r2 > r1, "Second win further increases rating");
    }
}
