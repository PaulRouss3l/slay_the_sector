package com.slaythesector.run;

/** Kind of a Node. S1 uses START, COMBAT and BOSS; the rest arrive in S2/S3. */
public enum NodeType {
    START,
    COMBAT,
    BOSS;

    public boolean isFight() {
        return this == COMBAT || this == BOSS;
    }

    public String label() {
        switch (this) {
            case START:  return "Entry Beacon";
            case COMBAT: return "Combat";
            case BOSS:   return "Boss";
            default:     return name();
        }
    }
}
