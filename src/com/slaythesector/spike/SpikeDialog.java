package com.slaythesector.spike;

import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

/**
 * Spike: test menu on the Hub (normal sector) and on the Beacon (Run system).
 * Every option maps to one Spike item in docs/ROADMAP.md.
 */
public class SpikeDialog implements InteractionDialogPlugin {

    private enum Opt { SWAP, RESTORE, ENTER, MAP, ENEMY, WIPE, EXIT, LEAVE }

    private InteractionDialogAPI dialog;
    private boolean atBeacon;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        SectorEntityToken target = dialog.getInteractionTarget();
        atBeacon = target != null && target.hasTag(SpikeIds.TAG_BEACON);
        dialog.getTextPanel().addPara(atBeacon
                ? "[STS spike] Descent Beacon — inside the contained Run system."
                : "[STS spike] Descent Relay — test Hub. Swap fleets, then enter the Run system.");
        showOptions();
    }

    private void showOptions() {
        OptionPanelAPI o = dialog.getOptionPanel();
        o.clearOptions();
        boolean swapped = SpikeFleetSwap.isSwapped();
        if (atBeacon) {
            o.addOption("Spawn a test enemy (custom battle setup)", Opt.ENEMY);
            o.addOption("Spawn an overwhelming enemy (fleet wipe test)", Opt.WIPE);
            o.addOption("Open the node map canvas", Opt.MAP);
            o.addOption("Return to the Hub", Opt.EXIT);
        } else {
            if (!swapped) o.addOption("Stash Home fleet, take a Run fleet", Opt.SWAP);
            else o.addOption("Restore Home fleet", Opt.RESTORE);
            o.addOption("Jump into the Run system", Opt.ENTER);
            o.addOption("Open the node map canvas", Opt.MAP);
        }
        o.addOption("Leave", Opt.LEAVE);
        o.setShortcut(Opt.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof Opt)) return;
        switch ((Opt) optionData) {
            case SWAP:
                SpikeFleetSwap.stashHomeFleet();
                dialog.getTextPanel().addPara("Home fleet detached. You now command the Run fleet.");
                showOptions();
                break;
            case RESTORE:
                SpikeFleetSwap.restoreHomeFleet();
                dialog.getTextPanel().addPara("Home fleet restored.");
                showOptions();
                break;
            case ENTER: {
                StarSystemAPI sys = SpikeWorld.ensureRunSystem();
                SectorEntityToken beacon = sys.getEntityById(SpikeIds.BEACON_ID);
                SectorEntityToken from = dialog.getInteractionTarget();
                dialog.dismiss();
                SpikeWorld.jumpTo(from, beacon);
                break;
            }
            case EXIT: {
                SectorEntityToken hub = Global.getSector().getEntityById(SpikeIds.HUB_ID);
                SectorEntityToken from = dialog.getInteractionTarget();
                dialog.dismiss();
                if (hub != null) SpikeWorld.jumpTo(from, hub);
                break;
            }
            case MAP:
                dialog.showCustomVisualDialog(SpikeMapDelegate.WIDTH, SpikeMapDelegate.HEIGHT, new SpikeMapDelegate());
                break;
            case ENEMY:
                SpikeWorld.spawnTestEnemy(Factions.PIRATES, 40f);
                dialog.dismiss();
                break;
            case WIPE:
                SpikeWorld.spawnTestEnemy(Factions.REMNANTS, 300f);
                dialog.dismiss();
                break;
            case LEAVE:
                dialog.dismiss();
                break;
        }
    }

    @Override public void optionMousedOver(String optionText, Object optionData) {}
    @Override public void advance(float amount) {}
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
