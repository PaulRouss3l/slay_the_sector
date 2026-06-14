package com.slaythesector.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.slaythesector.util.DepthScaling;

/**
 * Produces a small set of candidate reward ships of a given faction, preferring a
 * target hull size. Works by generating a throwaway faction fleet (so the ships use
 * that faction's doctrine + fits) and bucketing its members by size.
 */
public final class RewardShipFactory {

    private RewardShipFactory() {}

    /** Up to {@code n} distinct candidate members of {@code factionId}, preferring {@code size}. */
    public static List<FleetMemberAPI> pick(String factionId, HullSize size, int n) {
        List<FleetMemberAPI> result = new ArrayList<FleetMemberAPI>();

        float fp = DepthScaling.rewardPoolPoints(size);
        FleetParamsV3 params = new FleetParamsV3(
                new Vector2f(0f, 0f), factionId, null, FleetTypes.PATROL_LARGE,
                fp, 0f, 0f, 0f, 0f, 0f, 0f);
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
        if (fleet == null) {
            return result;
        }

        List<FleetMemberAPI> exact = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> smaller = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.isFighterWing()) {
                continue;
            }
            HullSize hs = m.getHullSpec().getHullSize();
            if (hs == size) {
                exact.add(m);
            } else if (hs.ordinal() < size.ordinal()) {
                smaller.add(m);
            }
        }

        Set<String> seen = new HashSet<String>();
        addDistinct(result, exact, seen, n);
        if (result.size() < n) {
            Collections.sort(smaller, new Comparator<FleetMemberAPI>() {
                @Override
                public int compare(FleetMemberAPI a, FleetMemberAPI b) {
                    return b.getHullSpec().getHullSize().ordinal() - a.getHullSpec().getHullSize().ordinal();
                }
            });
            addDistinct(result, smaller, seen, n);
        }
        return result;
    }

    private static void addDistinct(List<FleetMemberAPI> out, List<FleetMemberAPI> src,
                                    Set<String> seen, int n) {
        for (FleetMemberAPI m : src) {
            if (out.size() >= n) {
                break;
            }
            if (seen.add(m.getHullId())) {
                out.add(m);
            }
        }
    }
}
