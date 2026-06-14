package com.slaythesector.run;

/** Kind of a run node. */
public enum NodeType {
    START,
    COMBAT,
    ELITE,
    EVENT,
    SHOP,
    BOSS;

    /** True if arriving at this node triggers a battle. */
    public boolean isCombat() {
        return this == COMBAT || this == ELITE || this == BOSS;
    }

    /** Display name shown on the node's menu. */
    public String label() {
        switch (this) {
            case START:  return "Entry Point";
            case COMBAT: return "Hostile Contact";
            case ELITE:  return "Elite Patrol";
            case EVENT:  return "Anomaly";
            case SHOP:   return "Black Market";
            case BOSS:   return "The Apex";
            default:     return name();
        }
    }
}
