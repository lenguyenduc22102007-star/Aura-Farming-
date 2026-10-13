
import controller.actionmanager.PlayerController;
import controller.democontroller.DemoController;
import controller.environmentcontroller.EnvironmentController;
import controller.farmcontroller.FarmController;
import controller.lifecyclemanager.BankruptcyCondition;
import controller.lifecyclemanager.GameManager;
import controller.lifecyclemanager.GameOverCondition;
import controller.lifecyclemanager.GameOverController;
import controller.lifecyclemanager.GameStatus;
import controller.shopmanager.ItemManager;
import controller.shopmanager.ShopController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import model.soil.FarmMap;
import utils.Constant;
import view.assets.Assets;
import view.screen_controller.GameOverViewController;
import view.screen_controller.HelpController;
import view.screen_controller.InGameController;
import view.screen_controller.MainMenuController;
import view.screen_controller.PauseController;
import view.screen_controller.ShopViewController;
import view.screen_controller.StorageViewController;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;
import view.screen_util.ScreensLoader;

import java.net.URL;
import java.util.List;

public class Main extends Application implements GameLauncher {

    private SceneRouter router;
    private MainMenuController menuController;

    // --- current session (null until the first new farm) ---
    private Player player;
    private FarmMap farmMap;
    private GameManager gameManager;
    private PlayerController playerController;
    private ShopController shopController;
    private DemoController demoController;
    private GameOverViewController gameOverView;
    // held so the auto-registered DayObservers stay referenced
    private EnvironmentController environmentController;
    private FarmController farmController;
    private GameOverController gameOverController;

    @Override
    public void start(Stage stage) {
        // Single view-layer error boundary: surface anything that escapes to the
        // JavaFX thread as a friendly dialog instead of a silent console trace.
        Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Smart Farm");
            alert.setHeaderText("Unexpected error");
            alert.setContentText(String.valueOf(ex.getMessage()));
            alert.showAndWait();
        });

        Assets.load();
        router = new SceneRouter();

        // session-less screens, registered once
        FXMLLoader menuLoader = ScreensLoader.load(router, Screens.MAIN_MENU, Screens.MAIN_MENU_FXML);
        menuController = menuLoader.getController();
        menuController.init(router, this);
        menuController.setContinueEnabled(false);

        FXMLLoader helpLoader = ScreensLoader.load(router, Screens.HELP, Screens.HELP_FXML);
        HelpController help = helpLoader.getController();
        help.setRouter(router);

        FXMLLoader pauseLoader = ScreensLoader.load(router, Screens.PAUSE, Screens.PAUSE_FXML);
        PauseController pause = pauseLoader.getController();
        pause.init(router, this);

        Scene scene = new Scene(router.getRoot(), 1280, 720);
        URL css = getClass().getResource(Screens.STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setTitle("Smart Farm");
        stage.setResizable(false);
        stage.show();

        router.show(Screens.MAIN_MENU);
    }

    // ---------------- GameLauncher ----------------
    @Override
    public void startNewFarm() {
        gameManager = GameManager.getInstance();
        gameManager.reset();

        view.screen_util.TiledMap tmxMap = new view.screen_util.TiledMap("/view/assets/tiled/level1.tmx");
        if (tmxMap.width == 0 || tmxMap.height == 0) {
            System.err.println("Map failed to load! Using default 40x40 dimensions.");
            tmxMap.width = Constant.DEFAULT_FARM_COLS;
            tmxMap.height = Constant.DEFAULT_FARM_ROWS;
        }
        int[][] terrain = tmxMap.layers.isEmpty() ? null : tmxMap.layers.get(0).data;
        farmMap = new FarmMap(tmxMap.width, tmxMap.height, terrain);
        
        player = new Player();

        // Day-lifecycle order matters: FarmController (growth) must run before
        // EnvironmentController (rolls the next day's weather + pests) so crops
        // resolve under the weather the player saw. GameOverController evaluates
        // last, after the day has fully resolved. Observers fire in registration
        // (construction) order, so they are created in exactly that order here.
        farmController = new FarmController(gameManager, farmMap);
        environmentController = new EnvironmentController(gameManager, farmMap);
        shopController = new ShopController(player, gameManager);
        playerController = new PlayerController(player, gameManager);
        demoController = new DemoController(gameManager, farmMap);

        // Simulator: the only outcome is a loss (bankruptcy) — no win condition.
        List<GameOverCondition> conditions = List.of(
                new BankruptcyCondition(cheapestSeedPrice(shopController))
        );
        gameOverController = new GameOverController(
                gameManager, farmMap, player, conditions, this::showGameOver,
                Constant.GAME_OVER_GRACE_DAYS);

        FXMLLoader shopLoader = ScreensLoader.load(router, Screens.SHOP, Screens.SHOP_FXML);
        ShopViewController shopView = shopLoader.getController();
        shopView.setRouter(router);
        shopView.setShop(player, shopController);

        FXMLLoader storageLoader = ScreensLoader.load(router, Screens.STORAGE, Screens.STORAGE_FXML);
        StorageViewController storageView = storageLoader.getController();
        storageView.setRouter(router);
        storageView.setPlayer(player);

        FXMLLoader ingameLoader = ScreensLoader.load(router, Screens.INGAME, Screens.INGAME_FXML);
        InGameController ingame = ingameLoader.getController();
        ingame.setRouter(router);
        ingame.setGame(farmMap, player, gameManager, playerController, demoController, tmxMap);

        FXMLLoader gameOverLoader = ScreensLoader.load(router, Screens.GAME_OVER, Screens.GAME_OVER_FXML);
        gameOverView = gameOverLoader.getController();
        gameOverView.init(router, this);

        router.show(Screens.INGAME);
    }

    @Override
    public void continueFarm() {
        if (!hasSession()) {
            startNewFarm();
        } else {
            router.show(Screens.INGAME);
        }
    }

    @Override
    public boolean hasSession() {
        // A finished (lost) run is no longer continuable.
        return player != null && farmMap != null
                && gameManager != null && gameManager.getStatus() == GameStatus.RUNNING;
    }

    @Override
    public void returnToMainMenu() {
        if (menuController != null) {
            menuController.setContinueEnabled(hasSession());
        }
        router.show(Screens.MAIN_MENU);
    }

    @Override
    public void quit() {
        Platform.exit();
        System.exit(0);
    }

    // ---------------- helpers ----------------
    /**
     * Called by the GameOverController (via listener) when the run ends
     * (bankruptcy).
     */
    private void showGameOver(GameStatus status) {
        if (gameOverView != null) {
            gameOverView.showResult(gameManager.getCurrentDay(), player.getMoney());
        }
        router.show(Screens.GAME_OVER);
    }

    /**
     * Lowest price among the seeds in the shop catalog (used for bankruptcy).
     */
    private int cheapestSeedPrice(ShopController shop) {
        int min = Integer.MAX_VALUE;
        for (ItemManager entry : shop.getCatalog()) {
            Item prototype = entry.create();
            if (prototype instanceof Seed && prototype.getPrice() < min) {
                min = prototype.getPrice();
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
