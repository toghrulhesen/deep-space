package com.deepspaceminer.states;

import com.deepspaceminer.ui.MenuButton;
import com.deepspaceminer.ui.MenuMouse;
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

import java.util.List;
import java.util.logging.Logger;

/** Title screen. Every entry works with the keyboard and with the mouse. */
public class MainMenuState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(MainMenuState.class.getName());

    private static final String MAP_START = "MenuStart";
    private static final String MAP_SETTINGS = "MenuSettings";
    private static final String MAP_EXIT = "MenuExit";
    private static final String MAP_CLICK = "MenuClick";

    private SimpleApplication app;
    private Node guiNode;
    private InputManager inputManager;
    private BitmapFont guiFont;

    private BitmapText titleText;
    private MenuButton startButton;
    private MenuButton settingsButton;
    private MenuButton exitButton;
    private MenuMouse mouse;

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
        titleText = new BitmapText(guiFont);
        titleText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        titleText.setText("DEEP SPACE MINER");
        titleText.setColor(ColorRGBA.Cyan);

        startButton = new MenuButton(guiFont, "Start Game [Enter]", 1.3f,
                ColorRGBA.Green, this::startGame);
        settingsButton = new MenuButton(guiFont, "Settings [S]", 1.3f,
                ColorRGBA.Yellow, this::openSettings);
        exitButton = new MenuButton(guiFont, "Exit [Q]", 1.3f,
                ColorRGBA.Red, this::exitGame);

        mouse = new MenuMouse(inputManager, MAP_CLICK,
                List.of(startButton, settingsButton, exitButton));
    }

    private void updateMenuPositions() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        lastWidth = app.getCamera().getWidth();
        lastHeight = app.getCamera().getHeight();

        // Center the texts on screen
        titleText.setLocalTranslation(
                (screenWidth - titleText.getLineWidth()) / 2f, screenHeight / 2f + 80, 0);
        startButton.centerHorizontally(screenWidth, screenHeight / 2f);
        settingsButton.centerHorizontally(screenWidth, screenHeight / 2f - 50);
        exitButton.centerHorizontally(screenWidth, screenHeight / 2f - 100);
    }

    private void initKeys() {
        inputManager.addMapping(MAP_START, new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping(MAP_SETTINGS, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(MAP_EXIT, new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addListener(actionListener, MAP_START, MAP_SETTINGS, MAP_EXIT);

        mouse.register();
    }

    private boolean isAltHeld() {
        FullscreenState fullscreen = getStateManager().getState(FullscreenState.class);
        return fullscreen != null && fullscreen.isAltDown();
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isEnabled() || !isPressed) return;

        switch (name) {
            case MAP_START -> {
                // Alt + Enter is the fullscreen shortcut, not "Start"
                if (!isAltHeld()) startGame();
            }
            case MAP_SETTINGS -> openSettings();
            case MAP_EXIT -> exitGame();
            default -> { }
        }
    };

    private void startGame() {
        logger.info("Game is starting, switching to GameplayState...");
        getStateManager().detach(this);
        getStateManager().attach(new GameplayState());
    }

    private void openSettings() {
        logger.info("Opening settings...");
        // The settings screen enables this menu again when it closes
        setEnabled(false);
        getStateManager().attach(new SettingsMenuState(this));
    }

    private void exitGame() {
        app.stop();
    }

    @Override
    public void update(float tpf) {
        // Re-center the menu when the screen size changes (resize / fullscreen toggle)
        if (app.getCamera().getWidth() != lastWidth
                || app.getCamera().getHeight() != lastHeight) {
            updateMenuPositions();
        }
        mouse.updateHover();
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_START);
        inputManager.deleteMapping(MAP_SETTINGS);
        inputManager.deleteMapping(MAP_EXIT);
        inputManager.removeListener(actionListener);
        mouse.unregister();
    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(titleText);
        guiNode.attachChild(startButton.getText());
        guiNode.attachChild(settingsButton.getText());
        guiNode.attachChild(exitButton.getText());
        mouse.setActive(true);
        inputManager.setCursorVisible(true);
        updateMenuPositions();
    }

    @Override
    protected void onDisable() {
        guiNode.detachChild(titleText);
        guiNode.detachChild(startButton.getText());
        guiNode.detachChild(settingsButton.getText());
        guiNode.detachChild(exitButton.getText());
        mouse.setActive(false);
    }
}