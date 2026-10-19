package com.slaythesector.combat;

import java.awt.Color;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

/**
 * Ambush: shows which enemy ship is the marked target, with a pulsing ring around it in the
 * battle and a status line naming it; announces when it is destroyed or escapes.
 */
public class MarkedTargetPlugin extends BaseEveryFrameCombatPlugin {

    private static final Color RING = new Color(255, 120, 40);
    private static final Object STATUS_KEY = new Object();

    private final String memberId;
    private ShipAPI target;
    private float time;
    private boolean announced;
    private boolean over;

    public MarkedTargetPlugin(String memberId) {
        this.memberId = memberId;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || over) return;
        time += amount;
        if (target == null) {
            for (ShipAPI s : engine.getShips()) {
                if (memberId.equals(s.getFleetMemberId())) {
                    target = s;
                    break;
                }
            }
            if (target == null) return;
        }
        String name = target.getName() + " (" + target.getHullSpec().getHullName() + ")";
        if (!announced && engine.getCombatUI() != null) {
            engine.getCombatUI().addMessage(1, RING, "Marked target: " + name + ". Destroy it before it escapes.");
            announced = true;
        }
        if (!target.isAlive() || target.isHulk()) {
            if (engine.getCombatUI() != null) engine.getCombatUI().addMessage(1, RING, "Marked target destroyed.");
            over = true;
            return;
        }
        if (!engine.isInPlay(target)) {
            if (engine.getCombatUI() != null) engine.getCombatUI().addMessage(1, RING, "The marked target escaped.");
            over = true;
            return;
        }
        engine.maintainStatusForPlayerShip(STATUS_KEY, "graphics/icons/hullsys/targeting_feed.png",
                "Marked target", name, true);
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        if (target == null || over) return;
        float r = target.getCollisionRadius() + 40f + 10f * (float) Math.sin(time * 4f);
        float x = target.getLocation().x;
        float y = target.getLocation().y;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3f);
        GL11.glColor4f(RING.getRed() / 255f, RING.getGreen() / 255f, RING.getBlue() / 255f, 0.8f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            GL11.glVertex2f(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }
}
