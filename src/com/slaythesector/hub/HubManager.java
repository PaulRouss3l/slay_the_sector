package com.slaythesector.hub;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;

/**
 * The "hub" is the existing inhabited world the player launches runs from. Rather
 * than spawning a standalone marker, a "Descend into the Sector" option is injected
 * into that world's interaction menu (see {@code data/campaign/rules.csv} +
 * {@link com.slaythesector.rulecmd.STS_AddDescendOption}). Runs return the player here.
 */
public final class HubManager {

    private HubManager() {}

    /** Market/entity id the descend option is attached to. */
    public static final String HUB_ENTITY_ID = "jangala";

    /** The hub world token, or null if it doesn't exist in this game. */
    public static SectorEntityToken findHub() {
        return Global.getSector().getEntityById(HUB_ENTITY_ID);
    }
}
