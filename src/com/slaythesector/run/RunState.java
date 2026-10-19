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
    /** Current Act, 1-based. 0 in saves from before the field existed. */
    private int act = 1;
    /** Ascension level 0 (Gamma) .. 6 (Omega). */
    private int ascension;
    /** Depth of the current Act's Start: Acts are laid out one below the other (global Depth). */
    private int depthOffset;
    /** Fleet DP soft cap added by Boss rewards. */
    private int softCapBonus;
    /** Act 3's Boss beaten: the Run is won whatever happens in Act 4. */
    private boolean act3Won;
    private boolean trueBossWon;
    /** Starting bonuses taken for this Run (Meta progress unlocks). */
    private boolean bonusOfficerLevel;
    /** Width of the map grid Node columns refer to; 0 in older maps, whose columns index their row. */
    private int gridColumns;
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
    public void setAct(int act) { this.act = act; }
    public int getAscension() { return ascension; }
    public void setAscension(int ascension) { this.ascension = ascension; }
    public int getDepthOffset() { return depthOffset; }
    public void setDepthOffset(int depthOffset) { this.depthOffset = depthOffset; }
    public int getSoftCapBonus() { return softCapBonus; }
    public void addSoftCapBonus(int dp) { softCapBonus += dp; }
    public boolean isAct3Won() { return act3Won; }
    public void setAct3Won() { act3Won = true; }
    public boolean isTrueBossWon() { return trueBossWon; }
    public void setTrueBossWon() { trueBossWon = true; }
    public boolean isBonusOfficerLevel() { return bonusOfficerLevel; }
    public void setBonusOfficerLevel(boolean on) { bonusOfficerLevel = on; }

    /** The last Act of this Run: 3, or 4 (the true Boss) at Ascension Omega. */
    public int getFinalAct() { return ascension >= Ascension.OMEGA ? 4 : 3; }

    /** Depth across the whole Run (Acts stacked). */
    public int getGlobalDepth(RunNode n) { return depthOffset + n.getDepth(); }

    /** Drops the current Act's map, keeping everything else. */
    public void clearNodes() {
        nodes.clear();
        currentNodeId = null;
        targetNodeId = null;
    }
    public int getGridColumns() { return gridColumns; }
    public void setGridColumns(int gridColumns) { this.gridColumns = gridColumns; }
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
        maxDepthReached = Math.max(maxDepthReached, getGlobalDepth(target));
        if (won) maxDepthWon = Math.max(maxDepthWon, getGlobalDepth(target));
    }
}
