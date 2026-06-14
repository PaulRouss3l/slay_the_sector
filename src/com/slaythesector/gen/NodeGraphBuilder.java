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
 * Pure logic (no game API) that lays out a run's node graph: a layered, branching
 * DAG from a single START down to a single BOSS. Every non-boss node has at least
 * one successor and every node below depth 0 has at least one parent, so the graph
 * is always traversable to the boss.
 *
 * <p>The first node returned is always START; the last is always BOSS.
 */
public final class NodeGraphBuilder {

    private NodeGraphBuilder() {}

    /** Number of depth layers below START (boss sits at this depth). */
    private static final int DEPTHS = 7;

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
            int width = 2 + rng.nextInt(2); // 2 or 3
            List<RunNode> row = new ArrayList<RunNode>();
            // Distinct factions within a row, so a choice never shows the same enemy twice.
            List<String> rowFactions = new ArrayList<String>(Arrays.asList(FACTION_POOL));
            Collections.shuffle(rowFactions, rng);
            int next = 0;
            for (int i = 0; i < width; i++) {
                NodeType type = (d == 1) ? NodeType.COMBAT : randomType(rng);
                RunNode node = makeNode("n_d" + d + "_" + i, type, d, i, width);
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

        // Connect each row to the next, guaranteeing parent + child coverage.
        for (int d = 0; d < rows.size() - 1; d++) {
            connect(rows.get(d), rows.get(d + 1), rng);
        }

        List<RunNode> flat = new ArrayList<RunNode>();
        for (List<RunNode> row : rows) {
            flat.addAll(row);
        }
        return flat;
    }

    private static void connect(List<RunNode> parents, List<RunNode> children, Random rng) {
        int pw = parents.size();
        int cw = children.size();

        // Every child gets at least one parent (nearest by normalized index).
        for (int j = 0; j < cw; j++) {
            int pi = (cw == 1) ? rng.nextInt(pw) : Math.round((float) j / (cw - 1) * (pw - 1));
            link(parents.get(pi), children.get(j));
        }
        // Every parent gets at least one child; optionally a second for branching.
        for (int i = 0; i < pw; i++) {
            RunNode p = parents.get(i);
            if (p.getSuccessorIds().isEmpty()) {
                int cj = (pw == 1) ? rng.nextInt(cw) : Math.round((float) i / (pw - 1) * (cw - 1));
                link(p, children.get(cj));
            }
            if (cw > 1 && rng.nextFloat() < 0.4f) {
                RunNode extra = children.get(rng.nextInt(cw));
                if (!p.getSuccessorIds().contains(extra.getId())) {
                    link(p, extra);
                }
            }
        }
    }

    private static void link(RunNode parent, RunNode child) {
        if (!parent.getSuccessorIds().contains(child.getId())) {
            parent.addSuccessor(child.getId());
        }
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
