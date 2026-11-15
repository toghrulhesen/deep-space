package com.deepspaceminer.states;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Global game settings (lives for the whole app lifetime, like FullscreenState).
 * Other states read the values from here; the settings menu changes them.
 * Values are stored with java.util.prefs, so they survive restarts
 * (on Windows they are kept in the registry under HKCU\Software\JavaSoft\Prefs).
 */
public class GameSettingsState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(GameSettingsState.class.getName());

    private static final String PREFS_NODE = "deepspaceminer";
    private static final String KEY_INVERT_Y = "invertY";

    private Preferences prefs;
    private boolean invertY;

    @Override
    protected void initialize(Application app) {
        prefs = Preferences.userRoot().node(PREFS_NODE);
        invertY = prefs.getBoolean(KEY_INVERT_Y, false);
    }

    /** True if moving the mouse up should look down (and vice versa). */
    public boolean isInvertY() {
        return invertY;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
        prefs.putBoolean(KEY_INVERT_Y, invertY);
        save();
    }

    private void save() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "Could not save settings", e);
        }
    }

    @Override
    protected void cleanup(Application app) { }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}