package com.slaythesector.world;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
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
        if (hub != null) return hub;

        StarSystemAPI sys = pickSystem();
        if (sys == null) {
            STSLog.warn("no populated system found; Hub not placed");
            return null;
        }
        SectorEntityToken focus = sys.getStar() != null ? sys.getStar() : sys.getCenter();
        hub = sys.addCustomEntity(STSConstants.HUB_ID, "Descent Relay", STSConstants.HUB_ENTITY_TYPE, Factions.NEUTRAL);
        hub.addTag(STSConstants.TAG_HUB);
        hub.setCircularOrbitPointingDown(focus, 45f, 2600f, 160f);
        STSLog.info("Hub placed in " + sys.getName());
        return hub;
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
