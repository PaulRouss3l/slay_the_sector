package com.slaythesector.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.dialog.RunDialogPlugin;

/**
 * Starts a run by hijacking the current (hub) interaction dialog into the
 * {@link RunDialogPlugin}. The whole descent then plays out in this dialog — no
 * star system, no fleet movement. Fired by rules.csv on {@code $option == sts_enter}.
 */
public class STS_EnterRun extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }
        RunDialogPlugin plugin = new RunDialogPlugin();
        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }
}
