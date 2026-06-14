package com.slaythesector.dialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.slaythesector.gen.EncounterFleetFactory;
import com.slaythesector.gen.NodeGraphBuilder;
import com.slaythesector.gen.RewardShipFactory;
import com.slaythesector.run.MetaProgressManager;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.run.RunStateManager;
import com.slaythesector.util.DepthScaling;
import com.slaythesector.util.STSLog;

/**
 * The entire run plays out inside this single interaction dialog — no star system,
 * no fleet movement. For the run, the player's real fleet is stashed and replaced
 * with a chosen starting ship; ships won as combat rewards build out the run fleet;
 * at the end the original fleet is restored and the run fleet discarded.
 */
public class RunDialogPlugin implements InteractionDialogPlugin {

    private static final String OPT_ABANDON = "sts_abandon";
    private static final String OPT_DONE = "sts_done";
    private static final String OPT_CANCEL = "sts_cancel";
    private static final String OPT_ENGAGE = "sts_engage";
    private static final String OPT_EVENT_SALVAGE = "sts_event_salvage";
    private static final String OPT_EVENT_SKIP = "sts_event_skip";
    private static final String OPT_SHOP_SUPPLIES = "sts_shop_supplies";
    private static final String OPT_SHOP_FUEL = "sts_shop_fuel";
    private static final String OPT_REWARD_NONE = "sts_reward_none";
    private static final String OPT_SHOP_DONE = "sts_shop_done";
    private static final String OPT_REFIT = "sts_refit";
    private static final String OPT_CONTINUE = "sts_continue";
    private static final String START_PREFIX = "start:";
    private static final String REWARD_PREFIX = "reward:";

    /** Starting ship always available (fully-fitted vanilla start Wolf); meta unlocks add more. */
    private static final String BASE_STARTER = "wolf_Starting";
    private static final int REWARD_CHOICES = 3;
    private static final int SUPPLIES_COST = 200;
    private static final int FUEL_COST = 150;

    private InteractionDialogAPI dialog;
    private RunState state;
    private RunNode current;
    private RunNode pendingCombat;
    private CampaignFleetAPI enemy;
    private List<FleetMemberAPI> rewardChoices;

    // Stashed real fleet, restored at run end.
    private boolean fleetSwapped = false;
    private List<FleetMemberAPI> originalMembers;
    private FleetMemberAPI originalFlagship;
    private int stashedCrew;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.hideVisualPanel();

        long seed = System.nanoTime();
        state = new RunState();
        state.setActive(true);
        state.setSeed(seed);
        List<RunNode> nodes = NodeGraphBuilder.build(seed);
        for (RunNode n : nodes) {
            state.addNode(n);
        }
        current = nodes.get(0);
        state.setStartNodeId(current.getId());
        state.setCurrentNodeId(current.getId());
        RunStateManager.store(state);

