package com.slaythesector.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.slaythesector.util.STSConstants;

/**
 * Static accessor for the current {@link RunState}, stored in sector memory.
 * Never touches meta-progression (that lives in persistent data under
 * {@link STSConstants#PDATA_META} and survives runs).
 */
public final class RunStateManager {

    private RunStateManager() {}

    private static MemoryAPI mem() {
        return Global.getSector().getMemoryWithoutUpdate();
    }

    /** Current run state, or null if no run exists. */
    public static RunState get() {
        Object o = mem().get(STSConstants.MEM_RUN_STATE);
        return (o instanceof RunState) ? (RunState) o : null;
    }

    public static boolean hasRun() {
        return get() != null;
    }

    public static void store(RunState state) {
        mem().set(STSConstants.MEM_RUN_STATE, state);
    }

    /** Drops the run state and the in-run fleet flag. Leaves meta-progression intact. */
    public static void clear() {
        mem().unset(STSConstants.MEM_RUN_STATE);
        Global.getSector().getPlayerFleet().getMemoryWithoutUpdate().unset(STSConstants.MEM_IN_RUN);
    }
}
