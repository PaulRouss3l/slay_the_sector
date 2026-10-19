package com.slaythesector.combat;

import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunState;

/** Replaces the vanilla "fleet destroyed" flow while a Run is active: the Run is lost, Home fleet restored. */
public class RunRespawnDialog implements InteractionDialogPlugin {

    private static final String OPT_RETURN = "sts_respawn_return";

    private InteractionDialogAPI dialog;
    private boolean won;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        RunState state = RunManager.get();
        int depth = state == null ? 0 : state.getMaxDepthWon();
        // Destroyed in Act 4: the Run was already won in Act 3.
        won = state != null && state.isAct3Won();
        dialog.getTextPanel().addPara("Your Run fleet has been destroyed. " + (won ? "The true Boss stands, but the Run was won."
                : "The Run is lost."), won ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
        dialog.getTextPanel().addPara("Deepest Node won: %s. Payout: %s.", Misc.getHighlightColor(),
                String.valueOf(depth), Misc.getDGSCredits(RunManager.payout(state, won)));
        dialog.getOptionPanel().addOption("Return to the Hub", OPT_RETURN);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!OPT_RETURN.equals(optionData)) return;
        RunManager.end(won);
        dialog.dismiss();
        RunManager.returnToHub();
    }

    @Override public void optionMousedOver(String optionText, Object optionData) {}
    @Override public void advance(float amount) {}
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
