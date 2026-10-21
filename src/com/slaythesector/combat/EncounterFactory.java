package com.slaythesector.combat;

import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.loading.VariantSource;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.util.STSSettings;

/**
 * Builds the fleets of a fight, placed on the Node entity. Ships and fits come from the generated
 * faction; the flag then becomes {@code sts_descent} (enemies) or {@code sts_ally} (allies) so
 * commissions, bounties and reputation ignore Run fights.
 */
public final class EncounterFactory {

    private EncounterFactory() {}

    /** The fight of a Combat, Elite, Ambush or Boss Node. */
    public static CampaignFleetAPI spawn(RunState state, RunNode node, SectorEntityToken at) {
        NodeType type = node.getType();
        if (type == NodeType.BOSS && state.getAct() >= 4) return spawnTrueBoss(at);
        float fp = DepthScaling.combatPoints(state, node);
        if (type == NodeType.ELITE) fp *= STSSettings.eliteFPMult();
        FleetParamsV3 params = params(node.getFactionId(), DepthScaling.quality(state, node), fp, 0f,
                type == NodeType.BOSS || type == NodeType.ELITE ? FleetTypes.PATROL_LARGE : FleetTypes.PATROL_MEDIUM);
        if (type == NodeType.BOSS) applyBossOfficers(params, state, node.getFactionId());
        String name = type == NodeType.BOSS ? "Apex Fleet" : null;
        CampaignFleetAPI fleet = create(params, node.getFactionId(), name, STSConstants.FACTION_DESCENT, at);
        if (type == NodeType.ELITE) {
            fleet.getMemoryWithoutUpdate().set(STSConstants.MEM_BATTLE, STSConstants.BATTLE_ESCAPE);
        } else if (type == NodeType.AMBUSH) {
            fleet.getMemoryWithoutUpdate().set(STSConstants.MEM_BATTLE, STSConstants.BATTLE_PURSUIT);
            FleetMemberAPI target = largest(fleet);
            if (target != null) fleet.getMemoryWithoutUpdate().set(STSConstants.MEM_TARGET, target.getId());
        }
        return fleet;
    }

    /** Event "convoy raid": freighters with a light escort, fleeing. */
    public static CampaignFleetAPI spawnConvoy(RunState state, String factionId, RunNode node, SectorEntityToken at) {
        float fp = DepthScaling.combatPoints(state, node.getDepth(), NodeType.COMBAT);
        FleetParamsV3 params = params(factionId, DepthScaling.quality(state, node), fp * 0.4f, fp, FleetTypes.TRADE);
        CampaignFleetAPI fleet = create(params, factionId, factionName(factionId) + " Convoy", STSConstants.FACTION_DESCENT, at);
        fleet.getMemoryWithoutUpdate().set(STSConstants.MEM_BATTLE, STSConstants.BATTLE_PURSUIT);
        return fleet;
    }

    /** Event "side battle": one side, as an enemy ({@code ally} false) or an ally of the player. */
    public static CampaignFleetAPI spawnSide(RunState state, String factionId, RunNode node, boolean ally, SectorEntityToken at) {
        float fp = DepthScaling.combatPoints(state, node.getDepth(), NodeType.COMBAT) * (ally ? STSSettings.sideAllyFPMult() : 1f);
        FleetParamsV3 params = params(factionId, DepthScaling.quality(state, node), fp, 0f, FleetTypes.PATROL_MEDIUM);
        String flag = ally ? STSConstants.FACTION_ALLY : STSConstants.FACTION_DESCENT;
        CampaignFleetAPI fleet = create(params, factionId, factionName(factionId) + " Fleet", flag, at);
        if (ally) {
            fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
            fleet.setLocation(at.getLocation().x + 150f, at.getLocation().y);
        }
        return fleet;
    }

