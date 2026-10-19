package com.slaythesector.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.fs.starfarer.api.Global;

/** Tunables from data/config/settings.json ("sts_*" keys), with code defaults as a fallback. */
public final class STSSettings {

    private STSSettings() {}

    public static int actRows() { return i("sts_actRows", 10); }
    public static int mapColumns() { return i("sts_mapColumns", 5); }
    public static int mapPaths() { return i("sts_mapPaths", 4); }
    public static int startingDP() { return i("sts_startingDP", 30); }
    public static int startingSupplies() { return i("sts_startingSupplies", 150); }
    public static float enemyBaseFP() { return f("sts_enemyBaseFP", 15f); }
    public static float enemyFPPerDepth() { return f("sts_enemyFPPerDepth", 4.5f); }
    public static float bossFPMult() { return f("sts_bossFPMult", 1.3f); }
    /** Boss pilots: "none", "gamma" or "normal" (see EncounterFactory). */
    public static String bossOfficers() {
        try {
            return Global.getSettings().getString("sts_bossOfficers");
        } catch (Exception e) {
            return "none";
        }
    }
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
    public static float weightCombat() { return f("sts_nodeWeightCombat", 5f); }
    public static float weightShop() { return f("sts_nodeWeightShop", 1.5f); }
    public static float weightRest() { return f("sts_nodeWeightRest", 1.5f); }
    public static float weightElite() { return f("sts_nodeWeightElite", 1f); }
    public static float weightAmbush() { return f("sts_nodeWeightAmbush", 1f); }
    public static float weightEvent() { return f("sts_nodeWeightEvent", 2f); }
    /** Elite: enemy strength × this; escaping with at least this share of the fleet's DP is a win. */
    public static float eliteFPMult() { return f("sts_eliteFPMult", 1.5f); }
    public static float eliteEscapeFraction() { return f("sts_eliteEscapeFraction", 0.6f); }
    public static int rewardSuppliesElite() { return i("sts_rewardSuppliesElite", 60); }
    /** Ambush: supplies when the marked target escapes (no Reward pick). */
    public static int ambushEscapedSupplies() { return i("sts_ambushEscapedSupplies", 15); }
    public static int sabotageInspectCost() { return i("sts_sabotageInspectCost", 15); }
    public static float sabotageChance() { return f("sts_sabotageChance", 0.5f); }
    public static float sabotageLoss() { return f("sts_sabotageLoss", 0.2f); }
    public static int stationRewardPicks() { return i("sts_stationRewardPicks", 3); }
    public static float convoySuppliesPerDP() { return f("sts_convoySuppliesPerDP", 3f); }
    /** Convoy raid: destroying at least this share of the convoy's DP also gives a Reward pick. */
    public static float convoyRewardFraction() { return f("sts_convoyRewardFraction", 0.5f); }
    public static float sideAllyFPMult() { return f("sts_sideAllyFPMult", 0.8f); }
    /** Hard cap = soft cap + this; above it no course can be set. */
    public static int fleetDPHardMargin() { return i("sts_fleetDPHardMargin", 30); }
    /** Supplies cut from each fight reward per DP over the soft cap. */
    public static int overCapSuppliesPerDP() { return i("sts_overCapSuppliesPerDP", 5); }
    /** Soft Fleet DP cap of Act {@code act} (1-based); the last value covers later Acts. */
    public static int fleetDPCap(int act) {
        List<String> caps = list("sts_fleetDPCap");
        if (caps.isEmpty()) return 80 + 30 * (act - 1);
        try {
            return Integer.parseInt(caps.get(Math.min(act, caps.size()) - 1));
        } catch (NumberFormatException e) {
            return 80;
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
