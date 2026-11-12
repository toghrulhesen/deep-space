package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.logging.Logger;

/**
 * Pause menu shown on top of the frozen gameplay scene.
 * ESC resumes, M returns to the main menu, Q exits the game.
 */
public class PauseState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(PauseState.class.getName());

    private static final String MAP_RESUME = "PauseResume";
    private static final String MAP_MAIN_MENU = "PauseMainMenu";
    private static final String MAP_EXIT = "PauseExit";

    private enum Action { NONE, RESUME, MAIN_MENU }

    private SimpleApplication app;
    private Node guiNode;
    private InputManager inputManager;
    private BitmapFont guiFont;

    private Node pauseRoot;
    private Geometry dimmer;
    private BitmapText titleText;
    private BitmapText resumeText;
    private BitmapText menuText;
    private BitmapText exitText;

    private int lastWidth;
    private int lastHeight;

    // Actions are deferred to update() so they never run in the middle of input dispatch
    private Action pendingAction = Action.NONE;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        this.inputManager = this.app.getInputManager();
        this.guiFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        pauseRoot = new Node("PauseRoot");
        initUI();
        updatePositions();
        initKeys();
    }

    private void initUI() {
        // Semi-transparent overlay that dims the frozen scene
        dimmer = new Geometry("PauseDimmer", new Quad(1, 1));
        Material dimMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        dimMat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.6f));
        dimMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dimmer.setMaterial(dimMat);
        dimmer.setQueueBucket(RenderQueue.Bucket.Gui);
        pauseRoot.attachChild(dimmer);

        titleText = createText("PAUSED", 2.5f, ColorRGBA.Cyan);
        resumeText = createText("Press ESC to Resume", 1.3f, ColorRGBA.Green);
        menuText = createText("Press M for Main Menu", 1.3f, ColorRGBA.Yellow);
        exitText = createText("Press Q to Exit", 1.3f, ColorRGBA.Red);

        pauseRoot.attachChild(titleText);
        pauseRoot.attachChild(resumeText);
        pauseRoot.attachChild(menuText);
        pauseRoot.attachChild(exitText);
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

        // Overlay covers the whole screen, placed behind the texts (z = -1)
        ((Quad) dimmer.getMesh()).updateGeometry(w, h);
        dimmer.setLocalTranslation(0, 0, -1);

        center(titleText, w, h / 2f + 80);
        center(resumeText, w, h / 2f);
        center(menuText, w, h / 2f - 50);
        center(exitText, w, h / 2f - 100);
    }

    private void center(BitmapText text, float screenWidth, float y) {
        text.setLocalTranslation((screenWidth - text.getLineWidth()) / 2f, y, 0);
    }

    private void initKeys() {
        inputManager.addMapping(MAP_RESUME, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping(MAP_MAIN_MENU, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping(MAP_EXIT, new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addListener(actionListener, MAP_RESUME, MAP_MAIN_MENU, MAP_EXIT);
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isEnabled() || !isPressed) return;

        switch (name) {
            case MAP_RESUME -> pendingAction = Action.RESUME;
            case MAP_MAIN_MENU -> pendingAction = Action.MAIN_MENU;
            case MAP_EXIT -> app.stop();
            default -> { }
        }
    };

    @Override
    public void update(float tpf) {
        // Keep the menu centered after resize / fullscreen toggle
        if (app.getCamera().getWidth() != lastWidth
                || app.getCamera().getHeight() != lastHeight) {
            updatePositions();
        }

        Action action = pendingAction;
        pendingAction = Action.NONE;

        switch (action) {
            case RESUME -> {
                logger.info("Resuming game...");
                // cleanup() re-enables GameplayState
                getStateManager().detach(this);
            }
            case MAIN_MENU -> {
                logger.info("Returning to main menu...");
                getStateManager().detach(this);
                GameplayState gameplay = getStateManager().getState(GameplayState.class);
                if (gameplay != null) {
                    getStateManager().detach(gameplay);
                }
                getStateManager().attach(new MainMenuState());
            }
            default -> { }
        }
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_RESUME);
        inputManager.deleteMapping(MAP_MAIN_MENU);
        inputManager.deleteMapping(MAP_EXIT);
        inputManager.removeListener(actionListener);

        // Give control back to gameplay (if it still exists)
        GameplayState gameplay = getStateManager().getState(GameplayState.class);
        if (gameplay != null) {
            gameplay.setEnabled(true);
        }
    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(pauseRoot);
        inputManager.setCursorVisible(true);
        updatePositions();
    }

    @Override
    protected void onDisable() {
        pauseRoot.removeFromParent();
    }
}