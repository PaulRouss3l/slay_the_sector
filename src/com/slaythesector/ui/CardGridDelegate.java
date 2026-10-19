package com.slaythesector.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The card grid: the one reusable picker (Starting pick, Reward pick, later Shop and Treasure).
 * SINGLE mode: each card has a Pick button. BUDGET mode: +/- per card against a cost budget.
 * Open with {@code dialog.showCustomVisualDialog(WIDTH, HEIGHT, grid)}; the listener runs after
 * the grid has closed.
 */
public class CardGridDelegate implements CustomVisualDialogDelegate {

    public enum Mode { SINGLE, BUDGET }

    public interface Listener {
        /** SINGLE: one card. BUDGET: the picked cards, repeated per copy. Empty when cancelled. */
        void done(List<Card> picked);
    }

    public static final float WIDTH = 1000f;
    public static final float HEIGHT = 640f;
    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final float CARD_W = 236f;
    private static final float CARD_H = 250f;
    private static final float HEADER_H = 90f;

    private final String title;
    private final List<Card> cards;
    private final Mode mode;
    private final int budget;
    private final String cancelText;
    private final Listener listener;
    private final int[] counts;
    /** BUDGET mode: what the budget counts, e.g. "Deployment points" or "Supplies". */
    private String budgetLabel = "Deployment points";
    /** BUDGET mode: show a "Random" button that fills the budget with random cards. */
    private boolean randomButton;

    private CustomPanelAPI root;
    private CustomPanelAPI content;
    private DialogCallbacks callbacks;
    private LabelAPI status;
    private ButtonAPI confirm;
    private int page;
    /** Rebuild on the next frame, not inside the click that asked for it (the clicked panel is removed). */
    private boolean dirty;
    /** Guards against a double click closing (and applying) twice. */
    private boolean closed;
    private List<Card> result = new ArrayList<Card>();

    public CardGridDelegate(String title, List<Card> cards, Mode mode, int budget, String cancelText, Listener listener) {
        this.title = title;
        this.cards = cards;
        this.mode = mode;
        this.budget = budget;
        this.cancelText = cancelText;
        this.listener = listener;
        this.counts = new int[cards.size()];
    }

    public CardGridDelegate withBudgetLabel(String label) {
        this.budgetLabel = label;
        return this;
    }

    public CardGridDelegate withRandomButton() {
        this.randomButton = true;
        return this;
    }

