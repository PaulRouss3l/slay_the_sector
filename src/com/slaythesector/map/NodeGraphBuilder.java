package com.slaythesector.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Pure logic (no game API): lays out one Act as START, {@code rows} rows of 2-3 Nodes, and a
 * single BOSS. Each Node links to the column-proportional Node of the next row and, half the
 * time, a neighbour; every Node keeps at least one parent and one successor.
 * <p>
 * Node types follow Slay the Spire's placement rules: the first two rows are Combat, the middle
 * row is Treasure, the row before the Boss is Rest, the rest is weighted (Combat, Shop, Rest)
 * with at least one Shop per Act and no Rest in row 3.
 */
public final class NodeGraphBuilder {

    private NodeGraphBuilder() {}

    /** Relative weights of the Node types drawn on rows without a fixed type. */
    public static class Weights {
        final float combat;
        final float shop;
        final float rest;

        public Weights(float combat, float shop, float rest) {
            this.combat = combat;
            this.shop = shop;
            this.rest = rest;
        }
    }

    public static void build(RunState state, int rows, List<String> enemyFactions, String bossFaction,
                             Weights weights, long seed) {
        // Deterministic map layout from a seed; not security-sensitive.
        Random rng = new Random(seed); // nosemgrep: java-lang-security-audit-crypto-weak-random

        List<List<RunNode>> grid = new ArrayList<List<RunNode>>();
        grid.add(row(new RunNode("n_d0_0", NodeType.START, 0, 0)));
        for (int d = 1; d <= rows; d++) {
            int width = 2 + rng.nextInt(2);
            List<RunNode> row = new ArrayList<RunNode>();
            boolean shopPlaced = false;
            for (int c = 0; c < width; c++) {
                NodeType type = typeFor(d, rows, weights, rng);
                if (type == NodeType.SHOP) {
                    // At most one Shop per row: a second one would just be a choice between two Shops.
                    if (shopPlaced) type = NodeType.COMBAT;
                    shopPlaced = true;
                }
                RunNode n = new RunNode("n_d" + d + "_" + c, type, d, c);
                if (type.isFight()) n.setFactionId(enemyFactions.get(rng.nextInt(enemyFactions.size())));
                row.add(n);
            }
            grid.add(row);
        }
        ensureShop(grid, rows, rng);
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

    private static NodeType typeFor(int depth, int rows, Weights w, Random rng) {
        if (depth <= 2) return NodeType.COMBAT;
        if (depth == rows) return NodeType.REST;
        if (depth == treasureRow(rows)) return NodeType.TREASURE;
        float rest = depth == 3 ? 0f : w.rest;
        float roll = rng.nextFloat() * (w.combat + w.shop + rest);
        if (roll < w.combat) return NodeType.COMBAT;
        if (roll < w.combat + w.shop) return NodeType.SHOP;
        return NodeType.REST;
    }

    /** The middle row of the Act. */
    public static int treasureRow(int rows) {
        return Math.max(3, (rows + 1) / 2);
    }

    /** At least one Shop per Act: turns a random Combat Node of a free row into one. */
    private static void ensureShop(List<List<RunNode>> grid, int rows, Random rng) {
        List<RunNode> candidates = new ArrayList<RunNode>();
        for (int d = 3; d < rows; d++) {
            if (d == treasureRow(rows)) continue;
            for (RunNode n : grid.get(d)) {
                if (n.getType() == NodeType.SHOP) return;
                if (n.getType() == NodeType.COMBAT) candidates.add(n);
            }
        }
        if (candidates.isEmpty()) return;
        RunNode old = candidates.get(rng.nextInt(candidates.size()));
        List<RunNode> row = grid.get(old.getDepth());
        RunNode shop = new RunNode(old.getId(), NodeType.SHOP, old.getDepth(), old.getColumn());
        row.set(row.indexOf(old), shop);
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
