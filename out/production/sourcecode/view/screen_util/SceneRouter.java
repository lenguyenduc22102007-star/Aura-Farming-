package view.screen_util;

import exception.SmartFarmException;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class SceneRouter {
    private final StackPane root = new StackPane();
    private final Map<String, Parent> screens = new HashMap<>();
    private Node currentOverlay;
    private String currentScreenName;

    public SceneRouter() {
        // Nền tối ấm để hiệu ứng FadeTransition 0.35s giữa MainMenu và InGame mượt mà, không bị chớp trắng
        root.setStyle("-fx-background-color: #120D08;");
    }

    public StackPane getRoot() {
        return root;
    }

    public void register(String name, Parent screen) {
        screens.put(name, screen);
    }

    public void show(String name) {
        Parent screen = screens.get(name);

        if (screen == null) {
            throw new SmartFarmException("Screen " + name + " not found");
        }
        closeOverlay();

        boolean animateFade = currentScreenName != null
                && !name.equals(currentScreenName)
                && ((Screens.MAIN_MENU.equals(currentScreenName) && Screens.INGAME.equals(name))
                    || (Screens.INGAME.equals(currentScreenName) && Screens.MAIN_MENU.equals(name))
                    || Screens.MAIN_MENU.equals(name)
                    || Screens.INGAME.equals(name));

        currentScreenName = name;
        root.getChildren().setAll(screen);

        if (animateFade) {
            screen.setOpacity(0.0);
            FadeTransition ft = new FadeTransition(Duration.seconds(0.35), screen);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } else {
            screen.setOpacity(1.0);
        }
    }

    public Parent getScreen(String name) {
        Parent screen = screens.get(name);
        if (screen == null) {
            throw new SmartFarmException("Screen " + name + " not found");
        }
        return screen;
    }

    public void showOverlay(Node overlay) {
        if (currentOverlay != null || overlay == null) {
            return;
        }
        currentOverlay = overlay;
        root.getChildren().add(currentOverlay);
    }

    public void closeOverlay() {
        if (currentOverlay != null) {
            root.getChildren().remove(currentOverlay);
            currentOverlay = null;
        }
    }

    public boolean hasOverlay() {
        return currentOverlay != null;
    }
}
