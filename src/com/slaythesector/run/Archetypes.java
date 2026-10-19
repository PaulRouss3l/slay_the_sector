package com.slaythesector.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSSettings;

/**
 * Which factions can be played (Archetypes) or fought, read from the game at runtime so modded
 * factions are included. Ship pools come from throwaway faction fleets, so they use each
 * faction's own doctrine and fits.
 */
public final class Archetypes {

    private Archetypes() {}

    /** Factions eligible as Archetype or regular enemy, sorted by display name. */
    public static List<FactionAPI> eligible() {
        Set<String> excluded = new HashSet<String>();
        excluded.add(Factions.PLAYER);
        excluded.add(Factions.NEUTRAL);
        excluded.add(Factions.INDEPENDENT);
        excluded.addAll(STSSettings.bossFactions());
        excluded.addAll(STSSettings.factionBlacklist());

        List<FactionAPI> out = new ArrayList<FactionAPI>();
        for (FactionAPI f : Global.getSector().getAllFactions()) {
            if (excluded.contains(f.getId())) continue;
            if (!f.isShowInIntelTab()) continue;
            if (f.getKnownShips().size() < STSSettings.minKnownShips()) continue;
            out.add(f);
        }
        Collections.sort(out, new Comparator<FactionAPI>() {
            @Override
            public int compare(FactionAPI a, FactionAPI b) {
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
        });
        return out;
    }

    /**
     * Distinct ships (one per hull) a faction fields, each costing at most {@code maxDP}, cheapest
     * first. Members belong to a throwaway fleet: copy their variants, don't add them directly.
     */
    public static List<FleetMemberAPI> shipPool(String factionId, float maxDP, int limit) {
        List<FleetMemberAPI> out = new ArrayList<FleetMemberAPI>();
        Set<String> hulls = new HashSet<String>();
        for (int attempt = 0; attempt < 2 && out.size() < limit; attempt++) {
            CampaignFleetAPI fleet = throwawayFleet(factionId, 220f);
            if (fleet == null) continue;
            for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                if (m.isFighterWing() || m.isCivilian()) continue;
                if (m.getDeploymentPointsCost() > maxDP) continue;
                if (hulls.add(m.getHullId())) out.add(m);
            }
        }
        Collections.sort(out, new Comparator<FleetMemberAPI>() {
            @Override
            public int compare(FleetMemberAPI a, FleetMemberAPI b) {
                return Float.compare(a.getDeploymentPointsCost(), b.getDeploymentPointsCost());
            }
        });
        return out.size() > limit ? new ArrayList<FleetMemberAPI>(out.subList(0, limit)) : out;
    }

    /**
     * Shuffled {@link #shipPool} of a faction (no automated hulls, which need a skill to crew),
     * no larger than {@code maxSize}. Empty for an unknown faction.
     */
    public static List<FleetMemberAPI> shipsUpTo(String factionId, HullSize maxSize, Random rng) {
        List<FleetMemberAPI> pool = new ArrayList<FleetMemberAPI>();
        if (factionId == null || Global.getSector().getFaction(factionId) == null) return pool;
        for (FleetMemberAPI m : shipPool(factionId, 999f, 40)) {
            if (m.getHullSpec().getHullSize().ordinal() > maxSize.ordinal()) continue;
            if (Misc.isAutomated(m)) continue;
            pool.add(m);
        }
        Collections.shuffle(pool, rng);
        return pool;
    }

    private static CampaignFleetAPI throwawayFleet(String factionId, float fp) {
        FleetParamsV3 params = new FleetParamsV3(new Vector2f(0f, 0f), factionId, 1f,
                FleetTypes.PATROL_LARGE, fp, 0f, 0f, 0f, 0f, 0f, 0f);
        params.ignoreMarketFleetSizeMult = true;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null || fleet.getFleetData().getNumMembers() == 0) return null;
        fleet.inflateIfNeeded();
        return fleet;
    }
}
