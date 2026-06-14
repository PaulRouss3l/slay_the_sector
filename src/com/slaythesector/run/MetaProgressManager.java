package com.slaythesector.run;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;

/**
 * Static accessor for {@link MetaProgress}, stored in sector persistent data so it
 * survives across runs (and is independent of any single run's state).
 */
public final class MetaProgressManager {

    private MetaProgressManager() {}

    /** Ship variants unlocked one-by-one as the player wins runs. */
    private static final String[] UNLOCK_POOL = {
        "sunder_Assault",
        "hammerhead_Balanced",
        "enforcer_Assault",
        "dominator_Assault",
    };

    /** Returns the meta store, creating and persisting a fresh one on first use. */
    public static MetaProgress get() {
        Map<String, Object> p = Global.getSector().getPersistentData();
        Object o = p.get(STSConstants.PDATA_META);
        if (o instanceof MetaProgress) {
            return (MetaProgress) o;
        }
        MetaProgress meta = new MetaProgress();
        p.put(STSConstants.PDATA_META, meta);
        return meta;
    }

    private static void store(MetaProgress meta) {
        Global.getSector().getPersistentData().put(STSConstants.PDATA_META, meta);
    }

    /** Called at run start. */
    public static void onRunStarted() {
        MetaProgress meta = get();
        meta.incRunsStarted();
        store(meta);
    }

    /** Called on boss victory: record the win and unlock the next ship in the pool. */
    public static void onRunWon(int reachedDepth) {
        MetaProgress meta = get();
        meta.incRunsWon();
        meta.recordDepth(reachedDepth);
        for (String variant : UNLOCK_POOL) {
            if (!meta.getUnlockedShipVariants().contains(variant)) {
                meta.getUnlockedShipVariants().add(variant);
                STSLog.info("meta: unlocked " + variant);
                break;
            }
        }
        store(meta);
    }

    /** Called on loss/abandon: just record how deep the player got. */
    public static void onRunEnded(int reachedDepth) {
        MetaProgress meta = get();
        meta.recordDepth(reachedDepth);
        store(meta);
    }
}
