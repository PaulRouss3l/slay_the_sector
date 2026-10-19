package com.slaythesector.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunNode;
import com.slaythesector.ui.Card;
import com.slaythesector.util.DepthScaling;

/** Builds the three Reward pick cards after a won fight, and applies the chosen one. */
public final class Rewards {

    private Rewards() {}

    public enum Kind { SHIP, OFFICER, WEAPONS }

    /** Payload carried by a reward card. */
    public static class Reward {
        public final Kind kind;
        public final FleetMemberAPI ship;
        public final int officerLevel;
        public final List<String> weaponIds;

        Reward(Kind kind, FleetMemberAPI ship, int officerLevel, List<String> weaponIds) {
            this.kind = kind;
            this.ship = ship;
            this.officerLevel = officerLevel;
            this.weaponIds = weaponIds;
        }
    }

    /** Two ships of the defeated faction plus an officer or a weapon bundle. */
    public static List<Card> offer(RunNode node) {
        // Not security-sensitive: reward variety only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        List<Card> cards = new ArrayList<Card>();
        HullSize maxSize = DepthScaling.rewardSize(node.getDepth());

        List<FleetMemberAPI> pool = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : Archetypes.shipPool(node.getFactionId(), 999f, 40)) {
            if (m.getHullSpec().getHullSize().ordinal() <= maxSize.ordinal()) pool.add(m);
        }
        Collections.shuffle(pool, rng);
        for (int i = 0; i < 2 && i < pool.size(); i++) {
            FleetMemberAPI m = pool.get(i);
            cards.add(new Card(new Reward(Kind.SHIP, m, 0, null), m.getHullSpec().getHullName(),
                    m.getVariant().getDisplayName() + " · " + (int) m.getDeploymentPointsCost() + " DP", m, 0));
        }

        List<String> weapons = weaponBundle(pool, rng);
        if (rng.nextBoolean() || weapons.isEmpty()) {
            int level = DepthScaling.officerLevel(node.getDepth());
            cards.add(new Card(new Reward(Kind.OFFICER, null, level, null), "Officer",
                    "A level " + level + " officer joins the Run fleet.", null, 0));
        } else {
            cards.add(new Card(new Reward(Kind.WEAPONS, null, 0, weapons), "Weapon bundle",
                    describe(weapons), null, 0));
        }
        return cards;
    }

    public static String apply(Reward r, CampaignFleetAPI fleet) {
        switch (r.kind) {
            case SHIP:
                FleetMemberAPI m = RunFleet.addShip(fleet, r.ship.getVariant());
                RunFleet.topUp(fleet);
                return m.getHullSpec().getHullName() + " joins the Run fleet.";
            case OFFICER:
                PersonAPI officer = OfficerManagerEvent.createOfficer(Global.getSector().getPlayerFaction(), r.officerLevel);
                fleet.getFleetData().addOfficer(officer);
                return officer.getNameString() + " (level " + r.officerLevel + ") joins. Assign them in the Fleet screen.";
            case WEAPONS:
                for (String id : r.weaponIds) fleet.getCargo().addWeapons(id, 1);
                return "Weapons added to the Run cargo: " + describe(r.weaponIds) + ".";
            default:
                return "";
        }
    }

    /** Three weapons taken from the pool ships' loadouts. */
    private static List<String> weaponBundle(List<FleetMemberAPI> pool, Random rng) {
        List<String> all = new ArrayList<String>();
        for (FleetMemberAPI m : pool) {
            for (String slot : m.getVariant().getNonBuiltInWeaponSlots()) {
                String w = m.getVariant().getWeaponId(slot);
                if (w != null) all.add(w);
            }
        }
        Collections.shuffle(all, rng);
        return new ArrayList<String>(all.subList(0, Math.min(3, all.size())));
    }

    private static String describe(List<String> weaponIds) {
        StringBuilder sb = new StringBuilder();
        for (String id : weaponIds) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Global.getSettings().getWeaponSpec(id).getWeaponName());
        }
        return sb.toString();
    }
}
