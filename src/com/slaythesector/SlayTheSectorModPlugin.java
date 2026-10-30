package com.slaythesector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.slaythesector.intel.HubIntel;
import com.slaythesector.intel.RunIntel;
import com.slaythesector.intel.RunResultIntel;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunFactions;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunPayrollListener;
import com.slaythesector.run.RunState;
import com.slaythesector.run.SafetyNetScript;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.RunWatchScript;
import com.slaythesector.world.HubPlacer;
import com.slaythesector.world.RunSystem;
import com.thoughtworks.xstream.XStream;

/**
 * Entry point for the Slay the Sector mod.
 * Registered via "modPlugin" in mod_info.json.
 */
public class SlayTheSectorModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        STSLog.info("application loaded");
    }

    /** Short, stable save names: these classes are saved and must never be renamed after release. */
    @Override
    public void configureXStream(XStream x) {
        x.alias("STS_RunState", RunState.class);
        x.alias("STS_RunNode", RunNode.class);
        x.alias("STS_NodeType", NodeType.class);
        x.alias("STS_HubIntel", HubIntel.class);
        x.alias("STS_RunIntel", RunIntel.class);
        x.alias("STS_RunResultIntel", RunResultIntel.class);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        STSLog.info("game loaded (newGame=" + newGame + ")");
        SectorAPI sector = Global.getSector();

        if (STSSettings.prepareRemoval()) {
            String blocker = ModRemoval.blocker();
            if (blocker == null) {
                ModRemoval.run();
                sector.addTransientScript(new ModRemoval.Notice("Slay the Sector removed from this save. "
                        + "Save, quit, then disable the mod in the launcher.", true));
                return;
            }
            STSLog.warn("sts_prepareRemoval set but " + blocker);
            sector.addTransientScript(new ModRemoval.Notice("Slay the Sector can't be removed yet: " + blocker + ".", false));
        }

        // Transient hooks, re-added every load.
        sector.unregisterPlugin(STSConstants.CAMPAIGN_PLUGIN_ID);
        sector.registerPlugin(new RunCampaignPlugin());
        if (!sector.hasTransientScript(RunWatchScript.class)) sector.addTransientScript(new RunWatchScript());
        sector.addTransientScript(new SafetyNetScript());
        if (!sector.getListenerManager().hasListenerOfClass(RunPayrollListener.class)) {
            sector.getListenerManager().addListener(new RunPayrollListener(), true);
        }

        // Run enemies fly under the Descent so commissions and bounties ignore their kills.
        RunFactions.apply();

        HubPlacer.ensureHub();
        // Older saves created the Descent far outside the Sector: move it in (see placeInBounds).
        if (RunSystem.get() != null) RunSystem.placeInBounds(RunSystem.get());
        RunIntel.ensure();
    }
}
