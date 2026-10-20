package com.slaythesector.intel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.run.Ascension;

/**
 * The report of a finished Run, posted as intel with a notification when the Run ends: result,
 * Ascension, Act and Depth reached, Payout, and what the Run unlocked. Plain values only (saved);
 * fades out of the Intel screen after a while.
 */
public class RunResultIntel extends BaseIntelPlugin {

    /** Days the report stays in the Intel screen. */
    private static final float DAYS_SHOWN = 60f;

    private final String archetypeId;
    private final boolean won;
    private final boolean trueBossWon;
    private final int ascension;
    private final int act;
    private final int depth;
    private final int payout;
    private final ArrayList<String> unlocked = new ArrayList<String>();

    public RunResultIntel(String archetypeId, boolean won, boolean trueBossWon, int ascension, int act, int depth,
                          int payout, List<String> unlocked) {
        this.archetypeId = archetypeId;
        this.won = won;
        this.trueBossWon = trueBossWon;
        this.ascension = ascension;
        this.act = act;
        this.depth = depth;
        this.payout = payout;
        if (unlocked != null) this.unlocked.addAll(unlocked);
    }

    /** Posts the report with its notification; it ends on its own after {@link #DAYS_SHOWN} days. */
    public void post() {
        Global.getSector().getIntelManager().addIntel(this, false);
        endAfterDelay(DAYS_SHOWN);
    }

    @Override
    protected String getName() {
        String result = trueBossWon ? "True victory" : won ? "Run won" : "Run lost";
        return "The Descent: " + result;
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        Color h = Misc.getHighlightColor();
        info.addPara("Payout: %s", initPad, tc, h, Misc.getDGSCredits(payout));
        info.addPara("Act %s, Depth %s, Ascension %s", 0f, tc, h, String.valueOf(act), String.valueOf(depth),
                Ascension.name(ascension));
        if (!unlocked.isEmpty()) info.addPara("%s new unlock" + (unlocked.size() > 1 ? "s" : ""), 0f, tc,
                Misc.getPositiveHighlightColor(), String.valueOf(unlocked.size()));
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        FactionAPI f = Global.getSector().getFaction(archetypeId);
        String faction = f == null ? archetypeId : f.getDisplayName();
        if (trueBossWon) {
            info.addPara("You beat the true Boss at the bottom of the Descent. Nothing waits below.",
                    Misc.getPositiveHighlightColor(), 0f);
        } else if (won) {
            info.addPara("You fought your way down all three Acts. The Run is won.", Misc.getPositiveHighlightColor(), 0f);
        } else {
            info.addPara("Your Run ended in Act %s.", 0f, Misc.getNegativeHighlightColor(), h, String.valueOf(act));
        }
        info.addPara("Faction: %s. Ascension: %s. Deepest Node won: Depth %s.", opad, h, faction,
                Ascension.name(ascension), String.valueOf(depth));
        info.addPara("Payout: %s, added to your credits. Your Home fleet is back, exactly as you left it.", opad, h,
                Misc.getDGSCredits(payout));
        if (!unlocked.isEmpty()) {
            info.addPara("Unlocked:", Misc.getPositiveHighlightColor(), opad);
            info.setBulletedListMode(BaseIntelPlugin.BULLET);
            for (String u : unlocked) info.addPara(u, 3f);
            info.setBulletedListMode(null);
        }
        info.addPara("Records and Ascensions are at the Descent Relay.", Misc.getGrayColor(), opad);
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
}
