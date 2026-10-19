package com.slaythesector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.slaythesector.run.RunFleet;
import com.slaythesector.util.STSSettings;

/**
 * Hidden, built into every Run fleet ship (also carries the Boss-reward S-mod slots) (hull_mods.csv id sts_run_logistics): no monthly supply
 * use and no passive hull/CR recovery, so Supplies are spent only by the player's choices. Extra
 * cargo space so salvaged weapons don't put the fleet over capacity.
 */
public class RunLogistics extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, 0f);
        stats.getRepairRatePercentPerDay().modifyMult(id, 0f);
        stats.getBaseCRRecoveryRatePercentPerDay().modifyMult(id, 0f);
        stats.getCargoMod().modifyFlat(id, STSSettings.cargoPerShip());
        // Boss reward "Refit": extra S-mod slots, one variant tag each.
        int slots = 0;
        if (stats.getVariant() != null) {
            while (stats.getVariant().hasTag(RunFleet.TAG_SMOD_SLOT + (slots + 1))) slots++;
        }
        if (slots > 0) stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, slots);
    }
}
