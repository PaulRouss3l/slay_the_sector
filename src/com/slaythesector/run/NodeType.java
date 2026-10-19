package com.slaythesector.run;

/** Kind of a Node. Elite, Ambush and Event arrive in S3. */
public enum NodeType {
    START,
    COMBAT,
    BOSS,
    SHOP,
    REST,
    TREASURE;

    public boolean isFight() {
        return this == COMBAT || this == BOSS;
    }

    public String label() {
        switch (this) {
            case START:    return "Entry Beacon";
            case COMBAT:   return "Combat";
            case BOSS:     return "Boss";
            case SHOP:     return "Shop";
            case REST:     return "Rest";
            case TREASURE: return "Treasure";
            default:       return name();
        }
    }
}
