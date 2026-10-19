package com.slaythesector.economy;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;

/** The two Run upgrades that are never Reward picks: S-mods (Rest, Treasure) and officer levels (Rest). */
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
            if (spec == null || spec.isHidden() || spec.isHiddenEverywhere() || spec.hasTag("dmod")) continue;
            out.add(spec);
        }
        return out;
    }

    public static void buildIn(FleetMemberAPI m, String modId) {
        m.getVariant().addPermaMod(modId, true);
        m.updateStats();
    }

    /** Run officers (never the player) below their max level. */
    public static List<OfficerDataAPI> trainableOfficers(CampaignFleetAPI fleet) {
        List<OfficerDataAPI> out = new ArrayList<OfficerDataAPI>();
        OfficerLevelupPlugin plugin = levelup();
        for (OfficerDataAPI od : fleet.getFleetData().getOfficersCopy()) {
            PersonAPI p = od.getPerson();
            if (p.isPlayer() || p.isAICore()) continue;
            if (p.getStats().getLevel() >= plugin.getMaxLevel(p)) continue;
            out.add(od);
        }
        return out;
    }

    /**
     * Gives the officer exactly the XP for its next level; the player then picks the new skill in
     * the vanilla Fleet screen.
     */
    public static void train(OfficerDataAPI od) {
        PersonAPI p = od.getPerson();
        long need = levelup().getXPForLevel(p.getStats().getLevel() + 1) - p.getStats().getXP();
        if (need > 0) od.addXP(need);
    }

    private static OfficerLevelupPlugin levelup() {
        return (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
    }
}
