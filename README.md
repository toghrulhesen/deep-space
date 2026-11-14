# Deep Space Miner

A 3D space mining game built with Java and jMonkeyEngine. This is a hobby project and a work in progress.

## Current state

Playable prototype with:

- Main menu and in-game pause menu
- Alt + Enter fullscreen toggle (restores window position, size and maximized state)
- First-person walking with gravity, jumping and sprinting
- Physics-based collision with a static planet floor and objects

Planned next: minable asteroids and minerals with a resource counter (HUD), a free-flight (zero-g) mode and a settings menu (including an "Invert Y" option).

## Controls

| Key | Action |
| --- | --- |
| `W` `A` `S` `D` | Move |
| Mouse | Look around |
| `Space` | Jump |
| `Left Shift` | Sprint |
| `Esc` | Pause / resume |
| `M` (in pause menu) | Back to main menu |
| `Enter` (in main menu) | Start game |
| `Q` (in menus) | Quit |
| `Alt` + `Enter` | Toggle fullscreen |
| `E` / Left mouse button | Hold to mine the mineral you are looking at |

## Tech stack

- Java 21
- [jMonkeyEngine](https://jmonkeyengine.org/) 3.6.1-stable (LWJGL 3 backend)
- [Minie](https://github.com/stephengold/Minie) 8.2.0 (Bullet physics with bundled native libraries)
- Gradle

## Running the game

Requirements: JDK 21.

1. Clone the repository.
2. Open the project in IntelliJ IDEA (or any IDE with Gradle support) and let it import the Gradle project.
3. Run the `com.deepspaceminer.Main` class.

The game is developed and tested on Windows.

## Project structure

```
src/main/java/com/deepspaceminer
├── Main.java                      Window settings and application start
└── states
    ├── MainMenuState.java         Title screen
    ├── GameplayState.java         World setup, pause handling, owns child states
    ├── PauseState.java            Pause overlay
    ├── PhysicsState.java          Bullet physics world and gravity
    ├── PlayerControlState.java    Walking player, mouse look, camera
    └── FullscreenState.java       Alt + Enter fullscreen switching
```

The code follows a modular `AppState` architecture: each state is responsible for one concern only.

## Notes

- Fullscreen switching uses GLFW (`glfwSetWindowMonitor`) directly, so the window and OpenGL context are not recreated.
- The physics space is created after the first frame, so physics objects and the player are registered lazily in `update()`.
- Minie extracts its native library (`bulletjme.dll` on Windows) next to the project at runtime. It is excluded via `.gitignore`.
