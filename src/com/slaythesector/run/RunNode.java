package com.slaythesector.run;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One node in a run's graph. Plain Serializable POJO; stores ids and a fixed
 * position (no SectorEntityToken references) to stay XStream-safe. The matching
 * world entity is created by {@link com.slaythesector.gen.NodeEntityFactory} and
 * resolved lazily via the entity memory key {@code $sts_node_id}.
 */
public class RunNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final NodeType type;
    private final int depth;
    private final List<String> successorIds = new ArrayList<String>();
    private final float x;
    private final float y;
    private boolean cleared;
    /** For combat nodes: the faction the player fights (and whose ships drop as reward). */
    private String factionId;

    public RunNode(String id, NodeType type, int depth, float x, float y) {
        this.id = id;
        this.type = type;
        this.depth = depth;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public NodeType getType() { return type; }
    public int getDepth() { return depth; }
    public List<String> getSuccessorIds() { return successorIds; }
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }

    public String getFactionId() { return factionId; }
    public void setFactionId(String factionId) { this.factionId = factionId; }

    public void addSuccessor(String nodeId) {
        successorIds.add(nodeId);
    }
}
