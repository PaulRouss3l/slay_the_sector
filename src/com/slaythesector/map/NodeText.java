package com.slaythesector.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.util.DepthScaling;

/** One-line descriptions of Nodes, shared by the dialogs and the node map. */
public final class NodeText {

    private NodeText() {}

    public static String factionName(RunNode n) {
        if (n.getFactionId() == null) return "";
        FactionAPI f = Global.getSector().getFaction(n.getFactionId());
        return f == null ? n.getFactionId() : f.getDisplayName();
    }

    /** "Combat — Hegemony", "Shop", ... */
    public static String title(RunNode n) {
        return n.getType().isFight() ? n.getType().label() + " — " + factionName(n) : n.getType().label();
    }

    /** Title plus Depth and, for fights, the estimated enemy strength. */
    public static String full(RunNode n) {
        String s = title(n) + " (Depth " + n.getDepth();
        if (n.getType().isFight()) {
            s += ", strength " + Math.round(DepthScaling.combatPoints(n.getDepth(), n.getType()));
        }
        return s + ")";
    }

    /** What the player finds there, for the map's info line. */
    public static String blurb(NodeType type) {
        switch (type) {
            case START:    return "Where the Descent begins.";
            case COMBAT:   return "A standard fight. Win for Supplies and a Reward pick.";
            case BOSS:     return "The Apex. No retreat. Win it to win the Run.";
            case SHOP:     return "Buy ships, weapons, officers and cheap repairs with Supplies.";
            case REST:     return "Choose one: free full repair, an S-mod, or an officer level.";
            case TREASURE: return "Choose one rare prize: a ship above your Depth, rare weapons, or an S-mod.";
            default:       return "";
        }
    }
}
