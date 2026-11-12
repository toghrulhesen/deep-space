package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Walking player: WASD to move, mouse to look, SPACE to jump, LEFT SHIFT to sprint.
 * The player is a physics capsule (BetterCharacterControl) affected by gravity;
 * the camera follows the capsule at eye height.
 */
public class PlayerControlState extends BaseAppState {

    private static final String MAP_FORWARD = "PlayerForward";
    private static final String MAP_BACKWARD = "PlayerBackward";
    private static final String MAP_LEFT = "PlayerLeft";
    private static final String MAP_RIGHT = "PlayerRight";
    private static final String MAP_JUMP = "PlayerJump";
    private static final String MAP_SPRINT = "PlayerSprint";

    private static final String MAP_LOOK_LEFT = "PlayerLookLeft";
    private static final String MAP_LOOK_RIGHT = "PlayerLookRight";
    private static final String MAP_LOOK_UP = "PlayerLookUp";
    private static final String MAP_LOOK_DOWN = "PlayerLookDown";

    private static final float WALK_SPEED = 5f;
    private static final float SPRINT_MULTIPLIER = 1.8f;
    private static final float LOOK_SENSITIVITY = 1f;
    private static final float MAX_PITCH = FastMath.HALF_PI - 0.01f;

    // Capsule dimensions (meters / kg)
    private static final float PLAYER_RADIUS = 0.4f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_MASS = 80f;
    private static final float EYE_HEIGHT = 1.6f;

    // Feet position at spawn (slightly above the floor, the player drops onto it)
    private static final Vector3f START_FEET = new Vector3f(0f, -1.7f, 8f);

    // If the player falls below this height (off the planet edge), respawn
    private static final float RESPAWN_Y = -50f;

    private SimpleApplication app;
    private InputManager inputManager;
    private Camera cam;

    private Node playerNode;
    private BetterCharacterControl character;
    private PhysicsSpace space;

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean sprint;
    private boolean jumpRequested;

    private float yaw;
    private float pitch;

    // Reused vectors to avoid per-frame allocations
    private final Vector3f move = new Vector3f();
    private final Vector3f tmpDir = new Vector3f();
    private final Vector3f tmpLeft = new Vector3f();
    private final Vector3f tmpEye = new Vector3f();

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.inputManager = this.app.getInputManager();
        this.cam = this.app.getCamera();

        // Start looking along -Z towards the origin
        yaw = 0f;
        pitch = 0f;
        cam.setLocation(tmpEye.set(START_FEET).addLocal(0f, EYE_HEIGHT, 0f));
        applyRotation();

