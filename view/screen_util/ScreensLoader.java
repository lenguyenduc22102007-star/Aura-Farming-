package view.screen_util;

import exception.SmartFarmException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public final class ScreensLoader {
    private ScreensLoader() {}

    public static FXMLLoader load(SceneRouter router, String key, String path) {
        try {
            FXMLLoader loader = new FXMLLoader(ScreensLoader.class.getResource(path));
            Parent root = loader.load();
            router.register(key, root);
            return loader;
        } catch (IOException | RuntimeException e) {
            throw new SmartFarmException("Failed to load FXML screen '" + key + "' from " + path, e);
        }
    }
}
