package com.deepspaceminer;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.deepspaceminer.states.FullscreenState;
import com.deepspaceminer.states.GameSettingsState;
import com.deepspaceminer.states.MainMenuState;

public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Deep Space Miner");
        settings.setResolution(1152, 648);
        settings.setResizable(true);
        settings.setCenterWindow(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Disable the default ESC-to-exit mapping so we can handle it ourselves
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);

        // The built-in fly camera is replaced by PlayerControlState
        flyCam.setEnabled(false);

        // Global state: game settings (Invert Y, ...), used by the settings menu and the player
        stateManager.attach(new GameSettingsState());

        // Global state: Alt + Enter fullscreen toggle
        stateManager.attach(new FullscreenState());

        // Start with the main menu
        stateManager.attach(new MainMenuState());
    }
}