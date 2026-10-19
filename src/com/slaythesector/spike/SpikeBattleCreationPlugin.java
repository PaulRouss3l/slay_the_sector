package com.slaythesector.spike;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.slaythesector.util.STSLog;

/**
 * Spike: picked by {@link SpikeCampaignPlugin#pickBattleCreationPlugin} for fleets tagged
 * {@link SpikeIds#MEM_SCENARIO}. Visible proof in combat: no objectives on the map, the enemy
 * never retreats, and a combat message.
 */
public class SpikeBattleCreationPlugin extends BattleCreationPluginImpl {

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        context.objectivesAllowed = false;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
        super.initBattle(context, loader);
        STSLog.info("spike: custom battle setup applied");
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new AnnouncePlugin());
    }

    /** Shows one combat message a moment after the battle starts. */
    private static class AnnouncePlugin extends BaseEveryFrameCombatPlugin {
        private float elapsed;
        private boolean done;

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (done) return;
            elapsed += amount;
            if (elapsed < 2f) return;
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.getCombatUI() == null) return;
            engine.getCombatUI().addMessage(1, new Color(120, 220, 255), "[STS spike] custom battle setup active");
            done = true;
        }
    }
}
