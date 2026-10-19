package com.slaythesector.run;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSLog;
import com.slaythesector.world.RunSystem;

/**
 * Runs once after a save loads (transient): a stashed Home fleet without an active Run is
 * restored automatically, so a bug can never strand the player's real fleet.
 */
public class SafetyNetScript implements EveryFrameScript {

    private boolean done;

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (done || Global.getSector().getCampaignUI().isShowingDialog()) return;
        done = true;
        if (RunManager.isActive() || !HomeFleet.isStashed()) return;

        STSLog.warn("Home fleet stashed with no active Run: restoring");
        CampaignFleetAPI home = HomeFleet.restore();
        Global.getSector().getCampaignUI().addMessage("Slay the Sector: your Home fleet was recovered.", Misc.getHighlightColor());
        if (RunSystem.isRunSystem(home.getContainingLocation())) {
            RunManager.returnToHub();
        }
    }
}
