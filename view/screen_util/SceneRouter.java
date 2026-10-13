package view.screen_util;

import exception.SmartFarmException;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

public class SceneRouter {
    private final StackPane root = new StackPane();
    private final Map<String, Parent> screens = new HashMap<>();
    private Node currentOverlay;

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
        root.getChildren().setAll(screen);
    }

    public Parent getScreen(String name) {
        Parent screen = screens.get(name);
        if (screen == null) {
            throw new SmartFarmException("Screen " + name + " not found");
        }
        return screen;
    }

    public void showOverlay(Node overlay) {
        if(currentOverlay != null || overlay == null) {
            return;
        }
        currentOverlay = overlay;
        root.getChildren().add(currentOverlay);
    }

    public void closeOverlay() {
        if(currentOverlay != null) {
            root.getChildren().remove(currentOverlay);
            currentOverlay = null;
        }
    }

    public boolean hasOverlay() {
        return currentOverlay != null;
    }
}