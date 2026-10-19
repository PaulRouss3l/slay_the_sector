package com.slaythesector.nodes;

import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.combat.EncounterFactory;
import com.slaythesector.combat.Rewards;
import com.slaythesector.economy.Shop;
import com.slaythesector.economy.Treasure;
import com.slaythesector.economy.Upgrades;
import com.slaythesector.map.NodeText;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunFleet;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.ui.Card;
import com.slaythesector.ui.CardGridDelegate;
import com.slaythesector.ui.NodeMapDelegate;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.RunSystem;

/**
 * Dialog of a Node entity in the Run system. At a target fight Node: the encounter, fought through
 * the vanilla fleet encounter flow (pattern: vanilla SalvageDefenderInteraction). Arriving at a
 * Shop, Rest or Treasure Node resolves it and offers its service once. At the current Node: the
 * service if still unclaimed, Fleet services, and the node map to choose the next Node.
 */
public class NodeDialog implements InteractionDialogPlugin {

    private enum Opt {
        ENGAGE, SERVICES, REPAIR_ALL, REFIT, BACK, ABANDON, ABANDON_CONFIRM, END_RUN, LEAVE, MAP,
        SHOP, DOCKYARD, REST, REST_REPAIR, REST_SMOD, REST_TRAIN, TREASURE, SMOD_BACK, SMOD_SHIPS
    }

    /** Option data: set course for a Node. */
    private static class Course {
        final String nodeId;
        Course(String nodeId) { this.nodeId = nodeId; }
    }

    /** Option data: pick a ship for an S-mod, then (with {@code modId}) the hullmod to build in. */
    private static class Smod {
        final String memberId;
        final String modId;
        Smod(String memberId, String modId) { this.memberId = memberId; this.modId = modId; }
    }

    /** Option data: train this officer (+1 level). */
    private static class Train {
        final String personId;
        Train(String personId) { this.personId = personId; }
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
    /** Shop stock / Treasure prizes, drawn once per visit; the Node is claimed when the player leaves. */
    private List<Card> shopCards;
    private List<Card> treasureCards;
    /** Where the S-mod picker returns on "Back": the Rest menu or the Treasure prizes. */
    private Opt smodFrom;
    /** Course chosen on the node map; applied on the next frame, after the map has closed. */
    private String pendingCourse;

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
        } else if (node.getId().equals(state.getTargetNodeId())) {
            // Shop, Rest, Treasure: arriving resolves the Node (never counts as a win for the Payout).
            state.resolveTarget(false);
            showArrival();
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
        boolean boss = node.getType() == NodeType.BOSS;
        text.addPara((boss ? "The Apex: " : "") + "a %s fleet holds this beacon. Depth %s, estimated strength %s.",
                Misc.getHighlightColor(), NodeText.factionName(node), String.valueOf(node.getDepth()),
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
        int dp = RunFleet.totalDP(pf);
        int cap = STSSettings.fleetDPCap(state.getAct());
        text.addPara("Supplies: %s. Ships: %s. Fleet DP: %s / %s.", Misc.getHighlightColor(),
                String.valueOf((int) pf.getCargo().getSupplies()), String.valueOf(pf.getFleetData().getNumMembers()),
                String.valueOf(dp), String.valueOf(cap));
        addServiceOption();
        RunNode committed = state.getTargetNode();
        if (committed != null) {
            text.addPara("Course locked: %s.", Misc.getHighlightColor(), NodeText.full(committed));
            options.addOption("Resume course", new Course(committed.getId()));
        } else {
            if (dp > cap) {
                text.addPara("The fleet is over the Fleet DP cap. Sell ships (Fleet services) before setting a course.",
                        Misc.getNegativeHighlightColor());
            }
            options.addOption("Open the node map (choose your course)", Opt.MAP);
        }
        options.addOption("Fleet services (repair, sell)", Opt.SERVICES);
        options.addOption("Refit the Run fleet", Opt.REFIT);
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void openMap() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        boolean overCap = RunFleet.totalDP(pf) > STSSettings.fleetDPCap(state.getAct());
        String note = overCap ? "Over the Fleet DP cap: sell ships before setting a course." : null;
        dialog.showCustomVisualDialog(NodeMapDelegate.WIDTH, NodeMapDelegate.HEIGHT,
                new NodeMapDelegate(state, note, !overCap, new NodeMapDelegate.Listener() {
                    @Override
                    public void done(RunNode chosen) {
                        if (chosen != null) pendingCourse = chosen.getId();
                    }
                }));
    }

    // ---- Shop, Rest, Treasure ------------------------------------------------------------------

    private void showArrival() {
        switch (node.getType()) {
            case SHOP:
                text.addPara("A trader's hulk drifts by the beacon, lights on. Everything is priced in supplies, "
                        + "and the trader moves on once you leave.");
                break;
            case REST:
                text.addPara("A quiet beacon. Your crews get a moment to breathe. Choose one thing to do here.");
                break;
            case TREASURE:
                text.addPara("A sealed cache, older than the Descent itself. You may take one prize.");
                break;
            default:
                break;
        }
        showWaypoint();
    }

    /** The current Node's own service, while it hasn't been used. Shop repairs stay available. */
    private void addServiceOption() {
        RunNode cur = state.getCurrentNode();
        if (cur == null || !cur.getId().equals(node.getId())) return;
        switch (cur.getType()) {
            case SHOP:
                if (!cur.isClaimed()) options.addOption("Browse the shop", Opt.SHOP);
                int cost = dockyardCost();
                if (cost > 0) {
                    options.addOption("Dockyard: repair the whole fleet — " + cost + " supplies", Opt.DOCKYARD);
                    if (cost > Global.getSector().getPlayerFleet().getCargo().getSupplies()) options.setEnabled(Opt.DOCKYARD, false);
                }
                break;
            case REST:
                if (!cur.isClaimed()) options.addOption("Rest (choose one)", Opt.REST);
                break;
            case TREASURE:
                if (!cur.isClaimed()) options.addOption("Open the cache", Opt.TREASURE);
                break;
            default:
                break;
        }
    }

    private int dockyardCost() {
        return Math.round(repairAllCost() * STSSettings.shopRepairMult());
    }

    private int repairAllCost() {
        int total = 0;
        for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            total += RunFleet.repairCost(m);
        }
        return total;
    }

