package com.slaythesector.util;

/**
 * Central registry of string keys and ids used across the mod.
 * Keeping them in one place avoids typos in memory lookups.
 */
public final class STSConstants {

    private STSConstants() {}

    // --- Sector memory keys (per save) ---
    /** The active RunState; absent when no Run is in progress. */
    public static final String MEM_RUN_STATE = "$sts_run_state";
    /** The detached Home fleet (CampaignFleetAPI) while a Run fleet is out. */
    public static final String MEM_HOME_FLEET = "$sts_home_fleet";

    // --- Enemy fleet memory (transient: the fleet lives only while its fight is open) ---
    /** Battle setup for {@code RunBattleCreationPlugin}: {@link #BATTLE_ESCAPE} or {@link #BATTLE_PURSUIT}. */
    public static final String MEM_BATTLE = "$sts_battle";
    /** The player must escape (Elite). */
    public static final String BATTLE_ESCAPE = "escape";
    /** The enemy flees (Ambush, convoy raid). */
    public static final String BATTLE_PURSUIT = "pursuit";
    /** Fleet member id of the Ambush's marked target. */
    public static final String MEM_TARGET = "$sts_target";

    // --- Entity memory / tags ---
    /** Entity memory key holding a Node entity's node id. */
    public static final String MEM_NODE_ID = "$sts_node_id";
    public static final String TAG_HUB = "sts_hub";
    public static final String TAG_NODE = "sts_node";

    // --- Entity ids and types (data/config/custom_entities.json) ---
    public static final String HUB_ID = "sts_hub";
    public static final String HUB_ENTITY_TYPE = "sts_hub_station";
    public static final String NODE_ENTITY_TYPE = "sts_node_marker";
    public static final String RUN_SYSTEM_NAME = "The Descent";

    // --- Hullmods ---
    /** Hidden built-in hullmod on every Run fleet ship: no maintenance, no passive repair. */
    public static final String HULLMOD_RUN_LOGISTICS = "sts_run_logistics";
    /** Built into the flagship of an all-automated Run fleet so the player can command it. */
    public static final String HULLMOD_COMMAND_UPLINK = "sts_command_uplink";

    // --- Factions ---
    /** Mod faction Run enemies fly under, so campaign systems ignore Run kills. */
    public static final String FACTION_DESCENT = "sts_descent";
    /** Mod faction of allied fleets in a side battle: friendly to the player, hostile to the Descent. */
    public static final String FACTION_ALLY = "sts_ally";

    // --- Plugin ids ---
    public static final String CAMPAIGN_PLUGIN_ID = "sts_campaign_plugin";
}
