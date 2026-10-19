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
 * pursuit layouts: player ESCAPE for an Elite, enemy ESCAPE for an Ambush or a convoy raid. Combat
 * runs on the original context, so the layout the vanilla setup wrote into the copy is copied back.
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
        // A fleeing enemy deploys everything, so the Ambush mark can't hide in reserve (reserves count as escaped).
        ctx.enemyDeployAll = context.enemyDeployAll || STSConstants.BATTLE_PURSUIT.equals(mode);
        ctx.fightToTheLast = context.fightToTheLast;
        ctx.setPlayerCommandPoints(context.getPlayerCommandPoints());
        ctx.setPursuitRangeModifier(context.getPursuitRangeModifier());
        super.initBattle(ctx, loader);
        copyLayout(ctx, context);
        STSLog.info("battle setup: " + mode + " (player " + playerGoal + ", enemy " + otherGoal + ")");
    }

    private static void copyLayout(BattleCreationContext from, BattleCreationContext to) {
        to.setInitialStepSize(from.getInitialStepSize());
        to.setInitialNumSteps(from.getInitialNumSteps());
        to.setInitialDeploymentBurnDuration(from.getInitialDeploymentBurnDuration());
        to.setNormalDeploymentBurnDuration(from.getNormalDeploymentBurnDuration());
        to.setEscapeDeploymentBurnDuration(from.getEscapeDeploymentBurnDuration());
        to.setStandoffRange(from.getStandoffRange());
        to.setInitialEscapeRange(from.getInitialEscapeRange());
        to.setFlankDeploymentDistance(from.getFlankDeploymentDistance());
        to.setPursuitRangeModifier(from.getPursuitRangeModifier());
        to.extraEnemyStandoffRange = from.extraEnemyStandoffRange;
        to.aiRetreatAllowed = from.aiRetreatAllowed;
        to.objectivesAllowed = from.objectivesAllowed;
        to.forceObjectivesOnMap = from.forceObjectivesOnMap;
        to.enemyDeployAll = from.enemyDeployAll;
        to.fightToTheLast = from.fightToTheLast;
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        if (targetMemberId != null) engine.addPlugin(new MarkedTargetPlugin(targetMemberId));
    }
}
