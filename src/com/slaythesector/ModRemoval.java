package com.slaythesector;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.run.HomeFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.world.RunSystem;

/**
 * Strips what the mod keeps in a save, so the save loads once the mod is disabled (ADR 0002):
 * our intel, the Descent Relay and the Descent, fleets under our factions, our memory keys.
 * Runs on game load when {@code sts_prepareRemoval} is true, or with the console's {@code sts remove}.
 * The factions sts_descent and sts_ally stay: the API cannot remove a faction.
 */
public final class ModRemoval {

    private ModRemoval() {}

    /** Why removal can't run now, or null when it can. */
    public static String blocker() {
        if (RunManager.isActive()) return "a Run is in progress: finish or abandon it first";
        if (HomeFleet.isStashed()) return "your Home fleet is still stashed: load the save once without removal to recover it";
        return null;
    }

    /** Removes everything; returns one line per kind of thing removed. Check {@link #blocker()} first. */
    public static List<String> run() {
        SectorAPI sector = Global.getSector();
        List<String> done = new ArrayList<String>();

        int intel = 0;
        for (IntelInfoPlugin i : new ArrayList<IntelInfoPlugin>(sector.getIntelManager().getIntel())) {
            if (!isOurs(i)) continue;
            sector.getIntelManager().removeIntel(i);
            intel++;
        }
        done.add(intel + " intel entries");

        StarSystemAPI descent = RunSystem.get();
        int entities = 0;
        int fleets = 0;
        for (LocationAPI loc : sector.getAllLocations()) {
            if (loc == descent) continue;
            for (SectorEntityToken e : new ArrayList<SectorEntityToken>(loc.getAllEntities())) {
                String type = e.getCustomEntityType();
                if (type == null || !type.startsWith("sts_")) continue;
                loc.removeEntity(e);
                entities++;
            }
            for (CampaignFleetAPI f : new ArrayList<CampaignFleetAPI>(loc.getFleets())) {
                if (f.isPlayerFleet() || !isOurFaction(f.getFaction().getId())) continue;
                loc.removeEntity(f);
                fleets++;
            }
        }
        done.add(entities + " stations (Descent Relay)");
        done.add(fleets + " stray Run fleets");
        if (descent != null) {
            sector.removeStarSystemNextFrame(descent);
            done.add("the Descent star system");
        }

        int keys = unsetOurKeys(sector.getMemoryWithoutUpdate()) + unsetOurKeys(sector.getPlayerMemoryWithoutUpdate());
        done.add(keys + " memory keys");

        sector.unregisterPlugin(STSConstants.CAMPAIGN_PLUGIN_ID);
        STSLog.info("prepared for removal: " + done);
        return done;
    }

    private static boolean isOurs(Object o) {
        return o.getClass().getName().startsWith("com.slaythesector.");
    }

    private static boolean isOurFaction(String id) {
        return STSConstants.FACTION_DESCENT.equals(id) || STSConstants.FACTION_ALLY.equals(id);
    }

    private static int unsetOurKeys(MemoryAPI mem) {
        int n = 0;
        for (String key : new ArrayList<String>(mem.getKeys())) {
            if (!key.startsWith("$sts_")) continue;
            mem.unset(key);
            n++;
        }
        return n;
    }

    /** Transient: tells the player the outcome once the campaign is on screen. */
    public static class Notice implements EveryFrameScript {

        private final String text;
        private final boolean ok;
        private boolean done;

        public Notice(String text, boolean ok) {
            this.text = text;
            this.ok = ok;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done || Global.getSector().getCampaignUI().isShowingDialog()) return;
            done = true;
            Global.getSector().getCampaignUI().addMessage(text,
                    ok ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
        }
    }
}
