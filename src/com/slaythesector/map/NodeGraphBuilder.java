package com.slaythesector.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Pure logic (no game API): lays out one Act the way Slay the Spire does.
 * <p>
 * <b>Paths</b>: on a grid of {@code columns} × {@code rows}, {@code paths} paths climb from row 1
 * to the top row, each step moving to one of the three nearest cells above without crossing an
 * existing edge; the first two paths start in different cells. Only cells a path touches become
 * Nodes. START links to every row-1 Node, every top-row Node links to the BOSS.
 * <p>
 * <b>Types</b>: rows 1-2 Combat, the middle row Treasure, the top row Rest. Other Nodes are drawn
 * by weight (Combat, Elite, Ambush, Event, Shop, Rest) and re-drawn while they break a rule: no
 * Rest or Elite before row 4, no Ambush before row 3, no Rest on the row under the Rest row; an
 * Elite, Shop or Rest never follows a Node of the same type; Nodes reached from the same parent
 * all differ. At least one Shop per Act.
 */
public final class NodeGraphBuilder {

    private NodeGraphBuilder() {}

    /** First rows where a drawn Rest / Elite / Ambush may appear. */
    private static final int FIRST_REST_ROW = 4;
    private static final int FIRST_ELITE_ROW = 4;
    private static final int FIRST_AMBUSH_ROW = 3;
    private static final int MAX_DRAWS = 30;

    /** Relative weights of the Node types drawn on rows without a fixed type. */
    public static class Weights {
        final Map<NodeType, Float> byType = new LinkedHashMap<NodeType, Float>();

        public Weights(float combat, float shop, float rest, float elite, float ambush, float event) {
            byType.put(NodeType.COMBAT, combat);
            byType.put(NodeType.SHOP, shop);
            byType.put(NodeType.REST, rest);
            byType.put(NodeType.ELITE, elite);
            byType.put(NodeType.AMBUSH, ambush);
            byType.put(NodeType.EVENT, event);
        }
    }

    public static void build(RunState state, int rows, int columns, int paths, List<String> enemyFactions,
                             String bossFaction, Weights weights, long seed) {
        // Deterministic map layout from a seed; not security-sensitive.
        Random rng = new Random(seed); // nosemgrep: java-lang-security-audit-crypto-weak-random

        // cells[row][col] -> successor columns on row + 1; row 0 unused (START sits below row 1).
        Map<Integer, Map<Integer, List<Integer>>> edges = new HashMap<Integer, Map<Integer, List<Integer>>>();
        List<Integer> starts = new ArrayList<Integer>();
        for (int p = 0; p < paths; p++) {
            int col = rng.nextInt(columns);
            if (p == 1) {
                while (columns > 1 && col == starts.get(0)) col = rng.nextInt(columns);
            }
            starts.add(col);
            touch(edges, 1, col);
            for (int row = 1; row < rows; row++) {
                int next = step(edges, row, col, columns, rng);
                link(edges, row, col, next);
                touch(edges, row + 1, next);
                col = next;
            }
        }

        // Nodes, row by row, so parents are typed before their children.
        int mid = (columns - 1) / 2;
        RunNode start = new RunNode("n_d0_" + mid, NodeType.START, 0, mid);
        state.addNode(start);
        Map<String, RunNode> byCell = new LinkedHashMap<String, RunNode>();
        Map<String, List<RunNode>> parents = new HashMap<String, List<RunNode>>();
        Map<String, NodeType> types = new HashMap<String, NodeType>();
        for (int row = 1; row <= rows; row++) {
            Map<Integer, List<Integer>> cells = edges.get(row);
            for (int col = 0; col < columns; col++) {
                if (cells == null || !cells.containsKey(col)) continue;
                String id = "n_d" + row + "_" + col;
                List<RunNode> ps = row == 1 ? single(start) : parents.get(id);
                NodeType type = typeFor(row, rows, ps, types, weights, rng);
                types.put(id, type);
                RunNode n = new RunNode(id, type, row, col);
                if (type.isFight()) n.setFactionId(enemyFactions.get(rng.nextInt(enemyFactions.size())));
                byCell.put(id, n);
                for (RunNode parent : ps) parent.addSuccessor(id);
                for (int next : cells.get(col)) {
                    String childId = "n_d" + (row + 1) + "_" + next;
                    List<RunNode> cp = parents.get(childId);
                    if (cp == null) {
                        cp = new ArrayList<RunNode>();
                        parents.put(childId, cp);
                    }
                    if (!cp.contains(n)) cp.add(n);
                }
            }
        }
        ensureShop(byCell, rows, rng);

        RunNode boss = new RunNode("n_d" + (rows + 1) + "_" + mid, NodeType.BOSS, rows + 1, mid);
        boss.setFactionId(bossFaction);
        for (RunNode n : byCell.values()) {
            if (n.getDepth() == rows) n.addSuccessor(boss.getId());
            state.addNode(n);
        }
        state.addNode(boss);
        state.setGridColumns(columns);
        state.setCurrentNodeId(start.getId());
    }

