package com.slaythesector.ui;

import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.map.NodeText;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * The node map as a picker: hover a Node to read it, click a selectable one, then "Set course".
 * Open with {@code dialog.showCustomVisualDialog(WIDTH, HEIGHT, map)}; the listener runs after
 * the map has closed, with the chosen Node or null.
 */
public class NodeMapDelegate implements CustomVisualDialogDelegate {

    public interface Listener {
        void done(RunNode chosen);
    }

    public static final float WIDTH = 1000f;
    public static final float HEIGHT = 680f;
    private static final float HEADER_H = 100f;

    private final RunState state;
    private final String note;
    private final boolean canChoose;
    private final Listener listener;

    private DialogCallbacks callbacks;
    private NodeMapPanel map;
    private LabelAPI info;
    private ButtonAPI confirm;
    private RunNode selected;
    private RunNode result;
    private boolean closed;

    /**
     * @param note      extra line under the title (e.g. why courses are locked), or null
     * @param canChoose false shows the map without letting the player pick (e.g. over the DP cap)
     */
    public NodeMapDelegate(RunState state, String note, boolean canChoose, Listener listener) {
        this.state = state;
        this.note = note;
        this.canChoose = canChoose;
        this.listener = listener;
    }

    private final CustomUIPanelPlugin plugin = new CustomUIPanelPlugin() {
        private PositionAPI pos;

        @Override
        public void positionChanged(PositionAPI position) {
            pos = position;
        }

        /** Opaque backdrop: the interaction dialog's text would otherwise show through the map. */
        @Override
        public void renderBelow(float alphaMult) {
            if (pos == null) return;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(0.02f, 0.03f, 0.05f, 0.95f * alphaMult);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(pos.getX(), pos.getY());
            GL11.glVertex2f(pos.getX() + pos.getWidth(), pos.getY());
            GL11.glVertex2f(pos.getX() + pos.getWidth(), pos.getY() + pos.getHeight());
            GL11.glVertex2f(pos.getX(), pos.getY() + pos.getHeight());
            GL11.glEnd();
            GL11.glPopAttrib();
        }
        @Override public void render(float alphaMult) {}
        @Override public void advance(float amount) {}

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI e : events) {
                if (!e.isConsumed() && e.isKeyDownEvent() && e.getEventValue() == Keyboard.KEY_ESCAPE) {
                    close(null);
                    e.consume();
                }
            }
        }

        @Override
        public void buttonPressed(Object id) {
            if ("close".equals(id)) close(null);
            else if ("confirm".equals(id) && selected != null) close(selected);
        }
    };

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;

        TooltipMakerAPI header = panel.createUIElement(WIDTH - 20f, HEADER_H, false);
        header.addTitle("The Descent: Act " + state.getAct());
        // The note (why choosing is locked) stays visible; the info line follows the cursor.
        if (note != null) header.addPara(note, Misc.getNegativeHighlightColor(), 4f);
        info = header.addPara(canChoose ? "Hover a Node to read it. Click a highlighted one to choose your next course."
                : "Hover a Node to read it.", 4f);
        confirm = header.addButton("Set course", "confirm", 140f, 24f, 8f);
        confirm.setEnabled(false);
        ButtonAPI close = header.addButton("Close", "close", 120f, 24f, 0f);
        close.getPosition().rightOfTop(confirm, 8f);
        panel.addUIElement(header).inTL(10f, 6f);

        map = new NodeMapPanel(state, canChoose, new NodeMapPanel.Listener() {
            @Override
            public void hovered(RunNode node) {
                if (node != null) info.setText(describe(node));
                else if (selected != null) info.setText(describe(selected));
            }

            @Override
            public void clicked(RunNode node) {
                if (!canChoose || !map.isChoice(node)) {
                    info.setText(describe(node) + (canChoose ? " Not reachable from here." : " Locked: see above."));
                    return;
                }
                selected = node;
                map.setSelected(node);
                confirm.setEnabled(true);
                info.setText("Selected: " + describe(node));
            }
        });
        float bodyH = HEIGHT - HEADER_H - 10f;
        CustomPanelAPI body = panel.createCustomPanel(WIDTH - NodeMapPanel.LEGEND_W - 20f, bodyH, map);
        panel.addComponent(body).inTL(0f, HEADER_H + 10f);
        panel.addUIElement(NodeMapPanel.legend(panel, state, bodyH)).inTL(WIDTH - NodeMapPanel.LEGEND_W - 10f, HEADER_H + 10f);
    }

    private static String describe(RunNode n) {
        String s = NodeText.full(n) + ". " + NodeText.blurb(n.getType());
        if (n.isResolved()) s += " (visited)";
        return s;
    }

    private void close(RunNode chosen) {
        if (closed) return;
        closed = true;
        result = chosen;
        callbacks.dismissDialog();
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return plugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {}

    @Override
    public void reportDismissed(int option) {
        listener.done(result);
    }
}
