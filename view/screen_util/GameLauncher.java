package view.screen_util;

public interface GameLauncher {

    // Wipe any current session and build a fresh farm + controllers.
    void startNewFarm();

    // Resume the current session, or start a new one if none exists yet.
    void continueFarm();

    // return true once at least one farm has been started.
    boolean hasSession();

    // Navigate back to the main menu (the session is kept).
    void returnToMainMenu();

    // Quit the application.
    void quit();
}