package com.slaythesector.nodes;

import java.awt.Color;
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
import com.slaythesector.run.FleetCap;
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
    /** Kinds of fight, each scored its own way in {@link #afterBattle}. */
    private enum Fight { STANDARD, BOSS, ELITE, AMBUSH, STATION, SIDE, CONVOY }

    /** Option data: an Event choice. */
    private enum Ev { ASSAULT, SLIP, INSPECT, IGNORE, SIDE_A, SIDE_B, STAY_OUT, RAID, LET_PASS }

    /**
     * Reward picks still to open after a won fight (a battle station gives several). They open on
     * the next frame, outside the loot screen's callback.
     */
    private int pendingRewards;
    /** Faction whose ships the pending Reward picks offer (the defeated or, in a side battle, the helped one). */
    private String rewardFaction;
    /** The fight in progress and what was measured before it. */
    private Fight fight;
    private float playerDPBefore;
    private float enemyDPBefore;
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
        } else if (node.getId().equals(state.getTargetNodeId()) && node.getType() == NodeType.EVENT) {
            showEvent();
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
        NodeType type = node.getType();
        float strength = DepthScaling.combatPoints(node.getDepth(), type) * (type == NodeType.ELITE ? STSSettings.eliteFPMult() : 1f);
        String lead = type == NodeType.BOSS ? "The Apex: a" : type == NodeType.ELITE ? "An elite" : "A";
        text.addPara(lead + " %s fleet holds this beacon. Depth %s, estimated strength %s.",
                Misc.getHighlightColor(), NodeText.factionName(node), String.valueOf(node.getDepth()),
                String.valueOf(Math.round(strength)));
        if (type == NodeType.BOSS) {
            text.addPara("There is no retreat from this fight: withdrawing ends the Run.");
        } else if (type == NodeType.ELITE) {
            text.addPara("It closes in from every side. You can't win this head-on: break out. Escape with at least "
                    + "%s of your fleet's DP to count it a win; the reward grows with what you save.",
                    Misc.getHighlightColor(), Math.round(STSSettings.eliteEscapeFraction() * 100f) + "%");
        } else if (type == NodeType.AMBUSH) {
            text.addPara("You caught it off guard, and it will run. Its flagship is marked: destroy it before it "
                    + "escapes for a Reward pick. If it gets away, you only salvage a few supplies.");
        }
        String capWarning = FleetCap.warning(Global.getSector().getPlayerFleet(), state);
        if (capWarning != null) text.addPara(capWarning, Misc.getNegativeHighlightColor());
        options.addOption("Engage", Opt.ENGAGE);
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void engageNode() {
        Fight kind;
        switch (node.getType()) {
            case BOSS:   kind = Fight.BOSS; break;
            case ELITE:  kind = Fight.ELITE; break;
            case AMBUSH: kind = Fight.AMBUSH; break;
            default:     kind = Fight.STANDARD; break;
        }
        rewardFaction = node.getFactionId();
        engage(EncounterFactory.spawn(node, entity), null, kind);
    }

    /**
     * Hands the dialog to the vanilla fleet encounter flow against {@code enemy}; {@code ally}
     * (side battle) is already in a battle with it, which the player joins.
     */
    private void engage(final CampaignFleetAPI enemy, final CampaignFleetAPI ally, Fight kind) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        RunFleet.restoreCR(pf);
        fight = kind;
        playerDPBefore = EncounterFactory.dp(pf);
        enemyDPBefore = EncounterFactory.dp(enemy);
        final String targetId = enemy.getMemoryWithoutUpdate().getString(STSConstants.MEM_TARGET);
        final boolean boss = kind == Fight.BOSS;
        final InteractionDialogPlugin self = this;
        if (ally != null) Global.getFactory().createBattle(ally, enemy);

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
                // Measure the enemy before it is removed: what survived, and whether the mark did.
                float enemyDPAfter = EncounterFactory.dp(enemy);
                boolean targetAlive = targetId != null && EncounterFactory.has(enemy, targetId);
                EncounterFactory.despawn(enemy);
                EncounterFactory.despawn(ally);
                d.setPlugin(self);
                d.setInteractionTarget(entity);
                d.getVisualPanel().fadeVisualOut();
                d.setPromptText("You decide to...");
                afterBattle(engaged, won, enemyDPAfter, targetAlive);
            }
        };
        dialog.setInteractionTarget(enemy);
        dialog.setPlugin(fid);
        fid.init(dialog);
    }

    private void afterBattle(boolean engaged, boolean won, float enemyDPAfter, boolean targetAlive) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        options.clearOptions();
        if (pf.getFleetData().getNumMembers() == 0) {
            // Run fleet wiped: the respawn intercept (RunCampaignPlugin) ends the Run.
            dialog.dismiss();
            return;
        }
        if (!engaged) {
            if (node.getType() == NodeType.EVENT) showEvent();
            else showEncounter();
            return;
        }
        if (!won && fight == Fight.BOSS) {
            state.resolveTarget(false);
            showRunEnd(false);
            return;
        }

        // What each kind of fight counts as a win, and what it pays before the DP-cap cut.
        boolean success = won;
        int supplies = 0;
        int picks = 0;
        switch (fight) {
            case BOSS:
                supplies = STSSettings.rewardSuppliesBoss();
                break;
            case ELITE: {
                float saved = playerDPBefore <= 0f ? 0f : EncounterFactory.dp(pf) / playerDPBefore;
                success = won || saved >= STSSettings.eliteEscapeFraction();
                text.addPara("You saved %s of your fleet's DP.", success ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor(),
                        Math.round(Math.min(1f, saved) * 100f) + "%");
                supplies = success ? Math.round(STSSettings.rewardSuppliesElite() * (won ? 1f : saved)) : 0;
                picks = success ? 1 : 0;
                break;
            }
            case AMBUSH:
                success = won || !targetAlive;
                supplies = success ? STSSettings.rewardSuppliesCombat() : STSSettings.ambushEscapedSupplies();
                picks = success ? 1 : 0;
                if (!success) text.addPara("The marked target got away.", Misc.getNegativeHighlightColor());
                break;
            case CONVOY: {
                float destroyed = Math.max(0f, enemyDPBefore - enemyDPAfter);
                success = won || (enemyDPBefore > 0f && destroyed / enemyDPBefore >= STSSettings.convoyRewardFraction());
                supplies = Math.round(destroyed * STSSettings.convoySuppliesPerDP());
                picks = success ? 1 : 0;
                text.addPara("You destroyed %s DP of the convoy.", Misc.getHighlightColor(), String.valueOf(Math.round(destroyed)));
                break;
            }
            case STATION:
                supplies = won ? STSSettings.rewardSuppliesCombat() : 0;
                picks = won ? STSSettings.stationRewardPicks() : 0;
                break;
            default:
                supplies = won ? STSSettings.rewardSuppliesCombat() : 0;
                picks = won ? 1 : 0;
                break;
        }
        state.resolveTarget(success);
        RunFleet.afterFight(pf);

        int cut = Math.min(supplies, FleetCap.penalty(pf, state));
        supplies -= cut;
        if (supplies > 0) {
            pf.getCargo().addSupplies(supplies);
            text.addPara((success ? "Victory. " : "") + "Salvage crews recover %s supplies.",
                    Misc.getPositiveHighlightColor(), String.valueOf(supplies));
        }
        if (cut > 0) {
            text.addPara("%s supplies lost to the oversized fleet (%s DP over the soft cap).", Misc.getNegativeHighlightColor(),
                    String.valueOf(cut), String.valueOf(FleetCap.over(pf, state)));
        }
        if (fight == Fight.BOSS) {
            showRunEnd(true);
            return;
        }
        if (!success && supplies == 0) {
            text.addPara("You withdrew. The beacon is behind you, but there is no reward.");
        }
        if (picks > 0) {
            pendingRewards = picks;
        } else {
            showWaypoint();
        }
    }

    private void openRewardPick() {
        List<Card> cards = Rewards.offer(node.getDepth(), rewardFaction, state.getArchetypeId());
        if (cards.isEmpty()) {
            pendingRewards = 0;
            showWaypoint();
            return;
        }
        final int left = pendingRewards - 1;
        String title = left > 0 ? "Reward: choose one (" + (left + 1) + " picks left)" : "Reward: choose one";
        pendingRewards = 0;
        dialog.showCustomVisualDialog(CardGridDelegate.WIDTH, CardGridDelegate.HEIGHT, new CardGridDelegate(
                title, withDP(cards), CardGridDelegate.Mode.SINGLE, 0, "Skip reward",
                new CardGridDelegate.Listener() {
                    @Override
                    public void done(List<Card> picked) {
                        if (!picked.isEmpty()) {
                            String msg = Rewards.apply((Rewards.Reward) picked.get(0).data, Global.getSector().getPlayerFleet());
                            text.addPara(msg);
                        } else {
                            text.addPara("You leave the spoils behind.");
                        }
                        if (left > 0) pendingRewards = left;
                        else showWaypoint();
                    }
                }).withNote(dpNote(), dpNoteColor()));
    }

    // ---- Events ---------------------------------------------------------------------------------

    /** The Event of this Node, drawn on first arrival and kept in the Node (no re-roll by reloading). */
    private String eventId() {
        if (node.getEventId() == null) node.setEventId(Events.draw(state, node));
        return node.getEventId();
    }

    private void showEvent() {
        options.clearOptions();
        String id = eventId();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        if (id.equals(Events.STATION)) {
            text.addPara("A battle station sits on the beacon, guns tracking your fleet. It hasn't fired yet. "
                    + "Taking it would be costly, but its vaults hold the pick of the Descent: %s Reward picks.",
                    Misc.getHighlightColor(), String.valueOf(STSSettings.stationRewardPicks()));
            options.addOption("Assault the station", Ev.ASSAULT);
            options.addOption("Slip past (no fight, no reward)", Ev.SLIP);
        } else if (id.equals(Events.SABOTAGE)) {
            int cost = STSSettings.sabotageInspectCost();
            text.addPara("Your engineers report tampering in the supply holds: someone came aboard at the last beacon. "
                    + "A full inspection costs %s supplies. Ignore it, and there is a %s chance of losing %s of your supplies.",
                    Misc.getHighlightColor(), String.valueOf(cost), Math.round(STSSettings.sabotageChance() * 100f) + "%",
                    Math.round(STSSettings.sabotageLoss() * 100f) + "%");
            options.addOption("Inspect every hold (" + cost + " supplies)", Ev.INSPECT);
            if (pf.getCargo().getSupplies() < cost) options.setEnabled(Ev.INSPECT, false);
            options.addOption("Press on", Ev.IGNORE);
        } else if (id.startsWith(Events.SIDE)) {
            String a = Events.sideA(id);
            String b = Events.sideB(id);
            text.addPara("Two fleets are already fighting at the beacon: %s against %s. Both hail you. Whoever you "
                    + "help fights at your side, and pays you in its own ships.", Misc.getHighlightColor(),
                    Events.factionName(a), Events.factionName(b));
            options.addOption("Join " + Events.factionName(a) + " against " + Events.factionName(b), Ev.SIDE_A);
            options.addOption("Join " + Events.factionName(b) + " against " + Events.factionName(a), Ev.SIDE_B);
            options.addOption("Stay out of it", Ev.STAY_OUT);
        } else if (id.startsWith(Events.CONVOY)) {
            text.addPara("A %s convoy is crossing the beacon, freighters heavy and escort thin. It will run at the first "
                    + "shot. Every hull you catch is supplies; catch at least %s of it for a Reward pick too.",
                    Misc.getHighlightColor(), Events.factionName(Events.convoyFaction(id)),
                    Math.round(STSSettings.convoyRewardFraction() * 100f) + "%");
            options.addOption("Raid the convoy", Ev.RAID);
            options.addOption("Let it pass", Ev.LET_PASS);
        }
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void eventChoice(Ev ev) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        String id = eventId();
        switch (ev) {
            case ASSAULT:
                rewardFaction = state.getEnemyFactionIds().isEmpty() ? state.getArchetypeId() : state.getEnemyFactionIds().get(0);
                engage(EncounterFactory.spawnStation(node.getDepth(), entity), null, Fight.STATION);
                return;
            case SIDE_A:
            case SIDE_B: {
                String helped = ev == Ev.SIDE_A ? Events.sideA(id) : Events.sideB(id);
                String other = ev == Ev.SIDE_A ? Events.sideB(id) : Events.sideA(id);
                rewardFaction = helped;
                CampaignFleetAPI ally = EncounterFactory.spawnSide(helped, node.getDepth(), true, entity);
                CampaignFleetAPI enemy = EncounterFactory.spawnSide(other, node.getDepth(), false, entity);
                engage(enemy, ally, Fight.SIDE);
                return;
            }
            case RAID:
                rewardFaction = Events.convoyFaction(id);
                engage(EncounterFactory.spawnConvoy(rewardFaction, node.getDepth(), entity), null, Fight.CONVOY);
                return;
            case INSPECT: {
                int cost = STSSettings.sabotageInspectCost();
                if (pf.getCargo().getSupplies() < cost) return;
                pf.getCargo().removeSupplies(cost);
                text.addPara("The inspection finds the charges and clears them. %s supplies spent.", Misc.getHighlightColor(), String.valueOf(cost));
                break;
            }
            case IGNORE:
                if (Events.roll() < STSSettings.sabotageChance()) {
                    int lost = Math.round(pf.getCargo().getSupplies() * STSSettings.sabotageLoss());
                    pf.getCargo().removeSupplies(lost);
                    text.addPara("Two holds blow out an hour later. %s supplies lost.", Misc.getNegativeHighlightColor(), String.valueOf(lost));
                } else {
                    text.addPara("Nothing happens. Whoever it was, they were bad at their job.");
                }
                break;
            case SLIP:
                text.addPara("You give the station a wide berth. It lets you go.");
                break;
            case STAY_OUT:
                text.addPara("You leave them to it.");
                break;
            case LET_PASS:
                text.addPara("The convoy slips away into the dark.");
                break;
        }
        state.resolveTarget(false);
        showWaypoint();
    }

    // ---- Waypoint: services + next course ------------------------------------------------------

    private void showWaypoint() {
        options.clearOptions();
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        text.addPara("Supplies: %s. Ships: %s. %s.", Misc.getHighlightColor(),
                String.valueOf((int) pf.getCargo().getSupplies()), String.valueOf(pf.getFleetData().getNumMembers()),
                FleetCap.summary(pf, state));
        String capWarning = FleetCap.warning(pf, state);
        if (capWarning != null) text.addPara(capWarning, Misc.getNegativeHighlightColor());
        addServiceOption();
        RunNode committed = state.getTargetNode();
        if (committed != null) {
            text.addPara("Course locked: %s.", Misc.getHighlightColor(), NodeText.full(committed));
            options.addOption("Resume course", new Course(committed.getId()));
        } else {
            options.addOption("Open the node map (choose your course)", Opt.MAP);
        }
        options.addOption("Fleet services (repair, sell)", Opt.SERVICES);
        options.addOption("Refit the Run fleet", Opt.REFIT);
        options.addOption("Abandon the Run", Opt.ABANDON);
        addLeave();
    }

    private void openMap() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        boolean blocked = FleetCap.blocked(pf, state);
        String note = blocked
                ? "Course locked: " + FleetCap.summary(pf, state) + ". Close the map and sell ships in Fleet services."
                : FleetCap.warning(pf, state);
        dialog.showCustomVisualDialog(NodeMapDelegate.WIDTH, NodeMapDelegate.HEIGHT,
                new NodeMapDelegate(state, note, !blocked, new NodeMapDelegate.Listener() {
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
                    options.addOption("Dockyard: repair the whole fleet (" + cost + " supplies)", Opt.DOCKYARD);
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
                "Shop: spend your supplies", withDP(shopCards), CardGridDelegate.Mode.BUDGET, supplies, "Close the shop",
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
                }).withBudgetLabel("Supplies").withNote(dpNote(), dpNoteColor()));
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
                "Treasure: take one prize", withDP(treasureCards), CardGridDelegate.Mode.SINGLE, 0, "Not now",
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
                }).withNote(dpNote(), dpNoteColor()));
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
                        + ", CR " + pct(m.getRepairTracker().getCR()) + "): " + cost + " supplies", repair);
                if (cost > supplies) options.setEnabled(repair, false);
            }
        }
        if (total > 0) {
            options.addOption("Repair all: " + total + " supplies", Opt.REPAIR_ALL);
            if (total > supplies) options.setEnabled(Opt.REPAIR_ALL, false);
        } else {
            text.addPara("Every ship is fully repaired.");
        }
        for (FleetMemberAPI m : pf.getFleetData().getMembersListCopy()) {
            if (m.isFlagship() || pf.getFleetData().getNumMembers() <= 1) continue;
            options.addOption("Sell " + shipName(m) + ": +" + RunFleet.sellValue(m) + " supplies", new Service(true, m.getId()));
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
        if (FleetCap.blocked(pf, state)) {
            text.addPara(FleetCap.warning(pf, state), Misc.getNegativeHighlightColor());
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
        if (data instanceof Ev) {
            eventChoice((Ev) data);
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
                engageNode();
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

    /** Fleet DP reminder for the card grids that can add ships. */
    private String dpNote() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        return FleetCap.summary(pf, state) + ". Each DP over " + FleetCap.soft(state) + " cuts every fight reward by "
                + STSSettings.overCapSuppliesPerDP() + " supplies; above " + FleetCap.hard(state) + " you can't set a course.";
    }

    private Color dpNoteColor() {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        return FleetCap.over(pf, state) > 0 ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor();
    }

    /** Adds "Fleet DP after: x / soft" to each ship card, red when taking it passes the soft cap. */
    private List<Card> withDP(List<Card> cards) {
        int dp = RunFleet.totalDP(Global.getSector().getPlayerFleet());
        int soft = FleetCap.soft(state);
        for (Card c : cards) {
            if (c.preview == null) continue;
            int after = dp + Math.round(c.preview.getDeploymentPointsCost());
            c.withNote("Fleet DP after: " + after + " / " + soft, after > soft ? Misc.getNegativeHighlightColor() : Misc.getGrayColor());
        }
        return cards;
    }

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
        if (pendingRewards > 0) {
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
