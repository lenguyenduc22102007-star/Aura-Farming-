package test;

import controller.actionmanager.PlayerController;
import controller.democontroller.DemoController;
import controller.lifecyclemanager.GameManager;
import controller.shopmanager.ShopController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.player_inventory.Player;
import model.soil.FarmMap;
import view.assets.Assets;
import view.screen_controller.HelpController;
import view.screen_controller.InGameController;
import view.screen_controller.MainMenuController;
import view.screen_controller.PauseController;
import view.screen_controller.ShopViewController;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;
import view.screen_util.ScreensLoader;

import java.net.URL;

public class GameTest extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Assets.load();

        SceneRouter router = new SceneRouter();

        GameManager gameManager = GameManager.getInstance();
        gameManager.reset();
        Player player = new Player();
        FarmMap farmMap = new FarmMap();
        PlayerController playerController = new PlayerController(player, gameManager);
        DemoController demoController = new DemoController(gameManager, farmMap);
        ShopController domainShop = new ShopController(player, gameManager);

        MainMenuController[] menuRef = new MainMenuController[1];

        GameLauncher mockLauncher = new GameLauncher() {
            private boolean sessionActive = false;

            @Override
            public void startNewFarm() {
                sessionActive = true;
                router.show(Screens.INGAME);
            }
            @Override
            public void continueFarm() {
                router.show(Screens.INGAME);
            }
            @Override
            public boolean hasSession() {
                return sessionActive;
            }
            @Override
            public void returnToMainMenu() {
                router.closeOverlay();
                if (menuRef[0] != null) {
                    menuRef[0].setContinueEnabled(sessionActive);
                }
                router.show(Screens.MAIN_MENU);
            }
            @Override
            public void quit() {
                javafx.application.Platform.exit();
            }
        };

        MainMenuController mainMenu = ScreensLoader
                .load(router, Screens.MAIN_MENU, Screens.MAIN_MENU_FXML)
                .getController();
        menuRef[0] = mainMenu;
        mainMenu.init(router, mockLauncher);
        mainMenu.setContinueEnabled(false);

        HelpController help = ScreensLoader
                .load(router, Screens.HELP, Screens.HELP_FXML)
                .getController();
        help.setRouter(router);

        InGameController inGame = ScreensLoader
                .load(router, Screens.INGAME, Screens.INGAME_FXML)
                .getController();
        inGame.setRouter(router);
        inGame.setGame(farmMap, player, gameManager, playerController, demoController);

        ShopViewController shop = ScreensLoader
                .load(router, Screens.SHOP, Screens.SHOP_FXML)
                .getController();
        shop.setRouter(router);
        shop.setShop(player, domainShop);

        PauseController pause = ScreensLoader
                .load(router, Screens.PAUSE, Screens.PAUSE_FXML)
                .getController();
        pause.init(router, mockLauncher);

        Scene scene = new Scene(router.getRoot(), 1280, 720);

        URL css = getClass().getResource(Screens.STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setTitle("Smart Farm (Test Mode)");
        stage.show();

        router.show(Screens.MAIN_MENU);
    }

    public static void main(String[] args) {
        launch();
    }
}
