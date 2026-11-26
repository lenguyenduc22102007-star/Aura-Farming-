package view.screen_util;

public interface GameLauncher {

    // Wipe any current session and build a fresh farm + controllers.
    void startNewFarm();

    // Start a fresh farm with a custom player-chosen farm name.
    default void startNewFarm(String farmName) {
        startNewFarm();
    }

    // Resume the current session, or start a new one if none exists yet.
    void continueFarm();

    // return true once at least one farm has been started.
    boolean hasSession();

    // Persist the current run to disk so it can be continued later.
    default void saveGame() {}

    // Navigate back to the main menu (the session is kept).
    void returnToMainMenu();

    // Quit the application.
    void quit();
}