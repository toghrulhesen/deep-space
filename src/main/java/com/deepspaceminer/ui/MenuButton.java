package com.deepspaceminer.ui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * A clickable text line for menus. It knows its screen area, gets lighter while the
 * mouse is over it and runs an action when it is clicked.
 * The button must be placed under a node without its own transform (e.g. the gui node
 * or a plain root node at the origin), so its local position equals its screen position.
 */
public class MenuButton {

    // How far the text color moves towards white while the mouse is over the button
    private static final float HOVER_BRIGHTNESS = 0.4f;
    // Extra clickable margin around the text, in pixels
    private static final float HIT_PADDING = 8f;

    private final BitmapText text;
    private final Runnable action;

    private ColorRGBA normalColor;
    private ColorRGBA hoverColor;
    private boolean hovered;

    public MenuButton(BitmapFont font, String label, float sizeMultiplier,
                      ColorRGBA color, Runnable action) {
        this.action = action;
        this.text = new BitmapText(font);
        text.setSize(font.getCharSet().getRenderedSize() * sizeMultiplier);
        text.setText(label);
        setColor(color);
    }

    /** The text spatial, to be attached to a node. */
    public BitmapText getText() {
        return text;
    }

    public void setLabel(String label) {
        text.setText(label);
    }

    public void setColor(ColorRGBA color) {
        normalColor = color;
        hoverColor = brighten(color, HOVER_BRIGHTNESS);
        applyColor();
    }

    /** Centers the button horizontally. The y value is the top edge of the text. */
    public void centerHorizontally(float screenWidth, float top) {
        text.setLocalTranslation((screenWidth - text.getLineWidth()) / 2f, top, 0f);
    }

    /** True if the given screen point (origin at the bottom-left) is over the button. */
    public boolean contains(Vector2f point) {
        Vector3f position = text.getLocalTranslation();
        float left = position.x - HIT_PADDING;
        float right = position.x + text.getLineWidth() + HIT_PADDING;
        // BitmapText is drawn downwards from its y position
        float top = position.y + HIT_PADDING;
        float bottom = position.y - text.getHeight() - HIT_PADDING;
        return point.x >= left && point.x <= right && point.y >= bottom && point.y <= top;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        applyColor();
    }

    public void click() {
        action.run();
    }

    private void applyColor() {
        text.setColor(hovered ? hoverColor : normalColor);
    }

    /** Moves a color towards white. The given color is not modified. */
    private static ColorRGBA brighten(ColorRGBA color, float amount) {
        return new ColorRGBA(
                color.r + (1f - color.r) * amount,
                color.g + (1f - color.g) * amount,
                color.b + (1f - color.b) * amount,
                color.a);
    }
}