package com.deepspaceminer.ui;

import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector2f;

import java.util.List;

/**
 * Mouse support for one menu: highlights the button under the cursor and clicks it
 * on a left mouse button press. Every menu uses its own mapping name.
 */
public class MenuMouse {

    private final InputManager inputManager;
    private final String mapping;
    private final List<MenuButton> buttons;

    private boolean active = true;

    private final ActionListener listener = (name, isPressed, tpf) -> {
        if (!active || !isPressed) return;

        MenuButton button = findButtonUnderCursor();
        if (button != null) {
            button.click();
        }
    };

    public MenuMouse(InputManager inputManager, String mapping, List<MenuButton> buttons) {
        this.inputManager = inputManager;
        this.mapping = mapping;
        this.buttons = buttons;
    }

    public void register() {
        inputManager.addMapping(mapping, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(listener, mapping);
    }

    public void unregister() {
        inputManager.deleteMapping(mapping);
        inputManager.removeListener(listener);
    }

    /** An inactive menu (hidden or covered by another screen) ignores the mouse. */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            clearHover();
        }
    }

    /** Call every frame while the menu is visible. */
    public void updateHover() {
        if (!active) return;

        Vector2f cursor = inputManager.getCursorPosition();
        for (MenuButton button : buttons) {
            button.setHovered(button.contains(cursor));
        }
    }

    private MenuButton findButtonUnderCursor() {
        Vector2f cursor = inputManager.getCursorPosition();
        for (MenuButton button : buttons) {
            if (button.contains(cursor)) {
                return button;
            }
        }
        return null;
    }

    private void clearHover() {
        for (MenuButton button : buttons) {
            button.setHovered(false);
        }
    }
}