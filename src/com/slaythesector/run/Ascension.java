package com.slaythesector.run;

/**
 * Ascension levels, named after AI cores (vanilla and RAT) ordered by their officer level. Each
 * level adds its handicap to those below and +sts_ascensionPayout to the Payout.
 */
public final class Ascension {

    private Ascension() {}

    public static final int GAMMA = 0;
    public static final int BETA = 1;
    public static final int CHRONOS = 2;
    public static final int ALPHA = 3;
    public static final int SERAPH = 4;
    public static final int PRIMORDIAL = 5;
    public static final int OMEGA = 6;
    public static final int MAX = OMEGA;

    private static final String[] NAMES = { "Gamma", "Beta", "Chronos", "Alpha", "Seraph", "Primordial", "Omega" };
    private static final String[] EFFECTS = {
            "No handicap.",
            "+15% enemy strength.",
            "-20 supplies on every fight reward.",
            "More Elites on the map.",
            "Rest repairs only half.",
            "Bosses get pilots: Gamma cores, or officers.",
            "Unlocks Act 4 and the true Boss.",
    };

    public static String name(int level) {
        return NAMES[Math.max(0, Math.min(MAX, level))];
    }

    /** The handicap this level adds (on top of the lower ones). */
    public static String effect(int level) {
        return EFFECTS[Math.max(0, Math.min(MAX, level))];
    }
}
