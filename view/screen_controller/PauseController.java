package view.screen_controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;

public final class PauseController {

    @FXML private Button btnResume;
    @FXML private Button btnMainMenu;
    @FXML private Button btnQuitGame;

    private SceneRouter router;
    private GameLauncher launcher;

    public void init(SceneRouter router, GameLauncher launcher) {
        this.router   = router;
        this.launcher = launcher;
    }

    @FXML
    private void onResume() {
        if (router != null) {
            try {
                router.closeOverlay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onMainMenu() {
        try {
            if (router != null) router.closeOverlay();
            if (launcher != null) launcher.returnToMainMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onQuit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quit Game");
        alert.setHeaderText("Are you sure you want to quit?");
        alert.setContentText("All unsaved progress will be lost.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (launcher != null) {
                launcher.quit();
            } else {
                Platform.exit();
            }
        }
    }
}
