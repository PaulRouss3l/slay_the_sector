package com.slaythesector.world;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunManager;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSLog;

/**
 * The contained Run system: no jump points, entered and left only by our transitions.
 * Created once per save and reused; Node entities are cleared between Runs.
 */
public final class RunSystem {

    private RunSystem() {}

    private static final float NODE_SPACING_Y = 3500f;
    private static final float NODE_SPACING_X = 2200f;

    public static StarSystemAPI get() {
        return Global.getSector().getStarSystem(STSConstants.RUN_SYSTEM_NAME);
    }

    public static boolean isRunSystem(LocationAPI loc) {
        return loc != null && loc == get();
    }

    public static StarSystemAPI ensure() {
        StarSystemAPI sys = get();
        if (sys != null) return sys;
        sys = Global.getSector().createStarSystem(STSConstants.RUN_SYSTEM_NAME);
        sys.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        placeInBounds(sys);
        SectorEntityToken center = sys.initNonStarCenter();
        center.addTag(Tags.THEME_HIDDEN);
        center.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        sys.addTag(Tags.THEME_HIDDEN);
        sys.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        sys.addTag(Tags.DO_NOT_SHOW_STRANDED_DIALOG);
        // No autogenerateHyperspaceJumpPoints(): the only way out is our transition.
        STSLog.info("Run system created");
        return sys;
    }

    /**
     * Keeps the Descent inside the Sector's bounds, at the point farthest from every other system.
     * A system far outside them skews mods that size the Sector from its systems (MagicLib's
     * bounty distance bands). It has no jump points or star, so it stays unreachable and unseen
     * wherever it sits. Also moves the system of older saves (created at -85000, -85000).
     */
    public static void placeInBounds(StarSystemAPI sys) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        List<Vector2f> others = new ArrayList<Vector2f>();
        for (StarSystemAPI s : Global.getSector().getStarSystems()) {
            if (s == sys) continue;
            Vector2f p = s.getLocation();
            others.add(p);
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        if (others.isEmpty()) return;
        Vector2f at = sys.getLocation();
        if (at.x >= minX && at.x <= maxX && at.y >= minY && at.y <= maxY && !(at.x == 0f && at.y == 0f)) return;
        float bestX = 0f, bestY = 0f, bestDist = -1f;
        int steps = 30;
        for (int i = 1; i < steps; i++) {
            for (int j = 1; j < steps; j++) {
                float x = minX + (maxX - minX) * i / steps;
                float y = minY + (maxY - minY) * j / steps;
                float nearest = Float.MAX_VALUE;
                for (Vector2f p : others) {
                    float dx = p.x - x, dy = p.y - y;
                    nearest = Math.min(nearest, dx * dx + dy * dy);
                }
                if (nearest > bestDist) {
                    bestDist = nearest;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        at.set(bestX, bestY);
        STSLog.info("Run system placed at " + Math.round(bestX) + ", " + Math.round(bestY));
    }

    /**
     * Full wipe: everything but the player and the system center (Node entities, fleets, wrecks,
     * debris). Only call while the player is outside the system, i.e. at Run start.
     */
    public static void clear() {
        StarSystemAPI sys = get();
        if (sys == null) return;
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        Set<SectorEntityToken> doomed = new LinkedHashSet<SectorEntityToken>();
        for (SectorEntityToken e : sys.getAllEntities()) {
            if (e != pf && e != sys.getCenter()) doomed.add(e);
        }
        doomed.addAll(sys.getTerrainCopy());
        for (SectorEntityToken e : doomed) sys.removeEntity(e);
    }

    /** Light cleanup safe during the player's own transition out: Node entities and other fleets. */
    public static void clearNodesAndFleets() {
        StarSystemAPI sys = get();
        if (sys == null) return;
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        List<SectorEntityToken> doomed = new ArrayList<SectorEntityToken>(sys.getEntitiesWithTag(STSConstants.TAG_NODE));
        for (CampaignFleetAPI f : sys.getFleets()) {
            if (f != pf) doomed.add(f);
        }
        for (SectorEntityToken e : doomed) sys.removeEntity(e);
    }

    /**
     * The world entity for a Node, created on demand at its map position. Acts are stacked by
     * global Depth, so a new Act's Start sits exactly where the previous Act's Boss was.
     */
    public static SectorEntityToken ensureNodeEntity(RunNode node) {
        StarSystemAPI sys = ensure();
        SectorEntityToken existing = findNodeEntity(node.getId());
        if (existing != null) return existing;
        String name = node.getType() == NodeType.START ? "Entry Beacon" : node.getType().label();
        SectorEntityToken e = sys.addCustomEntity(null, name, STSConstants.NODE_ENTITY_TYPE, Factions.NEUTRAL);
        e.addTag(STSConstants.TAG_NODE);
        e.getMemoryWithoutUpdate().set(STSConstants.MEM_NODE_ID, node.getId());
        RunState state = RunManager.get();
        int depth = state == null ? node.getDepth() : state.getGlobalDepth(node);
        e.setFixedLocation(node.getColumn() * NODE_SPACING_X - NODE_SPACING_X, -depth * NODE_SPACING_Y);
        return e;
    }

    public static SectorEntityToken findNodeEntity(String nodeId) {
        StarSystemAPI sys = get();
        if (sys == null || nodeId == null) return null;
        for (SectorEntityToken e : sys.getEntitiesWithTag(STSConstants.TAG_NODE)) {
            if (nodeId.equals(e.getMemoryWithoutUpdate().getString(STSConstants.MEM_NODE_ID))) return e;
        }
        return null;
    }

    /** Vanilla gate pattern (GateCMD): dismiss the dialog first, unpause, then transition. */
    public static void jump(SectorEntityToken from, SectorEntityToken to) {
        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        Global.getSector().setPaused(false);
        Global.getSector().doHyperspaceTransition(pf, from, new JumpDestination(to, null), 1f);
        STSLog.info("transition -> " + to.getName());
    }
}
