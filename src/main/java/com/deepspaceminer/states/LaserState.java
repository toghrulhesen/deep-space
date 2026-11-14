package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

/**
 * Mining tool and laser. A placeholder tool box is shown at the lower right of the screen
 * (it follows the camera) and, while the player mines, a laser beam goes from its tip
 * to the point of the mineral the player is aiming at.
 * When a real tool model exists, only createTool() and the muzzle offset need to change.
 */
public class LaserState extends BaseAppState {

    // Tool position relative to the camera, in meters
    private static final float TOOL_RIGHT = 0.3f;
    private static final float TOOL_DOWN = 0.18f;
    private static final float TOOL_FORWARD = 0.8f;
    private static final float TOOL_HALF_WIDTH = 0.05f;
    private static final float TOOL_HALF_LENGTH = 0.25f;

    // Laser look: the beam width pulses slightly while mining
    private static final float BEAM_WIDTH = 0.04f;
    private static final float PULSE_AMPLITUDE = 0.35f;
    private static final float PULSE_SPEED = 40f;
    private static final float SPARK_RADIUS = 0.08f;
    private static final float MIN_BEAM_LENGTH = 0.01f;

    private static final ColorRGBA TOOL_COLOR = ColorRGBA.Gray;
    private static final ColorRGBA BEAM_COLOR = new ColorRGBA(1f, 0.25f, 0.2f, 1f);
    private static final ColorRGBA SPARK_COLOR = new ColorRGBA(1f, 0.9f, 0.5f, 1f);

    // The default near plane (1 m) would cut the tool off, so it is moved closer
    private static final float TOOL_NEAR_PLANE = 0.1f;

    private final MiningState mining;

    private SimpleApplication app;
    private Camera cam;
    private float originalNear;

    private Node laserRoot;
    private Geometry tool;
    private Geometry beam;
    private Geometry spark;

    private float pulseTime;

    // Reused objects to avoid per-frame allocations
    private final Vector3f forward = new Vector3f();
    private final Vector3f left = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Vector3f toolPosition = new Vector3f();
    private final Vector3f muzzle = new Vector3f();
    private final Vector3f beamDirection = new Vector3f();
    private final Vector3f beamCenter = new Vector3f();
    private final Quaternion beamRotation = new Quaternion();

    public LaserState(MiningState mining) {
        this.mining = mining;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.cam = this.app.getCamera();

        originalNear = cam.getFrustumNear();
        setNearPlane(TOOL_NEAR_PLANE);

        laserRoot = new Node("LaserRoot");
        tool = createGeometry("MiningTool",
                new Box(TOOL_HALF_WIDTH, TOOL_HALF_WIDTH, TOOL_HALF_LENGTH), TOOL_COLOR);
        // A unit cube; its length and width are set through the scale every frame
        beam = createGeometry("MiningBeam", new Box(0.5f, 0.5f, 0.5f), BEAM_COLOR);
        spark = createGeometry("MiningSpark", new Sphere(8, 8, SPARK_RADIUS), SPARK_COLOR);

        laserRoot.attachChild(tool);
        laserRoot.attachChild(beam);
        laserRoot.attachChild(spark);
        setBeamVisible(false);
    }

    private Geometry createGeometry(String name, Mesh mesh, ColorRGBA color) {
        Geometry geom = new Geometry(name, mesh);
        // Unshaded: the tool and the beam do not depend on the scene lights
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        return geom;
    }

    /**
     * Moves the near plane while keeping the field of view: the frustum planes are defined
     * at the near plane distance, so they are scaled together with it.
     */
    private void setNearPlane(float newNear) {
        float scale = newNear / cam.getFrustumNear();
        cam.setFrustum(newNear, cam.getFrustumFar(),
                cam.getFrustumLeft() * scale, cam.getFrustumRight() * scale,
                cam.getFrustumTop() * scale, cam.getFrustumBottom() * scale);
    }

    @Override
    public void update(float tpf) {
        pulseTime += tpf;

        cam.getDirection(forward);
        cam.getLeft(left);
        cam.getUp(up);

        // Tool: lower right of the view, pointing where the camera looks
        toolPosition.set(cam.getLocation());
        addScaled(toolPosition, forward, TOOL_FORWARD);
        addScaled(toolPosition, left, -TOOL_RIGHT);
        addScaled(toolPosition, up, -TOOL_DOWN);
        tool.setLocalTranslation(toolPosition);
        tool.setLocalRotation(cam.getRotation());

        // The laser starts at the tip of the tool
        muzzle.set(toolPosition);
        addScaled(muzzle, forward, TOOL_HALF_LENGTH);

        if (mining.isMining() && mining.getTargetType() != null) {
            updateBeam(mining.getTargetPoint());
        } else {
            setBeamVisible(false);
        }
    }

    private void updateBeam(Vector3f target) {
        beamDirection.set(target).subtractLocal(muzzle);
        float length = beamDirection.length();
        if (length < MIN_BEAM_LENGTH) {
            setBeamVisible(false);
            return;
        }
        beamDirection.divideLocal(length);

        // The beam is a unit cube stretched along its local Z axis and turned towards the target
        beamRotation.lookAt(beamDirection, up);
        beam.setLocalRotation(beamRotation);

        float pulse = 1f + PULSE_AMPLITUDE * FastMath.sin(pulseTime * PULSE_SPEED);
        beam.setLocalScale(BEAM_WIDTH * pulse, BEAM_WIDTH * pulse, length);
        beamCenter.set(muzzle).addLocal(target).multLocal(0.5f);
        beam.setLocalTranslation(beamCenter);

        // Glowing spot where the laser hits the mineral
        spark.setLocalTranslation(target);
        spark.setLocalScale(pulse);

        setBeamVisible(true);
    }

    private void setBeamVisible(boolean visible) {
        Spatial.CullHint hint = visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always;
        beam.setCullHint(hint);
        spark.setCullHint(hint);
    }

    /** target += direction * amount (no allocation). */
    private static void addScaled(Vector3f target, Vector3f direction, float amount) {
        target.addLocal(direction.x * amount, direction.y * amount, direction.z * amount);
    }

    @Override
    protected void cleanup(Application app) {
        // Give the camera its original near plane back
        setNearPlane(originalNear);
        laserRoot.removeFromParent();
    }

    @Override
    protected void onEnable() {
        app.getRootNode().attachChild(laserRoot);
    }

    @Override
    protected void onDisable() {
        laserRoot.removeFromParent();
    }
}