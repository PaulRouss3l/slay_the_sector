package com.slaythesector.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;

/**
 * Zero campaign impact for salaries (transient). Vanilla charges crew, marine and officer pay
 * each economy tick for whatever fleet is the player fleet (CoreScript.reportEconomyTick). During
 * a Run that is the Run fleet, while the parked Home fleet pays nothing. Each tick this adds the
 * Run fleet's pay and subtracts the Home fleet's to the Run state; {@link HomeFleet#restore()}
 * settles the total with the credits it hands back, so the campaign ends up as if no Run happened.
 */
public class RunPayrollListener implements EconomyTickListener {

    @Override
    public void reportEconomyTick(int iterIndex) {
        RunState state = RunManager.get();
        Object home = Global.getSector().getMemoryWithoutUpdate().get(STSConstants.MEM_HOME_FLEET);
        if (state == null || !(home instanceof CampaignFleetAPI)) return;
        float f = 1f / Global.getSettings().getFloat("economyIterPerMonth");
        float runPay = payroll(Global.getSector().getPlayerFleet());
        float homePay = payroll((CampaignFleetAPI) home);
        state.addPayrollRefund((runPay - homePay) * f);
    }

    /** Monthly pay of a fleet, computed as vanilla does. */
    private static float payroll(CampaignFleetAPI fleet) {
        float pay = fleet.getCargo().getCrew() * Global.getSettings().getInt("crewSalary")
                + fleet.getCargo().getMarines() * Global.getSettings().getInt("marineSalary");
        for (OfficerDataAPI od : fleet.getFleetData().getOfficersCopy()) {
            pay += Misc.getOfficerSalary(od.getPerson());
        }
        return pay;
    }

    @Override
    public void reportEconomyMonthEnd() {}
}
