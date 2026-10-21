package com.slaythesector.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.economy.Upgrades;
import com.slaythesector.intel.RunResultIntel;
import com.slaythesector.map.NodeGraphBuilder;
import com.slaythesector.meta.MetaProgress;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;
import com.slaythesector.util.STSSettings;
import com.slaythesector.world.HubPlacer;
import com.slaythesector.world.RunSystem;

/** Starts and ends Runs: fleet swap, map generation, Payout. The only code that touches both fleets. */
public final class RunManager {

    private RunManager() {}

    public static RunState get() {
        Object o = Global.getSector().getMemoryWithoutUpdate().get(STSConstants.MEM_RUN_STATE);
        return o instanceof RunState ? (RunState) o : null;
    }

    public static boolean isActive() {
        return get() != null;
    }

    /**
     * Swaps in the Run fleet built from {@code picks} and prepares Act 1. Call from the Hub dialog.
     * {@code ascension} 0 (Gamma) .. 6 (Omega); the bonuses are the Meta-progress unlocks taken.
     */
    public static RunState start(String archetypeId, List<FleetMemberAPI> picks, int ascension,
                                 int bonusSupplies, boolean bonusOfficerLevel) {
        RunState state = new RunState(archetypeId);
        state.setAscension(ascension);
        state.setBonusOfficerLevel(bonusOfficerLevel);
        snapshot(state);
        CampaignFleetAPI runFleet = RunFleet.create(picks);
        if (bonusSupplies > 0) runFleet.getCargo().addSupplies(bonusSupplies);
        if (bonusOfficerLevel) {
            // A pending level-up: the player picks the skill in the vanilla Fleet screen.
            for (OfficerDataAPI od : runFleet.getFleetData().getOfficersCopy()) Upgrades.train(od);
        }
        if (!HomeFleet.stash(runFleet)) return null;
        Global.getSector().getMemoryWithoutUpdate().set(STSConstants.MEM_RUN_STATE, state);
        RunFactions.apply();
        RunSystem.ensure();
        RunSystem.clear();
        buildAct(state, 1);
        RunSystem.ensureNodeEntity(state.getCurrentNode());
        STSLog.info("Run started: archetype=" + archetypeId + ", ascension=" + Ascension.name(ascension)
                + ", enemies=" + state.getEnemyFactionIds());
        return state;
    }

    /**
     * After an Act's Boss: the next Act's map, laid out below the old one so its Start sits where
     * the Boss was (where the player is). The caller has dismissed its dialog.
     */
    public static void nextAct(RunState state) {
        RunNode boss = state.getCurrentNode();
        int bossDepth = boss == null ? STSSettings.actRows() + 1 : boss.getDepth();
        RunSystem.clearNodesAndFleets();
        state.setDepthOffset(state.getDepthOffset() + bossDepth);
        buildAct(state, state.getAct() + 1);
        RunSystem.ensureNodeEntity(state.getCurrentNode());
        STSLog.info("Act " + state.getAct() + " begins: enemies=" + state.getEnemyFactionIds());
    }

    /**
     * Debug (console {@code sts act}): skip straight to Act {@code act}'s Entry Beacon and move the
     * player fleet onto it. Act 4 also sets Ascension Omega and marks Act 3 won. Call only from the
     * campaign map with no dialog open (it relocates the player fleet).
     */
    public static void debugSkipToAct(RunState state, int act) {
        if (act >= 4) {
            state.setAscension(Ascension.OMEGA);
            state.setAct3Won();
        }
        RunSystem.clearNodesAndFleets();
        state.setDepthOffset((act - 1) * (STSSettings.actRows() + 1));
        buildAct(state, act);
        SectorEntityToken start = RunSystem.ensureNodeEntity(state.getCurrentNode());
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        pf.setLocation(start.getLocation().x, start.getLocation().y);
        STSLog.info("debug: skipped to Act " + act);
    }

    /** Fresh map and enemy factions for Act {@code act}. */
    private static void buildAct(RunState state, int act) {
        // Not security-sensitive: map/enemy randomness only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        state.clearNodes();
        state.setAct(act);
        state.getEnemyFactionIds().clear();
        state.getEnemyFactionIds().addAll(drawEnemyFactions(state.getArchetypeId(), rng));
        if (act >= 4) {
            NodeGraphBuilder.buildFinal(state, STSSettings.mapColumns());
            return;
        }
        float elite = STSSettings.weightElite()
                + (state.getAscension() >= Ascension.ALPHA ? STSSettings.ascensionEliteWeight() : 0f);
        NodeGraphBuilder.build(state, STSSettings.actRows(), STSSettings.mapColumns(), STSSettings.mapPaths(),
                state.getEnemyFactionIds(), bossFaction(act, state.getEnemyFactionIds(), rng),
                new NodeGraphBuilder.Weights(STSSettings.weightCombat(), STSSettings.weightShop(), STSSettings.weightRest(),
                        elite, STSSettings.weightAmbush(), STSSettings.weightEvent()),
                rng.nextLong());
    }

