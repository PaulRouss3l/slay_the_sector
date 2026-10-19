package com.slaythesector.world;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.intel.HubIntel;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;

/**
 * Places the Hub station once per save (also in saves that predate the mod): orbiting the star of
 * the most populated core system, so it works in random sectors too. No market.
 */
public final class HubPlacer {

    private HubPlacer() {}

    public static SectorEntityToken getHub() {
        return Global.getSector().getEntityById(STSConstants.HUB_ID);
    }

    public static SectorEntityToken ensureHub() {
        SectorEntityToken hub = getHub();
        if (hub != null) {
            HubIntel.ensure();
            return hub;
        }

        StarSystemAPI sys = pickSystem();
        if (sys == null) {
            STSLog.warn("no populated system found; Hub not placed");
            return null;
        }
        SectorEntityToken focus = sys.getStar() != null ? sys.getStar() : sys.getCenter();
        hub = sys.addCustomEntity(STSConstants.HUB_ID, "Descent Relay", STSConstants.HUB_ENTITY_TYPE, Factions.NEUTRAL);
        hub.addTag(STSConstants.TAG_HUB);
        // Plain orbit (not pointing down) keeps the station sprite upright, as in the Spike.
        hub.setCircularOrbit(focus, 45f, freeOrbitRadius(sys, focus), 160f);
        STSLog.info("Hub placed in " + sys.getName());
        HubIntel.ensure();
        return hub;
    }

    /** First orbit radius from 2600 that keeps clear of every planet's orbit. */
    private static float freeOrbitRadius(StarSystemAPI sys, SectorEntityToken focus) {
        float radius = 2600f;
        boolean clear = false;
        while (!clear && radius < 20000f) {
            clear = true;
            for (PlanetAPI p : sys.getPlanets()) {
                if (p == focus) continue;
                float d = Misc.getDistance(p.getLocation(), focus.getLocation());
                if (Math.abs(d - radius) < p.getRadius() + 400f) {
                    clear = false;
                    radius += 300f;
                    break;
                }
            }
        }
        return radius;
    }

    /** The system whose non-hidden markets have the largest total size. */
    private static StarSystemAPI pickSystem() {
        Map<StarSystemAPI, Integer> weight = new HashMap<StarSystemAPI, Integer>();
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            if (m.isHidden() || m.getStarSystem() == null) continue;
            Integer w = weight.get(m.getStarSystem());
            weight.put(m.getStarSystem(), (w == null ? 0 : w) + m.getSize());
        }
        StarSystemAPI best = null;
        int bestWeight = -1;
        for (Map.Entry<StarSystemAPI, Integer> e : weight.entrySet()) {
            if (e.getValue() > bestWeight) {
                best = e.getKey();
                bestWeight = e.getValue();
            }
        }
        return best;
    }
}
