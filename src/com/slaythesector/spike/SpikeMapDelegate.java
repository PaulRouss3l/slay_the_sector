package com.slaythesector.spike;

import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.slaythesector.util.STSLog;

/**
 * Spike: a GL-drawn node map in {@code showCustomVisualDialog} that reacts to clicks.
 * Proves custom drawing + mouse input; the real map comes in S2.
 */
public class SpikeMapDelegate implements CustomVisualDialogDelegate {

    public static final float WIDTH = 900f;
    public static final float HEIGHT = 600f;

    private static final String BTN_CLOSE = "sts_spike_map_close";
    private static final float NODE_SIZE = 28f;

    /** Node positions as fractions of the panel: 1 start, 3 middle, 1 boss. */
    private static final float[][] NODES = {
            { 0.5f, 0.12f }, { 0.25f, 0.45f }, { 0.5f, 0.45f }, { 0.75f, 0.45f }, { 0.5f, 0.82f } };
    private static final int[][] EDGES = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 4 }, { 2, 4 }, { 3, 4 } };

    private DialogCallbacks callbacks;
    private LabelAPI label;
    private int selected = -1;
    private int hovered = -1;

    private final CustomUIPanelPlugin panelPlugin = new CustomUIPanelPlugin() {
        private PositionAPI pos;

        @Override
        public void positionChanged(PositionAPI position) {
            pos = position;
        }

        @Override
        public void renderBelow(float alphaMult) {}

        @Override
        public void render(float alphaMult) {
            if (pos == null) return;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL11.glLineWidth(2f);
            GL11.glColor4f(0.5f, 0.7f, 0.9f, 0.6f * alphaMult);
            GL11.glBegin(GL11.GL_LINES);
            for (int[] e : EDGES) {
                GL11.glVertex2f(x(e[0]), y(e[0]));
                GL11.glVertex2f(x(e[1]), y(e[1]));
            }
            GL11.glEnd();

            for (int i = 0; i < NODES.length; i++) {
                if (i == selected) GL11.glColor4f(1f, 0.8f, 0.2f, alphaMult);
                else if (i == hovered) GL11.glColor4f(0.8f, 0.9f, 1f, alphaMult);
                else GL11.glColor4f(0.3f, 0.5f, 0.8f, alphaMult);
                float h = NODE_SIZE / 2f;
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(x(i) - h, y(i) - h);
                GL11.glVertex2f(x(i) + h, y(i) - h);
                GL11.glVertex2f(x(i) + h, y(i) + h);
                GL11.glVertex2f(x(i) - h, y(i) + h);
                GL11.glEnd();
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        @Override
        public void advance(float amount) {}

        @Override
        public void processInput(List<InputEventAPI> events) {
            if (pos == null) return;
            for (InputEventAPI e : events) {
                if (e.isConsumed()) continue;
                if (e.isMouseMoveEvent()) {
                    hovered = hit(e.getX(), e.getY());
                } else if (e.isLMBDownEvent()) {
                    int node = hit(e.getX(), e.getY());
                    if (node >= 0) {
                        selected = node;
                        label.setText("Selected node " + node + " — click works. Esc or Close to exit.");
                        STSLog.info("spike: map node clicked " + node);
                        e.consume();
                    }
                } else if (e.isKeyDownEvent() && e.getEventValue() == Keyboard.KEY_ESCAPE) {
                    callbacks.dismissDialog();
                    e.consume();
                }
            }
        }

        @Override
        public void buttonPressed(Object buttonId) {
            if (BTN_CLOSE.equals(buttonId)) callbacks.dismissDialog();
        }

        private float x(int i) {
            return pos.getX() + NODES[i][0] * pos.getWidth();
        }

        private float y(int i) {
            // Fractions run top-down; GL y runs bottom-up.
            return pos.getY() + (1f - NODES[i][1]) * pos.getHeight();
        }

        private int hit(float mx, float my) {
            float h = NODE_SIZE / 2f + 4f;
            for (int i = 0; i < NODES.length; i++) {
                if (Math.abs(mx - x(i)) <= h && Math.abs(my - y(i)) <= h) return i;
            }
            return -1;
        }
    };

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;
        TooltipMakerAPI ui = panel.createUIElement(WIDTH - 20f, 60f, false);
        label = ui.addPara("[STS spike] Node map canvas — click a node.", 0f);
        ui.addButton("Close", BTN_CLOSE, 120f, 24f, 10f);
        panel.addUIElement(ui).inTL(10f, 10f);
        STSLog.info("spike: map canvas opened");
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelPlugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {}

    @Override
    public void reportDismissed(int option) {
        STSLog.info("spike: map canvas dismissed (selected=" + selected + ")");
    }
}
