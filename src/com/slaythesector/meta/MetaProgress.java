package com.slaythesector.meta;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.RunState;
import com.slaythesector.util.STSLog;

/**
 * Cross-save Meta progress, in {@code saves/common/} (never in a save): Records and Ascension per
 * Archetype, and the starting bonuses unlocked. Read and written on demand; a missing or broken
 * file reads as a fresh profile.
 */
public final class MetaProgress {

    private static final String FILE = "slay_the_sector_meta.json";

    public static final String BONUS_DP = "startDP";
    public static final String BONUS_OFFICER = "officerLevel";
    public static final String BONUS_SUPPLIES = "supplies";

    private final JSONObject root;

    private MetaProgress(JSONObject root) {
        this.root = root;
    }

    public static MetaProgress load() {
        try {
            if (Global.getSettings().fileExistsInCommon(FILE)) {
                return new MetaProgress(Global.getSettings().readJSONFromCommon(FILE, false));
            }
        } catch (Exception e) {
            STSLog.warn("Meta progress unreadable, starting fresh: " + e);
        }
        return new MetaProgress(new JSONObject());
    }

    private void save() {
        try {
            Global.getSettings().writeJSONToCommon(FILE, root, false);
        } catch (Exception e) {
            STSLog.warn("Meta progress not saved: " + e);
        }
    }

    // ---- Records -------------------------------------------------------------------------------

    private JSONObject archetype(String id) {
        try {
            JSONObject all = root.optJSONObject("archetypes");
            if (all == null) {
                all = new JSONObject();
                root.put("archetypes", all);
            }
            JSONObject a = all.optJSONObject(id);
            if (a == null) {
                a = new JSONObject();
                all.put(id, a);
            }
            return a;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public int runs(String archetypeId) { return archetype(archetypeId).optInt("runs", 0); }
    public int wins(String archetypeId) { return archetype(archetypeId).optInt("wins", 0); }
    public int bestDepth(String archetypeId) { return archetype(archetypeId).optInt("bestDepth", 0); }
    public boolean omegaCleared(String archetypeId) { return archetype(archetypeId).optBoolean("omegaCleared", false); }

    /** Highest Ascension the player may pick for this Archetype. */
    public int ascensionUnlocked(String archetypeId) {
        return Math.min(Ascension.MAX, archetype(archetypeId).optInt("ascensionUnlocked", 0));
    }

    /** Highest Ascension won with this Archetype, -1 if none. */
    public int bestAscensionWon(String archetypeId) {
        return archetype(archetypeId).optInt("bestAscensionWon", -1);
    }

    /** Archetype ids with at least one Run. */
    @SuppressWarnings("unchecked")
    public Iterator<String> archetypeIds() {
        JSONObject all = root.optJSONObject("archetypes");
        return all == null ? new JSONObject().keys() : all.keys();
    }

    // ---- Starting bonuses ----------------------------------------------------------------------

    public boolean bonusUnlocked(String bonus) {
        JSONObject b = root.optJSONObject("bonuses");
        return b != null && b.optBoolean(bonus, false);
    }

    private void unlock(String bonus) throws JSONException {
        JSONObject b = root.optJSONObject("bonuses");
        if (b == null) {
            b = new JSONObject();
            root.put("bonuses", b);
        }
        if (!b.optBoolean(bonus, false)) STSLog.info("Meta progress: starting bonus unlocked: " + bonus);
        b.put(bonus, true);
    }

    // ---- Run end -------------------------------------------------------------------------------

    /**
     * Records a finished Run: counts, deepest Depth, Ascension unlock (winning at the highest level
     * reached opens the next), Omega cleared, and the starting-bonus milestones.
     */
    public void recordRun(RunState state, boolean won) {
        try {
            String id = state.getArchetypeId();
            JSONObject a = archetype(id);
            a.put("runs", a.optInt("runs", 0) + 1);
            a.put("bestDepth", Math.max(a.optInt("bestDepth", 0), state.getMaxDepthReached()));
            if (state.getAct() >= 3) unlock(BONUS_OFFICER);
            if (won) {
                a.put("wins", a.optInt("wins", 0) + 1);
                a.put("bestAscensionWon", Math.max(a.optInt("bestAscensionWon", -1), state.getAscension()));
                if (state.getAscension() >= a.optInt("ascensionUnlocked", 0)) {
                    a.put("ascensionUnlocked", Math.min(Ascension.MAX, state.getAscension() + 1));
                }
                unlock(BONUS_DP);
                JSONArray winners = root.optJSONArray("winArchetypes");
                if (winners == null) {
                    winners = new JSONArray();
                    root.put("winArchetypes", winners);
                }
                boolean seen = false;
                for (int i = 0; i < winners.length(); i++) {
                    if (id.equals(winners.optString(i))) seen = true;
                }
                if (!seen) winners.put(id);
                if (winners.length() >= 3) unlock(BONUS_SUPPLIES);
            }
            if (state.isTrueBossWon()) a.put("omegaCleared", true);
        } catch (JSONException e) {
            STSLog.warn("Meta progress not recorded: " + e);
            return;
        }
        save();
    }
}
