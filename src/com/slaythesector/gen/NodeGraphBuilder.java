package com.slaythesector.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;

/**
 * Pure logic (no game API) that lays out a run's node graph: layered, with a single
 * START, fixed-width middle rows, and a single BOSS. Every node connects to the whole
 * next row, so the player is always offered the same number of choices each depth
 * (the full next row) until the final boss.
 *
 * <p>The first node returned is always START; the last is always BOSS.
 */
public final class NodeGraphBuilder {

    private NodeGraphBuilder() {}

    /** Number of depth layers below START (boss sits at this depth). */
    private static final int DEPTHS = 7;

    /** Fixed nodes per middle row — also the number of choices shown each depth. */
    private static final int ROW_WIDTH = 3;

    private static final float TOP_Y = 7200f;
    private static final float DEPTH_GAP = 900f;
    private static final float COLUMN_SPREAD = 900f;

    /** Factions the player can fight (combat/elite nodes); boss is always Remnants. */
    private static final String[] FACTION_POOL = {
        Factions.PIRATES, Factions.HEGEMONY, Factions.TRITACHYON,
        Factions.LUDDIC_CHURCH, Factions.LUDDIC_PATH, Factions.PERSEAN, Factions.DIKTAT,
    };

    public static List<RunNode> build(long seed) {
        // Deterministic map layout from a seed; not security-sensitive.
        Random rng = new Random(seed); // nosemgrep: java-lang-security-audit-crypto-weak-random

        // Row 0: START. Rows 1..DEPTHS-1: 2-3 nodes. Row DEPTHS: BOSS.
        List<List<RunNode>> rows = new ArrayList<List<RunNode>>();

        List<RunNode> startRow = new ArrayList<RunNode>();
        startRow.add(makeNode("n_d0_0", NodeType.START, 0, 0, 1));
        rows.add(startRow);

        for (int d = 1; d < DEPTHS; d++) {
            List<RunNode> row = new ArrayList<RunNode>();
            // Distinct factions within a row, so a choice never shows the same enemy twice.
            List<String> rowFactions = new ArrayList<String>(Arrays.asList(FACTION_POOL));
            Collections.shuffle(rowFactions, rng);
            int next = 0;
            for (int i = 0; i < ROW_WIDTH; i++) {
                NodeType type = (d == 1) ? NodeType.COMBAT : randomType(rng);
                RunNode node = makeNode("n_d" + d + "_" + i, type, d, i, ROW_WIDTH);
                if (type.isCombat()) {
                    node.setFactionId(rowFactions.get(next++));
                }
                row.add(node);
            }
            rows.add(row);
        }

        List<RunNode> bossRow = new ArrayList<RunNode>();
        RunNode boss = makeNode("n_d" + DEPTHS + "_0", NodeType.BOSS, DEPTHS, 0, 1);
        boss.setFactionId(Factions.REMNANTS);
        bossRow.add(boss);
        rows.add(bossRow);

        // Fully connect each row to the next: every node offers the whole next row.
        for (int d = 0; d < rows.size() - 1; d++) {
            for (RunNode parent : rows.get(d)) {
                for (RunNode child : rows.get(d + 1)) {
                    parent.addSuccessor(child.getId());
                }
            }
        }

        List<RunNode> flat = new ArrayList<RunNode>();
        for (List<RunNode> row : rows) {
            flat.addAll(row);
        }
        return flat;
    }

    private static NodeType randomType(Random rng) {
        float r = rng.nextFloat();
        if (r < 0.55f) return NodeType.COMBAT;
        if (r < 0.75f) return NodeType.EVENT;
        if (r < 0.90f) return NodeType.SHOP;
        return NodeType.ELITE;
    }

    private static RunNode makeNode(String id, NodeType type, int depth, int indexInRow, int rowWidth) {
        float x = (indexInRow - (rowWidth - 1) / 2f) * COLUMN_SPREAD;
        float y = TOP_Y - depth * DEPTH_GAP;
        return new RunNode(id, type, depth, x, y);
    }
}
