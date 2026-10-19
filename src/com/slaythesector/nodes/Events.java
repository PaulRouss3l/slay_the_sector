package com.slaythesector.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * The Events an Event Node can hold, and the id saved in {@link RunNode#getEventId()}: a kind,
 * plus the factions involved ("side:hegemony:pirates", "convoy:tritachyon").
 */
final class Events {

    private Events() {}

    static final String STATION = "station";
    /** A battle station the player chose to trade with: the Node then works as a Shop. */
    static final String STATION_TRADE = "station-trade";
    static final String SABOTAGE = "sabotage";
    static final String SIDE = "side";
    static final String CONVOY = "convoy";

    // Not security-sensitive: event variety only.
    private static final Random RNG = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random

    /** Draws an Event for {@code node}, picking its factions from this Run's enemies. */
    static String draw(RunState state, RunNode node) {
        List<String> factions = new ArrayList<String>(state.getEnemyFactionIds());
        if (factions.size() < 2) {
            for (FactionAPI f : Archetypes.eligible()) {
                if (!factions.contains(f.getId()) && !f.getId().equals(state.getArchetypeId())) factions.add(f.getId());
                if (factions.size() >= 2) break;
            }
        }
        int kind = RNG.nextInt(4);
        if (kind == 2 && factions.size() >= 2) {
            int a = RNG.nextInt(factions.size());
            int b = (a + 1 + RNG.nextInt(factions.size() - 1)) % factions.size();
            return SIDE + ":" + factions.get(a) + ":" + factions.get(b);
        }
        if (kind == 3 && !factions.isEmpty()) return CONVOY + ":" + factions.get(RNG.nextInt(factions.size()));
        return kind == 0 ? STATION : SABOTAGE;
    }

    static String sideA(String id) {
        return id.split(":")[1];
    }

    static String sideB(String id) {
        return id.split(":")[2];
    }

    static String convoyFaction(String id) {
        return id.split(":")[1];
    }

    static String factionName(String factionId) {
        FactionAPI f = Global.getSector().getFaction(factionId);
        return f == null ? factionId : f.getDisplayName();
    }

    static float roll() {
        return RNG.nextFloat();
    }
}
