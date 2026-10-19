package com.slaythesector.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSSettings;

/** One-line descriptions of Nodes, shared by the dialogs and the node map. */
public final class NodeText {

    private NodeText() {}

    public static String factionName(RunNode n) {
        if (n.getFactionId() == null) return "";
        FactionAPI f = Global.getSector().getFaction(n.getFactionId());
        return f == null ? n.getFactionId() : f.getDisplayName();
    }

    /** "Combat: Hegemony", "Shop", ... (no em dash: the UI fonts lack it). */
    public static String title(RunNode n) {
        if (n.getType().isFight() && n.getFactionId() != null) return n.getType().label() + ": " + factionName(n);
        return n.getType() == NodeType.BOSS ? "True Boss" : n.getType().label();
    }

    /** Title plus Depth (across the Run) and, for fights, the estimated enemy strength. */
    public static String full(RunNode n) {
        RunState state = RunManager.get();
        if (state == null) return title(n);
        String s = title(n) + " (Depth " + state.getGlobalDepth(n);
        if (n.getType().isFight() && n.getFactionId() != null) {
            float fp = DepthScaling.combatPoints(state, n);
            if (n.getType() == NodeType.ELITE) fp *= STSSettings.eliteFPMult();
            s += ", strength " + Math.round(fp);
        }
        return s + ")";
    }

    /** What the player finds there, for the map's info line. */
    public static String blurb(NodeType type) {
        switch (type) {
            case START:    return "Where the Descent begins.";
            case COMBAT:   return "A standard fight. Win for Supplies and a Reward pick.";
            case BOSS:     return "The Act's Boss. No retreat. Beat it to go deeper, or to win the Run.";
            case SHOP:     return "Buy ships, weapons, officers and cheap repairs with Supplies.";
            case REST:     return "Choose one: free full repair, an S-mod, or an officer level.";
            case TREASURE: return "Choose one rare prize: a ship above your Depth, rare weapons, or an S-mod.";
            case ELITE:    return "A stronger fleet surrounds you: break out with most of your fleet for a bigger reward.";
            case AMBUSH:   return "The enemy flees: destroy its marked flagship for a Reward pick.";
            case EVENT:    return "Something unexpected: a battle station, sabotage, a side battle or a convoy.";
            default:       return "";
        }
    }
}
