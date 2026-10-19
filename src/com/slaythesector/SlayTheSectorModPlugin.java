package com.slaythesector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.slaythesector.intel.RunIntelPlugin;
import com.slaythesector.spike.SpikeCampaignPlugin;
import com.slaythesector.spike.SpikeFtlLock;
import com.slaythesector.spike.SpikeIds;
import com.slaythesector.spike.SpikeWorld;
import com.slaythesector.util.STSLog;

/**
 * Entry point for the Slay the Sector mod.
 * Registered via "modPlugin" in mod_info.json.
 */
public class SlayTheSectorModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        STSLog.info("application loaded");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        STSLog.info("game loaded (newGame=" + newGame + ")");

        // Persistent "Sector Descent" intel (points to the hub world; describes a run when active).
        RunIntelPlugin.ensure();

        // Spike milestone (docs/ROADMAP.md): transient hooks, re-added every load.
        SectorAPI sector = Global.getSector();
        sector.unregisterPlugin(SpikeIds.PLUGIN_ID);
        sector.registerPlugin(new SpikeCampaignPlugin());
        if (!sector.hasTransientScript(SpikeFtlLock.class)) {
            sector.addTransientScript(new SpikeFtlLock());
        }
        SpikeWorld.ensureHub();
    }
}
