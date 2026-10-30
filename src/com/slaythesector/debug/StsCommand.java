package com.slaythesector.debug;

import java.util.ArrayList;
import java.util.List;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.slaythesector.ModRemoval;
import com.slaythesector.meta.MetaProgress;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.FleetCap;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunState;
import com.slaythesector.world.RunSystem;

/**
 * Debug command for the Console Commands mod (registered in data/console/commands.csv). Console
 * loads it only when it is enabled, so players without Console never load this class.
 *
 *   sts status            Run state
 *   sts unlock [level]    Ascension up to level (0-6, default 6 = Omega) for every faction, all bonuses
 *   sts act <2-4>         during a Run, skip to that Act's Entry Beacon (4 = Omega Act 4)
 *   sts supplies <n>      give the Run fleet n supplies
 *   sts remove            strip the mod from this save so it can be removed (ModRemoval)
 */
public class StsCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Slay the Sector commands work on the campaign map only.");
            return CommandResult.WRONG_CONTEXT;
        }
        String[] a = args.trim().isEmpty() ? new String[0] : args.trim().split("\\s+");
        if (a.length == 0) return CommandResult.BAD_SYNTAX;
        try {
            switch (a[0].toLowerCase()) {
                case "status":
                    return status();
                case "unlock":
                    return unlock(a.length > 1 ? Integer.parseInt(a[1]) : Ascension.MAX);
                case "act":
                    if (a.length < 2) return CommandResult.BAD_SYNTAX;
                    return act(Integer.parseInt(a[1]));
                case "supplies":
                    if (a.length < 2) return CommandResult.BAD_SYNTAX;
                    return supplies(Integer.parseInt(a[1]));
                case "remove":
                    return remove();
                default:
                    return CommandResult.BAD_SYNTAX;
            }
        } catch (NumberFormatException e) {
            return CommandResult.BAD_SYNTAX;
        }
    }

    private CommandResult status() {
        RunState state = RunManager.get();
        if (state == null) {
            Console.showMessage("No Run in progress.");
            return CommandResult.SUCCESS;
        }
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        Console.showMessage("Run: " + state.getArchetypeId() + ", Act " + state.getAct() + " of " + state.getFinalAct()
                + ", Ascension " + Ascension.name(state.getAscension()) + ", deepest Node won " + state.getMaxDepthWon()
                + ", supplies " + (int) pf.getCargo().getSupplies() + ", " + FleetCap.summary(pf, state)
                + (state.isAct3Won() ? ", Act 3 won" : ""));
        return CommandResult.SUCCESS;
    }

    private CommandResult unlock(int level) {
        List<String> ids = new ArrayList<String>();
        for (FactionAPI f : Archetypes.eligible()) ids.add(f.getId());
        MetaProgress.load().debugUnlock(ids, level);
        Console.showMessage("Unlocked Ascension up to " + Ascension.name(level) + " for " + ids.size()
                + " factions, and every starting bonus.");
        return CommandResult.SUCCESS;
    }

    private CommandResult act(int act) {
        RunState state = RunManager.get();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        if (state == null || !RunSystem.isRunSystem(pf.getContainingLocation())) {
            Console.showMessage("Start a Run and jump into the Descent first.");
            return CommandResult.ERROR;
        }
        if (act < 2 || act > 4) return CommandResult.BAD_SYNTAX;
        RunManager.debugSkipToAct(state, act);
        Console.showMessage("Now at Act " + act + "'s Entry Beacon" + (act == 4 ? " (Ascension Omega, Act 3 counted as won)" : "")
                + ". Interact with the beacon to continue.");
        return CommandResult.SUCCESS;
    }

    private CommandResult remove() {
        String blocker = ModRemoval.blocker();
        if (blocker != null) {
            Console.showMessage("Can't remove Slay the Sector yet: " + blocker + ".");
            return CommandResult.ERROR;
        }
        Console.showMessage("Removed " + String.join(", ", ModRemoval.run()) + ".\nSave now, quit, then disable"
                + " the mod in the launcher. Loading this save with the mod still enabled puts the Relay back.");
        return CommandResult.SUCCESS;
    }

    private CommandResult supplies(int n) {
        if (RunManager.get() == null) {
            Console.showMessage("No Run in progress.");
            return CommandResult.ERROR;
        }
        Global.getSector().getPlayerFleet().getCargo().addSupplies(n);
        Console.showMessage("Added " + n + " supplies to the Run fleet.");
        return CommandResult.SUCCESS;
    }
}
