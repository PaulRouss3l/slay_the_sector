package com.slaythesector.run;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-run state, stored in sector memory under {@link com.slaythesector.util.STSConstants#MEM_RUN_STATE}.
 * Lives only for the duration of a run; cleared on exit. Plain Serializable POJO
 * holding ids only (no SectorEntityToken references) to stay XStream-safe.
 */
public class RunState implements Serializable {

    private static final long serialVersionUID = 2L;

    /** Id of the generated run {@code StarSystemAPI}. */
    private String runSystemId;

    /** True while the player is inside an active run. */
    private boolean active;

    /** Node graph, keyed by node id, in generation order. */
    private final Map<String, RunNode> nodes = new LinkedHashMap<String, RunNode>();

    private String startNodeId;

    /** Node the player is currently at (set on arrival). */
    private String currentNodeId;

    /** Nodes whose entities have been spawned (fog of war: only these are reachable). */
    private final Set<String> revealedNodeIds = new HashSet<String>();

    /** Seed used to build the node graph (kept for potential regen). */
    private long seed;

    public RunState() {}

    public String getRunSystemId() { return runSystemId; }
    public void setRunSystemId(String runSystemId) { this.runSystemId = runSystemId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, RunNode> getNodes() { return nodes; }
    public RunNode getNode(String id) { return id == null ? null : nodes.get(id); }
    public void addNode(RunNode node) { nodes.put(node.getId(), node); }

    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public Set<String> getRevealedNodeIds() { return revealedNodeIds; }
    public boolean isRevealed(String id) { return revealedNodeIds.contains(id); }
    public void reveal(String id) { revealedNodeIds.add(id); }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
}
