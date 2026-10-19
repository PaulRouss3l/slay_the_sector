package com.slaythesector.economy;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSSettings;

/**
 * Run upgrades that are never Reward picks: S-mods and D-mod removal (Rest, Treasure, Shop) and
 * officer levels (Rest).
 */
public final class Upgrades {

    private Upgrades() {}

    /** Ships that have a free S-mod slot and at least one hullmod that could be built in. */
    public static List<FleetMemberAPI> smodShips(CampaignFleetAPI fleet) {
        List<FleetMemberAPI> out = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (!smodChoices(m).isEmpty()) out.add(m);
        }
        return out;
    }

    /** Installed hullmods of {@code m} that can become S-mods; empty when its S-mod slots are full. */
    public static List<HullModSpecAPI> smodChoices(FleetMemberAPI m) {
        List<HullModSpecAPI> out = new ArrayList<HullModSpecAPI>();
        ShipVariantAPI v = m.getVariant();
        int max = Misc.getMaxPermanentMods(m, Global.getSector().getPlayerStats());
        if (Misc.getCurrSpecialMods(v) >= max) return out;
        for (String id : v.getNonBuiltInHullmods()) {
            if (v.getSMods().contains(id) || STSConstants.HULLMOD_RUN_LOGISTICS.equals(id)) continue;
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec == null || spec.isHidden() || spec.isHiddenEverywhere() || spec.hasTag(Tags.HULLMOD_DMOD)
                    || spec.hasTag(Tags.HULLMOD_NO_BUILD_IN)) continue;
            out.add(spec);
        }
        return out;
    }

    public static void buildIn(FleetMemberAPI m, String modId) {
        m.getVariant().addPermaMod(modId, true);
        m.updateStats();
    }

    /** Ships carrying at least one D-mod. */
    public static List<FleetMemberAPI> dmodShips(CampaignFleetAPI fleet) {
        List<FleetMemberAPI> out = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (!dmods(m).isEmpty()) out.add(m);
        }
        return out;
    }

    public static List<HullModSpecAPI> dmods(FleetMemberAPI m) {
        List<HullModSpecAPI> out = new ArrayList<HullModSpecAPI>();
        for (String id : m.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec != null && spec.hasTag(Tags.HULLMOD_DMOD)) out.add(spec);
        }
        return out;
    }

    /** Removes one D-mod the vanilla way; the last one gone turns a "(D)" hull back into the clean hull. */
    public static void removeDMod(FleetMemberAPI m, String modId) {
        ShipVariantAPI v = m.getVariant();
        DModManager.removeDMod(v, modId);
        if (DModManager.getNumDMods(v) == 0) FieldRepairsScript.restoreToNonDHull(v);
        m.updateStats();
    }

    public static void removeAllDMods(FleetMemberAPI m) {
        for (HullModSpecAPI spec : dmods(m)) removeDMod(m, spec.getId());
    }

    /** Shop price to remove one D-mod from {@code m}. */
    public static int dmodPrice(FleetMemberAPI m) {
        return Math.round(STSSettings.shopDModBase() + m.getDeploymentPointsCost() * STSSettings.shopDModPerDP());
    }

    /** Run officers (never the player) below their max level, with no level-up already waiting. */
    public static List<OfficerDataAPI> trainableOfficers(CampaignFleetAPI fleet) {
        List<OfficerDataAPI> out = new ArrayList<OfficerDataAPI>();
        OfficerLevelupPlugin plugin = levelup();
        for (OfficerDataAPI od : fleet.getFleetData().getOfficersCopy()) {
            PersonAPI p = od.getPerson();
            if (p.isPlayer() || p.isAICore()) continue;
            if (p.getStats().getLevel() >= plugin.getMaxLevel(p) || od.canLevelUp()) continue;
            out.add(od);
        }
        return out;
    }

    /**
     * Gives the officer exactly the XP for its next level; the player then picks the new skill in
     * the vanilla Fleet screen.
     */
    public static boolean train(OfficerDataAPI od) {
        PersonAPI p = od.getPerson();
        long need = levelup().getXPForLevel(p.getStats().getLevel() + 1) - p.getStats().getXP();
        if (need <= 0) return false;
        od.addXP(need);
        return true;
    }

    private static OfficerLevelupPlugin levelup() {
        return (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
    }
}
