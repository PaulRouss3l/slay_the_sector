package com.slaythesector.util;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.slaythesector.run.NodeType;

/** Maps a node's depth + type to encounter difficulty and reward-ship size. */
public final class DepthScaling {

    private DepthScaling() {}

    /**
     * Combat fleet-point budget for an encounter at the given depth/type.
     * Depth 1 is a single light frigate (~4 FP) vs the player's starting Wolf;
     * the budget then climbs as the player's reward fleet grows.
     */
    public static float combatPoints(int depth, NodeType type) {
        float base = 4f + (depth - 1) * 5f;
        switch (type) {
            case ELITE: return base * 1.5f;
            case BOSS:  return base * 2f;
            default:    return base;
        }
    }

    /** Fleet quality (0..1) — higher means better ships/officers. */
    public static float quality(int depth) {
        return Math.min(1f, 0.4f + depth * 0.07f);
    }

    /** Reward-ship hull size, tied to the depth (difficulty) of the cleared fight. */
    public static HullSize rewardSize(int depth) {
        if (depth >= 7) return HullSize.CAPITAL_SHIP;
        if (depth >= 5) return HullSize.CRUISER;
        if (depth >= 3) return HullSize.DESTROYER;
        return HullSize.FRIGATE;
    }

    /** Fleet-point budget to generate a candidate pool likely to contain {@code size}. */
    public static float rewardPoolPoints(HullSize size) {
        switch (size) {
            case CAPITAL_SHIP: return 140f;
            case CRUISER:      return 80f;
            case DESTROYER:    return 45f;
            default:           return 25f;
        }
    }
}
