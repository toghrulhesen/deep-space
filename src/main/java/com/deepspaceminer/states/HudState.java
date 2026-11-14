package com.deepspaceminer.states;

import com.deepspaceminer.model.Inventory;
import com.deepspaceminer.model.MineralType;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

/**
 * In-game HUD: crosshair, resource counter (top-left) and a "Press E to mine" prompt.
 * Enabled only while the game is running, so it is hidden behind the pause menu.
 */
public class HudState extends BaseAppState {

    private static final float CROSSHAIR_SIZE = 1.5f;
    private static final float TEXT_SIZE = 1.1f;
    private static final float MARGIN = 20f;
    private static final float PROMPT_OFFSET = 40f;

    private final Inventory inventory;
    private final MiningState mining;

    private SimpleApplication app;
    private Node guiNode;
    private BitmapFont guiFont;

    private Node hudRoot;
    private BitmapText crosshair;
    private BitmapText resourcesText;
    private BitmapText promptText;

    // Cached values: texts are rebuilt only when their content changes
    private String lastResources = "";
    private String lastPrompt = "";

    // Last known screen size, used to re-layout the HUD after resize/fullscreen
    private int lastWidth;
    private int lastHeight;

    public HudState(Inventory inventory, MiningState mining) {
        this.inventory = inventory;
        this.mining = mining;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        this.guiFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        hudRoot = new Node("HudRoot");
        crosshair = createText("+", CROSSHAIR_SIZE, ColorRGBA.White);
        resourcesText = createText("", TEXT_SIZE, ColorRGBA.White);
        promptText = createText("", TEXT_SIZE, ColorRGBA.Yellow);

        hudRoot.attachChild(crosshair);
        hudRoot.attachChild(resourcesText);
        hudRoot.attachChild(promptText);

        refreshResources();
        updatePositions();
    }

    private BitmapText createText(String text, float sizeMultiplier, ColorRGBA color) {
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(guiFont.getCharSet().getRenderedSize() * sizeMultiplier);
        bt.setText(text);
        bt.setColor(color);
        return bt;
    }

    private void updatePositions() {
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();
        lastWidth = w;
        lastHeight = h;

        // BitmapText is drawn downwards from its y position
        crosshair.setLocalTranslation(
                (w - crosshair.getLineWidth()) / 2f, h / 2f + crosshair.getLineHeight() / 2f, 0);
        resourcesText.setLocalTranslation(MARGIN, h - MARGIN, 0);
        centerPrompt();
    }

    private void centerPrompt() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        promptText.setLocalTranslation((w - promptText.getLineWidth()) / 2f, h / 2f - PROMPT_OFFSET, 0);
    }

    @Override
    public void update(float tpf) {
        if (app.getCamera().getWidth() != lastWidth
                || app.getCamera().getHeight() != lastHeight) {
            updatePositions();
        }
        refreshResources();
        refreshPrompt();
    }

    private void refreshResources() {
        StringBuilder sb = new StringBuilder("RESOURCES");
        for (MineralType type : MineralType.values()) {
            sb.append('\n').append(type.getDisplayName()).append(": ").append(inventory.get(type));
        }

        String text = sb.toString();
        if (!text.equals(lastResources)) {
            lastResources = text;
            resourcesText.setText(text);
        }
    }

    private void refreshPrompt() {
        MineralType target = mining.getTargetType();
        String text = target == null ? "" : "Press E to mine " + target.getDisplayName();

        if (!text.equals(lastPrompt)) {
            lastPrompt = text;
            promptText.setText(text);
            centerPrompt();
        }
    }

    @Override
    protected void cleanup(Application app) { }

    @Override
    protected void onEnable() {
        guiNode.attachChild(hudRoot);
        updatePositions();
    }

    @Override
    protected void onDisable() {
        hudRoot.removeFromParent();
    }
}