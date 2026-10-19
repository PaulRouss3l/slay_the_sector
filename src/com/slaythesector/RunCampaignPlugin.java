package com.slaythesector;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.slaythesector.combat.RunRespawnDialog;
import com.slaythesector.hub.HubDialog;
import com.slaythesector.nodes.NodeDialog;
import com.slaythesector.run.HomeFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.util.STSConstants;

/** Routes Hub and Node interactions to our dialogs, and intercepts a fleet wipe during a Run. Transient. */
public class RunCampaignPlugin extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return STSConstants.CAMPAIGN_PLUGIN_ID;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken target) {
        if (target == null) return null;
        if (target.hasTag(STSConstants.TAG_HUB)) {
            return new PluginPick<InteractionDialogPlugin>(new HubDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (target.hasTag(STSConstants.TAG_NODE)) {
            return new PluginPick<InteractionDialogPlugin>(new NodeDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickRespawnPlugin() {
        if (RunManager.isActive() && HomeFleet.isStashed()) {
            return new PluginPick<InteractionDialogPlugin>(new RunRespawnDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }
}
