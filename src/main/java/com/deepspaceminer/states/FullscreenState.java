package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.system.lwjgl.LwjglWindow;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.logging.Logger;

/**
 * Handles Alt + Enter fullscreen/windowed switching.
 * Uses GLFW directly (glfwSetWindowMonitor) so the window and the
 * OpenGL context are NOT recreated, which avoids size/position drift.
 */
public class FullscreenState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(FullscreenState.class.getName());

    private static final String MAP_ALT_LEFT = "AltLeft";
    private static final String MAP_ALT_RIGHT = "AltRight";
    private static final String MAP_TOGGLE = "ToggleFullscreen";
    private static final String MAP_TOGGLE_NUMPAD = "ToggleFullscreenNumpad";

    private SimpleApplication app;
    private InputManager inputManager;

    // Native GLFW window handle (0 if unavailable)
    private long windowHandle;

    private boolean altLeft;
    private boolean altRight;
    private boolean fullscreen;

    // Remembered windowed geometry, restored when leaving fullscreen
    private int windowedX = 100;
    private int windowedY = 100;
    private int windowedWidth = 1152;
    private int windowedHeight = 648;

    // True if the window was maximized before entering fullscreen
    private boolean wasMaximized;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.inputManager = this.app.getInputManager();

        if (this.app.getContext() instanceof LwjglWindow) {
            windowHandle = ((LwjglWindow) this.app.getContext()).getWindowHandle();
        } else {
            logger.severe("Context is not LwjglWindow, fullscreen toggle disabled.");
        }

        inputManager.addMapping(MAP_ALT_LEFT, new KeyTrigger(KeyInput.KEY_LMENU));
        inputManager.addMapping(MAP_ALT_RIGHT, new KeyTrigger(KeyInput.KEY_RMENU));
        inputManager.addMapping(MAP_TOGGLE, new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping(MAP_TOGGLE_NUMPAD, new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        inputManager.addListener(actionListener,
                MAP_ALT_LEFT, MAP_ALT_RIGHT, MAP_TOGGLE, MAP_TOGGLE_NUMPAD);
    }

    /** Other states can use this to ignore Enter while Alt is held. */
    public boolean isAltDown() {
        return altLeft || altRight;
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        switch (name) {
            case MAP_ALT_LEFT -> altLeft = isPressed;
            case MAP_ALT_RIGHT -> altRight = isPressed;
            case MAP_TOGGLE, MAP_TOGGLE_NUMPAD -> {
                if (isPressed && isAltDown()) {
                    toggleFullscreen();
                }
            }
            default -> { }
        }
    };

    private void toggleFullscreen() {
        if (windowHandle == 0L) {
            return;
        }

        if (!fullscreen) {
            enterFullscreen();
        } else {
            leaveFullscreen();
        }
    }

    private void enterFullscreen() {
        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (monitor == MemoryUtil.NULL || mode == null) {
            logger.warning("No primary monitor / video mode, cannot enter fullscreen.");
            return;
        }

        // A maximized window reports the maximized size, not the "normal" one.
        // Restore it first so we save the real windowed geometry.
        wasMaximized = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
        if (wasMaximized) {
            GLFW.glfwRestoreWindow(windowHandle);
        }

        // Remember current windowed geometry before leaving it
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(windowHandle, x, y);
            GLFW.glfwGetWindowSize(windowHandle, w, h);
            windowedX = x.get(0);
            windowedY = y.get(0);
            windowedWidth = w.get(0);
            windowedHeight = h.get(0);
        }

        GLFW.glfwSetWindowMonitor(windowHandle, monitor, 0, 0,
                mode.width(), mode.height(), mode.refreshRate());
        fullscreen = true;
        logger.info("Fullscreen " + mode.width() + "x" + mode.height()
                + " @" + mode.refreshRate() + "Hz (was maximized: " + wasMaximized + ")");
    }

    private void leaveFullscreen() {
        // Keep the title bar reachable even if the saved position was odd
        int x = Math.max(windowedX, 0);
        int y = Math.max(windowedY, 32);

        GLFW.glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, x, y,
                windowedWidth, windowedHeight, GLFW.GLFW_DONT_CARE);
        fullscreen = false;

        // Put the window back into the maximized state if it was maximized before
        if (wasMaximized) {
            GLFW.glfwMaximizeWindow(windowHandle);
        }
        logger.info("Windowed " + windowedWidth + "x" + windowedHeight + " at " + x + "," + y
                + " (maximized: " + wasMaximized + ")");
    }

    @Override
    protected void cleanup(Application app) {
        inputManager.deleteMapping(MAP_ALT_LEFT);
        inputManager.deleteMapping(MAP_ALT_RIGHT);
        inputManager.deleteMapping(MAP_TOGGLE);
        inputManager.deleteMapping(MAP_TOGGLE_NUMPAD);
        inputManager.removeListener(actionListener);
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}