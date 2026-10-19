package com.slaythesector.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Pure logic (no game API): lays out one Act as START, {@code rows} rows of 2-3 Combat Nodes,
 * and a single BOSS. Each Node links to the column-proportional Node of the next row and, half
 * the time, a neighbour; every Node keeps at least one parent and one successor.
 */
public final class NodeGraphBuilder {

    private NodeGraphBuilder() {}

    public static void build(RunState state, int rows, List<String> enemyFactions, String bossFaction, long seed) {
        // Deterministic map layout from a seed; not security-sensitive.
        Random rng = new Random(seed); // nosemgrep: java-lang-security-audit-crypto-weak-random

        List<List<RunNode>> grid = new ArrayList<List<RunNode>>();
        grid.add(row(new RunNode("n_d0_0", NodeType.START, 0, 0)));
        for (int d = 1; d <= rows; d++) {
            int width = 2 + rng.nextInt(2);
            List<RunNode> row = new ArrayList<RunNode>();
            for (int c = 0; c < width; c++) {
                RunNode n = new RunNode("n_d" + d + "_" + c, NodeType.COMBAT, d, c);
                n.setFactionId(enemyFactions.get(rng.nextInt(enemyFactions.size())));
                row.add(n);
            }
            grid.add(row);
        }
        RunNode boss = new RunNode("n_d" + (rows + 1) + "_0", NodeType.BOSS, rows + 1, 0);
        boss.setFactionId(bossFaction);
        grid.add(row(boss));

        for (int d = 0; d < grid.size() - 1; d++) {
            link(grid.get(d), grid.get(d + 1), rng);
        }
        for (List<RunNode> row : grid) {
            for (RunNode n : row) state.addNode(n);
        }
        state.setCurrentNodeId("n_d0_0");
    }

    private static void link(List<RunNode> parents, List<RunNode> children, Random rng) {
        boolean[] hasParent = new boolean[children.size()];
        for (int p = 0; p < parents.size(); p++) {
            int c = proportional(p, parents.size(), children.size());
            connect(parents.get(p), children, c, hasParent);
            if (children.size() > 1 && rng.nextBoolean()) {
                int neighbour = c + (rng.nextBoolean() ? 1 : -1);
                if (neighbour >= 0 && neighbour < children.size()) {
                    connect(parents.get(p), children, neighbour, hasParent);
                }
            }
        }
        for (int c = 0; c < children.size(); c++) {
            if (!hasParent[c]) {
                connect(parents.get(proportional(c, children.size(), parents.size())), children, c, hasParent);
            }
        }
    }

    private static void connect(RunNode parent, List<RunNode> children, int c, boolean[] hasParent) {
        parent.addSuccessor(children.get(c).getId());
        hasParent[c] = true;
    }

    private static int proportional(int i, int from, int to) {
        if (from <= 1 || to <= 1) return from <= 1 ? (to - 1) / 2 : 0;
        return Math.round(i * (to - 1) / (float) (from - 1));
    }

    private static List<RunNode> row(RunNode n) {
        List<RunNode> r = new ArrayList<RunNode>();
        r.add(n);
        return r;
    }
}
