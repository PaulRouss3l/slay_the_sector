package com.slaythesector.hub;

import java.util.ArrayList;
import java.util.Iterator;
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
import com.slaythesector.meta.MetaProgress;
import com.slaythesector.run.Archetypes;
import com.slaythesector.run.Ascension;
import com.slaythesector.run.HomeFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.ui.CardGridDelegate;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.RunSystem;

/**
 * The Hub station: start a Run (Archetype → Ascension → starting bonuses → Starting pick → refit →
 * jump), see the Records, or recover the Home fleet.
 */
public class HubDialog implements InteractionDialogPlugin {

    private enum Opt {
        BEGIN, RECORDS, RECOVER, RESUME, ABANDON, ABANDON_CONFIRM, MORE, BACK, REFIT, DESCEND, LEAVE,
        BONUS_DP, BONUS_OFFICER, BONUS_SUPPLIES, BONUS_DONE
    }

    /** Option data: an Ascension level for the chosen Archetype. */
    private static class AscensionChoice {
        final int level;
        AscensionChoice(int level) { this.level = level; }
    }

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
    /** The Run being set up. */
    private String pickedFaction;
    private int pickedAscension;
    private boolean useBonusDP;
    private boolean useBonusOfficer;
    private boolean useBonusSupplies;

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
            options.addOption("Records", Opt.RECORDS);
            // No Leave while the Run fleet is out: it must not roam (or trade in) the real sector.
            addLeave();
        }
    }

    private void showArchetypes() {
        options.clearOptions();
        List<FactionAPI> all = Archetypes.eligible();
        int from = archetypePage * ARCHETYPES_PER_PAGE;
        MetaProgress meta = MetaProgress.load();
        for (int i = from; i < Math.min(all.size(), from + ARCHETYPES_PER_PAGE); i++) {
            FactionAPI f = all.get(i);
            int best = meta.bestAscensionWon(f.getId());
            String label = "Fly as " + f.getDisplayName() + (best >= 0 ? " (won at " + Ascension.name(best) + ")" : "");
            options.addOption(label, new ArchetypeChoice(f.getId()), f.getBaseUIColor(), null);
        }
        if (all.size() > ARCHETYPES_PER_PAGE) options.addOption("More factions...", Opt.MORE);
        options.addOption("Back", Opt.BACK);
        options.setShortcut(Opt.BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    /** Ascension levels up to the one unlocked for the chosen Archetype (skipped at Gamma only). */
    private void showAscension() {
        int unlocked = MetaProgress.load().ascensionUnlocked(pickedFaction);
        if (unlocked <= 0) {
            pickedAscension = 0;
            showBonuses();
            return;
        }
        options.clearOptions();
        text.addPara("Choose the Ascension. Each level stacks its handicap on the ones below and adds %s to the Payout.",
                Misc.getHighlightColor(), Misc.getDGSCredits(STSSettings.ascensionPayout()));
        for (int level = 0; level <= unlocked; level++) {
            options.addOption(Ascension.name(level) + ": " + Ascension.effect(level), new AscensionChoice(level));
        }
        options.addOption("Back", Opt.BEGIN);
    }

    /** Toggles for the unlocked starting bonuses (skipped when none is unlocked). */
    private void showBonuses() {
        MetaProgress meta = MetaProgress.load();
        boolean any = meta.bonusUnlocked(MetaProgress.BONUS_DP) || meta.bonusUnlocked(MetaProgress.BONUS_OFFICER)
                || meta.bonusUnlocked(MetaProgress.BONUS_SUPPLIES);
        if (!any) {
            openStartingPick(pickedFaction);
            return;
        }
        options.clearOptions();
        text.addPara("Starting bonuses (unlocked by your Records):");
        if (meta.bonusUnlocked(MetaProgress.BONUS_DP)) {
            options.addOption((useBonusDP ? "[x] " : "[ ] ") + "+" + STSSettings.bonusStartDP() + " starting DP", Opt.BONUS_DP);
        }
        if (meta.bonusUnlocked(MetaProgress.BONUS_OFFICER)) {
            options.addOption((useBonusOfficer ? "[x] " : "[ ] ") + "+1 level for the starting officer", Opt.BONUS_OFFICER);
        }
        if (meta.bonusUnlocked(MetaProgress.BONUS_SUPPLIES)) {
            options.addOption((useBonusSupplies ? "[x] " : "[ ] ") + "+" + STSSettings.bonusStartSupplies() + " starting supplies",
                    Opt.BONUS_SUPPLIES);
        }
        options.addOption("Continue to the Starting pick", Opt.BONUS_DONE);
        options.addOption("Back", Opt.BEGIN);
    }

    private void showRecords() {
        options.clearOptions();
        MetaProgress meta = MetaProgress.load();
        text.addPara("Records of the Descent", Misc.getHighlightColor());
        Iterator<String> ids = meta.archetypeIds();
        boolean any = false;
        while (ids.hasNext()) {
            String id = ids.next();
            FactionAPI f = Global.getSector().getFaction(id);
            String name = f == null ? id : f.getDisplayName();
            int best = meta.bestAscensionWon(id);
            text.addPara("%s: %s Runs, %s wins, deepest Depth %s, best Ascension won %s, next Ascension %s%s",
                    Misc.getHighlightColor(), name, String.valueOf(meta.runs(id)), String.valueOf(meta.wins(id)),
                    String.valueOf(meta.bestDepth(id)), best >= 0 ? Ascension.name(best) : "none",
                    Ascension.name(meta.ascensionUnlocked(id)), meta.omegaCleared(id) ? ". Omega cleared" : "");
            any = true;
        }
        if (!any) text.addPara("No Run recorded yet.");
        text.addPara("Starting bonuses: +%s DP %s, +1 officer level %s, +%s supplies %s.", Misc.getHighlightColor(),
                String.valueOf(STSSettings.bonusStartDP()), lockText(meta, MetaProgress.BONUS_DP, "first win"),
                lockText(meta, MetaProgress.BONUS_OFFICER, "reach Act 3"),
                String.valueOf(STSSettings.bonusStartSupplies()), lockText(meta, MetaProgress.BONUS_SUPPLIES, "win with 3 factions"));
        options.addOption("Back", Opt.BACK);
        options.setShortcut(Opt.BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private static String lockText(MetaProgress meta, String bonus, String how) {
        return meta.bonusUnlocked(bonus) ? "(unlocked)" : "(locked: " + how + ")";
    }

    private void openStartingPick(final String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        int budget = STSSettings.startingDP() + (useBonusDP ? STSSettings.bonusStartDP() : 0);
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
        int supplies = useBonusSupplies ? STSSettings.bonusStartSupplies() : 0;
        if (RunManager.start(factionId, ships, pickedAscension, supplies, useBonusOfficer) == null) {
            text.addPara("The relay refuses: a Run fleet is already out.");
            showMain();
            return;
        }
        text.addPara("Your Home fleet docks at the relay; it will wait here, untouched, until the Run ends. "
                + "You take command of the Run fleet with %s supplies.",
                Misc.getHighlightColor(), String.valueOf(STSSettings.startingSupplies() + supplies));
        if (pickedAscension > 0) text.addPara("Ascension: %s.", Misc.getHighlightColor(), Ascension.name(pickedAscension));
        options.clearOptions();
        options.addOption("Refit the Run fleet", Opt.REFIT);
        options.addOption("Jump into the Descent", Opt.DESCEND);
    }

    @Override
    public void optionSelected(String optionText, Object data) {
        if (data instanceof ArchetypeChoice) {
            pickedFaction = ((ArchetypeChoice) data).factionId;
            showAscension();
            return;
        }
        if (data instanceof AscensionChoice) {
            pickedAscension = ((AscensionChoice) data).level;
            showBonuses();
            return;
        }
        if (!(data instanceof Opt)) return;
        switch ((Opt) data) {
            case BEGIN:
                archetypePage = 0;
                text.addPara("Choose the faction you fly as. It sets the ships you can start with.");
                showArchetypes();
                break;
            case RECORDS:
                showRecords();
                break;
            case BONUS_DP:
                useBonusDP = !useBonusDP;
                showBonuses();
                break;
            case BONUS_OFFICER:
                useBonusOfficer = !useBonusOfficer;
                showBonuses();
                break;
            case BONUS_SUPPLIES:
                useBonusSupplies = !useBonusSupplies;
                showBonuses();
                break;
            case BONUS_DONE:
                openStartingPick(pickedFaction);
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
                RunState current = RunManager.get();
                text.addPara(current != null && current.isAct3Won() ? "Leave the Descent now? The Run stays won."
                        : "Abandon the Run? It counts as a loss.");
                options.addOption("Abandon", Opt.ABANDON_CONFIRM);
                options.addOption("Back", Opt.BACK);
                break;
            case ABANDON_CONFIRM:
                // Abandoning Act 4 keeps the win earned in Act 3.
                RunManager.end(RunManager.get() != null && RunManager.get().isAct3Won());
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
