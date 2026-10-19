package com.slaythesector.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.util.IntervalUtil;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunManager;

/**
 * While the player is in the Run system (transient): keeps transverse jump on cooldown, and puts
 * the logistics hullmod on ships that joined outside a fight (e.g. recovered from a wreck), and
 * drops fuel and crew above capacity.
 */
public class RunWatchScript implements EveryFrameScript {

    private final IntervalUtil logisticsCheck = new IntervalUtil(1f, 1f);

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
