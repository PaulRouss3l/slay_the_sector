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
import com.slaythesector.run.RunState;
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

    /**
     * One ship of the defeated faction and one of the Run's Archetype (either pool covers for the
     * other when empty), plus an officer or a weapon bundle.
     */
    public static List<Card> offer(RunState state, int row, String factionId) {
        String archetypeId = state.getArchetypeId();
        // Not security-sensitive: reward variety only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        List<Card> cards = new ArrayList<Card>();
        HullSize maxSize = DepthScaling.rewardSize(state, row);

        List<FleetMemberAPI> enemyPool = Archetypes.shipsUpTo(factionId, maxSize, rng);
        List<FleetMemberAPI> ownPool = archetypeId == null || archetypeId.equals(factionId)
                ? new ArrayList<FleetMemberAPI>() : Archetypes.shipsUpTo(archetypeId, maxSize, rng);
        List<FleetMemberAPI> pool = new ArrayList<FleetMemberAPI>(enemyPool);
        pool.addAll(ownPool);

        List<FleetMemberAPI> picks = new ArrayList<FleetMemberAPI>();
        List<String> pickFactions = new ArrayList<String>();
        if (!enemyPool.isEmpty()) {
            picks.add(enemyPool.remove(0));
            pickFactions.add(factionId);
        }
        if (!ownPool.isEmpty()) {
            picks.add(ownPool.remove(0));
            pickFactions.add(archetypeId);
        }
        while (picks.size() < 2 && !(enemyPool.isEmpty() && ownPool.isEmpty())) {
            boolean fromEnemy = !enemyPool.isEmpty();
            picks.add((fromEnemy ? enemyPool : ownPool).remove(0));
            pickFactions.add(fromEnemy ? factionId : archetypeId);
        }
        for (int i = 0; i < picks.size(); i++) {
            FleetMemberAPI m = picks.get(i);
            String faction = Global.getSector().getFaction(pickFactions.get(i)).getDisplayName();
            cards.add(new Card(new Reward(Kind.SHIP, m, 0, null), m.getHullSpec().getHullName(),
                    faction + " - " + m.getVariant().getDisplayName() + " - " + (int) m.getDeploymentPointsCost() + " DP", m, 0));
        }

        List<String> weapons = weaponBundle(pool, rng);
        if (rng.nextBoolean() || weapons.isEmpty()) {
            int level = DepthScaling.officerLevel(state, row);
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
    static List<String> weaponBundle(List<FleetMemberAPI> pool, Random rng) {
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

    public static String describe(List<String> weaponIds) {
        StringBuilder sb = new StringBuilder();
        for (String id : weaponIds) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Global.getSettings().getWeaponSpec(id).getWeaponName());
        }
        return sb.toString();
    }
}
