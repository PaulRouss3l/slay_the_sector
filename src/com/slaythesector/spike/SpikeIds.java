package com.slaythesector.spike;

/**
 * Ids for the Spike milestone (throwaway code proving risky mechanisms in game).
 * See docs/ROADMAP.md "Spike".
 */
public final class SpikeIds {

    private SpikeIds() {}

    public static final String PLUGIN_ID = "sts_spike_campaign_plugin";

    public static final String ENTITY_TYPE = "sts_spike_station";
    public static final String HUB_ID = "sts_spike_hub";
    public static final String BEACON_ID = "sts_spike_beacon";
    public static final String TAG_HUB = "sts_spike_hub";
    public static final String TAG_BEACON = "sts_spike_beacon";

    public static final String RUN_SYSTEM_NAME = "STS Spike Depths";

    /** Sector memory: the detached Home fleet (CampaignFleetAPI) while swapped out. */
    public static final String MEM_HOME_FLEET = "$sts_spike_homeFleet";
    /** Fleet memory: marks enemy fleets that get the spike battle setup. */
    public static final String MEM_SCENARIO = "$sts_scenario";

    public static final String STARTER_VARIANT = "wolf_Starting";
    public static final String SECOND_VARIANT = "hammerhead_Balanced";
}
