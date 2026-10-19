package com.slaythesector.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.run.NodeType;
import com.slaythesector.run.RunNode;
import com.slaythesector.run.RunState;

/**
 * Draws a Run's node map (Start at the bottom, Boss at the top) with GL lines and the Node-type
 * icons ({@code graphics.sts_map} in settings.json). Interactive mode reports hovers and clicks
 * on the selectable Nodes (successors of the current Node, Path lock); read-only mode (Intel)
 * only draws.
 */
public class NodeMapPanel implements CustomUIPanelPlugin {

    public interface Listener {
        /** Hovered Node, or null when the cursor leaves every Node. */
        void hovered(RunNode node);

        void clicked(RunNode node);
    }

    private static final float ICON = 30f;
    private static final float ICON_BOSS = 46f;
    private static final float MARGIN_X = 60f;
    private static final float MARGIN_Y = 34f;

    private static final Color EDGE = new Color(90, 120, 150);
    private static final Color EDGE_TAKEN = new Color(255, 200, 80);
    private static final Color EDGE_CHOICE = new Color(120, 220, 255);
    private static final Color RING_CURRENT = new Color(255, 200, 80);
    private static final Color RING_CHOICE = new Color(120, 220, 255);
    private static final Color RING_SELECTED = new Color(255, 255, 255);
    /** The Rest icon is a white station glyph; green keeps it apart from the white Combat crosshair. */
    private static final Color REST_TINT = new Color(120, 230, 140);

    private final RunState state;
    private final Listener listener;
    private final Set<String> choices = new HashSet<String>();
    private final Map<String, float[]> layout = new HashMap<String, float[]>();
    private PositionAPI pos;
    private RunNode hovered;
    private RunNode selected;
    private float time;

    /**
     * {@code listener} null = read-only. {@code canChoose} false still reports hovers and clicks
     * but highlights no Node as selectable (e.g. over the Fleet DP cap).
     */
    public NodeMapPanel(RunState state, boolean canChoose, Listener listener) {
        this.state = state;
        this.listener = listener;
        if (listener != null && canChoose && state.getTargetNodeId() == null) {
            for (RunNode n : state.getChoices()) choices.add(n.getId());
        }
    }

    public void setSelected(RunNode node) {
        selected = node;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        pos = position;
        layout.clear();
        Map<Integer, List<RunNode>> rows = new HashMap<Integer, List<RunNode>>();
        int maxDepth = 1;
        for (RunNode n : state.getNodes()) {
            List<RunNode> row = rows.get(n.getDepth());
            if (row == null) {
                row = new ArrayList<RunNode>();
                rows.put(n.getDepth(), row);
            }
            row.add(n);
            maxDepth = Math.max(maxDepth, n.getDepth());
        }
        float w = pos.getWidth() - 2f * MARGIN_X;
        float h = pos.getHeight() - 2f * MARGIN_Y;
        for (List<RunNode> row : rows.values()) {
            for (RunNode n : row) {
                // Grid maps place Nodes by grid column; older maps spread each row evenly.
                int cols = state.getGridColumns();
                float x = cols > 0
                        ? pos.getX() + MARGIN_X + w * (n.getColumn() + 0.5f) / cols
                        : pos.getX() + MARGIN_X + w * (n.getColumn() + 0.5f) / row.size();
                // GL y runs bottom-up: Depth 0 at the bottom, the Boss at the top.
                float y = pos.getY() + MARGIN_Y + h * n.getDepth() / maxDepth;
                layout.put(n.getId(), new float[] { x, y });
            }
        }
    }

    @Override
    public void renderBelow(float alphaMult) {}

