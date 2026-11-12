package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;

/**
 * Owns the Bullet physics world (gravity, simulation step).
 * Disabling this state pauses the whole simulation.
 * Note: getPhysicsSpace() is null until the state is initialized by the state manager.
 */
public class PhysicsState extends BulletAppState {

    /** Set to true to draw collision shapes (useful for debugging). */
    private static final boolean DEBUG_SHAPES = false;

    /** Gravity strength in m/s^2 (applied downwards). */
    public static final float GRAVITY = 9.81f;

    public PhysicsState() {
        setDebugEnabled(DEBUG_SHAPES);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        getPhysicsSpace().setGravity(new Vector3f(0f, -GRAVITY, 0f));
    }
}