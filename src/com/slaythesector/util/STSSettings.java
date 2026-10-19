package com.slaythesector.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.fs.starfarer.api.Global;

/** Tunables from data/config/settings.json ("sts_*" keys), with code defaults as a fallback. */
public final class STSSettings {

    private STSSettings() {}

    public static int actRows() { return i("sts_actRows", 6); }
    public static int startingDP() { return i("sts_startingDP", 30); }
    public static int startingSupplies() { return i("sts_startingSupplies", 150); }
    public static float enemyBaseFP() { return f("sts_enemyBaseFP", 15f); }
    public static float enemyFPPerDepth() { return f("sts_enemyFPPerDepth", 6f); }
    public static float bossFPMult() { return f("sts_bossFPMult", 1.65f); }
    public static float crRegenFraction() { return f("sts_crRegenFraction", 0.7f); }
    public static float repairCostMult() { return f("sts_repairCostMult", 1f); }
    public static int sellSuppliesPerDP() { return i("sts_sellSuppliesPerDP", 4); }
    public static float cargoPerShip() { return f("sts_cargoPerShip", 300f); }
    public static int rewardSuppliesCombat() { return i("sts_rewardSuppliesCombat", 30); }
    public static int rewardSuppliesBoss() { return i("sts_rewardSuppliesBoss", 80); }
    public static int payoutMinDepth() { return i("sts_payoutMinDepth", 3); }
    public static int payoutAtMinDepth() { return i("sts_payoutAtMinDepth", 5000); }
    public static int payoutAtBoss() { return i("sts_payoutAtBoss", 25000); }
    public static int payoutWin() { return i("sts_payoutWin", 50000); }
    public static int minKnownShips() { return i("sts_minKnownShips", 8); }
    public static List<String> bossFactions() { return list("sts_bossFactions"); }
    public static List<String> factionBlacklist() { return list("sts_factionBlacklist"); }

    private static int i(String key, int def) {
        try {
            return Global.getSettings().getInt(key);
        } catch (Exception e) {
            return def;
        }
    }

    private static float f(String key, float def) {
        try {
            return Global.getSettings().getFloat(key);
        } catch (Exception e) {
            return def;
        }
    }

    private static List<String> list(String key) {
        List<String> out = new ArrayList<String>();
        try {
            JSONArray arr = Global.getSettings().getJSONArray(key);
            for (int k = 0; k < arr.length(); k++) {
                out.add(arr.getString(k));
            }
        } catch (Exception e) {
            STSLog.warn("settings: " + key + " missing or malformed, using empty list");
        }
        return out;
    }
}
