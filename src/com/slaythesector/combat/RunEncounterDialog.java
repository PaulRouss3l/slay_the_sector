package com.slaythesector.combat;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.slaythesector.run.RunFleet;
import com.slaythesector.util.STSLog;

/**
 * The vanilla fleet encounter dialog, used only for Run fights (created by NodeDialog; every other
 * encounter in the campaign keeps the vanilla dialog). With {@code finishOffSurvivors}, a battle
 * the player won counts as a total victory: enemy ships still on the field (retreated, in reserve,
 * or left when the player claimed victory) are counted as lost before vanilla scores the battle,
 * so it goes straight to loot and recovery instead of offering a re-engagement.
 */
public class RunEncounterDialog extends FleetInteractionDialogPluginImpl {

    private final boolean finishOffSurvivors;

    public RunEncounterDialog(FIDConfig config, boolean finishOffSurvivors) {
        super(config);
        this.finishOffSurvivors = finishOffSurvivors;
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
        if (finishOffSurvivors && result != null && result.didPlayerWin()) {
            EngagementResultForFleetAPI winner = result.getWinnerResult();
            EngagementResultForFleetAPI loser = result.getLoserResult();
            // Only a straight fight: in an escape or a pursuit, getting away is the point.
            if (winner != null && loser != null && winner.getGoal() == FleetGoal.ATTACK && loser.getGoal() == FleetGoal.ATTACK) {
                try {
                    List<FleetMemberAPI> survivors = new ArrayList<FleetMemberAPI>();
                    survivors.addAll(loser.getDeployed());
                    survivors.addAll(loser.getRetreated());
                    survivors.addAll(loser.getReserves());
                    loser.getDeployed().clear();
                    loser.getRetreated().clear();
                    loser.getReserves().clear();
                    for (FleetMemberAPI m : survivors) {
                        if (!loser.getDestroyed().contains(m) && !loser.getDisabled().contains(m)) loser.getDestroyed().add(m);
                    }
                } catch (UnsupportedOperationException e) {
                    // Read-only result lists: fall back to the vanilla flow (survivors can re-engage).
                    STSLog.warn("could not finish off survivors: " + e);
                }
            }
        }
        // Vanilla scores the battle a total defeat when no ship that isn't automated survives:
        // an all-drone Run fleet that lost its uplinked flagship moves the uplink first.
        EngagementResultForFleetAPI own = result == null ? null : result.didPlayerWin() ? result.getWinnerResult() : result.getLoserResult();
        if (own != null) {
            List<FleetMemberAPI> lost = new ArrayList<FleetMemberAPI>(own.getDestroyed());
            lost.addAll(own.getDisabled());
            RunFleet.ensureCommandShip(Global.getSector().getPlayerFleet(), lost);
        }
        super.backFromEngagement(result);
    }
}
