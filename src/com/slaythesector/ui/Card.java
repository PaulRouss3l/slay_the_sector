package com.slaythesector.ui;

import java.awt.Color;

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
    /** Optional extra line under the subtitle (e.g. the Fleet DP after taking a ship), and its colour. */
    public String note;
    public Color noteColor;

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

    public Card withNote(String note, Color color) {
        this.note = note;
        this.noteColor = color;
        return this;
    }
}
