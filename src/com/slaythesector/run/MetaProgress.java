package com.slaythesector.run;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Roguelite meta-progression: persists across runs in sector persistent data under
 * {@link com.slaythesector.util.STSConstants#PDATA_META}. Never cleared by
 * {@link RunStateManager#clear()}.
 */
public class MetaProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Set<String> unlockedShipVariants = new LinkedHashSet<String>();
    private int runsStarted;
    private int runsWon;
    private int bestDepth;

    public MetaProgress() {}

    public Set<String> getUnlockedShipVariants() { return unlockedShipVariants; }

    public int getRunsStarted() { return runsStarted; }
    public void incRunsStarted() { runsStarted++; }

    public int getRunsWon() { return runsWon; }
    public void incRunsWon() { runsWon++; }

    public int getBestDepth() { return bestDepth; }
    public void recordDepth(int depth) {
        if (depth > bestDepth) {
            bestDepth = depth;
        }
    }
}