    /**
     * Ends the Run: Home fleet back, Payout added, Run state cleared. The caller dismisses its dialog
     * and then calls {@link #returnToHub()}.
     */
    public static int end(boolean won) {
        RunState state = get();
        int depth = state == null ? 0 : state.getMaxDepthWon();
        int payout = payout(state, won);
        CampaignFleetAPI home = HomeFleet.restore();
        if (state != null) restoreSnapshot(state);
        if (payout > 0) home.getCargo().getCredits().add(payout);
        Global.getSector().getMemoryWithoutUpdate().unset(STSConstants.MEM_RUN_STATE);
        STSLog.info("Run ended: won=" + won + ", depth=" + depth + ", payout=" + payout);
        if (state != null) {
            List<String> unlocked = MetaProgress.load().recordRun(state, won);
            // The Run-end report: intel with a notification (result, Payout, unlocks).
            new RunResultIntel(state.getArchetypeId(), won, state.isTrueBossWon(), state.getAscension(),
                    state.getAct(), depth, payout, unlocked).post();
        }
        return payout;
    }

    /** Player XP/level/points and veteran marines: the Run fleet would otherwise change them. */
    private static void snapshot(RunState state) {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerStats();
        PlayerFleetPersonnelTracker.PersonnelData marines = PlayerFleetPersonnelTracker.getInstance().getMarineData();
        state.snapshotCampaign(s.getXP(), s.getBonusXp(), s.getLevel(), s.getPoints(), s.getStoryPoints(),
                marines.num, marines.xp);
        state.setSnapDeferredBonusXp(s.getDeferredBonusXp());
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : s.getSkillsCopy()) {
            state.getSnapSkills().put(sl.getSkill().getId(), sl.getLevel());
        }
    }

    private static void restoreSnapshot(RunState state) {
        MutableCharacterStatsAPI s = Global.getSector().getPlayerStats();
        s.setLevel(state.getSnapLevel());
        s.setXP(state.getSnapXp());
        s.setBonusXp(state.getSnapBonusXp());
        s.setPoints(state.getSnapSkillPoints());
        s.setStoryPoints(state.getSnapStoryPoints());
        s.setDeferredBonusXp(state.getSnapDeferredBonusXp());
        for (MutableCharacterStatsAPI.SkillLevelAPI sl : s.getSkillsCopy()) {
            Float before = state.getSnapSkills().get(sl.getSkill().getId());
            s.setSkillLevel(sl.getSkill().getId(), before == null ? 0f : before);
        }
        s.refreshCharacterStatsEffects();
        PlayerFleetPersonnelTracker.PersonnelData marines = PlayerFleetPersonnelTracker.getInstance().getMarineData();
        marines.num = state.getSnapMarines();
        marines.xp = state.getSnapMarineXp();
    }

    /** Jump from wherever the player is to the Hub, then clean the Run system. */
    public static void returnToHub() {
        SectorEntityToken hub = HubPlacer.ensureHub();
        if (hub != null) RunSystem.jump(null, hub);
        RunSystem.clearNodesAndFleets();
    }

    /**
     * Payout for ending {@code state}'s Run: a fixed amount for a win, else linear in the deepest
     * Node won up to the Act 3 Boss (0 before the minimum Depth); × (1 + Ascension level); plus the
     * true-Boss bonus.
     */
    public static int payout(RunState state, boolean won) {
        if (state == null) return won ? STSSettings.payoutWin() : 0;
        int base;
        if (won) {
            base = STSSettings.payoutWin();
        } else {
            int depth = state.getMaxDepthWon();
            int min = STSSettings.payoutMinDepth();
            int finalDepth = 3 * (STSSettings.actRows() + 1);
            if (depth < min) return 0;
            float t = finalDepth <= min ? 1f : Math.min(1f, (depth - min) / (float) (finalDepth - min));
            base = Math.round(STSSettings.payoutAtMinDepth() + t * (STSSettings.payoutAtBoss() - STSSettings.payoutAtMinDepth()));
        }
        int total = base + state.getAscension() * (won ? STSSettings.ascensionPayout()
                : Math.round(base * (float) STSSettings.ascensionPayout() / STSSettings.payoutWin()));
        if (state.isTrueBossWon()) total += STSSettings.trueBossPayout();
        return total;
    }

    private static List<String> drawEnemyFactions(String archetypeId, Random rng) {
        List<String> pool = new ArrayList<String>();
        for (FactionAPI f : Archetypes.eligible()) {
            if (!f.getId().equals(archetypeId)) pool.add(f.getId());
        }
        if (pool.isEmpty()) pool.add(Factions.PIRATES);
        Collections.shuffle(pool, rng);
        int n = Math.min(pool.size(), 2 + rng.nextInt(2));
        return new ArrayList<String>(pool.subList(0, n));
    }

    /** Act Boss faction: {@code sts_actBossFactions}, or one of the Act's enemy factions when empty. */
    private static String bossFaction(int act, List<String> enemies, Random rng) {
        String id = STSSettings.actBossFaction(act);
        if (id != null && !id.isEmpty() && Global.getSector().getFaction(id) != null) return id;
        return enemies.isEmpty() ? Factions.PIRATES : enemies.get(rng.nextInt(enemies.size()));
    }
}
