package com.slaythesector.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
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
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.combat.EncounterFactory;
import com.slaythesector.combat.Rewards;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.ui.CardGridDelegate;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.RunSystem;

/**
 * Dialog of a Node entity in the Run system. At the target Node: the encounter, fought through
 * the vanilla fleet encounter flow (pattern: vanilla SalvageDefenderInteraction). At the current
 * Node: Fleet services and the choice of the next Node.
 */
public class NodeDialog implements InteractionDialogPlugin {

    private enum Opt { ENGAGE, SERVICES, REPAIR_ALL, REFIT, BACK, ABANDON, ABANDON_CONFIRM, END_RUN, LEAVE }

    /** Option data: set course for a Node. */
    private static class Course {
        final String nodeId;
        Course(String nodeId) { this.nodeId = nodeId; }
    }

    /** Option data: repair or sell one ship. */
    private static class Service {
        final boolean sell;
        final String memberId;
        Service(boolean sell, String memberId) { this.sell = sell; this.memberId = memberId; }
    }

    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private SectorEntityToken entity;
    private RunState state;
    private RunNode node;
    private boolean runWon;
    /** Set after a won fight; the Reward pick opens on the next frame, outside the loot screen's callback. */
    private boolean pendingReward;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.entity = dialog.getInteractionTarget();
        this.state = RunManager.get();
        this.node = state == null ? null : state.getNode(entity.getMemoryWithoutUpdate().getString(STSConstants.MEM_NODE_ID));