        text("You cut the drives and let the Sector pull you in. There is no turning back now — "
                + "only the path ahead, and whatever waits at the bottom.");
        showStartChoice();
    }

    // --- Start ship selection ---

    private void showStartChoice() {
        text("Choose the ship you will descend with:");
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();

        LinkedHashSet<String> starters = new LinkedHashSet<String>();
        starters.add(BASE_STARTER);
        starters.addAll(MetaProgressManager.get().getUnlockedShipVariants());
        for (String variant : starters) {
            opts.addOption(hullName(variant) + "-class (fully fitted)", START_PREFIX + variant);
        }
        opts.addOption("Not now", OPT_CANCEL);
        dialog.setOptionOnEscape("Not now", OPT_CANCEL);
    }

    private void beginRun(String starterVariant) {
        MetaProgressManager.onRunStarted();
        int won = MetaProgressManager.get().getRunsWon();
        if (won > 0) {
            CargoAPI cargo = playerCargo();
            cargo.addSupplies(20f * won);
            cargo.getCredits().add(500f * won);
        }
        swapToRunFleet(starterVariant);
        text("You take the helm of the " + hullName(starterVariant) + "-class and begin the descent.");
        showChoices();
    }

    // --- Fleet swap / restore ---

    /** The mothball bay, initialized on first use (getMothballedShips() is null until then). */
    private FleetDataAPI mothballBay(CampaignFleetAPI player) {
        CargoAPI cargo = player.getCargo();
        if (cargo.getMothballedShips() == null) {
            cargo.initMothballedShips(player.getFaction().getId());
        }
        return cargo.getMothballedShips();
    }

    private void swapToRunFleet(String starterVariant) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        FleetDataAPI fd = player.getFleetData();
        FleetDataAPI bay = mothballBay(player);
        stashedCrew = player.getCargo().getCrew(); // remember to restore at run end
        originalFlagship = player.getFlagship();
        originalMembers = fd.getMembersListCopy();
        // Stash the real fleet in the mothball bay: it travels safely, never fights,
        // and keeps the player "alive" so a wiped run fleet can't trigger game-over.
        for (FleetMemberAPI m : originalMembers) {
            fd.removeFleetMember(m);
            bay.addFleetMember(m);
        }
        FleetMemberAPI starter = Global.getFactory().createFleetMember(FleetMemberType.SHIP, starterVariant);
        fd.addFleetMember(starter);
        fd.setFlagship(starter);
        starter.setCaptain(Global.getSector().getPlayerPerson());
        // Modest supply/fuel so the run fleet can deploy + recover CR.
        player.getCargo().addSupplies(200f);
        player.getCargo().addFuel(50f);
        setRunCrew(player);
        makeReady(starter);
        fleetSwapped = true;
    }

    /** Set crew to exactly the run fleet's capacity — excess crew penalizes CR. */
    private void setRunCrew(CampaignFleetAPI player) {
        int target = 0;
        for (FleetMemberAPI m : player.getFleetData().getMembersListCopy()) {
            target += (int) Math.ceil(m.getMaxCrew());
        }
        setCrewExact(player.getCargo(), target);
    }

    private void setCrewExact(CargoAPI cargo, int target) {
        int cur = cargo.getCrew();
        if (cur < target) {
            cargo.addCrew(target - cur);
        } else if (cur > target) {
            cargo.removeCrew(cur - target);
        }
    }

    /** Bring a freshly added ship to full combat readiness. */
    private void makeReady(FleetMemberAPI member) {
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
    }

    private void restoreFleet() {
        if (!fleetSwapped) {
            return;
        }
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        FleetDataAPI fd = player.getFleetData();
        FleetDataAPI bay = mothballBay(player);
        // Drop the run fleet (and any reward ships).
        for (FleetMemberAPI m : fd.getMembersListCopy()) {
            fd.removeFleetMember(m);
        }
        // Pull the real fleet back out of the mothball bay.
        if (originalMembers != null) {
            for (FleetMemberAPI m : originalMembers) {
                bay.removeFleetMember(m);
                fd.addFleetMember(m);
            }
        }
        if (originalFlagship != null) {
            fd.setFlagship(originalFlagship);
            originalFlagship.setCaptain(Global.getSector().getPlayerPerson());
        }
        // Restore the crew count the player had before the run.
        setCrewExact(player.getCargo(), stashedCrew);
        fleetSwapped = false;
    }

    // --- Path selection ---

    private void showChoices() {
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        if (current.getSuccessorIds().isEmpty()) {
            opts.addOption("Leave", OPT_DONE);
            dialog.setOptionOnEscape("Leave", OPT_DONE);
            return;
        }
        text("Choose your path:");
        for (String succId : current.getSuccessorIds()) {
            RunNode succ = state.getNode(succId);
            if (succ != null) {
                opts.addOption(pathLabel(succ), succId);
            }
        }
        opts.addOption("Abandon the descent", OPT_ABANDON);
        dialog.setOptionOnEscape("Abandon the descent", OPT_ABANDON);
    }

    private String pathLabel(RunNode node) {
        String base = node.getType().label() + "  (depth " + node.getDepth() + ")";
        if (node.getType().isCombat() && node.getFactionId() != null) {
            return base + " - " + factionName(node.getFactionId());
        }
        return base;
    }

    private void arriveAt(RunNode node) {
        current = node;
        state.setCurrentNodeId(node.getId());
        switch (node.getType()) {
            case COMBAT:
            case ELITE:
            case BOSS:
                showCombatIntro(node);
                break;
            case EVENT:
                showEvent();
                break;
            case SHOP:
                showShop();
                break;
            default:
                showChoices();
                break;
        }
    }

    private void afterNonCombatNode() {
        current.setCleared(true);
        showChoices();
    }

    // --- Combat ---

    private void showCombatIntro(RunNode node) {
        boolean boss = node.getType() == NodeType.BOSS;
        if (boss) {
            text("The Apex unfolds from the dark — a " + factionName(node.getFactionId())
                    + " warmachine. This is the end of the descent.");
        } else {
            text(factionName(node.getFactionId()) + " forces move to intercept. Steel yourself — "
                    + "wreck them and you can salvage one of their hulls.");
        }
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        opts.addOption("Engage", OPT_ENGAGE);
        opts.addOption("Abandon the descent", OPT_ABANDON);
        dialog.setOptionOnEscape("Abandon the descent", OPT_ABANDON);
    }

    private void startCombat() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        enemy = EncounterFleetFactory.create(current);
        pendingCombat = current;
        LocationAPI loc = player.getContainingLocation();
        if (loc != null) {
            loc.addEntity(enemy);
            enemy.setLocation(player.getLocation().x, player.getLocation().y);
        }
        dialog.startBattle(new BattleCreationContext(player, FleetGoal.ATTACK, enemy, FleetGoal.ATTACK));
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
        boolean won = result != null && result.didPlayerWin();
        if (enemy != null && !enemy.isDespawning()) {
            enemy.despawn();
        }
        enemy = null;
        if (!won) {
            endRun(false, "lost");
            return;
        }
        current = pendingCombat;
        current.setCleared(true);
        if (current.getType() == NodeType.BOSS) {
            endRun(true, "won");
            return;
        }
        text("The hostiles are destroyed.");
        offerReward(current);
    }

    // --- Reward ---

    private void offerReward(RunNode node) {
        rewardChoices = RewardShipFactory.pick(
                node.getFactionId(), DepthScaling.rewardSize(node.getDepth()), REWARD_CHOICES);
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        if (rewardChoices == null || rewardChoices.isEmpty()) {
            text("Nothing salvageable remains.");
            opts.addOption("Continue", OPT_REWARD_NONE);
            dialog.setOptionOnEscape("Continue", OPT_REWARD_NONE);
            return;
        }
        text("Salvage from the wreckage - choose one hull to add to your fleet:");
        for (int i = 0; i < rewardChoices.size(); i++) {
            FleetMemberAPI m = rewardChoices.get(i);
            opts.addOption(m.getHullSpec().getHullNameWithDashClass()
                    + " (" + sizeName(m) + ")", REWARD_PREFIX + i);
        }
        opts.addOption("Take nothing", OPT_REWARD_NONE);
        dialog.setOptionOnEscape("Take nothing", OPT_REWARD_NONE);
    }

    private void takeReward(int index) {
        if (rewardChoices != null && index >= 0 && index < rewardChoices.size()) {
            FleetMemberAPI chosen = rewardChoices.get(index);
            FleetMemberAPI member = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, chosen.getVariant());
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            player.getFleetData().addFleetMember(member);
            setRunCrew(player);
            makeReady(member);
            text("The " + chosen.getHullSpec().getHullNameWithDashClass() + " joins your fleet.");
        }
        rewardChoices = null;
        showPostReward();
    }

    /** After a reward: let the player refit before picking the next node. */
    private void showPostReward() {
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        opts.addOption("Refit your fleet", OPT_REFIT);
        opts.addOption("Continue the descent", OPT_CONTINUE);
        dialog.setOptionOnEscape("Continue the descent", OPT_CONTINUE);
    }

    // --- Event ---

    private void showEvent() {
        text("A derelict tumbles in the dark, reactor still ticking. Salvage looks possible — "
                + "though something may be lurking in its hull.");
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        opts.addOption("Salvage the derelict", OPT_EVENT_SALVAGE);
        opts.addOption("Leave it be", OPT_EVENT_SKIP);
        dialog.setOptionOnEscape("Leave it be", OPT_EVENT_SKIP);
    }

    private void salvageEvent() {
        int reward = 100 * (current.getDepth() + 1);
        playerCargo().getCredits().add(reward);
        text("You strip the derelict for " + reward + " credits.");
        afterNonCombatNode();
    }

    // --- Shop ---

    private void showShop() {
        int credits = (int) playerCargo().getCredits().get();
        text("A black-market tender drifts alongside. You have " + credits + " credits.");
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        opts.addOption("Buy 50 supplies (" + SUPPLIES_COST + " cr)", OPT_SHOP_SUPPLIES);
        opts.addOption("Buy 50 fuel (" + FUEL_COST + " cr)", OPT_SHOP_FUEL);
        opts.addOption("Move on", OPT_SHOP_DONE);
        dialog.setOptionOnEscape("Move on", OPT_SHOP_DONE);
    }

    private void buy(String which) {
        CargoAPI cargo = playerCargo();
        float credits = cargo.getCredits().get();
        if (OPT_SHOP_SUPPLIES.equals(which)) {
            if (credits >= SUPPLIES_COST) {
                cargo.getCredits().subtract(SUPPLIES_COST);
                cargo.addSupplies(50);
                text("Purchased 50 supplies.");
            } else {
                text("Not enough credits.");
            }
        } else {
            if (credits >= FUEL_COST) {
                cargo.getCredits().subtract(FUEL_COST);
                cargo.addFuel(50);
                text("Purchased 50 fuel.");
            } else {
                text("Not enough credits.");
            }
        }
        showShop();
    }

    // --- End ---

    private void endRun(boolean won, String reason) {
        int depth = current == null ? 0 : current.getDepth();
        restoreFleet();
        if (won) {
            MetaProgressManager.onRunWon(depth);
            text("The Apex falls silent. You have conquered the Sector. Your own fleet returns to you, "
                    + "and new strength carries into the next descent.");
        } else {
            MetaProgressManager.onRunEnded(depth);
            text(("lost".equals(reason)
                    ? "Your run fleet is shattered. The Sector keeps what it takes."
                    : "You break off the descent and slip away.")
                    + " Your own fleet returns to you. (Reached depth " + depth + ".)");
        }
        state.setActive(false);
        RunStateManager.clear();
        STSLog.info("run " + reason + " at depth " + depth);
        OptionPanelAPI opts = dialog.getOptionPanel();
        opts.clearOptions();
        opts.addOption("Leave", OPT_DONE);
        dialog.setOptionOnEscape("Leave", OPT_DONE);
    }

    // --- Plumbing ---

    @Override
    public void optionSelected(String text, Object data) {
        if (!(data instanceof String)) {
            return;
        }
        String d = (String) data;
        if (OPT_DONE.equals(d)) {
            dialog.dismiss();
        } else if (OPT_CANCEL.equals(d)) {
            // Backed out before stashing the fleet — nothing to undo.
            RunStateManager.clear();
            dialog.dismiss();
        } else if (OPT_ABANDON.equals(d)) {
            endRun(false, "abandoned");
        } else if (d.startsWith(START_PREFIX)) {
            beginRun(d.substring(START_PREFIX.length()));
        } else if (OPT_ENGAGE.equals(d)) {
            startCombat();
        } else if (d.startsWith(REWARD_PREFIX)) {
            takeReward(Integer.parseInt(d.substring(REWARD_PREFIX.length())));
        } else if (OPT_REWARD_NONE.equals(d)) {
            takeReward(-1);
        } else if (OPT_REFIT.equals(d)) {
            // Open the refit screen; options stay, so closing it returns here.
            Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.REFIT);
        } else if (OPT_CONTINUE.equals(d)) {
            showChoices();
        } else if (OPT_EVENT_SALVAGE.equals(d)) {
            salvageEvent();
        } else if (OPT_EVENT_SKIP.equals(d)) {
            text("You leave the derelict to the dark.");
            afterNonCombatNode();
        } else if (OPT_SHOP_SUPPLIES.equals(d) || OPT_SHOP_FUEL.equals(d)) {
            buy(d);
        } else if (OPT_SHOP_DONE.equals(d)) {
            afterNonCombatNode();
        } else {
            RunNode node = state.getNode(d);
            if (node != null) {
                arriveAt(node);
            }
        }
    }

    private void text(String s) {
        dialog.getTextPanel().addPara(s);
    }

    private CargoAPI playerCargo() {
        return Global.getSector().getPlayerFleet().getCargo();
    }

    private String factionName(String factionId) {
        FactionAPI f = factionId == null ? null : Global.getSector().getFaction(factionId);
        return f != null ? f.getDisplayName() : "hostile";
    }

    private static String sizeName(FleetMemberAPI m) {
        switch (m.getHullSpec().getHullSize()) {
            case FRIGATE:      return "frigate";
            case DESTROYER:    return "destroyer";
            case CRUISER:      return "cruiser";
            case CAPITAL_SHIP: return "capital";
            default:           return "ship";
        }
    }

    private static String hullName(String variantId) {
        String hull = variantId.contains("_") ? variantId.substring(0, variantId.indexOf('_')) : variantId;
        return hull.isEmpty() ? variantId : Character.toUpperCase(hull.charAt(0)) + hull.substring(1);
    }

    @Override
    public void advance(float amount) {}

    @Override
    public void optionMousedOver(String text, Object data) {}

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
