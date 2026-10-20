package com.slaythesector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.slaythesector.intel.HubIntel;
import com.slaythesector.intel.RunIntel;
import com.slaythesector.intel.RunResultIntel;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunPayrollListener;
import com.slaythesector.run.RunState;
import com.slaythesector.run.SafetyNetScript;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.world.RunWatchScript;
import com.slaythesector.world.HubPlacer;
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

        // Transient hooks, re-added every load.
        sector.unregisterPlugin(STSConstants.CAMPAIGN_PLUGIN_ID);
        sector.registerPlugin(new RunCampaignPlugin());
        if (!sector.hasTransientScript(RunWatchScript.class)) sector.addTransientScript(new RunWatchScript());
        sector.addTransientScript(new SafetyNetScript());
        if (!sector.getListenerManager().hasListenerOfClass(RunPayrollListener.class)) {
            sector.getListenerManager().addListener(new RunPayrollListener(), true);
        }

        // Run enemies fly under the Descent so commissions and bounties ignore their kills.
        FactionAPI descent = sector.getFaction(STSConstants.FACTION_DESCENT);
        if (descent != null) descent.setRelationship(Factions.PLAYER, RepLevel.VENGEFUL);
        FactionAPI ally = sector.getFaction(STSConstants.FACTION_ALLY);
        if (ally != null) {
            ally.setRelationship(Factions.PLAYER, RepLevel.COOPERATIVE);
            ally.setRelationship(STSConstants.FACTION_DESCENT, RepLevel.VENGEFUL);
        }

        HubPlacer.ensureHub();
        RunIntel.ensure();
    }
}
