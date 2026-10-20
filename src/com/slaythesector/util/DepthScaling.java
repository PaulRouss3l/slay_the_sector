package com.slaythesector.util;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Fixed difficulty curve (never the player's fleet strength): strength climbs row by row and never
 * drops between Acts; Bosses scale with the Act factor; Ascension adds on top. Rewards grow with
 * Act and row.
 */
public final class DepthScaling {

    private DepthScaling() {}

    /** Enemy combat fleet points of a fight Node (before the Elite multiplier). */
    public static float combatPoints(RunState state, RunNode node) {
        return combatPoints(state, node.getDepth(), node.getType());
    }

    /**
     * Fights climb from where the previous Act's fights ended (never easier than the last fight
     * before a Boss) to this Act's own last-fight strength, (base + perDepth × (rows-1)) × Act
     * factor. A Boss is (base + perDepth × rows) × Act factor × Boss multiplier.
     */
    public static float combatPoints(RunState state, int row, NodeType type) {
        int act = state.getAct();
        int rows = Math.max(2, STSSettings.actRows());
        float fp;
        if (type == NodeType.BOSS) {
            fp = (STSSettings.enemyBaseFP() + rows * STSSettings.enemyFPPerDepth()) * STSSettings.actFactor(act)
                    * STSSettings.bossFPMult();
        } else {
            float start = act <= 1 ? STSSettings.enemyBaseFP() : lastFight(act - 1, rows);
            float end = lastFight(act, rows);
            float t = Math.max(0f, Math.min(1f, (row - 1) / (float) (rows - 1)));
            fp = start + (end - start) * t;
        }
        if (state.getAscension() >= Ascension.BETA) fp *= 1f + STSSettings.ascensionEnemyMult();
        return fp;
    }

    /** Strength of an Act's last row (the fights just before its Boss). */
    private static float lastFight(int act, int rows) {
        return (STSSettings.enemyBaseFP() + (rows - 1) * STSSettings.enemyFPPerDepth()) * STSSettings.actFactor(act);
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
