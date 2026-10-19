package com.slaythesector.spike;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.slaythesector.util.STSLog;

/**
 * Spike: replaces the vanilla "fleet destroyed" respawn while the Run fleet is out
 * (picked by {@link SpikeCampaignPlugin#pickRespawnPlugin}). Ends the "Run": Home fleet back,
 * player sent to the Hub.
 */
public class SpikeRespawnDialog implements InteractionDialogPlugin {

    private static final String OPT_RETURN = "sts_spike_respawn_return";

    private InteractionDialogAPI dialog;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        STSLog.info("spike: respawn intercepted");
        dialog.getTextPanel().addPara("[STS spike] Your Run fleet was destroyed. The Run is lost.");
        dialog.getTextPanel().addPara("Your Home fleet is waiting at the Descent Relay.");
        dialog.getOptionPanel().addOption("Return to the Hub", OPT_RETURN);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!OPT_RETURN.equals(optionData)) return;
        SpikeFleetSwap.restoreHomeFleet();
        dialog.dismiss();
        SectorEntityToken hub = SpikeWorld.ensureHub();
        if (hub != null) {
            SpikeWorld.jumpTo(null, hub);
        } else {
            STSLog.warn("spike: no hub found after respawn");
        }
        Global.getSector().setPaused(false);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) {}
    @Override public void advance(float amount) {}
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
