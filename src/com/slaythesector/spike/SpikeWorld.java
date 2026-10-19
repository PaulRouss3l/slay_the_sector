package com.slaythesector.spike;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.slaythesector.util.STSLog;

/** Spike: test Hub placement, contained Run system, jump transitions, test enemy fleets. */
public final class SpikeWorld {

    private SpikeWorld() {}

    /** Idempotent: places the test Hub next to the player the first time a save is loaded. */
    public static SectorEntityToken ensureHub() {
        SectorAPI sector = Global.getSector();
        SectorEntityToken hub = sector.getEntityById(SpikeIds.HUB_ID);
        if (hub != null) return hub;

        CampaignFleetAPI pf = sector.getPlayerFleet();
        if (pf == null || pf.getContainingLocation() == null) return null;
        LocationAPI loc = pf.getContainingLocation();
        if (isRunSystem(loc)) return null;

        hub = loc.addCustomEntity(SpikeIds.HUB_ID, "Descent Relay (spike)", SpikeIds.ENTITY_TYPE, Factions.NEUTRAL);
        hub.addTag(SpikeIds.TAG_HUB);
        hub.setFixedLocation(pf.getLocation().x + 400f, pf.getLocation().y);
        STSLog.info("spike: hub placed in " + loc.getName());
        return hub;
    }

    public static StarSystemAPI ensureRunSystem() {
        SectorAPI sector = Global.getSector();
        StarSystemAPI sys = sector.getStarSystem(SpikeIds.RUN_SYSTEM_NAME);
        if (sys != null) return sys;

        sys = sector.createStarSystem(SpikeIds.RUN_SYSTEM_NAME);
        sys.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        sys.getLocation().set(-80000f, -80000f);
        SectorEntityToken center = sys.initNonStarCenter();
        center.addTag(Tags.THEME_HIDDEN);
        center.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        sys.addTag(Tags.THEME_HIDDEN);
        sys.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        sys.addTag(Tags.DO_NOT_SHOW_STRANDED_DIALOG);
        // No autogenerateHyperspaceJumpPoints(): the system has no way out but our beacon.

        SectorEntityToken beacon = sys.addCustomEntity(SpikeIds.BEACON_ID, "Descent Beacon (spike)",
                SpikeIds.ENTITY_TYPE, Factions.NEUTRAL);
        beacon.addTag(SpikeIds.TAG_BEACON);
        beacon.setFixedLocation(0f, 0f);
        STSLog.info("spike: run system created");
        return sys;
    }

    public static boolean isRunSystem(LocationAPI loc) {
        return loc != null && SpikeIds.RUN_SYSTEM_NAME.equals(loc.getName());
    }

    /** Vanilla gate pattern (GateCMD): dismiss the dialog first, unpause, then transition. */
    public static void jumpTo(SectorEntityToken from, SectorEntityToken to) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        Global.getSector().setPaused(false);
        Global.getSector().doHyperspaceTransition(pf, from, new JumpDestination(to, null), 1f);
        STSLog.info("spike: transition " + (from == null ? "?" : from.getName()) + " -> " + to.getName());
    }

    /** Spawns a hostile fleet tagged for the spike battle setup, near the player. */
    public static CampaignFleetAPI spawnTestEnemy(String factionId, float combatPoints) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        FleetParamsV3 params = new FleetParamsV3(new Vector2f(0f, 0f), factionId, 1f,
                FleetTypes.PATROL_MEDIUM, combatPoints, 0f, 0f, 0f, 0f, 0f, 0f);
        params.ignoreMarketFleetSizeMult = true;
        CampaignFleetAPI enemy = FleetFactoryV3.createFleet(params);
        if (enemy == null || enemy.getFleetData().getNumMembers() == 0) {
            STSLog.warn("spike: createFleet returned no fleet for " + factionId);
            return null;
        }
        enemy.setNoAutoDespawn(true);
        enemy.getMemoryWithoutUpdate().set(SpikeIds.MEM_SCENARIO, "spike");
        enemy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        enemy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        pf.getContainingLocation().addEntity(enemy);
        enemy.setLocation(pf.getLocation().x + 600f, pf.getLocation().y + 300f);
        enemy.addAssignment(FleetAssignment.INTERCEPT, pf, 30f);
        STSLog.info("spike: spawned " + factionId + " enemy, " + enemy.getFleetData().getNumMembers() + " ships");
        return enemy;
    }
}
