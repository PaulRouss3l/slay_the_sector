package com.slaythesector.run;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;

/**
 * Swaps the whole player fleet object (vanilla respawn does the same with
 * {@code setPlayerFleet}). While a Run is out, the Home fleet (ships, officers, cargo, credits)
 * is held detached in sector memory. Verified in game by the Spike.
 */
public final class HomeFleet {

    private HomeFleet() {}

    public static boolean isStashed() {
        return mem().get(STSConstants.MEM_HOME_FLEET) instanceof CampaignFleetAPI;
    }

    /** Detaches the current player fleet and makes {@code runFleet} the player fleet in its place. */
    public static boolean stash(CampaignFleetAPI runFleet) {
        if (isStashed()) {
            STSLog.warn("home fleet already stashed; refusing to stash twice");
            return false;
        }
        CampaignFleetAPI home = Global.getSector().getPlayerFleet();
        LocationAPI loc = home.getContainingLocation();
        Vector2f pos = new Vector2f(home.getLocation());

        mem().set(STSConstants.MEM_HOME_FLEET, home);
        becomePlayerFleet(runFleet, loc, pos);
        loc.removeEntity(home);
        STSLog.info("home fleet stashed: " + home.getFleetData().getNumMembers() + " ships, "
                + (int) home.getCargo().getCredits().get() + " cr");
        return true;
    }

    /** Puts the Home fleet back where the player fleet is now; the Run fleet is discarded. */
    public static CampaignFleetAPI restore() {
        if (!isStashed()) return Global.getSector().getPlayerFleet();
        CampaignFleetAPI run = Global.getSector().getPlayerFleet();
        CampaignFleetAPI home = (CampaignFleetAPI) mem().get(STSConstants.MEM_HOME_FLEET);
        LocationAPI loc = run.getContainingLocation();
        Vector2f pos = new Vector2f(run.getLocation());

        // Monthly income settles on the player fleet's credits: hand it back. A negative balance
        // is Run-fleet payroll (Run officers and crew) and must not be charged to the Home fleet.
        home.getCargo().getCredits().add(Math.max(0f, run.getCargo().getCredits().get()));
        becomePlayerFleet(home, loc, pos);
        if (loc != null && loc.getFleets().contains(run)) loc.removeEntity(run);
        mem().unset(STSConstants.MEM_HOME_FLEET);
        STSLog.info("home fleet restored: " + home.getFleetData().getNumMembers() + " ships");
        return home;
    }

    private static void becomePlayerFleet(CampaignFleetAPI fleet, LocationAPI loc, Vector2f pos) {
        Global.getSector().setPlayerFleet(fleet);
        // getContainingLocation() stays set after removeEntity(), so test real membership (Spike finding).
        if (loc != null && !loc.getFleets().contains(fleet)) {
            loc.addEntity(fleet);
        }
        fleet.setLocation(pos.x, pos.y);
        Global.getSector().getCampaignUI().resetViewOffset();
    }

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }
}
