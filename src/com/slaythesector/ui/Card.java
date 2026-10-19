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

    public Card(Object data, String title, String subtitle, FleetMemberAPI preview, int cost) {
        this.data = data;
        this.title = title;
        this.subtitle = subtitle;
        this.preview = preview;
        this.cost = cost;
    }
}
