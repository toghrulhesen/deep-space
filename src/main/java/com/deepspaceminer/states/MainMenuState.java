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
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

import java.util.logging.Logger;

public class MainMenuState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(MainMenuState.class.getName());

    private SimpleApplication app;
    private Node guiNode;
    private InputManager inputManager;
    private BitmapFont guiFont;

    private BitmapText titleText;
    private BitmapText startText;
    private BitmapText exitText;

    // Last known screen size, used to re-center the menu after resize/fullscreen
    private int lastWidth;
    private int lastHeight;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        this.inputManager = this.app.getInputManager();
        this.guiFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        initUI();
        updateMenuPositions();
        initKeys();
    }

    private void initUI() {
        titleText = new BitmapText(guiFont, false);
        titleText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        titleText.setText("DEEP SPACE MINER");
        titleText.setColor(ColorRGBA.Cyan);

        startText = new BitmapText(guiFont, false);
        startText.setSize(guiFont.getCharSet().getRenderedSize() * 1.3f);
        startText.setText("Press ENTER to Start");
        startText.setColor(ColorRGBA.Green);

        exitText = new BitmapText(guiFont, false);
        exitText.setSize(guiFont.getCharSet().getRenderedSize() * 1.3f);
        exitText.setText("Press Q to Exit");
        exitText.setColor(ColorRGBA.Red);
    }

    private void updateMenuPositions() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        lastWidth = app.getCamera().getWidth();
        lastHeight = app.getCamera().getHeight();

        float tWidth = titleText.getLineWidth();
        float sWidth = startText.getLineWidth();
        float eWidth = exitText.getLineWidth();

        // Center the texts on screen
        titleText.setLocalTranslation((screenWidth - tWidth) / 2f, screenHeight / 2f + 80, 0);
        startText.setLocalTranslation((screenWidth - sWidth) / 2f, screenHeight / 2f, 0);
        exitText.setLocalTranslation((screenWidth - eWidth) / 2f, screenHeight / 2f - 50, 0);
    }

    private void initKeys() {
        inputManager.addMapping("MenuStart", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("MenuExit", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addListener(actionListener, "MenuStart", "MenuExit");
    }

    private boolean isAltHeld() {
        FullscreenState fullscreen = getStateManager().getState(FullscreenState.class);
        return fullscreen != null && fullscreen.isAltDown();
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isEnabled()) return;

            if (name.equals("MenuStart") && isPressed) {
                // Alt + Enter is the fullscreen shortcut, not "Start"
                if (isAltHeld()) return;

                logger.info("Game is starting, switching to GameplayState...");
                getStateManager().detach(MainMenuState.this);
                getStateManager().attach(new GameplayState());
            }
            if (name.equals("MenuExit") && isPressed) {
                app.stop();
            }
        }
    };

    @Override
    public void update(float tpf) {
        // Re-center the menu when the screen size changes (resize / fullscreen toggle)
        if (app.getCamera().getWidth() != lastWidth
                || app.getCamera().getHeight() != lastHeight) {
            updateMenuPositions();
        }
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping("MenuStart");
        inputManager.deleteMapping("MenuExit");
        inputManager.removeListener(actionListener);
    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(titleText);
        guiNode.attachChild(startText);
        guiNode.attachChild(exitText);
        inputManager.setCursorVisible(true);
        updateMenuPositions();
    }

    @Override
    protected void onDisable() {
        guiNode.detachChild(titleText);
        guiNode.detachChild(startText);
        guiNode.detachChild(exitText);
    }
}