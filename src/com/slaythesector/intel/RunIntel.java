package com.slaythesector.intel;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.map.NodeText;
import com.slaythesector.run.FleetCap;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.NodeMapPanel;

/**
 * Read-only overview of the active Run: summary in the list, the node map as the large
 * description. One permanent instance per save, hidden while no Run is active; holds no state.
 */
public class RunIntel extends BaseIntelPlugin {

    public RunIntel() {
        setImportant(true);
    }

    /** Adds the entry once per save. */
    public static void ensure() {
        if (Global.getSector().getIntelManager().hasIntelOfClass(RunIntel.class)) return;
        Global.getSector().getIntelManager().addIntel(new RunIntel(), true);
    }

    @Override
    public boolean isHidden() {
        return !RunManager.isActive();
    }

    @Override
    protected String getName() {
        return "The Descent: current Run";
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        RunState state = RunManager.get();
        if (state == null) return;
        RunNode next = state.getTargetNode();
        info.addPara("Act %s, deepest Node won: %s", initPad, tc, Misc.getHighlightColor(),
                String.valueOf(state.getAct()), String.valueOf(state.getMaxDepthWon()));
        if (next != null) info.addPara("Course: %s", 0f, tc, Misc.getHighlightColor(), NodeText.full(next));
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        RunState state = RunManager.get();
        if (state == null) return;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        FactionAPI archetype = Global.getSector().getFaction(state.getArchetypeId());
        info.addPara("Archetype: %s. Act %s.", 0f, h,
                archetype == null ? state.getArchetypeId() : archetype.getDisplayName(), String.valueOf(state.getAct()));
        info.addPara("Deepest Node won: %s. Supplies: %s.", opad, h,
                String.valueOf(state.getMaxDepthWon()), String.valueOf((int) pf.getCargo().getSupplies()));
        info.addPara(FleetCap.summary(pf, state) + ".", opad);
        String capWarning = FleetCap.warning(pf, state);
        if (capWarning != null) info.addPara(capWarning, Misc.getNegativeHighlightColor(), opad);
        RunNode next = state.getTargetNode();
        if (next != null) info.addPara("Course locked: %s.", opad, h, NodeText.full(next));
        info.addPara("Courses are chosen on the node map at each beacon.", Misc.getGrayColor(), opad);
    }

    @Override
    public boolean hasLargeDescription() {
        return RunManager.isActive();
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        RunState state = RunManager.get();
        if (state == null) return;
        TooltipMakerAPI title = panel.createUIElement(width, 30f, false);
        title.addTitle("The Descent: Act " + state.getAct());
        panel.addUIElement(title).inTL(0f, 0f);
        CustomPanelAPI map = panel.createCustomPanel(width, height - 40f, new NodeMapPanel(state, false, null));
        panel.addComponent(map).inTL(0f, 40f);
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "sts_hub");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(HubIntel.TAG);
        return tags;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }
}
