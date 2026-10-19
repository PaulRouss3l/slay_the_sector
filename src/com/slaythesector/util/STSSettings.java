package com.slaythesector.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.fs.starfarer.api.Global;

/** Tunables from data/config/settings.json ("sts_*" keys), with code defaults as a fallback. */
public final class STSSettings {

    private STSSettings() {}

    public static int actRows() { return i("sts_actRows", 7); }
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
    /** Relative weights of Node types on rows without a fixed type. */
    public static float weightCombat() { return f("sts_nodeWeightCombat", 6f); }
    public static float weightShop() { return f("sts_nodeWeightShop", 2f); }
    public static float weightRest() { return f("sts_nodeWeightRest", 1.5f); }
    /** Fleet DP cap of Act {@code act} (1-based); the last value covers later Acts. */
    public static int fleetDPCap(int act) {
        List<String> caps = list("sts_fleetDPCap");
        if (caps.isEmpty()) return 60 + 30 * (act - 1);
        try {
            return Integer.parseInt(caps.get(Math.min(act, caps.size()) - 1));
        } catch (NumberFormatException e) {
            return 60;
        }
    }
    public static int shopSuppliesPerDP() { return i("sts_shopSuppliesPerDP", 8); }
    public static int shopWeaponBase() { return i("sts_shopWeaponBase", 4); }
    public static float shopWeaponPerOP() { return f("sts_shopWeaponPerOP", 1.5f); }
    public static int shopOfficerBase() { return i("sts_shopOfficerBase", 30); }
    public static int shopOfficerPerLevel() { return i("sts_shopOfficerPerLevel", 10); }
    public static float shopRepairMult() { return f("sts_shopRepairMult", 0.5f); }

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
