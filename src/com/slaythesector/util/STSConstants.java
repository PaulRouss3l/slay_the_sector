package com.slaythesector.util;

/**
 * Central registry of string keys and ids used across the mod.
 * Keeping them in one place avoids typos in memory/persistent-data lookups.
 */
public final class STSConstants {

    private STSConstants() {}

    // --- Sector memory keys (per-save; cleared when a run ends) ---
    /** Sector memory key holding the serialized RunState. */
    public static final String MEM_RUN_STATE = "$sts_run_state";
    /** Player-fleet memory flag set while inside a run. */
    public static final String MEM_IN_RUN = "$sts_in_run";
    /** Entity memory key tagging a node entity with its node id. */
    public static final String MEM_NODE_ID = "$sts_node_id";
    /** Entity memory flag marking an entity as a run node (routes rules.csv). */
    public static final String MEM_NODE = "$sts_node";

    // --- Persistent data keys (survive across runs) ---
    /** Persistent data key holding the serialized MetaProgress. */
    public static final String PDATA_META = "sts_meta";

    // --- Vanilla ids we depend on ---
    /** Transverse Jump ability id (NOT "transverse_jump"). */
    public static final String ABILITY_TRANSVERSE_JUMP = "fracture_jump";
    /** Player-fleet memory flag that blocks jumping. */
    public static final String MEM_NO_JUMP = "$cfai_noJump";

    // --- Mod-defined ids ---
    /** Display name for a generated run system. */
    public static final String RUN_SYSTEM_NAME = "The Sector";
    /** Custom-entity type reused for node markers (MVP). */
    public static final String NODE_ENTITY_TYPE = "stable_location";
}