        initKeys();
    }

    private void initKeys() {
        inputManager.addMapping(MAP_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(MAP_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(MAP_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(MAP_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(MAP_JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(MAP_SPRINT, new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addListener(actionListener,
                MAP_FORWARD, MAP_BACKWARD, MAP_LEFT, MAP_RIGHT, MAP_JUMP, MAP_SPRINT);

        // Mouse look. Moving the mouse up looks up
        // (in jME a positive pitch tilts the camera down, so the Y signs are flipped).
        inputManager.addMapping(MAP_LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(MAP_LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(MAP_LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(MAP_LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addListener(analogListener,
                MAP_LOOK_LEFT, MAP_LOOK_RIGHT, MAP_LOOK_UP, MAP_LOOK_DOWN);
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isEnabled()) return;

        switch (name) {
            case MAP_FORWARD -> forward = isPressed;
            case MAP_BACKWARD -> backward = isPressed;
            case MAP_LEFT -> left = isPressed;
            case MAP_RIGHT -> right = isPressed;
            case MAP_SPRINT -> sprint = isPressed;
            case MAP_JUMP -> {
                // Jump only on key press, not while held
                if (isPressed) jumpRequested = true;
            }
            default -> { }
        }
    };

    private final AnalogListener analogListener = (name, value, tpf) -> {
        if (!isEnabled()) return;

        float delta = value * LOOK_SENSITIVITY;
        switch (name) {
            case MAP_LOOK_LEFT -> yaw += delta;
            case MAP_LOOK_RIGHT -> yaw -= delta;
            case MAP_LOOK_UP -> pitch += delta;
            case MAP_LOOK_DOWN -> pitch -= delta;
            default -> { }
        }
        pitch = FastMath.clamp(pitch, -MAX_PITCH, MAX_PITCH);
        applyRotation();
    };

    private void applyRotation() {
        cam.setRotation(new Quaternion().fromAngles(pitch, yaw, 0f));
    }

    /**
     * The physics space is created by PhysicsState during its own initialization,
     * which may happen after ours, so the character is created lazily in update().
     */
    private void setupCharacter() {
        PhysicsState physics = getStateManager().getState(PhysicsState.class);
        if (physics == null || physics.getPhysicsSpace() == null) {
            return;
        }
        space = physics.getPhysicsSpace();

        playerNode = new Node("Player");
        playerNode.setLocalTranslation(START_FEET);

        character = new BetterCharacterControl(PLAYER_RADIUS, PLAYER_HEIGHT, PLAYER_MASS);
        character.setGravity(new Vector3f(0f, -PhysicsState.GRAVITY, 0f));
        playerNode.addControl(character);

        app.getRootNode().attachChild(playerNode);
        space.add(character);
        character.warp(START_FEET);
    }

    @Override
    public void update(float tpf) {
        if (character == null) {
            setupCharacter();
            if (character == null) return;
        }

        // Horizontal movement relative to where the camera looks (pitch is ignored)
        cam.getDirection(tmpDir);
        tmpDir.y = 0f;
        tmpDir.normalizeLocal();
        cam.getLeft(tmpLeft);
        tmpLeft.y = 0f;
        tmpLeft.normalizeLocal();

        move.set(0f, 0f, 0f);
        if (forward) move.addLocal(tmpDir);
        if (backward) move.subtractLocal(tmpDir);
        if (left) move.addLocal(tmpLeft);
        if (right) move.subtractLocal(tmpLeft);

        if (move.lengthSquared() > 0f) {
            // Normalize so diagonal movement is not faster
            float speed = WALK_SPEED * (sprint ? SPRINT_MULTIPLIER : 1f);
            move.normalizeLocal().multLocal(speed);
        }
        // Walk direction is a velocity (m/s); a zero vector stops the player
        character.setWalkDirection(move);

        if (jumpRequested) {
            // Does nothing while the player is in the air
            character.jump();
            jumpRequested = false;
        }

        if (playerNode.getWorldTranslation().y < RESPAWN_Y) {
            character.warp(START_FEET);
        }

        // Camera follows the capsule at eye height
        tmpEye.set(playerNode.getWorldTranslation()).addLocal(0f, EYE_HEIGHT, 0f);
        cam.setLocation(tmpEye);
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_FORWARD);
        inputManager.deleteMapping(MAP_BACKWARD);
        inputManager.deleteMapping(MAP_LEFT);
        inputManager.deleteMapping(MAP_RIGHT);
        inputManager.deleteMapping(MAP_JUMP);
        inputManager.deleteMapping(MAP_SPRINT);
        inputManager.deleteMapping(MAP_LOOK_LEFT);
        inputManager.deleteMapping(MAP_LOOK_RIGHT);
        inputManager.deleteMapping(MAP_LOOK_UP);
        inputManager.deleteMapping(MAP_LOOK_DOWN);
        inputManager.removeListener(actionListener);
        inputManager.removeListener(analogListener);

        // PhysicsState is cleaned up after us (see GameplayState.cleanup), so the space is still valid
        if (space != null && character != null) {
            space.remove(character);
        }
        if (playerNode != null) {
            playerNode.removeFromParent();
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() {
        // Clear held keys so the player does not keep moving after a pause
        forward = false;
        backward = false;
        left = false;
        right = false;
        sprint = false;
        jumpRequested = false;
        if (character != null) {
            character.setWalkDirection(Vector3f.ZERO);
        }
    }
}