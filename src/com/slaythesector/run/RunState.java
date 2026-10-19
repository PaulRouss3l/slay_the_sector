package com.slaythesector.run;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The active Run, stored in sector memory under {@code $sts_run_state}. Plain Serializable POJO
 * holding ids only. The detached Home fleet is kept separately (see {@link HomeFleet}).
 */
public class RunState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String archetypeId;
    /** Current Act, 1-based (one Act until S3). 0 in saves from before the field existed. */
    private int act = 1;
    private final Map<String, RunNode> nodes = new LinkedHashMap<String, RunNode>();
    private final List<String> enemyFactionIds = new ArrayList<String>();
    /** Last Node the player resolved (or START). Its successors are the next choices. */
    private String currentNodeId;
    /** Node the player has set course for but not resolved yet; null when choosing. */
    private String targetNodeId;
    private int maxDepthReached;
    /** Deepest Node won; the Payout scales on this so withdrawing can't farm Depth. */
    private int maxDepthWon;
    /** Campaign values the Run must not change; restored at Run end (zero campaign impact). */
    private long snapXp;
    private long snapBonusXp;
    private int snapLevel;
    private int snapSkillPoints;
    private int snapStoryPoints;
    private float snapMarines;
    private float snapMarineXp;
    private long snapDeferredBonusXp;
    /** Run-fleet pay minus the Home fleet's pay since the Run started; refunded at the end (RunPayrollListener). */
    private float payrollRefund;
    /** Player skill id -> level at Run start; skills learned during the Run are removed at the end. */
    private final HashMap<String, Float> snapSkills = new HashMap<String, Float>();

    public RunState(String archetypeId) {
        this.archetypeId = archetypeId;
    }

    public String getArchetypeId() { return archetypeId; }
    public int getAct() { return Math.max(1, act); }
    public Collection<RunNode> getNodes() { return nodes.values(); }

    /** Depth of the Boss: the deepest Node of this Run's map (S1 maps were one row shorter). */
    public int getBossDepth() {
        int max = 1;
        for (RunNode n : nodes.values()) max = Math.max(max, n.getDepth());
        return max;
    }
    public List<String> getEnemyFactionIds() { return enemyFactionIds; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String id) { this.currentNodeId = id; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String id) { this.targetNodeId = id; }
    public int getMaxDepthReached() { return maxDepthReached; }
    public int getMaxDepthWon() { return maxDepthWon; }

    public void snapshotCampaign(long xp, long bonusXp, int level, int skillPoints, int storyPoints,
                                 float marines, float marineXp) {
        snapXp = xp;
        snapBonusXp = bonusXp;
        snapLevel = level;
        snapSkillPoints = skillPoints;
        snapStoryPoints = storyPoints;
        snapMarines = marines;
        snapMarineXp = marineXp;
    }

    public long getSnapXp() { return snapXp; }
    public long getSnapBonusXp() { return snapBonusXp; }
    public int getSnapLevel() { return snapLevel; }
    public int getSnapSkillPoints() { return snapSkillPoints; }
    public int getSnapStoryPoints() { return snapStoryPoints; }
    public float getSnapMarines() { return snapMarines; }
    public float getSnapMarineXp() { return snapMarineXp; }
    public long getSnapDeferredBonusXp() { return snapDeferredBonusXp; }
    public void setSnapDeferredBonusXp(long xp) { snapDeferredBonusXp = xp; }
    public Map<String, Float> getSnapSkills() { return snapSkills; }
    public float getPayrollRefund() { return payrollRefund; }
    public void addPayrollRefund(float credits) { payrollRefund += credits; }

    public void addNode(RunNode node) {
        nodes.put(node.getId(), node);
    }

    public RunNode getNode(String id) {
        return id == null ? null : nodes.get(id);
    }

    public RunNode getCurrentNode() {
        return getNode(currentNodeId);
    }

    public RunNode getTargetNode() {
        return getNode(targetNodeId);
    }

    /** Successors of the current Node: the only Nodes the player may choose (Path lock). */
    public List<RunNode> getChoices() {
        List<RunNode> out = new ArrayList<RunNode>();
        RunNode cur = getCurrentNode();
        if (cur == null) return out;
        for (String id : cur.getSuccessorIds()) {
            RunNode n = nodes.get(id);
            if (n != null) out.add(n);
        }
        return out;
    }

    /** Marks the target Node resolved and makes it the current Node. */
    public void resolveTarget(boolean won) {
        RunNode target = getTargetNode();
        if (target == null) return;
        target.resolve(won);
        currentNodeId = target.getId();
        targetNodeId = null;
        maxDepthReached = Math.max(maxDepthReached, target.getDepth());
        if (won) maxDepthWon = Math.max(maxDepthWon, target.getDepth());
    }
}
