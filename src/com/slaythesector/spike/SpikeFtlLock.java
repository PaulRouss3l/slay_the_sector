package com.slaythesector.spike;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;

/** Spike: keeps transverse jump on cooldown while the player is in the Run system. Transient. */
public class SpikeFtlLock implements EveryFrameScript {

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
        if (pf == null || !SpikeWorld.isRunSystem(pf.getContainingLocation())) return;
        AbilityPlugin jump = pf.getAbility(Abilities.TRANSVERSE_JUMP);
        if (jump != null && jump.getCooldownLeft() < 0.5f) {
            jump.setCooldownLeft(1f);
        }
    }
}