    private final CustomUIPanelPlugin plugin = new CustomUIPanelPlugin() {
        @Override public void positionChanged(PositionAPI position) {}
        @Override public void renderBelow(float alphaMult) {}
        @Override public void render(float alphaMult) {}
        @Override public void advance(float amount) {}

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI e : events) {
                if (!e.isConsumed() && e.isKeyDownEvent() && e.getEventValue() == Keyboard.KEY_ESCAPE) {
                    close(new ArrayList<Card>());
                    e.consume();
                }
            }
        }

        @Override
        public void buttonPressed(Object id) {
            onButton(String.valueOf(id));
        }
    };

    /** Plugin of the rebuilt card panel: forwards button clicks only, no per-frame work. */
    private final CustomUIPanelPlugin buttonsOnly = new CustomUIPanelPlugin() {
        @Override public void positionChanged(PositionAPI position) {}
        @Override public void renderBelow(float alphaMult) {}
        @Override public void render(float alphaMult) {}
        @Override public void advance(float amount) {}
        @Override public void processInput(List<InputEventAPI> events) {}

        @Override
        public void buttonPressed(Object id) {
            onButton(String.valueOf(id));
        }
    };

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.root = panel;
        this.callbacks = callbacks;

        TooltipMakerAPI header = panel.createUIElement(WIDTH - 20f, HEADER_H, false);
        header.addTitle(title);
        status = header.addPara("", 6f);
        ButtonAPI prev = header.addButton("< Prev", "prev", 90f, 24f, 8f);
        ButtonAPI next = header.addButton("Next >", "next", 90f, 24f, 0f);
        next.getPosition().rightOfTop(prev, 8f);
        ButtonAPI cancel = header.addButton(cancelText, "cancel", 140f, 24f, 0f);
        cancel.getPosition().rightOfTop(next, 24f);
        if (mode == Mode.BUDGET) {
            confirm = header.addButton("Confirm", "confirm", 140f, 24f, 0f);
            confirm.getPosition().rightOfTop(cancel, 8f);
            if (randomButton) {
                ButtonAPI random = header.addButton("Random", "random", 120f, 24f, 0f);
                random.getPosition().rightOfTop(confirm, 24f);
            }
        }
        panel.addUIElement(header).inTL(10f, 6f);
        rebuild();
    }

    private void onButton(String id) {
        if (closed) return;
        if (id.equals("cancel")) {
            close(new ArrayList<Card>());
        } else if (id.equals("confirm")) {
            List<Card> picks = new ArrayList<Card>();
            for (int i = 0; i < cards.size(); i++) {
                for (int k = 0; k < counts[i]; k++) picks.add(cards.get(i));
            }
            close(picks);
        } else if (id.equals("random")) {
            fillRandom();
            dirty = true;
        } else if (id.equals("prev")) {
            if (page > 0) page--;
            dirty = true;
        } else if (id.equals("next")) {
            if ((page + 1) * COLS * ROWS < cards.size()) page++;
            dirty = true;
        } else if (id.startsWith("pick:")) {
            List<Card> picks = new ArrayList<Card>();
            picks.add(cards.get(Integer.parseInt(id.substring(5))));
            close(picks);
        } else if (id.startsWith("add:")) {
            int i = Integer.parseInt(id.substring(4));
            if (canAdd(i)) counts[i]++;
            dirty = true;
        } else if (id.startsWith("rem:")) {
            int i = Integer.parseInt(id.substring(4));
            if (counts[i] > 0) counts[i]--;
            dirty = true;
        }
    }

    private void close(List<Card> picks) {
        if (closed) return;
        closed = true;
        result = picks;
        callbacks.dismissDialog();
    }

    /** Clears the picks, then adds random affordable cards until none fits; each press rolls again. */
    private void fillRandom() {
        // Not security-sensitive: a random starting fleet.
        Random rng = new Random(); // nosemgrep: java-lang-security-audit-crypto-weak-random
        Arrays.fill(counts, 0);
        List<Integer> fits = new ArrayList<Integer>();
        do {
            fits.clear();
            for (int i = 0; i < cards.size(); i++) {
                if (cards.get(i).cost > 0 && canAdd(i)) fits.add(i);
            }
            if (!fits.isEmpty()) counts[fits.get(rng.nextInt(fits.size()))]++;
        } while (!fits.isEmpty());
    }

    private boolean canAdd(int i) {
        Card c = cards.get(i);
        return used() + c.cost <= budget && (c.maxCount <= 0 || counts[i] < c.maxCount);
    }

    private int used() {
        int u = 0;
        for (int i = 0; i < cards.size(); i++) u += counts[i] * cards.get(i).cost;
        return u;
    }

    private void rebuild() {
        if (content != null) root.removeComponent(content);
        content = root.createCustomPanel(WIDTH, HEIGHT - HEADER_H, buttonsOnly);

        int perPage = COLS * ROWS;
        int pages = Math.max(1, (cards.size() + perPage - 1) / perPage);
        String pageText = "Page " + (page + 1) + "/" + pages;
        if (mode == Mode.BUDGET) {
            status.setText(budgetLabel + ": " + used() + " / " + budget + "     " + pageText);
            confirm.setEnabled(used() > 0);
        } else {
            status.setText(pageText);
        }

        for (int slot = 0; slot < perPage; slot++) {
            int i = page * perPage + slot;
            if (i >= cards.size()) break;
            TooltipMakerAPI ui = content.createUIElement(CARD_W, CARD_H, false);
            buildCard(ui, i);
            float x = 10f + (slot % COLS) * (CARD_W + 10f);
            float y = (slot / COLS) * (CARD_H + 10f);
            content.addUIElement(ui).inTL(x, y);
        }
        root.addComponent(content).inTL(0f, HEADER_H + 6f);
    }

    private void buildCard(TooltipMakerAPI ui, int i) {
        Card c = cards.get(i);
        Color base = Misc.getBasePlayerColor();
        if (c.preview != null) {
            List<FleetMemberAPI> one = new ArrayList<FleetMemberAPI>();
            one.add(c.preview);
            ui.addShipList(1, 1, 96f, base, one, 0f);
        }
        ui.addPara(c.title, Misc.getHighlightColor(), 6f);
        ui.addPara(c.subtitle, 3f);
        if (mode == Mode.SINGLE) {
            ui.addButton("Pick", "pick:" + i, CARD_W - 20f, 24f, 8f);
        } else {
            ui.addPara("Selected: " + counts[i], 4f);
            ButtonAPI add = ui.addButton("+ Add", "add:" + i, 100f, 24f, 6f);
            add.setEnabled(canAdd(i));
            ButtonAPI rem = ui.addButton("- Remove", "rem:" + i, 100f, 24f, 0f);
            rem.getPosition().rightOfTop(add, 8f);
            rem.setEnabled(counts[i] > 0);
        }
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return plugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    /** Rebuilds here, outside the panels' own update loops. */
    @Override
    public void advance(float amount) {
        if (dirty && !closed) {
            dirty = false;
            rebuild();
        }
    }

    @Override
    public void reportDismissed(int option) {
        listener.done(result);
    }
}
