package com.slaythesector.util;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.slaythesector.run.NodeType;

/** Fixed difficulty curve by Depth (not by the player's fleet strength). */
public final class DepthScaling {

    private DepthScaling() {}

    /** Enemy combat fleet points for a fight Node at the given Depth. */
    public static float combatPoints(int depth, NodeType type) {
        float base = STSSettings.enemyBaseFP() + (depth - 1) * STSSettings.enemyFPPerDepth();
        return type == NodeType.BOSS ? base * STSSettings.bossFPMult() : base;
    }

    /** Enemy fleet quality (0..1): fewer D-mods deeper in. */
    public static float quality(int depth) {
        return Math.min(1f, 0.4f + depth * 0.07f);
    }

    /** Largest hull size offered as a ship Reward at this Depth. */
    public static HullSize rewardSize(int depth) {
        if (depth >= 8) return HullSize.CRUISER;
        if (depth >= 4) return HullSize.DESTROYER;
        return HullSize.FRIGATE;
    }

    /** Officer level offered as a Reward at this Depth. */
    public static int officerLevel(int depth) {
        return Math.min(5, 1 + depth / 2);
    }
}
