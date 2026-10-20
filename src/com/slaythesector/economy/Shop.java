package com.slaythesector.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSSettings;

/**
 * A Shop Node's stock, priced in Supplies: one page per Act of ships (mostly the Archetype's, one
 * size above the usual Reward), single weapons and an officer. Shown in the card grid in budget mode.
 */
public final class Shop {

    private Shop() {}

    public enum Kind { SHIP, WEAPON, OFFICER, DP_CAP }

    public static class Item {
        public final Kind kind;
        public final FleetMemberAPI ship;
        public final String weaponId;
        public final int officerLevel;

        Item(Kind kind, FleetMemberAPI ship, String weaponId, int officerLevel) {
            this.kind = kind;
            this.ship = ship;
            this.weaponId = weaponId;
            this.officerLevel = officerLevel;
        }
    }

    /**
     * One page of stock per Act (Act 2 = 2 pages...): each page holds 3 ships (2 of the Archetype,
     * 1 of an enemy faction, taking turns), 4 weapons and 1 officer. No hull or weapon repeats.
     */
    public static List<Card> stock(RunState state, RunNode node) {
        // Not security-sensitive: stock variety only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        int pages = Math.max(1, Math.min(3, state.getAct()));
        HullSize maxSize = bigger(DepthScaling.rewardSize(state, node.getDepth()));

        List<FleetMemberAPI> own = Archetypes.shipsUpTo(state.getArchetypeId(), maxSize, rng);
        List<String> enemies = new ArrayList<String>(state.getEnemyFactionIds());
        Collections.shuffle(enemies, rng);
        Set<String> hulls = new HashSet<String>();
        List<String> weaponIds = weapons(state, rng, 4 * pages);
        int level = DepthScaling.officerLevel(state, node.getDepth());

        List<Card> cards = new ArrayList<Card>();
        for (int page = 0; page < pages; page++) {
            List<FleetMemberAPI> ships = new ArrayList<FleetMemberAPI>();
            for (FleetMemberAPI m : own) {
                if (ships.size() >= 2) break;
                if (hulls.add(m.getHullId())) ships.add(m);
            }
            if (!enemies.isEmpty()) {
                String enemy = enemies.get(page % enemies.size());
                for (FleetMemberAPI m : Archetypes.shipsUpTo(enemy, maxSize, rng)) {
                    if (hulls.add(m.getHullId())) {
                        ships.add(m);
                        break;
                    }
                }
            }
            for (FleetMemberAPI m : ships) cards.add(shipCard(m));
            // Page 1 opens with the Fleet DP cap upgrade and carries one weapon less, so it stays 8 cards.
            int firstWeapon = page * 4;
            int lastWeapon = page * 4 + 4;
            if (page == 0) {
                cards.add(0, dpCapCard());
                lastWeapon--;
            }
            for (int w = firstWeapon; w < Math.min(weaponIds.size(), lastWeapon); w++) cards.add(weaponCard(weaponIds.get(w)));
            cards.add(officerCard(level));
        }
        return cards;
    }

    private static Card shipCard(FleetMemberAPI m) {
        int price = Math.round(m.getDeploymentPointsCost() * STSSettings.shopSuppliesPerDP());
        return new Card(new Item(Kind.SHIP, m, null, 0), m.getHullSpec().getHullName(),
                m.getHullSpec().getManufacturer() + " - " + (int) m.getDeploymentPointsCost() + " DP - "
                        + price + " supplies", m, price, 1);
    }

    private static Card weaponCard(String id) {
        WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
        int price = weaponPrice(spec);
        return new Card(new Item(Kind.WEAPON, null, id, 0), spec.getWeaponName(),
                spec.getSize().getDisplayName() + " " + spec.getMountType().getDisplayName() + " - "
                        + price + " supplies", null, price, 1);
    }

    private static Card officerCard(int level) {
        int price = STSSettings.shopOfficerBase() + level * STSSettings.shopOfficerPerLevel();
        return new Card(new Item(Kind.OFFICER, null, null, level), "Officer (level " + level + ")",
                "Joins the Run fleet - " + price + " supplies", null, price, 1);
    }

    private static Card dpCapCard() {
        int dp = STSSettings.shopDPCapAmount();
        int price = STSSettings.shopDPCapPrice();
        return new Card(new Item(Kind.DP_CAP, null, null, 0), "+" + dp + " Fleet DP cap",
                "For the rest of the Run (the hard cap rises too) - " + price + " supplies", null, price, 1);
    }

    /** Applies a confirmed purchase; Supplies are taken by the caller. */
    public static String apply(List<Card> bought, CampaignFleetAPI fleet, RunState state) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Card c : bought) {
            Item it = (Item) c.data;
            switch (it.kind) {
                case SHIP:
                    RunFleet.addShip(fleet, it.ship.getVariant());
                    break;
                case WEAPON:
                    fleet.getCargo().addWeapons(it.weaponId, 1);
                    break;
                case OFFICER:
                    PersonAPI officer = OfficerManagerEvent.createOfficer(Global.getSector().getPlayerFaction(), it.officerLevel);
                    fleet.getFleetData().addOfficer(officer);
                    break;
                case DP_CAP:
                    state.addSoftCapBonus(STSSettings.shopDPCapAmount());
                    break;
            }
            Integer n = counts.get(c.title);
            counts.put(c.title, n == null ? 1 : n + 1);
        }
        RunFleet.topUp(fleet);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue() > 1 ? e.getValue() + "x " : "").append(e.getKey());
        }
        return sb.toString();
    }

    public static int total(List<Card> cards) {
        int t = 0;
        for (Card c : cards) t += c.cost;
        return t;
    }

    static int weaponPrice(WeaponSpecAPI spec) {
        float op = spec.getOrdnancePointCost(Global.getSector().getPlayerStats());
        return Math.round(STSSettings.shopWeaponBase() + op * STSSettings.shopWeaponPerOP());
    }

    /**
     * Weapons the Archetype and this Run's enemies know, excluding built-in/system ones. With
     * {@code minTier} 0 any tier; used by the Treasure with a higher tier.
     */
    static List<String> weapons(RunState state, Random rng, int count) {
        return weapons(state, rng, count, 0);
    }

    static List<String> weapons(RunState state, Random rng, int count, int minTier) {
        List<String> factions = new ArrayList<String>(state.getEnemyFactionIds());
        factions.add(state.getArchetypeId());
        List<String> pool = new ArrayList<String>();
        for (String fid : factions) {
            if (Global.getSector().getFaction(fid) == null) continue;
            for (String id : Global.getSector().getFaction(fid).getKnownWeapons()) {
                if (pool.contains(id)) continue;
                WeaponSpecAPI spec;
                try {
                    spec = Global.getSettings().getWeaponSpec(id);
                } catch (RuntimeException e) {
                    continue;
                }
                if (spec == null || spec.getTier() < minTier) continue;
                WeaponType t = spec.getType();
                if (t == WeaponType.BUILT_IN || t == WeaponType.DECORATIVE || t == WeaponType.SYSTEM
                        || t == WeaponType.STATION_MODULE || t == WeaponType.LAUNCH_BAY) continue;
                if (spec.hasTag("no_drop") || spec.hasTag("restricted")) continue;
                pool.add(id);
            }
        }
        Collections.shuffle(pool, rng);
        return new ArrayList<String>(pool.subList(0, Math.min(count, pool.size())));
    }

    static HullSize bigger(HullSize size) {
        switch (size) {
            case FRIGATE:   return HullSize.DESTROYER;
            case DESTROYER: return HullSize.CRUISER;
            default:        return HullSize.CAPITAL_SHIP;
        }
    }
}
