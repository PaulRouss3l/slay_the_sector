package com.slaythesector.run;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.loading.VariantSource;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSSettings;

/** Builds the Run fleet and applies the Run's attrition rules to it. */
public final class RunFleet {

    private RunFleet() {}

    /** A new player-faction fleet with copies of the picked ships, 1 officer and starting Supplies. */
    public static CampaignFleetAPI create(List<FleetMemberAPI> picks) {
        PersonAPI player = Global.getSector().getPlayerPerson();
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, null, null);
        fleet.setName("Run Fleet");
        fleet.setCommander(player);

        FleetMemberAPI flagship = null;
        for (FleetMemberAPI pick : picks) {
            FleetMemberAPI m = addShip(fleet, pick.getVariant());
            if (flagship == null || m.getDeploymentPointsCost() > flagship.getDeploymentPointsCost()) flagship = m;
        }
        if (flagship != null) {
            flagship.setCaptain(player);
            fleet.getFleetData().setFlagship(flagship);
        }
        PersonAPI officer = OfficerManagerEvent.createOfficer(Global.getSector().getPlayerFaction(), 1);
        fleet.getFleetData().addOfficer(officer);

        for (String id : Global.getSector().getCharacterData().getAbilities()) {
            fleet.addAbility(id);
        }
        if (fleet.getAbility(Abilities.TRANSPONDER) != null) fleet.getAbility(Abilities.TRANSPONDER).activate();
        fleet.getCargo().addSupplies(STSSettings.startingSupplies());
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        }
        topUp(fleet);
        return fleet;
    }

    /** Adds a ship built from a copy of {@code variant}, with the Run logistics hullmod built in. */
    public static FleetMemberAPI addShip(CampaignFleetAPI fleet, ShipVariantAPI variant) {
        ShipVariantAPI copy = variant.clone();
        copy.setSource(VariantSource.REFIT);
        FleetMemberAPI m = Global.getFactory().createFleetMember(FleetMemberType.SHIP, copy);
        fleet.getFleetData().addFleetMember(m);
        m.setShipName(fleet.getFleetData().pickShipName(m, null));
        ensureLogistics(m);
        m.getStatus().repairFully();
        m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        return m;
    }

    /** After every fight: free CR up to the regen fraction, crew and fuel refilled, logistics hullmod on recovered ships. */
    public static void afterFight(CampaignFleetAPI fleet) {
        float fraction = STSSettings.crRegenFraction();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            ensureLogistics(m);
            float target = m.getRepairTracker().getMaxCR() * fraction;
            if (m.getRepairTracker().getCR() < target) m.getRepairTracker().setCR(target);
        }
        topUp(fleet);
    }

    /** Crew to cover every ship plus a margin, fuel to capacity. Neither is a Run resource. */
    public static void topUp(CampaignFleetAPI fleet) {
        CargoAPI cargo = fleet.getCargo();
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        int maxCrew = (int) cargo.getMaxPersonnel();
        int wantCrew = Math.min((int) Math.ceil(fleet.getFleetData().getMinCrew() * 1.25f) + 10, maxCrew);
        int crew = cargo.getCrew();
        if (crew < wantCrew) cargo.addCrew(wantCrew - crew);
        float fuelSpace = cargo.getMaxFuel() - cargo.getFuel();
        if (fuelSpace > 0) cargo.addFuel(fuelSpace);
        trimOverCapacity(fleet);
    }

    /**
     * Drops fuel and crew above capacity: neither is a Run resource, and going over capacity
     * would cost Supplies while flying.
     */
    public static void trimOverCapacity(CampaignFleetAPI fleet) {
        CargoAPI cargo = fleet.getCargo();
        float extraFuel = cargo.getFuel() - cargo.getMaxFuel();
        if (extraFuel > 0) cargo.removeFuel(extraFuel);
        int extraCrew = cargo.getCrew() - Math.max((int) cargo.getMaxPersonnel(), (int) Math.ceil(fleet.getFleetData().getMinCrew()));
        if (extraCrew > 0) cargo.removeCrew(extraCrew);
    }

    /** Built-in hidden hullmod: no monthly supply use and no passive repair/CR recovery. */
    public static void ensureLogistics(FleetMemberAPI m) {
        ShipVariantAPI v = m.getVariant();
        if (v.hasHullMod(STSConstants.HULLMOD_RUN_LOGISTICS)) return;
        if (v.getSource() != VariantSource.REFIT) {
            v = v.clone();
            v.setSource(VariantSource.REFIT);
            m.setVariant(v, false, true);
        }
        v.addPermaMod(STSConstants.HULLMOD_RUN_LOGISTICS);
        m.updateStats();
    }

    /** Hull + CR repair cost in Supplies for one ship (0 if nothing to repair). */
    public static int repairCost(FleetMemberAPI m) {
        float hullMissing = 1f - m.getStatus().getHullFraction();
        float max = m.getRepairTracker().getMaxCR();
        float crMissing = max <= 0f ? 0f : Math.max(0f, max - m.getRepairTracker().getCR()) / max;
        float perFull = m.getHullSpec().getSuppliesToRecover();
        return Math.round((hullMissing + crMissing * 0.5f) * perFull * STSSettings.repairCostMult());
    }

    public static void repair(FleetMemberAPI m) {
        m.getStatus().repairFully();
        m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
    }

    public static int sellValue(FleetMemberAPI m) {
        return Math.round(m.getDeploymentPointsCost() * STSSettings.sellSuppliesPerDP());
    }
}
