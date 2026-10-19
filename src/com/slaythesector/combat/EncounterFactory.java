package com.slaythesector.combat;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.util.STSSettings;

/** Builds the enemy fleet for a fight Node, placed on the Node entity. */
public final class EncounterFactory {

    private EncounterFactory() {}

    public static CampaignFleetAPI spawn(RunNode node, SectorEntityToken at) {
        float fp = DepthScaling.combatPoints(node.getDepth(), node.getType());
        FleetParamsV3 params = new FleetParamsV3(new Vector2f(0f, 0f), node.getFactionId(),
                DepthScaling.quality(node.getDepth()),
                node.getType() == NodeType.BOSS ? FleetTypes.PATROL_LARGE : FleetTypes.PATROL_MEDIUM,
                fp, 0f, 0f, 0f, 0f, 0f, 0f);
        // Same difficulty whatever the state of the Sector's markets.
        params.ignoreMarketFleetSizeMult = true;
        if (node.getType() == NodeType.BOSS) applyBossOfficers(params);
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.getFleetData().getNumMembers() == 0) {
            STSLog.warn("createFleet gave no ships for " + node.getFactionId() + "; using a fallback fleet");
            fleet = FleetFactoryV3.createEmptyFleet(node.getFactionId(), FleetTypes.PATROL_SMALL, null);
            fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hound_Standard"));
        }
        fleet.setName(node.getType() == NodeType.BOSS ? "Apex Fleet" : fleet.getFaction().getDisplayName() + " Fleet");
        // Ships and fits stay the generated faction's; the flag changes so commissions/bounties ignore the kill.
        if (Global.getSector().getFaction(STSConstants.FACTION_DESCENT) != null) {
            fleet.setFaction(STSConstants.FACTION_DESCENT, false);
        }
        fleet.setNoAutoDespawn(true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.inflateIfNeeded();
        at.getContainingLocation().addEntity(fleet);
        fleet.setLocation(at.getLocation().x, at.getLocation().y);
        fleet.setDoNotAdvanceAI(true);
        return fleet;
    }

    /**
     * Boss pilots ({@code sts_bossOfficers}): "none" = no officers or AI cores at all, "gamma" =
     * Gamma cores only on automated ships, anything else = the faction's normal officers/cores.
     */
    private static void applyBossOfficers(FleetParamsV3 params) {
        String mode = STSSettings.bossOfficers();
        if ("none".equals(mode)) {
            params.withOfficers = false;
        } else if ("gamma".equals(mode)) {
            params.aiCores = OfficerQuality.AI_GAMMA;
        }
    }

    public static void despawn(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getContainingLocation() == null) return;
        fleet.clearAssignments();
        if (fleet.getContainingLocation().getFleets().contains(fleet)) {
            fleet.getContainingLocation().removeEntity(fleet);
        }
    }
}
