package com.slaythesector.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.HomeFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.ui.CardGridDelegate;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.RunSystem;

/** The Hub station: start a Run (Archetype → Starting pick → refit → jump), or recover the Home fleet. */
public class HubDialog implements InteractionDialogPlugin {

    private enum Opt { BEGIN, RECOVER, RESUME, ABANDON, ABANDON_CONFIRM, MORE, BACK, REFIT, DESCEND, LEAVE }

    /** Option data for one Archetype. */
    private static class ArchetypeChoice {
        final String factionId;
        ArchetypeChoice(String factionId) { this.factionId = factionId; }
    }

    private static final int ARCHETYPES_PER_PAGE = 8;

    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private int archetypePage;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        text.addPara("The Descent Relay hums at the edge of the system. Its beacon points into a pocket of "
                + "space no chart shows. Fleets that go down never come back the same.");
        showMain();
    }

    private void showMain() {
        options.clearOptions();
        if (RunManager.isActive()) {
            text.addPara("A Run is in progress.");
            options.addOption("Return to the Run", Opt.RESUME);
            options.addOption("Abandon the Run", Opt.ABANDON);
        } else if (HomeFleet.isStashed()) {
            text.addPara("Your Home fleet is still docked here from an interrupted Run.");
            options.addOption("Recover your Home fleet", Opt.RECOVER);
        } else {
            options.addOption("Begin a Run", Opt.BEGIN);
            // No Leave while the Run fleet is out: it must not roam (or trade in) the real sector.
            addLeave();
        }
    }

    private void showArchetypes() {
        options.clearOptions();
        List<FactionAPI> all = Archetypes.eligible();
        int from = archetypePage * ARCHETYPES_PER_PAGE;
        for (int i = from; i < Math.min(all.size(), from + ARCHETYPES_PER_PAGE); i++) {
            FactionAPI f = all.get(i);
            options.addOption("Fly as " + f.getDisplayName(), new ArchetypeChoice(f.getId()), f.getBaseUIColor(), null);
        }
        if (all.size() > ARCHETYPES_PER_PAGE) options.addOption("More factions...", Opt.MORE);
        options.addOption("Back", Opt.BACK);
        options.setShortcut(Opt.BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void openStartingPick(final String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        int budget = STSSettings.startingDP();
        List<Card> cards = new ArrayList<Card>();
        for (FleetMemberAPI m : Archetypes.shipPool(factionId, budget, 24)) {
            int dp = (int) Math.ceil(m.getDeploymentPointsCost());
            cards.add(new Card(m, m.getHullSpec().getHullName(), m.getVariant().getDisplayName() + " - " + dp + " DP", m, dp));
        }
        if (cards.isEmpty()) {
            text.addPara(faction.getDisplayName() + " has no ships within " + budget + " DP. Pick another faction.");
            showArchetypes();
            return;
        }
        dialog.showCustomVisualDialog(CardGridDelegate.WIDTH, CardGridDelegate.HEIGHT, new CardGridDelegate(
                "Starting pick: " + faction.getDisplayName() + " (" + budget + " DP)", cards,
                CardGridDelegate.Mode.BUDGET, budget, "Cancel", new CardGridDelegate.Listener() {
                    @Override
                    public void done(List<Card> picked) {
                        if (picked.isEmpty()) {
                            showArchetypes();
                            return;
                        }
                        startRun(factionId, picked);
                    }
                }).withRandomButton());
    }

    private void startRun(String factionId, List<Card> picked) {
        List<FleetMemberAPI> ships = new ArrayList<FleetMemberAPI>();
        for (Card c : picked) ships.add((FleetMemberAPI) c.data);
        if (RunManager.start(factionId, ships) == null) {
            text.addPara("The relay refuses: a Run fleet is already out.");
            showMain();
            return;
        }
        text.addPara("Your Home fleet docks at the relay; it will wait here, untouched, until the Run ends. "
                + "You take command of the Run fleet with %s supplies.",
                Misc.getHighlightColor(), String.valueOf(STSSettings.startingSupplies()));
        options.clearOptions();
        options.addOption("Refit the Run fleet", Opt.REFIT);
        options.addOption("Jump into the Descent", Opt.DESCEND);
    }

    @Override
    public void optionSelected(String optionText, Object data) {
        if (data instanceof ArchetypeChoice) {
            openStartingPick(((ArchetypeChoice) data).factionId);
            return;
        }
        if (!(data instanceof Opt)) return;
        switch ((Opt) data) {
            case BEGIN:
                archetypePage = 0;
                text.addPara("Choose the faction you fly as. It sets the ships you can start with.");
                showArchetypes();
                break;
            case MORE: {
                int pages = (Archetypes.eligible().size() + ARCHETYPES_PER_PAGE - 1) / ARCHETYPES_PER_PAGE;
                archetypePage = (archetypePage + 1) % Math.max(1, pages);
                showArchetypes();
                break;
            }
            case BACK:
                showMain();
                break;
            case RECOVER:
                HomeFleet.restore();
                text.addPara("Your Home fleet undocks.");
                showMain();
                break;
            case REFIT:
                dialog.getVisualPanel().showCore(CoreUITabId.REFIT, dialog.getInteractionTarget(), CoreUITradeMode.NONE,
                        new CoreInteractionListener() {
                            @Override
                            public void coreUIDismissed() {}
                        });
                break;
            case RESUME:
            case DESCEND:
                descend();
                break;
            case ABANDON:
                options.clearOptions();
                text.addPara("Abandon the Run? It counts as a loss.");
                options.addOption("Abandon", Opt.ABANDON_CONFIRM);
                options.addOption("Back", Opt.BACK);
                break;
            case ABANDON_CONFIRM:
                RunManager.end(false);
                text.addPara("The Run fleet is scuttled. Your Home fleet undocks.");
                showMain();
                break;
            case LEAVE:
                dialog.dismiss();
                break;
        }
    }

    private void descend() {
        RunState state = RunManager.get();
        if (state == null) return;
        SectorEntityToken at = RunSystem.ensureNodeEntity(state.getCurrentNode());
        SectorEntityToken from = dialog.getInteractionTarget();
        dialog.dismiss();
        RunSystem.jump(from, at);
    }

    private void addLeave() {
        options.addOption("Leave", Opt.LEAVE);
        options.setShortcut(Opt.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override public void optionMousedOver(String optionText, Object optionData) {}
    @Override public void advance(float amount) {}
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
