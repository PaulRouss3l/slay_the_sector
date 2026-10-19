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
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.map.NodeGraphBuilder;
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

    /** Swaps in the Run fleet built from {@code picks} and prepares the Run system. Call from the Hub dialog. */
    public static RunState start(String archetypeId, List<FleetMemberAPI> picks) {
        RunState state = new RunState(archetypeId);
        // Not security-sensitive: map/enemy randomness only.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        state.getEnemyFactionIds().addAll(drawEnemyFactions(archetypeId, rng));
        NodeGraphBuilder.build(state, STSSettings.actRows(), state.getEnemyFactionIds(), bossFaction(rng), rng.nextLong());
        snapshot(state);
        if (!HomeFleet.stash(RunFleet.create(picks))) return null;
        Global.getSector().getMemoryWithoutUpdate().set(STSConstants.MEM_RUN_STATE, state);
        RunSystem.ensure();
        RunSystem.clear();
        RunSystem.ensureNodeEntity(state.getCurrentNode());
        STSLog.info("Run started: archetype=" + archetypeId + ", enemies=" + state.getEnemyFactionIds());
        return state;
    }

    /**
     * Ends the Run: Home fleet back, Payout added, Run state cleared. The caller dismisses its dialog
     * and then calls {@link #returnToHub()}.
     */
    public static int end(boolean won) {
        RunState state = get();
        int depth = state == null ? 0 : state.getMaxDepthWon();
        int payout = payout(depth, won);
        CampaignFleetAPI home = HomeFleet.restore();
        if (state != null) restoreSnapshot(state);
        if (payout > 0) home.getCargo().getCredits().add(payout);
        Global.getSector().getMemoryWithoutUpdate().unset(STSConstants.MEM_RUN_STATE);
        STSLog.info("Run ended: won=" + won + ", depth=" + depth + ", payout=" + payout);
        Global.getSector().getCampaignUI().addMessage("Run " + (won ? "won" : "lost") + " at Depth " + depth
                + ". Payout: " + Misc.getDGSCredits(payout), won ? Misc.getPositiveHighlightColor() : Misc.getHighlightColor());
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

    /** By deepest Node won: 0 before the minimum Depth, linear up to the Boss, a fixed amount for a win. */
    public static int payout(int depth, boolean won) {
        if (won) return STSSettings.payoutWin();
        int min = STSSettings.payoutMinDepth();
        if (depth < min) return 0;
        int bossDepth = STSSettings.actRows() + 1;
        float t = bossDepth <= min ? 1f : Math.min(1f, (depth - min) / (float) (bossDepth - min));
        return Math.round(STSSettings.payoutAtMinDepth() + t * (STSSettings.payoutAtBoss() - STSSettings.payoutAtMinDepth()));
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

    private static String bossFaction(Random rng) {
        List<String> bosses = new ArrayList<String>();
        for (String id : STSSettings.bossFactions()) {
            if (Global.getSector().getFaction(id) != null) bosses.add(id);
        }
        return bosses.isEmpty() ? Factions.REMNANTS : bosses.get(rng.nextInt(bosses.size()));
    }
}
