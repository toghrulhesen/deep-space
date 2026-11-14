package com.deepspaceminer.states;

import com.deepspaceminer.model.Inventory;
import com.deepspaceminer.model.MineralType;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Scatters minerals over the planet surface, finds the one the player is looking at
 * and lets the player mine it: hold E or the left mouse button while aiming at a mineral
 * until the progress bar is full. Mined resources go to the inventory.
 */
public class MiningState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(MiningState.class.getName());

    private static final String MAP_MINE = "MiningMine";

    // Spawning. A fixed seed gives the same layout on every run (easier to test).
    private static final int MINERAL_COUNT = 25;
    private static final long SPAWN_SEED = 12345L;
    private static final int MAX_SPAWN_ATTEMPTS = 500;
    private static final float EDGE_MARGIN = 3f;
    private static final float MIN_RADIUS = 0.4f;
    private static final float MAX_RADIUS = 1.0f;
    // Minerals are partially buried: center height = ground + radius * SINK_FACTOR
    private static final float SINK_FACTOR = 0.6f;
    private static final int MESH_SAMPLES = 8;

    // Areas (x, z) that must stay free: the cube at the origin and the player spawn
    private static final float[][] KEEP_CLEAR_ZONES = {{0f, 0f}, {0f, 8f}};
    private static final float KEEP_CLEAR_RADIUS = 5f;

    // Maximum distance (meters) from which a mineral can be mined
    private static final float MINING_RANGE = 5f;

    // Mining time grows with the size of the mineral (radius 0.4 -> 0.8 s, radius 1.0 -> 2 s)
    private static final float MINING_SECONDS_PER_RADIUS = 2f;

    /** A mineral that is currently in the world. */
    private static final class Mineral {
        private final MineralType type;
        private final float radius;

        private Mineral(MineralType type, float radius) {
            this.type = type;
            this.radius = radius;
        }

        private float getMiningSeconds() {
            return radius * MINING_SECONDS_PER_RADIUS;
        }
    }

    private final Node sceneRoot;
    private final Inventory inventory;
    private final float groundY;
    private final float areaHalfSize;

    private AssetManager assetManager;
    private InputManager inputManager;
    private Camera cam;

    private Node mineralRoot;
    private PhysicsSpace space;

    private final Map<Geometry, Mineral> minerals = new HashMap<>();
    // Static bodies waiting to be added to the physics space
    private final List<RigidBodyControl> pendingBodies = new ArrayList<>();

    private final Ray ray = new Ray();
    private final CollisionResults results = new CollisionResults();

    // The mineral the player is aiming at (within range), or null
    private Geometry targetGeometry;
    // The exact point on that mineral where the aiming ray hits it
    private final Vector3f targetPoint = new Vector3f();

    // Hold-to-mine state: which mineral is being mined and how far along (0..1)
    private boolean mineHeld;
    private Geometry miningGeometry;
    private float miningProgress;

    /**
     * @param sceneRoot    gameplay scene root (it holds the lights, so minerals must live under it)
     * @param inventory    where mined resources are stored
     * @param groundY      height of the planet surface
     * @param areaHalfSize half size of the square area where minerals may spawn
     */
    public MiningState(Node sceneRoot, Inventory inventory, float groundY, float areaHalfSize) {
        this.sceneRoot = sceneRoot;
        this.inventory = inventory;
        this.groundY = groundY;
        this.areaHalfSize = areaHalfSize;
    }

    @Override
    protected void initialize(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;
        this.assetManager = simpleApp.getAssetManager();
        this.inputManager = simpleApp.getInputManager();
        this.cam = simpleApp.getCamera();

        mineralRoot = new Node("Minerals");
        spawnMinerals();
        sceneRoot.attachChild(mineralRoot);

        inputManager.addMapping(MAP_MINE,
                new KeyTrigger(KeyInput.KEY_E),
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, MAP_MINE);
    }

    private void spawnMinerals() {
        Random random = new Random(SPAWN_SEED);
        float range = areaHalfSize - EDGE_MARGIN;

        int spawned = 0;
        int attempts = 0;
        while (spawned < MINERAL_COUNT && attempts < MAX_SPAWN_ATTEMPTS) {
            attempts++;

            float x = (random.nextFloat() * 2f - 1f) * range;
            float z = (random.nextFloat() * 2f - 1f) * range;
            if (isInKeepClearZone(x, z)) {
                continue;
            }

            MineralType type = MineralType.pickRandom(random);
            float radius = MIN_RADIUS + random.nextFloat() * (MAX_RADIUS - MIN_RADIUS);
            createMineral(type, radius, x, z);
            spawned++;
        }
        logger.info("Spawned " + spawned + " minerals");
    }

    private boolean isInKeepClearZone(float x, float z) {
        for (float[] zone : KEEP_CLEAR_ZONES) {
            float dx = x - zone[0];
            float dz = z - zone[1];
            if (dx * dx + dz * dz < KEEP_CLEAR_RADIUS * KEEP_CLEAR_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void createMineral(MineralType type, float radius, float x, float z) {
        Geometry geom = new Geometry("Mineral_" + type.name(),
                new Sphere(MESH_SAMPLES, MESH_SAMPLES, radius));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", type.getColor());
        mat.setColor("Ambient", type.getColor());
        geom.setMaterial(mat);

        Vector3f position = new Vector3f(x, groundY + radius * SINK_FACTOR, z);
        geom.setLocalTranslation(position);
        mineralRoot.attachChild(geom);

        // Static body (mass 0) so the player cannot walk through the mineral
        RigidBodyControl body = new RigidBodyControl(new SphereCollisionShape(radius), 0f);
        geom.addControl(body);
        body.setPhysicsLocation(position);
        pendingBodies.add(body);

        minerals.put(geom, new Mineral(type, radius));
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isEnabled()) return;

        if (name.equals(MAP_MINE)) {
            // Only remember the button state; the actual work happens in update()
            mineHeld = isPressed;
        }
    };

    @Override
    public void update(float tpf) {
        registerBodies();
        updateTarget();
        updateMining(tpf);
    }

    /** The physics space is created after the first frame, so bodies are registered lazily. */
    private void registerBodies() {
        if (pendingBodies.isEmpty()) return;

        PhysicsState physics = getStateManager().getState(PhysicsState.class);
        if (physics == null || physics.getPhysicsSpace() == null) return;

        space = physics.getPhysicsSpace();
        for (RigidBodyControl body : pendingBodies) {
            space.add(body);
        }
        pendingBodies.clear();
    }

    /**
     * Casts a ray from the camera against the whole scene, so a mineral hidden behind
     * another object cannot be targeted. Only the closest hit counts.
     */
    private void updateTarget() {
        targetGeometry = null;

        ray.setOrigin(cam.getLocation());
        ray.setDirection(cam.getDirection());
        results.clear();
        sceneRoot.collideWith(ray, results);

        CollisionResult closest = results.getClosestCollision();
        if (closest == null || closest.getDistance() > MINING_RANGE) return;

        Geometry hit = closest.getGeometry();
        if (minerals.containsKey(hit)) {
            targetGeometry = hit;
            targetPoint.set(closest.getContactPoint());
        }
    }

    /**
     * Fills the progress while the player holds the button and keeps aiming at the same mineral.
     * Releasing the button or looking away (or at another mineral) resets the progress.
     */
    private void updateMining(float tpf) {
        if (!mineHeld || targetGeometry == null) {
            resetMining();
            return;
        }

        if (targetGeometry != miningGeometry) {
            // New mineral: start from zero
            miningGeometry = targetGeometry;
            miningProgress = 0f;
        }

        Mineral mineral = minerals.get(miningGeometry);
        miningProgress += tpf / mineral.getMiningSeconds();
        if (miningProgress >= 1f) {
            mine(miningGeometry);
        }
    }

    private void resetMining() {
        miningGeometry = null;
        miningProgress = 0f;
    }

    private void mine(Geometry geom) {
        Mineral mineral = minerals.remove(geom);
        RigidBodyControl body = geom.getControl(RigidBodyControl.class);
        // If the body was never added to the space it is simply dropped from the pending list
        if (!pendingBodies.remove(body) && space != null) {
            space.remove(body);
        }
        geom.removeFromParent();

        resetMining();
        targetGeometry = null;

        MineralType type = mineral.type;
        inventory.add(type, type.getYield());
        logger.info("Mined " + type.getDisplayName() + " (+" + type.getYield() + ")");
    }

    /** The mineral type the player is looking at within mining range, or null. */
    public MineralType getTargetType() {
        if (targetGeometry == null) return null;
        Mineral mineral = minerals.get(targetGeometry);
        return mineral == null ? null : mineral.type;
    }

    /** Mining progress from 0 to 1; 0 when nothing is being mined. */
    public float getMiningProgress() {
        return miningGeometry == null ? 0f : FastMath.clamp(miningProgress, 0f, 1f);
    }

    /** True while a mineral is actually being mined (button held and aiming at it). */
    public boolean isMining() {
        return getMiningProgress() > 0f;
    }

    /**
     * The point on the aimed mineral where the aiming ray hits it (read only).
     * Only meaningful while getTargetType() is not null.
     */
    public Vector3f getTargetPoint() {
        return targetPoint;
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_MINE);
        inputManager.removeListener(actionListener);

        // PhysicsState is cleaned up after us (see GameplayState.cleanup), so the space is still valid
        if (space != null) {
            for (Geometry geom : minerals.keySet()) {
                space.remove(geom.getControl(RigidBodyControl.class));
            }
        }
        minerals.clear();
        pendingBodies.clear();
        mineralRoot.removeFromParent();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() {
        // Forget held input and progress so mining does not continue (or finish) after a pause
        mineHeld = false;
        targetGeometry = null;
        resetMining();
    }
}