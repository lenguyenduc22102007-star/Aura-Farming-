package view.screen_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;

public final class GameOverViewController {

    @FXML private Label titleLabel;
    @FXML private Label detailLabel;
    @FXML private Button btnNewFarm;
    @FXML private Button btnMainMenu;

    private SceneRouter router;
    private GameLauncher launcher;

    public void init(SceneRouter router, GameLauncher launcher) {
        this.router = router;
        this.launcher = launcher;
    }

    // Populate the screen with the run's result (bankruptcy).
    public void showResult(int day, int money) {
        if (titleLabel != null) {
            titleLabel.setText("GAME OVER");
        }
        if (detailLabel != null) {
            detailLabel.setText("Your farm went bankrupt  —  Day " + day + "  •  $" + money);
        }
    }

    @FXML
    private void onNewFarm() {
        if (launcher != null) {
            try {
                launcher.startNewFarm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onMainMenu() {
        if (launcher != null) {
            try {
                launcher.returnToMainMenu();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
