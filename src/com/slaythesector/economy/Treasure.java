package com.slaythesector.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.slaythesector.combat.Rewards;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.util.DepthScaling;

/**
 * A Treasure Node's prizes (choose one): a ship one size above the usual Reward, a bundle of
 * top-tier weapons, an S-mod, or a full restoration (no D-mods) of one ship; the caller runs the
 * ship pickers for the last two.
 */
public final class Treasure {

    private Treasure() {}

    public enum Kind { SHIP, WEAPONS, SMOD, RESTORE }

    public static class Prize {
        public final Kind kind;
        public final FleetMemberAPI ship;
        public final List<String> weaponIds;

        Prize(Kind kind, FleetMemberAPI ship, List<String> weaponIds) {
            this.kind = kind;
            this.ship = ship;
            this.weaponIds = weaponIds;
        }
    }

    public static List<Card> offer(RunState state, RunNode node, boolean smodPossible, boolean restorePossible) {
        // Not security-sensitive: prize variety only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        List<Card> cards = new ArrayList<Card>();

        FleetMemberAPI ship = rareShip(state, node, rng);
        if (ship != null) {
            cards.add(new Card(new Prize(Kind.SHIP, ship, null), ship.getHullSpec().getHullName(),
                    "Rare hull - " + ship.getHullSpec().getManufacturer() + " - "
                            + (int) ship.getDeploymentPointsCost() + " DP", ship, 0));
        }

        List<String> weapons = Shop.weapons(state, rng, 2, 3);
        if (weapons.size() < 2) weapons = Shop.weapons(state, rng, 2, 2);
        if (!weapons.isEmpty()) {
            cards.add(new Card(new Prize(Kind.WEAPONS, null, weapons), "Rare weapons",
                    Rewards.describe(weapons), null, 0));
        }

        if (smodPossible) {
            cards.add(new Card(new Prize(Kind.SMOD, null, null), "S-mod",
                    "Build one installed hullmod into a ship of your choice.", null, 0));
        }
        if (restorePossible) {
            cards.add(new Card(new Prize(Kind.RESTORE, null, null), "Restoration",
                    "Remove every D-mod from a ship of your choice.", null, 0));
        }
        return cards;
    }

    /** The largest hull the Archetype or this Run's enemies field, one size above the Reward cap. */
    private static FleetMemberAPI rareShip(RunState state, RunNode node, Random rng) {
        HullSize size = Shop.bigger(DepthScaling.rewardSize(node.getDepth()));
        List<String> factions = new ArrayList<String>(state.getEnemyFactionIds());
        factions.add(0, state.getArchetypeId());
        FleetMemberAPI best = null;
        for (String fid : factions) {
            for (FleetMemberAPI m : Archetypes.shipsUpTo(fid, size, rng)) {
                if (m.getHullSpec().getHullSize() != size) continue;
                if (best == null || rng.nextBoolean()) best = m;
                break;
            }
        }
        return best;
    }

    /** Applies a ship or weapons prize. The S-mod prize is applied by the caller's picker. */
    public static String apply(Prize p, CampaignFleetAPI fleet) {
        switch (p.kind) {
            case SHIP:
                FleetMemberAPI m = RunFleet.addShip(fleet, p.ship.getVariant());
                RunFleet.topUp(fleet);
                return m.getHullSpec().getHullName() + " joins the Run fleet.";
            case WEAPONS:
                for (String id : p.weaponIds) fleet.getCargo().addWeapons(id, 1);
                return "Weapons added to the Run cargo: " + Rewards.describe(p.weaponIds) + ".";
            default:
                return "";
        }
    }
}
