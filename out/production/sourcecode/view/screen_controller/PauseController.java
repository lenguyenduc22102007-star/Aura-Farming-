package view.screen_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import view.fx.SoundHook;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;

public final class PauseController {

    @FXML private StackPane pauseRoot;
    @FXML private Label saveFeedbackLabel;
    @FXML private Button btnResume;
    @FXML private Button btnSaveGame;
    @FXML private Button btnSettings;
    @FXML private Button btnMainMenu;

    private SceneRouter router;
    private GameLauncher launcher;
    private Runnable onOpenSettingsCallback;
    private Runnable onResumeFocusCallback;

    @FXML
    private void initialize() {
        if (pauseRoot != null) {
            pauseRoot.setFocusTraversable(true);
            pauseRoot.setOnMouseClicked(ev -> {
                if (ev.getTarget() == pauseRoot) {
                    onResume();
                }
            });
            pauseRoot.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    ev.consume();
                    onResume();
                }
            });
        }
    }

    public void init(SceneRouter router, GameLauncher launcher) {
        this.router   = router;
        this.launcher = launcher;
    }

    public void setSessionCallbacks(Runnable onOpenSettingsCallback, Runnable onResumeFocusCallback) {
        this.onOpenSettingsCallback = onOpenSettingsCallback;
        this.onResumeFocusCallback = onResumeFocusCallback;
    }

    public void resetFeedback() {
        if (saveFeedbackLabel != null) {
            saveFeedbackLabel.setText("Nhấn ESC để tiếp tục nhanh");
            saveFeedbackLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F5DEB3;");
        }
    }

    @FXML
    private void onResume() {
        SoundHook.playMenuClick();
        if (router != null) {
            router.closeOverlay();
        }
        if (onResumeFocusCallback != null) {
            onResumeFocusCallback.run();
        }
    }

    @FXML
    private void onSaveGame() {
        if (launcher != null) {
            launcher.saveGame();
        }
        SoundHook.playPickup();
        if (saveFeedbackLabel != null) {
            saveFeedbackLabel.setText("✓ Đã lưu tiến trình nông trại thành công!");
            saveFeedbackLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #A8FF9E;");
        }
    }

    @FXML
    private void onSettings() {
        SoundHook.playMenuClick();
        if (router != null) {
            router.closeOverlay();
        }
        if (onOpenSettingsCallback != null) {
            onOpenSettingsCallback.run();
        }
    }

    @FXML
    private void onMainMenu() {
        SoundHook.playMenuClick();
        try {
            if (router != null) router.closeOverlay();
            if (launcher != null) launcher.returnToMainMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