    @Override
    public void render(float alphaMult) {
        if (pos == null) return;
        // Leave no GL state behind (line smoothing/width, blend) for the rest of the UI.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        for (RunNode n : state.getNodes()) {
            float[] a = layout.get(n.getId());
            if (a == null) continue;
            for (String sid : n.getSuccessorIds()) {
                float[] b = layout.get(sid);
                RunNode s = state.getNode(sid);
                if (b == null || s == null) continue;
                Color c = EDGE;
                float alpha = 0.35f;
                float width = 1.5f;
                if (isOnPath(n) && (s.isResolved() || sid.equals(state.getTargetNodeId()))) {
                    c = EDGE_TAKEN;
                    alpha = 0.9f;
                    width = 3f;
                } else if (n.getId().equals(state.getCurrentNodeId()) && choices.contains(sid)) {
                    c = EDGE_CHOICE;
                    alpha = 0.8f;
                    width = 2.5f;
                }
                line(a[0], a[1], b[0], b[1], c, alpha * alphaMult, width);
            }
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 4f);
        for (RunNode n : state.getNodes()) {
            float[] p = layout.get(n.getId());
            if (p == null) continue;
            float size = n.getType() == NodeType.BOSS ? ICON_BOSS : ICON;
            boolean current = n.getId().equals(state.getCurrentNodeId());
            boolean target = n.getId().equals(state.getTargetNodeId());
            boolean choice = choices.contains(n.getId());
            float bright = n.isResolved() && !current ? 0.45f : (choice || current || target ? 1f : 0.7f);
            if (n == hovered && choice) size *= 1.2f;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (n == selected) ring(p[0], p[1], size * 0.85f, RING_SELECTED, alphaMult, 3f);
            if (current || target) ring(p[0], p[1], size * 0.75f, RING_CURRENT, alphaMult, 2.5f);
            else if (choice) ring(p[0], p[1], size * 0.75f, RING_CHOICE, (0.5f + 0.5f * pulse) * alphaMult, 2f);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            SpriteAPI icon = Global.getSettings().getSprite("sts_map", n.getType().name());
            if (icon == null) continue;
            icon.setSize(size, size);
            Color tint = n.getType() == NodeType.REST ? REST_TINT : Color.WHITE;
            icon.setColor(new Color(tint.getRed() / 255f * bright, tint.getGreen() / 255f * bright, tint.getBlue() / 255f * bright));
            icon.setAlphaMult(alphaMult);
            icon.renderAtCenter(p[0], p[1]);
        }
        GL11.glPopAttrib();
    }

    /** A Node is on the travelled path if it was resolved, or is where the player stands. */
    private boolean isOnPath(RunNode n) {
        return n.isResolved() || n.getType() == NodeType.START || n.getId().equals(state.getCurrentNodeId());
    }

    private static void line(float x1, float y1, float x2, float y2, Color c, float alpha, float width) {
        GL11.glLineWidth(width);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    private static void ring(float x, float y, float r, Color c, float alpha, float width) {
        GL11.glLineWidth(width);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32;
            GL11.glVertex2f(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r);
        }
        GL11.glEnd();
    }

    @Override
    public void advance(float amount) {
        time += amount;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (listener == null || pos == null) return;
        for (InputEventAPI e : events) {
            if (e.isConsumed()) continue;
            if (e.isMouseMoveEvent()) {
                RunNode h = hit(e.getX(), e.getY());
                if (h != hovered) {
                    hovered = h;
                    listener.hovered(h);
                }
            } else if (e.isLMBDownEvent()) {
                RunNode h = hit(e.getX(), e.getY());
                if (h != null) {
                    listener.clicked(h);
                    e.consume();
                }
            }
        }
    }

    private RunNode hit(float mx, float my) {
        for (RunNode n : state.getNodes()) {
            float[] p = layout.get(n.getId());
            if (p == null) continue;
            float r = (n.getType() == NodeType.BOSS ? ICON_BOSS : ICON) * 0.6f + 4f;
            if (Math.abs(mx - p[0]) <= r && Math.abs(my - p[1]) <= r) return n;
        }
        return null;
    }

    /** Width of the legend column built by {@link #legend}. */
    public static final float LEGEND_W = 190f;

    /**
     * Legend for the map: icon + name + one-line meaning of each Node type present in the Run, then
     * what the rings and the gold path mean. The caller places the returned element.
     */
    public static TooltipMakerAPI legend(CustomPanelAPI parent, RunState state, float height) {
        TooltipMakerAPI ui = parent.createUIElement(LEGEND_W, height, false);
        ui.addSectionHeading("Legend", Alignment.MID, 0f);
        EnumSet<NodeType> present = EnumSet.noneOf(NodeType.class);
        for (RunNode n : state.getNodes()) present.add(n.getType());
        for (NodeType type : present) {
            TooltipMakerAPI row = ui.beginImageWithText(Global.getSettings().getSpriteName("sts_map", type.name()), 24f);
            row.addPara(type.label(), Misc.getHighlightColor(), 0f);
            row.addPara(LEGEND_TEXT.get(type), Misc.getGrayColor(), 0f);
            ui.addImageWithText(8f);
        }
        ui.addPara("Gold ring: where you are or your locked course.", RING_CURRENT, 12f);
        ui.addPara("Pulsing blue ring: Nodes you can choose next.", RING_CHOICE, 4f);
        ui.addPara("Gold line: the path travelled.", EDGE_TAKEN, 4f);
        ui.addPara("Dimmed: visited.", Misc.getGrayColor(), 4f);
        return ui;
    }

    private static final Map<NodeType, String> LEGEND_TEXT = new EnumMap<NodeType, String>(NodeType.class);
    static {
        LEGEND_TEXT.put(NodeType.START, "Entry of the Descent");
        LEGEND_TEXT.put(NodeType.COMBAT, "Fight: Supplies + Reward");
        LEGEND_TEXT.put(NodeType.BOSS, "The Apex, no retreat");
        LEGEND_TEXT.put(NodeType.SHOP, "Buy with Supplies");
        LEGEND_TEXT.put(NodeType.REST, "Repair, S-mod or train");
        LEGEND_TEXT.put(NodeType.TREASURE, "One rare prize");
        LEGEND_TEXT.put(NodeType.ELITE, "Stronger fleet, escape it");
        LEGEND_TEXT.put(NodeType.AMBUSH, "Enemy flees, kill the mark");
        LEGEND_TEXT.put(NodeType.EVENT, "Unknown: a choice");
    }

    /** Whether the Node can be chosen as the next course. */
    public boolean isChoice(RunNode n) {
        return n != null && choices.contains(n.getId());
    }

    @Override
    public void buttonPressed(Object buttonId) {}
}
