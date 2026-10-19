package com.slaythesector.spike;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

/** Spike: routes Hub/Beacon interactions, the custom battle setup and the fleet-wipe intercept. Transient. */
public class SpikeCampaignPlugin extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return SpikeIds.PLUGIN_ID;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken target) {
        if (target != null && (target.hasTag(SpikeIds.TAG_HUB) || target.hasTag(SpikeIds.TAG_BEACON))) {
            return new PluginPick<InteractionDialogPlugin>(new SpikeDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        if (opponent != null && opponent.getMemoryWithoutUpdate().contains(SpikeIds.MEM_SCENARIO)) {
            return new PluginPick<BattleCreationPlugin>(new SpikeBattleCreationPlugin(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickRespawnPlugin() {
        if (SpikeFleetSwap.isSwapped()) {
            return new PluginPick<InteractionDialogPlugin>(new SpikeRespawnDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }
}
