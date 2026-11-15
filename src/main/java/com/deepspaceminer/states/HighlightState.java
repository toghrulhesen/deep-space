package com.deepspaceminer.states;

import com.deepspaceminer.model.MineralType;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;

/**
 * Highlights the mineral the player is aiming at by making its color lighter.
 * The original color is restored when the player looks away, the mineral is mined,
 * or the game is paused.
 */
public class HighlightState extends BaseAppState {

    // 0 = original color, 1 = white
    private static final float HIGHLIGHT_AMOUNT = 0.45f;

    private final MiningState mining;

    // The mineral that is currently highlighted (and its type, needed to restore the color)
    private Geometry highlighted;
    private MineralType highlightedType;

    public HighlightState(MiningState mining) {
        this.mining = mining;
    }

    @Override
    protected void initialize(Application app) { }

    @Override
    public void update(float tpf) {
        Geometry target = mining.getTargetGeometry();
        if (target == highlighted) return;

        clearHighlight();
        if (target != null) {
            applyHighlight(target, mining.getTargetType());
        }
    }

    private void applyHighlight(Geometry geom, MineralType type) {
        if (type == null) return;

        // A new color object is needed: the material keeps a reference to the one it is given
        ColorRGBA lighter = brighten(type.getColor(), HIGHLIGHT_AMOUNT);
        Material mat = geom.getMaterial();
        mat.setColor("Diffuse", lighter);
        mat.setColor("Ambient", lighter);

        highlighted = geom;
        highlightedType = type;
    }

    private void clearHighlight() {
        if (highlighted == null) return;

        // Each mineral has its own material, so restoring it also works for a mineral
        // that was already removed from the scene (mined)
        Material mat = highlighted.getMaterial();
        mat.setColor("Diffuse", highlightedType.getColor());
        mat.setColor("Ambient", highlightedType.getColor());

        highlighted = null;
        highlightedType = null;
    }

    /** Moves a color towards white. The given color is not modified. */
    private static ColorRGBA brighten(ColorRGBA color, float amount) {
        return new ColorRGBA(
                color.r + (1f - color.r) * amount,
                color.g + (1f - color.g) * amount,
                color.b + (1f - color.b) * amount,
                color.a);
    }

    @Override
    protected void cleanup(Application app) {
        clearHighlight();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() {
        // Paused: show the minerals with their normal colors
        clearHighlight();
    }
}