package com.slaythesector.run;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One Node of a Run's map. Plain Serializable POJO holding ids only; its world entity is
 * found by scanning for entity memory {@code $sts_node_id}.
 */
public class RunNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final NodeType type;
    private final int depth;
    /** Position in its row, used for map layout. */
    private final int column;
    private final List<String> successorIds = new ArrayList<String>();
    /** Enemy faction for fight Nodes. */
    private String factionId;
    private boolean resolved;
    private boolean won;
    /** Shop/Rest/Treasure: the service was used (one visit per Node). */
    private boolean claimed;

    public RunNode(String id, NodeType type, int depth, int column) {
        this.id = id;
        this.type = type;
        this.depth = depth;
        this.column = column;
    }

    public String getId() { return id; }
    public NodeType getType() { return type; }
    public int getDepth() { return depth; }
    public int getColumn() { return column; }
    public List<String> getSuccessorIds() { return successorIds; }
    public String getFactionId() { return factionId; }
    public void setFactionId(String factionId) { this.factionId = factionId; }
    public boolean isResolved() { return resolved; }
    public boolean isWon() { return won; }
    public boolean isClaimed() { return claimed; }
    public void claim() { claimed = true; }

    public void addSuccessor(String nodeId) {
        if (!successorIds.contains(nodeId)) successorIds.add(nodeId);
    }

    public void resolve(boolean won) {
        this.resolved = true;
        this.won = won;
    }
}
