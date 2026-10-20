package com.slaythesector.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;

/**
 * Hover tooltip on a ship card's name: hull, DP, system and ordnance points; F1 expands it to the
 * full fit (weapons, fighter wings, hullmods), like the vanilla ship tooltips. The ship icon keeps
 * vanilla's own tooltip.
 */
public class ShipFitTooltip implements TooltipCreator {

    private static final float WIDTH = 420f;

    private final FleetMemberAPI member;

    public ShipFitTooltip(FleetMemberAPI member) {
        this.member = member;
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return true;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        ShipVariantAPI v = member.getVariant();
        ShipHullSpecAPI hull = member.getHullSpec();
        float pad = 3f;
        float opad = 10f;

        tooltip.addTitle(hull.getHullName() + " (" + hull.getDesignation() + ")");
        tooltip.addPara("%s DP. Variant: %s.", pad, Misc.getHighlightColor(),
                String.valueOf((int) member.getDeploymentPointsCost()), v.getDisplayName());
        if (hull.getShipSystemId() != null && !hull.getShipSystemId().isEmpty()) {
            ShipSystemSpecAPI system = Global.getSettings().getShipSystemSpec(hull.getShipSystemId());
            if (system != null) tooltip.addPara("System: %s.", pad, Misc.getHighlightColor(), system.getName());
        }
        tooltip.addPara("Ordnance points: %s / %s.", pad, Misc.getHighlightColor(),
                String.valueOf(v.computeOPCost(Global.getSector().getPlayerStats())), String.valueOf(hull.getOrdnancePoints(Global.getSector().getPlayerStats())));

        if (!expanded) {
            tooltip.addPara("Press F1 to see the weapons and hullmods.", Misc.getGrayColor(), opad);
            return;
        }

        tooltip.addSectionHeading("Weapons", Alignment.MID, opad);
        List<String> weapons = counted(weaponNames(v));
        if (weapons.isEmpty()) tooltip.addPara("None.", pad);
        for (String w : weapons) tooltip.addPara(w, pad);

        List<String> wings = new ArrayList<String>();
        for (String id : v.getFittedWings()) {
            FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(id);
            wings.add(spec == null ? id : spec.getWingName());
        }
        if (!wings.isEmpty()) {
            tooltip.addSectionHeading("Fighter wings", Alignment.MID, opad);
            for (String w : counted(wings)) tooltip.addPara(w, pad);
        }

        tooltip.addSectionHeading("Hullmods", Alignment.MID, opad);
        boolean any = false;
        for (String id : v.getHullMods()) {
            if (STSConstants.HULLMOD_RUN_LOGISTICS.equals(id)) continue;
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (spec == null || spec.isHidden() || spec.isHiddenEverywhere()) continue;
            String tag = hull.getBuiltInMods().contains(id) ? " (built-in)" : v.getSMods().contains(id) ? " (S-mod)" : "";
            tooltip.addPara(spec.getDisplayName() + tag, pad, Misc.getGrayColor(), Misc.getHighlightColor(), tag.trim());
            any = true;
        }
        if (!any) tooltip.addPara("None.", pad);
    }

    private static List<String> weaponNames(ShipVariantAPI v) {
        List<String> names = new ArrayList<String>();
        for (String slot : v.getFittedWeaponSlots()) {
            WeaponSlotAPI s = v.getSlot(slot);
            if (s != null && (s.isDecorative() || s.isSystemSlot() || s.isStationModule())) continue;
            String id = v.getWeaponId(slot);
            WeaponSpecAPI spec = id == null ? null : Global.getSettings().getWeaponSpec(id);
            if (spec != null) names.add(spec.getWeaponName());
        }
        return names;
    }

    /** "2x Heavy Mauler", in first-seen order. */
    private static List<String> counted(List<String> names) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String n : names) {
            Integer c = counts.get(n);
            counts.put(n, c == null ? 1 : c + 1);
        }
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            out.add((e.getValue() > 1 ? e.getValue() + "x " : "") + e.getKey());
        }
        return out;
    }
}