    /**
     * Event "battle station": a lone tier-1 orbital station (STATION hull), sturdier in later Acts.
     * Tier 2 (~200 FP) would outclass a capped Run fleet.
     */
    public static CampaignFleetAPI spawnStation(RunState state, SectorEntityToken at) {
        int act = state.getAct();
        String variant = act >= 3 ? "station1_hightech_Standard" : act == 2 ? "station1_midline_Standard" : "station1_Standard";
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(STSConstants.FACTION_DESCENT, FleetTypes.BATTLESTATION, null);
        FleetMemberAPI station;
        try {
            station = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        } catch (RuntimeException e) {
            STSLog.warn("station variant " + variant + " missing; using station1_Standard");
            station = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "station1_Standard");
        }
        // Never recoverable into the Run fleet. Tag a copy: the stock variant is shared game-wide.
        unboardable(station);
        fleet.getFleetData().addFleetMember(station);
        fleet.setName("Battle Station");
        fleet.setStationMode(true);
        prepare(fleet, at);
        return fleet;
    }

    /**
     * Act 4's true Boss: one unique ship ({@code sts_trueBossHull}), no escort, no officer or core,
     * fighting to the last.
     */
    public static CampaignFleetAPI spawnTrueBoss(SectorEntityToken at) {
        String hull = STSSettings.trueBossHull();
        String variant = null;
        List<String> variants = Global.getSettings().getHullIdToVariantListMap().getList(hull);
        for (String id : variants) {
            if (!id.endsWith("_Hull")) {
                variant = id;
                break;
            }
        }
        if (variant == null) {
            STSLog.warn("no variant for true Boss hull " + hull + "; using ziggurat_Experimental");
            variant = "ziggurat_Experimental";
        }
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(STSConstants.FACTION_DESCENT, FleetTypes.PATROL_SMALL, null);
        FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        unboardable(ship);
        fleet.getFleetData().addFleetMember(ship);
        fleet.setName("The " + ship.getHullSpec().getHullName());
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        prepare(fleet, at);
        return fleet;
    }

    /**
     * Marks a spawned ship as never recoverable. A member made from a variant id uses the shared
     * stock variant, so the tag goes on a copy (vanilla TTBlackSite does the same).
     */
    private static void unboardable(FleetMemberAPI m) {
        ShipVariantAPI copy = m.getVariant().clone();
        copy.setSource(VariantSource.REFIT);
        copy.addTag(Tags.VARIANT_UNBOARDABLE);
        m.setVariant(copy, false, false);
        m.updateStats();
    }

    private static FleetParamsV3 params(String factionId, float quality, float combatFP, float freighterFP, String type) {
        FleetParamsV3 params = new FleetParamsV3(new Vector2f(0f, 0f), factionId, quality,
                type, combatFP, freighterFP, 0f, 0f, 0f, 0f, 0f);
        // Same difficulty whatever the state of the Sector's markets.
        params.ignoreMarketFleetSizeMult = true;
        return params;
    }

    private static CampaignFleetAPI create(FleetParamsV3 params, String factionId, String name, String flag, SectorEntityToken at) {
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.getFleetData().getNumMembers() == 0) {
            STSLog.warn("createFleet gave no ships for " + factionId + "; using a fallback fleet");
            fleet = FleetFactoryV3.createEmptyFleet(factionId, FleetTypes.PATROL_SMALL, null);
            fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hound_Standard"));
        }
        fleet.setName(name != null ? name : fleet.getFaction().getDisplayName() + " Fleet");
        if (Global.getSector().getFaction(flag) != null) fleet.setFaction(flag, false);
        prepare(fleet, at);
        return fleet;
    }

    private static void prepare(CampaignFleetAPI fleet, SectorEntityToken at) {
        fleet.setNoAutoDespawn(true);
        // Run fights never touch reputation (Nexerelin's warmonger event checks this flag).
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.inflateIfNeeded();
        // As vanilla FleetFactoryV3 does: sync stats first, then every ship at its maximum CR (a CR
        // set before the sync uses stale stats, e.g. right after a variant swap).
        fleet.forceSync();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            m.updateStats();
            m.getRepairTracker().setCR(m.getRepairTracker().getMaxCR());
        }
        at.getContainingLocation().addEntity(fleet);
        fleet.setLocation(at.getLocation().x, at.getLocation().y);
        fleet.setDoNotAdvanceAI(true);
    }

    private static String factionName(String factionId) {
        return Global.getSector().getFaction(factionId) == null ? factionId : Global.getSector().getFaction(factionId).getDisplayName();
    }

    private static FleetMemberAPI largest(CampaignFleetAPI fleet) {
        FleetMemberAPI best = null;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.isFighterWing()) continue;
            if (best == null || m.getDeploymentPointsCost() > best.getDeploymentPointsCost()) best = m;
        }
        return best;
    }

    /**
     * Boss pilots ({@code sts_bossOfficers}): "none" = no officers or AI cores at all, "gamma" =
     * Gamma cores only on automated ships, anything else = the faction's normal officers/cores.
     * Ascension Primordial overrides: pilots for every Boss.
     */
    private static void applyBossOfficers(FleetParamsV3 params, RunState state, String factionId) {
        if (state.getAscension() >= Ascension.PRIMORDIAL) {
            // Ascension Primordial: Bosses get pilots. Gamma cores for the automated Boss factions
            // (Derelict, Remnant), the faction's own officers for the Act 1 Apex fleet.
            params.withOfficers = true;
            if (STSSettings.bossFactions().contains(factionId)) params.aiCores = OfficerQuality.AI_GAMMA;
            return;
        }
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

    /** Total deployment points of a fleet (used to score escapes and raids). */
    public static float dp(CampaignFleetAPI fleet) {
        float dp = 0f;
        if (fleet == null) return dp;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) dp += m.getDeploymentPointsCost();
        return dp;
    }

    /** Whether the member with {@code memberId} is still in the fleet (not destroyed or captured). */
    public static boolean has(CampaignFleetAPI fleet, String memberId) {
        if (fleet == null || memberId == null) return false;
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.getId().equals(memberId)) return true;
        }
        return false;
    }
}