        if (node == null) {
            text.addPara("The beacon is silent.");
            options.addOption("Leave", Opt.LEAVE);
        } else if (node.getId().equals(state.getTargetNodeId()) && node.getType().isFight()) {
            showEncounter();
        } else if (node.getId().equals(state.getCurrentNodeId())) {
            if (node.getType() == NodeType.START) {
                text.addPara("The entry beacon of the Descent. Every course from here leads down.");
            }
            showWaypoint();
        } else {
            text.addPara("A spent beacon. Nothing remains here.");
            options.addOption("Leave", Opt.LEAVE);
        }
    }

    // ---- Encounter ------------------------------------------------------------------------------

    private void showEncounter() {
        options.clearOptions();
        FactionAPI f = Global.getSector().getFaction(node.getFactionId());
        boolean boss = node.getType() == NodeType.BOSS;
        text.addPara((boss ? "The Apex: " : "") + "a %s fleet holds this beacon. Depth %s, estimated strength %s.",
                Misc.getHighlightColor(), f.getDisplayName(), String.valueOf(node.getDepth()),
                String.valueOf(Math.round(DepthScaling.combatPoints(node.getDepth(), node.getType()))));
        if (boss) text.addPara("There is no retreat from this fight: withdrawing ends the Run.");
        options.addOption("Engage", Opt.ENGAGE);
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void engage() {
        RunFleet.restoreCR(Global.getSector().getPlayerFleet());
        final CampaignFleetAPI enemy = EncounterFactory.spawn(node, entity);
        final boolean boss = node.getType() == NodeType.BOSS;
        final InteractionDialogPlugin self = this;

        FIDConfig config = new FIDConfig();
        config.leaveAlwaysAvailable = !boss;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.lootCredits = false;
        config.dismissOnLeave = false;
        config.printXPToDialog = true;
        config.firstTimeEngageOptionText = "Engage";
        config.afterFirstTimeEngageOptionText = "Re-engage";
        config.noSalvageLeaveOptionText = "Continue";

        final FleetInteractionDialogPluginImpl fid = new FleetInteractionDialogPluginImpl(config);
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void battleContextCreated(InteractionDialogAPI d, BattleCreationContext bcc) {
                bcc.objectivesAllowed = false;
                if (boss) {
                    bcc.aiRetreatAllowed = false;
                    bcc.fightToTheLast = true;
                    bcc.enemyDeployAll = true;
                }
            }

            @Override
            public void postPlayerSalvageGeneration(InteractionDialogAPI d, FleetEncounterContext ctx, CargoAPI salvage) {
                // Supplies come only from designed rewards; fuel is not a Run resource (it is
                // refilled free) and would only push the fleet over capacity. Weapons and the rest stay.
                salvage.removeSupplies(salvage.getSupplies());
                salvage.removeFuel(salvage.getFuel());
            }

            @Override
            public void notifyLeave(InteractionDialogAPI d) {
                boolean engaged = false;
                boolean won = false;
                if (fid.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext ctx = (FleetEncounterContext) fid.getContext();
                    engaged = ctx.isEngagedInHostilities();
                    won = ctx.didPlayerWinEncounterOutright();
                }
                EncounterFactory.despawn(enemy);
                d.setPlugin(self);
                d.setInteractionTarget(entity);
                d.getVisualPanel().fadeVisualOut();
                d.setPromptText("You decide to...");
                afterBattle(engaged, won);
            }
        };
        dialog.setInteractionTarget(enemy);
        dialog.setPlugin(fid);
        fid.init(dialog);
    }

    private void afterBattle(boolean engaged, boolean won) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        options.clearOptions();
        if (pf.getFleetData().getNumMembers() == 0) {
            // Run fleet wiped: the respawn intercept (RunCampaignPlugin) ends the Run.
            dialog.dismiss();
            return;
        }
        if (!engaged) {
            showEncounter();
            return;
        }
        boolean boss = node.getType() == NodeType.BOSS;
        if (!won && boss) {
            state.resolveTarget(false);
            showRunEnd(false);
            return;
        }
        state.resolveTarget(won);
        RunFleet.afterFight(pf);
        if (!won) {
            text.addPara("You withdrew. The beacon is behind you, but there is no reward.");
            showWaypoint();
            return;
        }
        int supplies = boss ? STSSettings.rewardSuppliesBoss() : STSSettings.rewardSuppliesCombat();
        pf.getCargo().addSupplies(supplies);
        text.addPara("Victory. Salvage crews recover %s supplies.", Misc.getPositiveHighlightColor(), String.valueOf(supplies));
        if (boss) {
            showRunEnd(true);
        } else {
            pendingReward = true;
        }
    }

    private void openRewardPick() {
        List<Card> cards = Rewards.offer(node, state.getArchetypeId());
        if (cards.isEmpty()) {
            showWaypoint();
            return;
        }
        dialog.showCustomVisualDialog(CardGridDelegate.WIDTH, CardGridDelegate.HEIGHT, new CardGridDelegate(
                "Reward: choose one", cards, CardGridDelegate.Mode.SINGLE, 0, "Skip reward",
                new CardGridDelegate.Listener() {
                    @Override
                    public void done(List<Card> picked) {
                        if (!picked.isEmpty()) {
                            String msg = Rewards.apply((Rewards.Reward) picked.get(0).data, Global.getSector().getPlayerFleet());
                            text.addPara(msg);
                        } else {
                            text.addPara("You leave the spoils behind.");
                        }
                        showWaypoint();
                    }
                }));
    }

    // ---- Waypoint: services + next course ------------------------------------------------------

    private void showWaypoint() {
        options.clearOptions();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        text.addPara("Supplies: %s. Ships: %s.", Misc.getHighlightColor(),
                String.valueOf((int) pf.getCargo().getSupplies()), String.valueOf(pf.getFleetData().getNumMembers()));
        RunNode committed = state.getTargetNode();
        if (committed != null) {
            text.addPara("Course locked: %s, Depth %s.", Misc.getHighlightColor(),
                    committed.getType().label() + " — " + factionName(committed), String.valueOf(committed.getDepth()));
            options.addOption("Resume course", new Course(committed.getId()));
        }
        for (RunNode next : committed != null ? new ArrayList<RunNode>() : state.getChoices()) {
            String label = next.getType() == NodeType.BOSS
                    ? "Set course: Boss — " + factionName(next) + " (Depth " + next.getDepth() + ")"
                    : "Set course: " + next.getType().label() + " — " + factionName(next) + " (Depth " + next.getDepth() + ")";
            options.addOption(label, new Course(next.getId()));
        }
        options.addOption("Fleet services (repair, sell)", Opt.SERVICES);
        options.addOption("Refit the Run fleet", Opt.REFIT);
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void showServices() {
        options.clearOptions();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        int supplies = (int) pf.getCargo().getSupplies();
        int total = 0;
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) {
            int cost = RunFleet.repairCost(m);
            total += cost;
            if (cost > 0) {
                Service repair = new Service(false, m.getId());
                options.addOption("Repair " + shipName(m) + " (hull " + pct(m.getStatus().getHullFraction())
                        + ", CR " + pct(m.getRepairTracker().getCR()) + ") — " + cost + " supplies", repair);
                if (cost > supplies) options.setEnabled(repair, false);
            }
        }
        if (total > 0) {
            options.addOption("Repair all — " + total + " supplies", Opt.REPAIR_ALL);
            if (total > supplies) options.setEnabled(Opt.REPAIR_ALL, false);
        } else {
            text.addPara("Every ship is fully repaired.");
        }
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) {
            if (m.isFlagship() || pf.getFleetData().getNumMembers() <= 1) continue;
            options.addOption("Sell " + shipName(m) + " — +" + RunFleet.sellValue(m) + " supplies", new Service(true, m.getId()));
        }
        options.addOption("Back", Opt.BACK);
        options.setShortcut(Opt.BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void doService(Service s) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        FleetMemberAPI m = find(pf, s.memberId);
        if (m == null) return;
        if (s.sell) {
            int value = RunFleet.sellValue(m);
            pf.getFleetData().removeFleetMember(m);
            pf.getCargo().addSupplies(value);
            text.addPara("Sold " + shipName(m) + " for " + value + " supplies.");
        } else {
            int cost = RunFleet.repairCost(m);
            if (cost > pf.getCargo().getSupplies()) return;
            pf.getCargo().removeSupplies(cost);
            RunFleet.repair(m);
            text.addPara("Repaired " + shipName(m) + " for " + cost + " supplies.");
        }
        showServices();
    }

    private void repairAll() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        int total = 0;
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) total += RunFleet.repairCost(m);
        if (total > pf.getCargo().getSupplies()) return;
        pf.getCargo().removeSupplies(total);
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) RunFleet.repair(m);
        text.addPara("Fleet repaired for " + total + " supplies.");
        showServices();
    }

    private void setCourse(String nodeId) {
        // Path lock: once set, a course can only be resumed, never swapped.
        if (state.getTargetNodeId() == null) state.setTargetNodeId(nodeId);
        nodeId = state.getTargetNodeId();
        SectorEntityToken target = RunSystem.ensureNodeEntity(state.getNode(nodeId));
        dialog.dismiss();
        Global.getSector().layInCourseFor(target);
    }

    // ---- Run end --------------------------------------------------------------------------------

    private void showRunEnd(boolean won) {
        runWon = won;
        options.clearOptions();
        int payout = RunManager.payout(state.getMaxDepthWon(), won);
        if (won) {
            text.addPara("The Apex fleet breaks apart. The Run is won.", Misc.getPositiveHighlightColor());
        } else {
            text.addPara("The Run is lost.", Misc.getNegativeHighlightColor());
        }
        text.addPara("Deepest Node won: %s. Payout: %s.", Misc.getHighlightColor(),
                String.valueOf(state.getMaxDepthWon()), Misc.getDGSCredits(payout));
        options.addOption("Return to the Hub", Opt.END_RUN);
    }

    @Override
    public void optionSelected(String optionText, Object data) {
        if (data instanceof Course) {
            setCourse(((Course) data).nodeId);
            return;
        }
        if (data instanceof Service) {
            doService((Service) data);
            return;
        }
        if (!(data instanceof Opt)) return;
        switch ((Opt) data) {
            case ENGAGE:
                engage();
                break;
            case SERVICES:
                showServices();
                break;
            case REPAIR_ALL:
                repairAll();
                break;
            case REFIT:
                dialog.getVisualPanel().showCore(CoreUITabId.REFIT, entity, CoreUITradeMode.NONE,
                        new CoreInteractionListener() {
                            @Override
                            public void coreUIDismissed() {}
                        });
                break;
            case BACK:
                showWaypoint();
                break;
            case ABANDON:
                options.clearOptions();
                text.addPara("Abandon the Run? It counts as a loss.");
                options.addOption("Abandon", Opt.ABANDON_CONFIRM);
                options.addOption("Back", Opt.BACK);
                break;
            case ABANDON_CONFIRM:
                showRunEnd(false);
                break;
            case END_RUN:
                RunManager.end(runWon);
                dialog.dismiss();
                RunManager.returnToHub();
                break;
            case LEAVE:
                dialog.dismiss();
                break;
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private void addLeave() {
        options.addOption("Leave", Opt.LEAVE);
        options.setShortcut(Opt.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private static String factionName(RunNode n) {
        FactionAPI f = Global.getSector().getFaction(n.getFactionId());
        return f == null ? n.getFactionId() : f.getDisplayName();
    }

    private static String shipName(FleetMemberAPI m) {
        return m.getShipName() + " (" + m.getHullSpec().getHullName() + ")";
    }

    private static String pct(float f) {
        return Math.round(f * 100f) + "%";
    }

    private static FleetMemberAPI find(CampaignFleetAPI fleet, String memberId) {
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.getId().equals(memberId)) return m;
        }
        return null;
    }

    @Override public void optionMousedOver(String optionText, Object optionData) {}
    @Override
    public void advance(float amount) {
        if (pendingReward) {
            pendingReward = false;
            openRewardPick();
        }
    }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
