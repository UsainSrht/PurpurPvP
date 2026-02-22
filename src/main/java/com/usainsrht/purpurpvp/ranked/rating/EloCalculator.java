package com.usainsrht.purpurpvp.ranked.rating;

/**
 * Standard Elo rating calculator.
 */
public class EloCalculator {

    private final int kFactor;

    public EloCalculator(int kFactor) {
        this.kFactor = kFactor;
    }

    /**
     * Calculate the expected score for player A.
     * @param ratingA Player A's rating
     * @param ratingB Player B's rating
     * @return Expected score between 0 and 1
     */
    public double expectedScore(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    /**
     * Calculate new rating for a player.
     * @param playerRating Current rating
     * @param opponentRating Opponent's rating
     * @param actualScore 1.0 for win, 0.5 for draw, 0.0 for loss
     * @return New rating
     */
    public double calculateNewRating(double playerRating, double opponentRating, double actualScore) {
        double expected = expectedScore(playerRating, opponentRating);
        return playerRating + kFactor * (actualScore - expected);
    }
}

