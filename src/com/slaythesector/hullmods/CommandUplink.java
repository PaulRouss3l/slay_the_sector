package com.slaythesector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/**
 * Built into the flagship of an all-automated Run fleet (hull_mods.csv id sts_command_uplink), which
 * also has its "automated" built-in suppressed (see RunFleet#uplink). Vanilla only lets the player
 * command a ship that isn't automated; without one the fleet counts as destroyed. One berth for
 * the commander.
 */
public class CommandUplink extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyFlat(id, 1f);
        stats.getMaxCrewMod().modifyFlat(id, 1f);
    }
}
