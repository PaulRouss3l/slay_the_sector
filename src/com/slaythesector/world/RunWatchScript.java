package com.slaythesector.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.util.IntervalUtil;
import com.slaythesector.run.HomeFleet;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunState;
import com.slaythesector.util.STSLog;

/**
 * While the player is in the Run system (transient): keeps transverse jump on cooldown, and puts
 * the logistics hullmod on ships that joined outside a fight (e.g. recovered from a wreck), and
 * drops fuel and crew above capacity.
 */
public class RunWatchScript implements EveryFrameScript {

    private final IntervalUtil logisticsCheck = new IntervalUtil(1f, 1f);
    /** The Run fleet and its credits as last seen (transient script; reset whenever the fleet changes). */
    private CampaignFleetAPI watchedFleet;
    private float lastCredits;

    /**
     * During a Run, any change to the Run fleet's credits other than the monthly report is
     * "foreign" (another mod paying into the player fleet) and is kept out of what goes home.
     */
    private void trackCredits(CampaignFleetAPI pf) {
        RunState state = RunManager.get();
        if (pf == null || state == null || !HomeFleet.isStashed()) {
            watchedFleet = null;
            return;
        }
        float credits = pf.getCargo().getCredits().get();
        if (pf != watchedFleet) {
            watchedFleet = pf;
            lastCredits = credits;
            return;
        }
        if (credits == lastCredits) return;
        float diff = credits - lastCredits;
        lastCredits = credits;
        if (state.consumeMonthEnd()) return;
        state.addForeignCredits(diff);
        STSLog.warn("credits paid to the Run fleet outside the monthly report (" + Math.round(diff)
                + "): not carried home");
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        trackCredits(pf);
        if (pf == null || !RunSystem.isRunSystem(pf.getContainingLocation())) return;
        AbilityPlugin jump = pf.getAbility(Abilities.TRANSVERSE_JUMP);
        if (jump != null && jump.getCooldownLeft() < 0.5f) {
            jump.setCooldownLeft(1f);
        }
        logisticsCheck.advance(amount);
        if (logisticsCheck.intervalElapsed() && RunManager.isActive()) {
            for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) {
                RunFleet.ensureLogistics(m);
            }
            RunFleet.trimOverCapacity(pf);
        }
    }
}