    /** Next column for a path at ({@code row}, {@code col}): one of the three nearest, never crossing. */
    private static int step(Map<Integer, Map<Integer, List<Integer>>> edges, int row, int col, int columns, Random rng) {
        List<Integer> options = new ArrayList<Integer>();
        for (int d = -1; d <= 1; d++) {
            int next = col + d;
            if (next < 0 || next >= columns || crosses(edges.get(row), col, next)) continue;
            options.add(next);
        }
        // Going straight can never cross (paths move one column at most), so this is a safe fallback.
        return options.isEmpty() ? col : options.get(rng.nextInt(options.size()));
    }

    /** Whether edge {@code a -> b} crosses an existing edge between the same two rows. */
    private static boolean crosses(Map<Integer, List<Integer>> row, int a, int b) {
        if (row == null) return false;
        for (Map.Entry<Integer, List<Integer>> e : row.entrySet()) {
            int c = e.getKey();
            for (int d : e.getValue()) {
                if ((a < c && b > d) || (a > c && b < d)) return true;
            }
        }
        return false;
    }

    private static void touch(Map<Integer, Map<Integer, List<Integer>>> edges, int row, int col) {
        Map<Integer, List<Integer>> cells = edges.get(row);
        if (cells == null) {
            cells = new HashMap<Integer, List<Integer>>();
            edges.put(row, cells);
        }
        if (!cells.containsKey(col)) cells.put(col, new ArrayList<Integer>());
    }

    private static void link(Map<Integer, Map<Integer, List<Integer>>> edges, int row, int col, int next) {
        touch(edges, row, col);
        List<Integer> out = edges.get(row).get(col);
        if (!out.contains(next)) out.add(next);
    }

    /** The middle row of the Act, all Treasure. */
    public static int treasureRow(int rows) {
        return Math.max(3, rows / 2);
    }

    private static NodeType typeFor(int row, int rows, List<RunNode> parents, Map<String, NodeType> types,
                                    Weights w, Random rng) {
        if (row <= 2) return NodeType.COMBAT;
        if (row == rows) return NodeType.REST;
        if (row == treasureRow(rows)) return NodeType.TREASURE;
        for (int i = 0; i < MAX_DRAWS; i++) {
            NodeType t = draw(w, rng);
            if (allowed(t, row, rows, parents, types)) return t;
        }
        return NodeType.COMBAT;
    }

    private static NodeType draw(Weights w, Random rng) {
        float total = 0f;
        for (float v : w.byType.values()) total += Math.max(0f, v);
        float roll = rng.nextFloat() * total;
        for (Map.Entry<NodeType, Float> e : w.byType.entrySet()) {
            roll -= Math.max(0f, e.getValue());
            if (roll < 0f) return e.getKey();
        }
        return NodeType.COMBAT;
    }

    private static boolean allowed(NodeType t, int row, int rows, List<RunNode> parents, Map<String, NodeType> types) {
        if (t == NodeType.REST && (row < FIRST_REST_ROW || row == rows - 1)) return false;
        if (t == NodeType.ELITE && row < FIRST_ELITE_ROW) return false;
        if (t == NodeType.AMBUSH && row < FIRST_AMBUSH_ROW) return false;
        for (RunNode p : parents) {
            // An Elite, Shop or Rest never follows the same type.
            if ((t == NodeType.ELITE || t == NodeType.SHOP || t == NodeType.REST) && p.getType() == t) return false;
        }
        // Siblings differ: the Nodes reachable from one parent are distinct choices. Children are
        // linked to their parents as they are created, so the siblings typed so far are known.
        for (RunNode p : parents) {
            for (String sibling : p.getSuccessorIds()) {
                if (types.get(sibling) == t) return false;
            }
        }
        return true;
    }

    /** At least one Shop per Act: turns a drawn Combat Node whose parents aren't Shops into one. */
    private static void ensureShop(Map<String, RunNode> byCell, int rows, Random rng) {
        List<RunNode> candidates = new ArrayList<RunNode>();
        for (RunNode n : byCell.values()) {
            if (n.getType() == NodeType.SHOP) return;
            if (n.getType() == NodeType.COMBAT && n.getDepth() > 2) candidates.add(n);
        }
        if (candidates.isEmpty()) return;
        RunNode old = candidates.get(rng.nextInt(candidates.size()));
        RunNode shop = new RunNode(old.getId(), NodeType.SHOP, old.getDepth(), old.getColumn());
        for (String s : old.getSuccessorIds()) shop.addSuccessor(s);
        byCell.put(old.getId(), shop);
    }

    private static List<RunNode> single(RunNode n) {
        List<RunNode> out = new ArrayList<RunNode>();
        out.add(n);
        return out;
    }
}
