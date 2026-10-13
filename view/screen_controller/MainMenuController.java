package view.screen_controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

public final class MainMenuController {

    @FXML
    private Button btnContinue;

    private SceneRouter router;
    private GameLauncher launcher;

    public void init(SceneRouter router, GameLauncher launcher) {
        this.router = router;
        this.launcher = launcher;
    }

    public void setContinueEnabled(boolean enabled) {
        if(btnContinue != null) btnContinue.setDisable(!enabled);
    }

    @FXML
    private void onContinue() {
        if (launcher != null) {
            try {
                launcher.continueFarm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onStartNew() {
        if (launcher != null) {
            try {
                launcher.startNewFarm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onHelp() {
        if (router != null) {
            try {
                router.show(Screens.HELP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onQuit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quit Game");
        alert.setHeaderText("Are you sure you want to quit?");
        alert.setContentText("All unsaved progress will be lost.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }
}
