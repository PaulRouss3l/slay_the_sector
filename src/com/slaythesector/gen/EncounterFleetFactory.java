package com.slaythesector.gen;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.slaythesector.run.RunNode;
import com.slaythesector.util.DepthScaling;

/** Builds the hostile fleet for a combat node, scaled by depth/type. */
public final class EncounterFleetFactory {

    private EncounterFleetFactory() {}

    public static CampaignFleetAPI create(RunNode node) {
        float combatPts = DepthScaling.combatPoints(node.getDepth(), node.getType());
        Float quality = DepthScaling.quality(node.getDepth());
        String factionId = node.getFactionId() != null ? node.getFactionId() : Factions.PIRATES;

        FleetParamsV3 params = new FleetParamsV3(
                new Vector2f(node.getX(), node.getY()),
                factionId,
                quality,
                FleetTypes.PATROL_SMALL,
                combatPts, // combat
                0f,        // freighter
                0f,        // tanker
                0f,        // personnel
                0f,        // liner
                0f,        // utility
                0f);       // quality mod

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        fleet.setName(node.getType().label());
        fleet.setNoAutoDespawn(true);
        fleet.inflateIfNeeded();
        return fleet;
    }
}
