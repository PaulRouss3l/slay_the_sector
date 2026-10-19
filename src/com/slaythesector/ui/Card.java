package com.slaythesector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;

/** One card in a {@link CardGridDelegate}: a payload plus what to show. */
public class Card {

    public final Object data;
    public final String title;
    public final String subtitle;
    /** Optional ship preview; may belong to a throwaway fleet. */
    public final FleetMemberAPI preview;
    /** Cost against the grid's budget (budget mode only). */
    public final int cost;
    /** Budget mode: most copies that can be taken; 0 = no limit. */
    public final int maxCount;

    public Card(Object data, String title, String subtitle, FleetMemberAPI preview, int cost) {
        this(data, title, subtitle, preview, cost, 0);
    }

    public Card(Object data, String title, String subtitle, FleetMemberAPI preview, int cost, int maxCount) {
        this.data = data;
        this.title = title;
        this.subtitle = subtitle;
        this.preview = preview;
        this.cost = cost;
        this.maxCount = maxCount;
    }
}
