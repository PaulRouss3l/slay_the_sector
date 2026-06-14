package com.slaythesector;

import com.fs.starfarer.api.BaseModPlugin;
import com.slaythesector.intel.RunIntelPlugin;
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
    }
}
