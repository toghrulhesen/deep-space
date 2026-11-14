package com.deepspaceminer.states;

import com.deepspaceminer.model.Inventory;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import java.util.ArrayList;
import java.util.List;

public class GameplayState extends BaseAppState {

    private static final String MAP_PAUSE = "GamePause";

    // Planet surface: a wide, thin box. Its top surface is at y = -1.9
    private static final float FLOOR_HALF_SIZE = 50f;
    private static final float FLOOR_HALF_THICKNESS = 0.1f;
    private static final float FLOOR_CENTER_Y = -2f;
    private static final float FLOOR_TOP_Y = FLOOR_CENTER_Y + FLOOR_HALF_THICKNESS;

    private SimpleApplication app;
    private Node rootNode;
    private Node localRootNode; // Keeps all objects of this scene in one place

    // Data shared between child states
    private Inventory inventory;

    // Child states: physics world, player control, mining and HUD
    private PhysicsState physics;
    private PlayerControlState player;
    private MiningState mining;
    private HudState hud;

    // Static bodies waiting to be added to the physics space
    private final List<RigidBodyControl> pendingBodies = new ArrayList<>();

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        this.localRootNode = new Node("GameplayRoot");

        setupWorld();

        // The scene stays attached while paused, so the world remains visible behind the pause menu
        rootNode.attachChild(localRootNode);

        inventory = new Inventory();

        // Physics must be attached before the player and minerals (they need its space)
        physics = new PhysicsState();
        getStateManager().attach(physics);
        player = new PlayerControlState();
        getStateManager().attach(player);
        mining = new MiningState(localRootNode, inventory, FLOOR_TOP_Y, FLOOR_HALF_SIZE);
        getStateManager().attach(mining);
        hud = new HudState(inventory, mining);
        getStateManager().attach(hud);

        app.getInputManager().addMapping(MAP_PAUSE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, MAP_PAUSE);
    }

    private void setupWorld() {
        // 1. Lighting
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.75f, -0.3f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        localRootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.Gray);
        localRootNode.addLight(ambient);

        // 2. Ground (planet surface)
        createStaticBox("PlanetFloor",
                new Vector3f(FLOOR_HALF_SIZE, FLOOR_HALF_THICKNESS, FLOOR_HALF_SIZE),
                new Vector3f(0f, FLOOR_CENTER_Y, 0f),
                ColorRGBA.DarkGray);

        // 3. 3D box (landmark), resting on the ground
        createStaticBox("MyCube",
                new Vector3f(1f, 1f, 1f),
                new Vector3f(0f, -0.9f, 0f),
                ColorRGBA.Cyan);
    }

    /** Creates a lit box with a static (mass 0) physics body. */
    private void createStaticBox(String name, Vector3f halfExtents, Vector3f position, ColorRGBA color) {
        Geometry geom = new Geometry(name, new Box(halfExtents.x, halfExtents.y, halfExtents.z));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        mat.setColor("Ambient", color);
        geom.setMaterial(mat);
        geom.setLocalTranslation(position);
        localRootNode.attachChild(geom);

        // The shape is built from the extents (not from the spatial's bounds), because
        // world bounds are not computed until the first scene update.
        RigidBodyControl body = new RigidBodyControl(new BoxCollisionShape(halfExtents), 0f);
        geom.addControl(body);
        body.setPhysicsLocation(position);
        pendingBodies.add(body);
    }

    @Override
    public void update(float tpf) {
        // PhysicsState creates its space during its own initialization, so register bodies once it exists
        if (pendingBodies.isEmpty()) return;

        PhysicsSpace space = physics.getPhysicsSpace();
        if (space == null) return;

        for (RigidBodyControl body : pendingBodies) {
            space.add(body);
        }
        pendingBodies.clear();
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        // While paused this state is disabled, so ESC is ignored here (PauseState handles it)
        if (!isEnabled() || !isPressed) return;

        if (name.equals(MAP_PAUSE)) {
            setEnabled(false);
            getStateManager().attach(new PauseState());
        }
    };

    @Override
    protected void cleanup(Application app) {
        app.getInputManager().deleteMapping(MAP_PAUSE);
        app.getInputManager().removeListener(actionListener);

        // Order matters: players and minerals remove their bodies from the space before the space is destroyed
        getStateManager().detach(hud);
        getStateManager().detach(mining);
        getStateManager().detach(player);
        getStateManager().detach(physics);
        localRootNode.removeFromParent();
    }

    @Override
    protected void onEnable() {
        // Gameplay is active: physics runs, player controls the camera, HUD is shown, cursor is hidden
        if (physics != null) {
            physics.setEnabled(true);
        }
        if (player != null) {
            player.setEnabled(true);
        }
        if (mining != null) {
            mining.setEnabled(true);
        }
        if (hud != null) {
            hud.setEnabled(true);
        }
        app.getInputManager().setCursorVisible(false);
    }

    @Override
    protected void onDisable() {
        // Gameplay is paused or closed: freeze everything, hide the HUD, show the cursor
        if (physics != null) {
            physics.setEnabled(false);
        }
        if (player != null) {
            player.setEnabled(false);
        }
        if (mining != null) {
            mining.setEnabled(false);
        }
        if (hud != null) {
            hud.setEnabled(false);
        }
        app.getInputManager().setCursorVisible(true);
    }
}