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
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.List;
import java.util.logging.Logger;

/**
 * Settings screen, opened from the main menu or the pause menu.
 * The menu that opened it is disabled while this screen is open and is enabled again
 * when this state is detached. Click the Invert Y line (or press I) to toggle it,
 * click Back (or press ESC) to go back.
 */
public class SettingsMenuState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(SettingsMenuState.class.getName());

    private static final String MAP_TOGGLE_INVERT_Y = "SettingsToggleInvertY";
    private static final String MAP_BACK = "SettingsBack";
    private static final String MAP_CLICK = "SettingsClick";

    // The menu that opened the settings (main menu or pause menu)
    private final BaseAppState parent;

    private SimpleApplication app;
    private Node guiNode;
    private InputManager inputManager;
    private BitmapFont guiFont;
    private GameSettingsState settings;

    private Node settingsRoot;
    private Geometry dimmer;
    private BitmapText titleText;
    private MenuButton invertYButton;
    private MenuButton backButton;
    private MenuMouse mouse;

    private int lastWidth;
    private int lastHeight;

    // Leaving the screen is deferred to update() so it never runs in the middle of input dispatch
    private boolean pendingBack;

    public SettingsMenuState(BaseAppState parent) {
        this.parent = parent;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.guiNode = this.app.getGuiNode();
        this.inputManager = this.app.getInputManager();
        this.guiFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        settings = getStateManager().getState(GameSettingsState.class);
        if (settings == null) {
            throw new IllegalStateException("GameSettingsState must be attached in Main");
        }

        settingsRoot = new Node("SettingsRoot");
        initUI();
        refreshInvertYButton();
        updatePositions();
        initKeys();
    }

    private void initUI() {
        // Semi-transparent overlay, same look as the pause menu
        dimmer = new Geometry("SettingsDimmer", new Quad(1, 1));
        Material dimMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        dimMat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.6f));
        dimMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dimmer.setMaterial(dimMat);
        dimmer.setQueueBucket(RenderQueue.Bucket.Gui);
        settingsRoot.attachChild(dimmer);

        titleText = new BitmapText(guiFont);
        titleText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        titleText.setText("SETTINGS");
        titleText.setColor(ColorRGBA.Cyan);
        settingsRoot.attachChild(titleText);

        // Label and color are set in refreshInvertYButton()
        invertYButton = new MenuButton(guiFont, "", 1.3f, ColorRGBA.White, this::toggleInvertY);
        backButton = new MenuButton(guiFont, "Back [Esc]", 1.3f,
                ColorRGBA.Yellow, () -> pendingBack = true);

        settingsRoot.attachChild(invertYButton.getText());
        settingsRoot.attachChild(backButton.getText());

        mouse = new MenuMouse(inputManager, MAP_CLICK, List.of(invertYButton, backButton));
    }

    private void refreshInvertYButton() {
        boolean on = settings.isInvertY();
        invertYButton.setLabel("Invert Y: " + (on ? "ON" : "OFF") + "  [I]");
        invertYButton.setColor(on ? ColorRGBA.Green : ColorRGBA.Red);
    }

    private void updatePositions() {
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();
        lastWidth = w;
        lastHeight = h;

        // Overlay covers the whole screen, placed behind the texts (z = -1)
        ((Quad) dimmer.getMesh()).updateGeometry(w, h);
        dimmer.setLocalTranslation(0, 0, -1);

        titleText.setLocalTranslation((w - titleText.getLineWidth()) / 2f, h / 2f + 80, 0);
        invertYButton.centerHorizontally(w, h / 2f);
        backButton.centerHorizontally(w, h / 2f - 60);
    }

    private void initKeys() {
        inputManager.addMapping(MAP_TOGGLE_INVERT_Y, new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping(MAP_BACK, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(actionListener, MAP_TOGGLE_INVERT_Y, MAP_BACK);

        mouse.register();
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isEnabled() || !isPressed) return;

        switch (name) {
            case MAP_TOGGLE_INVERT_Y -> toggleInvertY();
            case MAP_BACK -> pendingBack = true;
            default -> { }
        }
    };

    private void toggleInvertY() {
        settings.setInvertY(!settings.isInvertY());
        logger.info("Invert Y: " + settings.isInvertY());

        refreshInvertYButton();
        // The text width changed, so it has to be centered again
        updatePositions();
    }

    @Override
    public void update(float tpf) {
        // Keep the screen centered after resize / fullscreen toggle
        if (app.getCamera().getWidth() != lastWidth
                || app.getCamera().getHeight() != lastHeight) {
            updatePositions();
        }
        mouse.updateHover();

        if (pendingBack) {
            pendingBack = false;
            // cleanup() enables the parent menu again
            getStateManager().detach(this);
        }
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_TOGGLE_INVERT_Y);
        inputManager.deleteMapping(MAP_BACK);
        inputManager.removeListener(actionListener);
        mouse.unregister();

        settingsRoot.removeFromParent();
        // Give control back to the menu that opened the settings
        parent.setEnabled(true);
    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(settingsRoot);
        mouse.setActive(true);
        inputManager.setCursorVisible(true);
        updatePositions();
    }

    @Override
    protected void onDisable() {
        settingsRoot.removeFromParent();
        mouse.setActive(false);
    }
}