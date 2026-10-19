package com.slaythesector.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.slaythesector.util.STSSettings;

/**
 * The Fleet DP cap. Above the soft cap each DP over costs Supplies off every fight reward; above
 * the hard cap (soft + margin) no course can be set until ships are sold.
 */
public final class FleetCap {

    private FleetCap() {}

    public static int soft(RunState state) {
        return STSSettings.fleetDPCap(state.getAct());
    }

    public static int hard(RunState state) {
        return soft(state) + STSSettings.fleetDPHardMargin();
    }

    public static int over(CampaignFleetAPI fleet, RunState state) {
        return Math.max(0, RunFleet.totalDP(fleet) - soft(state));
    }

    /** Supplies taken off each fight reward. */
    public static int penalty(CampaignFleetAPI fleet, RunState state) {
        return over(fleet, state) * STSSettings.overCapSuppliesPerDP();
    }

    public static boolean blocked(CampaignFleetAPI fleet, RunState state) {
        return RunFleet.totalDP(fleet) > hard(state);
    }

    /** "Fleet DP 72 / 80 (hard cap 110)". */
    public static String summary(CampaignFleetAPI fleet, RunState state) {
        return "Fleet DP " + RunFleet.totalDP(fleet) + " / " + soft(state) + " (hard cap " + hard(state) + ")";
    }

    /** One-line warning when over the soft cap, else null. */
    public static String warning(CampaignFleetAPI fleet, RunState state) {
        if (blocked(fleet, state)) {
            return "Over the hard cap of " + hard(state) + " DP: sell ships (Fleet services) before setting a course.";
        }
        int over = over(fleet, state);
        if (over == 0) return null;
        return over + " DP over the soft cap: every fight reward is cut by " + penalty(fleet, state)
                + " supplies (" + STSSettings.overCapSuppliesPerDP() + " per DP).";
    }
}