    private void dockyard() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        int cost = dockyardCost();
        if (cost > pf.getCargo().getSupplies()) return;
        pf.getCargo().removeSupplies(cost);
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) RunFleet.repair(m);
        text.addPara("The dockyard crews patch up the fleet for " + cost + " supplies.");
        showWaypoint();
    }

    private void openShop() {
        if (shopCards == null) shopCards = Shop.stock(state, node);
        final CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        int supplies = (int) pf.getCargo().getSupplies();
        dialog.showCustomVisualDialog(CardGridDelegate.WIDTH, CardGridDelegate.HEIGHT, new CardGridDelegate(
                "Shop: spend your supplies", shopCards, CardGridDelegate.Mode.BUDGET, supplies, "Close the shop",
                new CardGridDelegate.Listener() {
                    @Override
                    public void done(List<Card> picked) {
                        if (!picked.isEmpty()) {
                            int total = Shop.total(picked);
                            if (total <= pf.getCargo().getSupplies()) {
                                pf.getCargo().removeSupplies(total);
                                text.addPara("Bought for " + total + " supplies: " + Shop.apply(picked, pf) + ".");
                                shopCards.removeAll(picked);
                            }
                        }
                        showWaypoint();
                    }
                }).withBudgetLabel("Supplies"));
    }

    private void showRest() {
        options.clearOptions();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        options.addOption("Full repair of the fleet (free)", Opt.REST_REPAIR);
        if (repairAllCost() == 0) options.setEnabled(Opt.REST_REPAIR, false);
        options.addOption("Build in an S-mod on one ship", Opt.REST_SMOD);
        if (Upgrades.smodShips(pf).isEmpty()) options.setEnabled(Opt.REST_SMOD, false);
        options.addOption("Train an officer (+1 level)", Opt.REST_TRAIN);
        if (Upgrades.trainableOfficers(pf).isEmpty()) options.setEnabled(Opt.REST_TRAIN, false);
        options.addOption("Back", Opt.BACK);
        options.setShortcut(Opt.BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void restRepair() {
        for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) RunFleet.repair(m);
        node.claim();
        text.addPara("Every ship is repaired and back to full combat readiness.");
        showWaypoint();
    }

    private void showTrainees() {
        options.clearOptions();
        for (OfficerDataAPI od : Upgrades.trainableOfficers(Global.getSector().getPlayerFleet())) {
            options.addOption(od.getPerson().getNameString() + " (level " + od.getPerson().getStats().getLevel() + ")",
                    new Train(od.getPerson().getId()));
        }
        options.addOption("Back", Opt.REST);
        options.setShortcut(Opt.REST, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void train(Train t) {
        for (OfficerDataAPI od : Upgrades.trainableOfficers(Global.getSector().getPlayerFleet())) {
            if (!od.getPerson().getId().equals(t.personId)) continue;
            if (!Upgrades.train(od)) break;
            node.claim();
            text.addPara(od.getPerson().getNameString() + " is ready to level up: pick the new skill in the Fleet screen.",
                    Misc.getPositiveHighlightColor());
            break;
        }
        showWaypoint();
    }

    private void openTreasure() {
        if (treasureCards == null) {
            treasureCards = Treasure.offer(state, node, !Upgrades.smodShips(Global.getSector().getPlayerFleet()).isEmpty());
        }
        dialog.showCustomVisualDialog(CardGridDelegate.WIDTH, CardGridDelegate.HEIGHT, new CardGridDelegate(
                "Treasure: take one prize", treasureCards, CardGridDelegate.Mode.SINGLE, 0, "Not now",
                new CardGridDelegate.Listener() {
                    @Override
                    public void done(List<Card> picked) {
                        if (picked.isEmpty()) {
                            showWaypoint();
                            return;
                        }
                        Treasure.Prize prize = (Treasure.Prize) picked.get(0).data;
                        if (prize.kind == Treasure.Kind.SMOD) {
                            smodFrom = Opt.TREASURE;
                            showSmodShips();
                            return;
                        }
                        text.addPara(Treasure.apply(prize, Global.getSector().getPlayerFleet()));
                        node.claim();
                        showWaypoint();
                    }
                }));
    }

    private void showSmodShips() {
        options.clearOptions();
        text.addPara("Choose the ship.");
        for (FleetMemberAPI m : Upgrades.smodShips(Global.getSector().getPlayerFleet())) {
            options.addOption(shipName(m), new Smod(m.getId(), null));
        }
        options.addOption("Back", Opt.SMOD_BACK);
        options.setShortcut(Opt.SMOD_BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void smod(Smod s) {
        FleetMemberAPI m = find(Global.getSector().getPlayerFleet(), s.memberId);
        if (m == null) {
            showSmodShips();
            return;
        }
        if (s.modId == null) {
            options.clearOptions();
            text.addPara("Choose the hullmod to build into " + shipName(m) + ".");
            for (HullModSpecAPI spec : Upgrades.smodChoices(m)) {
                options.addOption(spec.getDisplayName(), new Smod(m.getId(), spec.getId()));
            }
            options.addOption("Back", Opt.SMOD_SHIPS);
            options.setShortcut(Opt.SMOD_SHIPS, Keyboard.KEY_ESCAPE, false, false, false, true);
            return;
        }
        Upgrades.buildIn(m, s.modId);
        node.claim();
        text.addPara(Global.getSettings().getHullModSpec(s.modId).getDisplayName() + " is now built into "
                + shipName(m) + ".", Misc.getPositiveHighlightColor());
        showWaypoint();
    }

    /** Leaving the Node: a Shop or Treasure that was opened is gone (no re-rolling by leaving). */
    private void leave() {
        if ((shopCards != null || treasureCards != null) && node != null) node.claim();
        dialog.dismiss();
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
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        if (RunFleet.totalDP(pf) > STSSettings.fleetDPCap(state.getAct())) {
            text.addPara("The fleet is over the Fleet DP cap. Sell ships (Fleet services) first.",
                    Misc.getNegativeHighlightColor());
            showWaypoint();
            return;
        }
        // Path lock: once set, a course can only be resumed, never swapped. Setting out also ends
        // this Node's unused service: it can't be kept for later.
        if (state.getTargetNodeId() == null) {
            state.setTargetNodeId(nodeId);
            RunNode cur = state.getCurrentNode();
            if (cur != null) cur.claim();
        }
        nodeId = state.getTargetNodeId();
        SectorEntityToken target = RunSystem.ensureNodeEntity(state.getNode(nodeId));
        leave();
        Global.getSector().layInCourseFor(target);
    }

    // ---- Run end --------------------------------------------------------------------------------

    private void showRunEnd(boolean won) {
        runWon = won;
        options.clearOptions();
        int payout = RunManager.payout(state, won);
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
        if (data instanceof Smod) {
            smod((Smod) data);
            return;
        }
        if (data instanceof Train) {
            train((Train) data);
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
                leave();
                break;
            case MAP:
                openMap();
                break;
            case SHOP:
                openShop();
                break;
            case DOCKYARD:
                dockyard();
                break;
            case REST:
                showRest();
                break;
            case REST_REPAIR:
                restRepair();
                break;
            case REST_SMOD:
                smodFrom = Opt.REST;
                showSmodShips();
                break;
            case REST_TRAIN:
                showTrainees();
                break;
            case TREASURE:
                openTreasure();
                break;
            case SMOD_SHIPS:
                showSmodShips();
                break;
            case SMOD_BACK:
                if (smodFrom == Opt.TREASURE) openTreasure();
                else showRest();
                break;
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private void addLeave() {
        options.addOption("Leave", Opt.LEAVE);
        options.setShortcut(Opt.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
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
        if (pendingCourse != null) {
            String id = pendingCourse;
            pendingCourse = null;
            setCourse(id);
        }
    }
    @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
    @Override public Object getContext() { return null; }
    @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
}
