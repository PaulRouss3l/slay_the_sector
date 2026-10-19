package com.slaythesector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/**
 * Hidden, built into every Run fleet ship (hull_mods.csv id sts_run_logistics): no monthly supply
 * use and no passive hull/CR recovery, so Supplies are spent only by the player's choices.
 */
public class RunLogistics extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, 0f);
        stats.getRepairRatePercentPerDay().modifyMult(id, 0f);
        stats.getBaseCRRecoveryRatePercentPerDay().modifyMult(id, 0f);
    }
}
