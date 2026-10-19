package com.slaythesector.spike;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.slaythesector.util.STSLog;

/**
 * Spike: swap the whole player fleet object (vanilla StandardRespawnDialogPluginImpl uses
 * {@code setPlayerFleet}). The Home fleet keeps its ships, officers, cargo and credits and is held
 * detached in sector memory; the Run fleet starts with its own empty cargo.
 */
public final class SpikeFleetSwap {

    private SpikeFleetSwap() {}

    public static boolean isSwapped() {
        return mem().get(SpikeIds.MEM_HOME_FLEET) instanceof CampaignFleetAPI;
    }

    public static void stashHomeFleet() {
        if (isSwapped()) return;
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI home = sector.getPlayerFleet();
        LocationAPI loc = home.getContainingLocation();
        Vector2f pos = new Vector2f(home.getLocation());

        CampaignFleetAPI run = createRunFleet();
        mem().set(SpikeIds.MEM_HOME_FLEET, home);
        setPlayerFleet(run, loc, pos);
        loc.removeEntity(home);
        log("stash", home, run);
    }

    /** Puts the Home fleet back at the current player position and discards the Run fleet. */
    public static void restoreHomeFleet() {
        if (!isSwapped()) return;
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI run = sector.getPlayerFleet();
        CampaignFleetAPI home = (CampaignFleetAPI) mem().get(SpikeIds.MEM_HOME_FLEET);
        LocationAPI loc = run.getContainingLocation();
        Vector2f pos = new Vector2f(run.getLocation());

        setPlayerFleet(home, loc, pos);
        if (run.getContainingLocation() != null) run.getContainingLocation().removeEntity(run);
        mem().unset(SpikeIds.MEM_HOME_FLEET);
        log("restore", home, run);
    }

    private static CampaignFleetAPI createRunFleet() {
        PersonAPI player = Global.getSector().getPlayerPerson();
        CampaignFleetAPI run = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, null, null);
        run.setName("Run Fleet");
        run.setCommander(player);

        FleetMemberAPI flagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, SpikeIds.STARTER_VARIANT);
        FleetMemberAPI escort = Global.getFactory().createFleetMember(FleetMemberType.SHIP, SpikeIds.SECOND_VARIANT);
        run.getFleetData().addFleetMember(flagship);
        run.getFleetData().addFleetMember(escort);
        flagship.setCaptain(player);
        run.getFleetData().setFlagship(flagship);

        for (FleetMemberAPI m : run.getFleetData().getMembersListCopy()) {
            m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        }
        run.getCargo().addCrew((int) Math.ceil(run.getFleetData().getMinCrew()) + 20);
        run.getCargo().addSupplies(100f);
        run.getCargo().addFuel(100f);

        for (String id : Global.getSector().getCharacterData().getAbilities()) {
            run.addAbility(id);
        }
        run.getFleetData().setSyncNeeded();
        run.getFleetData().syncIfNeeded();
        return run;
    }

    private static void setPlayerFleet(CampaignFleetAPI fleet, LocationAPI loc, Vector2f pos) {
        Global.getSector().setPlayerFleet(fleet);
        if (fleet.getContainingLocation() == null) {
            loc.addEntity(fleet);
        }
        fleet.setLocation(pos.x, pos.y);
        Global.getSector().getCampaignUI().resetViewOffset();
    }

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }

    private static void log(String what, CampaignFleetAPI home, CampaignFleetAPI run) {
        STSLog.info("spike: " + what + " — home=" + home.getFleetData().getNumMembers() + " ships, "
                + (int) home.getCargo().getCredits().get() + " cr, officers=" + home.getFleetData().getOfficersCopy().size()
                + ", homeLoc=" + home.getContainingLocation()
                + " | run=" + run.getFleetData().getNumMembers() + " ships, runLoc=" + run.getContainingLocation()
                + " | player=" + (Global.getSector().getPlayerFleet() == home ? "home" : "run"));
    }
}
