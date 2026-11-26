
import controller.actionmanager.PlayerController;
import controller.democontroller.DemoController;
import controller.environmentcontroller.EnvironmentController;
import controller.farmcontroller.FarmController;
import controller.lifecyclemanager.BankruptcyCondition;
import controller.lifecyclemanager.GameManager;
import controller.lifecyclemanager.GameOverCondition;
import controller.lifecyclemanager.GameOverController;
import controller.lifecyclemanager.GameStatus;
import controller.lifecyclemanager.SaveManager;
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
import java.util.Properties;

public class Main extends Application implements GameLauncher {

    private SceneRouter router;
    private MainMenuController menuController;
    private PauseController pauseController;
    private Stage primaryStage;
    private String currentFarmName = "Nông Trại Vui Vẻ";

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
    private InGameController ingameView;

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

        this.primaryStage = stage;
        Assets.load();
        router = new SceneRouter();

        // session-less screens, registered once
        FXMLLoader menuLoader = ScreensLoader.load(router, Screens.MAIN_MENU, Screens.MAIN_MENU_FXML);
        menuController = menuLoader.getController();
        menuController.init(router, this);
        refreshMainMenuInfo(false);

        FXMLLoader helpLoader = ScreensLoader.load(router, Screens.HELP, Screens.HELP_FXML);
        HelpController help = helpLoader.getController();
        help.setRouter(router);

        FXMLLoader pauseLoader = ScreensLoader.load(router, Screens.PAUSE, Screens.PAUSE_FXML);
        pauseController = pauseLoader.getController();
        pauseController.init(router, this);

        Scene scene = new Scene(router.getRoot(), 1280, 720);
        URL css = getClass().getResource(Screens.STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setTitle("Smart Farm");
        stage.setResizable(false);
        stage.show();

        stage.setOnCloseRequest(e -> saveGame());

        router.show(Screens.MAIN_MENU);
    }

    // ---------------- GameLauncher ----------------
    @Override
    public void startNewFarm() {
        startNewFarm("Nông Trại Vui Vẻ");
    }

    @Override
    public void startNewFarm(String farmName) {
        String clean = (farmName == null) ? "" : farmName.trim();
        this.currentFarmName = clean.isEmpty() ? "Nông Trại Vui Vẻ" : clean;
        buildSession(null);
        saveGameAsync();
    }

    /** Build a session; when save != null the saved state is restored on top of the fresh farm. */
    private void buildSession(Properties save) {
        view.fx.SoundHook.stopMenuBgm();
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

        if (save != null) {
            this.currentFarmName = SaveManager.readFarmName(save);
            SaveManager.applyModel(save, gameManager, player, farmMap, shopController);
        } else {
            player.getInventory().addItem(new model.game_item.AnimalFeedItem(), 2);
            player.getInventory().addItem(new model.game_item.ChickenFeedItem(), 2);
            player.getInventory().addItem(new model.game_item.CowFeedItem(), 2);
            player.getInventory().addItem(new model.game_item.PigFeedItem(), 2);
        }
        if (primaryStage != null) {
            primaryStage.setTitle("Smart Farm — " + currentFarmName);
        }

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
        ingame.setShopAndPause(shopController, pauseController);
        if (pauseController != null) {
            pauseController.setSessionCallbacks(
                () -> ingame.openGameBook(view.ui.GameBookOverlay.TAB_SETTINGS),
                ingame::restoreGameFocus
            );
        }
        ingame.setFarmName(currentFarmName);
        ingame.setGame(farmMap, player, gameManager, playerController, demoController, tmxMap);
        ingame.setAutosave(this::saveGameAsync);
        this.ingameView = ingame;
        if (save != null) {
            ingame.importViewState(save);
        }

        FXMLLoader gameOverLoader = ScreensLoader.load(router, Screens.GAME_OVER, Screens.GAME_OVER_FXML);
        gameOverView = gameOverLoader.getController();
        gameOverView.init(router, this);

        router.show(Screens.INGAME);
    }

    @Override
    public void continueFarm() {
        view.fx.SoundHook.stopMenuBgm();
        if (hasSession()) {
            router.show(Screens.INGAME);
            return;
        }
        Properties save = SaveManager.read();
        if (save == null) {
            startNewFarm();
            return;
        }
        try {
            buildSession(save);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            SaveManager.delete();
            menuController.setContinueEnabled(false);
            startNewFarm();
        }
    }

    /** Write the current run to disk (no-op without a running session). */
    @Override
    public void saveGame() {
        if (!hasSession() || ingameView == null) return;
        Properties p = new Properties();
        SaveManager.captureModel(p, gameManager, player, farmMap, currentFarmName);
        ingameView.exportViewState(p);
        SaveManager.write(p);
    }

    /** Lưu nền không chặn JavaFX UI thread (dùng khi câu cá, chuyển ngày...) để giữ 60 FPS mượt mà. */
    public void saveGameAsync() {
        if (!hasSession() || ingameView == null) return;
        Properties p = new Properties();
        SaveManager.captureModel(p, gameManager, player, farmMap, currentFarmName);
        ingameView.exportViewState(p);
        SaveManager.writeAsync(p);
    }

    @Override
    public boolean hasSession() {
        // A finished (lost) run is no longer continuable.
        return player != null && farmMap != null
                && gameManager != null && gameManager.getStatus() == GameStatus.RUNNING;
    }

    @Override
    public void returnToMainMenu() {
        saveGame();
        refreshMainMenuInfo(true);
        router.show(Screens.MAIN_MENU);
    }

    private void refreshMainMenuInfo(boolean skipSplash) {
        if (menuController == null) return;
        boolean canContinue = hasSession() || SaveManager.exists();
        String name = currentFarmName;
        int day = 1;
        int money = Constant.START_MONEY;
        if (hasSession()) {
            day = gameManager.getCurrentDay();
            money = player.getMoney();
        } else {
            Properties saved = SaveManager.read();
            if (saved != null) {
                name = SaveManager.readFarmName(saved);
                this.currentFarmName = name;
                day = Integer.parseInt(saved.getProperty("day", "1"));
                money = Integer.parseInt(saved.getProperty("money", Integer.toString(Constant.START_MONEY)));
            }
        }
        menuController.setContinueEnabled(canContinue);
        menuController.updateFarmPreview(canContinue, name, day, money);
        if (skipSplash) {
            menuController.showDashboardDirectly();
        }
    }

    @Override
    public void quit() {
        view.fx.SoundHook.stopMenuBgm();
        saveGame();
        Platform.exit();
        System.exit(0);
    }

    // ---------------- helpers ----------------
    /**
     * Called by the GameOverController (via listener) when the run ends
     * (bankruptcy).
     */
    private void showGameOver(GameStatus status) {
        SaveManager.delete(); // a lost run can't be continued
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
