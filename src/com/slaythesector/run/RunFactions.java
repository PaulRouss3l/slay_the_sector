package com.slaythesector.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.slaythesector.util.STSConstants;

/**
 * Relations of the mod's two factions: the Descent (Run enemies) hostile to the player, the
 * allied echoes friendly to the player and hostile to the Descent. Other mods can rewrite them
 * (Nexerelin syncs the player's relations from a commissioning faction), so they are re-applied
 * on load, at Run start and before every Run fight.
 */
public final class RunFactions {

    private RunFactions() {}

    public static void apply() {
        FactionAPI descent = Global.getSector().getFaction(STSConstants.FACTION_DESCENT);
        if (descent != null) descent.setRelationship(Factions.PLAYER, RepLevel.VENGEFUL);
        FactionAPI ally = Global.getSector().getFaction(STSConstants.FACTION_ALLY);
        if (ally != null) {
            ally.setRelationship(Factions.PLAYER, RepLevel.COOPERATIVE);
            ally.setRelationship(STSConstants.FACTION_DESCENT, RepLevel.VENGEFUL);
        }
    }
}
