package com.slaythesector.intel;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.hub.HubManager;
import com.slaythesector.run.MetaProgressManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.run.RunStateManager;

/**
 * Persistent "Sector Descent" intel. Always present (added once at game load):
 * <ul>
 *   <li>Idle (no run): points to the Sector Waystation and shows meta stats.</li>
 *   <li>In a run: shows current depth, node, the node(s) ahead, and progress.</li>
 * </ul>
 */
public class RunIntelPlugin extends BaseIntelPlugin {

    /** Adds the intel once if it isn't already present. Call on game load. */
    public static void ensure() {
        IntelManagerAPI mgr = Global.getSector().getIntelManager();
        if (mgr.getFirstIntel(RunIntelPlugin.class) == null) {
            mgr.addIntel(new RunIntelPlugin());
        }
    }

    private static boolean inRun() {
        RunState st = RunStateManager.get();
        return st != null && st.isActive();
    }

    @Override
    protected String getName() {
        return "Sector Descent";
    }

    @Override
    public String getIcon() {
        return Global.getSector().getPlayerFaction().getCrest();
    }

    @Override
    public String getSortString() {
        return "Sector Descent";
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new HashSet<String>();
        tags.add(Tags.INTEL_IMPORTANT);
        return tags;
    }

    /** Persistent: never auto-removed. */
    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isEnding() {
        return false;
    }

    /** Map marker: the hub world (the run itself happens inside a dialog there). */
    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return HubManager.findHub();
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode), 0f);
        if (inRun()) {
            RunState st = RunStateManager.get();
            RunNode cur = st.getNode(st.getCurrentNodeId());
            int depth = cur == null ? 0 : cur.getDepth();
            info.addPara("Descending - depth " + depth, Misc.getGrayColor(), 3f);
        } else {
            info.addPara("Travel to the Sector Waystation to begin", Misc.getGrayColor(), 3f);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color hl = Misc.getHighlightColor();
        if (inRun()) {
            describeRun(info, hl);
        } else {
            describeIdle(info, hl);
        }
    }

    private void describeIdle(TooltipMakerAPI info, Color hl) {
        SectorEntityToken hub = HubManager.findHub();
        String name = hub != null ? hub.getName() : "Jangala";
        String where = (hub != null && hub.getContainingLocation() != null)
                ? hub.getContainingLocation().getName() : "Corvus";
        info.addSectionHeading("Begin a Descent", Alignment.MID, 5f);
        info.addPara("Travel to %s in %s and interact with it, then choose \"Descend into the Sector\".",
                10f, hl, name, where);

        info.addSectionHeading("Record", Alignment.MID, 10f);
        info.addPara("Runs won: %s", 10f, hl, String.valueOf(MetaProgressManager.get().getRunsWon()));
        info.addPara("Best depth: %s", 5f, hl, String.valueOf(MetaProgressManager.get().getBestDepth()));
        info.addPara("Ships unlocked: %s", 5f, hl,
                String.valueOf(MetaProgressManager.get().getUnlockedShipVariants().size()));
    }

    private void describeRun(TooltipMakerAPI info, Color hl) {
        RunState st = RunStateManager.get();
        RunNode cur = st.getNode(st.getCurrentNodeId());

        info.addSectionHeading("Descent", Alignment.MID, 5f);
        info.addPara("Current depth: %s", 10f, hl, String.valueOf(cur == null ? 0 : cur.getDepth()));
        info.addPara("Current node: %s", 5f, hl, cur == null ? "-" : cur.getType().label());
        info.addPara("Nodes cleared: %s", 5f, hl, String.valueOf(clearedCount(st)));

        info.addSectionHeading("Ahead", Alignment.MID, 10f);
        if (cur == null || cur.getSuccessorIds().isEmpty()) {
            info.addPara("Nothing but the dark.", 10f);
        } else {
            for (String succId : cur.getSuccessorIds()) {
                RunNode succ = st.getNode(succId);
                if (succ != null) {
                    info.addPara("- " + succ.getType().label(), 5f);
                }
            }
        }
    }

    private int clearedCount(RunState st) {
        int n = 0;
        for (RunNode node : st.getNodes().values()) {
            if (node.isCleared()) {
                n++;
            }
        }
        return n;
    }
}
