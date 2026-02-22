package com.usainsrht.purpurpvp.ranked.rating;

import com.usainsrht.purpurpvp.ranked.PlayerProfile;

/**
 * Glicko-2 rating system implementation.
 * Reference: http://www.glicko.net/glicko/glicko2.pdf
 */
public class Glicko2Calculator {

    private static final double TAU = 0.5;  // system constant (constrains volatility change)
    private static final double EPSILON = 0.000001;
    private static final double GLICKO2_SCALE = 173.7178;

    /**
     * Update a player's Glicko-2 rating after a single game result.
     *
     * @param profile      The player's profile
     * @param opponentRating Opponent's Glicko rating (not Glicko-2 scale)
     * @param opponentRd    Opponent's RD
     * @param score         1.0 for win, 0.5 for draw, 0.0 for loss
     */
    public void updateRating(PlayerProfile profile, double opponentRating, double opponentRd, double score) {
        // Step 1: Convert to Glicko-2 scale
        double mu = (profile.getGlickoRating() - 1500.0) / GLICKO2_SCALE;
        double phi = profile.getGlickoRd() / GLICKO2_SCALE;
        double sigma = profile.getGlickoVolatility();

        double muJ = (opponentRating - 1500.0) / GLICKO2_SCALE;
        double phiJ = opponentRd / GLICKO2_SCALE;

        // Step 2: Compute g(phi) and E
        double gPhiJ = g(phiJ);
        double eVal = e(mu, muJ, gPhiJ);

        // Step 3: Compute estimated variance
        double v = 1.0 / (gPhiJ * gPhiJ * eVal * (1.0 - eVal));

        // Step 4: Compute delta
        double delta = v * gPhiJ * (score - eVal);

        // Step 5: Compute new volatility
        double newSigma = computeNewVolatility(sigma, delta, phi, v);

        // Step 6: Update phi to phi*
        double phiStar = Math.sqrt(phi * phi + newSigma * newSigma);

        // Step 7: Update phi and mu
        double newPhi = 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / v);
        double newMu = mu + newPhi * newPhi * gPhiJ * (score - eVal);

        // Step 8: Convert back to Glicko scale
        profile.setGlickoRating(newMu * GLICKO2_SCALE + 1500.0);
        profile.setGlickoRd(newPhi * GLICKO2_SCALE);
        profile.setGlickoVolatility(newSigma);
    }

    private double g(double phi) {
        return 1.0 / Math.sqrt(1.0 + 3.0 * phi * phi / (Math.PI * Math.PI));
    }

    private double e(double mu, double muJ, double gPhiJ) {
        return 1.0 / (1.0 + Math.exp(-gPhiJ * (mu - muJ)));
    }

    /**
     * Iterative algorithm to compute new volatility (Step 5 of Glicko-2).
     */
    private double computeNewVolatility(double sigma, double delta, double phi, double v) {
        double a = Math.log(sigma * sigma);
        double deltaSquared = delta * delta;
        double phiSquared = phi * phi;

        // Function f(x) from the paper
        double A = a;
        double B;
        if (deltaSquared > phiSquared + v) {
            B = Math.log(deltaSquared - phiSquared - v);
        } else {
            int k = 1;
            B = a - k * TAU;
            while (f(B, deltaSquared, phiSquared, v, a) < 0) {
                k++;
                B = a - k * TAU;
            }
        }

        // Iterate
        double fA = f(A, deltaSquared, phiSquared, v, a);
        double fB = f(B, deltaSquared, phiSquared, v, a);

        while (Math.abs(B - A) > EPSILON) {
            double C = A + (A - B) * fA / (fB - fA);
            double fC = f(C, deltaSquared, phiSquared, v, a);

            if (fC * fB <= 0) {
                A = B;
                fA = fB;
            } else {
                fA /= 2.0;
            }
            B = C;
            fB = fC;
        }

        return Math.exp(A / 2.0);
    }

    private double f(double x, double deltaSquared, double phiSquared, double v, double a) {
        double eX = Math.exp(x);
        double num1 = eX * (deltaSquared - phiSquared - v - eX);
        double den1 = 2.0 * (phiSquared + v + eX) * (phiSquared + v + eX);
        double term1 = num1 / den1;
        double term2 = (x - a) / (TAU * TAU);
        return term1 - term2;
    }
}

