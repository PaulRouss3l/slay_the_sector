package com.slaythesector.run;

/** Kind of a Node. New constants go at the end: saves store enum names, but keep the order stable. */
public enum NodeType {
    START,
    COMBAT,
    BOSS,
    SHOP,
    REST,
    TREASURE,
    ELITE,
    AMBUSH,
    EVENT;

    /** Nodes whose arrival is a fight (an Event may or may not lead to one). */
    public boolean isFight() {
        return this == COMBAT || this == BOSS || this == ELITE || this == AMBUSH;
    }

    public String label() {
        switch (this) {
            case START:    return "Entry Beacon";
            case COMBAT:   return "Combat";
            case BOSS:     return "Boss";
            case SHOP:     return "Shop";
            case REST:     return "Rest";
            case TREASURE: return "Treasure";
            case ELITE:    return "Elite";
            case AMBUSH:   return "Ambush";
            case EVENT:    return "Event";
            default:       return name();
        }
    }
}
