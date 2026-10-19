package com.slaythesector.util;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Fixed difficulty curve (never the player's fleet strength): strength grows with the row inside
 * an Act, times the Act factor, plus the Ascension handicap. Rewards grow with Act and row.
 */
public final class DepthScaling {

    private DepthScaling() {}

    /** Enemy combat fleet points of a fight Node (before the Elite multiplier). */
    public static float combatPoints(RunState state, RunNode node) {
        return combatPoints(state, node.getDepth(), node.getType());
    }

    public static float combatPoints(RunState state, int row, NodeType type) {
        float base = STSSettings.enemyBaseFP() + (row - 1) * STSSettings.enemyFPPerDepth();
        base *= STSSettings.actFactor(state.getAct());
        if (state.getAscension() >= Ascension.BETA) base *= 1f + STSSettings.ascensionEnemyMult();
        return type == NodeType.BOSS ? base * STSSettings.bossFPMult() : base;
    }

    /** Enemy fleet quality (0..1): fewer D-mods deeper in the Run. */
    public static float quality(RunState state, RunNode node) {
        return Math.min(1f, 0.4f + state.getGlobalDepth(node) * 0.07f);
    }

    /** Largest hull size offered as a ship Reward: one step per Act, plus rows 4 and 8. */
    public static HullSize rewardSize(RunState state, int row) {
        int tier = state.getAct() - 1 + (row >= 4 ? 1 : 0) + (row >= 8 ? 1 : 0);
        HullSize[] sizes = { HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP };
        return sizes[Math.min(sizes.length - 1, tier)];
    }

    /** Officer level offered as a Reward or in a Shop. */
    public static int officerLevel(RunState state, int row) {
        return Math.min(5, state.getAct() + row / 3);
    }
}
