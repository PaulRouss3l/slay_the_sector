package com.slaythesector.combat;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;

/**
 * Battle setup for Run fleets tagged {@link STSConstants#MEM_BATTLE} (picked by
 * {@code RunCampaignPlugin.pickBattleCreationPlugin}). The context's goals have no setters, so it
 * is rebuilt with the wanted goals and handed to the vanilla setup, which has the escape and
 * pursuit layouts: player ESCAPE for an Elite, enemy ESCAPE for an Ambush or a convoy raid.
 */
public class RunBattleCreationPlugin extends BattleCreationPluginImpl {

    private String targetMemberId;

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        CampaignFleetAPI other = context.getOtherFleet();
        MemoryAPI mem = other.getMemoryWithoutUpdate();
        String mode = mem.getString(STSConstants.MEM_BATTLE);
        FleetGoal playerGoal = context.getPlayerGoal();
        FleetGoal otherGoal = context.getOtherGoal();
        if (STSConstants.BATTLE_ESCAPE.equals(mode)) {
            playerGoal = FleetGoal.ESCAPE;
            otherGoal = FleetGoal.ATTACK;
        } else if (STSConstants.BATTLE_PURSUIT.equals(mode)) {
            playerGoal = FleetGoal.ATTACK;
            otherGoal = FleetGoal.ESCAPE;
        }
        targetMemberId = mem.getString(STSConstants.MEM_TARGET);

        BattleCreationContext ctx = new BattleCreationContext(context.getPlayerFleet(), playerGoal, other, otherGoal);
        ctx.aiRetreatAllowed = context.aiRetreatAllowed;
        ctx.objectivesAllowed = false;
        ctx.forceObjectivesOnMap = false;
        ctx.enemyDeployAll = context.enemyDeployAll;
        ctx.fightToTheLast = context.fightToTheLast;
        ctx.setPlayerCommandPoints(context.getPlayerCommandPoints());
        ctx.setPursuitRangeModifier(context.getPursuitRangeModifier());
        super.initBattle(ctx, loader);
        STSLog.info("battle setup: " + mode + " (player " + playerGoal + ", enemy " + otherGoal + ")");
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        if (targetMemberId != null) engine.addPlugin(new MarkedTargetPlugin(targetMemberId));
    }
}
