package view.screen_controller;

import controller.actionmanager.*;
import controller.democontroller.DemoController;
import controller.lifecyclemanager.GameManager;
import exception.SmartFarmException;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.crop.Crop;
import model.crop.GrowthState;
import model.game_item.Item;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.CellState;
import model.soil.FarmMap;
import model.soil.TerrainType;
import model.weather.WeatherType;
import utils.Constant;
import view.assets.Assets;
import view.screen_util.NpcDialog;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class InGameController {

    // --- tool hotbar ---
    @FXML private ToggleButton hoeTool;
    @FXML private ToggleButton axeTool;
    @FXML private ToggleButton waterTool;
    @FXML private ToggleButton seedTool;
    @FXML private ToggleButton handTool;
    @FXML private ToggleButton fertilizerTool;
    @FXML private ToggleButton bagTool;

    // --- weather selector ---
    @FXML private ToggleButton sunWeatherButton;
    @FXML private ToggleButton rainWeatherButton;
    @FXML private ToggleButton dryWeatherButton;
    @FXML private javafx.scene.control.Button fishingGuideButton;
    @FXML private javafx.scene.layout.VBox questTrackerBox;
    @FXML private Label questTitleLabel;
    @FXML private Label questDescLabel;
    @FXML private Label questProgressLabel;
    @FXML private Label questRewardLabel;
    @FXML private javafx.scene.control.Button questClaimButton;

    // --- layout + labels ---
    @FXML private BorderPane rootPane;
    @FXML private GridPane farmGrid;
    @FXML private javafx.scene.layout.Pane weatherOverlay;
    private javafx.scene.shape.Rectangle timeOverlayRect;
    @FXML private Label toolLabel;
    @FXML private TextField notificationField;
    @FXML private TextField dayField;
    @FXML private Label moneyField;
    @FXML private javafx.scene.layout.VBox soilStatusPanel;
    @FXML private Label cellLabel;
    @FXML private Label waterLabel;
    @FXML private Label nutrientLabel;
    @FXML private Label cropLabel;

    private SceneRouter router;
    private controller.shopmanager.ShopController shopController;
    private PauseController pauseController;

    // injected domain
    private FarmMap farmMap;
    private Player player;
    private GameManager gameManager;
    private PlayerController playerController;
    private DemoController demoController;

    private int helperDaysLeft = 0;
    
    // tool strategy instances
    private Hoe hoe;
    private Axe axe;
    private WateringCan wateringCan;
    private SeedBag seedBag;
    private Hand hand;
    private FertilizerBag fertilizerBag;
    private FeedBag feedBag;

    private Tile[][] tiles;
    private Cell hovered;
    private static final CornerRadii TILE_RADII = new CornerRadii(6);

    // ======================== HỆ THỐNG ACTION RPG ========================
    private Set<KeyCode> activeKeys = new HashSet<>();
    private AnimationTimer gameLoop;
    private Pane mapPane;
    private Pane entityPane;
    private javafx.scene.shape.Rectangle facingHighlight;
    private javafx.scene.Group playerCharacter;
    private javafx.scene.shape.Ellipse luffyShadow;
    private ImageView luffySpriteView;
    private view.river.RiverFlow riverFlow;
    private view.river.FishShadows fishShadows;
    private view.river.BoatEntity boatEntity;
    private view.river.FishingGame fishingGame;
    private view.fx.ParticleSystem particleSystem;
    private view.fx.DroppedItemSystem droppedItemSystem;
    private boolean isOnBoat = false;
    private long lastRiverUpdateMs = 0L;
    private final java.util.Map<model.game_item.FishSpecies, Integer> fishCaughtCounts = new java.util.EnumMap<>(model.game_item.FishSpecies.class);
    private final java.util.Map<model.game_item.FishSpecies, Double> fishMaxCm = new java.util.EnumMap<>(model.game_item.FishSpecies.class);
    private view.ui.FishingGuideOverlay activeFishingGuide = null;
    private view.ui.GameBookOverlay activeGameBook = null;
    private view.ui.CookingCraftView activeCookingOverlay = null;
    private view.ui.CasinoOverlay activeCasinoOverlay = null;
    private view.ui.FactoryView activeFactoryView = null;
    private javafx.scene.layout.StackPane activeGiftDialog = null;
    private view.npc.NpcManager npcManager = null;
    private controller.environmentcontroller.EnvironmentController environmentController = null;
    private final model.factory.FactoryManager factoryManager = new model.factory.FactoryManager();
    private final model.factory.MarketManager marketManager = new model.factory.MarketManager();
    private final model.festival.FestivalManager festivalManager = new model.festival.FestivalManager();
    private final view.mine.MineManager mineManager = new view.mine.MineManager();
    private javafx.scene.layout.StackPane activeSleepDialog = null;
    private boolean sleepTransitionRunning = false;
    private boolean isSitting = false;
    private double sittingFireX = 0.0, sittingFireY = 0.0;
    private final java.util.List<CampForageSpot> campForageSpots = new java.util.ArrayList<>();
    private final java.util.List<TreeObject> choppableTrees = new java.util.ArrayList<>();
    private boolean pendingAxeChopFelled = false;
    private final java.util.List<double[]> factoryDoorPoints = new java.util.ArrayList<>();
    private javafx.geometry.Rectangle2D orchardGateBounds = null;
    private javafx.geometry.Rectangle2D orchardInteractBox = null;
    private boolean orchardUnlocked = false;
    private Label orchardWorldSignLabel = null;
    private Label festivalTopBannerLabel = null;
    private double fireworkBurstTimer = 0.0;

    /** Đối tượng cây gỗ có thể chặt bằng Rìu quanh nông trại (HP=4, rớt +4 Củi khô, mọc lại sau 3 ngày). */
    private static final class TreeObject {
        final String key;
        final ImageView view;
        final Image fullTreeImage;
        final double fullX;
        final double fullY;
        final double fullW;
        final double fullH;
        int hp = 4;
        int regrowDays = 0;
        boolean chopped = false;

        TreeObject(String key, ImageView view, Image fullTreeImage,
                   double fullX, double fullY, double fullW, double fullH) {
            this.key = key;
            this.view = view;
            this.fullTreeImage = fullTreeImage;
            this.fullX = fullX;
            this.fullY = fullY;
            this.fullW = fullW;
            this.fullH = fullH;
        }

        double trunkCenterX() { return fullX + fullW * 0.50; }
        double trunkCenterY() { return fullY + fullH * 0.86; }
    }

    private static final class CampForageSpot {
        final model.game_item.ForageItem item;
        final double x;
        final double y;
        final javafx.scene.Group node;
        final double bobPhase;

        CampForageSpot(model.game_item.ForageItem item, double x, double y, javafx.scene.Group node, double bobPhase) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.node = node;
            this.bobPhase = bobPhase;
        }
    }
    private final java.util.List<double[]> houseDoorPoints = new java.util.ArrayList<>();
    private final java.util.List<double[]> campfirePoints = new java.util.ArrayList<>();
    private final java.util.List<double[]> casinoDoorPoints = new java.util.ArrayList<>();
    private final java.util.List<double[]> poolSunbathePoints = new java.util.ArrayList<>();
    private final java.util.List<javafx.scene.shape.Line> poolWaveLines = new java.util.ArrayList<>();
    private final java.util.List<javafx.scene.shape.Circle> poolSparkleDots = new java.util.ArrayList<>();
    private final java.util.List<javafx.scene.shape.Circle> casinoMarqueeBulbs = new java.util.ArrayList<>();
    private double poolWaterX = 0.0, poolWaterY = 0.0, poolWaterW = 0.0, poolWaterH = 0.0;
    private double sunbatheLoungerX = 672.0, sunbatheLoungerY = 2630.0;
    private boolean isSunbathing = false;
    private double poolRelaxSpeedBuffSec = 0.0;
    private int lastRenderedRelaxSec = -1;
    private double sunbatheSparkleTimer = 0.0;
    private boolean suppressQuestMoneyTracking = false;
    private final java.util.List<javafx.scene.shape.Circle> campfireGlows = new java.util.ArrayList<>();
    /** Các cụm ánh sáng của cột đèn đường (tự động bật sáng ấm áp bắt đầu từ 18:00 tối). */
    private final java.util.List<javafx.scene.Node[]> streetlampLights = new java.util.ArrayList<>();
    private double[] farmGatePortalBox = null; // [x0, y0, x1, y1] vùng đi qua dưới vòm Cổng Chào Nông Trại
    private boolean playerInsideFarmGateZone = false;
    private double campfireEmberTimer = 0.0;
    private Label interactPromptLabel = null;
    private Label survivalWarningLabel = null;
    private javafx.scene.layout.Pane hudOverlayPane = null;
    private javafx.scene.layout.VBox survivalHudBox = null;
    private Label hungerValueLabel = null;
    private javafx.scene.shape.Rectangle hungerBarFill = null;
    private Label thirstValueLabel = null;
    private javafx.scene.shape.Rectangle thirstBarFill = null;
    private int lastRenderedHungerInt = -1;
    private int lastRenderedThirstInt = -1;
    private boolean lastHungerLow = false;
    private boolean lastThirstLow = false;
    private double worldRiverTopY = 32.0 * Constant.CELL_SIZE;
    private double worldRiverBotY = 37.0 * Constant.CELL_SIZE;
    private double bridgeWoodLeftX = -1.0;
    private double bridgeWoodRightX = -1.0;
    private double bridgeDeckLeftX = -1.0;
    private double bridgeDeckRightX = -1.0;
    private double bridgeDeckTopY = -1.0;
    private double bridgeDeckBotY = -1.0;
    private double bridgeArchHeightRatio = 0.0;
    private double bridgeArchSlope = 0.0;
    private javafx.scene.transform.Scale luffyBridgeScale = null;
    private javafx.scene.transform.Scale luffyToolBridgeScale = null;
    private ImageView bridgeCanopyView = null;
    private double northAbutmentX = -1.0, northAbutmentY = -1.0, northAbutmentW = 0.0, northAbutmentH = 0.0;
    private final java.util.List<BridgePillarOverlay> bridgePillarOverlays = new java.util.ArrayList<>();

    private static final class BridgePillarOverlay {
        final ImageView view;
        final double baseY;
        boolean lastPlayerBehind;

        BridgePillarOverlay(ImageView view, double baseY) {
            this.view = view;
            this.baseY = baseY;
            this.lastPlayerBehind = false;
        }
    }
    private final javafx.scene.effect.ColorAdjust underBridgePlayerShade = new javafx.scene.effect.ColorAdjust();
    private double lastPlayerBridgeShade = 0.0;
    private long lastFishLaborMs = 0L;
    private static final long DAY_DURATION_MS = 600_000L; // 600 giây/ngày (chu kỳ đủ 24 giờ 00:00 -> 24:00, ~25 giây/giờ game)
    private static final long MORNING_START_MS = DAY_DURATION_MS * 6L / 24L; // 06:00 sáng (150_000 ms)
    private static final double DAY_DURATION_SEC = DAY_DURATION_MS / 1000.0;
    private static final double HUNGER_FULL_GRACE_SEC = DAY_DURATION_SEC * 4.0 / 24.0; // đúng 4 tiếng trong game mới bắt đầu đói
    private double hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
    private int recentLaborStreak = 0;
    private double recentLaborDecaySec = 0.0;
    private final java.util.Set<Tile> waterNeedTiles = new java.util.HashSet<>();
    private final java.util.Set<Tile> ripeCropTiles = new java.util.HashSet<>();

    private String farmName = "Nông Trại Vui Vẻ";
    private Label survivalHeaderTitle = null;

    public void setFarmName(String farmName) {
        if (farmName != null && !farmName.trim().isEmpty()) {
            this.farmName = farmName.trim();
        }
        if (survivalHeaderTitle != null) {
            survivalHeaderTitle.setText(this.farmName.toUpperCase());
        }
    }

    private Label foodBuffHudPill = null;
    private boolean foodRareFishBuff = false;
    private Label boatHintBanner = null;
    private boolean boatHintVisible = false;
    private javafx.scene.layout.VBox fishCatchCard = null;
    private javafx.animation.SequentialTransition fishCatchAnim = null;
    private java.util.Map<String, Image[]> luffyIdleByDir;
    private java.util.Map<String, Image[]> luffyRunByDir;
    private long lastLuffyAnimTick = 0;
    private int luffyAnimFrame = 0;
    private boolean luffyWasWalking = false;
    private String luffyLastDirKey = "down";
    private boolean luffyFacingLeft = false;
    private javafx.scene.transform.Rotate leftArmSwing, rightArmSwing, leftLegSwing, rightLegSwing;
    private javafx.scene.Group luffyToolRig;            // Khung lật đối xứng quanh trục nhân vật (x=22)
    private javafx.scene.transform.Scale luffyToolFlip;
    private javafx.scene.Group armGroup;                // Cánh tay (áo đỏ + cẳng tay + bàn tay nắm) + công cụ xoay quanh khớp vai (0,0)
    private javafx.scene.transform.Rotate armRotate;
    private javafx.scene.transform.Rotate luffyBodyRotate; // Nghiêng nhẹ thân Luffy theo nhịp bổ cuốc / tưới cây
    private ImageView weaponView; // Công cụ sắc nét (smooth=false) gắn vào bàn tay cuối armGroup
    private javafx.scene.transform.Rotate toolRotate; // Nghiêng cổ tay (chuôi cầm) khi tưới nước / rắc hạt
    private double toolActionTimer = -1.0;
    private double toolActionDuration = 0.29;
    private String toolActionKind = "NONE"; // HOE, WATER, SPRINKLE
    private boolean toolImpactFired = false;
    private double toolTargetWorldX = 0.0;
    private double toolTargetWorldY = 0.0;
    private double walkSwayTime = 0.0;
    private long lastMoveTime = 0;
    private boolean isWalking = false;
    private long actionTime = 0;
    private java.util.List<ImageView> animatedFlags = new java.util.ArrayList<>();
    private final java.util.List<ImageView> animatedCampfires = new java.util.ArrayList<>();
    private double flagAnimTimer = 0.0;
    private int flagAnimFrame = 0;
    private double campfireAnimTimer = 0.0;
    private int campfireAnimFrame = 0;
    private java.util.List<ImageView> sceneryObjects = new java.util.ArrayList<>();
    private java.util.Set<String> clearedSceneryKeys = new java.util.HashSet<>();

    // Trạng thái nhân vật
    private double playerX = 100;
    private double playerY = 100;
    private double speed = 5.0;
    private String facing = "DOWN"; // Hướng nhìn: UP, DOWN, LEFT, RIGHT

    // ======================== FXML lifecycle ========================
    @FXML
    private void initialize() {
        rootPane.getStylesheets().add(getClass().getResource("/view/css/Style.css").toExternalForm());
        setupToolIcons();
        setupWeatherIcons();
        setupTimeOverlay();
        rootPane.sceneProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(rootPane::requestFocus);
            }
        });
    }

    private javafx.scene.shape.Rectangle weatherTintRect;
    private javafx.scene.shape.Rectangle dayFadeRect;
    private javafx.animation.SequentialTransition dayFadeAnim;
    private Color currentWeatherTint = Color.TRANSPARENT;
    private Color targetWeatherTint = Color.TRANSPARENT;
    private Color currentTimeTint = Color.TRANSPARENT;
    private double rainIntensity = 0.0;
    private double rainSpawnCarry = 0.0;

    private void setupTimeOverlay() {
        if (rootPane == null) return;
        if (weatherTintRect == null) {
            weatherTintRect = new javafx.scene.shape.Rectangle();
            weatherTintRect.setMouseTransparent(true);
            weatherTintRect.setManaged(false);
            weatherTintRect.setX(0);
            weatherTintRect.setY(0);
            weatherTintRect.widthProperty().bind(rootPane.widthProperty());
            weatherTintRect.heightProperty().bind(rootPane.heightProperty());
            weatherTintRect.setFill(Color.TRANSPARENT);
            rootPane.getChildren().add(weatherTintRect);
        }
        if (timeOverlayRect == null) {
            timeOverlayRect = new javafx.scene.shape.Rectangle();
            timeOverlayRect.setMouseTransparent(true);
            timeOverlayRect.setManaged(false);
            timeOverlayRect.setX(0);
            timeOverlayRect.setY(0);
            timeOverlayRect.widthProperty().bind(rootPane.widthProperty());
            timeOverlayRect.heightProperty().bind(rootPane.heightProperty());
            currentTimeTint = getTimeOverlayColor((double) MORNING_START_MS / DAY_DURATION_MS);
            timeOverlayRect.setFill(currentTimeTint);
            rootPane.getChildren().add(timeOverlayRect);
        } else {
            if (weatherOverlay != null) {
                weatherOverlay.getChildren().remove(timeOverlayRect);
            }
            if (!rootPane.getChildren().contains(weatherTintRect)) {
                rootPane.getChildren().add(weatherTintRect);
            }
            if (!rootPane.getChildren().contains(timeOverlayRect)) {
                rootPane.getChildren().add(timeOverlayRect);
            }
            weatherTintRect.toFront();
            timeOverlayRect.setManaged(false);
            timeOverlayRect.setX(0);
            timeOverlayRect.setY(0);
            timeOverlayRect.toFront();
        }
        if (dayFadeRect == null) {
            dayFadeRect = new javafx.scene.shape.Rectangle();
            dayFadeRect.setMouseTransparent(true);
            dayFadeRect.setManaged(false);
            dayFadeRect.setX(0);
            dayFadeRect.setY(0);
            dayFadeRect.widthProperty().bind(rootPane.widthProperty());
            dayFadeRect.heightProperty().bind(rootPane.heightProperty());
            dayFadeRect.setFill(Color.rgb(22, 18, 36));
            dayFadeRect.setOpacity(0.0);
            rootPane.getChildren().add(dayFadeRect);
        } else {
            if (!rootPane.getChildren().contains(dayFadeRect)) {
                rootPane.getChildren().add(dayFadeRect);
            }
            dayFadeRect.toFront();
        }
    }

    /** Hiệu ứng chuyển ngày mượt mà (màn sương hoàng hôn -> bình minh dịu nhẹ, không khựng khung hình). */
    private void playDayTransitionFade() {
        if (dayFadeRect == null) return;
        dayFadeRect.toFront();
        if (dayFadeAnim != null) {
            dayFadeAnim.stop();
        }
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(170), dayFadeRect);
        fadeIn.setFromValue(dayFadeRect.getOpacity());
        fadeIn.setToValue(0.42);
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(380), dayFadeRect);
        fadeOut.setFromValue(0.42);
        fadeOut.setToValue(0.0);
        dayFadeAnim = new javafx.animation.SequentialTransition(fadeIn, fadeOut);
        dayFadeAnim.play();
    }

    public void setRouter(SceneRouter router) {
        this.router = router;
    }

    public void setShopAndPause(controller.shopmanager.ShopController shopController, PauseController pauseController) {
        this.shopController = shopController;
        this.pauseController = pauseController;
    }

    public void setEnvironmentController(controller.environmentcontroller.EnvironmentController envController) {
        this.environmentController = envController;
    }

    public void restoreGameFocus() {
        activeKeys.clear();
        if (rootPane != null) {
            Platform.runLater(rootPane::requestFocus);
        }
    }

    private view.screen_util.TiledMap tmxMap;
    private java.util.List<ImageView> windmillBladeViews = new java.util.ArrayList<>();
    private javafx.scene.image.Image[] windmillBladeFrames = null;
    private long lastWindmillTick = 0;
    private int windmillFrameIdx = 0;

    public void setGame(FarmMap farmMap, Player player, GameManager gameManager,
                        PlayerController playerController, DemoController demoController) {
        setGame(farmMap, player, gameManager, playerController, demoController, null);
    }

    public void setGame(FarmMap farmMap, Player player, GameManager gameManager,
                        PlayerController playerController, DemoController demoController,
                        view.screen_util.TiledMap tmxMap) {
        this.farmMap          = farmMap;
        this.player           = player;
        this.gameManager      = gameManager;
        this.playerController = playerController;
        this.demoController   = demoController;
        this.tmxMap           = tmxMap;

        this.hoe            = new Hoe();
        this.axe            = new Axe();
        this.wateringCan    = new WateringCan();
        this.seedBag        = new SeedBag(player);
        this.hand           = new Hand(player);
        this.fertilizerBag  = new FertilizerBag("Standard Fertilizer", player);
        this.feedBag        = new FeedBag(player);

        moneyField.textProperty().bind(player.moneyProperty().asString("$%d"));
        player.moneyProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressQuestMoneyTracking && activeCasinoOverlay == null && oldVal != null && newVal != null) {
                int earned = newVal.intValue() - oldVal.intValue();
                if (earned > 0) {
                    onQuestMoneyEarned(earned);
                }
            }
        });

        wireButtons();
        setKeyHandlers();
        
        // Thay thế hàm buildGrid bằng hàm xây bản đồ tự do
        buildFreeMap(); 

        // Mặc định cầm sẵn Cuốc (Hoe) khi bắt đầu game mới
        selectTool(hoe, hoeTool);
        Platform.runLater(this::refreshAll);
        
        // Bắt đầu vòng lặp game
        startGameLoop();
    }

    // ======================== wiring ========================
    private void wireButtons() {
        hoeTool.setOnAction(e -> selectTool(hoe, hoeTool));
        if (axeTool != null) {
            axeTool.setOnAction(e -> selectTool(axe, axeTool));
        }
        waterTool.setOnAction(e -> selectTool(wateringCan, waterTool));
        seedTool.setOnAction(e -> openSeedPicker());
        handTool.setOnAction(e -> selectTool(hand, handTool));
        fertilizerTool.setOnAction(e -> selectTool(fertilizerBag, fertilizerTool));
        if (bagTool != null) {
            bagTool.setOnAction(e -> openFeedBagPicker());
        }

        sunWeatherButton.setOnAction(e -> forceWeather(WeatherType.SUNNY));
        rainWeatherButton.setOnAction(e -> forceWeather(WeatherType.RAINY));
        dryWeatherButton.setOnAction(e -> forceWeather(WeatherType.DROUGHT));
    }

    private void setKeyHandlers() {
        rootPane.setFocusTraversable(true);
        Platform.runLater(() -> rootPane.requestFocus());

        hoeTool.setFocusTraversable(false);
        if (axeTool != null) axeTool.setFocusTraversable(false);
        waterTool.setFocusTraversable(false);
        seedTool.setFocusTraversable(false);
        handTool.setFocusTraversable(false);
        fertilizerTool.setFocusTraversable(false);
        if (bagTool != null) bagTool.setFocusTraversable(false);
        sunWeatherButton.setFocusTraversable(false);
        rainWeatherButton.setFocusTraversable(false);
        dryWeatherButton.setFocusTraversable(false);
        if (fishingGuideButton != null) fishingGuideButton.setFocusTraversable(false);

        // Chặn phím TAB khỏi cơ chế chuyển focus mặc định của JavaFX để mở/đóng Cuốn Sổ Quản Lý 5 Tab mượt mà
        rootPane.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.TAB && !sleepTransitionRunning) {
                ev.consume();
                toggleGameBook(view.ui.GameBookOverlay.TAB_STORAGE);
            }
        });

        rootPane.setOnKeyPressed(event -> {
            if (sleepTransitionRunning) {
                event.consume();
                return;
            }
            KeyCode code = event.getCode();
            boolean isNewPress = activeKeys.add(code); // true chỉ khi nhấn phím mới (bỏ qua key-repeat khi giữ lì)
            if (code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                event.consume(); // Ngăn phím mũi tên nhảy focus sang nút HUD
            }

            switch (code) {
                case ESCAPE:  togglePause();   break;
                case DIGIT1:  openSeedPicker(); break;
                case DIGIT2:  selectTool(hoe, hoeTool);            break;
                case DIGIT3:  selectTool(axe, axeTool);            break;
                case DIGIT4:  selectTool(wateringCan, waterTool);  break;
                case DIGIT5:  selectTool(hand, handTool);          break;
                case DIGIT6:  selectTool(fertilizerBag, fertilizerTool); break;
                case DIGIT7:  openFeedBagPicker();                 break;
                case C:
                    if (isNewPress && !router.hasOverlay() && !isOnBoat && !isSunbathing) {
                        trySitAtCampfire();
                    }
                    break;
                case U:
                    if (isNewPress && !router.hasOverlay() && isSitting) {
                        standUpFromCampfire();
                    }
                    break;
                case E:
                    if (isNewPress) {
                        handleInteractKeyE();
                    }
                    break;
                case R:
                    if (isNewPress && !router.hasOverlay()) {
                        tryDrinkRiverWater();
                    }
                    break;
                case Q:
                    if (isNewPress && !router.hasOverlay()) {
                        tryQuickEatFood();
                    }
                    break;
                case J:       // Nút Action (Cuốc đất, trồng cây / Spam kéo cá khi trên thuyền)
                case SPACE:
                    if (isSitting) break;
                    if (isOnBoat) {
                        event.consume();
                        if (isNewPress) {
                            interactWithFacingCell();
                        }
                    } else {
                        interactWithFacingCell();
                    }
                    break;
                case F:
                    if (isOnBoat && fishingGame != null && !router.hasOverlay()) {
                        event.consume();
                        if (isNewPress) {
                            fishingGame.onActionPressed();
                        }
                        break;
                    }
                    demoController.fastForwardGrowthMap();
                    for (Animal a : animals) {
                        if (a.type != 2 && a.ageDays < 5) {
                            a.ageDays++;
                            a.applySpriteScale();
                        }
                    }
                    notify("Tua nhanh cây trồng & thú cưng (+1 ngày tuổi)");
                    refreshDynamicTiles();
                    break;
                case G:
                    event.consume();
                    if (isNewPress && !router.hasOverlay() && !isOnBoat && npcManager != null) {
                        view.npc.NpcManager.NpcTarget nearNpc = npcManager.findNearestNpc(playerX + 22.0, playerY + 52.0, 84.0);
                        if (nearNpc != null) {
                            openNpcGiftOverlay(nearNpc);
                            break;
                        }
                    }
                    toggleFishingGuide();
                    break;
                case V:
                    if (isNewPress && !router.hasOverlay() && !isOnBoat && !isSitting && !isSunbathing) {
                        tryPlaceAutoSprinklerUnderFeet();
                    }
                    break;
                case I:
                    openStorageMenu();
                    break;
                case N:
                    onNextDay();
                    break;
                case P:
                    boolean any = demoController.spawnDemoPest();
                    notify(any ? "Spawned a pest" : "No crops to infest");
                    refreshAll();
                    break;
                case T:
                    demoController.tillAll();
                    notify("Tilled every cell");
                    refreshAll();
                    break;
                default:
                    break;
            }
        });

        rootPane.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
        });
    }

    // ======================== HÀNH ĐỘNG NHÂN VẬT ========================
    private Cell getFacingCell() {
        int col = (int) ((playerX + 22) / Constant.CELL_SIZE);
        int row = (int) ((playerY + 50) / Constant.CELL_SIZE);

        switch(facing) {
            case "UP":         row -= 1; break;
            case "DOWN":       row += 1; break;
            case "LEFT":       col -= 1; break;
            case "RIGHT":      col += 1; break;
            case "UP_LEFT":    row -= 1; col -= 1; break;
            case "UP_RIGHT":   row -= 1; col += 1; break;
            case "DOWN_LEFT":  row += 1; col -= 1; break;
            case "DOWN_RIGHT": row += 1; col += 1; break;
        }

        return farmMap.getCell(row, col);
    }

    private boolean isCellBlockedByStructure(Cell cell) {
        if (cell == null || sceneryObjects == null) return false;
        double cellX = cell.getX() * Constant.CELL_SIZE + 8;
        double cellY = cell.getY() * Constant.CELL_SIZE + 8;
        javafx.geometry.Rectangle2D cellBox = new javafx.geometry.Rectangle2D(cellX, cellY, Constant.CELL_SIZE - 16, Constant.CELL_SIZE - 16);
        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id == null) continue;
            String idLower = id.toLowerCase();
            if (idLower.contains("tent") || idLower.contains("campfire") || idLower.contains("house") ||
                idLower.contains("barn") || idLower.contains("well") || idLower.contains("pet_shop") ||
                idLower.contains("windmill") || idLower.contains("garden_bed") ||
                idLower.contains("casino") || idLower.contains("swimming_pool") || idLower.contains("pool_lounger")) {
                double ox = objView.getLayoutX() + objView.getTranslateX();
                double oy = objView.getLayoutY() + objView.getTranslateY();
                double ow = objView.getFitWidth();
                double oh = objView.getFitHeight();
                double hh = (idLower.contains("tent") || idLower.contains("campfire")) ? oh * 0.72 : oh * 0.45;
                double hw = ow * 0.82;
                javafx.geometry.Rectangle2D structBox = new javafx.geometry.Rectangle2D(
                    ox + (ow - hw) / 2, oy + oh - hh, hw, hh
                );
                if (cellBox.intersects(structBox)) {
                    return true;
                }
            }
        }
        for (javafx.geometry.Rectangle2D afBox : animalFarmColliders) {
            if (cellBox.intersects(afBox)) {
                return true;
            }
        }
        return false;
    }

    private void interactWithFacingCell() {
        if (router.hasOverlay()) return;

        // Tương tác lên thuyền & Câu cá từ trên thuyền (Space / J)
        if (boatEntity != null) {
            if (isOnBoat && boatEntity.isPlayerAboard()) {
                if (fishingGame != null) {
                    if (!fishingGame.isActive() && isBoatCenterUnderBridge()) {
                        notify("Đang ở dưới gầm cầu — hãy chèo qua khoảng trống rồi câu cá!");
                        return;
                    }
                    long nowMs = System.currentTimeMillis();
                    if (nowMs - lastFishLaborMs >= 1500L) {
                        lastFishLaborMs = nowMs;
                        applyLaborSurvivalCost();
                    }
                    fishingGame.onActionPressed();
                }
                return;
            }
            if (boatEntity.isTransitioning()) {
                return;
            }
            if (boatEntity.isPlayerInBoardingZone(playerX, playerY)) {
                boatEntity.startBoarding(playerX, playerY);
                return;
            }
        }
        
        actionTime = System.currentTimeMillis(); // Kích hoạt hiệu ứng vung vũ khí

        double targetX = playerX + 22;
        double targetY = playerY + 50;
        double reach = 40;
        double diagReach = reach * 0.75;
        switch(facing) {
            case "UP":         targetY -= reach; break;
            case "DOWN":       targetY += reach; break;
            case "LEFT":       targetX -= reach; break;
            case "RIGHT":      targetX += reach; break;
            case "UP_LEFT":    targetX -= diagReach; targetY -= diagReach; break;
            case "UP_RIGHT":   targetX += diagReach; targetY -= diagReach; break;
            case "DOWN_LEFT":  targetX -= diagReach; targetY += diagReach; break;
            case "DOWN_RIGHT": targetX += diagReach; targetY += diagReach; break;
        }
        
        javafx.geometry.Rectangle2D hitBox = new javafx.geometry.Rectangle2D(targetX - 10, targetY - 10, 20, 20);

        // Kiểm tra tương tác với Máng ăn, NPC và Pet Shop
        if (playerController.getCurrentTool() instanceof Hand || playerController.getCurrentTool() instanceof FeedBag) {
            if (tryDepositFeed(hitBox)) {
                return;
            }
        }
        if (playerController.getCurrentTool().getName().equals("Hand")) {
            for (ImageView objView : sceneryObjects) {
                String id = objView.getId();
                if (id != null) {
                    javafx.geometry.Bounds b = objView.getBoundsInParent();
                    javafx.geometry.Rectangle2D bRect = new javafx.geometry.Rectangle2D(b.getMinX(), b.getMinY(), Math.max(1, b.getWidth()), Math.max(1, b.getHeight()));
                    if (hitBox.intersects(bRect)) {
                        if (id.equals("pet_shop")) {
                            openShop();
                            return;
                        } else if (id.equals("npc_zoro")) {
                            openZoroDialog();
                            return;
                        }
                    }
                }
            }
        }
        
        // Khóa cuốc đất, chặt cây & tưới nước khi kiệt sức (Đói <= 0 hoặc Khát <= 0)
        ToolStrategy curToolCheck = playerController.getCurrentTool();
        if (player != null && player.isExhausted() && (curToolCheck instanceof Hoe || curToolCheck instanceof Axe || curToolCheck instanceof WateringCan)) {
            spawnFloatingText("Quá đói/khát! Hãy ăn uống trước!", Color.rgb(255, 86, 86));
            notify("Quá đói/khát! Hãy ăn uống trước!");
            rootPane.requestFocus();
            return;
        }

        // Rìu (Axe): chặt các cây gỗ quanh rìa nông trại để thu hoạch Củi khô (HP=4, +4 Củi khô, mọc lại sau 3 ngày)
        if (curToolCheck instanceof Axe) {
            tryChopNearestTree(targetX, targetY);
            rootPane.requestFocus();
            return;
        }

        // Cuốc (Hoe): ưu tiên đập tảng đá mỏ quặng ở Khu Khai Khoáng Mini trước khi kiểm tra khu trồng trọt!
        if (curToolCheck instanceof Hoe) {
            if (mineManager.tryStrikeRock(targetX, targetY, playerX + 22.0, playerY + 52.0,
                    player, particleSystem, this::spawnFloatingText, this::notify)) {
                updateSurvivalHud(animTimeSec);
                triggerToolAnimation("HOE", -1, -1);
                rootPane.requestFocus();
                return;
            }
            Cell hoeCell = getFacingCell();
            if (!isCellInFarmZone(hoeCell)) {
                notify("Chỉ có thể dùng cuốc trong khu trồng trọt (Farming Zone)!");
                rootPane.requestFocus();
                return;
            }
            if (hoeCell != null && hoeCell.getTerrain() != TerrainType.DIRT) {
                notify("Đây là lối đi cỏ xanh — hãy cuốc trên các luống đất 2x2!");
                rootPane.requestFocus();
                return;
            }
            if (tryClearWheatAtCell(hoeCell)) {
                applyLaborSurvivalCost();
                double hwx = hoeCell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                double hwy = hoeCell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                triggerToolAnimation("HOE", hwx, hwy);
                rootPane.requestFocus();
                return;
            }
        }

        // Thao tác với thú nuôi (Bán khi đủ lớn hoặc Cho ăn)
        if (tryInteractWithAnimal(hitBox)) {
            return;
        }

        Cell targetCell = getFacingCell();
        if (targetCell != null) {
            if (isCellBlockedByStructure(targetCell)) {
                notify("Vướng công trình, không thể thao tác tại đây!");
                rootPane.requestFocus();
                return;
            }
            // Thử thả thú nuôi từ kho đồ nếu đang bấm vào bãi cỏ trống
            if (playerController.getCurrentTool().getName().equals("Hand") && targetCell.getTerrain() == TerrainType.GRASS && targetCell.getCrop() == null) {
                model.game_item.Item babyCow = player.getInventory().findByName("Baby Cow");
                model.game_item.Item babyPig = player.getInventory().findByName("Baby Pig");
                model.game_item.Item babyChicken = player.getInventory().findByName("Baby Chicken");

                
                if (babyCow != null && player.getInventory().getItemCount(babyCow) >= 1) {
                    player.getInventory().removeItem(babyCow, 1);
                    Animal a = new Animal(targetX, targetY, 1);
                    animals.add(a);
                    if (entityPane != null) entityPane.getChildren().add(a.view);
                    notify("Đã thả Bò con! (Cho ăn mỗi ngày để lớn)");
                    rootPane.requestFocus();
                    return;
                } else if (babyPig != null && player.getInventory().getItemCount(babyPig) >= 1) {
                    player.getInventory().removeItem(babyPig, 1);
                    Animal a = new Animal(targetX, targetY, 3);
                    animals.add(a);
                    if (entityPane != null) entityPane.getChildren().add(a.view);
                    notify("Đã thả Lợn con! (Cho ăn mỗi ngày để lớn)");
                    rootPane.requestFocus();
                    return;
                } else if (babyChicken != null && player.getInventory().getItemCount(babyChicken) >= 1) {
                    player.getInventory().removeItem(babyChicken, 1);
                    Animal a = new Animal(targetX, targetY, 0);
                    animals.add(a);
                    if (entityPane != null) entityPane.getChildren().add(a.view);
                    notify("Đã thả Gà con! (Cho ăn mỗi ngày để lớn)");
                    rootPane.requestFocus();
                    return;
                }
            }

            boolean ok = interactCellWithEffects(targetCell);
            if (ok) {
                notify("Thao tác thành công!");
            } else if (playerController.getCurrentTool() instanceof SeedBag && targetCell.getCellState() != CellState.TILLED) {
                notify("Hãy dùng Cuốc (Hoe) cuốc đất trước khi trồng cây!");
                spawnFloatingText("Cần cuốc đất trước!", Color.rgb(255, 208, 92));
            } else {
                notify("Không thể làm điều này ở đây");
            }
            revertSeedToolIfEmpty();
            
            // Tối ưu hóa: Chỉ cập nhật ô đất vừa thao tác, tránh gây giật khung hình
            int row = targetCell.getY();
            int col = targetCell.getX();
            if (tiles != null && row < tiles.length && col < tiles[0].length) {
                tiles[row][col].style();
            }
            
            updateHover(targetCell);
        } else {
            notify("Hướng này không có đất trống!");
        }
        
        // Luôn lấy lại focus cho rootPane để không bị liệt phím di chuyển
        rootPane.requestFocus();
    }

    /**
     * Thực thi tương tác công cụ trên ô đất kèm đầy đủ hiệu ứng Phase 1 (Particle + Drop Bounce + Magnet Pickup).
     */
    private boolean interactCellWithEffects(Cell cell) {
        if (cell == null) return false;
        ToolStrategy curTool = playerController.getCurrentTool();
        double wx = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
        double wy = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;

        // 1. Thu hoạch cây trồng bằng Tay:
        //    - Cây ăn quả thân cao (PerennialFruitTree: Táo, Cam, Dừa...) -> PICKING_HIGH + rung tán cây ở frame 2-3
        //    - Rau/cây trồng thấp dưới đất -> PICKING_LOW
        //    - Cộng vật phẩm vào Inventory & hiện chữ nổi "+1 ..." đúng frame 2
        if (curTool instanceof Hand && cell.getPest() == null && cell.getCrop() != null && cell.getCrop().isHarvestable()) {
            boolean isBigTree = (cell.getCrop() instanceof model.crop.PerennialFruitTree);
            int row = cell.getY();
            int col = cell.getX();
            ImageView treeToShake = (isBigTree && tiles != null && row >= 0 && row < tiles.length && col >= 0 && col < tiles[0].length)
                    ? tiles[row][col].entityTreeView
                    : null;
            boolean started = startPickAnimation(isBigTree, wx, wy, treeToShake, () -> {
                model.crop.Crop harvested = cell.harvestCrop();
                if (harvested == null) return;
                model.game_item.HarvestedCropItem item = new model.game_item.HarvestedCropItem(
                    harvested.getName(), harvested.getCropData().getValue()
                );
                if (particleSystem != null) {
                    particleSystem.spawnHarvestSparkles(wx, wy);
                }
                view.fx.SoundHook.playHarvest();
                player.getInventory().addItem(item, 1);
                spawnFloatingText("+1 " + item.getName(), Color.rgb(166, 255, 146));
                onQuestCropHarvested(harvested.getName());
                // Nguồn phụ chống thiếu củi: thu hoạch Ngô (Corn) hoặc Lúa nước (WaterRice) tặng kèm +1 Củi khô (rơm/thân khô)
                if (harvested instanceof model.crop.Corn || harvested instanceof model.crop.WaterRice) {
                    model.game_item.ForageItem strawWood = model.game_item.ForageItem.firewood();
                    player.getInventory().addItem(strawWood, 1);
                    spawnFloatingText("+1 Củi khô (rơm/thân khô)", Color.rgb(255, 224, 118));
                }
                if (tiles != null && row >= 0 && row < tiles.length && col >= 0 && col < tiles[0].length) {
                    tiles[row][col].style();
                }
                triggerAutosave();
            });
            return started;
        }

        // 2. Các công cụ khác (Tưới nước, Cuốc, Hạt giống, Phân bón, Bắt sâu)
        boolean ok = playerController.interactWithCell(cell);
        if (curTool instanceof Hoe || curTool instanceof WateringCan) {
            applyLaborSurvivalCost();
        }
        if (ok) {
            if (curTool instanceof Hoe) {
                triggerToolAnimation("HOE", wx, wy);
            } else if (curTool instanceof WateringCan) {
                triggerToolAnimation("WATER", wx, wy);
                if (particleSystem != null) {
                    particleSystem.spawnWaterSplash(wx, wy);
                }
                view.fx.SoundHook.playWaterSplash();
            } else if (curTool instanceof SeedBag || curTool instanceof FertilizerBag || curTool instanceof FeedBag) {
                triggerToolAnimation("SPRINKLE", wx, wy);
            }
        } else if (curTool instanceof Hoe) {
            // Vẫn vung cuốc tạo cảm giác tương tác ngay cả khi đất chưa cần cuốc thêm (không bắn đất nếu thất bại)
            triggerToolAnimation("HOE", -1, -1);
        } else if (curTool instanceof WateringCan) {
            // Vẫn diễn hoạt ảnh tưới nước khi bấm Space cầm bình tưới
            triggerToolAnimation("WATER", -1, -1);
        }
        return ok;
    }

    /** Kích hoạt hoạt ảnh vung công cụ 3 nhịp tương ứng từng loại dụng cụ. */
    private void triggerToolAnimation(String kind, double targetWx, double targetWy) {
        if (pickAnimTimer >= 0.0) return; // Không cắt ngang hoạt ảnh nhặt/hái đang chạy
        toolActionKind = kind;
        toolActionTimer = 0.0;
        toolImpactFired = (targetWx < 0); // Không bắn hạt nếu không có ô mục tiêu hợp lệ
        toolTargetWorldX = targetWx;
        toolTargetWorldY = targetWy;
        if ("HOE".equals(kind) || "AXE".equals(kind)) {
            toolActionDuration = 0.32;      // 4 frame bổ cuốc / chặt rìu (0.08s/frame)
        } else if ("WATER".equals(kind)) {
            toolActionDuration = 0.40;      // 4 bước tưới nước (0.10s/frame): 0 -> 1 -> 2 -> 1 -> 0
        } else {
            toolActionDuration = 0.28;      // Lắc qua lại (-18° <-> +18°) rắc hạt / rải phân
        }
    }

    // ======================== HOẠT ẢNH THU HOẠCH (PICKING_LOW / PICKING_HIGH) ========================
    private static final double PICK_ANIM_DURATION_SEC = 0.48; // 4 frame x 0.12s/frame
    private static final double PICK_FRAME_SEC = 0.12;
    private static final double LUFFY_DISPLAY_W = 108.0;
    private static final double LUFFY_DISPLAY_H = 108.0;
    private static final double LUFFY_REF_H = 108.0;

    private double pickAnimTimer = -1.0;
    private boolean pickAnimHigh = false;
    private boolean pickRewardFired = false;
    private Runnable pendingPickReward = null;
    private ImageView pickShakeTreeView = null;

    /**
     * Khởi chạy hoạt ảnh thu hoạch 4 frame (0.48s = 0.12s/frame), khóa di chuyển, tự lật hướng theo mục tiêu
     * và thực thi cộng Inventory + chữ nổi "+1 ..." tại frame 2.
     */
    private boolean startPickAnimation(boolean isHigh, double targetWx, double targetWy, ImageView treeToShake, Runnable onFrame2Reward) {
        // Không xung đột với chặt cây (AXE), tưới nước (WATER), cuốc (HOE), nằm tắm nắng, ngồi bếp lửa hay đang chèo thuyền
        if (pickAnimTimer >= 0.0 || isSitting || isSunbathing || isOnBoat) {
            return false;
        }
        if (toolActionTimer >= 0.0 && ("AXE".equals(toolActionKind) || "WATER".equals(toolActionKind) || "HOE".equals(toolActionKind))) {
            return false;
        }
        Image[] frames = isHigh ? Assets.LUFFY_PICK_HIGH : Assets.LUFFY_PICK_LOW;
        if (frames == null || frames.length < 4 || frames[0] == null) {
            // Fallback an toàn nếu thiếu ảnh
            if (onFrame2Reward != null) onFrame2Reward.run();
            return true;
        }
        if (targetWx >= 0.0) {
            double pCx = playerX + 22.0;
            if (targetWx < pCx - 4.0) {
                facing = "LEFT";
            } else if (targetWx > pCx + 4.0) {
                facing = "RIGHT";
            }
        }
        pickAnimTimer = 0.0;
        pickAnimHigh = isHigh;
        pickRewardFired = false;
        pendingPickReward = onFrame2Reward;
        pickShakeTreeView = isHigh ? treeToShake : null;
        if (player != null) {
            player.setActionState(isHigh ? Player.ActionState.PICKING_HIGH : Player.ActionState.PICKING_LOW);
        }
        return true;
    }

    private void finishPickAnimation() {
        pickAnimTimer = -1.0;
        pickRewardFired = false;
        pendingPickReward = null;
        if (pickShakeTreeView != null) {
            pickShakeTreeView.setTranslateX(0.0);
            pickShakeTreeView = null;
        }
        if (player != null) {
            player.setActionState(Player.ActionState.IDLE);
        }
        if (luffySpriteView != null) {
            luffySpriteView.setFitWidth(LUFFY_DISPLAY_W);
            luffySpriteView.setFitHeight(LUFFY_DISPLAY_H);
            luffySpriteView.setX(-32.0);
            luffySpriteView.setY(-38.0);
            luffySpriteView.setTranslateY(0.0);
        }
    }

    // ======================== ZORO DIALOG ========================
    private void openZoroDialog() {
        if (router.hasOverlay()) return;
        NpcDialog dlg = new NpcDialog(router, "Zoro").onClosed(() -> rootPane.requestFocus());
        dlg.open("Chào bạn, bạn cần gì?", zoroMainOptions(dlg));
    }

    private NpcDialog.Option[] zoroMainOptions(NpcDialog dlg) {
        String hireLabel = "Thuê (" + (helperDaysLeft > 0 ? "Đang làm" : "1000$") + ")";
        return new NpcDialog.Option[] {
            new NpcDialog.Option("Nói chuyện", true, () -> {
                String[] lines = {
                    "Dạo này thời tiết thất thường quá nhỉ.",
                    "Cửa hàng của tôi luôn có những hạt giống tốt nhất!",
                    "Nếu bận quá, bạn có thể thuê tôi chăm sóc thú nuôi.",
                    "Nông trại của bạn trông có vẻ phát triển tốt đấy!"
                };
                dlg.setPage(lines[new java.util.Random().nextInt(lines.length)],
                        new NpcDialog.Option("Quay lại", true, () -> dlg.setPage("Bạn cần gì nữa không?", zoroMainOptions(dlg))),
                        new NpcDialog.Option("Rời đi", false, dlg::close));
            }),
            new NpcDialog.Option("Cửa hàng", false, () -> { dlg.close(); openShop(); }),
            new NpcDialog.Option(hireLabel, false, () -> {
                if (helperDaysLeft > 0) {
                    dlg.setPage("Tôi đang làm việc cho bạn rồi! (còn " + helperDaysLeft + " ngày)",
                            new NpcDialog.Option("Quay lại", true, () -> dlg.setPage("Bạn cần gì nữa không?", zoroMainOptions(dlg))));
                } else {
                    dlg.setPage("Ngươi muốn thuê ta cho thú ăn à?\nGiá: 1000$ cho 7 ngày. Đồng ý chứ?",
                            new NpcDialog.Option("Đồng ý", true, () -> {
                                if (player.deductMoney(1000)) {
                                    helperDaysLeft = 7;
                                    notify("Đã thuê Zoro 7 ngày!");
                                    dlg.setPage("Thỏa thuận xong! Cứ giao thú nuôi cho ta.",
                                            new NpcDialog.Option("Cảm ơn", true, dlg::close));
                                } else {
                                    dlg.setPage("Ngươi không đủ 1000$ rồi...",
                                            new NpcDialog.Option("Quay lại", true, () -> dlg.setPage("Bạn cần gì nữa không?", zoroMainOptions(dlg))));
                                }
                            }),
                            new NpcDialog.Option("Thôi", false, () -> dlg.setPage("Bạn cần gì nữa không?", zoroMainOptions(dlg))));
                }
            }),
            new NpcDialog.Option("Rời đi", false, dlg::close)
        };
    }

    // ======================== SAVE / LOAD (view state) ========================
    private Runnable autosave;
    private double periodicAutosaveTimerSec = 0.0;

    public void setAutosave(Runnable autosave) {
        this.autosave = autosave;
    }

    private void triggerAutosave() {
        if (autosave != null && gameManager != null && gameManager.getStatus() == controller.lifecyclemanager.GameStatus.RUNNING) {
            periodicAutosaveTimerSec = 0.0;
            autosave.run();
        }
    }

    private String sceneryKey(ImageView v) {
        return v.getId() + "@" + (int) v.getLayoutX() + "," + (int) v.getLayoutY();
    }

    /** Write everything the controller owns (position, clock, animals, cleared obstacles) into p. */
    public void exportViewState(java.util.Properties p) {
        if (droppedItemSystem != null) {
            droppedItemSystem.collectAllImmediately();
        }
        double saveX = (boatEntity != null && boatEntity.isLockedToBoat()) ? boatEntity.getDisembarkPlayerX() : playerX;
        double saveY = (boatEntity != null && boatEntity.isLockedToBoat()) ? boatEntity.getDisembarkPlayerY() : playerY;
        p.setProperty("view.playerX", Double.toString(saveX));
        p.setProperty("view.playerY", Double.toString(saveY));
        p.setProperty("view.facing", facing);
        p.setProperty("view.elapsed", Long.toString(elapsedTimeInDay));
        p.setProperty("view.clock24h", "true");
        p.setProperty("view.helperDays", Integer.toString(helperDaysLeft));
        p.setProperty("view.foodRareFish", Boolean.toString(foodRareFishBuff));
        p.setProperty("view.poolRelaxSec", Double.toString(poolRelaxSpeedBuffSec));
        p.setProperty("view.hungerGraceSec", Double.toString(hungerGraceTimerSec));
        if (player != null && player.getQuestManager() != null) {
            p.setProperty("quest.currentQuestIndex", Integer.toString(player.getQuestManager().getCurrentQuestIndex()));
            p.setProperty("quest.currentQuestProgress", Integer.toString(player.getQuestManager().getCurrentQuestProgress()));
        }

        p.setProperty("cleared.count", Integer.toString(clearedSceneryKeys.size()));
        int ci = 0;
        for (String key : clearedSceneryKeys) {
            p.setProperty("cleared." + (ci++), key);
        }

        p.setProperty("animal.count", Integer.toString(animals.size()));
        p.setProperty("trough.count", Integer.toString(troughs.size()));
        for (int i = 0; i < troughs.size(); i++) {
            p.setProperty("trough." + i, Integer.toString(troughs.get(i).amount));
        }
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);
            p.setProperty("animal." + i, a.type + "," + a.x + "," + a.y + "," + a.ageDays + "," + a.isFed);
        }

        // Lưu trạng thái các cây gỗ đã chặt & số ngày mọc lại qua SaveManager
        java.util.Map<String, int[]> treeStates = new java.util.LinkedHashMap<>();
        for (TreeObject t : choppableTrees) {
            if (t.chopped || t.hp < 4) {
                treeStates.put(t.key, new int[] { t.hp, t.regrowDays, t.chopped ? 1 : 0 });
            }
        }
        controller.lifecyclemanager.SaveManager.saveTreeStates(p, treeStates);

        // Lưu đầy đủ trạng thái Nhà Máy, Thị Trường Sốt Giá, Lễ Hội 7 Ngày, Mỏ Đá & Vòi Tưới, Vườn Cây Ăn Quả và Tim 6 NPC
        p.setProperty("orchard.unlocked", Boolean.toString(orchardUnlocked));
        factoryManager.save(p);
        marketManager.save(p);
        festivalManager.save(p);
        mineManager.save(p);
        if (npcManager != null) {
            npcManager.save(p);
        }
    }

    /** Restore the state written by {@link #exportViewState}. Call after setGame. */
    public void importViewState(java.util.Properties p) {
        suppressQuestMoneyTracking = true;
        try {
            playerX = Double.parseDouble(p.getProperty("view.playerX", Double.toString(playerX)));
            playerY = Double.parseDouble(p.getProperty("view.playerY", Double.toString(playerY)));
            facing = p.getProperty("view.facing", facing);
            long rawElapsed = Long.parseLong(p.getProperty("view.elapsed", Long.toString(MORNING_START_MS)));
            if (!"true".equals(p.getProperty("view.clock24h"))) {
                // Chuyển đổi mượt file save cũ (hệ 16 giờ 06:00 -> 22:00) sang đồng hồ 24 giờ mới
                double oldProgress = Math.max(0.0, Math.min(1.0, (double) rawElapsed / 405_000.0));
                double oldHour = 6.0 + oldProgress * 16.0;
                elapsedTimeInDay = Math.round((oldHour / 24.0) * DAY_DURATION_MS);
            } else {
                elapsedTimeInDay = rawElapsed;
            }
            if (elapsedTimeInDay < 0 || elapsedTimeInDay >= DAY_DURATION_MS) {
                elapsedTimeInDay = ((elapsedTimeInDay % DAY_DURATION_MS) + DAY_DURATION_MS) % DAY_DURATION_MS;
            }
            helperDaysLeft = Integer.parseInt(p.getProperty("view.helperDays", "0"));
            foodRareFishBuff = Boolean.parseBoolean(p.getProperty("view.foodRareFish", "false"));
            poolRelaxSpeedBuffSec = Double.parseDouble(p.getProperty("view.poolRelaxSec", "0.0"));
            hungerGraceTimerSec = Math.max(0.0, Math.min(HUNGER_FULL_GRACE_SEC,
                    Double.parseDouble(p.getProperty("view.hungerGraceSec", Double.toString(HUNGER_FULL_GRACE_SEC)))));
            if (player != null && player.getQuestManager() != null && p.getProperty("quest.currentQuestIndex") != null) {
                int qIdx = Integer.parseInt(p.getProperty("quest.currentQuestIndex", "1"));
                int qProg = Integer.parseInt(p.getProperty("quest.currentQuestProgress", "0"));
                player.getQuestManager().restoreState(qIdx, qProg);
            }
            if (fishShadows != null) fishShadows.setRareFishBuff(foodRareFishBuff);
            updateFoodBuffHud();
            lastRenderedHungerInt = -1;
            lastRenderedThirstInt = -1;
            updateSurvivalHud(animTimeSec);
            updateQuestTrackerUi();
            lastFrameTime = System.currentTimeMillis();
            double restoredProgress = (double) elapsedTimeInDay / DAY_DURATION_MS;
            currentTimeTint = getTimeOverlayColor(restoredProgress);
            if (timeOverlayRect != null) {
                timeOverlayRect.setFill(currentTimeTint);
            }
            lastDisplayedMinute = -1;
            updateTimeDisplay();
            resetFeedingSchedule(restoredProgress);
            int tc = Integer.parseInt(p.getProperty("trough.count", "0"));
            for (int i = 0; i < tc && i < troughs.size(); i++) {
                troughs.get(i).add(Integer.parseInt(p.getProperty("trough." + i, "0")));
            }
            if (playerCharacter != null) {
                playerCharacter.setTranslateX(playerX);
                playerCharacter.setTranslateY(playerY);
            }

            // cleared obstacles (chỉ xóa đúng những vật cản người chơi đã chủ động cuốc phá, KHÔNG xóa object mới của map)
            clearedSceneryKeys.clear();
            if (p.getProperty("cleared.count") != null) {
                int n = Integer.parseInt(p.getProperty("cleared.count"));
                for (int i = 0; i < n; i++) {
                    String k = p.getProperty("cleared." + i);
                    if (k != null && !k.isEmpty()) clearedSceneryKeys.add(k);
                }
                for (ImageView v : new java.util.ArrayList<>(sceneryObjects)) {
                    if (clearedSceneryKeys.contains(sceneryKey(v))) {
                        sceneryObjects.remove(v);
                        if (entityPane != null) entityPane.getChildren().remove(v);
                    }
                }
            }

            // Khôi phục trạng thái các cây gỗ đã chặt & tiến độ mọc lại từ SaveManager
            java.util.Map<String, int[]> savedTrees = controller.lifecyclemanager.SaveManager.loadTreeStates(p);
            if (!savedTrees.isEmpty()) {
                for (TreeObject t : choppableTrees) {
                    int[] st = savedTrees.get(t.key);
                    if (st != null) {
                        t.hp = st[0];
                        t.regrowDays = st[1];
                        t.chopped = (st[2] == 1);
                        if (t.chopped) {
                            applyStumpVisual(t);
                        }
                    }
                }
            }

            // Khôi phục Vườn Cây Ăn Quả, Nhà Máy, Thị Trường, Lễ Hội, Mỏ Đá / Vòi Tưới & Tim 6 NPC
            orchardUnlocked = Boolean.parseBoolean(p.getProperty("orchard.unlocked", "false"));
            if (orchardUnlocked) {
                applyOrchardUnlockedVisuals();
            }
            factoryManager.load(p);
            marketManager.load(p);
            festivalManager.load(p);
            mineManager.load(p, entityPane);
            if (npcManager != null) {
                npcManager.load(p);
            }
            syncAllNpcPerks();
            updateFestivalBannerUi();

            // animals
            if (p.getProperty("animal.count") != null) {
                for (Animal a : animals) {
                    if (entityPane != null) entityPane.getChildren().remove(a.view);
                }
                animals.clear();
                int n = Integer.parseInt(p.getProperty("animal.count"));
                for (int i = 0; i < n; i++) {
                    String[] f = p.getProperty("animal." + i).split(",");
                    Animal a = new Animal(Double.parseDouble(f[1]), Double.parseDouble(f[2]), Integer.parseInt(f[0]));
                    a.ageDays = Integer.parseInt(f[3]);
                    a.applySpriteScale();
                    if (Boolean.parseBoolean(f[4])) a.feed();
                    animals.add(a);
                    if (entityPane != null) entityPane.getChildren().add(a.view);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Could not fully restore view state: " + e);
        } finally {
            suppressQuestMoneyTracking = false;
        }
        refreshAll();
    }

    private Item findAvailableFeedItem(int animalType) {
        if (feedBag != null && feedBag.getSelectedItem() != null) {
            return feedBag.getSelectedItem();
        }
        String preferredName = animalType == 0 ? "Chicken Feed" : (animalType == 1 ? "Cow Feed" : "Pig Feed");
        Item preferred = player.getInventory().findByName(preferredName);
        if (preferred != null && player.getInventory().getItemCount(preferred) >= 1) {
            return preferred;
        }
        Item general = player.getInventory().findByName("Animal Feed");
        if (general != null && player.getInventory().getItemCount(general) >= 1) {
            return general;
        }
        for (Item item : player.getInventory().getItems().keySet()) {
            if (item instanceof model.game_item.AnimalFeedItem && player.getInventory().getItemCount(item) >= 1) {
                return item;
            }
        }
        return null;
    }

    private boolean tryInteractWithAnimal(javafx.geometry.Rectangle2D hitBox) {
        ToolStrategy curTool = playerController.getCurrentTool();
        if (curTool == null || (!(curTool instanceof Hand) && !(curTool instanceof FeedBag))) {
            return false;
        }

        // 1. Kiểm tra thu hoạch thú vật đã trưởng thành (ageDays >= 5)
        if (curTool instanceof Hand) {
            Animal toHarvest = null;
            for (Animal a : animals) {
                if (a.ageDays >= 5 && a.type != 2) {
                    javafx.geometry.Rectangle2D aRect = new javafx.geometry.Rectangle2D(a.x - 15, a.y - 15, 60, 60);
                    if (hitBox.intersects(aRect)) {
                        toHarvest = a;
                        break;
                    }
                }
            }
            if (toHarvest != null) {
                animals.remove(toHarvest);
                if (entityPane != null) entityPane.getChildren().remove(toHarvest.view);
                int money = toHarvest.getSellPrice();
                String name = toHarvest.getAnimalName();
                player.addMoney(money);
                String soldTypeKey = (toHarvest.type == 0) ? "Egg" : (toHarvest.type == 1 ? "Milk" : "Truffle");
                onQuestAnimalHarvested(soldTypeKey);
                if (particleSystem != null) {
                    particleSystem.spawnHarvestSparkles(toHarvest.x + 18, toHarvest.y + 8);
                    particleSystem.spawnHeartBurst(toHarvest.x + 18, toHarvest.y - 8);
                }
                if (droppedItemSystem != null) {
                    droppedItemSystem.spawnFloatingText("+$" + money, toHarvest.x + 18, toHarvest.y - 10, Color.rgb(255, 232, 92));
                }
                view.fx.SoundHook.playHarvest();
                notify("💰 Đã bán " + name + " trưởng thành thành công: +" + money + "$!");
                rootPane.requestFocus();
                return true;
            }
        }

        // 2. Kiểm tra cho thú ăn hoặc vuốt ve thu sản phẩm nếu chưa trưởng thành (ageDays < 5)
        Animal toFeed = null;
        for (Animal a : animals) {
            if (a.ageDays < 5 && a.type != 2) {
                javafx.geometry.Rectangle2D aRect = new javafx.geometry.Rectangle2D(a.x - 18, a.y - 18, 66, 66);
                if (hitBox.intersects(aRect)) {
                    toFeed = a;
                    if (!a.isFed) break;
                }
            }
        }
        if (toFeed != null) {
            String aName = toFeed.getAnimalName();
            model.game_item.Item feedItem = findAvailableFeedItem(toFeed.type);
            if (!toFeed.isFed && feedItem != null && player.getInventory().getItemCount(feedItem) >= 1) {
                player.getInventory().removeItem(feedItem, 1);
                toFeed.feed();
                revertBagToolIfEmpty();
                if (particleSystem != null) {
                    particleSystem.spawnHeartBurst(toFeed.x + 18, toFeed.y - 6);
                }
                view.fx.SoundHook.playHeart();
                notify("Đã cho " + aName + " ăn " + feedItem.getName() + "! (Tiến độ: Ngày " + toFeed.ageDays + "/5 - Mai sẽ lớn thêm)");
            } else if (!toFeed.isFed) {
                model.game_item.Item rawCrop = null;
                for (model.game_item.Item item : player.getInventory().getItems().keySet()) {
                    if (item instanceof model.game_item.HarvestedCropItem && player.getInventory().getItemCount(item) > 0) {
                        rawCrop = item;
                        break;
                    }
                }
                if (rawCrop != null) {
                    player.getInventory().removeItem(rawCrop, 1);
                    toFeed.feed();
                    revertBagToolIfEmpty();
                    if (particleSystem != null) {
                        particleSystem.spawnHeartBurst(toFeed.x + 18, toFeed.y - 6);
                    }
                    view.fx.SoundHook.playHeart();
                    notify("Đã cho " + aName + " ăn bằng " + rawCrop.getName() + "! (Tiến độ: Ngày " + toFeed.ageDays + "/5)");
                } else {
                    // Vuốt ve thú khi không có thức ăn -> vẫn bật tim thân thiện
                    if (curTool instanceof Hand && particleSystem != null) {
                        particleSystem.spawnHeartBurst(toFeed.x + 18, toFeed.y - 6);
                        view.fx.SoundHook.playHeart();
                    }
                    notify(aName + " đang đói! Bấm vào Cặp (Bag - phím 6) hoặc vào Shop mua thức ăn nhé!");
                }
            } else {
                // Thú đã no: vuốt ve bật icon trái tim + rơi sản phẩm vật nuôi (1 lần/ngày) bật nảy & tự hút!
                if (particleSystem != null) {
                    particleSystem.spawnHeartBurst(toFeed.x + 18, toFeed.y - 6);
                }
                view.fx.SoundHook.playHeart();
                if (curTool instanceof Hand && !toFeed.producedToday) {
                    toFeed.producedToday = true;
                    final Animal harvestedAnimal = toFeed;
                    final String harvestedName = aName;
                    model.game_item.AnimalProductItem prod = model.game_item.AnimalProductItem.forAnimalType(harvestedAnimal.type);
                    boolean isLowPick = (harvestedAnimal.type == 0 || harvestedAnimal.type == 3); // Nhặt Trứng gà / Nấm Truffle dưới đất
                    if (isLowPick) {
                        startPickAnimation(false, harvestedAnimal.x + 18, harvestedAnimal.y + 12, null, () -> {
                            player.getInventory().addItem(prod, 1);
                            view.fx.SoundHook.playPickup();
                            spawnFloatingText("+1 " + prod.getName(), Color.rgb(166, 255, 146));
                            onQuestAnimalHarvested(prod.getName());
                            notify("♥ Đã nhặt " + prod.getName() + " từ " + harvestedName + "!");
                            triggerAutosave();
                        });
                    } else {
                        if (droppedItemSystem != null) {
                            droppedItemSystem.spawnDrop(prod, Assets.getShopIcon(prod), harvestedAnimal.x + 18, harvestedAnimal.y + 12, 1);
                        } else {
                            player.getInventory().addItem(prod, 1);
                        }
                        onQuestAnimalHarvested(prod.getName());
                        notify("♥ Đã vuốt ve " + harvestedName + " và thu được " + prod.getName() + "!");
                        triggerAutosave();
                    }
                } else {
                    notify("♥ " + aName + " rất vui! (Ngày " + toFeed.ageDays + "/5 - Còn " + (5 - toFeed.ageDays) + " ngày để bán)");
                }
            }
            rootPane.requestFocus();
            return true;
        }

        return false;
    }

    private long lastFrameTime = 0;
    private long lastAnimNanos = 0L;
    private double animTimeSec = 0.0;
    private long elapsedTimeInDay = MORNING_START_MS;

    private Color getTimeOverlayColor(double progress) {
        boolean fireworksNight = gameManager != null
                && festivalManager.getFestivalForDay(gameManager.getCurrentDay()) == model.festival.FestivalManager.FestivalType.FIREWORKS_NIGHT;
        Color nightColor = fireworksNight
                ? Color.rgb(10, 18, 48, 0.58)
                : Color.rgb(16, 26, 58, 0.50); // buổi tối xanh chàm dịu mắt, nổi rõ ánh đèn lồng & lửa trại
        Color morningFresh = Color.rgb(225, 244, 255, 0.08);
        Color clearDay = Color.rgb(255, 255, 255, 0.0);
        Color sunsetClear = Color.rgb(255, 196, 172, 0.0);
        Color sunsetColor = Color.rgb(255, 196, 172, 0.09); // hoàng hôn hồng nhạt dịu mắt, không ngả cam đậm

        // Đồng bộ chính xác theo đồng hồ 24 giờ liên tục (00:00 -> 24:00) cùng mốc bật/tắt đèn đường
        double hour = Math.max(0.0, Math.min(1.0, progress)) * 24.0;

        if (hour < 4.75) {
            // 00:00 - 04:45: Đêm khuya -> Rạng sáng (nối liền mượt từ 24:00)
            return nightColor;
        } else if (hour < 6.0) {
            // 04:45 - 06:00: Bình minh hửng sáng dần
            return nightColor.interpolate(morningFresh, (hour - 4.75) / 1.25);
        } else if (hour < 7.25) {
            // 06:00 - 07:15: Sáng sớm trong trẻo -> Ban ngày sáng rõ
            return morningFresh.interpolate(clearDay, (hour - 6.0) / 1.25);
        } else if (hour < 16.0) {
            // 07:15 - 16:00: Ban ngày tươi sáng hoàn toàn, không phủ cam
            return Color.TRANSPARENT;
        } else if (hour < 17.0) {
            // 16:00 - 17:00: Chiều muộn -> Hoàng hôn dịu nhẹ
            return sunsetClear.interpolate(sunsetColor, hour - 16.0);
        } else if (hour < 18.5) {
            // 17:00 - 18:30: Chập tối -> Đêm (đến 18:00 trời đã tối tương đương mức 19:00 cũ, khớp lúc bật đèn)
            return sunsetColor.interpolate(nightColor, (hour - 17.0) / 1.5);
        } else {
            // 18:30 - 24:00: Buổi tối / Ban đêm (nối liền sang 00:00 ngày hôm sau)
            return nightColor;
        }
    }

    private void startGameLoop() {
        lastFrameTime = System.currentTimeMillis();
        lastAnimNanos = 0L;
        elapsedTimeInDay = MORNING_START_MS;
        Platform.runLater(() -> rootPane.requestFocus());
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    // Dùng mốc nanosecond của AnimationTimer (khử hoàn toàn giật 15.6ms của currentTimeMillis trên Windows)
                    double dtSec = (lastAnimNanos == 0L)
                            ? 0.016
                            : Math.min(0.05, Math.max(0.001, (now - lastAnimNanos) / 1_000_000_000.0));
                    lastAnimNanos = now;
                    animTimeSec += dtSec;
                    long delta = (long) Math.round(dtSec * 1000.0);
                    lastFrameTime = System.currentTimeMillis();

                    if (rootPane.getParent() == null) {
                        return;
                    }
                    // Dây chuyền Nhà Máy luôn chạy ngầm theo thời gian thực ngay cả khi người chơi đang làm việc ngoài đồng (hoặc mở Sổ Quản Lý)
                    if (activeFactoryView == null && !sleepTransitionRunning) {
                        factoryManager.update(dtSec);
                    }
                    if (!router.hasOverlay() && !sleepTransitionRunning) {
                        updatePlayerPosition(dtSec, animTimeSec);
                        updateRainEffect(dtSec);
                        updateFestivalEffectsAndCheckReward(dtSec);

                        // Tự động lưu ngầm định kỳ mỗi 15 giây để luôn ghi nhớ Đói/Khát, hành trang và vị trí
                        periodicAutosaveTimerSec += dtSec;
                        if (periodicAutosaveTimerSec >= 15.0) {
                            triggerAutosave();
                        }

                        // Chuyển màu nền thời tiết mượt mà bằng nội suy GPU (không chạm CSS rootPane gây khựng)
                        if (weatherTintRect != null && !currentWeatherTint.equals(targetWeatherTint)) {
                            double blend = Math.min(1.0, dtSec * 4.5);
                            currentWeatherTint = currentWeatherTint.interpolate(targetWeatherTint, blend);
                            if (Math.abs(currentWeatherTint.getOpacity() - targetWeatherTint.getOpacity()) < 0.002
                                    && Math.abs(currentWeatherTint.getRed() - targetWeatherTint.getRed()) < 0.004) {
                                currentWeatherTint = targetWeatherTint;
                            }
                            weatherTintRect.setFill(currentWeatherTint);
                        }
                        
                        // Chu kỳ 24 giờ liên tục: nếu không ngủ, đồng hồ chạy qua 24:00 -> 00:00 sang ngày hôm sau tự nhiên, không skip giờ
                        elapsedTimeInDay += delta;
                        if (elapsedTimeInDay >= DAY_DURATION_MS) {
                            elapsedTimeInDay %= DAY_DURATION_MS;
                            advanceDayCore(false, false);
                        }
                        // Nội suy mượt màu đêm/bình minh/sáng/trưa/chiều/tối mỗi frame
                        if (timeOverlayRect != null) {
                            double progress = (double) elapsedTimeInDay / DAY_DURATION_MS;
                            Color targetTime = getTimeOverlayColor(progress);
                            currentTimeTint = currentTimeTint.interpolate(targetTime, Math.min(1.0, dtSec * 5.0));
                            timeOverlayRect.setFill(currentTimeTint);
                        }
                        updateTimeDisplay();
                        updateFeedingSchedule();
                        updateFlagsAnimation(dtSec);
                    }
                } catch (Exception e) {
                    System.err.println("EXCEPTION IN ANIMATION TIMER:");
                    e.printStackTrace();
                }
            }
        };
        gameLoop.start();
    }

    private int lastDisplayedMinute = -1;

    private void updateTimeDisplay() {
        if (dayField == null || gameManager == null) return;
        double progress = Math.min(0.999999, Math.max(0.0, (double) elapsedTimeInDay / DAY_DURATION_MS));
        // Đồng hồ 24 giờ liên tục từ 00:00 đến 23:50 (qua 24:00 tự nối sang 00:00 ngày mới)
        int totalMins = (int) (progress * 24 * 60);
        int hour = (totalMins / 60) % 24;
        int min = (totalMins % 60) / 10 * 10;
        int currentMinuteHash = gameManager.getCurrentDay() * 1440 + hour * 60 + min;
        if (currentMinuteHash != lastDisplayedMinute) {
            lastDisplayedMinute = currentMinuteHash;
            String timeStr = String.format("%02d:%02d", hour, min);
            dayField.setText("DAY " + gameManager.getCurrentDay() + "   " + timeStr);
        }
    }

    /** Trời mưa (RAINY) sẽ dập tắt đống lửa trại; trời nắng / khô ráo lửa sẽ cháy lại. */
    private boolean isCampfireExtinguished() {
        return gameManager != null
                && gameManager.getCurrentWeather() != null
                && gameManager.getCurrentWeather().getType() == model.weather.WeatherType.RAINY;
    }

    private void syncCampfireWeatherState() {
        boolean extinguished = isCampfireExtinguished();
        if (!animatedCampfires.isEmpty()) {
            javafx.scene.image.Image targetImg = (extinguished && Assets.CAMPFIRE_EXTINGUISHED != null)
                    ? Assets.CAMPFIRE_EXTINGUISHED
                    : ((Assets.CAMPFIRE_FRAMES != null && Assets.CAMPFIRE_FRAMES.length == 6)
                            ? Assets.CAMPFIRE_FRAMES[campfireAnimFrame]
                            : null);
            if (targetImg != null) {
                for (ImageView fireView : animatedCampfires) {
                    if (fireView.getImage() != targetImg) {
                        fireView.setImage(targetImg);
                    }
                }
            }
        }
        for (javafx.scene.shape.Circle glow : campfireGlows) {
            glow.setVisible(!extinguished);
        }
        if (extinguished && activeCookingOverlay != null) {
            closeCookingOverlay();
            notify("🌧 Trời đổ mưa, đống lửa trại đã bị dập tắt!");
        }
    }

    private void updateFlagsAnimation(double dtSec) {
        // 1. Hoạt ảnh Đống Lửa Trại: trời mưa bị dập tắt (hiện ảnh củi than ướt), trời nắng/khô cháy lại (6 frame, 0.12s/frame)
        if (!animatedCampfires.isEmpty() && Assets.CAMPFIRE_FRAMES != null && Assets.CAMPFIRE_FRAMES.length == 6) {
            if (isCampfireExtinguished()) {
                if (Assets.CAMPFIRE_EXTINGUISHED != null) {
                    for (ImageView fireView : animatedCampfires) {
                        if (fireView.getImage() != Assets.CAMPFIRE_EXTINGUISHED) {
                            fireView.setImage(Assets.CAMPFIRE_EXTINGUISHED);
                        }
                    }
                }
            } else {
                campfireAnimTimer += dtSec;
                boolean needRelight = false;
                for (ImageView fireView : animatedCampfires) {
                    if (fireView.getImage() == Assets.CAMPFIRE_EXTINGUISHED) {
                        needRelight = true;
                        break;
                    }
                }
                if (needRelight || campfireAnimTimer >= 0.12) {
                    if (campfireAnimTimer >= 0.12) {
                        campfireAnimTimer -= 0.12;
                        campfireAnimFrame = (campfireAnimFrame + 1) % 6;
                    }
                    javafx.scene.image.Image fireImg = Assets.CAMPFIRE_FRAMES[campfireAnimFrame];
                    if (fireImg != null) {
                        for (ImageView fireView : animatedCampfires) {
                            fireView.setImage(fireImg);
                        }
                    }
                }
            }
        }

        // 2. Hoạt ảnh Cờ bay theo gió (6 frame; trời nắng/khô 0.15s/frame, trời mưa/gió mạnh 0.09s/frame)
        if (animatedFlags != null && !animatedFlags.isEmpty()
                && Assets.FLAG_FRAMES != null && Assets.FLAG_FRAMES.length == 6) {
            boolean windyRain = gameManager != null
                    && gameManager.getCurrentWeather() != null
                    && gameManager.getCurrentWeather().getType() == model.weather.WeatherType.RAINY;
            double frameInterval = windyRain ? 0.09 : 0.15;
            flagAnimTimer += dtSec;
            if (flagAnimTimer >= frameInterval) {
                flagAnimTimer -= frameInterval;
                flagAnimFrame = (flagAnimFrame + 1) % 6;
                javafx.scene.image.Image flagImg = Assets.FLAG_FRAMES[flagAnimFrame];
                if (flagImg != null) {
                    for (ImageView flag : animatedFlags) {
                        flag.setImage(flagImg);
                    }
                }
            }
        }
    }

    private void updateRainEffect(double dtSec) {
        if (weatherOverlay == null) return;
        
        model.weather.WeatherType cur = gameManager.getCurrentWeather() != null
                ? gameManager.getCurrentWeather().getType() : null;
        boolean raining = (cur == model.weather.WeatherType.RAINY);

        // Chuyển cường độ mưa mượt mà (tăng dần khi bật mưa, tan dần khi tạnh mưa)
        double targetIntensity = raining ? 1.0 : 0.0;
        rainIntensity += (targetIntensity - rainIntensity) * Math.min(1.0, dtSec * 3.8);
        if (Math.abs(rainIntensity - targetIntensity) < 0.01) rainIntensity = targetIntensity;

        if (raining && rainIntensity > 0.05) {
            double w = Math.max(1200, rootPane.getWidth());
            rainSpawnCarry += (2.8 * rainIntensity) * (dtSec * 60.0);
            int toSpawn = Math.min(5, (int) rainSpawnCarry);
            rainSpawnCarry -= toSpawn;
            for (int i = 0; i < toSpawn; i++) {
                javafx.scene.shape.Line drop = new javafx.scene.shape.Line(0, 0, -4, 15);
                drop.setStroke(javafx.scene.paint.Color.rgb(188, 226, 255, 0.78));
                drop.setStrokeWidth(2.2);
                drop.setOpacity(rainIntensity);
                drop.setTranslateX(Math.random() * (w + 360));
                drop.setTranslateY(-24);
                drop.setUserData(16.0 + Math.random() * 11.0);
                weatherOverlay.getChildren().add(drop);
            }
        }
        
        if (weatherOverlay.getChildren().isEmpty()) return;

        double h = Math.max(800, rootPane.getHeight());
        double stepScale = dtSec * 60.0;
        java.util.List<javafx.scene.Node> toRemove = null;
        for (javafx.scene.Node node : weatherOverlay.getChildren()) {
            if (node instanceof javafx.scene.shape.Line) {
                javafx.scene.shape.Line drop = (javafx.scene.shape.Line) node;
                double speed = (double) drop.getUserData();
                drop.setTranslateY(drop.getTranslateY() + speed * stepScale);
                drop.setTranslateX(drop.getTranslateX() - speed * 0.28 * stepScale);
                if (!raining) {
                    drop.setOpacity(Math.max(0.0, drop.getOpacity() - dtSec * 2.8));
                }
                if (drop.getTranslateY() > h + 50 || drop.getOpacity() <= 0.02) {
                    if (toRemove == null) toRemove = new java.util.ArrayList<>();
                    toRemove.add(drop);
                }
            }
        }
        if (toRemove != null) {
            weatherOverlay.getChildren().removeAll(toRemove);
        }
    }

    private void updatePlayerPosition(double deltaRiverSec, double timeSec) {
        double mapW = farmMap.getWidth() * Constant.CELL_SIZE;
        double mapH = farmMap.getHeight() * Constant.CELL_SIZE;

        double dx = 0;
        double dy = 0;
        isWalking = false;

        // Đọc phím điều hướng NGAY TỪ ĐẦU trước mọi logic khóa đất liền để chèo thuyền A/D luôn mượt mà
        int dirX = 0;
        int dirY = 0;
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.UP))    dirY -= 1;
        if (activeKeys.contains(KeyCode.S) || activeKeys.contains(KeyCode.DOWN))  dirY += 1;
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT))  dirX -= 1;
        if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) dirX += 1;

        // Chèo thuyền dọc sông (bỏ qua hoàn toàn va chạm tile nước & khóa đất liền khi đang trên thuyền và không câu)
        boolean activelyFishing = (fishingGame != null && fishingGame.isActive());
        if (boatEntity != null && boatEntity.isPlayerAboard() && !boatEntity.isTransitioning() && !activelyFishing) {
            if (dirX != 0) {
                boatEntity.rowHorizontal(dirX, deltaRiverSec, mapW);
            }
        }

        // Cập nhật hệ thống con sông & Game Feel dùng dtSec nanosecond chuẩn 60 FPS
        if (riverFlow != null) {
            riverFlow.update(deltaRiverSec, timeSec);
        }
        if (fishShadows != null) {
            fishShadows.update(deltaRiverSec, timeSec);
        }
        if (boatEntity != null) {
            boatEntity.setSuppressPrompt(activelyFishing);
            boatEntity.update(timeSec, playerX, playerY);
            updateBoatHintBanner(boatEntity.isPlayerAboard() || boatEntity.canInteractFromShore(playerX, playerY));
        }
        if (fishingGame != null) {
            fishingGame.update(deltaRiverSec, timeSec);
        }
        if (particleSystem != null) {
            // Đống lửa trại sinh động: chỉ phát sáng & bắn tàn lửa khi lửa đang cháy (trời mưa sẽ bị dập tắt)
            if (!campfirePoints.isEmpty()) {
                boolean fireOut = isCampfireExtinguished();
                if (!fireOut) {
                    campfireEmberTimer += deltaRiverSec;
                    if (campfireEmberTimer >= 0.16) {
                        campfireEmberTimer = 0.0;
                        for (double[] cp : campfirePoints) {
                            particleSystem.spawnCampfireEmber(cp[0], cp[1]);
                        }
                    }
                }
                for (int i = 0; i < campfireGlows.size(); i++) {
                    javafx.scene.shape.Circle glow = campfireGlows.get(i);
                    glow.setVisible(!fireOut);
                    if (!fireOut) {
                        double pulse = 0.88 + 0.14 * Math.sin(timeSec * 6.8 + i * 1.3);
                        glow.setScaleX(pulse);
                        glow.setScaleY(pulse * 0.82);
                        glow.setOpacity(0.72 + 0.18 * Math.sin(timeSec * 9.2 + i));
                    }
                }
            }
            // Cột đèn đường bật sáng dần từ 18:00, sáng xuyên qua 00:00 đêm và tắt dần lúc bình minh (05:30 -> 06:00)
            if (!streetlampLights.isEmpty()) {
                double dayProgress = Math.min(1.0, Math.max(0.0, (double) elapsedTimeInDay / DAY_DURATION_MS));
                double exactHour = dayProgress * 24.0;
                double lampPower;
                if (exactHour >= 18.0) {
                    lampPower = Math.min(1.0, (exactHour - 18.0) / 0.35);
                } else if (exactHour < 5.5) {
                    lampPower = 1.0;
                } else if (exactHour < 6.0) {
                    lampPower = Math.max(0.0, (6.0 - exactHour) / 0.5);
                } else {
                    lampPower = 0.0;
                }
                boolean lit = lampPower > 0.005;
                for (int i = 0; i < streetlampLights.size(); i++) {
                    javafx.scene.Node[] parts = streetlampLights.get(i);
                    double flicker = 0.93 + 0.07 * Math.sin(timeSec * 4.6 + i * 1.7);
                    double scalePulse = 0.97 + 0.04 * Math.sin(timeSec * 3.2 + i * 1.1);
                    for (int k = 0; k < parts.length; k++) {
                        javafx.scene.Node n = parts[k];
                        n.setVisible(lit);
                        if (lit) {
                            n.setOpacity(lampPower * flicker);
                            if (k == 1) { // quầng sáng trên đầu đèn nhấp nháy nhẹ
                                n.setScaleX(scalePulse);
                                n.setScaleY(scalePulse);
                            }
                        }
                    }
                }
            }
            // Vùng chuyển khu vực tự nhiên ngay dưới vòm Cổng Chào Nông Trại Gỗ & Đá
            if (farmGatePortalBox != null) {
                double pxGate = playerX + 22.0;
                double pyGate = playerY + 52.0;
                boolean inGate = pxGate >= farmGatePortalBox[0] && pxGate <= farmGatePortalBox[2]
                        && pyGate >= farmGatePortalBox[1] && pyGate <= farmGatePortalBox[3];
                if (inGate && !playerInsideFarmGateZone) {
                    playerInsideFarmGateZone = true;
                    String msg = (facing.equals("LEFT") || facing.equals("DOWN"))
                            ? "🌾 Khu Vực Nông Trại — 12 Luống Đất Canh Tác"
                            : "🛤 Lối Ra Đường Lớn & Xưởng Chế Biến";
                    spawnFloatingText(msg, Color.rgb(254, 240, 138));
                } else if (!inGate && playerInsideFarmGateZone) {
                    playerInsideFarmGateZone = false;
                }
            }
            if (!campForageSpots.isEmpty()) {
                for (CampForageSpot spot : campForageSpots) {
                    spot.node.setTranslateY(Math.sin(timeSec * 3.1 + spot.bobPhase) * 2.6);
                }
            }
            mineManager.updateSprinklerEffects(deltaRiverSec, particleSystem);
            particleSystem.update(deltaRiverSec);
        }
        if (droppedItemSystem != null) {
            droppedItemSystem.update(deltaRiverSec, timeSec, playerX + 22.0, playerY + 38.0);
        }
        // Cập nhật 4 Bot đi dạo + Nami & Hancock ở bể bơi
        if (npcManager != null) {
            npcManager.update(deltaRiverSec, timeSec);
        }
        // Hiệu ứng mặt nước bể bơi gợn sóng lấp lánh nhẹ + biển hiệu đèn Casino nhấp nháy
        updatePoolAndCasinoVisuals(timeSec);

        // Đếm ngược hiệu ứng Buff "Thư giãn +20% tốc độ chạy" (2 phút) từ bể bơi
        if (poolRelaxSpeedBuffSec > 0.0) {
            poolRelaxSpeedBuffSec = Math.max(0.0, poolRelaxSpeedBuffSec - deltaRiverSec);
            int secShown = (int) Math.ceil(poolRelaxSpeedBuffSec);
            if (secShown != lastRenderedRelaxSec) {
                lastRenderedRelaxSec = secShown;
                updateFoodBuffHud();
            }
        }
        // Nhấp nhô nhẹ icon bình tưới nước xanh trên các ô cây trồng đang thiếu nước (O(số ô thiếu nước))
        if (!waterNeedTiles.isEmpty()) {
            for (Tile wt : waterNeedTiles) {
                wt.waterNeedIcon.setTranslateY(-8.0 + Math.sin(timeSec * 4.0 + wt.bobPhase) * 3.0);
            }
        }
        // Nhấp nháy nhẹ thanh vàng kim (#FFD700) & chấm sao vàng trên các ô cây chín 100% sẵn sàng thu hoạch
        if (!ripeCropTiles.isEmpty()) {
            for (Tile rt : ripeCropTiles) {
                double pulse = 0.62 + 0.38 * Math.abs(Math.sin(timeSec * 4.6 + rt.bobPhase));
                rt.growthBarFill.setOpacity(pulse);
                rt.ripeStar.setOpacity(0.55 + 0.45 * Math.abs(Math.sin(timeSec * 6.0 + rt.bobPhase)));
                double sc = 0.88 + 0.24 * Math.abs(Math.sin(timeSec * 5.0 + rt.bobPhase));
                rt.ripeStar.setScaleX(sc);
                rt.ripeStar.setScaleY(sc);
            }
        }

        boolean lockedOnBoat = false;
        if (boatEntity != null && boatEntity.isTransitioning()) {
            // Đang bước mượt lên hoặc rời thuyền
            boolean steppingOntoBoat = !boatEntity.isPlayerAboard();
            double[] pos = boatEntity.stepTransition();
            playerX = pos[0];
            playerY = pos[1];
            isWalking = true;
            facing = steppingOntoBoat ? "DOWN" : "UP";
            lockedOnBoat = true;
            isOnBoat = boatEntity.isPlayerAboard();
        } else if (boatEntity != null && boatEntity.isPlayerAboard()) {
            // Nhân vật đang đứng trên thuyền: đồng bộ 100% tọa độ X (theo chiều chèo) & Y (nhịp dập dềnh) + Camera tự bám theo
            isOnBoat = true;
            if (!activelyFishing && dirX == 0 && dirY < 0 && boatEntity.canToggleNow()) {
                if (isBoatCenterUnderBridge()) {
                    notify("Đang ở dưới gầm cầu — hãy chèo qua hẳn bên kia rồi lên bờ!");
                } else {
                    // Nhấn W / UP khi đang dừng thuyền để bước lên bờ bắc
                    boatEntity.startDisembarking();
                }
            } else {
                playerX = boatEntity.getBoardedPlayerX();
                playerY = boatEntity.getSyncedPlayerY();
                if (activelyFishing) facing = "DOWN";
                else if (dirX < 0)   facing = "DOWN_LEFT";
                else if (dirX > 0)   facing = "DOWN_RIGHT";
                else                 facing = "DOWN";
            }
            isWalking = false;
            lockedOnBoat = true;
        } else {
            isOnBoat = false;
            if (boatEntity != null && dirY > 0 && boatEntity.isAtDockEdgeForWalkBoard(playerX, playerY) && boatEntity.canToggleNow()) {
                // Đi thẳng xuống cuối cầu tàu -> tự động bước lên sàn sau của thuyền
                boatEntity.startBoarding(playerX, playerY);
                facing = "DOWN";
                isWalking = true;
                lockedOnBoat = true;
            }
        }

        // Tụt chỉ số Đói & Khát tự nhiên rất nhẹ: giữ no ~4 tiếng trong game mới bắt đầu tụt Đói, Khát chỉ tụt nhanh khi làm việc nhiều
        // Nếu đang nằm tắm nắng cạnh bể bơi: tự động hồi dần Đói/Khát & nạp Buff Thư giãn +20% tốc độ chạy (2 phút)!
        if (isSunbathing && (dirX != 0 || dirY != 0)) {
            stopSunbathing();
        }
        if (player != null) {
            if (isSunbathing) {
                // Đặc quyền Hancock >= 3 Tim: nằm thư giãn ở Bể Bơi hồi Đói/Khát nhanh gấp đôi!
                double hancockBoost = (npcManager != null && npcManager.hasPerkUnlocked("hancock")) ? 2.0 : 1.0;
                player.addHunger(6.5 * hancockBoost * deltaRiverSec);
                player.addThirst(8.0 * hancockBoost * deltaRiverSec);
                hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
                recentLaborStreak = 0;
                recentLaborDecaySec = 0.0;
                sunbatheSparkleTimer += deltaRiverSec;
                if (sunbatheSparkleTimer >= 0.85 && particleSystem != null) {
                    sunbatheSparkleTimer = 0.0;
                    particleSystem.spawnHeartBurst(playerX + 22.0, playerY + 12.0);
                }
                updateSurvivalHud(timeSec);
            } else {
                // Đặc quyền Chopper >= 3 Tim: giảm 25% tốc độ tụt Đói/Khát!
                double chopperDrainFactor = (npcManager != null && npcManager.hasPerkUnlocked("chopper")) ? 0.75 : 1.0;
                if (recentLaborDecaySec > 0.0) {
                    recentLaborDecaySec = Math.max(0.0, recentLaborDecaySec - deltaRiverSec);
                    if (recentLaborDecaySec <= 0.0) {
                        recentLaborStreak = 0;
                    }
                }
                if (hungerGraceTimerSec > 0.0) {
                    hungerGraceTimerSec = Math.max(0.0, hungerGraceTimerSec - deltaRiverSec);
                } else {
                    player.addHunger(-0.12 * chopperDrainFactor * deltaRiverSec);
                }
                WeatherType wType = (gameManager != null && gameManager.getCurrentWeather() != null)
                        ? gameManager.getCurrentWeather().getType()
                        : WeatherType.SUNNY;
                double thirstDrainRate = (wType == WeatherType.DROUGHT) ? 0.18 : (wType == WeatherType.RAINY ? 0.04 : 0.10);
                player.addThirst(-thirstDrainRate * chopperDrainFactor * deltaRiverSec);
                updateSurvivalHud(timeSec);
            }
        }

        if (!lockedOnBoat && !isSunbathing && !isSitting && pickAnimTimer < 0.0) {
            if (dirX != 0 || dirY != 0) {
                isWalking = true;
                // Chuẩn hóa tốc độ chạy chéo (8 hướng mượt mà) + Phạt kiệt sức (55%) + Buff Thư giãn bể bơi (+20%)
                // + Vật lý đi cầu cong thực tế: leo lên dốc cầu hơi nặng chân (~0.76x), đổ xuống dốc cầu bon hơn (~1.18x)
                double norm = (dirX != 0 && dirY != 0) ? 0.70710678 : 1.0;
                double exhaustionFactor = (player != null && player.isExhausted()) ? 0.55 : 1.0;
                double relaxFactor = (poolRelaxSpeedBuffSec > 0.0) ? 1.20 : 1.0;
                double bridgeSlopeFactor = 1.0;
                if (dirY != 0 && isPlayerOnBridge()) {
                    double uNow = Math.max(0.0, Math.min(1.0, ((playerY + 54.0) - bridgeDeckTopY) / Math.max(1.0, bridgeDeckBotY - bridgeDeckTopY)));
                    double slopeNow = Math.cos(uNow * Math.PI); // >0 ở dốc Bắc (leo lên khi đi Nam), <0 ở dốc Nam (đổ xuống khi đi Nam)
                    double slopeAlign = dirY * slopeNow;        // >0 = đang leo lên đỉnh cầu, <0 = đang đi xuống khỏi đỉnh cầu
                    bridgeSlopeFactor = (slopeAlign > 0.0)
                        ? (1.0 - 0.24 * slopeAlign)
                        : (1.0 + 0.18 * (-slopeAlign));
                }
                double effectiveSpeed = speed * exhaustionFactor * relaxFactor * bridgeSlopeFactor;
                dx = dirX * effectiveSpeed * norm;
                dy = dirY * effectiveSpeed * norm;

                if (dirY < 0 && dirX < 0)      facing = "UP_LEFT";
                else if (dirY < 0 && dirX > 0) facing = "UP_RIGHT";
                else if (dirY > 0 && dirX < 0) facing = "DOWN_LEFT";
                else if (dirY > 0 && dirX > 0) facing = "DOWN_RIGHT";
                else if (dirY < 0)             facing = "UP";
                else if (dirY > 0)             facing = "DOWN";
                else if (dirX < 0)             facing = "LEFT";
                else                           facing = "RIGHT";
            }

            double nextX = playerX + dx;
            double nextY = playerY + dy;

            // Giới hạn không cho nhân vật đi ra ngoài rìa bản đồ
            if (nextX < 0) nextX = 0;
            if (nextY < 0) nextY = 0;
            if (nextX > mapW - 42) nextX = mapW - 42;
            if (nextY > mapH - 68) nextY = mapH - 68;

            // Foot Hitbox thực tại đế dép dưới cùng của sprite Luffy (x + 12..32, y + 58..68; đáy dép đúng bằng y + 68)
            javafx.geometry.Rectangle2D oldFootBox = new javafx.geometry.Rectangle2D(playerX + 12.0, playerY + 58.0, 20.0, 10.0);
            javafx.geometry.Rectangle2D footBoxX   = new javafx.geometry.Rectangle2D(nextX + 12.0,   playerY + 58.0, 20.0, 10.0);
            javafx.geometry.Rectangle2D footBoxY   = new javafx.geometry.Rectangle2D(playerX + 12.0, nextY + 58.0,   20.0, 10.0);
            javafx.geometry.Rectangle2D footBoxXY  = new javafx.geometry.Rectangle2D(nextX + 12.0,   nextY + 58.0,   20.0, 10.0);

            // TMX Object Collision — vùng thân dưới (16x16) cho cây cối/công trình chung
            javafx.geometry.Rectangle2D oldBox = new javafx.geometry.Rectangle2D(playerX + 14, playerY + 46, 16, 16);
            javafx.geometry.Rectangle2D playerBoxX = new javafx.geometry.Rectangle2D(nextX + 14, playerY + 46, 16, 16);
            javafx.geometry.Rectangle2D playerBoxY = new javafx.geometry.Rectangle2D(playerX + 14, nextY + 46, 16, 16);
            javafx.geometry.Rectangle2D playerBoxXY = new javafx.geometry.Rectangle2D(nextX + 14, nextY + 46, 16, 16);
            boolean movingDiag = (dx != 0.0 && dy != 0.0);

            boolean colX_obj = false;
            boolean colY_obj = false;
            boolean colXY_obj = false;
            boolean onDockX  = (boatEntity != null && boatEntity.isOnWalkableDock(footBoxX));
            boolean onDockY  = (boatEntity != null && boatEntity.isOnWalkableDock(footBoxY));
            boolean onDockXY = (boatEntity != null && boatEntity.isOnWalkableDock(footBoxXY));
            javafx.geometry.Rectangle2D bridgeWalkableDeck = null;

            if (boatEntity != null) {
                if (boatEntity.collidesWithHull(playerBoxX) && !boatEntity.collidesWithHull(oldBox)) colX_obj = true;
                if (boatEntity.collidesWithHull(playerBoxY) && !boatEntity.collidesWithHull(oldBox)) colY_obj = true;
                if (movingDiag && boatEntity.collidesWithHull(playerBoxXY) && !boatEntity.collidesWithHull(oldBox)) colXY_obj = true;
            }

            if (troughs != null) {
                for (FeedTrough t : troughs) {
                    javafx.geometry.Rectangle2D troughBox = new javafx.geometry.Rectangle2D(t.x + 3.0, t.y + 4.0, 64.0, 20.0);
                    if (isBlockedByObstacle(oldBox, playerBoxX, troughBox)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, troughBox)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, troughBox)) colXY_obj = true;
                }
            }

            // Va chạm chuồng gà, lán lợn, nhà kho bò & hàng rào 3 khu trên nền animal_farm_bg.png
            // (chừa trống trục đường lát đá ở giữa và 3 cổng CHICKEN, PIG, COW)
            if (!animalFarmColliders.isEmpty()) {
                for (javafx.geometry.Rectangle2D afBox : animalFarmColliders) {
                    if (isBlockedByObstacle(oldBox, playerBoxX, afBox)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, afBox)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, afBox)) colXY_obj = true;
                }
            }

            for (ImageView objView : sceneryObjects) {
            if (!objView.isVisible()) continue;
            String objId = objView.getId();
            
            // Tính toán vị trí trực tiếp để tránh lag (KHÔNG dùng getBoundsInParent vì quá nặng khi gọi liên tục)
            double ox = objView.getLayoutX() + objView.getTranslateX();
            double oy = objView.getLayoutY() + objView.getTranslateY();
            double ow = objView.getFitWidth();
            double oh = objView.getFitHeight();

            if (ow <= 0 || oh <= 0) continue;
            
            if (objId != null) {
                String idLower = objId.toLowerCase();
                if (idLower.contains("bridge")) {
                    // Chi tiết hóa Collision CHÂN CỘT CẦU & LAN CAN khớp chuẩn bridge.png (240x408):
                    // Chỉ đặt solid collider ở phần CHÂN CỘT (bệ đá / gốc gỗ dưới cùng) và thành lan can,
                    // để trống phần thân trên & đỉnh cột cho phép nhân vật đi vòng ra phía sau tự nhiên.
                    double leftRailX  = ox + ow * 0.172;
                    double leftRailW  = ow * 0.078;
                    double rightRailX = ox + ow * 0.750;
                    double rightRailW = ow * 0.078;

                    javafx.geometry.Rectangle2D[] bridgeBlockers = new javafx.geometry.Rectangle2D[] {
                        // 1. Hai thành lan can gỗ cong chạy dọc hai bên mặt cầu
                        new javafx.geometry.Rectangle2D(leftRailX,  oy + oh * 0.165, leftRailW,  oh * 0.755),
                        new javafx.geometry.Rectangle2D(rightRailX, oy + oh * 0.165, rightRailW, oh * 0.755),
                        // 2. Chân bệ đá của 2 cột lớn đầu cầu Bắc (y = 22.5%..29.9%, đỉnh cột phía trên không chặn)
                        new javafx.geometry.Rectangle2D(ox + ow * 0.112, oy + oh * 0.225, ow * 0.130, oh * 0.074),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.748, oy + oh * 0.225, ow * 0.130, oh * 0.074),
                        // 3. Chân 2 cột giữa thân cầu (y = 63.5%..70.6%)
                        new javafx.geometry.Rectangle2D(ox + ow * 0.132, oy + oh * 0.635, ow * 0.112, oh * 0.071),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.756, oy + oh * 0.635, ow * 0.112, oh * 0.071),
                        // 4. Chân gốc gỗ dưới cùng của cụm 4 cột đầu cầu Nam (y = 91.2%..97.5%, đỉnh cột phía trên không chặn)
                        new javafx.geometry.Rectangle2D(ox + ow * 0.018, oy + oh * 0.912, ow * 0.224, oh * 0.063),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.758, oy + oh * 0.912, ow * 0.224, oh * 0.063)
                    };

                    // Sàn ván gỗ đi được (lọt lòng giữa 2 thành cầu)
                    double deckLeft = ox + ow * 0.250;
                    double deckWidth = ow * 0.500;
                    bridgeWalkableDeck = new javafx.geometry.Rectangle2D(
                        deckLeft, oy + oh * 0.140, deckWidth, oh * 0.855
                    );

                    for (javafx.geometry.Rectangle2D blocker : bridgeBlockers) {
                        if (isBlockedByObstacle(oldFootBox, footBoxX, blocker)) colX_obj = true;
                        if (isBlockedByObstacle(oldFootBox, footBoxY, blocker)) colY_obj = true;
                        if (movingDiag && isBlockedByObstacle(oldFootBox, footBoxXY, blocker)) colXY_obj = true;
                    }
                    continue;
                }
                if (idLower.equals("farm_gate_side")) {
                    // Cổng xoay ngang lối vào khu vườn đầu tiên: chỉ chặn trụ đá trên (Bắc) & trụ đá dưới (Nam),
                    // để trống lối đi rộng ở giữa cho nhân vật đi xuyên qua dưới vòm và sau trụ cổng bên ngoài
                    javafx.geometry.Rectangle2D topPillar = new javafx.geometry.Rectangle2D(
                        ox + ow * 0.18, oy + oh * 0.14, ow * 0.56, oh * 0.30
                    );
                    javafx.geometry.Rectangle2D bottomPillar = new javafx.geometry.Rectangle2D(
                        ox + ow * 0.18, oy + oh * 0.87, ow * 0.56, oh * 0.13
                    );
                    if (isBlockedByObstacle(oldBox, playerBoxX, topPillar)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, topPillar)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, topPillar)) colXY_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxX, bottomPillar)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, bottomPillar)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, bottomPillar)) colXY_obj = true;
                    continue;
                }
                if (idLower.equals("farm_gate") || idLower.equals("rustic_farm_gate")) {
                    // Cổng Chào Nông Trại (FARM_GATE): chỉ chặn 2 trụ đá trái & phải, để trống lối đi giữa vòm cổng
                    javafx.geometry.Rectangle2D leftPillar  = new javafx.geometry.Rectangle2D(ox + 6.0, oy + oh - 34.0, 28.0, 30.0);
                    javafx.geometry.Rectangle2D rightPillar = new javafx.geometry.Rectangle2D(ox + ow - 34.0, oy + oh - 34.0, 28.0, 30.0);
                    if (isBlockedByObstacle(oldBox, playerBoxX, leftPillar)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, leftPillar)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, leftPillar)) colXY_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxX, rightPillar)) colX_obj = true;
                    if (isBlockedByObstacle(oldBox, playerBoxY, rightPillar)) colY_obj = true;
                    if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, rightPillar)) colXY_obj = true;
                    continue;
                }
                if (idLower.equals("log_diag")) {
                    // Khúc gỗ nằm chéo cạnh lửa trại:
                    // 1) Dùng 4 hộp nhỏ xếp khít dọc theo thân gỗ chéo (không hở lỗ, không phình ra khe hẹp hai bên)
                    double ax = ox + ow * 0.20, ay = oy + oh * 0.20;
                    double bx = ox + ow * 0.80, by = oy + oh * 0.80;
                    double bw = ow * 0.28, bh = oh * 0.26;
                    for (int k = 0; k < 4; k++) {
                        double t = k / 3.0;
                        javafx.geometry.Rectangle2D seg = new javafx.geometry.Rectangle2D(
                            ax + (bx - ax) * t - bw * 0.5,
                            ay + (by - ay) * t - bh * 0.5,
                            bw, bh
                        );
                        if (isBlockedByObstacle(oldBox, playerBoxX, seg)) colX_obj = true;
                        if (isBlockedByObstacle(oldBox, playerBoxY, seg)) colY_obj = true;
                        if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, seg)) colXY_obj = true;
                    }
                    // 2) Cập nhật chiều sâu 2.5D theo độ cao Y của đường chéo gỗ tại tọa độ X nhân vật,
                    // tránh lỗi khúc gỗ đè ngang đầu nhân vật khi đứng ở phía trước đầu trên khúc gỗ
                    double pCx = playerX + 22.0;
                    double xA = ox + ow * 0.16;
                    double xB = ox + ow * 0.84;
                    double tDiag = Math.max(0.0, Math.min(1.0, (pCx - xA) / Math.max(1.0, xB - xA)));
                    objView.setUserData((oy + oh * 0.22) + tDiag * (oh * 0.66));
                    continue;
                }
                // Chỉ bỏ qua va chạm với các decal phẳng sát mặt đất và ghế tắm nắng (KHÔNG bỏ qua tree_bush, bush, flag, wood_sticks, clay_pot, dirt_mound, pool_umbrella)
                if (idLower.isEmpty() || idLower.equals("river_north_abutment") || idLower.equals("farm_bed_frame") || idLower.startsWith("ground_") ||
                    idLower.startsWith("wildflowers") || idLower.contains("crop") ||
                    idLower.contains("pumpkin") || idLower.startsWith("dirt_spot") || idLower.contains("path") ||
                    idLower.contains("terrain") || idLower.contains("wheat") || idLower.contains("construction") ||
                    idLower.contains("farm_rake") || idLower.contains("farm_hoe") || idLower.contains("farm_shovel") ||
                    idLower.contains("flower") || idLower.contains("pebble") || idLower.contains("broken_") ||
                    idLower.contains("grass_patch") || idLower.contains("windmill_blades") ||
                    idLower.contains("pool_lounger")) continue;
            }

            // Điều chỉnh hitbox chuẩn xác theo từng loại vật thể để không bị đi xuyên qua
            String idLow = objId != null ? objId.toLowerCase() : "";
            double hw, hh, cx, cy;
            if (idLow.contains("swimming_pool")) {
                // Bể bơi: cho phép đi sát viền gạch trắng, chỉ chặn lòng hồ nước xanh bên trong
                hw = Math.max(40, ow - 38.0);
                hh = Math.max(30, oh - 38.0);
                cx = ox + 19.0;
                cy = oy + 19.0;
            } else if (idLow.contains("windmill")) {
                // Cối xay gió: phần thân tháp hẹp hơn khung trên
                hw = ow * 0.68;
                hh = oh * 0.44;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("house") || idLow.contains("barn") || idLow.contains("pet_shop") || idLow.contains("casino")) {
                // Công trình lớn (Nhà, Nhà kho, Pet Shop, Casino): chặn toàn bộ phần thân & tường nhà
                hw = ow * 0.88;
                hh = oh * 0.62;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("tent")) {
                // Lều trại: gọn vừa thân lều để đi qua được khe giữa các lều
                hw = ow * 0.78;
                hh = oh * 0.56;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.equals("campfire_off")) {
                hw = ow * 0.72;
                hh = oh * 0.52;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("campfire")) {
                // Lửa trại (sprite 112x112 có khoảng trống trên ngọn lửa): chặn gọn đúng vòng đá bếp lửa
                hw = ow * 0.44;
                hh = oh * 0.28;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 6;
            } else if (idLow.contains("fence_v") || idLow.contains("wire_fence_v")) {
                // Hàng rào dọc: gọn sát thân cọc gỗ
                hw = Math.max(12, ow * 0.65);
                hh = oh * 0.86;
                cx = ox + (ow - hw) / 2;
                cy = oy + (oh - hh) / 2;
            } else if (idLow.contains("fence")) {
                // Hàng rào ngang / xiên / rào dây ngang
                hw = ow * 0.90;
                hh = Math.max(14, oh * 0.58);
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh;
            } else if (idLow.contains("garden_bed")) {
                // Luống gỗ trồng bí ngô: chặn gọn khung gỗ
                hw = ow * 0.86;
                hh = oh * 0.82;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh;
            } else if (idLow.equals("tree_bush")) {
                // Cây lớn kèm bụi rậm gốc bên phải: chặn gọn gốc cây & bụi gắn liền, vẫn mở lối hẹp bên cạnh
                hw = ow * 0.54;
                hh = oh * 0.20;
                cx = ox + ow * 0.30;
                cy = oy + oh - hh - 4;
            } else if (idLow.equals("palm_tree")) {
                // Cây dừa thân cong lệch phải: chặn đúng gốc thân dừa
                hw = ow * 0.24;
                hh = oh * 0.20;
                cx = ox + ow * 0.50;
                cy = oy + oh - hh - 4;
            } else if (idLow.contains("tree")) {
                // Cây sồi / cây tối / cây nhỏ: chặn gọn phần gốc & rễ cây
                hw = ow * 0.34;
                hh = oh * 0.20;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 4;
            } else if (idLow.contains("bush")) {
                // Bụi rậm lớn: chặn lõi chân bụi để vẫn lách qua khe hẹp xung quanh
                hw = ow * 0.56;
                hh = oh * 0.38;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 4;
            } else if (idLow.equals("log")) {
                // Khúc gỗ nằm ngang cạnh lửa trại: ôm sát thân gỗ
                hw = ow * 0.82;
                hh = oh * 0.48;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 4;
            } else if (idLow.contains("rock") || idLow.contains("stump") || idLow.contains("crate") ||
                       idLow.contains("well") || idLow.contains("water_trough") || idLow.contains("wood_plank") ||
                       idLow.contains("wood_sticks") || idLow.contains("clay_pot") || idLow.contains("dirt_mound") ||
                       idLow.contains("log") || idLow.equals("mine_anvil")) {
                if (idLow.equals("mine_rock") && mineManager.isRockBrokenView(objView)) {
                    continue; // Tảng đá mỏ đã đập vỡ hôm nay -> cho đi qua tự do
                }
                // Đá, gốc cây, thùng gỗ, giếng, máng nước, đống đất, chậu gốm, rào gãy, đe rèn: gọn vừa thân để đi lách khe hẹp
                hw = ow * 0.66;
                hh = oh * 0.48;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 4;
            } else if (idLow.contains("npc")) {
                // NPC (Zoro): chặn phần chân nhân vật, vẫn đứng sát đối thoại được
                hw = ow * 0.50;
                hh = oh * 0.36;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("flag")) {
                // Cột cờ: chặn gọn phần chân cột gỗ
                hw = Math.max(12, ow * 0.26);
                hh = oh * 0.16;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("pool_umbrella")) {
                // Ô che nắng hồ bơi: chỉ chặn chân trụ giữa để vẫn đi qua bóng mát xung quanh được
                hw = Math.max(12, ow * 0.20);
                hh = oh * 0.18;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("streetlamp") || idLow.contains("lantern") || idLow.contains("sign")) {
                // Cột đèn, biển chỉ dẫn: chặn gọn phần chân cột đúng theo hướng quay
                if (idLow.endsWith("_left")) {
                    hw = Math.max(12, ow * 0.30);
                    cx = ox + ow * 0.68 - hw * 0.5;
                } else if (idLow.endsWith("_down")) {
                    hw = Math.max(12, ow * 0.30);
                    cx = ox + ow * 0.48 - hw * 0.5;
                } else {
                    hw = Math.max(12, ow * 0.34);
                    cx = ox + (ow - hw) / 2;
                }
                hh = oh * 0.20;
                cy = oy + oh - hh - 2;
            } else {
                // Vật thể khác
                hw = Math.min(40, ow * 0.65);
                hh = Math.min(30, oh * 0.45);
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            }
            
            javafx.geometry.Rectangle2D objBox = new javafx.geometry.Rectangle2D(cx, cy, hw, hh);
            
            if (isBlockedByObstacle(oldBox, playerBoxX, objBox)) colX_obj = true;
            if (isBlockedByObstacle(oldBox, playerBoxY, objBox)) colY_obj = true;
            if (movingDiag && isBlockedByObstacle(oldBox, playerBoxXY, objBox)) colXY_obj = true;
        }
        
            boolean waterX = !onDockX
                    && isFootBoxBlockedByRiverOrCliff(footBoxX, bridgeWalkableDeck)
                    && !isFootBoxBlockedByRiverOrCliff(oldFootBox, bridgeWalkableDeck);
            boolean waterY = !onDockY
                    && isFootBoxBlockedByRiverOrCliff(footBoxY, bridgeWalkableDeck)
                    && !isFootBoxBlockedByRiverOrCliff(oldFootBox, bridgeWalkableDeck);

            if (waterX) nextX = playerX;
            if (waterY) nextY = playerY;
            
            if (colX_obj) nextX = playerX;
            if (colY_obj) nextY = playerY;
            // Chặn đi chéo lọt vào góc vật cản khi từng trục X/Y riêng lẻ chưa chạm
            if (!colX_obj && !colY_obj && colXY_obj) {
                if (Math.abs(dx) >= Math.abs(dy)) nextY = playerY;
                else nextX = playerX;
            }
            // Chặn đi chéo lọt vào vách đất bờ sông hoặc mặt nước
            if (nextX != playerX && nextY != playerY && !onDockXY) {
                if (isFootBoxBlockedByRiverOrCliff(footBoxXY, bridgeWalkableDeck)
                        && !isFootBoxBlockedByRiverOrCliff(oldFootBox, bridgeWalkableDeck)) {
                    if (Math.abs(dx) >= Math.abs(dy)) nextY = playerY;
                    else nextX = playerX;
                }
            }

            playerX = nextX;
            playerY = nextY;
        }

        // Ẩn bóng chân mặt đất & dụng cụ đất liền khi đang ở trong khoang thuyền, nằm tắm nắng hoặc ngồi bếp lửa
        if (luffyShadow != null) {
            luffyShadow.setVisible(!lockedOnBoat && !isSunbathing);
        }
        if (weaponView != null) {
            weaponView.setVisible(!lockedOnBoat && !isSunbathing && !isSitting && weaponView.getImage() != null);
        }

        // Xác định bộ sprite & hướng lật cho đủ 8 hướng chạy/đứng của Luffy
        String dirKey;
        switch (facing) {
            case "UP":         dirKey = "up";         luffyFacingLeft = false; break;
            case "DOWN":       dirKey = "down";       luffyFacingLeft = false; break;
            case "LEFT":       dirKey = "side";       luffyFacingLeft = true;  break;
            case "RIGHT":      dirKey = "side";       luffyFacingLeft = false; break;
            case "UP_LEFT":    dirKey = "up_right";   luffyFacingLeft = true;  break;
            case "UP_RIGHT":   dirKey = "up_right";   luffyFacingLeft = false; break;
            case "DOWN_LEFT":  dirKey = "down_right"; luffyFacingLeft = true;  break;
            case "DOWN_RIGHT": dirKey = "down_right"; luffyFacingLeft = false; break;
            default:           dirKey = "down";       luffyFacingLeft = false; break;
        }
        playerCharacter.setScaleX(luffyFacingLeft ? -1 : 1);
        if (luffyToolFlip != null) {
            luffyToolFlip.setX(luffyFacingLeft ? -1.0 : 1.0);
        }

        // Sprite Animation đầy đủ (Đứng yên + Chạy bộ + Làm việc) cho Cuốc (Hoe), Rìu (Axe) và Bình tưới (WateringCan)
        ToolStrategy activeTool = (playerController != null) ? playerController.getCurrentTool() : null;
        boolean hasHoeFrames = (Assets.LUFFY_HOE_FRAMES != null && Assets.LUFFY_HOE_FRAMES.length == 4 && Assets.LUFFY_HOE_FRAMES[0] != null);
        boolean hasHoeRunFrames = (Assets.LUFFY_HOE_RUN_FRAMES != null && Assets.LUFFY_HOE_RUN_FRAMES.length == 4 && Assets.LUFFY_HOE_RUN_FRAMES[0] != null);
        boolean hasAxeFrames = (Assets.LUFFY_AXE_FRAMES != null && Assets.LUFFY_AXE_FRAMES.length == 4 && Assets.LUFFY_AXE_FRAMES[0] != null);
        boolean hasWaterFrames = (Assets.LUFFY_WATER_FRAMES != null && Assets.LUFFY_WATER_FRAMES.length == 3 && Assets.LUFFY_WATER_FRAMES[0] != null);
        boolean hasWaterRunFrames = (Assets.LUFFY_WATER_RUN_FRAMES != null && Assets.LUFFY_WATER_RUN_FRAMES.length == 4 && Assets.LUFFY_WATER_RUN_FRAMES[0] != null);
        boolean usingNativeHoe = !lockedOnBoat && !isSitting && (activeTool instanceof Hoe) && hasHoeFrames;
        boolean usingNativeAxe = !lockedOnBoat && !isSitting && (activeTool instanceof Axe) && hasAxeFrames;
        boolean usingNativeWater = !lockedOnBoat && !isSitting && (activeTool instanceof WateringCan) && hasWaterFrames;
        boolean usingNativeActionTool = usingNativeHoe || usingNativeAxe || usingNativeWater;
        if (weaponView != null && usingNativeActionTool) {
            weaponView.setVisible(false);
        }

        // Cập nhật timer hoạt ảnh thu hoạch (PICKING_LOW / PICKING_HIGH — 4 frame x 0.12s = 0.48s)
        boolean pickAnimPlaying = false;
        double pickSpriteTranslateY = 0.0;
        if (pickAnimTimer >= 0.0) {
            pickAnimTimer += deltaRiverSec;
            if (pickAnimTimer >= PICK_ANIM_DURATION_SEC) {
                finishPickAnimation();
            } else {
                pickAnimPlaying = true;
            }
        }

        // Cập nhật timer hành động công cụ (khóa đổi frame chạy trong suốt thời gian bổ cuốc / chặt rìu / tưới nước)
        boolean nativeActionPlaying = false;
        if (!pickAnimPlaying && usingNativeActionTool && toolActionTimer >= 0.0) {
            toolActionTimer += deltaRiverSec;
            if (toolActionTimer >= toolActionDuration) {
                toolActionTimer = -1.0;
            } else {
                nativeActionPlaying = true;
            }
        }

        double bobbing = 0;
        if (luffySpriteView != null) {
            luffySpriteView.setSmooth(false);
            if (isSunbathing && Assets.LUFFY_SUNBATHE != null) {
                // Tư thế nằm tắm nắng dọc theo ghế bãi biển (đầu gác gối, mũ rơm đặt trên bụng)
                if (luffySpriteView.getFitWidth() != 49.0 || luffySpriteView.getFitHeight() != 111.0) {
                    luffySpriteView.setFitWidth(49.0);
                    luffySpriteView.setFitHeight(111.0);
                    luffySpriteView.setX(-2.5);
                    luffySpriteView.setY(-45.0);
                }
                luffySpriteView.setScaleX(1.0);
                luffySpriteView.setImage(Assets.LUFFY_SUNBATHE);
                bobbing = Math.sin(timeSec * 2.2) * 0.9;
            } else if (pickAnimPlaying) {
                // Hoạt ảnh thu hoạch 4 frame (0.12s/frame: 0 -> 1 -> 2 -> 3), khóa tỷ lệ & dính chân mặt đất
                int fIdx = Math.min(3, (int) (pickAnimTimer / PICK_FRAME_SEC));
                if (!pickRewardFired && fIdx >= 2) {
                    pickRewardFired = true;
                    if (pendingPickReward != null) {
                        Runnable reward = pendingPickReward;
                        pendingPickReward = null;
                        reward.run();
                    }
                }
                if (pickAnimHigh && pickShakeTreeView != null) {
                    if (fIdx >= 2) {
                        pickShakeTreeView.setTranslateX(Math.sin(pickAnimTimer * 52.0) * 4.2);
                    } else {
                        pickShakeTreeView.setTranslateX(0.0);
                    }
                }
                Image[] pickFrames = pickAnimHigh ? Assets.LUFFY_PICK_HIGH : Assets.LUFFY_PICK_LOW;
                Image pickImg = (pickFrames != null && pickFrames.length > fIdx) ? pickFrames[fIdx] : null;
                if (pickImg != null) {
                    luffySpriteView.setX(-32.0);
                    luffySpriteView.setY(-38.0);
                    if (pickAnimHigh) {
                        double zoom = LUFFY_DISPLAY_H / LUFFY_REF_H;
                        double fitH = pickImg.getHeight() * zoom;
                        luffySpriteView.setFitWidth(LUFFY_DISPLAY_W);
                        luffySpriteView.setFitHeight(fitH);
                        pickSpriteTranslateY = -(fitH - LUFFY_DISPLAY_H);
                    } else {
                        luffySpriteView.setFitWidth(LUFFY_DISPLAY_W);
                        luffySpriteView.setFitHeight(LUFFY_DISPLAY_H);
                        pickSpriteTranslateY = 0.0;
                    }
                    luffySpriteView.setImage(pickImg);
                }
                if (weaponView != null) weaponView.setVisible(false);
            } else if (usingNativeActionTool) {
                boolean isSideSheet = nativeActionPlaying
                        || (usingNativeAxe && !isWalking)
                        || (!usingNativeAxe && "side".equals(dirKey));
                if (isSideSheet) {
                    // Sheet 72x72 (ngang & bổ cuốc / chặt rìu / tưới nước): scale 120x120 cho chiều cao cơ thể ~90px khớp sprite 8 hướng
                    if (luffySpriteView.getFitWidth() != 120.0) {
                        luffySpriteView.setFitWidth(120.0);
                        luffySpriteView.setFitHeight(120.0);
                        luffySpriteView.setX(-38.0);
                        luffySpriteView.setY(-50.0);
                    }
                } else {
                    // Sprite 8 hướng (down / up / down_right / up_right hoặc chạy bộ cầm rìu): đúng kích thước 108x108 đồng bộ tuyệt đối
                    if (luffySpriteView.getFitWidth() != 108.0) {
                        luffySpriteView.setFitWidth(108.0);
                        luffySpriteView.setFitHeight(108.0);
                        luffySpriteView.setX(-32.0);
                        luffySpriteView.setY(-38.0);
                    }
                }

                if (usingNativeHoe) {
                    if (nativeActionPlaying && "HOE".equals(toolActionKind)) {
                        // Bấm Space đào đất: chạy tuần tự 0 -> 1 -> 2 -> 3 (0.08s/frame, tổng 0.32s), bắn hạt đất tại frame 2
                        int fIdx = Math.min(3, (int) (toolActionTimer / 0.08));
                        if (!toolImpactFired && fIdx >= 2) {
                            toolImpactFired = true;
                            if (particleSystem != null && toolTargetWorldX >= 0) {
                                particleSystem.spawnDirtClods(toolTargetWorldX, toolTargetWorldY);
                            }
                        }
                        luffySpriteView.setImage(Assets.LUFFY_HOE_FRAMES[fIdx]);
                    } else if (isWalking && hasHoeRunFrames) {
                        // Chạy đủ 8 hướng cầm cuốc (W/A/S/D + kết hợp chéo): vòng lặp 4 frame (0.10s/frame)
                        walkSwayTime += deltaRiverSec;
                        int runIdx = ((int) (walkSwayTime / 0.10)) % 4;
                        Image[] dirRun = (Assets.LUFFY_HOE_RUN_BY_DIR != null)
                                ? Assets.LUFFY_HOE_RUN_BY_DIR.getOrDefault(dirKey, Assets.LUFFY_HOE_RUN_FRAMES)
                                : Assets.LUFFY_HOE_RUN_FRAMES;
                        Image chosen = (dirRun != null && dirRun[runIdx] != null) ? dirRun[runIdx] : Assets.LUFFY_HOE_RUN_FRAMES[runIdx];
                        luffySpriteView.setImage(chosen);
                    } else {
                        // Đứng yên cầm cuốc đúng hướng vừa quay (đủ 8 hướng)
                        walkSwayTime = 0.0;
                        Image dirIdle = (Assets.LUFFY_HOE_IDLE_BY_DIR != null)
                                ? Assets.LUFFY_HOE_IDLE_BY_DIR.getOrDefault(dirKey, Assets.LUFFY_HOE_FRAMES[0])
                                : Assets.LUFFY_HOE_FRAMES[0];
                        luffySpriteView.setImage(dirIdle != null ? dirIdle : Assets.LUFFY_HOE_FRAMES[0]);
                    }
                } else if (usingNativeAxe) {
                    if (nativeActionPlaying && "AXE".equals(toolActionKind)) {
                        // Bấm Space chặt cây: chạy tuần tự 0 -> 1 -> 2 -> 3 (0.08s/frame, tổng 0.32s), khóa frame chạy, bắn vụn gỗ đúng frame 2
                        int fIdx = Math.min(3, (int) (toolActionTimer / 0.08));
                        if (!toolImpactFired && fIdx >= 2) {
                            toolImpactFired = true;
                            if (particleSystem != null && toolTargetWorldX >= 0) {
                                particleSystem.spawnWoodChipBurst(toolTargetWorldX, toolTargetWorldY - 16.0, pendingAxeChopFelled);
                            }
                        }
                        luffySpriteView.setImage(Assets.LUFFY_AXE_FRAMES[fIdx]);
                    } else if (isWalking && luffyRunByDir != null) {
                        // Đang chạy bộ cầm Rìu: giữ mượt animation chạy 8 hướng
                        Image[] arr = luffyRunByDir.getOrDefault(dirKey, luffyRunByDir.get("side"));
                        long nowAnim = System.currentTimeMillis();
                        if (isWalking != luffyWasWalking || !dirKey.equals(luffyLastDirKey)) {
                            luffyWasWalking = isWalking;
                            luffyLastDirKey = dirKey;
                            luffyAnimFrame = 0;
                            lastLuffyAnimTick = nowAnim;
                            if (arr != null && arr[0] != null) luffySpriteView.setImage(arr[0]);
                        } else if (arr != null && arr.length > 0 && nowAnim - lastLuffyAnimTick >= 95L) {
                            lastLuffyAnimTick = nowAnim;
                            luffyAnimFrame = (luffyAnimFrame + 1) % arr.length;
                            if (arr[luffyAnimFrame] != null) luffySpriteView.setImage(arr[luffyAnimFrame]);
                        }
                    } else {
                        // Đứng yên cầm Rìu: hiển thị LUFFY_AXE_FRAMES[3] sắc nét, đồng bộ hướng lật scaleX (phải=1, trái=-1)
                        walkSwayTime = 0.0;
                        luffyWasWalking = false;
                        luffySpriteView.setImage(Assets.LUFFY_AXE_FRAMES[3]);
                    }
                } else {
                    if (nativeActionPlaying && "WATER".equals(toolActionKind)) {
                        // Bấm Space tưới nước: chạy tuần tự 0 -> 1 -> 2 -> 1 (0.10s/frame, tổng 0.40s) rồi về 0
                        int step = Math.min(3, (int) (toolActionTimer / 0.10));
                        int fIdx = (step == 0) ? 0 : (step == 1 ? 1 : (step == 2 ? 2 : 1));
                        if (!toolImpactFired && fIdx >= 1) {
                            toolImpactFired = true;
                            if (particleSystem != null) {
                                double spoutX = playerX + 22.0 + (luffyFacingLeft ? -36.0 : 36.0);
                                double spoutY = playerY + 42.0;
                                particleSystem.spawnWaterSplash(spoutX, spoutY);
                            }
                        }
                        luffySpriteView.setImage(Assets.LUFFY_WATER_FRAMES[fIdx]);
                    } else if (isWalking && hasWaterRunFrames) {
                        // Chạy đủ 8 hướng cầm bình tưới (W/A/S/D + kết hợp chéo): vòng lặp 4 frame (0.10s/frame)
                        walkSwayTime += deltaRiverSec;
                        int runIdx = ((int) (walkSwayTime / 0.10)) % 4;
                        Image[] dirRun = (Assets.LUFFY_WATER_RUN_BY_DIR != null)
                                ? Assets.LUFFY_WATER_RUN_BY_DIR.getOrDefault(dirKey, Assets.LUFFY_WATER_RUN_FRAMES)
                                : Assets.LUFFY_WATER_RUN_FRAMES;
                        Image chosen = (dirRun != null && dirRun[runIdx] != null) ? dirRun[runIdx] : Assets.LUFFY_WATER_RUN_FRAMES[runIdx];
                        luffySpriteView.setImage(chosen);
                    } else {
                        // Đứng yên cầm bình tưới đúng hướng vừa quay (đủ 8 hướng)
                        walkSwayTime = 0.0;
                        Image dirIdle = (Assets.LUFFY_WATER_IDLE_BY_DIR != null)
                                ? Assets.LUFFY_WATER_IDLE_BY_DIR.getOrDefault(dirKey, Assets.LUFFY_WATER_FRAMES[0])
                                : Assets.LUFFY_WATER_FRAMES[0];
                        luffySpriteView.setImage(dirIdle != null ? dirIdle : Assets.LUFFY_WATER_FRAMES[0]);
                    }
                }
            } else if (luffyRunByDir != null && luffyIdleByDir != null) {
                // Công cụ khác (Hand, SeedBag, FertilizerBag, FeedBag) hoặc khi đang trên thuyền: giữ hoạt ảnh 8 hướng như cũ
                if (luffySpriteView.getFitWidth() != 108.0 || luffySpriteView.getFitHeight() != 108.0) {
                    luffySpriteView.setFitWidth(108.0);
                    luffySpriteView.setFitHeight(108.0);
                    luffySpriteView.setX(-32.0);
                    luffySpriteView.setY(-38.0);
                }
                Image[] arr = isWalking
                    ? luffyRunByDir.getOrDefault(dirKey, luffyRunByDir.get("side"))
                    : luffyIdleByDir.getOrDefault(dirKey, luffyIdleByDir.get("side"));
                long nowAnim = System.currentTimeMillis();
                if (isWalking != luffyWasWalking || !dirKey.equals(luffyLastDirKey)
                        || (luffySpriteView.getImage() != arr[luffyAnimFrame % arr.length] && !isWalking)) {
                    luffyWasWalking = isWalking;
                    luffyLastDirKey = dirKey;
                    luffyAnimFrame = 0;
                    lastLuffyAnimTick = nowAnim;
                    if (arr != null && arr[0] != null) luffySpriteView.setImage(arr[0]);
                } else if (arr != null && arr.length > 0) {
                    long walkInterval = 95L;
                    if (isWalking && dirY != 0 && isPlayerOnBridge()) {
                        double slopeAlign = dirY * bridgeArchSlope;
                        walkInterval = (slopeAlign > 0.15) ? 114L : ((slopeAlign < -0.15) ? 80L : 95L);
                    }
                    long interval = isWalking ? walkInterval : 380L;
                    if (nowAnim - lastLuffyAnimTick >= interval) {
                        lastLuffyAnimTick = nowAnim;
                        luffyAnimFrame = (luffyAnimFrame + 1) % arr.length;
                        if (arr[luffyAnimFrame] != null) {
                            luffySpriteView.setImage(arr[luffyAnimFrame]);
                        }
                    }
                }
            }
        } else if (isWalking) {
            lastMoveTime++;
            bobbing = Math.sin(lastMoveTime * 0.4) * 4.0;
        }

        // Hiệu ứng đi lên - đi xuống theo độ cong vòm cầu gỗ (độ cao 3D + tỉ lệ phối cảnh gần camera trên đỉnh cầu)
        double targetArchH = 0.0;
        double targetArchSlope = 0.0;
        if (!lockedOnBoat && !isSunbathing && isPlayerOnBridge()) {
            double uArch = Math.max(0.0, Math.min(1.0, ((playerY + 54.0) - bridgeDeckTopY) / Math.max(1.0, bridgeDeckBotY - bridgeDeckTopY)));
            targetArchH = Math.sin(uArch * Math.PI);
            targetArchSlope = Math.cos(uArch * Math.PI);
        }
        double archSmooth = Math.min(1.0, Math.max(0.18, deltaRiverSec * 14.0));
        bridgeArchHeightRatio += (targetArchH - bridgeArchHeightRatio) * archSmooth;
        if (bridgeArchHeightRatio < 0.002 && targetArchH == 0.0) {
            bridgeArchHeightRatio = 0.0;
        }
        bridgeArchSlope += (targetArchSlope - bridgeArchSlope) * archSmooth;
        double bridgeLiftY = -bridgeArchHeightRatio * 24.0;
        double bridgeScale = 1.0 + bridgeArchHeightRatio * 0.11;
        if (luffyBridgeScale != null) {
            luffyBridgeScale.setX(bridgeScale);
            luffyBridgeScale.setY(bridgeScale);
        }
        if (luffyShadow != null && !lockedOnBoat && !isSunbathing) {
            luffyShadow.setOpacity(1.0 - bridgeArchHeightRatio * 0.22);
        }

        // Cập nhật vị trí nhân vật trên bản đồ (Dùng setTranslate để tối ưu FPS, không làm lag 600 ô đất)
        playerCharacter.setTranslateX(playerX);
        playerCharacter.setTranslateY(playerY + bobbing + bridgeLiftY);
        updateNearbyInteractPrompt(lockedOnBoat);

        // Khi chọn Cuốc hoặc Bình tưới (đã có trọn bộ sprite Đứng + Chạy + Làm việc): ẨN HOÀN TOÀN weaponView / armGroup dán đè cũ!
        double bodyLeanAngle = 0.0;
        double bodyScaleY = 1.0;
        double bodyCrouchY = 0.0;

        if (luffyToolRig != null && armGroup != null && armRotate != null && weaponView != null && toolRotate != null) {
            boolean showArmTool = !lockedOnBoat && !usingNativeActionTool && weaponView.getImage() != null;
            luffyToolRig.setVisible(showArmTool);
            for (javafx.scene.Node child : armGroup.getChildren()) {
                child.setVisible(showArmTool);
            }

            if (showArmTool) {
                if (isWalking) {
                    walkSwayTime += deltaRiverSec;
                }

                ToolStrategy curTool = (playerController != null) ? playerController.getCurrentTool() : null;
                // Vị trí bàn tay nắm ở cuối cẳng tay bên trong armGroup (so với khớp vai (0, 0))
                double fistLocalX = 11.0;
                double fistLocalY = 18.0;
                double pivotX, pivotY;
                double restToolAngle;

                if (curTool instanceof Hoe || curTool instanceof Axe) {
                    pivotX = 8.0;
                    pivotY = 31.0;
                    restToolAngle = -6.0;
                } else if (curTool instanceof WateringCan) {
                    pivotX = 10.0;
                    pivotY = 25.0;
                    restToolAngle = 0.0;
                } else {
                    pivotX = 19.0;
                    pivotY = 31.0;
                    restToolAngle = 0.0;
                }

                // Gắn chặt chuôi công cụ / quai bình nước vào đúng tâm bàn tay nắm
                weaponView.setLayoutX(fistLocalX - pivotX);
                weaponView.setLayoutY(fistLocalY - pivotY);
                toolRotate.setPivotX(pivotX);
                toolRotate.setPivotY(pivotY);

                double shoulderAngle = 0.0;
                double wristAngle = restToolAngle;
                double breatheY = 0.0;

                if (toolActionTimer >= 0.0) {
                    toolActionTimer += deltaRiverSec;
                    double t = toolActionTimer;

                    if ("HOE".equals(toolActionKind) || "AXE".equals(toolActionKind)) {
                        // Đào đất / Chặt cây (0.07s + 0.10s + 0.12s): Giơ -65° sau vai -> Bổ mạnh +55° -> Thu mượt về
                        if (t < 0.07) {
                            double p = t / 0.07;
                            double ease = 1.0 - (1.0 - p) * (1.0 - p);
                            shoulderAngle = -65.0 * ease;
                            wristAngle = restToolAngle - 18.0 * ease;
                            bodyLeanAngle = -6.0 * ease;
                            bodyScaleY = 1.0 + 0.025 * ease;
                        } else if (t < 0.17) {
                            double p = (t - 0.07) / 0.10;
                            double ease = p * p;
                            shoulderAngle = -65.0 + (55.0 - (-65.0)) * ease;
                            wristAngle = (restToolAngle - 18.0) + 42.0 * ease;
                            bodyLeanAngle = -6.0 + 16.0 * ease;
                            bodyScaleY = 1.025 - 0.08 * ease;
                            bodyCrouchY = 3.0 * ease;
                            if (!toolImpactFired && p >= 0.78) {
                                toolImpactFired = true;
                                if ("HOE".equals(toolActionKind) && particleSystem != null && toolTargetWorldX >= 0) {
                                    particleSystem.spawnDirtClods(toolTargetWorldX, toolTargetWorldY);
                                }
                            }
                        } else if (t < toolActionDuration) {
                            double p = (t - 0.17) / (toolActionDuration - 0.17);
                            double ease = 1.0 - (1.0 - p) * (1.0 - p);
                            shoulderAngle = 55.0 * (1.0 - ease);
                            wristAngle = (restToolAngle + 24.0) + (restToolAngle - (restToolAngle + 24.0)) * ease;
                            bodyLeanAngle = 10.0 * (1.0 - ease);
                            bodyScaleY = 0.945 + 0.055 * ease;
                            bodyCrouchY = 3.0 * (1.0 - ease);
                        } else {
                            toolActionTimer = -1.0;
                        }
                    } else if ("WATER".equals(toolActionKind)) {
                        // Tưới nước (0.30s): Đưa cánh tay nâng bình ra trước + nghiêng cổ tay +35° + phun giọt nước từ đúng đầu vòi
                        double p = Math.min(1.0, t / toolActionDuration);
                        double curve = Math.sin(p * Math.PI);
                        shoulderAngle = -24.0 * curve;
                        wristAngle = restToolAngle + 35.0 * curve;
                        bodyLeanAngle = 5.0 * curve;
                        bodyCrouchY = 1.5 * curve;
                        if (!toolImpactFired && p >= 0.32) {
                            toolImpactFired = true;
                            if (particleSystem != null) {
                                // Tính đúng vị trí thế giới của đầu vòi bình nước khi đang nâng và nghiêng
                                double rad = Math.toRadians(shoulderAngle);
                                double rotFistX = fistLocalX * Math.cos(rad) - fistLocalY * Math.sin(rad);
                                double rotFistY = fistLocalX * Math.sin(rad) + fistLocalY * Math.cos(rad);
                                double localSpoutX = 29.0 + rotFistX + 22.0;
                                double localSpoutY = 9.0 + rotFistY + 2.0;
                                double worldSpoutX = playerX + (luffyFacingLeft ? (44.0 - localSpoutX) : localSpoutX);
                                double worldSpoutY = playerY + localSpoutY + bobbing;
                                particleSystem.spawnWaterSplash(worldSpoutX, worldSpoutY);
                            }
                        }
                        if (t >= toolActionDuration) {
                            toolActionTimer = -1.0;
                        }
                    } else {
                        // Túi hạt giống / phân bón: đưa tay ra trước lắc rắc hạt (-18° <-> +18°)
                        double p = Math.min(1.0, t / toolActionDuration);
                        double env = Math.sin(p * Math.PI);
                        shoulderAngle = -16.0 * env + Math.sin(p * Math.PI * 3.0) * 8.0;
                        wristAngle = restToolAngle + Math.sin(p * Math.PI * 3.0) * 18.0;
                        bodyLeanAngle = 4.0 * env;
                        if (t >= toolActionDuration) {
                            toolActionTimer = -1.0;
                        }
                    }
                } else {
                    // Đứng yên / đi bộ: cả armGroup (cánh tay + dụng cụ) đung đưa nhẹ theo nhịp bước chân!
                    if (isWalking) {
                        shoulderAngle = Math.sin(walkSwayTime * 10.0) * 6.0;
                        wristAngle = restToolAngle + Math.sin(walkSwayTime * 10.0) * 3.0;
                        breatheY = Math.abs(Math.sin(walkSwayTime * 10.0)) * 1.8;
                    } else {
                        shoulderAngle = Math.sin(timeSec * 3.0) * 1.5;
                        wristAngle = restToolAngle + Math.sin(timeSec * 3.0) * 1.0;
                        breatheY = Math.sin(timeSec * 3.0) * 1.5;
                    }
                }

                armRotate.setAngle(shoulderAngle);
                toolRotate.setAngle(wristAngle);

                // Đồng bộ vị trí khớp vai khi thân Luffy hơi cúi/ngả lúc làm việc
                double shoulderShiftX = Math.sin(Math.toRadians(bodyLeanAngle)) * 16.0;
                armGroup.setLayoutX(29.0 + shoulderShiftX);
                armGroup.setLayoutY(9.0 + bodyCrouchY + breatheY);

                if (luffyToolBridgeScale == null) {
                    luffyToolBridgeScale = new javafx.scene.transform.Scale(1.0, 1.0, 22.0, 66.0);
                    luffyToolRig.getTransforms().add(luffyToolBridgeScale);
                }
                luffyToolBridgeScale.setX(bridgeScale);
                luffyToolBridgeScale.setY(bridgeScale);
                luffyToolRig.setTranslateX(playerX);
                luffyToolRig.setTranslateY(playerY + bobbing + bridgeLiftY);
            }
        }

        // Bỏ hiệu ứng nghiêng/nhún giả lập khi dùng sprite hành động gốc (hoặc giữ thẳng ảnh nằm tắm nắng mới / tư thế ngồi cạnh bếp lửa / thu hoạch)
        if (luffyBodyRotate != null && luffySpriteView != null) {
            if (isSunbathing) {
                luffyBodyRotate.setAngle(Assets.LUFFY_SUNBATHE != null ? 0.0 : 72.0);
                luffySpriteView.setScaleY(1.0);
                luffySpriteView.setTranslateY(Assets.LUFFY_SUNBATHE != null ? 0.0 : -6.0);
                if (luffyToolRig != null) luffyToolRig.setVisible(false);
            } else if (isSitting) {
                luffyBodyRotate.setAngle(0.0);
                luffySpriteView.setScaleY(0.82);
                luffySpriteView.setTranslateY(11.0 + Math.sin(timeSec * 2.4) * 0.8);
                if (luffyToolRig != null) luffyToolRig.setVisible(false);
            } else if (pickAnimPlaying) {
                luffyBodyRotate.setAngle(0.0);
                luffySpriteView.setScaleY(1.0);
                luffySpriteView.setTranslateY(pickSpriteTranslateY);
                if (luffyToolRig != null) luffyToolRig.setVisible(false);
            } else {
                luffyBodyRotate.setAngle(usingNativeActionTool ? 0.0 : bodyLeanAngle);
                luffySpriteView.setScaleY(usingNativeActionTool ? 1.0 : bodyScaleY);
                luffySpriteView.setTranslateY(usingNativeActionTool ? 0.0 : bodyCrouchY);
            }
        }
        
        // ==========================================
        // CAMERA CHUYỂN ĐỘNG (Bí quyết của Soul Knight)
        // ==========================================
        // Lấy tâm màn hình
        double screenW = rootPane.getWidth();
        double screenH = rootPane.getHeight();
        if (screenW == 0) screenW = 1200; // Fallback
        if (screenH == 0) screenH = 800;  // Fallback
        
        double screenCenterX = screenW / 2.0; 
        double screenCenterY = screenH / 2.0;

        // Tính toán vị trí camera (muốn nhân vật ở giữa)
        double camX = playerX - screenCenterX + 20;
        double camY = playerY - screenCenterY + 20;

        // Giới hạn camera không cho nhìn ra ngoài map (khắc phục mảng trống)
        
        double maxCamX = Math.max(0, mapW - screenW);
        double maxCamY = Math.max(0, mapH - screenH);
        
        camX = Math.max(0, Math.min(camX, maxCamX));
        camY = Math.max(0, Math.min(camY, maxCamY));

        // Dịch chuyển ngược toàn bộ mặt đất theo camera
        mapPane.setTranslateX(-camX); 
        mapPane.setTranslateY(-camY);
        if (entityPane != null) {
            entityPane.setTranslateX(-camX);
            entityPane.setTranslateY(-camY);
        }
        
        // Cập nhật vị trí ô đang hướng tới (Highlight) để dễ thao tác (ẩn khi đang đứng trên thuyền)
        Cell fCell = getFacingCell();
        boolean hoeBlockedOutsideFarm = (playerController != null && playerController.getCurrentTool() instanceof Hoe
                && (!isCellInFarmZone(fCell) || fCell == null || fCell.getTerrain() != TerrainType.DIRT));
        if (!lockedOnBoat && fCell != null && facingHighlight != null && !isCellBlockedByStructure(fCell) && !hoeBlockedOutsideFarm) {
            facingHighlight.setVisible(true);
            facingHighlight.setTranslateX(fCell.getX() * Constant.CELL_SIZE);
            facingHighlight.setTranslateY(fCell.getY() * Constant.CELL_SIZE);
        } else if (facingHighlight != null) {
            facingHighlight.setVisible(false);
        }

        // Chỉ hiện bảng thông số đất (CELL, WATER, NUTR, CROP) khi nhân vật đứng bên trong Farming Zone
        updateSoilStatusPanelVisibility(lockedOnBoat, fCell);
        
        for (Animal animal : animals) {
            animal.update();
        }

        // Xoay cánh quạt cối xay gió mượt mà
        if (windmillBladeFrames != null && !windmillBladeViews.isEmpty()) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - lastWindmillTick >= 180) {
                lastWindmillTick = nowMs;
                windmillFrameIdx = (windmillFrameIdx + 1) % windmillBladeFrames.length;
                javafx.scene.image.Image fImg = windmillBladeFrames[windmillFrameIdx];
                if (fImg != null) {
                    for (ImageView bv : windmillBladeViews) {
                        bv.setImage(fImg);
                    }
                }
            }
        }
        
        // Cập nhật Y-coordinate (chiều sâu 2.5D) cho nhân vật và vũ khí
        // Khi trên thuyền (hoặc đang bước lên/xuống): đặt Z-order nằm GIỮA boatBackLayer và boatFrontLayer
        if (lockedOnBoat && boatEntity != null) {
            playerCharacter.setUserData(boatEntity.getBoardedPlayerSortY());
            if (luffyToolRig != null) {
                luffyToolRig.setUserData(boatEntity.getBoardedPlayerSortY() + 0.5);
            }
        } else {
            playerCharacter.setUserData(playerY + 50.0);
            if (luffyToolRig != null) {
                // Khi quay lưng (UP): đặt dụng cụ ra phía sau lưng; các hướng khác đặt ngay trên thân nhân vật
                luffyToolRig.setUserData("UP".equals(facing) ? (playerY + 49.0) : (playerY + 51.0));
            }
        }

        // Hiệu ứng thuyền & nhân vật chui lọt BÊN DƯỚI gầm cầu (mặt ván cầu che phía trên + bóng râm mượt)
        updateBoatUnderBridgeEffect(lockedOnBoat);
        
        // Sắp xếp động chiều sâu 2.5D — CHỈ sort khi thực sự có node đổi thứ tự (tránh phát event scene-graph mỗi frame)
        if (entityPane != null) {
            javafx.collections.ObservableList<javafx.scene.Node> children = entityPane.getChildren();
            boolean alreadySorted = true;
            double prevY = Double.NEGATIVE_INFINITY;
            for (int i = 0, sz = children.size(); i < sz; i++) {
                Object ud = children.get(i).getUserData();
                double curY = (ud instanceof Double) ? (Double) ud : 0.0;
                if (curY < prevY) {
                    alreadySorted = false;
                    break;
                }
                prevY = curY;
            }
            if (!alreadySorted) {
                javafx.collections.FXCollections.sort(children, (n1, n2) -> {
                    double y1 = n1.getUserData() instanceof Double ? (Double) n1.getUserData() : 0.0;
                    double y2 = n2.getUserData() instanceof Double ? (Double) n2.getUserData() : 0.0;
                    return Double.compare(y1, y2);
                });
            }
        }
    }

    /**
     * Kiểm tra bước di chuyển từ oldBox -> newBox có bị vật cản obstacle chặn không.
     * - Chưa đè trước đó: chặn tuyệt đối khi newBox chạm obstacle.
     * - Nếu vô tình đang đè sẵn (spawn/đứng dậy/tải save): chỉ cho phép di chuyển ra xa tâm vật cản (tự gỡ kẹt),
     *   chặn mọi bước đi sâu vào hoặc xuyên qua vật cản.
     */
    private boolean isBlockedByObstacle(javafx.geometry.Rectangle2D oldBox,
                                        javafx.geometry.Rectangle2D newBox,
                                        javafx.geometry.Rectangle2D obstacle) {
        if (!newBox.intersects(obstacle)) return false;
        if (!oldBox.intersects(obstacle)) return true;
        double ocx = obstacle.getMinX() + obstacle.getWidth() * 0.5;
        double ocy = obstacle.getMinY() + obstacle.getHeight() * 0.5;
        double oldDx = (oldBox.getMinX() + oldBox.getWidth() * 0.5) - ocx;
        double oldDy = (oldBox.getMinY() + oldBox.getHeight() * 0.5) - ocy;
        double newDx = (newBox.getMinX() + newBox.getWidth() * 0.5) - ocx;
        double newDy = (newBox.getMinY() + newBox.getHeight() * 0.5) - ocy;
        return (newDx * newDx + newDy * newDy) <= (oldDx * oldDx + oldDy * oldDy) + 0.01;
    }

    // ======================== VẼ BẢN ĐỒ TỰ DO ========================
// ======================== VẼ BẢN ĐỒ TỰ DO ========================
    private void buildFreeMap() {
        // Ẩn lưới cũ đi (Không xóa để tránh lỗi FXML)
        farmGrid.setVisible(false);

        // Tạo khung bản đồ mới
        if (mapPane != null) {
            ((StackPane) farmGrid.getParent()).getChildren().removeAll(mapPane, entityPane);
        }
        mapPane = new Pane();
        entityPane = new Pane();
        
        setupTimeOverlay();
        
        // Tối ưu hóa FPS cực mạnh: Tách layer tĩnh (mapPane) ra khỏi layer động (entityPane).
        // Cài đặt Cache cho mapPane giúp JavaFX không phải tính toán lại 6400 node mỗi khi di chuyển camera.
        // mapPane.setCache(true);
        // mapPane.setCacheHint(javafx.scene.CacheHint.SPEED);

        facingHighlight = new javafx.scene.shape.Rectangle(Constant.CELL_SIZE, Constant.CELL_SIZE, Color.TRANSPARENT);
        facingHighlight.setStroke(Color.WHITE);
        facingHighlight.setStrokeWidth(3.0);
        facingHighlight.getStrokeDashArray().addAll(5d, 5d);
        facingHighlight.setVisible(false);
        facingHighlight.setUserData(-480.0);
        entityPane.getChildren().add(facingHighlight);
        entityPane.setMouseTransparent(true); // Để click chuột xuyên qua rơi xuống mapPane

        mapPane.setOnMouseClicked(e -> {
            rootPane.requestFocus();
        });
        StackPane parentStack = (StackPane) farmGrid.getParent();
        StackPane.setAlignment(mapPane, Pos.TOP_LEFT);
        StackPane.setAlignment(entityPane, Pos.TOP_LEFT);
        
        // Chèn bản đồ và entities vào ngay dưới lớp thời tiết (weatherOverlay)
        parentStack.getChildren().add(1, mapPane);
        parentStack.getChildren().add(2, entityPane);

        int rows = farmMap.getHeight();
        int cols = farmMap.getWidth();
        tiles = new Tile[rows][cols];
        

        
        // Xóa thú vật cũ
        for (Animal a : animals) {
            if (entityPane != null) entityPane.getChildren().remove(a.view);
        }
        animals.clear();

        // 1. LỚP DƯỚI CÙNG: Trải thảm địa hình (Cỏ, Đất, Đường mòn)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Tile tile = new Tile(farmMap.getCell(r, c));
                tiles[r][c] = tile;
                
                tile.pane.setLayoutX(c * Constant.CELL_SIZE);
                tile.pane.setLayoutY(r * Constant.CELL_SIZE);
                
                mapPane.getChildren().add(tile.pane);
            }
        }

        // 2. LỚP GIỮA: Xây dựng các công trình kiến trúc và trang trí
        sceneryObjects = new java.util.ArrayList<>();
        clearedSceneryKeys.clear();
        windmillBladeViews.clear();
        if (windmillBladeFrames == null) {
            windmillBladeFrames = new javafx.scene.image.Image[6];
            for (int fi = 0; fi < 6; fi++) {
                try {
                    java.io.InputStream is = getClass().getResourceAsStream("/view/assets/tiled/objects/windmill_blades_" + fi + ".png");
                    if (is != null) windmillBladeFrames[fi] = new javafx.scene.image.Image(is);
                } catch (Exception ignored) {}
            }
        }
        
        // Xác định AnimalZone và FarmZone trước từ TMX
        farmZone = null;
        animalZone = null;
        if (tmxMap != null && tmxMap.objects != null) {
            double zs = (tmxMap.tileWidth > 0) ? (double) Constant.CELL_SIZE / tmxMap.tileWidth : 1.0;
            for (view.screen_util.TiledMap.TiledObject obj : tmxMap.objects) {
                String objName = obj.name != null ? obj.name.toLowerCase() : "";
                String type = obj.type != null ? obj.type.toLowerCase() : "";
                if ("zone".equals(type) || objName.endsWith("zone")) {
                    if (objName.equals("animalzone")) {
                        // Giới hạn chặt vào bên trong hàng rào chuồng thú (bên phải rào dọc x=985, dưới rào ngang y=948, trên bờ sông y=1253)
                        double zx = Math.max(obj.x, 985) * zs;
                        double zy = Math.max(obj.y, 948) * zs;
                        double maxZx = Math.min(obj.x + obj.width, 1540) * zs;
                        double maxZy = Math.min(obj.y + obj.height, 1253) * zs;
                        animalZone = new javafx.geometry.Rectangle2D(zx, zy, Math.max(200, maxZx - zx), Math.max(180, maxZy - zy));
                        initAnimalFarmLayout();
                    } else if (objName.equals("farmzone")) {
                        farmZone = new javafx.geometry.Rectangle2D(obj.x * zs, obj.y * zs, obj.width * zs, obj.height * zs);
                    }
                }
            }
        }

        // Render objects from TMX map
            if (tmxMap != null && tmxMap.objects != null) {
                for (view.screen_util.TiledMap.TiledObject obj : tmxMap.objects) {
                    String objName = obj.name != null ? obj.name.toLowerCase() : "";
                    String type = obj.type != null ? obj.type.toLowerCase() : "";
                    
                    // Xử lý Gameplay Zones
                    if ("zone".equals(type) || objName.endsWith("zone")) {
                        continue;
                    }
                    
                    // Bỏ qua các tile thú tĩnh không tên đặt nhầm ngoài map
                    if (objName.isEmpty() && (obj.gid == 22 || obj.gid == 23 || obj.gid == 24 || obj.gid == 3)) {
                        continue;
                    }

                    // Xử lý Thú vật (Cow, Chicken, Pig)
                    double scale = 1.0;
                    if (tmxMap != null && tmxMap.tileWidth > 0) {
                        scale = (double) Constant.CELL_SIZE / tmxMap.tileWidth;
                    }

                    // Xử lý Thú vật (Cow, Chicken, Pig)
                    if ("animal".equals(type) || objName.equals("cow") || objName.equals("chicken") || objName.equals("pig")) {
                        int aType = objName.equals("cow") ? 1 : (objName.equals("pig") ? 3 : 0);
                        // Tiled coordinates for tile-objects are at bottom-left
                        double ax = obj.x * scale;
                        double ay = (obj.y - obj.height) * scale;
                        if (obj.gid == 0) {
                            ay = obj.y * scale; // Standard rect objects are top-left
                        }
                        Animal animal = new Animal(ax, ay, aType);
                        animals.add(animal);
                        if (entityPane != null) entityPane.getChildren().add(animal.view);
                        continue;
                    }

                    // Xử lý các Object tĩnh
                    javafx.scene.image.Image img = null;
                    if (obj.gid > 0) {
                        img = tmxMap.getTileImage(obj.gid);
                    }
                    
                    // Fallback nếu thiếu gid (do lỗi cập nhật objects.tsx)
                    if (img == null && !objName.isEmpty()) {
                        String baseName = objName.replaceAll("\\d+$", ""); // VD: bush3 -> bush
                        String[] possiblePaths = {
                            "/view/assets/tiled/objects/" + baseName + ".png",
                            "/view/assets/tiled/objects/" + objName + ".png",
                            "/view/assets/tiled/objects/" + baseName + "_tile.png",
                            "/view/assets/tiled/objects/" + objName + "_tile.png"
                        };
                        for (String p : possiblePaths) {
                            java.io.InputStream is = getClass().getResourceAsStream(p);
                            if (is != null) {
                                img = new javafx.scene.image.Image(is);
                                break;
                            }
                        }
                        // Thử các tên ánh xạ tiếng Việt
                        if (img == null) {
                            String mappedPath = null;
                            if (baseName.equals("rock_large")) mappedPath = "/view/assets/tiled/objects/dato_tile.png";
                            else if (baseName.equals("rock_small")) mappedPath = "/view/assets/tiled/objects/danho_tile.png";
                            else if (baseName.equals("stump")) mappedPath = "/view/assets/tiled/objects/goccay_tile.png";
                            else if (baseName.equals("log")) mappedPath = "/view/assets/tiled/objects/thancay_tile.png";
                            else if (baseName.equals("bush")) mappedPath = "/view/assets/tiled/objects/buicay1_tile.png";
                            
                            if (mappedPath != null) {
                                java.io.InputStream is = getClass().getResourceAsStream(mappedPath);
                                if (is != null) img = new javafx.scene.image.Image(is);
                            }
                        }
                    }

                    if (img != null) {
                        ImageView objView = new ImageView(img);
                        objView.setFitWidth(obj.width * scale);
                        objView.setFitHeight(obj.height * scale);
                        objView.setLayoutX(obj.x * scale);
                        
                        // Default behavior for Tiled tile-objects (gid > 0): Y is bottom
                        // If it's a regular rect object (gid == 0), Y is top
                        if (obj.gid > 0) {
                            objView.setLayoutY((obj.y - obj.height) * scale);
                        } else {
                            objView.setLayoutY(obj.y * scale);
                        }
                        
                        // Sort by Y for depth
                        String lowerName = objName.toLowerCase();
                        if (lowerName.equals("river_north_abutment")) {
                            objView.setUserData(-1008.0); // Mố bờ đất & cỏ phía Bắc nằm trên dòng sông (-1020) và ngay dưới chân cầu (-1000)
                        } else if (lowerName.contains("bridge")) {
                            objView.setUserData(-1000.0); // Always draw bridge behind everything else (like ground)
                        } else if (lowerName.contains("grass_patch")) {
                            objView.setUserData(-700.0); // Tufted dark-olive grass patches sit right on top of ground under everything
                        } else if (lowerName.contains("dirt_spot") || lowerName.contains("flower") || lowerName.contains("pebble")) {
                            objView.setUserData(-600.0); // Ground dirt patches, wildflowers, pebbles stay under everything
                        } else if (lowerName.equals("swimming_pool")) {
                            objView.setUserData(-490.0); // Hồ bơi sát mặt đất, viền gạch nằm dưới nhân vật & ghế
                        } else if (lowerName.equals("pool_lounger")) {
                            objView.setUserData(-460.0); // Ghế tắm nắng nằm dưới lưng nhân vật khi nằm thư giãn
                        } else if (lowerName.contains("construction") || lowerName.contains("farm_rake") || lowerName.contains("farm_hoe") || lowerName.contains("farm_shovel") || lowerName.contains("broken_")) {
                            objView.setUserData(-500.0); // Ground decals always stay underneath player/trees/buildings
                        } else if (lowerName.contains("garden_bed")) {
                            objView.setUserData(objView.getLayoutY() + 10.0); // Pumpkins/vines inside bed sort on top
                        } else {
                            objView.setUserData((double)(objView.getLayoutY() + (obj.height * scale)));
                        }
                        objView.setSmooth(false);
                        
                        if (obj.rotation != 0) {
                            objView.setRotate(obj.rotation);
                        }
                        
                        objView.setId(objName);
                        sceneryObjects.add(objView);

                        // Tự động gắn cánh quạt xoay cho cối xay gió (windmill)
                        if (lowerName.equals("windmill") && windmillBladeFrames != null && windmillBladeFrames[0] != null) {
                            ImageView blades = new ImageView(windmillBladeFrames[0]);
                            double bw = objView.getFitWidth() * 1.38;
                            double bh = bw;
                            blades.setFitWidth(bw);
                            blades.setFitHeight(bh);
                            blades.setLayoutX(objView.getLayoutX() + (objView.getFitWidth() - bw) / 2.0);
                            blades.setLayoutY(objView.getLayoutY() - bh * 0.12);
                            blades.setSmooth(false);
                            blades.setId("windmill_blades");
                            blades.setUserData(((Double) objView.getUserData()) + 1.0);
                            sceneryObjects.add(blades);
                            windmillBladeViews.add(blades);
                        }
                    } else if (obj.gid > 0) {
                        System.err.println("Warning: Missing image for gid " + obj.gid + " and name " + objName);
                    }
                }
            }
            
            // THÊM PET SHOP VÀ NPC TỪ YÊU CẦU CỦA USER ĐÃ ĐƯỢC ĐƯA VÀO TMX MAP MẶC ĐỊNH
            
        sceneryObjects.sort((o1, o2) -> Double.compare((double)o1.getUserData(), (double)o2.getUserData()));
        if (entityPane != null) {
            entityPane.getChildren().addAll(sceneryObjects);
            mountAnimalPenProps();
        }
        setupHouseAndCampfireInteractions();
        buildTroughs();

        // Tự động xác định đúng tọa độ thế giới (CELL_SIZE = 64) của con sông từ các ô WATER trong FarmMap
        int firstWaterRow = -1;
        int lastWaterRow = -1;
        for (int r = 0; r < rows; r++) {
            int waterCount = 0;
            for (int c = 0; c < cols; c++) {
                if (farmMap.getCell(r, c).getTerrain() == TerrainType.WATER) waterCount++;
            }
            if (waterCount >= cols / 2) {
                if (firstWaterRow == -1) firstWaterRow = r;
                lastWaterRow = r;
            }
        }
        double worldMapW = cols * (double) Constant.CELL_SIZE;
        double riverTopY = (firstWaterRow >= 0 ? firstWaterRow : 32) * (double) Constant.CELL_SIZE;
        double riverBotY = ((lastWaterRow >= 0 ? lastWaterRow : 36) + 1) * (double) Constant.CELL_SIZE;
        this.worldRiverTopY = riverTopY;
        this.worldRiverBotY = riverBotY;

        // Xác định mép trái cây cầu trên tọa độ thế giới để đặt thuyền & cầu tàu gọn bên trái cầu, đồng thời dựng lớp Mái Cầu che phía trên khi thuyền chui gầm cầu
        double bridgeLeftX = worldMapW * 0.41;
        bridgeWoodLeftX = -1.0;
        bridgeWoodRightX = -1.0;
        bridgeDeckLeftX = -1.0;
        bridgeDeckRightX = -1.0;
        bridgeDeckTopY = -1.0;
        bridgeDeckBotY = -1.0;
        bridgeArchHeightRatio = 0.0;
        bridgeArchSlope = 0.0;
        bridgeCanopyView = null;
        northAbutmentX = -1.0;
        northAbutmentY = -1.0;
        northAbutmentW = 0.0;
        northAbutmentH = 0.0;
        if (entityPane != null && !bridgePillarOverlays.isEmpty()) {
            for (BridgePillarOverlay po : bridgePillarOverlays) {
                entityPane.getChildren().remove(po.view);
            }
        }
        bridgePillarOverlays.clear();
        for (ImageView objView : sceneryObjects) {
            if (objView.getId() != null && objView.getId().equalsIgnoreCase("river_north_abutment")) {
                northAbutmentX = objView.getLayoutX();
                northAbutmentY = objView.getLayoutY();
                northAbutmentW = objView.getFitWidth();
                northAbutmentH = objView.getFitHeight();
            }
        }
        for (ImageView objView : sceneryObjects) {
            if (objView.getId() != null && objView.getId().toLowerCase().contains("bridge")) {
                bridgeLeftX = objView.getLayoutX();
                setupBridgeUnderpassVisuals(objView, riverTopY, riverBotY);
                break;
            }
        }
        double boatWorldX = Math.max(120.0, bridgeLeftX - view.river.BoatEntity.BOAT_WIDTH - 105.0);
        double boatWorldY = riverTopY - 16.0;
        double dockWorldX = boatWorldX + 12.0;
        double dockWorldY = riverTopY - 26.0;

        // Gắn đầy đủ 4 module khu vực con sông đúng vị trí mặt nước (hàng WATER thực tế)
        isOnBoat = false;
        lastRiverUpdateMs = System.currentTimeMillis();
        double abutMinX = (northAbutmentW > 0.0) ? northAbutmentX : 960.0;
        double abutMaxX = (northAbutmentW > 0.0) ? (northAbutmentX + northAbutmentW) : 1472.0;
        riverFlow = new view.river.RiverFlow(worldMapW, riverTopY, riverBotY, abutMinX, abutMaxX);
        riverFlow.attachTo(entityPane);

        fishShadows = new view.river.FishShadows(worldMapW, riverTopY, riverBotY, boatWorldX);
        fishShadows.attachTo(entityPane);

        boatEntity = new view.river.BoatEntity(boatWorldX, boatWorldY, dockWorldX, dockWorldY);
        boatEntity.attachTo(entityPane);

        fishingGame = new view.river.FishingGame(boatEntity, fishShadows, riverFlow, new view.river.FishingGame.FishingCallbacks() {
            @Override
            public void onFishCaught(model.game_item.FishSpecies species, double sizeCm, boolean isCrown, int rewardMoney, int shadowSize) {
                fishCaughtCounts.merge(species, 1, Integer::sum);
                double prevMax = fishMaxCm.getOrDefault(species, 0.0);
                boolean isNewRecord = (sizeCm > prevMax);
                if (isNewRecord) {
                    fishMaxCm.put(species, sizeCm);
                }

                if (player != null) {
                    player.getInventory().addItem(model.game_item.FishItem.fromSpecies(species), 1);
                }
                onQuestFishCaught(species);
                // Kiểm tra tiến độ Giải Thi Câu Cá Bên Sông (Lễ Hội Ngày 7, 21... câu đủ 5 con nhận +$1.000!)
                int curFestDay = (gameManager != null) ? gameManager.getCurrentDay() : 1;
                if (festivalManager.recordFishCaught(curFestDay)) {
                    if (player != null) {
                        player.addMoney(model.festival.FestivalManager.FISHING_REWARD);
                    }
                    spawnFloatingText("🏆 VÔ ĐỊCH LỄ HỘI CÂU CÁ +$" + model.festival.FestivalManager.FISHING_REWARD + "!", Color.rgb(255, 226, 72));
                    if (particleSystem != null && boatEntity != null) {
                        particleSystem.spawnFireworkBurst(boatEntity.getBoardedPlayerX() + 22.0, boatEntity.getBoatY() - 36.0);
                    }
                }
                updateFestivalBannerUi();
                if (particleSystem != null && fishingGame != null) {
                    particleSystem.spawnWaterSplash(fishingGame.getLastBobberX(), fishingGame.getLastBobberY());
                    particleSystem.spawnHarvestSparkles(fishingGame.getLastBobberX(), fishingGame.getLastBobberY() - 8.0);
                    if (isCrown) {
                        particleSystem.spawnHeartBurst(boatEntity.getBoardedPlayerX() + 22.0, boatEntity.getBoatY() - 10.0);
                    }
                }
                view.fx.SoundHook.playPickup();
                showFishCatchCard(species, sizeCm, isCrown, isNewRecord, rewardMoney);

                String msg = String.format("Bắt được %s (%.1f cm%s) — đã cất vào Kho!",
                        species.getDisplayName(), sizeCm, isCrown ? " 👑 VƯƠNG MIỆN" : "");
                InGameController.this.notify(msg);

                if (autosave != null) {
                    javafx.animation.PauseTransition delaySave = new javafx.animation.PauseTransition(javafx.util.Duration.millis(720));
                    delaySave.setOnFinished(ev -> autosave.run());
                    delaySave.play();
                }
            }

            @Override
            public void onFishEscaped(String message) {
                InGameController.this.notify(message);
            }

            @Override
            public void onWaterSplash(double worldX, double worldY) {
                if (particleSystem != null) {
                    particleSystem.spawnWaterSplash(worldX, worldY);
                }
            }

            @Override
            public model.weather.WeatherType getCurrentWeather() {
                return (gameManager != null && gameManager.getCurrentWeather() != null)
                        ? gameManager.getCurrentWeather().getType()
                        : model.weather.WeatherType.SUNNY;
            }

            @Override
            public int getCurrentHour() {
                return getCurrentGameHour();
            }
        }, riverTopY, riverBotY, worldMapW);
        fishingGame.attachTo(entityPane);

        // Gắn hệ thống Game Feel Phase 1: Particle, Vật phẩm bật nảy & Tự hút, Rung lắc cây trồng
        particleSystem = new view.fx.ParticleSystem();
        particleSystem.attachTo(entityPane);

        droppedItemSystem = new view.fx.DroppedItemSystem((item, qty, wx, wy) -> {
            if (player != null && item != null && qty > 0) {
                player.getInventory().addItem(item, qty);
            }
        }, particleSystem);
        droppedItemSystem.attachTo(entityPane);

        // 3. LỚP TRÊN CÙNG: Đặt nhân vật vào game
        if (playerCharacter == null) {
            // Tự động tìm bãi đất trống để Spawn (Tránh bị kẹt trong cây/nhà)
            boolean foundSpawn = false;
            for (int r = 8; r < farmMap.getHeight() && !foundSpawn; r++) {
                for (int c = 8; c < farmMap.getWidth() && !foundSpawn; c++) {
                    if (farmMap.getCell(r, c).getTerrain() != TerrainType.WATER) {
                        double testX = c * Constant.CELL_SIZE;
                        double testY = r * Constant.CELL_SIZE;
                        javafx.geometry.Rectangle2D testBox = new javafx.geometry.Rectangle2D(testX, testY, 64, 64);
                        boolean overlap = false;
                        for (ImageView obj : sceneryObjects) {
                            javafx.geometry.Rectangle2D bRect = new javafx.geometry.Rectangle2D(obj.getLayoutX(), obj.getLayoutY(), obj.getFitWidth(), obj.getFitHeight());
                            if (testBox.intersects(bRect)) {
                                overlap = true; 
                                break;
                            }
                        }
                        if (!overlap) {
                            playerX = testX + 10;
                            playerY = testY + 10;
                            foundSpawn = true;
                        }
                    }
                }
            }

            playerCharacter = new javafx.scene.Group();

            // Load đủ bộ sprite pixel-art Luffy 8 hướng (side, down, up, down_right, up_right — kích thước lớn 108x108)
            if (luffyIdleByDir == null || luffyRunByDir == null) {
                luffyIdleByDir = new java.util.HashMap<>();
                luffyRunByDir  = new java.util.HashMap<>();
                String[] dirs = { "side", "down", "up", "down_right", "up_right" };
                for (String dk : dirs) {
                    Image[] idles = new Image[2];
                    for (int i = 0; i < 2; i++) {
                        try {
                            java.io.InputStream is = getClass().getResourceAsStream("/view/assets/player/luffy_idle_" + dk + "_" + i + ".png");
                            if (is != null) idles[i] = new Image(is);
                        } catch (Exception ignored) {}
                    }
                    luffyIdleByDir.put(dk, idles);

                    Image[] runs = new Image[4];
                    for (int i = 0; i < 4; i++) {
                        try {
                            java.io.InputStream is = getClass().getResourceAsStream("/view/assets/player/luffy_run_" + dk + "_" + i + ".png");
                            if (is != null) runs[i] = new Image(is);
                        } catch (Exception ignored) {}
                    }
                    luffyRunByDir.put(dk, runs);
                }
            }

            // Bóng mờ dưới chân Luffy (tỷ lệ lớn 108x108)
            luffyShadow = new javafx.scene.shape.Ellipse(22, 66, 22, 7);
            luffyShadow.setFill(Color.rgb(20, 26, 36, 0.30));

            Image startImg = luffyIdleByDir.get("down") != null ? luffyIdleByDir.get("down")[0] : null;
            luffySpriteView = new ImageView(startImg);
            luffySpriteView.setFitWidth(108);
            luffySpriteView.setFitHeight(108);
            luffySpriteView.setSmooth(false);
            // Căn tâm ngang x = 22 (-32 + 54 = 22) và dép khớp đúng y = 68 (-38 + 106 = 68)
            luffySpriteView.setX(-32);
            luffySpriteView.setY(-38);
            luffyBodyRotate = new javafx.scene.transform.Rotate(0.0, 22.0, 66.0);
            luffySpriteView.getTransforms().add(luffyBodyRotate);

            playerCharacter.getChildren().addAll(luffyShadow, luffySpriteView);
            luffyBridgeScale = new javafx.scene.transform.Scale(1.0, 1.0, 22.0, 66.0);
            playerCharacter.getTransforms().add(luffyBridgeScale);
            playerCharacter.setManaged(false);
            if (entityPane != null) entityPane.getChildren().add(playerCharacter);
        } else if (entityPane != null && !entityPane.getChildren().contains(playerCharacter)) {
            entityPane.getChildren().add(playerCharacter);
        }
        playerCharacter.setTranslateX(playerX);
        playerCharacter.setTranslateY(playerY);

        if (luffyToolRig == null) {
            // Tạo nhóm Cánh Tay + Công Cụ thống nhất (armGroup) xoay quanh khớp vai (0, 0) của Luffy
            weaponView = new ImageView();
            weaponView.setFitWidth(38);
            weaponView.setFitHeight(38);
            weaponView.setPreserveRatio(true);
            weaponView.setSmooth(false); // Giữ nét pixel sắc, không làm mờ
            weaponView.setManaged(false);
            toolRotate = new javafx.scene.transform.Rotate(0.0, 8.0, 31.0);
            weaponView.getTransforms().add(toolRotate);

            // 1. Tay áo đỏ (#C82828 viền đen) gắn tại khớp vai (0, 0)
            javafx.scene.shape.Rectangle sleeve = new javafx.scene.shape.Rectangle(-3.0, -2.0, 9.0, 12.0);
            sleeve.setArcWidth(2.0);
            sleeve.setArcHeight(2.0);
            sleeve.setRotate(-22.0);
            sleeve.setFill(Color.web("#C82828"));
            sleeve.setStroke(Color.rgb(28, 12, 14));
            sleeve.setStrokeWidth(1.8);

            // Cổ tay áo đỏ đậm tạo độ nổi pixel
            javafx.scene.shape.Rectangle sleeveCuff = new javafx.scene.shape.Rectangle(1.0, 8.0, 10.0, 3.2);
            sleeveCuff.setRotate(-22.0);
            sleeveCuff.setFill(Color.web("#8E1A1A"));
            sleeveCuff.setStroke(Color.rgb(28, 12, 14));
            sleeveCuff.setStrokeWidth(1.3);

            // 2. Cẳng tay màu da (#F2B880 viền đen) nối từ cổ tay áo tới bàn tay nắm
            javafx.scene.shape.Rectangle forearm = new javafx.scene.shape.Rectangle(4.5, 9.0, 7.0, 10.5);
            forearm.setArcWidth(2.0);
            forearm.setArcHeight(2.0);
            forearm.setRotate(-22.0);
            forearm.setFill(Color.web("#F2B880"));
            forearm.setStroke(Color.rgb(28, 12, 14));
            forearm.setStrokeWidth(1.6);

            // 3. Bàn tay nắm màu da (#F2B880 viền đen) bọc trực tiếp lên chuôi công cụ tại (11, 18)
            javafx.scene.shape.Rectangle handFist = new javafx.scene.shape.Rectangle(7.0, 14.5, 8.5, 7.5);
            handFist.setArcWidth(3.0);
            handFist.setArcHeight(3.0);
            handFist.setFill(Color.web("#F2B880"));
            handFist.setStroke(Color.rgb(28, 12, 14));
            handFist.setStrokeWidth(1.6);

            javafx.scene.shape.Line knuckle = new javafx.scene.shape.Line(11.0, 15.5, 11.0, 21.0);
            knuckle.setStroke(Color.rgb(186, 116, 72));
            knuckle.setStrokeWidth(1.2);

            // Thứ tự lớp trong armGroup: cẳng tay -> tay áo -> công cụ -> bàn tay nắm (bọc chuôi cầm)
            armGroup = new javafx.scene.Group(forearm, sleeve, sleeveCuff, weaponView, handFist, knuckle);
            armGroup.setLayoutX(29.0);
            armGroup.setLayoutY(9.0);
            armRotate = new javafx.scene.transform.Rotate(0.0, 0.0, 0.0);
            armGroup.getTransforms().add(armRotate);

            luffyToolRig = new javafx.scene.Group(armGroup);
            luffyToolRig.setManaged(false);
            luffyToolFlip = new javafx.scene.transform.Scale(1.0, 1.0, 22.0, 0.0);
            luffyToolRig.getTransforms().add(luffyToolFlip);

            if (entityPane != null) entityPane.getChildren().add(luffyToolRig);
        } else if (entityPane != null && !entityPane.getChildren().contains(luffyToolRig)) {
            entityPane.getChildren().add(luffyToolRig);
        }
        // Đồng bộ icon công cụ ngay khi khởi tạo map để nhân vật cầm ngay công cụ đang chọn
        if (playerController != null && playerController.getCurrentTool() != null) {
            ToolStrategy initTool = playerController.getCurrentTool();
            if (initTool instanceof Hoe) weaponView.setImage(Assets.HOE_ICON);
            else if (initTool instanceof WateringCan) weaponView.setImage(Assets.WATER_ICON);
            else if (initTool instanceof SeedBag) weaponView.setImage(Assets.SEED_ICON);
            else if (initTool instanceof FertilizerBag) weaponView.setImage(Assets.FERTILIZER_ICON);
            else if (initTool instanceof FeedBag) weaponView.setImage(Assets.BAG_ICON);
            else weaponView.setImage(null);
        }
    }

    // ======================== MỞ NHANH CÁC TAB TRONG CUỐN SỔ QUẢN LÝ (GAME BOOK) ========================
    @FXML
    private void openGameBookDefault() {
        toggleGameBook(view.ui.GameBookOverlay.TAB_STORAGE);
    }

    @FXML
    private void openShop() {
        openGameBook(view.ui.GameBookOverlay.TAB_SHOP);
    }

    @FXML
    private void openStorageMenu() {
        openGameBook(view.ui.GameBookOverlay.TAB_STORAGE);
    }

    @FXML
    private void openBookQuests(javafx.scene.input.MouseEvent ev) {
        // Nếu bấm trực tiếp vào nút Nhận Thưởng thì để sự kiện nút xử lý riêng
        if (ev != null && questClaimButton != null && questClaimButton.isVisible()) {
            javafx.scene.Node target = (ev.getTarget() instanceof javafx.scene.Node) ? (javafx.scene.Node) ev.getTarget() : null;
            while (target != null) {
                if (target == questClaimButton) return;
                target = target.getParent();
            }
        }
        openGameBook(view.ui.GameBookOverlay.TAB_QUESTS);
    }

    @FXML
    private void openBookSettings() {
        openGameBook(view.ui.GameBookOverlay.TAB_SETTINGS);
    }

    public void toggleGameBook(int tabIdx) {
        if (activeGameBook != null) {
            if (activeGameBook.getActiveTabIndex() == tabIdx) {
                closeGameBook();
            } else {
                activeGameBook.selectTab(tabIdx);
            }
            return;
        }
        openGameBook(tabIdx);
    }

    public void openGameBook(int tabIdx) {
        if (sleepTransitionRunning || router == null) return;
        if (activeGameBook != null) {
            activeGameBook.selectTab(tabIdx);
            Platform.runLater(activeGameBook::requestFocus);
            return;
        }
        if (isSunbathing) {
            stopSunbathing();
        }
        if (router.hasOverlay()) {
            return;
        }
        activeKeys.clear();
        activeGameBook = new view.ui.GameBookOverlay(
            player,
            shopController,
            () -> (gameManager != null && gameManager.getCurrentWeather() != null)
                    ? gameManager.getCurrentWeather().getType()
                    : WeatherType.SUNNY,
            this::getCurrentGameHour,
            () -> clockText(Math.min(1.0, Math.max(0.0, (double) elapsedTimeInDay / DAY_DURATION_MS))),
            fishCaughtCounts,
            fishMaxCm,
            this::onClaimQuestReward,
            this::closeGameBook,
            tabIdx
        );
        syncAllNpcPerks();
        activeGameBook.setOnEatItem(this::consumeItemFromStorage);
        activeGameBook.setMarketManager(marketManager);
        router.showOverlay(activeGameBook);
        Platform.runLater(activeGameBook::requestFocus);
    }

    private void closeGameBook() {
        if (activeGameBook != null) {
            activeGameBook = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            if (autosave != null) {
                autosave.run();
            }
            Platform.runLater(rootPane::requestFocus);
        }
    }

    @FXML
    private void onNextDay() {
        advanceDayCore(true, true);
    }

    /**
     * Logic chuyển ngày dùng chung cho:
     * - Nút NEXT DAY (useQuickFade=true, resetToMorning=true)
     * - Đi ngủ tại nhà (useQuickFade=false, resetToMorning=true)
     * - Đồng hồ tự chạy qua 24:00 -> 00:00 sang ngày hôm sau mà không đi ngủ (useQuickFade=false, resetToMorning=false)
     */
    private void advanceDayCore(boolean useQuickFade, boolean resetToMorning) {
        if (resetToMorning) {
            // Reset thời gian ngày về 06:00 sáng khi đi ngủ / bấm NEXT DAY
            if (droppedItemSystem != null) {
                droppedItemSystem.collectAllImmediately();
            }
            elapsedTimeInDay = MORNING_START_MS;
            if (isSunbathing) stopSunbathing();
            if (isSitting) standUpFromCampfire();
            // Khi đi ngủ sang sáng hôm sau, đảm bảo chỉ số buổi sáng tối thiểu 60 và giữ no 4 tiếng đầu buổi sáng
            hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
            recentLaborStreak = 0;
            recentLaborDecaySec = 0.0;
            if (player != null) {
                if (player.getHunger() < 60.0) player.setHunger(60.0);
                if (player.getThirst() < 60.0) player.setThirst(60.0);
                updateSurvivalHud(animTimeSec);
            }
            currentTimeTint = getTimeOverlayColor((double) elapsedTimeInDay / DAY_DURATION_MS);
            if (timeOverlayRect != null) {
                timeOverlayRect.setFill(currentTimeTint);
            }
        }
        clearFoodBuffs();
        spawnDailyCampForageItems();
        regrowChoppedTreesForNewDay();
        updateQuestTrackerUi();
        resetFeedingSchedule((double) elapsedTimeInDay / DAY_DURATION_MS);
        lastFrameTime = System.currentTimeMillis();
        lastDisplayedMinute = -1;
        for (Animal a : animals) {
            a.eatTrough = null;
            a.producedToday = false;
        }
        if (useQuickFade) {
            playDayTransitionFade();
        }

        gameManager.advanceDay();

        // Cập nhật Thị Trường Sốt Giá (+50%), Lễ Hội 7 Ngày, Mọc lại Đá Mỏ & Vòi Tưới Tự Động tưới 8 ô xung quanh!
        int newDayNum = gameManager.getCurrentDay();
        marketManager.rollForDay(newDayNum, orchardUnlocked);
        festivalManager.onDayStart(newDayNum);
        mineManager.respawnAllRocks();
        int autoWatered = mineManager.waterSurroundingCellsDaily(farmMap, particleSystem);
        syncAllNpcPerks();
        updateFestivalBannerUi();
        if (npcManager != null) {
            npcManager.setFireworksGathering(false, poolWaterX + poolWaterW * 0.5, poolWaterY + poolWaterH * 0.5);
        }
        
        // Auto-feed by helper
        if (helperDaysLeft > 0) {
            helperDaysLeft--;
            model.game_item.Item feedItem = player.getInventory().findByName("Animal Feed");
            int feedCount = (feedItem != null) ? player.getInventory().getItemCount(feedItem) : 0;
            int feedNeeded = 0;
            for (Animal a : animals) { 
                if (a.type != 2 && a.ageDays < 5 && !a.isFed) feedNeeded++; 
            }
            
            if (feedNeeded > 0) {
                if (feedCount >= feedNeeded) {
                    player.getInventory().removeItem(feedItem, feedNeeded);
                    for (Animal a : animals) {
                        if (a.type != 2 && a.ageDays < 5) a.feed();
                    }
                    notify("Người làm thuê đã cho thú ăn! (Còn " + helperDaysLeft + " ngày)");
                } else {
                    notify("Người làm thuê: Không đủ thức ăn trong kho!");
                }
            }
        }
        
        // Thú vật lớn lên sau khi qua ngày
        int grewCount = 0;
        int readyToSellCount = 0;
        for (Animal a : animals) {
            if (a.type != 2) { // 2 là vịt trời
                if (a.isFed) {
                    a.ageDays++;
                    a.isFed = false;
                    if (a.heartEmoji != null) a.heartEmoji.setVisible(false);
                    a.applySpriteScale();
                    grewCount++;
                    if (a.ageDays >= 5) {
                        readyToSellCount++;
                    }
                }
            }
        }
        
        String growthMsg = "";
        if (readyToSellCount > 0) {
            growthMsg = " (💰 " + readyToSellCount + " thú đã đủ lớn để bán!)";
        } else if (grewCount > 0) {
            growthMsg = " (" + grewCount + " thú cưng đã lớn thêm!)";
        }
        if (resetToMorning) {
            notify("Day " + gameManager.getCurrentDay() + "   " + weatherName() + growthMsg);
        } else {
            notify("🌙 Đã sang Day " + gameManager.getCurrentDay() + " (00:00)   " + weatherName() + growthMsg);
        }
        refreshDynamicTiles();
        if (autosave != null && gameManager.getStatus() == controller.lifecyclemanager.GameStatus.RUNNING) {
            autosave.run();
        }
    }

    private void selectTool(ToolStrategy tool, ToggleButton btn) {
        playerController.useTool(tool);
        if (btn != null) btn.setSelected(true);
        if (tool != seedBag) {
            resetSeedToolIcon();
        }
        if (tool != feedBag) {
            resetBagToolIcon();
        }
        ToolStrategy cur = playerController.getCurrentTool();
        toolLabel.setText("TOOL : " + (cur != null ? cur.getName().toUpperCase() : "(none)"));

        if (weaponView != null) {
            if (cur instanceof Hoe) weaponView.setImage(Assets.HOE_ICON);
            else if (cur instanceof Axe) weaponView.setImage(Assets.AXE_ICON);
            else if (cur instanceof WateringCan) weaponView.setImage(Assets.WATER_ICON);
            else if (cur instanceof Hand) weaponView.setImage(null); // Tay không: hiển thị trọn vẹn dáng chạy của Luffy
            else if (cur instanceof SeedBag) weaponView.setImage(Assets.SEED_ICON);
            else if (cur instanceof FertilizerBag) weaponView.setImage(Assets.FERTILIZER_ICON);
            else if (cur instanceof FeedBag) {
                Item selected = feedBag.getSelectedItem();
                Image icon = selected != null ? Assets.getShopIcon(selected) : Assets.BAG_ICON;
                weaponView.setImage(icon != null ? icon : Assets.BAG_ICON);
            }
            else weaponView.setImage(null);
        }
        
        // Trả focus lại cho màn hình game để có thể di chuyển
        rootPane.requestFocus();
    }

    private void forceWeather(WeatherType type) {
        demoController.forceWeather(type);
        notify("Weather forced: " + type);
        refreshHudOnly();
    }

    private void onCellClick(Cell cell) {
        if (router.hasOverlay()) return;
        
        // Hỗ trợ click chuột trực tiếp vào ô có thú nuôi để bán hoặc cho ăn (bằng Hand hoặc FeedBag)
        ToolStrategy curTool = playerController.getCurrentTool();
        if (curTool instanceof Hand || curTool instanceof FeedBag) {
            double clickX = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
            double clickY = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
            double playerCenterX = playerX + 22;
            double playerCenterY = playerY + 50;
            double maxReach = (curTool instanceof FeedBag) ? Constant.CELL_SIZE * 8.0 : Constant.CELL_SIZE * 3.5;
            if (Math.hypot(clickX - playerCenterX, clickY - playerCenterY) <= maxReach) {
                javafx.geometry.Rectangle2D clickHitBox = new javafx.geometry.Rectangle2D(clickX - 32, clickY - 32, 64, 64);
                if (tryDepositFeed(clickHitBox) || tryInteractWithAnimal(clickHitBox)) {
                    return;
                }
            }
        }

        if (curTool instanceof Axe) {
            if (player != null && player.isExhausted()) {
                spawnFloatingText("Quá đói/khát! Hãy ăn uống trước!", Color.rgb(255, 86, 86));
                notify("Quá đói/khát! Hãy ăn uống trước!");
                rootPane.requestFocus();
                return;
            }
            double clickX = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
            double clickY = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
            tryChopNearestTree(clickX, clickY);
            rootPane.requestFocus();
            return;
        }

        if (curTool instanceof Hoe) {
            double clickWx = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
            double clickWy = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
            if (mineManager.tryStrikeRock(clickWx, clickWy, playerX + 22.0, playerY + 52.0,
                    player, particleSystem, this::spawnFloatingText, this::notify)) {
                updateSurvivalHud(animTimeSec);
                triggerToolAnimation("HOE", -1, -1);
                rootPane.requestFocus();
                return;
            }
            if (!isCellInFarmZone(cell)) {
                notify("Chỉ có thể dùng cuốc trong khu trồng trọt (Farming Zone)!");
                rootPane.requestFocus();
                return;
            }
            if (cell != null && cell.getTerrain() != TerrainType.DIRT) {
                notify("Đây là lối đi cỏ xanh — hãy cuốc trên các luống đất 2x2!");
                rootPane.requestFocus();
                return;
            }
            if (tryClearWheatAtCell(cell)) {
                double hwx = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                double hwy = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                triggerToolAnimation("HOE", hwx, hwy);
                rootPane.requestFocus();
                return;
            }
        }

        boolean ok = interactCellWithEffects(cell);
        if (!ok && curTool instanceof SeedBag && cell.getCellState() != CellState.TILLED) {
            notify("Hãy dùng Cuốc (Hoe) cuốc đất trước khi trồng cây!");
            spawnFloatingText("Cần cuốc đất trước!", Color.rgb(255, 208, 92));
        } else {
            notify(ok ? "" : "Can't use this tool here");
        }
        revertSeedToolIfEmpty();
        revertBagToolIfEmpty();
        
        // Tối ưu hóa: Chỉ cập nhật (refresh) ô đất vừa tương tác, thay vì refresh toàn bộ map (gây lag)
        if (tiles != null && cell.getY() < tiles.length && cell.getX() < tiles[0].length) {
            tiles[cell.getY()][cell.getX()].style();
        }
        
        updateHover(cell);
        
        // Giữ focus cho rootPane sau khi click chuột
        rootPane.requestFocus();
    }

    private void revertSeedToolIfEmpty() {
        if (playerController.getCurrentTool() != seedBag) return;
        String name = seedBag.getSeedName();
        Item item = (name != null) ? player.getInventory().findByName(name) : null;
        if (item == null || player.getInventory().getItemCount(item) <= 0) {
            resetSeedToolIcon();
        }
    }

    private void revertBagToolIfEmpty() {
        if (playerController.getCurrentTool() != feedBag) return;
        Item item = feedBag.getSelectedItem();
        if (item == null) {
            resetBagToolIcon();
            if (weaponView != null) weaponView.setImage(Assets.BAG_ICON);
        }
    }

    private void togglePause() {
        if (sleepTransitionRunning) return;
        if (activeFactoryView != null) {
            activeFactoryView.requestExit();
            return;
        }
        if (activeGiftDialog != null) {
            closeNpcGiftOverlay();
            return;
        }
        if (activeCookingOverlay != null) {
            activeCookingOverlay.closeScreen();
            return;
        }
        if (activeSleepDialog != null) {
            closeSleepDialog();
            return;
        }
        if (activeGameBook != null) {
            closeGameBook();
            return;
        }
        if (activeFishingGuide != null) {
            closeFishingGuide();
            return;
        }
        if (activeCasinoOverlay != null) {
            activeCasinoOverlay.requestSafeClose();
            return;
        }
        if (isSunbathing) {
            stopSunbathing();
        }
        if (router.hasOverlay()) {
            router.closeOverlay();
            Platform.runLater(rootPane::requestFocus);
        } else {
            activeKeys.clear();
            if (pauseController != null) {
                pauseController.resetFeedback();
            }
            javafx.scene.Parent pauseNode = router.getScreen(Screens.PAUSE);
            router.showOverlay(pauseNode);
            Platform.runLater(pauseNode::requestFocus);
        }
    }

    // ======================== ĐI NGỦ TẠI NHÀ & NẤU ĂN LỬA TRẠI ========================

    /** Quét tự động vị trí cửa nhà, đống lửa, Khu Bể Bơi (Nami & Hancock), Sòng Bạc Casino và khởi tạo 4 Bot đi dạo. */
    private void setupHouseAndCampfireInteractions() {
        houseDoorPoints.clear();
        factoryDoorPoints.clear();
        orchardGateBounds = null;
        orchardInteractBox = null;
        if (orchardWorldSignLabel != null && entityPane != null) {
            entityPane.getChildren().remove(orchardWorldSignLabel);
            orchardWorldSignLabel = null;
        }
        campfirePoints.clear();
        campfireGlows.clear();
        streetlampLights.clear();
        animatedCampfires.clear();
        animatedFlags.clear();
        farmGatePortalBox = null;
        casinoDoorPoints.clear();
        poolSunbathePoints.clear();
        poolWaveLines.clear();
        poolSparkleDots.clear();
        casinoMarqueeBulbs.clear();
        isSunbathing = false;
        isSitting = false;

        double[] mainHouseDoor = null;
        double topLoungerX = 672.0;
        double topLoungerY = 2476.0;
        boolean foundFirstLounger = false;
        double orchardFenceMinX = Double.POSITIVE_INFINITY;
        double orchardFenceMinY = Double.POSITIVE_INFINITY;
        double orchardFenceMaxX = Double.NEGATIVE_INFINITY;
        double orchardFenceMaxY = Double.NEGATIVE_INFINITY;

        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id == null) continue;
            String idLow = id.toLowerCase();
            double ox = objView.getLayoutX();
            double oy = objView.getLayoutY();
            double ow = objView.getFitWidth();
            double oh = objView.getFitHeight();

            if (idLow.contains("house_main") || idLow.contains("house_small")) {
                double[] door = new double[] { ox + ow * 0.50, oy + oh + 10.0 };
                if (idLow.contains("house_main")) {
                    mainHouseDoor = door;
                } else {
                    houseDoorPoints.add(door);
                }
            } else if (idLow.equals("barn") || idLow.equals("house_bottom") || idLow.contains("factory")) {
                // Tòa nhà Nhà Máy Sản Xuất có sẵn trên bản đồ (cửa rộng chính diện)
                factoryDoorPoints.add(new double[] { ox + ow * 0.50, oy + oh + 12.0 });
                if (entityPane != null && idLow.equals("barn")) {
                    Label facSign = new Label("🏭 NHÀ MÁY SẢN XUẤT");
                    facSign.setMouseTransparent(true);
                    facSign.setManaged(false);
                    facSign.setStyle(
                        "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #FACC15; -fx-background-color: rgba(15, 23, 42, 0.88);" +
                        "-fx-border-color: #EAB308; -fx-border-width: 1.6px;" +
                        "-fx-padding: 2 8 2 8; -fx-background-radius: 5px; -fx-border-radius: 5px;"
                    );
                    facSign.setLayoutX(ox + ow * 0.50 - 74.0);
                    facSign.setLayoutY(oy + oh - 58.0);
                    facSign.setUserData(oy + oh + 6.0);
                    entityPane.getChildren().add(facSign);
                }
            } else if (idLow.equals("mine_sign") && entityPane != null) {
                Label mineLbl = new Label("⛏ KHU MỎ ĐÁ & ĐÚC VÒI TƯỚI");
                mineLbl.setMouseTransparent(true);
                mineLbl.setManaged(false);
                mineLbl.setStyle(
                    "-fx-font-family: 'Consolas', monospace; -fx-font-size: 10.5px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #E0F2FE; -fx-background-color: rgba(15, 23, 42, 0.88);" +
                    "-fx-border-color: #38BDF8; -fx-border-width: 1.5px;" +
                    "-fx-padding: 2 8 2 8; -fx-background-radius: 5px; -fx-border-radius: 5px;"
                );
                mineLbl.setLayoutX(ox - 54.0);
                mineLbl.setLayoutY(oy - 24.0);
                mineLbl.setUserData(oy + oh + 10.0);
                entityPane.getChildren().add(mineLbl);
            } else if (idLow.equals("farm_bed_frame") || idLow.startsWith("ground_") || idLow.startsWith("wildflowers")) {
                if (idLow.startsWith("wildflowers") && Assets.WILDFLOWERS != null) {
                    objView.setImage(Assets.WILDFLOWERS);
                }
                objView.setSmooth(false);
                objView.setUserData(-492.0);
            } else if (idLow.startsWith("garden_rock")) {
                if (Assets.GARDEN_ROCK != null) {
                    objView.setImage(Assets.GARDEN_ROCK);
                }
                objView.setSmooth(false);
            } else if (idLow.contains("fence") || idLow.startsWith("farm_decor_")) {
                if (idLow.equals("fence_h") && Assets.FENCE_WOOD != null) {
                    objView.setImage(Assets.FENCE_WOOD);
                }
                if (idLow.startsWith("orchard_fence")) {
                    orchardFenceMinX = Math.min(orchardFenceMinX, ox);
                    orchardFenceMinY = Math.min(orchardFenceMinY, oy);
                    orchardFenceMaxX = Math.max(orchardFenceMaxX, ox + ow);
                    orchardFenceMaxY = Math.max(orchardFenceMaxY, oy + oh);
                }
                objView.setSmooth(false);
            } else if (idLow.equals("farm_gate_side") && entityPane != null) {
                // Tách cổng xoay ngang thành 2 lớp chiều sâu 2.5D:
                // - Lớp sau (objView): trụ cổng phía trên (Bắc) -> nằm sau nhân vật khi đi qua giữa cổng
                // - Lớp trước (frontView): vòm cổng + đèn lồng + trụ cổng bên ngoài (Nam) -> che lên trên nhân vật khi đi qua giữa cổng
                if (Assets.FARM_GATE_SIDE_BACK != null) {
                    objView.setImage(Assets.FARM_GATE_SIDE_BACK);
                } else if (Assets.FARM_GATE_SIDE != null) {
                    objView.setImage(Assets.FARM_GATE_SIDE);
                }
                objView.setSmooth(false);
                objView.setUserData(oy + oh * 0.44);

                if (Assets.FARM_GATE_SIDE_FRONT != null) {
                    ImageView frontView = new ImageView(Assets.FARM_GATE_SIDE_FRONT);
                    frontView.setFitWidth(ow);
                    frontView.setFitHeight(oh);
                    frontView.setPreserveRatio(false);
                    frontView.setSmooth(false);
                    frontView.setManaged(false);
                    frontView.setMouseTransparent(true);
                    frontView.setLayoutX(ox);
                    frontView.setLayoutY(oy);
                    frontView.setUserData(oy + oh - 2.0);
                    entityPane.getChildren().add(frontView);
                }

                // Vùng đi qua giữa cổng xoay ngang (Đông <-> Tây giữa 2 trụ đá)
                farmGatePortalBox = new double[] { ox + ow * 0.10, oy + oh * 0.45, ox + ow * 0.85, oy + oh * 0.86 };

                // Ánh sáng vàng ấm cho chiếc đèn lồng treo trên cột ngoài cổng (tự bật sáng khi trời tối từ 18:00)
                double ltx = ox + ow * 0.831;
                double lanternY = oy + oh * 0.725;
                javafx.scene.paint.RadialGradient coreGrad = new javafx.scene.paint.RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255, 252, 224, 0.96)),
                    new javafx.scene.paint.Stop(0.45, Color.rgb(255, 218, 96, 0.80)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(255, 178, 48, 0.0))
                );
                javafx.scene.shape.Circle core = new javafx.scene.shape.Circle(0, 0, 12.0);
                core.setFill(coreGrad);
                core.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                core.setManaged(false);
                core.setMouseTransparent(true);
                core.setLayoutX(ltx);
                core.setLayoutY(lanternY);
                core.setOpacity(0.0);
                core.setVisible(false);
                core.setUserData(oy + oh + 6.0);

                javafx.scene.paint.RadialGradient haloGrad = new javafx.scene.paint.RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255, 214, 102, 0.52)),
                    new javafx.scene.paint.Stop(0.50, Color.rgb(255, 172, 54, 0.22)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(255, 140, 28, 0.0))
                );
                javafx.scene.shape.Circle halo = new javafx.scene.shape.Circle(0, 0, 44.0);
                halo.setFill(haloGrad);
                halo.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                halo.setManaged(false);
                halo.setMouseTransparent(true);
                halo.setLayoutX(ltx);
                halo.setLayoutY(lanternY);
                halo.setOpacity(0.0);
                halo.setVisible(false);
                halo.setUserData(oy + oh + 5.0);

                entityPane.getChildren().addAll(halo, core);
                streetlampLights.add(new javafx.scene.Node[] { core, halo });
            } else if ((idLow.equals("farm_gate") || idLow.equals("rustic_farm_gate") || idLow.equals("orchard_gate")) && entityPane != null) {
                if (Assets.FARM_GATE != null) {
                    objView.setImage(Assets.FARM_GATE);
                }
                objView.setSmooth(false);
                objView.setUserData(oy + oh - 16.0);
                // Cổng Vườn Cây Ăn Quả nằm ở khu vực phía dưới bể bơi (oy > worldRiverBotY): lưu toàn bộ khung cổng để tính AABB tương tác
                if (oy > worldRiverBotY) {
                    orchardGateBounds = new javafx.geometry.Rectangle2D(ox, oy, ow, oh);
                }
                // Vùng đi qua tự nhiên ngay dưới vòm Cổng Chào Nông Trại (không ghi đè cổng chính khu vườn đầu tiên)
                if (farmGatePortalBox == null) {
                    farmGatePortalBox = new double[] { ox + 28.0, oy + oh - 34.0, ox + ow - 28.0, oy + oh + 10.0 };
                }
                // Gắn 2 cụm ánh sáng vàng ấm cho 2 chiếc đèn lồng treo trên cổng (tự bật sáng khi trời tối từ 18:00)
                double[] lanternXs = { ox + ow * 0.285, ox + ow * 0.725 };
                double lanternY = oy + oh * 0.455;
                for (double ltx : lanternXs) {
                    javafx.scene.paint.RadialGradient coreGrad = new javafx.scene.paint.RadialGradient(
                        0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0.0, Color.rgb(255, 252, 224, 0.96)),
                        new javafx.scene.paint.Stop(0.45, Color.rgb(255, 218, 96, 0.80)),
                        new javafx.scene.paint.Stop(1.0, Color.rgb(255, 178, 48, 0.0))
                    );
                    javafx.scene.shape.Circle core = new javafx.scene.shape.Circle(0, 0, 12.0);
                    core.setFill(coreGrad);
                    core.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                    core.setManaged(false);
                    core.setMouseTransparent(true);
                    core.setLayoutX(ltx);
                    core.setLayoutY(lanternY);
                    core.setOpacity(0.0);
                    core.setVisible(false);
                    core.setUserData(oy + oh + 8.0);

                    javafx.scene.paint.RadialGradient haloGrad = new javafx.scene.paint.RadialGradient(
                        0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0.0, Color.rgb(255, 214, 102, 0.52)),
                        new javafx.scene.paint.Stop(0.50, Color.rgb(255, 172, 54, 0.22)),
                        new javafx.scene.paint.Stop(1.0, Color.rgb(255, 140, 28, 0.0))
                    );
                    javafx.scene.shape.Circle halo = new javafx.scene.shape.Circle(0, 0, 44.0);
                    halo.setFill(haloGrad);
                    halo.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                    halo.setManaged(false);
                    halo.setMouseTransparent(true);
                    halo.setLayoutX(ltx);
                    halo.setLayoutY(lanternY);
                    halo.setOpacity(0.0);
                    halo.setVisible(false);
                    halo.setUserData(oy + oh + 7.0);

                    entityPane.getChildren().addAll(halo, core);
                    streetlampLights.add(new javafx.scene.Node[] { core, halo });
                }
            } else if ((idLow.startsWith("streetlamp") || idLow.startsWith("lantern_post")) && entityPane != null) {
                boolean isLeftLamp = idLow.endsWith("_left");
                boolean isDownLamp = idLow.endsWith("_down");
                Image lampSprite = isLeftLamp
                    ? (Assets.LANTERN_POST_LEFT != null ? Assets.LANTERN_POST_LEFT : Assets.LANTERN_POST)
                    : (isDownLamp
                        ? (Assets.LANTERN_POST_DOWN != null ? Assets.LANTERN_POST_DOWN : Assets.LANTERN_POST)
                        : Assets.LANTERN_POST);
                if (lampSprite != null) {
                    objView.setImage(lampSprite);
                    objView.setSmooth(false);
                    objView.setPreserveRatio(false);
                    double newW = 76.0;
                    double newH = 108.0;
                    objView.setFitWidth(newW);
                    objView.setFitHeight(newH);
                    double anchorRatio = isLeftLamp ? 0.70 : (isDownLamp ? 0.50 : 0.30);
                    objView.setLayoutX(ox - (newW - ow) * anchorRatio);
                    objView.setLayoutY(oy + (oh - newH));
                    ox = objView.getLayoutX();
                    oy = objView.getLayoutY();
                    ow = newW;
                    oh = newH;
                }
                // Cột đèn lồng gỗ cỡ lớn & dày dặn: gắn 3 lớp ánh sáng vàng ấm đúng tâm lồng đèn treo (bật sáng từ 18:00 tối)
                double lx = ox + ow * (isLeftLamp ? 0.25 : (isDownLamp ? 0.475 : 0.72));
                double headY = oy + oh * (isLeftLamp ? 0.40 : (isDownLamp ? 0.48 : 0.36));
                double baseY = oy + oh * (isDownLamp ? 0.95 : 0.90);

                javafx.scene.paint.RadialGradient coreGrad = new javafx.scene.paint.RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255, 252, 224, 0.98)),
                    new javafx.scene.paint.Stop(0.45, Color.rgb(255, 218, 96, 0.88)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(255, 178, 48, 0.0))
                );
                javafx.scene.shape.Circle core = new javafx.scene.shape.Circle(0, 0, 20.0);
                core.setFill(coreGrad);
                core.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                core.setManaged(false);
                core.setMouseTransparent(true);
                core.setLayoutX(lx);
                core.setLayoutY(headY);
                core.setOpacity(0.0);
                core.setVisible(false);
                core.setUserData(oy + oh + 6.0);

                javafx.scene.paint.RadialGradient haloGrad = new javafx.scene.paint.RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255, 214, 102, 0.62)),
                    new javafx.scene.paint.Stop(0.50, Color.rgb(255, 172, 54, 0.28)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(255, 140, 28, 0.0))
                );
                javafx.scene.shape.Circle halo = new javafx.scene.shape.Circle(0, 0, 78.0);
                halo.setFill(haloGrad);
                halo.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                halo.setManaged(false);
                halo.setMouseTransparent(true);
                halo.setLayoutX(lx);
                halo.setLayoutY(headY);
                halo.setOpacity(0.0);
                halo.setVisible(false);
                halo.setUserData(oy + oh + 5.0);

                javafx.scene.paint.RadialGradient groundGrad = new javafx.scene.paint.RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255, 208, 92, 0.46)),
                    new javafx.scene.paint.Stop(0.55, Color.rgb(255, 168, 48, 0.18)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(255, 138, 24, 0.0))
                );
                javafx.scene.shape.Ellipse groundPool = new javafx.scene.shape.Ellipse(0, 0, 104.0, 58.0);
                groundPool.setFill(groundGrad);
                groundPool.setBlendMode(javafx.scene.effect.BlendMode.ADD);
                groundPool.setManaged(false);
                groundPool.setMouseTransparent(true);
                groundPool.setLayoutX(lx);
                groundPool.setLayoutY(baseY);
                groundPool.setOpacity(0.0);
                groundPool.setVisible(false);
                groundPool.setUserData(oy + oh - 4.0);

                entityPane.getChildren().addAll(groundPool, halo, core);
                streetlampLights.add(new javafx.scene.Node[] { core, halo, groundPool });
            } else if (idLow.equals("campfire")) {
                double cx = ox + ow * 0.50;
                double cy = oy + oh * 0.58;
                campfirePoints.add(new double[] { cx, cy });
                if (Assets.CAMPFIRE_FRAMES != null && Assets.CAMPFIRE_FRAMES.length > 0 && Assets.CAMPFIRE_FRAMES[0] != null) {
                    double bigW = 112.0;
                    double bigH = 112.0;
                    boolean rainOut = isCampfireExtinguished() && Assets.CAMPFIRE_EXTINGUISHED != null;
                    objView.setImage(rainOut ? Assets.CAMPFIRE_EXTINGUISHED : Assets.CAMPFIRE_FRAMES[0]);
                    objView.setSmooth(false);
                    objView.setPreserveRatio(false);
                    objView.setFitWidth(bigW);
                    objView.setFitHeight(bigH);
                    // Giữ tâm đáy vòng đá đúng vị trí cũ khi phóng to đống lửa
                    objView.setLayoutX(ox - (bigW - ow) * 0.5);
                    objView.setLayoutY(oy - (bigH - oh) + 10.0);
                    animatedCampfires.add(objView);
                }

                if (entityPane != null) {
                    javafx.scene.paint.RadialGradient fireGrad = new javafx.scene.paint.RadialGradient(
                        0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0.0, Color.rgb(255, 168, 56, 0.46)),
                        new javafx.scene.paint.Stop(0.55, Color.rgb(255, 114, 32, 0.20)),
                        new javafx.scene.paint.Stop(1.0, Color.rgb(255, 92, 20, 0.0))
                    );
                    javafx.scene.shape.Circle glow = new javafx.scene.shape.Circle(0, 0, 112.0);
                    glow.setFill(fireGrad);
                    glow.setManaged(false);
                    glow.setMouseTransparent(true);
                    glow.setLayoutX(cx);
                    glow.setLayoutY(cy + 4.0);
                    glow.setVisible(!isCampfireExtinguished());
                    double sortY = (objView.getUserData() instanceof Double) ? ((Double) objView.getUserData()) - 2.0 : (oy + oh - 2.0);
                    glow.setUserData(sortY);
                    entityPane.getChildren().add(glow);
                    campfireGlows.add(glow);
                }
            } else if (idLow.equals("swimming_pool")) {
                if (Assets.POOL_WATER != null) {
                    objView.setImage(Assets.POOL_WATER);
                    objView.setSmooth(false);
                }
                poolWaterX = ox + 22.0;
                poolWaterY = oy + 26.0;
                poolWaterW = Math.max(60.0, ow - 44.0);
                poolWaterH = Math.max(40.0, oh - 52.0);

                // Cho phép bấm [E] ngay sát mép phải & mép dưới bể bơi (< 75px)
                poolSunbathePoints.add(new double[] { ox + ow + 6.0, oy + oh * 0.35 });
                poolSunbathePoints.add(new double[] { ox + ow + 6.0, oy + oh * 0.72 });
                poolSunbathePoints.add(new double[] { ox + ow * 0.50, oy + oh + 8.0 });

                if (entityPane != null) {
                    // Các dải gợn sóng xanh ngọc & ánh nắng lấp lánh trên mặt hồ bơi
                    for (int wi = 0; wi < 5; wi++) {
                        double lx = poolWaterX + 18.0 + (wi % 3) * (poolWaterW * 0.31);
                        double ly = poolWaterY + 24.0 + (wi / 3) * (poolWaterH * 0.46) + (wi % 2) * 14.0;
                        javafx.scene.shape.Line wave = new javafx.scene.shape.Line(0, 0, 46.0, 0);
                        wave.setStroke(Color.rgb(210, 250, 255, 0.68));
                        wave.setStrokeWidth(2.4);
                        wave.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                        wave.setManaged(false);
                        wave.setMouseTransparent(true);
                        wave.setLayoutX(lx);
                        wave.setLayoutY(ly);
                        wave.setUserData(-485.0);
                        entityPane.getChildren().add(wave);
                        poolWaveLines.add(wave);
                    }
                    for (int si = 0; si < 8; si++) {
                        javafx.scene.shape.Circle sp = new javafx.scene.shape.Circle(2.4, Color.rgb(255, 255, 255, 0.85));
                        sp.setManaged(false);
                        sp.setMouseTransparent(true);
                        sp.setLayoutX(poolWaterX + 16.0 + ((si * 43) % (int) Math.max(30, poolWaterW - 32)));
                        sp.setLayoutY(poolWaterY + 14.0 + ((si * 29) % (int) Math.max(24, poolWaterH - 28)));
                        sp.setUserData(-484.0);
                        entityPane.getChildren().add(sp);
                        poolSparkleDots.add(sp);
                    }
                }
            } else if (idLow.equals("pool_lounger")) {
                if (Assets.POOL_CHAIR != null) {
                    objView.setImage(Assets.POOL_CHAIR);
                    objView.setSmooth(false);
                }
                double lxCenter = ox + ow * 0.50;
                double lyCenter = oy + oh * 0.55;
                poolSunbathePoints.add(new double[] { lxCenter, lyCenter });
                if (!foundFirstLounger || oy < topLoungerY) {
                    foundFirstLounger = true;
                    topLoungerX = ox;
                    topLoungerY = oy;
                }
                if (oy >= sunbatheLoungerY || sunbatheLoungerY <= 0) {
                    sunbatheLoungerX = ox;
                    sunbatheLoungerY = oy;
                }
            } else if (idLow.equals("pool_umbrella")) {
                if (Assets.POOL_UMBRELLA != null) {
                    objView.setImage(Assets.POOL_UMBRELLA);
                    objView.setSmooth(false);
                }
            } else if (idLow.equals("palm_tree")) {
                if (Assets.PALM_TREE != null) {
                    objView.setImage(Assets.PALM_TREE);
                    objView.setSmooth(false);
                }
            } else if (idLow.equals("casino_building")) {
                double doorX = ox + ow * 0.50;
                double doorY = oy + oh + 6.0;
                casinoDoorPoints.add(new double[] { doorX, doorY });

                // Dàn đèn led vàng & đỏ nhấp nháy rực rỡ quanh biển hiệu CASINO
                if (entityPane != null) {
                    double signLeft = ox + ow * 0.19;
                    double signRight = ox + ow * 0.81;
                    double signTopY = oy + oh * 0.055;
                    double signBotY = oy + oh * 0.245;
                    double sortTop = (objView.getUserData() instanceof Double) ? ((Double) objView.getUserData()) + 1.5 : (oy + oh + 1.5);
                    for (int bi = 0; bi < 8; bi++) {
                        double bx = signLeft + (signRight - signLeft) * (bi / 7.0);
                        javafx.scene.shape.Circle topBulb = new javafx.scene.shape.Circle(3.2, (bi % 2 == 0) ? Color.web("#FFE066") : Color.web("#FF5252"));
                        topBulb.setManaged(false);
                        topBulb.setMouseTransparent(true);
                        topBulb.setLayoutX(bx);
                        topBulb.setLayoutY(signTopY);
                        topBulb.setUserData(sortTop);

                        javafx.scene.shape.Circle botBulb = new javafx.scene.shape.Circle(3.2, (bi % 2 == 1) ? Color.web("#FFE066") : Color.web("#FF5252"));
                        botBulb.setManaged(false);
                        botBulb.setMouseTransparent(true);
                        botBulb.setLayoutX(bx);
                        botBulb.setLayoutY(signBotY);
                        botBulb.setUserData(sortTop);

                        entityPane.getChildren().addAll(topBulb, botBulb);
                        casinoMarqueeBulbs.add(topBulb);
                        casinoMarqueeBulbs.add(botBulb);
                    }
                }
            }
        }
        if (mainHouseDoor != null) {
            houseDoorPoints.add(0, mainHouseDoor);
        }

        // Thiết lập vùng tương tác AABB bao trùm toàn bộ chiều rộng & chiều cao Cổng Vườn Cây Ăn Quả (+ đệm 48px trước/sau/trái/phải)
        if (orchardGateBounds == null && orchardFenceMaxX > orchardFenceMinX) {
            orchardGateBounds = new javafx.geometry.Rectangle2D(
                orchardFenceMinX,
                orchardFenceMinY,
                orchardFenceMaxX - orchardFenceMinX,
                orchardFenceMaxY - orchardFenceMinY
            );
        }
        if (orchardGateBounds != null) {
            final double padX = 48.0;
            final double padY = 48.0;
            orchardInteractBox = new javafx.geometry.Rectangle2D(
                orchardGateBounds.getMinX() - padX,
                orchardGateBounds.getMinY() - padY,
                orchardGateBounds.getWidth() + padX * 2.0,
                orchardGateBounds.getHeight() + padY * 2.0
            );
            if (!orchardUnlocked && entityPane != null) {
                orchardWorldSignLabel = new Label("🔒 VƯỜN CÂY ĂN QUẢ — Mở khóa: $3.000");
                orchardWorldSignLabel.setMouseTransparent(true);
                orchardWorldSignLabel.setManaged(false);
                orchardWorldSignLabel.setStyle(
                    "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #FEF08A; -fx-background-color: rgba(58, 28, 12, 0.90);" +
                    "-fx-border-color: #F59E0B; -fx-border-width: 1.6px;" +
                    "-fx-padding: 3 9 3 9; -fx-background-radius: 6px; -fx-border-radius: 6px;"
                );
                entityPane.getChildren().add(orchardWorldSignLabel);
                orchardWorldSignLabel.applyCss();
                orchardWorldSignLabel.autosize();
                double signW = orchardWorldSignLabel.getWidth() > 10
                        ? orchardWorldSignLabel.getWidth()
                        : Math.max(248.0, orchardWorldSignLabel.prefWidth(-1));
                double gateCenterX = orchardGateBounds.getMinX() + orchardGateBounds.getWidth() * 0.5;
                orchardWorldSignLabel.setLayoutX(gateCenterX - signW * 0.5);
                orchardWorldSignLabel.setLayoutY(orchardGateBounds.getMinY() - 24.0);
                orchardWorldSignLabel.setUserData(orchardGateBounds.getMaxY() + 12.0);
            }
        }

        // Khởi tạo NpcManager: Nami & Hancock ở bể bơi + 4 Bot Zoro, Sanji, Chopper, Usopp đi dạo
        if (entityPane != null && farmMap != null) {
            npcManager = new view.npc.NpcManager(farmMap, this::isPointBlockedForBot);
            npcManager.attachTo(entityPane, topLoungerX, topLoungerY);
        }

        // Dòng gợi ý nổi phía trên đầu nhân vật
        if (entityPane != null) {
            interactPromptLabel = new Label();
            interactPromptLabel.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 11.5px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF6D6;" +
                "-fx-background-color: rgba(42, 22, 10, 0.90);" +
                "-fx-border-color: #E5AE58;" +
                "-fx-border-width: 1.8px;" +
                "-fx-padding: 3 9 3 9;" +
                "-fx-background-radius: 7px;" +
                "-fx-border-radius: 7px;"
            );
            interactPromptLabel.setMouseTransparent(true);
            interactPromptLabel.setManaged(false);
            interactPromptLabel.setVisible(false);
            interactPromptLabel.setUserData(9995.0);
            entityPane.getChildren().add(interactPromptLabel);

            survivalWarningLabel = new Label();
            survivalWarningLabel.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5F5;" +
                "-fx-background-color: rgba(168, 28, 28, 0.90);" +
                "-fx-border-color: #FFB2B2;" +
                "-fx-border-width: 1.5px;" +
                "-fx-padding: 3 8 3 8;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;"
            );
            survivalWarningLabel.setMouseTransparent(true);
            survivalWarningLabel.setManaged(false);
            survivalWarningLabel.setVisible(false);
            survivalWarningLabel.setUserData(9996.0);
            entityPane.getChildren().add(survivalWarningLabel);
        }
        setupSurvivalHud();
        updateSurvivalHud(animTimeSec);
        updateFoodBuffHud();
        updateQuestTrackerUi();
        spawnDailyCampForageItems();
        registerChoppableTrees();
        mineManager.registerMapObjects(sceneryObjects, entityPane);
        if (orchardUnlocked) {
            applyOrchardUnlockedVisuals();
        }
        if (gameManager != null) {
            marketManager.rollForDay(gameManager.getCurrentDay(), orchardUnlocked);
            festivalManager.onDayStart(gameManager.getCurrentDay());
        }
        syncAllNpcPerks();
        updateFestivalBannerUi();
        notify(marketManager.getMorningBannerText());
    }

    /** Đăng ký tất cả các cây gỗ xung quanh rìa nông trại thành đối tượng TreeObject có thể chặt (HP=4, mọc lại sau 3 ngày). */
    private void registerChoppableTrees() {
        choppableTrees.clear();
        if (sceneryObjects == null) return;
        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id == null) continue;
            String idLow = id.toLowerCase();
            // Chấp nhận mọi cây gỗ quanh nông trại (tree, tree_dark, tree_autumn, tree_blossom, tree_pine, tree_small, tree_bush); giữ nguyên cây dừa palm_tree ở bể bơi
            if (idLow.equals("tree") || idLow.startsWith("tree_")) {
                TreeObject t = new TreeObject(
                    sceneryKey(objView),
                    objView,
                    objView.getImage(),
                    objView.getLayoutX(),
                    objView.getLayoutY(),
                    objView.getFitWidth(),
                    objView.getFitHeight()
                );
                choppableTrees.add(t);
            }
        }
    }

    /** Chặt cây gỗ gần nhất khi cầm Rìu: -1 HP, rung cây, bắn vụn gỗ nâu, -2 Đói / -2 Khát; HP<=0 -> +4 Củi khô & đổi thành gốc cây cụt. */
    private void tryChopNearestTree(double targetX, double targetY) {
        double footX = playerX + 22.0;
        double footY = playerY + 52.0;

        TreeObject bestTree = null;
        double bestDist = Double.MAX_VALUE;
        TreeObject nearStump = null;
        double bestStumpDist = Double.MAX_VALUE;

        for (TreeObject t : choppableTrees) {
            double tx = t.trunkCenterX();
            double ty = t.trunkCenterY();
            double dTarget = Math.hypot(targetX - tx, targetY - ty);
            double dPlayer = Math.hypot(footX - tx, footY - ty);
            double minD = Math.min(dTarget, dPlayer);

            if (!t.chopped) {
                if (dTarget <= 92.0 || dPlayer <= 104.0) {
                    if (minD < bestDist) {
                        bestDist = minD;
                        bestTree = t;
                    }
                }
            } else if (minD <= 92.0 && minD < bestStumpDist) {
                bestStumpDist = minD;
                nearStump = t;
            }
        }

        if (bestTree == null) {
            triggerToolAnimation("AXE", -1, -1);
            if (nearStump != null) {
                int leftDays = Math.max(1, 3 - nearStump.regrowDays);
                spawnFloatingText("Gốc cây đang mọc lại (còn " + leftDays + " ngày)", Color.rgb(236, 206, 134));
                notify("🪵 Gốc cây này đã chặt — sẽ tự mọc lại sau " + leftDays + " ngày nữa!");
            } else {
                notify("🪓 Hãy đi sát các cây gỗ quanh rìa nông trại rồi bấm [Space] để chặt!");
            }
            return;
        }

        double trunkX = bestTree.trunkCenterX();
        double trunkY = bestTree.trunkCenterY();

        // Tự động xoay mặt Luffy hướng về phía thân cây (trái/phải) để nhát bổ rìu 4 frame ăn khớp 100%
        if (Math.abs(trunkX - footX) > 6.0) {
            facing = (trunkX >= footX) ? "RIGHT" : "LEFT";
        }

        bestTree.hp = Math.max(0, bestTree.hp - 1);
        boolean felled = (bestTree.hp <= 0);
        pendingAxeChopFelled = felled;

        // Vung rìu 4 frame (0 -> 1 -> 2 -> 3 trong 0.32s, bắn hạt vụn gỗ ngay tại frame 2) + trừ -2 Đói, -2 Khát
        triggerToolAnimation("AXE", trunkX, trunkY);
        if (player != null) {
            player.addHunger(-2.0);
            player.addThirst(-2.0);
            updateSurvivalHud(animTimeSec);
        }

        // Rung cây nhẹ bằng TranslateTransition (hạt vụn gỗ màu nâu sẽ bắn bung ngay tại frame bổ xuống = frame 2)
        shakeTreeView(bestTree.view);
        if ((Assets.LUFFY_AXE_FRAMES == null || Assets.LUFFY_AXE_FRAMES[0] == null) && particleSystem != null) {
            particleSystem.spawnWoodChipBurst(trunkX, trunkY - 16.0, felled);
        }

        if (felled) {
            bestTree.chopped = true;
            bestTree.regrowDays = 0;
            applyStumpVisual(bestTree);

            if (player != null) {
                player.getInventory().addItem(model.game_item.ForageItem.firewood(), 4);
            }
            view.fx.SoundHook.playPickup();
            spawnFloatingText("+4 Củi khô", Color.rgb(255, 224, 96));
            notify("🪓 Cây đổ rồi! Nhận +4 Củi khô (cây sẽ tự mọc lại sau 3 ngày).");
        } else {
            view.fx.SoundHook.playWoodGateOpen();
            spawnFloatingText("Chặt! (" + bestTree.hp + "/4 HP)", Color.rgb(255, 204, 132));
            notify("🪓 Đang chặt cây... (HP cây còn " + bestTree.hp + "/4 | -2 Đói, -2 Khát)");
        }
    }

    /** Hiệu ứng rung thân cây bằng TranslateTransition mỗi nhát bổ rìu. */
    private void shakeTreeView(ImageView treeView) {
        if (treeView == null) return;
        treeView.setTranslateX(0.0);
        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(42), treeView);
        shake.setFromX(-5.0);
        shake.setToX(5.0);
        shake.setAutoReverse(true);
        shake.setCycleCount(4);
        shake.setOnFinished(e -> treeView.setTranslateX(0.0));
        shake.play();
    }

    /** Chuyển hình cây gỗ đã chặt thành gốc cây cụt ngay tại đúng chân gốc cũ. */
    private void applyStumpVisual(TreeObject t) {
        if (t == null || t.view == null) return;
        Image stumpImg = Assets.STUMP_IMAGE;
        if (stumpImg != null) {
            t.view.setImage(stumpImg);
        }
        double stumpW = Math.min(68.0, Math.max(46.0, t.fullW * 0.42));
        double stumpH = Math.min(58.0, Math.max(38.0, t.fullH * 0.28));
        t.view.setFitWidth(stumpW);
        t.view.setFitHeight(stumpH);
        t.view.setLayoutX(t.fullX + (t.fullW - stumpW) * 0.5);
        t.view.setLayoutY(t.fullY + t.fullH - stumpH);
        t.view.setUserData(t.fullY + t.fullH);
        t.view.setTranslateX(0.0);
    }

    /** Khôi phục cây trưởng thành từ gốc cây cụt sau 3 ngày. */
    private void regrowTreeVisual(TreeObject t) {
        if (t == null || t.view == null) return;
        t.chopped = false;
        t.hp = 4;
        t.regrowDays = 0;
        if (t.fullTreeImage != null) {
            t.view.setImage(t.fullTreeImage);
        }
        t.view.setFitWidth(t.fullW);
        t.view.setFitHeight(t.fullH);
        t.view.setLayoutX(t.fullX);
        t.view.setLayoutY(t.fullY);
        t.view.setUserData(t.fullY + t.fullH);
        t.view.setTranslateX(0.0);
    }

    /** Khi sang ngày mới: tăng tiến độ mọc cho mọi gốc cây cụt; đủ 3 ngày tự mọc lại thành cây trưởng thành. */
    private void regrowChoppedTreesForNewDay() {
        int regrown = 0;
        for (TreeObject t : choppableTrees) {
            if (t.chopped) {
                t.regrowDays++;
                if (t.regrowDays >= 3) {
                    regrowTreeVisual(t);
                    regrown++;
                }
            }
        }
        if (regrown > 0) {
            notify("🌲 " + regrown + " cây gỗ quanh nông trại đã mọc lại trưởng thành!");
        }
    }

    /** Kiểm tra va chạm vật cản tĩnh trên bản đồ cho 4 Bot đi dạo (tránh nhà, hàng rào, bể bơi, cây, đá). */
    private boolean isPointBlockedForBot(double footX, double footY) {
        if (sceneryObjects == null) return false;
        javafx.geometry.Rectangle2D botBox = new javafx.geometry.Rectangle2D(footX - 10.0, footY - 8.0, 24.0, 20.0);
        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id == null) continue;
            String low = id.toLowerCase();
            if (low.isEmpty() || low.contains("grass_patch") || low.contains("dirt") || low.contains("flower") ||
                low.contains("pebble") || low.contains("flag") || low.contains("windmill_blades") ||
                low.contains("bridge") || low.contains("pool_lounger") || low.contains("pool_umbrella")) {
                continue;
            }
            double ox = objView.getLayoutX();
            double oy = objView.getLayoutY();
            double ow = objView.getFitWidth();
            double oh = objView.getFitHeight();
            if (ow <= 0 || oh <= 0) continue;

            double hw = ow * 0.78;
            double hh = low.contains("tree") ? (oh * 0.28) : (low.contains("swimming_pool") ? (oh * 0.92) : (oh * 0.62));
            javafx.geometry.Rectangle2D sBox = new javafx.geometry.Rectangle2D(
                ox + (ow - hw) * 0.5, oy + oh - hh, hw, hh
            );
            if (botBox.intersects(sBox)) {
                return true;
            }
        }
        return false;
    }

    /** Hoạt ảnh gợn sóng lấp lánh của hồ bơi & đèn biển hiệu Casino. */
    private void updatePoolAndCasinoVisuals(double timeSec) {
        for (int i = 0; i < poolWaveLines.size(); i++) {
            javafx.scene.shape.Line w = poolWaveLines.get(i);
            w.setTranslateX(Math.sin(timeSec * 2.2 + i * 1.3) * 8.5);
            w.setOpacity(0.42 + 0.36 * Math.abs(Math.sin(timeSec * 2.8 + i)));
        }
        for (int i = 0; i < poolSparkleDots.size(); i++) {
            javafx.scene.shape.Circle sp = poolSparkleDots.get(i);
            double twinkle = Math.abs(Math.sin(timeSec * 4.5 + i * 1.1));
            sp.setOpacity(0.20 + 0.80 * twinkle);
            double sc = 0.65 + 0.65 * twinkle;
            sp.setScaleX(sc);
            sp.setScaleY(sc);
        }
        int tick = (int) (timeSec * 4.0);
        for (int i = 0; i < casinoMarqueeBulbs.size(); i++) {
            javafx.scene.shape.Circle bulb = casinoMarqueeBulbs.get(i);
            boolean lit = ((i + tick) % 2 == 0);
            bulb.setOpacity(lit ? 1.0 : 0.28);
            bulb.setScaleX(lit ? 1.18 : 0.85);
            bulb.setScaleY(lit ? 1.18 : 0.85);
        }
    }

    // ======================== CHUỖI 1000 NHIỆM VỤ TỰ ĐỘNG (QUEST MANAGER) ========================

    private void onQuestCropHarvested(String cropName) {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onCropHarvested(cropName)) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestFishCaught(model.game_item.FishSpecies species) {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onFishCaught(species)) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestFoodCooked() {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onFoodCooked()) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestAnimalHarvested(String animalKey) {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onAnimalHarvested(animalKey)) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestMoneyEarned(int amount) {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onMoneyEarned(amount)) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestProgressUpdated() {
        updateQuestTrackerUi();
        if (player != null && player.getQuestManager() != null && player.getQuestManager().isCurrentQuestReadyToClaim()) {
            model.quest.Quest q = player.getQuestManager().getCurrentQuest();
            if (q != null) {
                view.fx.SoundHook.playPickup();
                notify("✨ Đã đủ tiến độ " + q.getTitle() + "! Bấm [Nhận Thưởng] ở bảng bên phải để lĩnh +$" + q.getRewardMoney() + "!");
            }
        }
    }

    @FXML
    private void onClaimQuestReward() {
        if (player == null || player.getQuestManager() == null) return;
        model.quest.QuestManager qm = player.getQuestManager();
        model.quest.Quest completed = qm.claimAndAdvance();
        if (completed == null) return;

        int reward = completed.getRewardMoney();
        suppressQuestMoneyTracking = true;
        try {
            player.addMoney(reward);
        } finally {
            suppressQuestMoneyTracking = false;
        }

        if (particleSystem != null) {
            particleSystem.spawnHarvestSparkles(playerX + 22.0, playerY + 18.0);
        }
        view.fx.SoundHook.playHarvest();
        spawnFloatingText("+$" + reward + " | Hoàn thành Nhiệm vụ #" + completed.getIndex() + "!", Color.rgb(255, 232, 92));

        model.quest.Quest nextQ = qm.getCurrentQuest();
        if (nextQ != null) {
            notify("🏆 Hoàn thành Nhiệm vụ #" + completed.getIndex() + " (+$" + reward + ")! Mở tiếp " + nextQ.getTitle() + ": " + nextQ.getDescription());
        } else {
            notify("👑 Chúc mừng! Bạn đã chinh phục trọn vẹn toàn bộ 1000 Nhiệm vụ của Nông trại!");
        }

        updateQuestTrackerUi();
        if (autosave != null) {
            autosave.run();
        }
        Platform.runLater(rootPane::requestFocus);
    }

    private void updateQuestTrackerUi() {
        if (questTitleLabel == null || player == null || player.getQuestManager() == null) return;
        model.quest.QuestManager qm = player.getQuestManager();
        if (qm.isAllCompleted()) {
            questTitleLabel.setText("👑 HOÀN TẤT 1000/1000");
            questDescLabel.setText("Đã hoàn thành mọi nhiệm vụ!");
            questProgressLabel.setText("Tiến độ: 1000 / 1000");
            questRewardLabel.setText("Danh hiệu: Vua Nông Trại");
            if (questClaimButton != null) {
                questClaimButton.setVisible(false);
                questClaimButton.setManaged(false);
            }
            return;
        }

        model.quest.Quest q = qm.getCurrentQuest();
        int prog = qm.getCurrentQuestProgress();
        int target = q.getTargetAmount();
        boolean ready = (prog >= target);

        questTitleLabel.setText(q.getTitle());
        questTitleLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " +
            (q.isSpecialMilestone() ? "#B02A0E;" : "#6B2A0C;")
        );

        questDescLabel.setText(q.getDescription());
        questProgressLabel.setText((ready ? "✓ Đủ: " : "Tiến độ: ") + prog + " / " + target);
        questProgressLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
            (ready ? "#197A2F;" : "#3E210C;")
        );

        questRewardLabel.setText((q.isSpecialMilestone() ? "★ x3 Thưởng: +$" : "Thưởng: +$") + q.getRewardMoney());
        questRewardLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
            (q.isSpecialMilestone() ? "#C43C00;" : "#9A5212;")
        );

        if (questTrackerBox != null) {
            if (ready) {
                questTrackerBox.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFF5CC 0%, #FFE48F 100%);" +
                    "-fx-border-color: #FFA800; -fx-border-width: 3px;" +
                    "-fx-background-radius: 9px; -fx-border-radius: 9px; -fx-padding: 8 9 9 9;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255, 196, 0, 0.55), 10, 0.3, 0, 0);"
                );
            } else {
                questTrackerBox.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #F8E7C6 0%, #E8CE9F 100%);" +
                    "-fx-border-color: " + (q.isSpecialMilestone() ? "#C44D18" : "#6B3C1A") + "; -fx-border-width: 2.5px;" +
                    "-fx-background-radius: 9px; -fx-border-radius: 9px; -fx-padding: 8 9 9 9;"
                );
            }
        }

        if (questClaimButton != null) {
            questClaimButton.setVisible(ready);
            questClaimButton.setManaged(ready);
        }
    }

    /**
     * Dựng lớp cầu gỗ che phía trên khi thuyền chui gầm cầu (giữ nguyên mặt nước sông đồng nhất 100%,
     * không vẽ dải chữ nhật tối lệch màu nước hai bên thành cầu) và lưu tọa độ mặt ván vòm để mô phỏng đi lên/xuống cầu.
     */
    private void setupBridgeUnderpassVisuals(ImageView bridgeObj, double riverTopY, double riverBotY) {
        if (bridgeObj == null || entityPane == null) return;
        double ox = bridgeObj.getLayoutX();
        double oy = bridgeObj.getLayoutY();
        double ow = bridgeObj.getFitWidth();
        double oh = bridgeObj.getFitHeight();
        if (ow <= 0 || oh <= 0) return;

        // Tỉ lệ phần gỗ thực qua lòng sông của bridge.png (240x408): x = 15.5% .. 84.5%
        final double leftFrac = 0.155;
        final double rightFrac = 0.845;
        bridgeWoodLeftX = ox + ow * leftFrac;
        bridgeWoodRightX = ox + ow * rightFrac;

        // Lưu vùng sàn gỗ đi bộ của cầu vòm (đầu ván Bắc y=16.5%, cuối ván Nam y=92.5%) để xử lý độ cong lên/xuống
        bridgeDeckLeftX = ox + ow * 0.240;
        bridgeDeckRightX = ox + ow * 0.760;
        bridgeDeckTopY = oy + oh * 0.165;
        bridgeDeckBotY = oy + oh * 0.925;

        Image src = bridgeObj.getImage();
        if (src != null && src.getPixelReader() != null) {
            int imgW = (int) src.getWidth();
            int imgH = (int) src.getHeight();
            int sx0 = Math.max(0, (int) Math.round(imgW * leftFrac));
            int sx1 = Math.min(imgW, (int) Math.round(imgW * rightFrac));
            int sy0 = Math.max(0, (int) Math.floor(((riverTopY - oy) / oh) * imgH));
            int sy1 = Math.min(imgH, (int) Math.ceil(((riverBotY - oy) / oh) * imgH));
            int cw = Math.max(1, sx1 - sx0);
            javafx.scene.image.WritableImage canopyImg = new javafx.scene.image.WritableImage(cw, imgH);
            javafx.scene.image.PixelReader pr = src.getPixelReader();
            javafx.scene.image.PixelWriter pw = canopyImg.getPixelWriter();
            for (int y = sy0; y < sy1; y++) {
                for (int x = 0; x < cw; x++) {
                    Color col = pr.getColor(sx0 + x, y);
                    pw.setColor(x, y, col);
                }
            }

            bridgeCanopyView = new ImageView(canopyImg);
            bridgeCanopyView.setFitWidth(bridgeWoodRightX - bridgeWoodLeftX);
            bridgeCanopyView.setFitHeight(oh);
            bridgeCanopyView.setLayoutX(bridgeWoodLeftX);
            bridgeCanopyView.setLayoutY(oy);
            bridgeCanopyView.setSmooth(false);
            bridgeCanopyView.setMouseTransparent(true);
            bridgeCanopyView.setVisible(false);
            bridgeCanopyView.setUserData(-999.0);
            entityPane.getChildren().add(bridgeCanopyView);

            // Tách lớp Foreground cho toàn bộ 6 cụm cột cầu (2 cột Bắc, 2 cột giữa, 2 cụm cột đôi Nam) để Y-sort chuẩn với chân nhân vật:
            // - 2 cột lớn đầu cầu Bắc (cắt gọn phần thân trên + đỉnh cột y=0..67 và bệ đá ngoài tay vịn y=68..123)
            addBridgeNorthPillarOverlay(src, ox, oy, ow, oh, true);
            addBridgeNorthPillarOverlay(src, ox, oy, ow, oh, false);
            // - 2 cột giữa thân cầu (y = 215..288 / 408)
            addBridgePillarSlice(src, ox, oy, ow, oh, 30.0 / 240.0, 215.0 / 408.0, 58.0 / 240.0, 288.0 / 408.0, 288.0 / 408.0);
            addBridgePillarSlice(src, ox, oy, ow, oh, 178.0 / 240.0, 215.0 / 408.0, 206.0 / 240.0, 288.0 / 408.0, 288.0 / 408.0);
            // - Cụm cột đôi trái & phải đầu cầu Nam (y = 315..399 / 408, baseY = 398 / 408)
            addBridgePillarSlice(src, ox, oy, ow, oh, 0.0 / 240.0, 315.0 / 408.0, 58.0 / 240.0, 399.0 / 408.0, 398.0 / 408.0);
            addBridgePillarSlice(src, ox, oy, ow, oh, 178.0 / 240.0, 315.0 / 408.0, 240.0 / 240.0, 399.0 / 408.0, 398.0 / 408.0);
        }
    }

    /** Tách riêng thân trên/đỉnh cột + bệ đá của 2 cột lớn đầu cầu Bắc (không cắt đè lên tay vịn gỗ) để che nhân vật khi đứng sau cột. */
    private void addBridgeNorthPillarOverlay(Image src, double ox, double oy, double ow, double oh, boolean leftSide) {
        if (src == null || src.getPixelReader() == null || entityPane == null) return;
        int imgW = (int) src.getWidth();
        int imgH = (int) src.getHeight();
        double x0Frac = leftSide ? (24.0 / 240.0) : (177.0 / 240.0);
        double x1Frac = leftSide ? (59.0 / 240.0) : (212.0 / 240.0);
        double y1Frac = 123.0 / 408.0;

        int sx0 = Math.max(0, (int) Math.round(imgW * x0Frac));
        int sx1 = Math.min(imgW, (int) Math.round(imgW * x1Frac));
        int sy1 = Math.min(imgH, (int) Math.round(imgH * y1Frac));
        int cw = Math.max(1, sx1 - sx0);
        int ch = Math.max(1, sy1);

        javafx.scene.image.WritableImage slice = new javafx.scene.image.WritableImage(cw, ch);
        javafx.scene.image.PixelReader pr = src.getPixelReader();
        javafx.scene.image.PixelWriter pw = slice.getPixelWriter();
        int jointY = (int) Math.round(imgH * (67.0 / 408.0));
        int innerCutX = leftSide
                ? (int) Math.round(imgW * (44.0 / 240.0))
                : (int) Math.round(imgW * (192.0 / 240.0));

        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                int srcX = sx0 + x;
                if (y >= jointY) {
                    if (leftSide && srcX > innerCutX) continue;
                    if (!leftSide && srcX < innerCutX) continue;
                }
                pw.setColor(x, y, pr.getColor(srcX, y));
            }
        }

        ImageView iv = new ImageView(slice);
        iv.setFitWidth(ow * (x1Frac - x0Frac));
        iv.setFitHeight(oh * y1Frac);
        iv.setLayoutX(ox + ow * x0Frac);
        iv.setLayoutY(oy);
        iv.setSmooth(false);
        iv.setManaged(false);
        iv.setMouseTransparent(true);

        double baseY = oy + oh * (122.0 / 408.0);
        iv.setUserData(baseY - 20.0);
        entityPane.getChildren().add(iv);
        bridgePillarOverlays.add(new BridgePillarOverlay(iv, baseY));
    }

    /** Tách 1 vùng chữ nhật cột cầu thành lớp foreground Y-sort theo tọa độ chân cột baseY. */
    private void addBridgePillarSlice(Image src, double ox, double oy, double ow, double oh,
                                      double x0Frac, double y0Frac, double x1Frac, double y1Frac, double baseYFrac) {
        if (src == null || src.getPixelReader() == null || entityPane == null) return;
        int imgW = (int) src.getWidth();
        int imgH = (int) src.getHeight();
        int sx0 = Math.max(0, (int) Math.round(imgW * x0Frac));
        int sx1 = Math.min(imgW, (int) Math.round(imgW * x1Frac));
        int sy0 = Math.max(0, (int) Math.round(imgH * y0Frac));
        int sy1 = Math.min(imgH, (int) Math.round(imgH * y1Frac));
        int cw = Math.max(1, sx1 - sx0);
        int ch = Math.max(1, sy1 - sy0);

        javafx.scene.image.WritableImage slice = new javafx.scene.image.WritableImage(cw, ch);
        javafx.scene.image.PixelReader pr = src.getPixelReader();
        javafx.scene.image.PixelWriter pw = slice.getPixelWriter();
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                pw.setColor(x, y, pr.getColor(sx0 + x, sy0 + y));
            }
        }

        ImageView iv = new ImageView(slice);
        iv.setFitWidth(ow * (x1Frac - x0Frac));
        iv.setFitHeight(oh * (y1Frac - y0Frac));
        iv.setLayoutX(ox + ow * x0Frac);
        iv.setLayoutY(oy + oh * y0Frac);
        iv.setSmooth(false);
        iv.setManaged(false);
        iv.setMouseTransparent(true);

        double baseY = oy + oh * baseYFrac;
        iv.setUserData(baseY - 20.0);
        entityPane.getChildren().add(iv);
        bridgePillarOverlays.add(new BridgePillarOverlay(iv, baseY));
    }

    /**
     * Trả về tọa độ Y thế giới của đường ranh giới giữa MÉP CỎ đi được và MÔ ĐẤT / VÁCH ĐẤT bờ Bắc tại tọa độ X.
     * Mọi vị trí có đáy chân vượt quá đường này (xuống phần vách đất nâu hoặc mặt nước) đều bị chặn tuyệt đối.
     */
    private double getNorthShoreGrassMaxY(double worldX) {
        // Bờ Bắc xuyên suốt toàn bản đồ có lớp viền cỏ rủ (~3.2px) + vách đất nâu (~13px) đổ xuống mặt nước:
        // Chặn chân nhân vật ngay tại ranh giới mép cỏ đi được (worldRiverTopY - 3.2), tuyệt đối không bước lên mô đất nâu.
        double baseGrassMaxY = worldRiverTopY - 3.2;
        if (northAbutmentW > 0.0 && worldX >= northAbutmentX && worldX <= (northAbutmentX + northAbutmentW)) {
            double lx = ((worldX - northAbutmentX) / northAbutmentW) * 320.0;
            double dL = Math.abs(lx - 86.0) / 58.0;
            double lobeL = (dL < 1.0) ? Math.pow(Math.cos(dL * Math.PI * 0.5), 1.35) : 0.0;
            double dR = Math.abs(lx - 234.0) / 58.0;
            double lobeR = (dR < 1.0) ? Math.pow(Math.cos(dR * Math.PI * 0.5), 1.35) : 0.0;
            double dC = Math.abs(lx - 160.0) / 64.0;
            double bridgeSpanFill = (dC < 1.0) ? 0.42 * Math.cos(dC * Math.PI * 0.5) : 0.0;
            double bulge = Math.max(Math.max(lobeL, lobeR), bridgeSpanFill);
            if (bulge > 0.03) {
                double localGrassEdgeY = 44.0 + bulge * 21.0;
                return northAbutmentY + (localGrassEdgeY / 80.0) * northAbutmentH;
            }
        }
        return baseGrassMaxY;
    }

    /**
     * Kiểm tra Foot Hitbox (đế dép nhân vật) có đè lên phần mô đất/vách đất bờ sông hoặc mặt nước hay không.
     * Chỉ cho phép đi qua lòng sông khi TOÀN BỘ bề ngang bàn chân nằm lọt trên mặt ván cầu gỗ.
     */
    private boolean isFootBoxBlockedByRiverOrCliff(javafx.geometry.Rectangle2D footBox,
                                                   javafx.geometry.Rectangle2D bridgeDeck) {
        boolean fullyOnBridgeDeck = bridgeDeck != null
                && footBox.getMinX() >= bridgeDeck.getMinX()
                && footBox.getMaxX() <= bridgeDeck.getMaxX()
                && footBox.getMinY() >= bridgeDeck.getMinY()
                && footBox.getMaxY() <= bridgeDeck.getMaxY();
        if (fullyOnBridgeDeck) {
            return false;
        }

        double[] sampleXs = {
            footBox.getMinX(),
            (footBox.getMinX() + footBox.getMaxX()) * 0.5,
            footBox.getMaxX()
        };
        double[] sampleYs = {
            footBox.getMinY(),
            footBox.getMaxY()
        };

        for (double fx : sampleXs) {
            double northGrassLimitY = getNorthShoreGrassMaxY(fx);
            for (double fy : sampleYs) {
                // Chặn tuyệt đối ngay từ ranh giới mép cỏ -> vách đất bờ Bắc xuyên suốt toàn bộ lòng sông tới mép cỏ bờ Nam
                if (fy > northGrassLimitY && fy < worldRiverBotY + 2.0) {
                    return true;
                }
                // Chặn các ô WATER khác ngoài vùng mố cỏ bờ Bắc (nếu có)
                if (fy <= northGrassLimitY) {
                    continue;
                }
                int col = (int) (fx / Constant.CELL_SIZE);
                int row = (int) (fy / Constant.CELL_SIZE);
                Cell c = (farmMap != null) ? farmMap.getCell(row, col) : null;
                if (c != null && c.getTerrain() == TerrainType.WATER) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Kiểm tra bàn chân nhân vật có đang đứng trên mặt ván cầu gỗ vòm hay không. */
    private boolean isPlayerOnBridge() {
        if (bridgeDeckBotY <= bridgeDeckTopY) return false;
        double footX = playerX + 22.0;
        double footY = playerY + 66.0;
        return footX >= bridgeDeckLeftX && footX <= bridgeDeckRightX
                && footY >= bridgeDeckTopY && footY <= bridgeDeckBotY;
    }

    private boolean isBoatCenterUnderBridge() {
        if (boatEntity == null || bridgeWoodLeftX < 0 || bridgeWoodRightX <= bridgeWoodLeftX) return false;
        double boatCenter = boatEntity.getBoatX() + view.river.BoatEntity.BOAT_WIDTH * 0.5;
        return boatCenter > (bridgeWoodLeftX - 24.0) && boatCenter < (bridgeWoodRightX + 24.0);
    }

    /**
     * Khi thuyền chạm vào vùng gầm cầu: đưa lớp cầu gỗ lên trên thuyền + nhân vật (Z-order cao hơn)
     * và tạo hiệu ứng bóng râm tối dần mượt mà như đang chui thật sự dưới gầm cầu.
     */
    private void updateBoatUnderBridgeEffect(boolean lockedOnBoat) {
        // Cập nhật Y-sorting động cho 6 cụm cột cầu dựa trên so sánh playerFootY (đáy dép = playerY + 68) với pillarBaseY
        if (!bridgePillarOverlays.isEmpty()) {
            double playerFootY = playerY + 68.0;
            for (BridgePillarOverlay po : bridgePillarOverlays) {
                boolean behind = !lockedOnBoat && (playerFootY < po.baseY);
                if (behind != po.lastPlayerBehind) {
                    po.lastPlayerBehind = behind;
                    po.view.setUserData(behind ? (po.baseY - 16.0) : (po.baseY - 20.0));
                }
            }
        }

        if (boatEntity == null || bridgeCanopyView == null || bridgeWoodLeftX < 0) return;

        double boatLeft = boatEntity.getBoatX() + 8.0;
        double boatRight = boatEntity.getBoatX() + view.river.BoatEntity.BOAT_WIDTH - 8.0;
        double overlap = Math.min(boatRight, bridgeWoodRightX) - Math.max(boatLeft, bridgeWoodLeftX);
        boolean boatUnderBridge = (overlap > 0.0);

        if (boatUnderBridge) {
            double boatSpan = Math.max(1.0, boatRight - boatLeft);
            double shadeRatio = Math.max(0.0, Math.min(1.0, overlap / (boatSpan * 0.65)));

            // Chỉ che phía trên thuyền & nhân vật khi thuyền đi dưới cầu (khi đi bộ trên cầu không bao giờ che nhân vật)
            bridgeCanopyView.setVisible(true);
            bridgeCanopyView.setUserData(boatEntity.getBoardedPlayerSortY() + 14.0);
            boatEntity.setUnderBridgeShade(shadeRatio);

            if (lockedOnBoat && playerCharacter != null) {
                if (Math.abs(shadeRatio - lastPlayerBridgeShade) >= 0.015) {
                    lastPlayerBridgeShade = shadeRatio;
                    underBridgePlayerShade.setBrightness(-0.38 * shadeRatio);
                    underBridgePlayerShade.setSaturation(-0.16 * shadeRatio);
                }
                if (playerCharacter.getEffect() != underBridgePlayerShade) {
                    playerCharacter.setEffect(underBridgePlayerShade);
                }
            } else if (playerCharacter != null && playerCharacter.getEffect() == underBridgePlayerShade) {
                playerCharacter.setEffect(null);
                lastPlayerBridgeShade = 0.0;
            }
        } else {
            if (bridgeCanopyView.isVisible()) {
                bridgeCanopyView.setVisible(false);
                bridgeCanopyView.setUserData(-999.0);
            }
            boatEntity.setUnderBridgeShade(0.0);
            if (playerCharacter != null && playerCharacter.getEffect() == underBridgePlayerShade) {
                playerCharacter.setEffect(null);
                lastPlayerBridgeShade = 0.0;
            }
        }
    }

    private double[] findNearestPoint(java.util.List<double[]> pts, double px, double py, double maxDist) {
        double[] best = null;
        double bestDistSq = maxDist * maxDist;
        for (double[] pt : pts) {
            double dx = px - pt[0];
            double dy = py - pt[1];
            double d2 = dx * dx + dy * dy;
            if (d2 <= bestDistSq) {
                bestDistSq = d2;
                best = pt;
            }
        }
        return best;
    }

    private void updateNearbyInteractPrompt(boolean lockedOnBoat) {
        if (interactPromptLabel == null) return;
        if (lockedOnBoat || router.hasOverlay() || sleepTransitionRunning) {
            interactPromptLabel.setVisible(false);
            if (survivalWarningLabel != null) survivalWarningLabel.setVisible(false);
            return;
        }

        String promptText = null;
        if (isSunbathing) {
            promptText = "☀ Đang tắm nắng thư giãn... (Bấm phím di chuyển để đứng dậy)";
        } else if (isSitting) {
            promptText = isCampfireExtinguished()
                    ? "🌧 Lửa trại đã tắt vì trời mưa  |  [U] Đứng dậy"
                    : "[E] Nấu ăn  |  [U] Đứng dậy";
        } else {
            double px = playerX + 22.0;
            double py = playerY + 52.0;
            double bestDist = Double.MAX_VALUE;

            double[] nearDoor = findNearestPoint(houseDoorPoints, px, py, 75.0);
            if (nearDoor != null) {
                double d = Math.hypot(px - nearDoor[0], py - nearDoor[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Đi ngủ để qua ngày";
                }
            }

            double[] nearFire = findNearestPoint(campfirePoints, px, py, 85.0);
            if (nearFire != null) {
                double d = Math.hypot(px - nearFire[0], py - nearFire[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = isCampfireExtinguished()
                            ? "🌧 Trời mưa — Lửa trại đã bị dập tắt (chờ trời nắng)"
                            : "[C] Ngồi xuống bếp lửa";
                }
            }

            CampForageSpot nearForage = findNearestForageSpot(px, py, 76.0);
            if (nearForage != null) {
                double d = Math.hypot(px - nearForage.x, py - nearForage.y);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Nhặt " + nearForage.item.getName();
                }
            }

            double[] nearPool = findNearestPoint(poolSunbathePoints, px, py, 75.0);
            if (nearPool != null) {
                double d = Math.hypot(px - nearPool[0], py - nearPool[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Nằm tắm nắng & thư giãn cùng Nami & Hancock";
                }
            }

            double[] nearCasino = findNearestPoint(casinoDoorPoints, px, py, 85.0);
            if (nearCasino != null) {
                double d = Math.hypot(px - nearCasino[0], py - nearCasino[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Vào Sòng Bạc Đánh Bài";
                }
            }

            double[] nearFactory = findNearestPoint(factoryDoorPoints, px, py, 85.0);
            if (nearFactory != null) {
                double d = Math.hypot(px - nearFactory[0], py - nearFactory[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Vào Nhà Máy Sản Xuất";
                }
            }

            if (isPlayerTouchingOrchardGateZone()) {
                double d = distanceToOrchardGateDoorway(px, py);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Mua mở khóa ($3.000)";
                }
            }

            double[] nearAnvil = findNearestPoint(mineManager.getAnvilPoints(), px, py, 82.0);
            if (nearAnvil != null) {
                double d = Math.hypot(px - nearAnvil[0], py - nearAnvil[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Đổi 2 Sắt + 1 Vàng ➡ Vòi Tưới Tự Động";
                }
            }

            if (npcManager != null) {
                view.npc.NpcManager.NpcTarget nearNpc = npcManager.findNearestNpc(px, py, 82.0);
                if (nearNpc != null) {
                    double d = Math.hypot(px - (nearNpc.x + 22.0), py - (nearNpc.y + 54.0));
                    if (d < bestDist) {
                        bestDist = d;
                        promptText = "[E] Trò chuyện với " + nearNpc.name + "  |  [G] Tặng quà";
                    }
                }
            }

            if (promptText == null && isNearRiverBankForDrink()) {
                promptText = "[R] Uống nước sông";
            }
        }

        boolean promptShown = false;
        double promptOffsetY = isSunbathing ? 82.0 : 48.0;
        if (promptText != null) {
            if (!promptText.equals(interactPromptLabel.getText())) {
                interactPromptLabel.setText(promptText);
                interactPromptLabel.applyCss();
                interactPromptLabel.autosize();
            }
            double prefW = interactPromptLabel.prefWidth(-1);
            double w = interactPromptLabel.getWidth() > 10
                    ? interactPromptLabel.getWidth()
                    : (prefW > 10 ? prefW : Math.max(196.0, promptText.length() * 6.8));
            boolean anchorToOrchardGate = !orchardUnlocked
                    && orchardGateBounds != null
                    && "[E] Mua mở khóa ($3.000)".equals(promptText);
            if (anchorToOrchardGate) {
                double gateCenterX = orchardGateBounds.getMinX() + orchardGateBounds.getWidth() * 0.5;
                interactPromptLabel.setLayoutX(gateCenterX - w * 0.5);
                interactPromptLabel.setLayoutY(orchardGateBounds.getMinY() + orchardGateBounds.getHeight() * 0.42);
            } else {
                interactPromptLabel.setLayoutX(playerX + 22.0 - w * 0.5);
                interactPromptLabel.setLayoutY(playerY - promptOffsetY);
            }
            interactPromptLabel.setVisible(true);
            promptShown = !anchorToOrchardGate;
        } else {
            interactPromptLabel.setVisible(false);
        }

        // Hiện bong bóng nhắc nhở phía trên đầu khi Đói < 30% hoặc Khát < 30%
        if (survivalWarningLabel != null && player != null) {
            double h = player.getHunger();
            double t = player.getThirst();
            String warn = null;
            if (h < 30.0 && t < 30.0) {
                warn = (h <= t) ? "Đói quá! Đi nấu ăn thôi!" : "Khát quá! Ra sông uống nước!";
            } else if (h < 30.0) {
                warn = "Đói quá! Đi nấu ăn thôi!";
            } else if (t < 30.0) {
                warn = "Khát quá! Ra sông uống nước!";
            }
            if (warn != null) {
                if (!warn.equals(survivalWarningLabel.getText())) {
                    survivalWarningLabel.setText(warn);
                    survivalWarningLabel.autosize();
                }
                double ww = survivalWarningLabel.getWidth() > 0 ? survivalWarningLabel.getWidth() : 206.0;
                survivalWarningLabel.setLayoutX(playerX + 22.0 - ww * 0.5);
                survivalWarningLabel.setLayoutY(playerY - (promptShown ? (promptOffsetY + 28.0) : promptOffsetY));
                survivalWarningLabel.setVisible(true);
            } else {
                survivalWarningLabel.setVisible(false);
            }
        }
    }

    /**
     * Xử lý phím [E] ưu tiên theo khoảng cách gần nhất giữa:
     * 1=Cửa nhà (ngủ), 2=Đống lửa (nấu ăn), 3=Thuyền, 4=Bể bơi tắm nắng, 5=Sòng bạc Casino, 6=Bot đi dạo.
     */
    private void handleInteractKeyE() {
        if (activeCookingOverlay != null) {
            return;
        }
        if (activeCasinoOverlay != null) {
            activeCasinoOverlay.requestSafeClose();
            return;
        }
        if (router.hasOverlay() || sleepTransitionRunning) return;

        if (isSunbathing) {
            stopSunbathing();
            return;
        }

        // Đang ngồi cạnh bếp lửa -> mở ngay màn hình Ghép Đồ Ăn kiểu Minecraft!
        if (isSitting) {
            openCookingOverlay(sittingFireX, sittingFireY);
            return;
        }

        // Đang ngồi trên thuyền -> xuống thuyền ngay (chặn nếu đang chui ngay dưới gầm cầu)
        if (boatEntity != null && (isOnBoat || boatEntity.isPlayerAboard())) {
            if (isBoatCenterUnderBridge()) {
                notify("Đang ở dưới gầm cầu — hãy chèo qua hẳn bên kia rồi bấm [E] lên bờ!");
                return;
            }
            if (fishingGame != null) fishingGame.cancel();
            boatEntity.startDisembarking();
            return;
        }

        double px = playerX + 22.0;
        double py = playerY + 52.0;

        double bestDist = Double.MAX_VALUE;
        int bestAction = 0; // 1=sleep, 2=cook(warn sit), 3=boat, 4=pool, 5=casino, 6=npc, 7=forage, 8=factory, 9=orchardUnlock, 10=anvil
        double[] bestPt = null;
        view.npc.NpcManager.NpcTarget bestNpc = null;
        CampForageSpot bestForage = null;

        double[] nearDoor = findNearestPoint(houseDoorPoints, px, py, 75.0);
        if (nearDoor != null) {
            double d = Math.hypot(px - nearDoor[0], py - nearDoor[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 1;
                bestPt = nearDoor;
            }
        }

        double[] nearFire = findNearestPoint(campfirePoints, px, py, 85.0);
        if (nearFire != null) {
            double d = Math.hypot(px - nearFire[0], py - nearFire[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 2;
                bestPt = nearFire;
            }
        }

        CampForageSpot nearForage = findNearestForageSpot(px, py, 76.0);
        if (nearForage != null) {
            double d = Math.hypot(px - nearForage.x, py - nearForage.y);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 7;
                bestForage = nearForage;
            }
        }

        if (boatEntity != null && boatEntity.canInteractFromShore(playerX, playerY)) {
            double d = Math.hypot(px - boatEntity.getBoardedPlayerX(), py - boatEntity.getSyncedPlayerY());
            if (d < bestDist) {
                bestDist = d;
                bestAction = 3;
            }
        }

        double[] nearPool = findNearestPoint(poolSunbathePoints, px, py, 75.0);
        if (nearPool != null) {
            double d = Math.hypot(px - nearPool[0], py - nearPool[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 4;
                bestPt = nearPool;
            }
        }

        double[] nearCasino = findNearestPoint(casinoDoorPoints, px, py, 85.0);
        if (nearCasino != null) {
            double d = Math.hypot(px - nearCasino[0], py - nearCasino[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 5;
                bestPt = nearCasino;
            }
        }

        double[] nearFactory = findNearestPoint(factoryDoorPoints, px, py, 85.0);
        if (nearFactory != null) {
            double d = Math.hypot(px - nearFactory[0], py - nearFactory[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 8;
                bestPt = nearFactory;
            }
        }

        if (isPlayerTouchingOrchardGateZone()) {
            double d = distanceToOrchardGateDoorway(px, py);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 9;
            }
        }

        double[] nearAnvil = findNearestPoint(mineManager.getAnvilPoints(), px, py, 82.0);
        if (nearAnvil != null) {
            double d = Math.hypot(px - nearAnvil[0], py - nearAnvil[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 10;
                bestPt = nearAnvil;
            }
        }

        if (npcManager != null) {
            view.npc.NpcManager.NpcTarget nearNpc = npcManager.findNearestNpc(px, py, 82.0);
            if (nearNpc != null) {
                double d = Math.hypot(px - (nearNpc.x + 22.0), py - (nearNpc.y + 54.0));
                if (d < bestDist) {
                    bestDist = d;
                    bestAction = 6;
                    bestNpc = nearNpc;
                }
            }
        }

        if (bestAction == 1 && bestPt != null) {
            openSleepDialog(bestPt[0], bestPt[1]);
        } else if (bestAction == 2 && bestPt != null) {
            if (isCampfireExtinguished()) {
                spawnFloatingText("🌧 Trời mưa — Lửa trại đã bị dập tắt!", Color.rgb(147, 197, 253));
                view.fx.SoundHook.playMenuClick();
                notify("🌧 Trời đang mưa nên lửa trại bị tắt! Chờ trời nắng lửa cháy lại rồi nấu ăn nhé.");
            } else {
                // Ràng buộc bắt buộc: phải bấm [C] ngồi xuống trước khi nấu ăn!
                spawnFloatingText("Hãy ấn [C] ngồi xuống trước khi nấu ăn!", Color.rgb(255, 224, 72));
                view.fx.SoundHook.playMenuClick();
                notify("Hãy ấn [C] ngồi xuống bếp lửa trước khi nấu ăn!");
            }
        } else if (bestAction == 7 && bestForage != null) {
            collectCampForageSpot(bestForage);
        } else if (bestAction == 8) {
            enterFactoryWithFade();
        } else if (bestAction == 9) {
            tryUnlockOrchardZone();
        } else if (bestAction == 10) {
            if (view.mine.MineManager.craftAutoSprinkler(player, this::notify)) {
                spawnFloatingText("+1 Vòi Tưới Tự Động! (Bấm [V] trên ruộng để đặt)", Color.rgb(125, 211, 252));
            }
        } else if (bestAction == 3 && boatEntity != null) {
            boatEntity.startBoarding(playerX, playerY);
        } else if (bestAction == 4) {
            startSunbathing();
        } else if (bestAction == 5) {
            openCasinoOverlay();
        } else if (bestAction == 6 && bestNpc != null) {
            int curDay = (gameManager != null) ? gameManager.getCurrentDay() : 1;
            npcManager.interactWithTarget(
                bestNpc,
                px,
                curDay,
                player,
                particleSystem,
                txt -> spawnFloatingText(txt, Color.rgb(144, 255, 164)),
                this::notify
            );
        } else if (isNearRiverBankForDrink()) {
            tryDrinkRiverWater();
        }
    }

    /** Nằm tắm nắng cạnh bể bơi cùng Nami & Hancock: đổi sang ảnh Luffy nằm ngửa trên ghế + hồi Đói/Khát + Buff +20% tốc độ chạy (2 phút). */
    private void startSunbathing() {
        isSunbathing = true;
        sunbatheSparkleTimer = 0.0;
        activeKeys.clear();
        // Căn chỉnh đúng thân Luffy nằm dọc gọn gàng trên ghế tắm nắng (83.2 x 115.2)
        playerX = sunbatheLoungerX + 19.0;
        playerY = sunbatheLoungerY + 46.0;
        facing = "DOWN";
        luffyFacingLeft = false;
        if (playerCharacter != null) {
            playerCharacter.setTranslateX(playerX);
            playerCharacter.setTranslateY(playerY);
        }
        if (luffySpriteView != null && Assets.LUFFY_SUNBATHE != null) {
            luffySpriteView.setFitWidth(49.0);
            luffySpriteView.setFitHeight(111.0);
            luffySpriteView.setX(-2.5);
            luffySpriteView.setY(-45.0);
            luffySpriteView.setScaleX(1.0);
            luffySpriteView.setImage(Assets.LUFFY_SUNBATHE);
        }
        boolean hancockPerk = (npcManager != null && npcManager.hasPerkUnlocked("hancock"));
        poolRelaxSpeedBuffSec = hancockPerk ? 240.0 : 120.0;
        lastRenderedRelaxSec = (int) poolRelaxSpeedBuffSec;
        updateFoodBuffHud();
        view.fx.SoundHook.playHeart();
        if (particleSystem != null) {
            particleSystem.spawnHeartBurst(playerX + 22.0, playerY + 6.0);
        }
        String durTxt = hancockPerk ? "4 phút (Đặc quyền Hancock 3❤)!" : "2 phút!";
        spawnFloatingText("☀ Thư giãn +20% tốc độ chạy (" + durTxt + ")", Color.rgb(255, 224, 102));
        notify("☀ Đang nằm tắm nắng cạnh Nami & Hancock! Hồi Đói/Khát" + (hancockPerk ? " NHANH GẤP ĐÔI" : "") + " & Buff +20% tốc độ chạy " + durTxt);
    }

    private void stopSunbathing() {
        if (!isSunbathing) return;
        isSunbathing = false;
        if (luffyBodyRotate != null) luffyBodyRotate.setAngle(0.0);
        if (luffySpriteView != null) {
            luffySpriteView.setFitWidth(108.0);
            luffySpriteView.setFitHeight(108.0);
            luffySpriteView.setX(-32.0);
            luffySpriteView.setY(-38.0);
            luffySpriteView.setTranslateY(0.0);
        }
        notify("Đã đứng dậy khỏi ghế tắm nắng! (Buff +20% tốc độ chạy còn " + (int) Math.ceil(poolRelaxSpeedBuffSec) + "s)");
        triggerAutosave();
    }

    /** Mở giao diện Sòng Bạc Đánh Bài (Xì Dách 21 & Đoán Bài Cao/Thấp). */
    private void openCasinoOverlay() {
        if (router == null || router.hasOverlay() || sleepTransitionRunning) return;
        activeKeys.clear();
        activeCasinoOverlay = new view.ui.CasinoOverlay(
            player,
            this::onQuestMoneyEarned,
            this::notify,
            this::closeCasinoOverlay
        );
        router.showOverlay(activeCasinoOverlay);
        Platform.runLater(activeCasinoOverlay::requestFocus);
    }

    private void closeCasinoOverlay() {
        if (activeCasinoOverlay != null) {
            activeCasinoOverlay = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            if (autosave != null) {
                autosave.run();
            }
            Platform.runLater(rootPane::requestFocus);
        }
    }

    /** Hộp thoại xác nhận đi ngủ phong cách gỗ/pixel trước cửa nhà. */
    private void openSleepDialog(double doorX, double doorY) {
        if (router == null || router.hasOverlay() || sleepTransitionRunning) return;
        activeKeys.clear();

        javafx.scene.layout.StackPane scrim = new javafx.scene.layout.StackPane();
        scrim.setStyle("-fx-background-color: rgba(14, 10, 8, 0.62);");
        scrim.setAlignment(Pos.CENTER);
        scrim.setFocusTraversable(true);
        scrim.setOnMouseClicked(ev -> {
            if (ev.getTarget() == scrim) closeSleepDialog();
        });
        scrim.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                closeSleepDialog();
            }
        });

        javafx.scene.layout.VBox woodBox = new javafx.scene.layout.VBox(14);
        woodBox.setAlignment(Pos.CENTER);
        woodBox.setMaxWidth(450);
        woodBox.setPrefWidth(450);
        woodBox.setMaxHeight(245);
        woodBox.setPadding(new javafx.geometry.Insets(20, 24, 20, 24));
        woodBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #7A4B2A 0%, #563218 100%);" +
            "-fx-border-color: #2B1508;" +
            "-fx-border-width: 4px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.25, 0, 6);"
        );

        Label title = new Label("🌙 NGHỈ NGƠI TẠI NHÀ");
        title.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE9A8;"
        );

        javafx.scene.layout.VBox inner = new javafx.scene.layout.VBox(6);
        inner.setAlignment(Pos.CENTER);
        inner.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));
        inner.setStyle(
            "-fx-background-color: #F6E6C4;" +
            "-fx-border-color: #8C5A2B;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );

        Label question = new Label("Bạn có muốn đi ngủ để sang ngày mới không?");
        question.setWrapText(true);
        question.setAlignment(Pos.CENTER);
        question.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #3A1F0C;"
        );

        Label note = new Label("Cây trồng, vật nuôi & thời tiết sẽ sang Ngày " + (gameManager.getCurrentDay() + 1) + " (06:00) và tự lưu game.");
        note.setWrapText(true);
        note.setAlignment(Pos.CENTER);
        note.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11.5px;" +
            "-fx-text-fill: #6E4422;"
        );
        inner.getChildren().addAll(question, note);

        javafx.scene.control.Button sleepBtn = new javafx.scene.control.Button("🛏 Đi ngủ");
        sleepBtn.setFocusTraversable(false);
        sleepBtn.setPrefSize(136, 38);
        sleepBtn.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #FFFDF2;" +
            "-fx-background-color: linear-gradient(to bottom, #4EA44E, #2E7A34);" +
            "-fx-border-color: #19471D; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        sleepBtn.setOnAction(ev -> {
            closeSleepDialog();
            startSleepTransition(doorX, doorY);
        });

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Hủy");
        cancelBtn.setFocusTraversable(false);
        cancelBtn.setPrefSize(116, 38);
        cancelBtn.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #FFF3D8;" +
            "-fx-background-color: linear-gradient(to bottom, #9A6338, #704322);" +
            "-fx-border-color: #3B1E0B; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(ev -> closeSleepDialog());

        javafx.scene.layout.HBox btns = new javafx.scene.layout.HBox(16, sleepBtn, cancelBtn);
        btns.setAlignment(Pos.CENTER);

        woodBox.getChildren().addAll(title, inner, btns);
        scrim.getChildren().add(woodBox);

        activeSleepDialog = scrim;
        router.showOverlay(scrim);
        Platform.runLater(scrim::requestFocus);
    }

    private void closeSleepDialog() {
        if (activeSleepDialog != null) {
            activeSleepDialog = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            Platform.runLater(rootPane::requestFocus);
        }
    }

    /** Nhắm mắt ngủ rồi tỉnh dậy (~3s): tối dần về đen 1.2s -> chuyển ngày, giữ đen 0.5s -> sáng lại 1.3s trước cửa nhà. */
    private void startSleepTransition(double wakeDoorX, double wakeDoorY) {
        if (sleepTransitionRunning || rootPane == null) return;
        sleepTransitionRunning = true;
        activeKeys.clear();
        if (interactPromptLabel != null) interactPromptLabel.setVisible(false);

        int upcomingDay = gameManager.getCurrentDay() + 1;

        javafx.scene.layout.StackPane curtain = new javafx.scene.layout.StackPane();
        curtain.setManaged(false);
        curtain.setLayoutX(0);
        curtain.setLayoutY(0);
        // curtain là node unmanaged nên layout không tự co giãn theo rootPane (prefWidth bind không có tác dụng,
        // khiến màn đen có kích thước 0 và không thấy fade). Phải tự resize theo kích thước rootPane.
        Runnable fitCurtain = () -> curtain.resize(rootPane.getWidth(), rootPane.getHeight());
        javafx.beans.value.ChangeListener<Number> curtainSizeListener = (obs, o, n) -> fitCurtain.run();
        rootPane.widthProperty().addListener(curtainSizeListener);
        rootPane.heightProperty().addListener(curtainSizeListener);
        fitCurtain.run();
        curtain.setStyle("-fx-background-color: rgb(6, 6, 12);");
        curtain.setOpacity(0.0);

        Label zzzLabel = new Label("Đang ngủ... Zzz");
        zzzLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 30px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF4D2;"
        );
        Label dayText = new Label("Ngày " + upcomingDay);
        dayText.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFD966;"
        );

        javafx.scene.layout.VBox centerMsg = new javafx.scene.layout.VBox(10, zzzLabel, dayText);
        centerMsg.setAlignment(Pos.CENTER);
        curtain.getChildren().add(centerMsg);
        rootPane.getChildren().add(curtain);
        curtain.toFront();

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), curtain);
        fadeOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);
        fadeOut.setFromValue(0.0);
        fadeOut.setToValue(1.0);
        fadeOut.setOnFinished(ev -> {
            // Đặt nhân vật đứng ngay trước cửa nhà buổi sáng 06:00 rồi gọi logic chuyển ngày chuẩn
            playerX = wakeDoorX - 22.0;
            playerY = wakeDoorY - 46.0;
            facing = "DOWN";
            if (playerCharacter != null) {
                playerCharacter.setTranslateX(playerX);
                playerCharacter.setTranslateY(playerY);
            }
            advanceDayCore(false, true);
            updateTimeDisplay();
            dayText.setText("Ngày " + gameManager.getCurrentDay() + "  •  06:00 Sáng");

            javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.0));
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), curtain);
            fadeIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            fadeIn.setFromValue(1.0);
            fadeIn.setToValue(0.0);
            fadeIn.setOnFinished(done -> {
                rootPane.widthProperty().removeListener(curtainSizeListener);
                rootPane.heightProperty().removeListener(curtainSizeListener);
                rootPane.getChildren().remove(curtain);
                sleepTransitionRunning = false;
                activeKeys.clear();
                Platform.runLater(rootPane::requestFocus);
            });
            new javafx.animation.SequentialTransition(hold, fadeIn).play();
        });
        fadeOut.play();
    }

    /** Ngồi xuống cạnh đống lửa trại bằng phím [C] (bắt buộc trước khi mở giao diện ghép nấu ăn). */
    private void trySitAtCampfire() {
        if (isSitting) {
            notify("Bạn đang ngồi ở bếp lửa rồi! Bấm [E] để nấu ăn hoặc [U] để đứng dậy.");
            return;
        }
        double px = playerX + 22.0;
        double py = playerY + 52.0;
        double[] nearFire = findNearestPoint(campfirePoints, px, py, 85.0);
        if (nearFire == null) {
            spawnFloatingText("Hãy lại gần đống lửa trại rồi bấm [C] để ngồi!", Color.rgb(255, 224, 96));
            return;
        }
        if (isCampfireExtinguished()) {
            spawnFloatingText("🌧 Trời đang mưa, lửa trại đã bị dập tắt!", Color.rgb(147, 197, 253));
            view.fx.SoundHook.playMenuClick();
            notify("🌧 Trời đang mưa nên lửa trại bị tắt! Chờ trời nắng lửa cháy lại nhé.");
            return;
        }

        isSitting = true;
        sittingFireX = nearFire[0];
        sittingFireY = nearFire[1];
        activeKeys.clear();

        // Quay mặt nhân vật hướng thẳng về phía đống lửa cho chân thực
        double ddx = nearFire[0] - px;
        double ddy = nearFire[1] - py;
        if (Math.abs(ddx) > Math.abs(ddy)) {
            facing = (ddx >= 0) ? "RIGHT" : "LEFT";
        } else {
            facing = (ddy >= 0) ? "DOWN" : "UP";
        }
        view.fx.SoundHook.playMenuClick();
        notify("🔥 Đã ngồi xuống bếp lửa! Bấm [E] để mở Bàn Ghép Nấu Ăn hoặc [U] để đứng dậy.");
    }

    /** Đứng dậy khỏi bếp lửa bằng phím [U], khôi phục tư thế đứng và mở khóa W/A/S/D. */
    private void standUpFromCampfire() {
        if (!isSitting) return;
        isSitting = false;
        activeKeys.clear();
        if (luffySpriteView != null) {
            luffySpriteView.setScaleY(1.0);
            luffySpriteView.setTranslateY(0.0);
        }
        view.fx.SoundHook.playMenuClick();
        notify("Đã đứng dậy khỏi bếp lửa!");
    }

    /** Sinh 5 vật phẩm tự nhiên (Củi khô, Nấm dại, Thảo mộc) quanh khu vực trại/đống lửa mỗi ngày. */
    private void spawnDailyCampForageItems() {
        if (entityPane == null) return;
        for (CampForageSpot old : campForageSpots) {
            entityPane.getChildren().remove(old.node);
        }
        campForageSpots.clear();
        if (campfirePoints.isEmpty()) return;

        double[] fire = campfirePoints.get(0);
        // 7 vị trí bãi cỏ thoáng xung quanh lều trại & đống lửa (ưu tiên dễ thấy, không chạm lửa/lều)
        double[][] candidateOffsets = {
            { -112.0,  58.0 },
            {  108.0,  54.0 },
            {  -56.0, 108.0 },
            {   64.0, 114.0 },
            { -154.0,  96.0 },
            {  162.0,  92.0 },
            {    6.0, 134.0 }
        };
        int day = (gameManager != null) ? gameManager.getCurrentDay() : 1;
        java.util.Random rng = new java.util.Random(day * 7919L + 2026L);

        // Xáo trộn vị trí mỗi ngày nhưng luôn đảm bảo đủ: 2 Củi khô + 1-2 Nấm dại + 1-2 Thảo mộc
        java.util.List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < candidateOffsets.length; i++) order.add(i);
        java.util.Collections.shuffle(order, rng);

        model.game_item.ForageItem[] dailyLoot = new model.game_item.ForageItem[] {
            model.game_item.ForageItem.firewood(),
            model.game_item.ForageItem.firewood(),
            model.game_item.ForageItem.wildMushroom(),
            model.game_item.ForageItem.herb(),
            (day % 2 == 0) ? model.game_item.ForageItem.firewood()
                           : (rng.nextBoolean() ? model.game_item.ForageItem.wildMushroom() : model.game_item.ForageItem.herb())
        };

        for (int i = 0; i < dailyLoot.length && i < order.size(); i++) {
            double[] off = candidateOffsets[order.get(i)];
            double fx = fire[0] + off[0] + (rng.nextDouble() * 18.0 - 9.0);
            double fy = fire[1] + off[1] + (rng.nextDouble() * 16.0 - 8.0);
            // Tránh vật cản tĩnh nếu lỡ trùng
            if (isPointBlockedForBot(fx, fy)) {
                fy += 32.0;
            }
            model.game_item.ForageItem fItem = dailyLoot[i];

            javafx.scene.shape.Ellipse shadow = new javafx.scene.shape.Ellipse(0, 14, 15, 5.5);
            shadow.setFill(Color.rgb(18, 22, 16, 0.34));

            javafx.scene.shape.Circle glowRing = new javafx.scene.shape.Circle(0, 2, 17);
            glowRing.setFill(Color.rgb(255, 228, 128, 0.16));
            glowRing.setStroke(Color.rgb(255, 224, 118, 0.55));
            glowRing.setStrokeWidth(1.3);

            Image iconImg = Assets.getShopIcon(fItem);
            ImageView iv = new ImageView(iconImg);
            iv.setFitWidth(34);
            iv.setFitHeight(34);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            iv.setX(-17);
            iv.setY(-15);

            javafx.scene.Group spotGroup = new javafx.scene.Group(shadow, glowRing, iv);
            spotGroup.setManaged(false);
            spotGroup.setLayoutX(fx);
            spotGroup.setLayoutY(fy);
            spotGroup.setUserData(fy + 10.0);
            spotGroup.setCursor(javafx.scene.Cursor.HAND);

            CampForageSpot spot = new CampForageSpot(fItem, fx, fy, spotGroup, i * 1.15);
            spotGroup.setOnMouseClicked(ev -> {
                ev.consume();
                if (!router.hasOverlay() && !sleepTransitionRunning) {
                    collectCampForageSpot(spot);
                }
            });

            campForageSpots.add(spot);
            entityPane.getChildren().add(spotGroup);
        }
    }

    private CampForageSpot findNearestForageSpot(double px, double py, double maxDist) {
        CampForageSpot best = null;
        double bestDistSq = maxDist * maxDist;
        for (CampForageSpot spot : campForageSpots) {
            double dx = px - spot.x;
            double dy = py - spot.y;
            double d2 = dx * dx + dy * dy;
            if (d2 <= bestDistSq) {
                bestDistSq = d2;
                best = spot;
            }
        }
        return best;
    }

    private void collectCampForageSpot(CampForageSpot spot) {
        if (spot == null || player == null || !campForageSpots.contains(spot)) return;
        campForageSpots.remove(spot);
        startPickAnimation(false, spot.x, spot.y, null, () -> {
            if (entityPane != null) {
                entityPane.getChildren().remove(spot.node);
            }
            player.getInventory().addItem(spot.item, 1);
            view.fx.SoundHook.playPickup();
            if (particleSystem != null) {
                particleSystem.spawnPickupPop(spot.x, spot.y - 6.0);
            }
            spawnFloatingText("+1 " + spot.item.getName(), Color.rgb(166, 255, 146));
            notify("🎒 Đã nhặt 1x " + spot.item.getName() + " quanh khu trại!");
            triggerAutosave();
        });
    }

    /** Mở màn hình Ghép Đồ Ăn kiểu Minecraft (CookingCraftView) khi đang ngồi tại Đống Lửa Trại. */
    private void openCookingOverlay(double fireX, double fireY) {
        if (router == null || router.hasOverlay() || sleepTransitionRunning) return;
        if (isCampfireExtinguished()) {
            spawnFloatingText("🌧 Lửa đã tắt vì trời mưa, không thể nấu!", Color.rgb(147, 197, 253));
            view.fx.SoundHook.playMenuClick();
            notify("🌧 Trời đang mưa nên bếp lửa bị tắt! Chờ trời nắng lửa cháy lại rồi nấu ăn nhé.");
            return;
        }
        activeKeys.clear();
        activeCookingOverlay = new view.ui.CookingCraftView(
            player,
            this::getActiveBuffSummary,
            () -> {
                if (particleSystem != null) {
                    particleSystem.spawnCookingBurst(fireX, fireY);
                }
                view.fx.SoundHook.playPickup();
                onQuestFoodCooked();
            },
            this::applyCookedFoodBuff,
            this::notify,
            this::closeCookingOverlay
        );
        router.showOverlay(activeCookingOverlay);
        Platform.runLater(activeCookingOverlay::requestFocus);
    }

    private void closeCookingOverlay() {
        if (activeCookingOverlay != null) {
            activeCookingOverlay = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            triggerAutosave();
            Platform.runLater(rootPane::requestFocus);
        }
    }

    /** Áp dụng hồi Đói/Khát + Buff tốc độ chạy (nếu có) + Buff tỉ lệ câu cá hiếm khi ăn món nấu từ đống lửa. */
    private void applyCookedFoodBuff(model.game_item.CookedFoodItem dish) {
        if (dish == null) return;
        int hAdd = dish.getHungerRestore();
        int tAdd = dish.getThirstRestore();
        if (player != null) {
            if (hAdd > 0) {
                player.addHunger(hAdd);
                hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC; // Ăn xong giữ no ~4 tiếng trong game mới bắt đầu tụt Đói lại
            }
            if (tAdd > 0) {
                player.addThirst(tAdd);
                recentLaborStreak = 0;
                recentLaborDecaySec = 0.0;
            }
            updateSurvivalHud(animTimeSec);
        }
        if (dish.getSpeedBonusPercent() > 0) {
            poolRelaxSpeedBuffSec = Math.max(poolRelaxSpeedBuffSec, 150.0);
            lastRenderedRelaxSec = (int) Math.ceil(poolRelaxSpeedBuffSec);
        }
        if (dish.hasRareFishBoost()) {
            foodRareFishBuff = true;
            if (fishShadows != null) fishShadows.setRareFishBuff(true);
        }
        updateFoodBuffHud();
        view.fx.SoundHook.playPickup();

        String restoreTxt = (tAdd > 0)
                ? ("+" + hAdd + " Đói, +" + tAdd + " Nước!")
                : ("+" + hAdd + " Đói!");
        spawnFloatingText(restoreTxt, Color.rgb(132, 255, 146));
        String buffPart = getActiveBuffSummary();
        notify("Đã ăn " + dish.getName() + "! (" + restoreTxt + (buffPart.isEmpty() ? "" : (" | " + buffPart)) + ")");
        triggerAutosave();
    }

    // ======================== HỆ THỐNG SINH TỒN: ĐÓI & KHÁT ========================

    private javafx.scene.layout.Pane ensureHudOverlayPane() {
        if (rootPane == null) return null;
        if (hudOverlayPane == null) {
            hudOverlayPane = new javafx.scene.layout.Pane();
            hudOverlayPane.setMouseTransparent(true);
            hudOverlayPane.setPickOnBounds(false);
            hudOverlayPane.setManaged(false);
            hudOverlayPane.setLayoutX(0);
            hudOverlayPane.setLayoutY(0);
            double w = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1280.0;
            double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : 720.0;
            hudOverlayPane.resize(w, h);
            rootPane.widthProperty().addListener((obs, ov, nv) -> hudOverlayPane.resize(nv.doubleValue(), rootPane.getHeight()));
            rootPane.heightProperty().addListener((obs, ov, nv) -> hudOverlayPane.resize(rootPane.getWidth(), nv.doubleValue()));
        }
        if (!rootPane.getChildren().contains(hudOverlayPane)) {
            rootPane.getChildren().add(hudOverlayPane);
        }
        hudOverlayPane.toFront();
        if (dayFadeRect != null && rootPane.getChildren().contains(dayFadeRect)) {
            dayFadeRect.toFront();
        }
        return hudOverlayPane;
    }

    /** Khởi tạo khung HUD gỗ/pixel 2 thanh ĐÓI & NƯỚC có tiêu đề rõ ràng + icon vẽ nét (không dùng emoji lỗi font). */
    private void setupSurvivalHud() {
        javafx.scene.layout.Pane hud = ensureHudOverlayPane();
        if (hud == null) return;
        if (survivalHudBox != null) {
            if (!hud.getChildren().contains(survivalHudBox)) {
                hud.getChildren().add(survivalHudBox);
            }
            survivalHudBox.toFront();
            return;
        }

        final double barW = 200.0;
        final double barH = 12.0;

        // Tiêu đề khung sinh tồn + tên nông trại
        survivalHeaderTitle = new Label(farmName.toUpperCase());
        survivalHeaderTitle.setMaxWidth(barW);
        survivalHeaderTitle.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FFE49E;"
        );
        javafx.scene.shape.Rectangle divider = new javafx.scene.shape.Rectangle(barW, 1.5);
        divider.setFill(Color.rgb(214, 158, 92, 0.55));

        // ---- THANH 1: ĐÓI (THỨC ĂN) ----
        // Icon đùi gà / thức ăn vẽ vector sắc nét (màu cam vàng ấm)
        javafx.scene.shape.Circle foodMeat = new javafx.scene.shape.Circle(5.5, 6.0, 4.8);
        foodMeat.setFill(Color.rgb(244, 152, 46));
        foodMeat.setStroke(Color.rgb(66, 30, 12));
        foodMeat.setStrokeWidth(1.2);
        javafx.scene.shape.Rectangle foodBone = new javafx.scene.shape.Rectangle(8.5, 4.5, 4.5, 3.0);
        foodBone.setArcWidth(2); foodBone.setArcHeight(2);
        foodBone.setFill(Color.rgb(255, 244, 218));
        foodBone.setStroke(Color.rgb(66, 30, 12));
        foodBone.setStrokeWidth(0.9);
        javafx.scene.Group foodIcon = new javafx.scene.Group(foodBone, foodMeat);

        Label hungerTitle = new Label("THANH ĐÓI");
        hungerTitle.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #FFD166;"
        );
        javafx.scene.layout.Region hungerSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(hungerSpacer, javafx.scene.layout.Priority.ALWAYS);
        hungerValueLabel = new Label("100/100");
        hungerValueLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #FFF6DC;"
        );
        javafx.scene.layout.HBox hungerRow = new javafx.scene.layout.HBox(6, foodIcon, hungerTitle, hungerSpacer, hungerValueLabel);
        hungerRow.setAlignment(Pos.CENTER_LEFT);
        hungerRow.setPrefWidth(barW);

        javafx.scene.shape.Rectangle hungerBg = new javafx.scene.shape.Rectangle(barW, barH);
        hungerBg.setArcWidth(5); hungerBg.setArcHeight(5);
        hungerBg.setFill(Color.rgb(34, 20, 12, 0.92));
        hungerBg.setStroke(Color.rgb(112, 66, 30));
        hungerBg.setStrokeWidth(1.3);
        hungerBarFill = new javafx.scene.shape.Rectangle(barW, barH);
        hungerBarFill.setArcWidth(5); hungerBarFill.setArcHeight(5);
        hungerBarFill.setFill(Color.rgb(245, 166, 35));
        javafx.scene.layout.StackPane hungerTrack = new javafx.scene.layout.StackPane(hungerBg, hungerBarFill);
        hungerTrack.setAlignment(Pos.CENTER_LEFT);
        hungerTrack.setMaxWidth(barW);

        // ---- THANH 2: NƯỚC (KHÁT) ----
        // Icon giọt nước vẽ vector sắc nét (màu xanh dương tươi)
        javafx.scene.shape.Polygon waterDrop = new javafx.scene.shape.Polygon(
            6.0, 0.5,
            10.8, 7.2,
            9.6, 10.8,
            6.0, 12.2,
            2.4, 10.8,
            1.2, 7.2
        );
        waterDrop.setFill(Color.rgb(58, 194, 255));
        waterDrop.setStroke(Color.rgb(18, 66, 108));
        waterDrop.setStrokeWidth(1.2);
        javafx.scene.shape.Circle waterShine = new javafx.scene.shape.Circle(4.5, 7.2, 1.3);
        waterShine.setFill(Color.rgb(228, 250, 255));
        javafx.scene.Group waterIcon = new javafx.scene.Group(waterDrop, waterShine);

        Label thirstTitle = new Label("THANH NƯỚC");
        thirstTitle.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #7CE2FF;"
        );
        javafx.scene.layout.Region thirstSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(thirstSpacer, javafx.scene.layout.Priority.ALWAYS);
        thirstValueLabel = new Label("100/100");
        thirstValueLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #E6F8FF;"
        );
        javafx.scene.layout.HBox thirstRow = new javafx.scene.layout.HBox(6, waterIcon, thirstTitle, thirstSpacer, thirstValueLabel);
        thirstRow.setAlignment(Pos.CENTER_LEFT);
        thirstRow.setPrefWidth(barW);

        javafx.scene.shape.Rectangle thirstBg = new javafx.scene.shape.Rectangle(barW, barH);
        thirstBg.setArcWidth(5); thirstBg.setArcHeight(5);
        thirstBg.setFill(Color.rgb(16, 26, 40, 0.92));
        thirstBg.setStroke(Color.rgb(66, 114, 156));
        thirstBg.setStrokeWidth(1.3);
        thirstBarFill = new javafx.scene.shape.Rectangle(barW, barH);
        thirstBarFill.setArcWidth(5); thirstBarFill.setArcHeight(5);
        thirstBarFill.setFill(Color.rgb(52, 182, 255));
        javafx.scene.layout.StackPane thirstTrack = new javafx.scene.layout.StackPane(thirstBg, thirstBarFill);
        thirstTrack.setAlignment(Pos.CENTER_LEFT);
        thirstTrack.setMaxWidth(barW);

        survivalHudBox = new javafx.scene.layout.VBox(4, survivalHeaderTitle, divider, hungerRow, hungerTrack, thirstRow, thirstTrack);
        survivalHudBox.setPadding(new javafx.geometry.Insets(7, 11, 9, 11));
        survivalHudBox.setMinWidth(226.0);
        survivalHudBox.setPrefWidth(226.0);
        survivalHudBox.setMaxWidth(226.0);
        survivalHudBox.setMouseTransparent(true);
        survivalHudBox.setLayoutX(14.0);
        survivalHudBox.setLayoutY(12.0);
        survivalHudBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(114, 68, 34, 0.93), rgba(74, 42, 20, 0.95));" +
            "-fx-border-color: #2E170A;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 8, 0.2, 0, 2);"
        );
        hud.getChildren().add(survivalHudBox);
        survivalHudBox.toFront();
    }

    /** Cập nhật 2 thanh ĐÓI & NƯỚC mượt mà, tự chuyển đỏ & nhấp nháy khi dưới 30%. */
    private void updateSurvivalHud(double timeSec) {
        if (player == null) return;
        if (survivalHudBox == null) setupSurvivalHud();
        if (hungerBarFill == null || thirstBarFill == null) return;

        final double barW = 200.0;
        double h = player.getHunger();
        double t = player.getThirst();
        int hInt = (int) Math.round(h);
        int tInt = (int) Math.round(t);
        boolean hLow = h < 30.0;
        boolean tLow = t < 30.0;

        if (hInt != lastRenderedHungerInt || hLow != lastHungerLow) {
            lastRenderedHungerInt = hInt;
            lastHungerLow = hLow;
            hungerValueLabel.setText(h <= 0.0 ? "0/100 (KIỆT)" : (hInt + "/100"));
            hungerValueLabel.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + (hLow ? "#FFB3B3;" : "#FFF6DC;")
            );
            hungerBarFill.setFill(hLow ? Color.rgb(235, 58, 58) : Color.rgb(245, 166, 35));
        }
        hungerBarFill.setWidth(Math.max(0.0, barW * (h / 100.0)));
        hungerBarFill.setOpacity(hLow ? (0.50 + 0.50 * Math.abs(Math.sin(timeSec * 6.5))) : 1.0);

        if (tInt != lastRenderedThirstInt || tLow != lastThirstLow) {
            lastRenderedThirstInt = tInt;
            lastThirstLow = tLow;
            thirstValueLabel.setText(t <= 0.0 ? "0/100 (KIỆT)" : (tInt + "/100"));
            thirstValueLabel.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + (tLow ? "#FFB3B3;" : "#E6F8FF;")
            );
            thirstBarFill.setFill(tLow ? Color.rgb(235, 58, 58) : Color.rgb(52, 182, 255));
        }
        thirstBarFill.setWidth(Math.max(0.0, barW * (t / 100.0)));
        thirstBarFill.setOpacity(tLow ? (0.50 + 0.50 * Math.abs(Math.sin(timeSec * 6.5 + 1.0))) : 1.0);
    }

    /**
     * Tiêu hao Đói & Khát khi lao động (cuốc đất, tưới nước, câu cá):
     * - Đói tụt rất nhẹ (đặc biệt trong 4 tiếng đang no).
     * - Khát tụt ít khi làm lác đác (1-4 thao tác), và chỉ tăng tốc rõ rệt khi làm việc nhiều liên tục.
     */
    private void applyLaborSurvivalCost() {
        if (player == null) return;
        recentLaborStreak++;
        recentLaborDecaySec = 6.0; // Nghỉ 6 giây sẽ hết mệt, reset chuỗi lao động

        double hungerCost = (hungerGraceTimerSec > 0.0) ? 0.3 : 0.55;
        double thirstCost;
        if (recentLaborStreak <= 4) {
            thirstCost = 0.7;
        } else if (recentLaborStreak <= 8) {
            thirstCost = 1.6;
        } else {
            thirstCost = 2.6;
        }
        WeatherType wType = (gameManager != null && gameManager.getCurrentWeather() != null)
                ? gameManager.getCurrentWeather().getType()
                : WeatherType.SUNNY;
        if (wType == WeatherType.DROUGHT) {
            thirstCost += 0.35;
        } else if (wType == WeatherType.RAINY) {
            thirstCost *= 0.75;
        }
        if (npcManager != null && npcManager.hasPerkUnlocked("chopper")) {
            hungerCost *= 0.75;
            thirstCost *= 0.75;
        }

        player.addHunger(-hungerCost);
        player.addThirst(-thirstCost);
        updateSurvivalHud(animTimeSec);
    }

    /** Kiểm tra nhân vật có đang đứng trên cỏ sát bờ con sông (không ở trên thuyền và không đứng giữa cầu) để bấm [R] uống nước hay không. */
    private boolean isNearRiverBankForDrink() {
        if (farmMap == null || isOnBoat || isPlayerOnBridge()) return false;
        if (boatEntity != null && (boatEntity.isPlayerAboard() || boatEntity.isTransitioning())) return false;

        double footX = playerX + 22.0;
        double footTopY = playerY + 58.0;
        double footBottomY = playerY + 68.0;

        // Mở rộng vùng nhận diện lên trên phần cỏ sát mép bờ Bắc (kể cả đường cong mố cỏ quanh chân cầu) và mép cỏ bờ Nam
        double northGrassEdgeY = getNorthShoreGrassMaxY(footX);
        if (footBottomY <= northGrassEdgeY + 4.0 && (northGrassEdgeY - footBottomY) <= 56.0) {
            return true;
        }
        if (footTopY >= worldRiverBotY - 4.0 && (footTopY - worldRiverBotY) <= 56.0) {
            return true;
        }

        int col = (int) (footX / Constant.CELL_SIZE);
        int row = (int) (footBottomY / Constant.CELL_SIZE);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                Cell c = farmMap.getCell(row + dr, col + dc);
                if (c != null && c.getTerrain() == TerrainType.WATER) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Uống nước trực tiếp tại bờ sông ([R] hoặc [E] khi không ở gần thuyền/nhà/lửa). */
    private void tryDrinkRiverWater() {
        if (player == null) return;
        if (!isNearRiverBankForDrink()) {
            notify("Hãy đi tới sát bờ sông rồi bấm [R] để uống nước!");
            return;
        }
        player.addThirst(50.0);
        recentLaborStreak = 0;
        recentLaborDecaySec = 0.0;
        updateSurvivalHud(animTimeSec);
        if (particleSystem != null) {
            particleSystem.spawnWaterSplash(playerX + 22.0, playerY + 34.0);
        }
        view.fx.SoundHook.playWaterSplash();
        spawnFloatingText("+50 Khát! Mát quá!", Color.rgb(86, 214, 255));
        notify("💧 +50 Khát! Mát quá! (Khát: " + (int) Math.round(player.getThirst()) + "/100)");
        triggerAutosave();
    }

    /** Ăn / uống 1 món ăn trực tiếp từ nút [Ăn] trong Kho Đồ (đã trừ khỏi Inventory trước khi gọi). */
    private void consumeItemFromStorage(Item item) {
        if (item == null || player == null) return;
        if (item instanceof model.game_item.CookedFoodItem) {
            if (particleSystem != null) {
                particleSystem.spawnPickupPop(playerX + 22.0, playerY + 18.0);
            }
            applyCookedFoodBuff((model.game_item.CookedFoodItem) item);
        } else if (item instanceof model.game_item.FactoryProductItem) {
            model.game_item.FactoryProductItem prod = (model.game_item.FactoryProductItem) item;
            if (prod.getHungerRestore() > 0) {
                player.addHunger(prod.getHungerRestore());
                hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
            }
            if (prod.getThirstRestore() > 0) {
                player.addThirst(prod.getThirstRestore());
                recentLaborStreak = 0;
                recentLaborDecaySec = 0.0;
            }
            updateSurvivalHud(animTimeSec);
            view.fx.SoundHook.playPickup();
            boolean drinkOnly = (prod.getHungerRestore() <= 0 && prod.getThirstRestore() > 0);
            String txt = drinkOnly
                    ? ("+" + prod.getThirstRestore() + " Khát!")
                    : (prod.getThirstRestore() > 0
                        ? ("+" + prod.getHungerRestore() + " Đói, +" + prod.getThirstRestore() + " Khát!")
                        : ("+" + prod.getHungerRestore() + " Đói!"));
            spawnFloatingText(txt, drinkOnly ? Color.rgb(125, 211, 252) : Color.rgb(134, 239, 172));
            notify((drinkOnly ? "🧃 Đã dùng " : "🍽 Đã ăn ") + prod.getName() + "! (" + txt + ")");
            triggerAutosave();
        }
    }

    /** Ăn nhanh 1 món đã nấu sẵn hoặc thành phẩm Nhà Máy trong Inventory bằng phím [Q]. */
    private void tryQuickEatFood() {
        if (player == null) return;
        model.game_item.CookedFoodItem chosen = null;
        model.game_item.FactoryProductItem chosenFac = null;
        boolean needWater = player.getThirst() < 45.0;
        for (java.util.Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() <= 0) continue;
            if (e.getKey() instanceof model.game_item.CookedFoodItem) {
                model.game_item.CookedFoodItem cand = (model.game_item.CookedFoodItem) e.getKey();
                if (chosen == null || (needWater && cand.getThirstRestore() > chosen.getThirstRestore())) {
                    chosen = cand;
                }
            } else if (e.getKey() instanceof model.game_item.FactoryProductItem) {
                model.game_item.FactoryProductItem fCand = (model.game_item.FactoryProductItem) e.getKey();
                if (fCand.getHungerRestore() > 0 || fCand.getThirstRestore() > 0) {
                    if (chosenFac == null || (needWater && fCand.getThirstRestore() > chosenFac.getThirstRestore())) {
                        chosenFac = fCand;
                    }
                }
            }
        }
        if (needWater && chosenFac != null && chosenFac.getThirstRestore() > 0 && (chosen == null || chosen.getThirstRestore() < chosenFac.getThirstRestore())) {
            if (player.getInventory().removeItem(chosenFac, 1)) {
                if (chosenFac.getHungerRestore() > 0) {
                    player.addHunger(chosenFac.getHungerRestore());
                    hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
                }
                player.addThirst(chosenFac.getThirstRestore());
                recentLaborStreak = 0;
                recentLaborDecaySec = 0.0;
                updateSurvivalHud(animTimeSec);
                view.fx.SoundHook.playPickup();
                spawnFloatingText("+" + chosenFac.getThirstRestore() + " Khát (" + chosenFac.getName() + ")!", Color.rgb(125, 211, 252));
                notify("🧃 Đã dùng " + chosenFac.getName() + "!");
                triggerAutosave();
                return;
            }
        }
        if (chosen != null) {
            if (player.getInventory().removeItem(chosen, 1)) {
                if (particleSystem != null) {
                    particleSystem.spawnPickupPop(playerX + 22.0, playerY + 18.0);
                }
                applyCookedFoodBuff(chosen);
            }
            return;
        }
        if (chosenFac != null && player.getInventory().removeItem(chosenFac, 1)) {
            player.addHunger(chosenFac.getHungerRestore());
            player.addThirst(chosenFac.getThirstRestore());
            hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
            updateSurvivalHud(animTimeSec);
            view.fx.SoundHook.playPickup();
            spawnFloatingText("+" + chosenFac.getHungerRestore() + " Đói (" + chosenFac.getName() + ")!", Color.rgb(134, 239, 172));
            notify("🍽 Đã ăn " + chosenFac.getName() + "!");
            triggerAutosave();
            return;
        }
        spawnFloatingText("Hết đồ ăn! Hãy nấu tại đống lửa hoặc Nhà Máy!", Color.rgb(255, 206, 102));
        notify("Không có món ăn/nước ép trong kho! Hãy tới đống lửa hoặc Nhà Máy chế biến.");
    }

    // ======================== NHÀ MÁY, LỄ HỘI 7 NGÀY, TẶNG QUÀ NPC & MỞ KHÓA VƯỜN CÂY ĂN QUẢ ========================

    /** Chuyển cảnh mượt FadeTransition 0.3s vào giao diện riêng FactoryView (1280x720). */
    private void enterFactoryWithFade() {
        if (router == null || router.hasOverlay() || sleepTransitionRunning) return;
        activeKeys.clear();
        view.fx.SoundHook.playWoodGateOpen();

        activeFactoryView = new view.ui.FactoryView(
            player,
            factoryManager,
            marketManager,
            null, // moneyProperty listener đã tự động cộng tiến độ nhiệm vụ kiếm tiền
            this::notify,
            this::closeFactoryView
        );
        activeFactoryView.setOpacity(0.0);
        router.showOverlay(activeFactoryView);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.30), activeFactoryView);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
        Platform.runLater(activeFactoryView::requestFocus);
    }

    private void closeFactoryView() {
        if (activeFactoryView == null) return;
        view.ui.FactoryView closing = activeFactoryView;
        activeFactoryView = null;
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.25), closing);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            if (autosave != null) autosave.run();
            Platform.runLater(rootPane::requestFocus);
        });
        ft.play();
    }

    /** Mở bảng chọn quà tặng nhanh [G] khi đứng cạnh bất kỳ ai trong 6 NPC. */
    private void openNpcGiftOverlay(view.npc.NpcManager.NpcTarget target) {
        if (target == null || router == null || router.hasOverlay() || player == null || npcManager == null) return;
        int curDay = (gameManager != null) ? gameManager.getCurrentDay() : 1;
        if (npcManager.hasGiftedToday(target.id, curDay)) {
            spawnFloatingText("Hôm nay đã tặng quà cho " + target.name + " rồi!", Color.rgb(255, 214, 102));
            notify("💖 Bạn đã tặng quà cho " + target.name + " hôm nay rồi! Hãy quay lại vào ngày mai.");
            return;
        }

        activeKeys.clear();
        javafx.scene.layout.StackPane scrim = new javafx.scene.layout.StackPane();
        scrim.setStyle("-fx-background-color: rgba(14, 9, 6, 0.68);");
        scrim.setAlignment(Pos.CENTER);
        scrim.setFocusTraversable(true);

        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setMaxSize(540, 440);
        card.setPrefSize(540, 440);
        card.setPadding(new javafx.geometry.Insets(16, 18, 16, 18));
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #784726, #522E16);" +
            "-fx-border-color: #2B1407; -fx-border-width: 4px;" +
            "-fx-background-radius: 14px; -fx-border-radius: 12px;"
        );

        String heartStr = view.npc.NpcManager.formatHeartBar(npcManager.getHearts(target.id));
        Label hdr = new Label("🎁 TẶNG QUÀ CHO " + target.name.toUpperCase() + "  " + heartStr);
        hdr.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFE79A;");

        Label perkInfo = new Label("Đặc quyền ≥ 3❤: " + view.npc.NpcManager.getPerkDescription(target.id));
        perkInfo.setWrapText(true);
        perkInfo.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-text-fill: #A7F3D0;");

        javafx.scene.layout.VBox listBox = new javafx.scene.layout.VBox(7);
        listBox.setPadding(new javafx.geometry.Insets(6));

        int count = 0;
        for (java.util.Map.Entry<Item, Integer> en : player.getInventory().getItems().entrySet()) {
            Item it = en.getKey();
            int qty = en.getValue();
            if (qty <= 0) continue;
            boolean highGift = (it instanceof model.game_item.CookedFoodItem) || (it instanceof model.game_item.FactoryProductItem);
            boolean normalGift = (it instanceof model.game_item.HarvestedCropItem) || (it instanceof model.game_item.ForageItem) || (it instanceof model.game_item.FishItem);
            if (!highGift && !normalGift) continue;
            count++;

            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new javafx.geometry.Insets(7, 10, 7, 10));
            row.setStyle("-fx-background-color: #FFFDF7; -fx-border-color: #C8A273; -fx-border-radius: 8px; -fx-background-radius: 8px;");

            ImageView iv = Assets.imageView(Assets.getShopIcon(it), 30);
            Label nm = new Label(it.getName() + " (×" + qty + ")  ➡  " + (highGift ? "+1.0 ❤" : "+0.5 ❤"));
            nm.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                    + (highGift ? "#BE123C;" : "#3B1E0B;"));
            javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.control.Button giveBtn = new javafx.scene.control.Button("🎁 Tặng");
            giveBtn.setFocusTraversable(false);
            giveBtn.setStyle(
                "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: #FFFFFF; -fx-background-color: #E11D48; -fx-padding: 4 12 4 12; -fx-background-radius: 6px; -fx-cursor: hand;"
            );
            giveBtn.setOnAction(ev -> {
                closeNpcGiftOverlay();
                npcManager.giveGiftToNpc(
                    target, it, curDay, player, particleSystem,
                    this::syncAllNpcPerks,
                    txt -> spawnFloatingText(txt, Color.rgb(255, 132, 168)),
                    this::notify
                );
            });

            if (iv != null) row.getChildren().add(iv);
            row.getChildren().addAll(nm, sp, giveBtn);
            listBox.getChildren().add(row);
        }

        if (count == 0) {
            Label empty = new Label("Túi đồ chưa có Món nấu lửa trại, Thành phẩm Nhà Máy (+1 ❤) hoặc Nông sản (+0.5 ❤) để tặng!");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #5C3418; -fx-padding: 12;");
            listBox.getChildren().add(empty);
        }

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F7E8CA; -fx-background-color: #F7E8CA; -fx-border-color: #3A1F0B; -fx-border-radius: 8px;");
        javafx.scene.layout.VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Đóng [ESC]");
        cancelBtn.setFocusTraversable(false);
        cancelBtn.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FFF4DC; -fx-background-color: #8C4A27; -fx-padding: 6 16 6 16; -fx-background-radius: 7px; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(ev -> closeNpcGiftOverlay());

        card.getChildren().addAll(hdr, perkInfo, scroll, cancelBtn);
        scrim.getChildren().add(card);
        scrim.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE || ev.getCode() == KeyCode.G) {
                ev.consume();
                closeNpcGiftOverlay();
            }
        });
        scrim.setOnMouseClicked(ev -> {
            if (ev.getTarget() == scrim) closeNpcGiftOverlay();
        });

        activeGiftDialog = scrim;
        router.showOverlay(scrim);
        Platform.runLater(scrim::requestFocus);
    }

    private void closeNpcGiftOverlay() {
        if (activeGiftDialog != null) {
            activeGiftDialog = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            Platform.runLater(rootPane::requestFocus);
        }
    }

    /** Đồng bộ toàn bộ đặc quyền ≥ 3 Tim của 6 NPC sang Shop, Môi trường & Sông câu cá. */
    private void syncAllNpcPerks() {
        boolean namiPerk = (npcManager != null && npcManager.hasPerkUnlocked("nami"));
        boolean zoroPerk = (npcManager != null && npcManager.hasPerkUnlocked("zoro"));
        boolean usoppPerk = (npcManager != null && npcManager.hasPerkUnlocked("usopp"));

        if (shopController != null) {
            shopController.setNamiDiscountActive(namiPerk);
            shopController.setOrchardUnlocked(orchardUnlocked);
        }
        if (environmentController != null) {
            environmentController.setZoroPestProtection(zoroPerk);
        }
        if (fishShadows != null) {
            fishShadows.setUsoppRareBoost(usoppPerk);
        }
        if (fishingGame != null) {
            fishingGame.setUsoppRareBoost(usoppPerk);
        }
    }

    /** Mua mở khóa Vườn Cây Ăn Quả Lâu Năm ($3.000): xóa sạch hàng rào gỗ và mở bán Cam/Táo/Dừa trong Shop! */
    private void tryUnlockOrchardZone() {
        if (orchardUnlocked || player == null) return;
        final int unlockCost = 3000;
        if (!player.deductMoney(unlockCost)) {
            spawnFloatingText("Cần $3.000 để mở khóa Vườn Cây Ăn Quả!", Color.rgb(255, 118, 108));
            notify("🔒 Chưa đủ $3.000! (Hiện có: $" + player.getMoney() + " / $3.000)");
            view.fx.SoundHook.playMenuClick();
            return;
        }
        orchardUnlocked = true;
        applyOrchardUnlockedVisuals();
        syncAllNpcPerks();
        view.fx.SoundHook.playPickup();
        if (particleSystem != null) {
            particleSystem.spawnFireworkBurst(playerX + 22.0, playerY - 18.0);
        }
        spawnFloatingText("🔓 ĐÃ MỞ KHÓA VƯỜN CÂY ĂN QUẢ!", Color.rgb(134, 255, 152));
        notify("🍊 Đã mở khóa Vườn Cây Ăn Quả! Hãy vào Shop mua hạt giống Cam, Táo, Dừa trồng thu hoạch vĩnh viễn mỗi 3 ngày!");
        if (autosave != null) autosave.run();
    }

    /** Hitbox của nhân vật dùng để kiểm tra giao cắt AABB với vùng tương tác Cổng Vườn Cây Ăn Quả. */
    private javafx.geometry.Rectangle2D getPlayerInteractHitbox() {
        return new javafx.geometry.Rectangle2D(playerX + 12.0, playerY + 40.0, 20.0, 24.0);
    }

    /** Kiểm tra hitbox nhân vật có giao với AABB tương tác của Cổng Vườn Cây Ăn Quả (trái, giữa, phải, trước, sau) hay không. */
    private boolean isPlayerTouchingOrchardGateZone() {
        return !orchardUnlocked
                && orchardInteractBox != null
                && getPlayerInteractHitbox().intersects(orchardInteractBox);
    }

    /** Khoảng cách từ chân nhân vật tới thân cửa cổng Vườn Cây Ăn Quả (bằng 0 khi đứng ngay sát cửa, dùng xét độ ưu tiên). */
    private double distanceToOrchardGateDoorway(double px, double py) {
        if (orchardGateBounds == null) return Double.MAX_VALUE;
        double minX = orchardGateBounds.getMinX();
        double maxX = orchardGateBounds.getMaxX();
        double minY = orchardGateBounds.getMinY() + orchardGateBounds.getHeight() * 0.50;
        double maxY = orchardGateBounds.getMaxY();
        double clampedX = Math.max(minX, Math.min(px, maxX));
        double clampedY = Math.max(minY, Math.min(py, maxY));
        return Math.hypot(px - clampedX, py - clampedY);
    }

    private void applyOrchardUnlockedVisuals() {
        if (sceneryObjects != null) {
            java.util.List<ImageView> toRemove = new java.util.ArrayList<>();
            for (ImageView iv : sceneryObjects) {
                String id = iv.getId();
                if (id != null && (id.toLowerCase().startsWith("orchard_fence") || id.toLowerCase().equals("orchard_sign"))) {
                    toRemove.add(iv);
                }
            }
            sceneryObjects.removeAll(toRemove);
            if (entityPane != null) {
                entityPane.getChildren().removeAll(toRemove);
            }
        }
        if (orchardWorldSignLabel != null && entityPane != null) {
            entityPane.getChildren().remove(orchardWorldSignLabel);
            orchardWorldSignLabel = null;
        }
        orchardGateBounds = null;
        orchardInteractBox = null;
    }

    /** Lắp Vòi Tưới Tự Động tại ô đất dưới chân người chơi bằng phím [V]. */
    private void tryPlaceAutoSprinklerUnderFeet() {
        int col = (int) ((playerX + 22.0) / Constant.CELL_SIZE);
        int row = (int) ((playerY + 52.0) / Constant.CELL_SIZE);
        Cell under = (farmMap != null) ? farmMap.getCell(row, col) : null;
        if (under == null || !isCellInFarmZone(under)) {
            notify("Hãy đứng trong Khu Trồng Trọt (hoặc Vườn Cây Ăn Quả đã mở) rồi bấm [V] để lắp Vòi Tưới Tự Động!");
            return;
        }
        if (mineManager.placeSprinklerAt(player, row, col, entityPane, this::notify)) {
            spawnFloatingText("💧 Đã lắp Vòi Tưới Tự Động!", Color.rgb(125, 211, 252));
        }
    }

    /** Cập nhật biểu ngữ Lễ Hội 7 Ngày ở đầu màn hình + hiệu ứng Đêm Pháo Hoa Bể Bơi từ 18:00. */
    private void updateFestivalEffectsAndCheckReward(double dtSec) {
        if (gameManager == null) return;
        int curDay = gameManager.getCurrentDay();
        int curHour = getCurrentGameHour();

        boolean fireworksNow = festivalManager.isFireworksShowActive(curDay, curHour);
        double poolCx = poolWaterX + poolWaterW * 0.5;
        double poolCy = poolWaterY + poolWaterH * 0.5;
        if (npcManager != null) {
            npcManager.setFireworksGathering(fireworksNow, poolCx, poolCy);
        }

        if (fireworksNow) {
            fireworkBurstTimer += dtSec;
            if (fireworkBurstTimer >= 0.52) {
                fireworkBurstTimer = 0.0;
                if (particleSystem != null) {
                    double fx = poolCx + (Math.random() - 0.5) * 380.0;
                    double fy = poolWaterY - 40.0 + (Math.random() - 0.5) * 160.0;
                    particleSystem.spawnFireworkBurst(fx, fy);
                }
            }
            // Người chơi ghé Khu Bể Bơi tham dự Đêm Pháo Hoa -> nhận ngay +$800 & +0.5 Tim cho toàn bộ 6 NPC!
            double distToPool = Math.hypot((playerX + 22.0) - poolCx, (playerY + 52.0) - poolCy);
            if (distToPool <= 290.0 && festivalManager.tryClaimFireworksParticipation(curDay, curHour)) {
                if (player != null) {
                    player.addMoney(model.festival.FestivalManager.FIREWORKS_REWARD);
                }
                if (npcManager != null) {
                    npcManager.addHeartsToAll(0.5);
                    syncAllNpcPerks();
                }
                view.fx.SoundHook.playHeart();
                spawnFloatingText("🎆 THAM DỰ ĐÊM PHÁO HOA: +$800 & +0.5 ❤ TOÀN BỘ NPC!", Color.rgb(255, 224, 96));
                notify("🎆 Tuyệt vời! Bạn nhận +$800 và +0.5 Tim thân mật với cả 6 NPC khi dự Đêm Hội Pháo Hoa!");
                updateFestivalBannerUi();
            }
        }
        updateFestivalBannerUi();
    }

    private void updateFestivalBannerUi() {
        if (rootPane == null || gameManager == null) return;
        String text = festivalManager.getTopBannerText(gameManager.getCurrentDay(), getCurrentGameHour());
        if (text == null || text.isEmpty()) {
            if (festivalTopBannerLabel != null) {
                festivalTopBannerLabel.setVisible(false);
            }
            return;
        }
        if (festivalTopBannerLabel == null) {
            festivalTopBannerLabel = new Label();
            festivalTopBannerLabel.setMouseTransparent(true);
            festivalTopBannerLabel.setManaged(false);
            festivalTopBannerLabel.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #FEF08A;" +
                "-fx-background-color: linear-gradient(to right, rgba(124,45,18,0.94), rgba(153,27,27,0.94));" +
                "-fx-border-color: #FACC15; -fx-border-width: 2px;" +
                "-fx-padding: 6 16 6 16; -fx-background-radius: 10px; -fx-border-radius: 10px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 10, 0.25, 0, 3);"
            );
            rootPane.getChildren().add(festivalTopBannerLabel);
        }
        if (!text.equals(festivalTopBannerLabel.getText())) {
            festivalTopBannerLabel.setText(text);
            festivalTopBannerLabel.autosize();
        }
        double rw = Math.max(960.0, rootPane.getWidth());
        double bw = festivalTopBannerLabel.getWidth() > 20 ? festivalTopBannerLabel.getWidth() : (text.length() * 7.2 + 32.0);
        festivalTopBannerLabel.setLayoutX(Math.max(228.0, (rw - 184.0 - bw) * 0.5));
        festivalTopBannerLabel.setLayoutY(12.0);
        festivalTopBannerLabel.setVisible(true);
        festivalTopBannerLabel.toFront();
    }

    /** Hiển thị dòng chữ nổi bay nhẹ lên trên đầu nhân vật rồi mờ dần. */
    private void spawnFloatingText(String text, Color color) {
        if (entityPane == null) return;
        Label lbl = new Label(text);
        String hex = String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
        lbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + hex + ";" +
            "-fx-background-color: rgba(24, 14, 8, 0.82);" +
            "-fx-padding: 3 8 3 8; -fx-background-radius: 6px;" +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.85), 2, 1.0, 0, 1);"
        );
        lbl.setMouseTransparent(true);
        lbl.setManaged(false);
        lbl.setUserData(9998.0);
        lbl.autosize();
        double estW = Math.max(120.0, text.length() * 8.0);
        lbl.setLayoutX(playerX + 22.0 - estW * 0.5);
        lbl.setLayoutY(playerY - 26.0);
        entityPane.getChildren().add(lbl);

        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(1.25), lbl);
        move.setByY(-42.0);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.25), lbl);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        javafx.animation.ParallelTransition anim = new javafx.animation.ParallelTransition(move, fade);
        anim.setOnFinished(e -> entityPane.getChildren().remove(lbl));
        anim.play();
    }

    /** Xóa toàn bộ buff đồ ăn khi qua ngày mới. */
    private void clearFoodBuffs() {
        foodRareFishBuff = false;
        if (fishShadows != null) fishShadows.setRareFishBuff(false);
        updateFoodBuffHud();
    }

    private String getActiveBuffSummary() {
        StringBuilder sb = new StringBuilder();
        if (poolRelaxSpeedBuffSec > 0.0) {
            int rem = Math.max(1, (int) Math.ceil(poolRelaxSpeedBuffSec));
            sb.append(String.format("Thư giãn +20%% tốc độ chạy (%d:%02d)", rem / 60, rem % 60));
        }
        if (foodRareFishBuff) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("Tăng tỉ lệ cá hiếm");
        }
        return sb.toString();
    }

    /** Hiển thị icon/huy hiệu Buff nhỏ trên HUD khi có hiệu lực trong ngày. */
    private void updateFoodBuffHud() {
        if (rootPane == null) return;
        String summary = getActiveBuffSummary();
        if (summary.isEmpty()) {
            if (foodBuffHudPill != null) foodBuffHudPill.setVisible(false);
            return;
        }
        javafx.scene.layout.Pane hud = ensureHudOverlayPane();
        if (hud == null) return;
        if (foodBuffHudPill == null) {
            foodBuffHudPill = new Label();
            foodBuffHudPill.setMouseTransparent(true);
            foodBuffHudPill.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 11.5px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF7D6;" +
                "-fx-background-color: rgba(54, 26, 10, 0.90);" +
                "-fx-border-color: #FFAD3B;" +
                "-fx-border-width: 2px;" +
                "-fx-padding: 4 11 4 11;" +
                "-fx-background-radius: 9px;" +
                "-fx-border-radius: 9px;"
            );
            hud.getChildren().add(foodBuffHudPill);
        }
        foodBuffHudPill.setText("BUFF: " + summary);
        foodBuffHudPill.setLayoutX(14.0);
        foodBuffHudPill.setLayoutY(128.0);
        foodBuffHudPill.setVisible(true);
        foodBuffHudPill.toFront();
    }

    /** Mở nhanh Tab 4: Sổ Cá trong Cuốn Sổ Quản Lý (phím G hoặc nút Sổ Cá). */
    @FXML
    private void toggleFishingGuide() {
        toggleGameBook(view.ui.GameBookOverlay.TAB_FISHING);
    }

    /**
     * Hiển thị thẻ Khoe Cá phía trên màn hình (tự mờ ẩn sau 2.5 giây) với Tên cá, Độ hiếm, cm, Vương Miện & Kỷ lục mới.
     */
    private void showFishCatchCard(model.game_item.FishSpecies species, double sizeCm,
                                   boolean isCrown, boolean isNewRecord, int rewardMoney) {
        javafx.scene.layout.Pane hud = ensureHudOverlayPane();
        if (hud == null) return;
        if (fishCatchCard == null) {
            fishCatchCard = new javafx.scene.layout.VBox(4);
            fishCatchCard.setAlignment(Pos.CENTER);
            fishCatchCard.setMouseTransparent(true);
            fishCatchCard.setMinWidth(420);
            fishCatchCard.setPrefWidth(420);
            fishCatchCard.setMaxWidth(420);
            fishCatchCard.setPadding(new javafx.geometry.Insets(10, 18, 10, 18));
            hud.getChildren().add(fishCatchCard);
        }

        // Màu sắc chuẩn theo độ hiếm: Trắng (Thường) / Xanh dương (Hiếm) / Tím (Cực hiếm) / Vàng kim (Siêu hiếm)
        String rarity = species.getRarityLabel();
        String rarityHex;
        if ("Siêu hiếm".equals(rarity))     rarityHex = "#FFD84A";
        else if ("Cực hiếm".equals(rarity)) rarityHex = "#C87BFF";
        else if ("Hiếm".equals(rarity))     rarityHex = "#4EB6FF";
        else                                rarityHex = "#FFFFFF";

        String borderHex = isCrown ? "#FFD84A" : rarityHex;
        fishCatchCard.setStyle(
            "-fx-background-color: rgba(34, 20, 12, 0.92);" +
            "-fx-border-color: " + borderHex + ";" +
            "-fx-border-width: 3px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 14, 0.25, 0, 4);"
        );

        String crownTag = isCrown ? "  👑 VƯƠNG MIỆN" : "";
        Label nameLbl = new Label(species.getDisplayName().toUpperCase() + "  [" + rarity + "]" + crownTag);
        nameLbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + rarityHex + ";"
        );

        String recTag = isNewRecord ? "   ★ KỶ LỤC MỚI!" : "";
        Label sizeLbl = new Label(String.format("Kích thước: %.1f cm%s", sizeCm, recTag));
        sizeLbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + (isNewRecord ? "#FFE66D" : "#FFF3D4") + ";"
        );

        String rewardTxt = "+1 " + species.getDisplayName() + " vào kho  •  Giá bán: $" + species.getPrice();
        Label rewardLbl = new Label(rewardTxt);
        rewardLbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #8CFF98;"
        );

        fishCatchCard.getChildren().setAll(nameLbl, sizeLbl, rewardLbl);
        double w = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1280.0;
        fishCatchCard.setLayoutX(Math.max(16, (w - 420.0) * 0.5));
        fishCatchCard.setLayoutY(22.0);
        fishCatchCard.setVisible(true);
        fishCatchCard.setOpacity(1.0);
        fishCatchCard.toFront();

        if (fishCatchAnim != null) {
            fishCatchAnim.stop();
        }
        javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.1));
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.45), fishCatchCard);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(ev -> fishCatchCard.setVisible(false));
        fishCatchAnim = new javafx.animation.SequentialTransition(hold, fade);
        fishCatchAnim.play();
    }

    private void closeFishingGuide() {
        if (activeFishingGuide != null) {
            activeFishingGuide = null;
            if (router != null && router.hasOverlay()) {
                router.closeOverlay();
            }
            activeKeys.clear();
            Platform.runLater(rootPane::requestFocus);
        }
    }

    private int getCurrentGameHour() {
        double progress = Math.min(0.999999, Math.max(0.0, (double) elapsedTimeInDay / DAY_DURATION_MS));
        int totalMins = (int) (progress * 24 * 60);
        return (totalMins / 60) % 24;
    }

    /** Thanh gợi ý nhỏ phía dưới màn hình khi người chơi đứng gần hoặc bước lên thuyền. */
    private void updateBoatHintBanner(boolean show) {
        if (show == boatHintVisible) return;
        boatHintVisible = show;
        javafx.scene.layout.Pane hud = ensureHudOverlayPane();
        if (hud == null) return;
        if (boatHintBanner == null) {
            boatHintBanner = new Label("[E] Lên/Xuống thuyền  |  [A/D] Chèo  |  [Space] Câu cá  |  [G] Xem Sổ Cá");
            boatHintBanner.setStyle(
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: 12.5px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF4D2;" +
                "-fx-background-color: rgba(42, 24, 12, 0.88);" +
                "-fx-border-color: #DDA95F;" +
                "-fx-border-width: 2px;" +
                "-fx-padding: 5 14 5 14;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;"
            );
            boatHintBanner.setMouseTransparent(true);
            boatHintBanner.setMinWidth(560);
            boatHintBanner.setPrefWidth(560);
            boatHintBanner.setMaxWidth(560);
            boatHintBanner.setAlignment(Pos.CENTER);
            hud.getChildren().add(boatHintBanner);
        }
        boatHintBanner.setVisible(show);
        if (show) {
            double w = rootPane.getWidth() > 0 ? rootPane.getWidth() : 1280.0;
            double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : 720.0;
            boatHintBanner.setLayoutX(Math.max(16, (w - 560.0) * 0.5));
            boatHintBanner.setLayoutY(h - 144.0);
            boatHintBanner.toFront();
        }
    }

    private void openFeedBagPicker() {
        if (router.hasOverlay()) return;

        StackPane scrim = new StackPane();
        scrim.getStyleClass().add("overlay-scrim");
        scrim.setAlignment(Pos.CENTER);

        VBox card = new VBox(12);
        card.getStyleClass().add("parchment-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setMaxHeight(380);

        Label title = new Label("FOOD BAG — pick animal food");
        title.getStyleClass().add("picker-title");

        VBox listBox = new VBox(8);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(260);
        scroll.getStyleClass().add("transparent-scroll");

        boolean any = false;
        if (player != null) {
            for (java.util.Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
                Item item = e.getKey();
                int qty = e.getValue();
                if (qty > 0 && (item instanceof model.game_item.AnimalFeedItem || item instanceof model.game_item.HarvestedCropItem)) {
                    any = true;
                    listBox.getChildren().add(makeFeedBagRow(item, qty));
                }
            }
        }

        if (!any) {
            Label empty = new Label("No animal food yet — visit the Shop!");
            empty.getStyleClass().add("seed-name");
            listBox.getChildren().add(empty);
        }

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("CANCEL");
        cancelBtn.getStyleClass().add("btn-wood");
        cancelBtn.setOnAction(e -> {
            router.closeOverlay();
            rootPane.requestFocus();
        });

        card.getChildren().addAll(title, scroll, cancelBtn);
        scrim.getChildren().add(card);
        router.showOverlay(scrim);
    }

    private HBox makeFeedBagRow(final Item foodItem, int qty) {
        Image iconImg = Assets.getShopIcon(foodItem);
        if (iconImg == null) iconImg = Assets.BAG_ICON;
        ImageView icon = Assets.imageView(iconImg, 36);

        Label name = new Label(foodItem.getName() + "  x" + qty);
        name.getStyleClass().add("seed-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        javafx.scene.control.Button use = new javafx.scene.control.Button("USE");
        use.getStyleClass().add("btn-gold");
        use.setMinWidth(Region.USE_PREF_SIZE);
        use.setDisable(qty <= 0);
        use.setOnAction(e -> {
            feedBag.loadFeed(foodItem.getName());
            selectTool(feedBag, bagTool);
            updateBagToolIcon(foodItem.getName());
            notify("Loaded " + foodItem.getName());
            router.closeOverlay();
            rootPane.requestFocus();
        });

        HBox row = new HBox(12, icon, name, spacer, use);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("seed-row");
        return row;
    }

    private void openSeedPicker() {
        if (router.hasOverlay()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Screens.SEED_PICKER_FXML));
            Parent overlay = loader.load();
            SeedPickerController picker = loader.getController();
            picker.init(player,
                    name -> {
                        seedBag.loadSeed(name);
                        selectTool(seedBag, seedTool);
                        updateSeedToolIcon(name);
                        InGameController.this.notify("Loaded " + name);
                    },
                    () -> router.closeOverlay()
            );
            router.showOverlay(overlay);
        } catch (IOException ex) {
            throw new SmartFarmException("Could not open the seed picker", ex);
        }
    }

    // ======================== rendering (Tối ưu 60 FPS, không khựng khi chuyển ngày/thời tiết) ========================
    private void refreshAll() {
        if (tiles != null) {
            for (Tile[] row : tiles) {
                for (Tile t : row) t.style();
            }
        }
        refreshHudOnly();
    }

    /** Chỉ cập nhật các ô đất canh tác / có cây / có sâu (nhanh gấp 40x so với duyệt toàn bộ 2080 ô). */
    private void refreshDynamicTiles() {
        if (tiles != null) {
            for (Tile[] row : tiles) {
                for (Tile t : row) {
                    if (t.needsDynamicRefresh()) {
                        t.style();
                    }
                }
            }
        }
        refreshHudOnly();
    }

    /** Chỉ cập nhật HUD, đồng hồ và hiệu ứng thời tiết mượt (0ms khựng, không duyệt lại ô map). */
    private void refreshHudOnly() {
        lastDisplayedMinute = -1;
        updateTimeDisplay();
        syncWeatherToggles();
        applyWeatherTheme();
        if (hovered != null) updateHover(hovered);
    }

    private void applyWeatherTheme() {
        WeatherType type = gameManager.getCurrentWeather() != null
                ? gameManager.getCurrentWeather().getType()
                : WeatherType.SUNNY;
        // Chuyển sắc thái thời tiết mượt mà qua weatherTintRect (tránh đổi CSS trên rootPane làm lag toàn bộ scene)
        switch (type) {
            case RAINY:
                targetWeatherTint = Color.rgb(58, 108, 168, 0.10);
                break;
            case DROUGHT:
                targetWeatherTint = Color.rgb(255, 244, 198, 0.05);
                break;
            case SUNNY:
            default:
                targetWeatherTint = Color.TRANSPARENT;
                break;
        }
        syncCampfireWeatherState();
    }

    private boolean lastSoilPanelVisible = false;
    private Cell lastAutoSoilCell = null;

    private boolean isPlayerInFarmZone() {
        if (farmZone == null || isOnBoat) return false;
        if (boatEntity != null && boatEntity.isLockedToBoat()) return false;
        return farmZone.contains(playerX + 22.0, playerY + 52.0);
    }

    /** Hiện bảng thông số đất khi vào Farming Zone và tự ẩn hoàn toàn khi rời khỏi khu vực đó. */
    private void updateSoilStatusPanelVisibility(boolean lockedOnBoat, Cell facingCell) {
        boolean inFarm = !lockedOnBoat && isPlayerInFarmZone();
        if (soilStatusPanel != null && (inFarm != lastSoilPanelVisible || soilStatusPanel.isVisible() != inFarm)) {
            lastSoilPanelVisible = inFarm;
            soilStatusPanel.setVisible(inFarm);
            soilStatusPanel.setManaged(inFarm);
        }
        if (inFarm) {
            Cell inspectCell = isCellInFarmZone(facingCell) ? facingCell : null;
            if (inspectCell == null && farmMap != null) {
                int col = (int) ((playerX + 22.0) / Constant.CELL_SIZE);
                int row = (int) ((playerY + 52.0) / Constant.CELL_SIZE);
                Cell underFoot = farmMap.getCell(row, col);
                if (isCellInFarmZone(underFoot)) inspectCell = underFoot;
            }
            if (inspectCell != null && inspectCell != lastAutoSoilCell) {
                lastAutoSoilCell = inspectCell;
                updateHover(inspectCell);
            }
        } else {
            lastAutoSoilCell = null;
        }
    }

    private void updateHover(Cell cell) {
        if (cell == null) return;
        if (!isPlayerInFarmZone() || !isCellInFarmZone(cell)) {
            if (soilStatusPanel != null && !isPlayerInFarmZone()) {
                soilStatusPanel.setVisible(false);
                soilStatusPanel.setManaged(false);
                lastSoilPanelVisible = false;
            }
            return;
        }
        hovered = cell;
        if (soilStatusPanel != null && !soilStatusPanel.isVisible()) {
            soilStatusPanel.setVisible(true);
            soilStatusPanel.setManaged(true);
            lastSoilPanelVisible = true;
        }
        cellLabel.setText("CELL : " + cell.getY() + "," + cell.getX());
        waterLabel.setText("WATER : " + cell.getMoistureLevel());
        nutrientLabel.setText("NUTR : " + cell.getNutrientLevel());
        Crop crop = cell.getCrop();
        if (crop == null) {
            cropLabel.setText("CROP : NONE");
        } else {
            cropLabel.setText("CROP : " + crop.getName()
                    + " [" + crop.getCurrentState() + "] "
                    + crop.getCurrentGrowthProgress() + "%");
        }
    }

    private void syncWeatherToggles() {
        WeatherType cur = gameManager.getCurrentWeather() != null
                ? gameManager.getCurrentWeather().getType() : null;
        sunWeatherButton.setSelected(cur == WeatherType.SUNNY);
        rainWeatherButton.setSelected(cur == WeatherType.RAINY);
        dryWeatherButton.setSelected(cur == WeatherType.DROUGHT);
    }

    private String weatherName() {
        return gameManager.getCurrentWeather() != null
                ? String.valueOf(gameManager.getCurrentWeather().getType()) : "-";
    }

    private void notify(String msg) {
        notificationField.setText(msg == null || msg.isEmpty() ? "..." : msg);
    }

    // ======================== icons (Giữ nguyên) ========================
    private void setupToolIcons() {
        hoeTool.setText(null);
        if (axeTool != null) axeTool.setText(null);
        waterTool.setText(null);
        seedTool.setText(null);
        handTool.setText(null);
        fertilizerTool.setText(null);
        hoeTool.setGraphic(Assets.imageView(Assets.HOE_ICON, 48));
        if (axeTool != null) axeTool.setGraphic(Assets.imageView(Assets.AXE_ICON, 48));
        waterTool.setGraphic(Assets.imageView(Assets.WATER_HOTBAR_ICON != null ? Assets.WATER_HOTBAR_ICON : Assets.WATER_ICON, 48));
        seedTool.setGraphic(Assets.imageView(Assets.SEED_ICON, 48));
        handTool.setGraphic(Assets.imageView(Assets.HAND_ICON, 48));
        fertilizerTool.setGraphic(Assets.imageView(Assets.FERTILIZER_ICON, 48));
        if (bagTool != null) {
            bagTool.setText(null);
            bagTool.setGraphic(Assets.imageView(Assets.BAG_ICON, 48));
        }
    }

    private void setupWeatherIcons() {
        sunWeatherButton.setText(null);
        rainWeatherButton.setText(null);
        dryWeatherButton.setText(null);
        sunWeatherButton.setGraphic(Assets.imageView(Assets.SUN_ICON, 42));
        rainWeatherButton.setGraphic(Assets.imageView(Assets.RAIN_ICON, 42));
        dryWeatherButton.setGraphic(Assets.imageView(Assets.DRY_ICON, 42));
    }

    private void resetSeedToolIcon() {
        seedTool.setGraphic(Assets.imageView(Assets.SEED_ICON, 48));
    }

    private void resetBagToolIcon() {
        if (bagTool != null) {
            bagTool.setGraphic(Assets.imageView(Assets.BAG_ICON, 48));
        }
    }

    private void updateBagToolIcon(String feedName) {
        if (bagTool == null) return;
        Image sprite = null;
        Item item = player.getInventory().findByName(feedName);
        if (item != null) {
            sprite = Assets.getShopIcon(item);
        }
        if (sprite == null) {
            sprite = Assets.BAG_ICON;
        }
        bagTool.setGraphic(Assets.imageView(sprite, 48));
    }

    private void updateSeedToolIcon(String seedName) {
        Image sprite = null;
        Item item = player.getInventory().findByName(seedName);
        if (item != null) {
            sprite = Assets.getShopIcon(item);
        }
        if (sprite == null) {
            sprite = Assets.SEED_ICON;
        }
        seedTool.setGraphic(Assets.imageView(sprite, 48));
    }

    // Border tái sử dụng cho Tile (tránh cấp phát 2080 đối tượng Border mới mỗi lần refresh)
    private static final Border TILE_BORDER_NONE = Border.EMPTY;
    private static final Border TILE_BORDER_HARVEST = new Border(
            new BorderStroke(Color.web("#F6D46F"), BorderStrokeStyle.SOLID, TILE_RADII, new BorderWidths(3)));
    private static final Border TILE_BORDER_DEAD = new Border(
            new BorderStroke(Color.web("#B23A2A"), BorderStrokeStyle.SOLID, TILE_RADII, new BorderWidths(2.5)));

    // ======================== cell tile (Tối ưu cache, 0 cấp phát thừa) ========================
private final class Tile {
        final Cell cell;
        final StackPane pane = new StackPane();
        final ImageView backgroundView = new ImageView();
        final ImageView pestMark = Assets.imageView(Assets.BEETLE_ICON, 20);
        final javafx.scene.shape.Rectangle dirtOverlay = new javafx.scene.shape.Rectangle(Constant.CELL_SIZE + 1.5, Constant.CELL_SIZE + 1.5, Color.rgb(0, 0, 0, 0.2));
        final ImageView cropView = new ImageView();
        private ImageView entityTreeView = null; // Cây ăn quả lâu năm vẽ trực tiếp trên entityPane để không bị ô gạch cạnh cắt tán & xếp lớp 2.5D chuẩn với cột cổng/rào/Luffy
        final ImageView waterNeedIcon = new ImageView(Assets.WATER_NEED_ICON != null ? Assets.WATER_NEED_ICON : Assets.WATER_ICON);
        // Thanh lực báo quá trình lớn lên & chín (34x5 px, nền tối viền đen mỏng + chấm sao vàng khi chín 100%)
        final javafx.scene.layout.HBox growthBarGroup = new javafx.scene.layout.HBox(2);
        final javafx.scene.shape.Rectangle growthBarFill = new javafx.scene.shape.Rectangle(0, 3);
        final javafx.scene.shape.Polygon ripeStar = new javafx.scene.shape.Polygon(
            0, -4.2, 1.3, -1.3, 4.2, 0, 1.3, 1.3,
            0, 4.2, -1.3, 1.3, -4.2, 0, -1.3, -1.3
        );
        final double bobPhase;

        // Cache trạng thái vẽ để bỏ qua các ô không thay đổi
        private int lastGid = Integer.MIN_VALUE;
        private boolean lastTilled = false;
        private boolean lastWet = false;
        private int lastMoistureBucket = -1;
        private int lastBorderKind = -1; // 0=none, 1=harvest, 2=dead
        private Image lastCropImg = null;
        private boolean lastWaterNeedVisible = false;
        private boolean lastBarVisible = false;
        private boolean lastBarRipe = false;
        private boolean lastBarDry = false;
        private double lastBarWidth = -1.0;
        private boolean lastPestVisible = false;
        private boolean hadDynamicState = false;

        boolean needsDynamicRefresh() {
            return hadDynamicState
                    || cell.getTerrain() == TerrainType.DIRT
                    || cell.getCellState() == CellState.TILLED
                    || cell.getCrop() != null
                    || cell.getPest() != null;
        }

        Tile(final Cell cell) {
            this.cell = cell;
            this.bobPhase = (cell.getX() * 0.7 + cell.getY() * 1.1);
            
            // Cài đặt kích thước cho ảnh nền lấp đầy ô đất, cộng thêm 1 pixel để đè lên viền/rãnh gạch (grid lines)
            backgroundView.setFitWidth(Constant.CELL_SIZE + 1.5);
            backgroundView.setFitHeight(Constant.CELL_SIZE + 1.5);
            backgroundView.setSmooth(false); // Fix: Tắt chế độ làm mờ để pixel art luôn sắc nét

            // Ảnh cây 4 giai đoạn sắc nét, căn gốc nằm gọn trên ô đất
            cropView.setFitWidth(54);
            cropView.setFitHeight(54);
            cropView.setPreserveRatio(true);
            cropView.setSmooth(false);
            StackPane.setAlignment(cropView, Pos.BOTTOM_CENTER);
            cropView.setTranslateY(-5.0);

            waterNeedIcon.setFitWidth(22);
            waterNeedIcon.setFitHeight(22);
            waterNeedIcon.setPreserveRatio(true);
            waterNeedIcon.setSmooth(false);
            waterNeedIcon.setMouseTransparent(true);
            waterNeedIcon.setVisible(false);
            StackPane.setAlignment(waterNeedIcon, Pos.TOP_CENTER);

            // Dựng thanh lực nhỏ 34x5 px (nền tối viền đen mỏng + thanh màu + chấm sao vàng khi chín)
            javafx.scene.shape.Rectangle barTrack = new javafx.scene.shape.Rectangle(34, 5);
            barTrack.setFill(Color.rgb(22, 16, 12, 0.90));
            barTrack.setStroke(Color.rgb(10, 8, 6));
            barTrack.setStrokeWidth(1.0);
            growthBarFill.setFill(Color.web("#4CAF50"));
            StackPane barStack = new StackPane(barTrack, growthBarFill);
            barStack.setAlignment(Pos.CENTER_LEFT);
            barStack.setMinSize(34, 5);
            barStack.setPrefSize(34, 5);
            barStack.setMaxSize(34, 5);
            StackPane.setMargin(growthBarFill, new javafx.geometry.Insets(0, 0, 0, 1));

            ripeStar.setFill(Color.web("#FFD700"));
            ripeStar.setStroke(Color.rgb(112, 68, 8));
            ripeStar.setStrokeWidth(0.8);
            ripeStar.setVisible(false);
            ripeStar.setManaged(false);

            growthBarGroup.setAlignment(Pos.CENTER);
            growthBarGroup.setMouseTransparent(true);
            growthBarGroup.setVisible(false);
            growthBarGroup.getChildren().addAll(barStack, ripeStar);
            StackPane.setAlignment(growthBarGroup, Pos.BOTTOM_CENTER);
            growthBarGroup.setTranslateY(-2.0);

            pane.setMinSize(Constant.CELL_SIZE, Constant.CELL_SIZE);
            pane.setPrefSize(Constant.CELL_SIZE, Constant.CELL_SIZE);
            pane.setCursor(Cursor.HAND);
            
            // QUAN TRỌNG: Thêm backgroundView vào đầu tiên để nó nằm dưới cùng (làm nền)
            dirtOverlay.setVisible(false);
            pestMark.setVisible(false);
            if (cell.getTerrain() == TerrainType.DIRT && tmxMap != null) {
                javafx.scene.image.Image grassImg = tmxMap.getTileImage(1);
                if (grassImg != null) {
                    ImageView grassUnderlay = new ImageView(grassImg);
                    grassUnderlay.setFitWidth(Constant.CELL_SIZE);
                    grassUnderlay.setFitHeight(Constant.CELL_SIZE);
                    grassUnderlay.setSmooth(false);
                    grassUnderlay.setMouseTransparent(true);
                    pane.getChildren().add(grassUnderlay);
                }
            }
            pane.getChildren().addAll(backgroundView, dirtOverlay, cropView, growthBarGroup, pestMark, waterNeedIcon);

            pane.setOnMouseClicked(e -> onCellClick(cell));
            pane.setOnMouseEntered(e -> updateHover(cell));

            style();
        }
// ...

void style() {
            int gid = 0;
            if (tmxMap != null && !tmxMap.layers.isEmpty()) {
                gid = tmxMap.layers.get(0).data[cell.getY()][cell.getX()];
            }
            if (cell.getTerrain() == TerrainType.DIRT) {
                gid = 3;
            }

            // 3 trạng thái đất hoàn toàn tách biệt (CHỈ áp dụng trên các luống đất DIRT, lối đi cỏ luôn giữ màu cỏ xanh):
            // 1) Chưa cuốc (UNTILLED): đất phẳng màu vàng cát sáng (UNPLOWED_BED_TILE) -> KHÔNG trồng được
            // 2) Đã cuốc - Khô (TILLED, moisture < 55): đất nâu ấm có rãnh cày ngang (TILLED_DRY_TILE) -> trồng được ngay
            // 3) Đã cuốc - Ướt (TILLED, moisture >= 55): đất nâu sẫm ẩm ướt tối hơn ~30% (TILLED_WET_TILE)
            boolean isDirtPlot = (cell.getTerrain() == TerrainType.DIRT);
            boolean tilled = isDirtPlot && (cell.getCellState() == CellState.TILLED);
            boolean wet = tilled && (cell.getMoistureLevel() >= 55);
            int visualKey = tilled ? (wet ? -3 : -2) : gid;
            if (visualKey != lastGid || tilled != lastTilled || wet != lastWet) {
                lastGid = visualKey;
                lastTilled = tilled;
                lastWet = wet;
                javafx.scene.image.Image img;
                if (tilled && Assets.FARM_PLOT != null) {
                    img = Assets.FARM_PLOT;
                    backgroundView.setSmooth(false);
                    backgroundView.setScaleX(1.0);
                    backgroundView.setScaleY(1.0);
                    if (wet) {
                        backgroundView.setEffect(new javafx.scene.effect.ColorAdjust(0.0, 0.05, -0.22, 0.0));
                    } else {
                        backgroundView.setEffect(null);
                    }
                } else if (tilled) {
                    img = wet ? Assets.TILLED_WET_TILE : Assets.TILLED_DRY_TILE;
                    backgroundView.setScaleX(1.0);
                    backgroundView.setScaleY(1.0);
                    backgroundView.setEffect(null);
                } else if (gid == 3 && Assets.UNPLOWED_BED_TILE != null) {
                    img = Assets.UNPLOWED_BED_TILE;
                    backgroundView.setSmooth(false);
                    backgroundView.setScaleX(1.0);
                    backgroundView.setScaleY(1.0);
                    backgroundView.setEffect(null);
                } else {
                    backgroundView.setEffect(null);
                    img = (gid > 0 && tmxMap != null) ? tmxMap.getTileImage(gid) : null;
                    backgroundView.setScaleX(1.0);
                    backgroundView.setScaleY(1.0);
                }
                backgroundView.setImage(img);
                dirtOverlay.setVisible(false);
            }

            // Viền chọn ô đất — dùng static Border, chỉ đổi khi khác loại viền
            Crop crop = cell.getCrop();
            int borderKind = 0;
            if (crop != null && crop.getCurrentState() == GrowthState.HARVEST) {
                borderKind = 1;
            } else if (crop != null && (crop.getCurrentState() == GrowthState.DEAD
                    || crop.getCurrentState() == GrowthState.ROTTEN)) {
                borderKind = 2;
            }
            if (borderKind != lastBorderKind) {
                lastBorderKind = borderKind;
                pane.setBorder(borderKind == 1 ? TILE_BORDER_HARVEST : (borderKind == 2 ? TILE_BORDER_DEAD : TILE_BORDER_NONE));
            }

            // Hiển thị cây trồng 4 giai đoạn & thanh lực báo lớn/chín (34x5 px)
            Image cropImg = (crop == null) ? null : Assets.getCropSprite(crop);
            boolean isBigTree = (crop instanceof model.crop.PerennialFruitTree);
            if (isBigTree && entityPane != null && cropImg != null) {
                // Vẽ cây ăn quả cỡ lớn (132x132) trực tiếp trên entityPane:
                // - Không bao giờ bị ô cỏ/đường đá bên cạnh cắt mất tán lá
                // - Xếp lớp 2.5D chuẩn theo trục Y (nằm TRƯỚC hàng rào & trụ cổng phía sau, Luffy đi trước/sau tán cây tự nhiên)
                if (cropView.getImage() != null) {
                    cropView.setImage(null);
                }
                if (entityTreeView == null) {
                    entityTreeView = new ImageView();
                    entityTreeView.setSmooth(false);
                    entityTreeView.setPreserveRatio(false);
                    entityTreeView.setManaged(false);
                    entityTreeView.setMouseTransparent(true);
                    double treeSize = 132.0;
                    entityTreeView.setFitWidth(treeSize);
                    entityTreeView.setFitHeight(treeSize);
                    entityTreeView.setLayoutX(cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5 - treeSize * 0.5);
                    entityTreeView.setLayoutY(cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.72 - treeSize);
                    entityTreeView.setUserData(cell.getY() * Constant.CELL_SIZE + 34.0);
                    entityPane.getChildren().add(entityTreeView);
                } else if (!entityPane.getChildren().contains(entityTreeView)) {
                    entityPane.getChildren().add(entityTreeView);
                }
                if (cropImg != lastCropImg) {
                    lastCropImg = cropImg;
                    entityTreeView.setImage(cropImg);
                }
                pane.setViewOrder(0.0);
            } else {
                if (entityTreeView != null) {
                    if (entityPane != null) {
                        entityPane.getChildren().remove(entityTreeView);
                    }
                    entityTreeView = null;
                }
                if (cropImg != lastCropImg) {
                    lastCropImg = cropImg;
                    cropView.setImage(cropImg);
                }
                cropView.setFitWidth(Constant.CELL_SIZE - 4.0);
                cropView.setFitHeight(Constant.CELL_SIZE - 4.0);
                cropView.setTranslateY(0.0);
                pane.setViewOrder(0.0);
            }
            boolean isAliveCrop = (crop != null
                    && crop.getCurrentState() != GrowthState.DEAD
                    && crop.getCurrentState() != GrowthState.ROTTEN);
            boolean isRipe = isAliveCrop && (crop.getCurrentState() == GrowthState.HARVEST || crop.getCurrentGrowthProgress() >= 100);
            boolean isGrowing = isAliveCrop && !isRipe;
            boolean isDryCrop = isGrowing && cell.getMoistureLevel() < 55;

            if (isAliveCrop != lastBarVisible) {
                lastBarVisible = isAliveCrop;
                growthBarGroup.setVisible(isAliveCrop);
            }
            if (isAliveCrop) {
                double pct = isRipe ? 1.0 : Math.max(0.0, Math.min(0.99, crop.getCurrentGrowthProgress() / 100.0));
                double targetW = isRipe ? 32.0 : Math.max(2.0, 32.0 * pct);
                if (Math.abs(targetW - lastBarWidth) > 0.25) {
                    lastBarWidth = targetW;
                    growthBarFill.setWidth(targetW);
                }
                if (isRipe != lastBarRipe || isDryCrop != lastBarDry) {
                    lastBarRipe = isRipe;
                    lastBarDry = isDryCrop;
                    if (isRipe) {
                        growthBarFill.setFill(Color.web("#FFD700"));
                        ripeStar.setVisible(true);
                        ripeStar.setManaged(true);
                        ripeCropTiles.add(this);
                    } else {
                        growthBarFill.setFill(isDryCrop ? Color.web("#FFA726") : Color.web("#4CAF50"));
                        growthBarFill.setOpacity(1.0);
                        ripeStar.setVisible(false);
                        ripeStar.setManaged(false);
                        ripeCropTiles.remove(this);
                    }
                }
            } else if (lastBarRipe) {
                lastBarRipe = false;
                ripeStar.setVisible(false);
                ripeStar.setManaged(false);
                ripeCropTiles.remove(this);
            }

            // Icon bình tưới màu xanh dương nổi trên ô đã gieo hạt/trồng cây đang thiếu nước (< 55% độ ẩm)
            boolean needsWater = isDryCrop;
            if (needsWater != lastWaterNeedVisible) {
                lastWaterNeedVisible = needsWater;
                waterNeedIcon.setVisible(needsWater);
                if (needsWater) {
                    waterNeedTiles.add(this);
                } else {
                    waterNeedTiles.remove(this);
                }
            }

            boolean hasPest = (cell.getPest() != null);
            if (hasPest != lastPestVisible) {
                lastPestVisible = hasPest;
                pestMark.setVisible(hasPest);
            }

            hadDynamicState = (tilled || crop != null || hasPest);
        }

    private static String lerpHex(String a, String b, double t) {
        t = Math.max(0, Math.min(1, t));
        int[] ca = rgb(a), cb = rgb(b);
        int r  = (int) Math.round(ca[0] + (cb[0] - ca[0]) * t);
        int g  = (int) Math.round(ca[1] + (cb[1] - ca[1]) * t);
        int bl = (int) Math.round(ca[2] + (cb[2] - ca[2]) * t);
        return String.format("#%02X%02X%02X", r, g, bl);
    }

    private static int[] rgb(String hex) {
        return new int[]{
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16)
        };
    }
}

    // ======================== ANIMAL NPCs ========================
    private java.util.List<Animal> animals = new java.util.ArrayList<>();
    /** Khu vực chăn thả tổng (AnimalZone trong TMX); null nếu map không định nghĩa. */
    private javafx.geometry.Rectangle2D animalZone;
    /** 3 chuồng chăn nuôi riêng biệt từ trái sang phải: Chuồng Gà | Chuồng Lợn | Chuồng Bò. */
    private javafx.geometry.Rectangle2D chickenZone;
    private javafx.geometry.Rectangle2D pigZone;
    private javafx.geometry.Rectangle2D cowZone;
    /** Danh sách hitbox va chạm chân đế của các vật thể rắn trong 3 chuồng (KHÔNG chặn vũng bùn). */
    private final java.util.List<javafx.geometry.Rectangle2D> animalFarmColliders = new java.util.ArrayList<>();
    private final java.util.List<ImageView> animalPropViews = new java.util.ArrayList<>();
    /** Khu vực canh tác (FarmZone trong TMX); giới hạn phạm vi hoạt động của Cuốc. */
    private javafx.geometry.Rectangle2D farmZone;

    private ImageView createFixedAnimalPropView(javafx.scene.image.Image img, double fitWidth, double x, double y, double sortY, String id) {
        ImageView iv = new ImageView(img);
        iv.setSmooth(false);
        iv.setPreserveRatio(true);
        iv.setFitWidth(fitWidth);
        iv.setLayoutX(x);
        iv.setLayoutY(y);
        iv.setMouseTransparent(true);
        iv.setId(id);
        iv.setUserData(sortY);
        return iv;
    }

    private double propScaledHeight(javafx.scene.image.Image img, double fitWidth, double fallbackHeight) {
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            return fitWidth * (img.getHeight() / img.getWidth());
        }
        return fallbackHeight;
    }

    /** Chia bãi cỏ gốc thành 3 chuồng khép kín (Gà | Lợn | Bò) khớp hàng rào dọc/ngang và khởi tạo hitbox chân đế. */
    private void initAnimalFarmLayout() {
        animalFarmColliders.clear();
        if (animalZone == null) {
            chickenZone = null;
            pigZone = null;
            cowZone = null;
            return;
        }
        double minY = animalZone.getMinY(); // ~1516.8
        double maxY = Math.min(animalZone.getMaxY(), 1966.0); // Dừng phía trên hàng rào ngang cạnh dưới (y=1252*1.6=2003.2)
        double minX = animalZone.getMinX(); // ~1576.0
        double maxX = animalZone.getMaxX(); // ~2464.0

        // 2 vách rào dọc TMX (x=1166, w=20 -> World 1865.6..1897.6; x=1354, w=20 -> World 2166.4..2198.4)
        double div1Left  = 1166.0 * 1.6;
        double div1Right = 1186.0 * 1.6;
        double div2Left  = 1354.0 * 1.6;
        double div2Right = 1374.0 * 1.6;

        // 1. Vùng đi lại riêng biệt cho 3 loài thú (trái -> giữa -> phải, khép kín phía trên rào dưới):
        //    • Khu 1 — Chuồng Gà (Bên trái)
        chickenZone = new javafx.geometry.Rectangle2D(minX + 10.0, minY + 132.0, (div1Left - 8.0) - (minX + 10.0), maxY - (minY + 132.0));
        //    • Khu 2 — Chuồng Lợn (Ở giữa, chứa vũng bùn ở tâm)
        pigZone     = new javafx.geometry.Rectangle2D(div1Right + 8.0, minY + 70.0, (div2Left - 8.0) - (div1Right + 8.0), maxY - (minY + 70.0));
        //    • Khu 3 — Chuồng Bò (Bên phải)
        cowZone     = new javafx.geometry.Rectangle2D(div2Right + 8.0, minY + 70.0, (maxX - 8.0) - (div2Right + 8.0), maxY - (minY + 70.0));

        // 2. Hitbox va chạm chân đế (nửa dưới) của các vật thể rắn trong 3 chuồng (KHÔNG chặn prop_mud_puddle)
        double coopH  = propScaledHeight(Assets.PROP_CHICKEN_COOP, 110.0, 122.0);
        double wtrfH  = propScaledHeight(Assets.PROP_WOOD_TROUGH,   72.0,  50.0);
        double watrH  = propScaledHeight(Assets.PROP_WATER_TROUGH,  84.0,  62.0);
        double hayH   = propScaledHeight(Assets.PROP_HAY_BALE,      64.0,  56.0);

        // Chuồng gà gỗ (110px) tại góc trên trái Khu 1
        double coopX = minX + 8.0;
        double coopY = minY + 6.0;
        animalFarmColliders.add(new javafx.geometry.Rectangle2D(coopX + 8.0, coopY + coopH * 0.50, 110.0 - 16.0, coopH * 0.46));

        // Máng thức ăn gỗ (72px) sát hàng rào trên bên trái Khu 2 (Chuồng Lợn)
        double woodX = div1Right + 6.0;
        double woodY = minY + 8.0;
        animalFarmColliders.add(new javafx.geometry.Rectangle2D(woodX + 5.0, woodY + wtrfH * 0.48, 72.0 - 10.0, wtrfH * 0.46));

        // Máng nước đá (84px) dịch sang giữa-trên Khu 3 (Chuồng Bò), cách mép phải >220px để né hoàn toàn bảng HUD bên phải
        double waterX = div2Right + 50.0;
        double waterY = minY + 106.0;
        animalFarmColliders.add(new javafx.geometry.Rectangle2D(waterX + 5.0, waterY + watrH * 0.48, 84.0 - 10.0, watrH * 0.46));

        // Đống rơm vàng (64px) đặt gần máng nước đá (~1.5 ô) cho cân đối và né HUD bên phải
        double hayX = div2Right + 20.0;
        double hayY = minY + 216.0;
        animalFarmColliders.add(new javafx.geometry.Rectangle2D(hayX + 5.0, hayY + hayH * 0.48, 64.0 - 10.0, hayH * 0.46));
    }

    /** Đặt 6 vật thể + 3 biển gỗ (CHICKEN, PIG, COW) + hoa cỏ chân rào vào 3 chuồng với kích thước khóa cứng. */
    private void mountAnimalPenProps() {
        if (entityPane == null || animalZone == null) return;
        for (ImageView old : animalPropViews) {
            entityPane.getChildren().remove(old);
        }
        animalPropViews.clear();

        double minX = animalZone.getMinX();
        double minY = animalZone.getMinY();
        double maxY = Math.min(animalZone.getMaxY(), 1966.0);
        double div1Right = 1186.0 * 1.6;
        double div2Left  = 1354.0 * 1.6;
        double div2Right = 1374.0 * 1.6;

        // 3 BIỂN GỖ (fitWidth = 56, smooth = false) cắm thẳng hàng ngay cạnh lối vào phía trên của 3 chuồng
        double signFootY = minY - 2.0; // Nằm sát chân hàng rào phía trên (y=945*1.6=1512)
        if (Assets.SIGN_CHICKEN != null) {
            double sh = propScaledHeight(Assets.SIGN_CHICKEN, 56.0, 90.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.SIGN_CHICKEN, 56.0, minX + 26.0, signFootY - sh, signFootY + 2.0, "sign_chicken"));
        }
        if (Assets.SIGN_PIG != null) {
            double sh = propScaledHeight(Assets.SIGN_PIG, 56.0, 96.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.SIGN_PIG, 56.0, div1Right + 10.0, signFootY - sh, signFootY + 2.0, "sign_pig"));
        }
        if (Assets.SIGN_COW != null) {
            double sh = propScaledHeight(Assets.SIGN_COW, 56.0, 96.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.SIGN_COW, 56.0, div2Right + 12.0, signFootY - sh, signFootY + 2.0, "sign_cow"));
        }

        // KHU 1 — CHUỒNG GÀ (Bên trái): Chuồng gà (110px) góc trên trái + Ổ rơm có trứng (52px) cạnh bên phải
        if (Assets.PROP_CHICKEN_COOP != null) {
            double coopX = minX + 8.0;
            double coopY = minY + 6.0;
            double coopH = propScaledHeight(Assets.PROP_CHICKEN_COOP, 110.0, 122.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_CHICKEN_COOP, 110.0, coopX, coopY, coopY + coopH, "prop_chicken_coop"));
        }
        if (Assets.PROP_STRAW_NEST != null) {
            double nestX = minX + 124.0;
            double nestY = minY + 82.0;
            double nestH = propScaledHeight(Assets.PROP_STRAW_NEST, 52.0, 39.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_STRAW_NEST, 52.0, nestX, nestY, nestY + nestH * 0.5, "prop_straw_nest"));
        }

        // KHU 2 — CHUỒNG LỢN (Ở giữa): Vũng bùn (140px) chính giữa dưới chân thú/người + Máng thức ăn gỗ (72px) sát rào trên trái
        if (Assets.PROP_MUD_PUDDLE != null) {
            double mudH = propScaledHeight(Assets.PROP_MUD_PUDDLE, 140.0, 68.0);
            double mudX = (div1Right + div2Left - 140.0) * 0.5;
            double mudY = minY + (maxY - minY) * 0.52 - mudH * 0.5;
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_MUD_PUDDLE, 140.0, mudX, mudY, -650.0, "prop_mud_puddle"));
        }
        if (Assets.PROP_WOOD_TROUGH != null) {
            double woodX = div1Right + 6.0;
            double woodY = minY + 8.0;
            double woodH = propScaledHeight(Assets.PROP_WOOD_TROUGH, 72.0, 50.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_WOOD_TROUGH, 72.0, woodX, woodY, woodY + woodH, "prop_wood_trough"));
        }

        // KHU 3 — CHUỒNG BÒ (Bên phải): Máng nước đá (84px) dịch sang giữa-trên né HUD phải + Đống rơm vàng (64px) ngay gần đó
        if (Assets.PROP_WATER_TROUGH != null) {
            double waterX = div2Right + 50.0;
            double waterY = minY + 106.0;
            double waterH = propScaledHeight(Assets.PROP_WATER_TROUGH, 84.0, 62.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_WATER_TROUGH, 84.0, waterX, waterY, waterY + waterH, "prop_water_trough"));
        }
        if (Assets.PROP_HAY_BALE != null) {
            double hayX = div2Right + 20.0;
            double hayY = minY + 216.0;
            double hayH = propScaledHeight(Assets.PROP_HAY_BALE, 64.0, 56.0);
            animalPropViews.add(createFixedAnimalPropView(Assets.PROP_HAY_BALE, 64.0, hayX, hayY, hayY + hayH, "prop_hay_bale"));
        }

        // Điểm xuyết vài cụm cỏ hoa nhỏ dưới chân hàng rào cho sinh động (nằm dưới chân, không chặn đi lại)
        if (Assets.WILDFLOWERS != null) {
            double[][] flowers = new double[][] {
                { minX + 16.0,      maxY - 40.0 },
                { div1Right - 84.0, maxY - 38.0 },
                { div1Right - 72.0, minY + 166.0 },
                { div1Right + 14.0, maxY - 40.0 },
                { div2Right + 16.0, maxY - 40.0 },
                { div2Right + 136.0, maxY - 42.0 }
            };
            for (int i = 0; i < flowers.length; i++) {
                animalPropViews.add(createFixedAnimalPropView(
                    Assets.WILDFLOWERS, 34.0, flowers[i][0], flowers[i][1], -620.0, "animal_flower_" + i
                ));
            }
        }

        entityPane.getChildren().addAll(animalPropViews);
    }

    private javafx.geometry.Rectangle2D getZoneForAnimalType(int type) {
        if (type == 0 && chickenZone != null) return chickenZone;
        if (type == 1 && cowZone != null) return cowZone;
        if (type == 3 && pigZone != null) return pigZone;
        return animalZone;
    }

    private boolean isCellInFarmZone(Cell cell) {
        if (cell == null) return false;
        if (orchardUnlocked && cell.getY() >= 47 && cell.getY() <= 50 && cell.getX() >= 5 && cell.getX() <= 14) {
            return true;
        }
        if (farmZone == null) return true;
        double cx = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
        double cy = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
        return farmZone.contains(cx, cy);
    }

    private boolean tryClearWheatAtCell(Cell cell) {
        if (cell == null || !isCellInFarmZone(cell)) return false;
        double fcx = cell.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
        double fcy = cell.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
        ImageView wheatToClear = null;
        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id != null && id.toLowerCase().contains("wheat")) {
                double wx = objView.getLayoutX() + objView.getFitWidth() / 2.0;
                double wy = objView.getLayoutY() + objView.getFitHeight() / 2.0;
                if (Math.abs(wx - fcx) < 22 && Math.abs(wy - fcy) < 22) {
                    wheatToClear = objView;
                    break;
                }
            }
        }
        if (wheatToClear != null) {
            clearedSceneryKeys.add(sceneryKey(wheatToClear));
            sceneryObjects.remove(wheatToClear);
            if (entityPane != null) {
                entityPane.getChildren().remove(wheatToClear);
            } else {
                mapPane.getChildren().remove(wheatToClear);
            }
            notify("Đã đào sạch bụi lúa! Giờ có thể cuốc đất trồng cây.");
            return true;
        }
        return false;
    }

    // ======================== MÁNG ĂN TỰ ĐỘNG ========================
    private static final int TROUGH_CAPACITY = 10;
    private static final double[] MEAL_TIMES = { 10.0 / 24.0, 16.0 / 24.0 }; // 10:00 và 16:00
    private static final double MEAL_REMIND_LEAD = 0.75 / 24.0; // nhắc trước 45 phút trong game
    private static final double EVENING_FEED_WARN_PROGRESS = 20.0 / 24.0; // 20:00 tối
    private final boolean[] mealStarted = new boolean[2];
    private final boolean[] mealReminded = new boolean[2];
    private boolean eveningWarned = false;
    private java.util.List<FeedTrough> troughs = new java.util.ArrayList<>();

    /** Mỗi loại thú có một máng ăn riêng trong khu chăn thả. */
    private class FeedTrough {
        final int animalType;
        final String emoji;
        final String name;
        final double x, y;
        int amount = 0;
        final javafx.scene.Group view = new javafx.scene.Group();
        final javafx.scene.shape.Rectangle feedFill;
        final javafx.scene.text.Text text = new javafx.scene.text.Text();

        FeedTrough(int animalType, String emoji, String name, double x, double y) {
            this.animalType = animalType;
            this.emoji = emoji;
            this.name = name;
            this.x = x;
            this.y = y;

            javafx.scene.shape.Rectangle body = new javafx.scene.shape.Rectangle(70, 26, Color.web("#7A4F28"));
            body.setArcWidth(8); body.setArcHeight(8);
            body.setStroke(Color.web("#241A0E")); body.setStrokeWidth(2);
            javafx.scene.shape.Rectangle inner = new javafx.scene.shape.Rectangle(6, 5, 58, 14);
            inner.setFill(Color.web("#3A2910"));
            inner.setArcWidth(4); inner.setArcHeight(4);
            feedFill = new javafx.scene.shape.Rectangle(6, 5, 0, 14);
            feedFill.setFill(Color.web("#F0CC52"));
            feedFill.setArcWidth(4); feedFill.setArcHeight(4);

            text.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 3, 0.5, 0, 1);");
            text.setX(4);
            text.setY(-6);

            view.getChildren().addAll(body, inner, feedFill, text);
            view.setTranslateX(x);
            view.setTranslateY(y);
            view.setUserData(y + 26.0);
            refresh();
        }

        void refresh() {
            feedFill.setWidth(58.0 * amount / TROUGH_CAPACITY);
            text.setText(emoji + " " + amount + "/" + TROUGH_CAPACITY);
            text.setFill(amount == 0 ? Color.web("#FF8A80") : Color.WHITE);
        }

        boolean take() {
            if (amount <= 0) return false;
            amount--;
            refresh();
            return true;
        }

        int add(int n) {
            int added = Math.max(0, Math.min(n, TROUGH_CAPACITY - amount));
            amount += added;
            refresh();
            return added;
        }

        double spotX() {
            javafx.geometry.Rectangle2D z = getZoneForAnimalType(animalType);
            double sx = x + 15.0;
            if (z != null) {
                return Math.max(z.getMinX() + 6.0, Math.min(sx, z.getMaxX() - 36.0));
            }
            return sx;
        }

        double spotY() {
            javafx.geometry.Rectangle2D z = getZoneForAnimalType(animalType);
            double sy = y + 34.0;
            if (z != null) {
                return Math.max(z.getMinY() + 6.0, Math.min(sy, z.getMaxY() - 32.0));
            }
            return sy;
        }

        javafx.geometry.Rectangle2D bounds() {
            return new javafx.geometry.Rectangle2D(x - 14, y - 16, 98, 56);
        }
    }

    private void buildTroughs() {
        troughs = new java.util.ArrayList<>();
        if (animalZone == null || entityPane == null) return;
        // Giữ nguyên thứ tự chỉ số (0: Gà, 1: Bò, 2: Lợn) để tương thích tuyệt đối với file save cũ (trough.0/1/2)
        // Cả 3 thanh trạng thái máng ăn nằm thẳng hàng tăm tắp (cùng cao độ ty) ngay cạnh lối vào mỗi chuồng
        double ty = animalZone.getMinY() + 29.0;
        double div1Left  = 1166.0 * 1.6;
        double div2Left  = 1354.0 * 1.6;
        double div2Right = 1374.0 * 1.6;
        // 1. Gà (Khu 1 — Bên trái): thẳng hàng ngay bên phải lối vào Chuồng Gà
        troughs.add(new FeedTrough(0, "\uD83D\uDC14", "Gà",  div1Left - 78.0,  ty));
        // 2. Bò (Khu 3 — Bên phải): thẳng hàng ngay bên trái lối vào Chuồng Bò (né hoàn toàn bảng HUD bên phải)
        troughs.add(new FeedTrough(1, "\uD83D\uDC04", "Bò",  div2Right + 8.0,  ty));
        // 3. Lợn (Khu 2 — Ở giữa): thẳng hàng ngay bên phải lối vào Chuồng Lợn
        troughs.add(new FeedTrough(3, "\uD83D\uDC16", "Lợn", div2Left - 78.0,  ty));
        for (FeedTrough t : troughs) entityPane.getChildren().add(t.view);
    }

    private FeedTrough troughFor(int type) {
        for (FeedTrough t : troughs) if (t.animalType == type) return t;
        return null;
    }

    private boolean needsFeeding(Animal a) {
        return a.type != 2 && a.ageDays < 5 && !a.isFed;
    }

    /** Tên các máng không đủ thức ăn cho số thú còn đói (rỗng nếu đủ). */
    private String troughShortage() {
        StringBuilder sb = new StringBuilder();
        for (FeedTrough t : troughs) {
            int need = 0;
            for (Animal a : animals) if (a.type == t.animalType && needsFeeding(a)) need++;
            if (need > t.amount) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(t.name);
            }
        }
        return sb.toString();
    }

    private String clockText(double progress) {
        int mins = (int) Math.round(Math.max(0.0, Math.min(0.999999, progress)) * 24 * 60);
        return String.format("%02d:%02d", (mins / 60) % 24, mins % 60);
    }

    private void resetFeedingSchedule(double progress) {
        for (int i = 0; i < MEAL_TIMES.length; i++) {
            boolean passed = progress >= MEAL_TIMES[i];
            mealStarted[i] = passed;
            mealReminded[i] = passed || progress >= MEAL_TIMES[i] - MEAL_REMIND_LEAD;
        }
        eveningWarned = progress >= EVENING_FEED_WARN_PROGRESS;
    }

    private void updateFeedingSchedule() {
        if (troughs.isEmpty()) return;
        double p = (double) elapsedTimeInDay / DAY_DURATION_MS;
        long now = System.currentTimeMillis();

        for (int i = 0; i < MEAL_TIMES.length; i++) {
            if (!mealReminded[i] && p >= MEAL_TIMES[i] - MEAL_REMIND_LEAD) {
                mealReminded[i] = true;
                String s = troughShortage();
                if (!s.isEmpty()) {
                    showToast("\uD83D\uDD14 Sắp đến giờ ăn (" + clockText(MEAL_TIMES[i]) + ")! Máng thiếu thức ăn: " + s);
                }
            }
            if (!mealStarted[i] && p >= MEAL_TIMES[i]) {
                mealStarted[i] = true;
                boolean anyHungry = false;
                for (Animal a : animals) {
                    if (!needsFeeding(a)) continue;
                    anyHungry = true;
                    FeedTrough t = troughFor(a.type);
                    if (t != null && t.amount > 0) {
                        a.eatTrough = t;
                        a.eatUntil = now + 40000;
                    }
                }
                if (anyHungry) {
                    String s = troughShortage();
                    showToast(s.isEmpty()
                            ? "\uD83C\uDF7D Giờ ăn rồi! Thú nuôi đang ra máng."
                            : "\uD83C\uDF7D Giờ ăn! Thiếu thức ăn ở máng: " + s + ". Hãy bỏ Animal Feed vào!");
                }
            }
        }

        if (!eveningWarned && p >= EVENING_FEED_WARN_PROGRESS) {
            eveningWarned = true;
            int hungry = 0;
            for (Animal a : animals) if (needsFeeding(a)) hungry++;
            if (hungry > 0) {
                showToast("\u26A0 Còn " + hungry + " thú chưa được ăn hôm nay! Bỏ Animal Feed vào máng hoặc cho ăn trực tiếp.");
            }
        }
    }

    /** Thông báo nổi bật trên màn hình (tự mờ dần) để người chơi không bỏ lỡ. */
    private Label toastLabel;
    private javafx.animation.SequentialTransition toastAnim;

    private void showToast(String msg) {
        notify(msg);
        javafx.scene.layout.Pane hud = ensureHudOverlayPane();
        if (hud == null) return;
        if (toastLabel == null) {
            toastLabel = new Label();
            toastLabel.getStyleClass().add("npc-name");
            toastLabel.setWrapText(true);
            toastLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            toastLabel.setAlignment(Pos.CENTER);
            toastLabel.setStyle("-fx-font-size: 16px;");
            toastLabel.setMouseTransparent(true);
            toastLabel.setMinWidth(620);
            toastLabel.setPrefWidth(620);
            toastLabel.setMaxWidth(620);
            toastLabel.setLayoutY(80);
            hud.getChildren().add(toastLabel);
        }
        toastLabel.setText(msg);
        toastLabel.setLayoutX(Math.max(0, (rootPane.getWidth() - 620) / 2));
        toastLabel.toFront();
        toastLabel.setOpacity(1);
        if (toastAnim != null) toastAnim.stop();
        javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(6));
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), toastLabel);
        fade.setFromValue(1); fade.setToValue(0);
        toastAnim = new javafx.animation.SequentialTransition(hold, fade);
        toastAnim.play();
    }

    /** Đổ thức ăn từ Cặp / Kho vào máng đang đứng trước mặt. */
    private boolean tryDepositFeed(javafx.geometry.Rectangle2D hitBox) {
        for (FeedTrough t : troughs) {
            if (!hitBox.intersects(t.bounds())) continue;
            model.game_item.Item feed = findAvailableFeedItem(t.animalType);
            int have = feed != null ? player.getInventory().getItemCount(feed) : 0;
            if (have <= 0) {
                notify("Trong Cặp không có thức ăn - bấm vào icon Cặp (phím 6) hoặc mua ở Shop nhé!");
            } else if (t.amount >= TROUGH_CAPACITY) {
                notify("Máng " + t.name + " đã đầy!");
            } else {
                int added = t.add(have);
                player.getInventory().removeItem(feed, added);
                revertBagToolIfEmpty();
                notify("Đã đổ " + added + " " + feed.getName() + " vào máng " + t.name + " (" + t.amount + "/" + TROUGH_CAPACITY + ")");
            }
            rootPane.requestFocus();
            return true;
        }
        return false;
    }

    private double clampToAnimalZoneX(double px, int type) {
        javafx.geometry.Rectangle2D z = getZoneForAnimalType(type);
        if (z != null) {
            double padR = (type == 1) ? 46.0 : ((type == 3) ? 40.0 : 30.0);
            double minX = z.getMinX() + 4.0;
            double maxX = Math.max(minX, z.getMaxX() - padR);
            return Math.max(minX, Math.min(px, maxX));
        }
        double mapW = farmMap.getWidth() * Constant.CELL_SIZE;
        return Math.max(0, Math.min(px, mapW - 40));
    }

    private double clampToAnimalZoneY(double py, int type) {
        javafx.geometry.Rectangle2D z = getZoneForAnimalType(type);
        if (z != null) {
            double padB = (type == 1) ? 34.0 : ((type == 3) ? 28.0 : 30.0);
            double minY = z.getMinY() + 4.0;
            double maxY = Math.max(minY, z.getMaxY() - padB);
            return Math.max(minY, Math.min(py, maxY));
        }
        double mapH = farmMap.getHeight() * Constant.CELL_SIZE;
        return Math.max(0, Math.min(py, mapH - 40));
    }

    private boolean isAnimalBlockedAt(double px, double py, int type) {
        javafx.geometry.Rectangle2D z = getZoneForAnimalType(type);
        if (z != null) {
            double padR = (type == 1) ? 46.0 : ((type == 3) ? 40.0 : 30.0);
            double padB = (type == 1) ? 34.0 : ((type == 3) ? 28.0 : 30.0);
            double minX = z.getMinX() + 2.0;
            double maxX = Math.max(minX, z.getMaxX() - padR);
            double minY = z.getMinY() + 2.0;
            double maxY = Math.max(minY, z.getMaxY() - padB);
            if (px < minX || px > maxX || py < minY || py > maxY) {
                return true;
            }
        }
        int cCol = (int) ((px + 18) / Constant.CELL_SIZE);
        int cRow = (int) ((py + 22) / Constant.CELL_SIZE);
        if (farmMap.getCell(cRow, cCol) == null || farmMap.getCell(cRow, cCol).getTerrain() == TerrainType.WATER) {
            return true;
        }
        javafx.geometry.Rectangle2D animalBox = new javafx.geometry.Rectangle2D(px + 6, py + 12, 24, 18);
        for (javafx.geometry.Rectangle2D afBox : animalFarmColliders) {
            if (animalBox.intersects(afBox)) {
                return true;
            }
        }
        for (ImageView objView : sceneryObjects) {
            double ox = objView.getLayoutX() + objView.getTranslateX();
            double oy = objView.getLayoutY() + objView.getTranslateY();
            double ow = objView.getFitWidth();
            double oh = objView.getFitHeight();
            if (ow <= 0 || oh <= 0) continue;
            double hw = Math.min(30, ow * 0.45);
            double hh = Math.min(22, oh * 0.25);
            double cx = ox + (ow - hw) / 2;
            double cy = oy + oh - hh - 4;
            if (animalBox.intersects(cx, cy, hw, hh)) {
                return true;
            }
        }
        return false;
    }
    
    private class Animal {
        javafx.scene.Group view;
        javafx.scene.Node spriteNode;
        double x, y, targetX, targetY;
        double speed = 0.5 + Math.random() * 0.5;
        long pauseUntil = 0;
        int type;
        int ageDays = 0;
        boolean isFed = false;
        boolean producedToday = false;
        boolean facingLeft = false;
        javafx.scene.text.Text heartEmoji;
        javafx.scene.control.Label saleBadge;
        double badgeBaseY = -28;
        double bobSeed = Math.random() * 1000;
        FeedTrough eatTrough = null; // máng đang trên đường tới ăn
        long eatUntil = 0;

        // Các khớp xoay chân tay và chuyển động (Phong cách nhân vật Luffy)
        private javafx.scene.transform.Rotate legFLSwing, legFRSwing, legBLSwing, legBRSwing; // 4 chân cho Bò và Lợn
        private javafx.scene.transform.Rotate legLSwing, legRSwing; // 2 chân cho Gà
        private javafx.scene.transform.Rotate tailSwing;
        private javafx.scene.transform.Rotate wingSwing;
        private double walkStep = Math.random() * 10.0;

        public Animal(double startX, double startY, int type) {
            if (type != 2) {
                startX = clampToAnimalZoneX(startX, type);
                startY = clampToAnimalZoneY(startY, type);
                if (isAnimalBlockedAt(startX, startY, type)) {
                    javafx.geometry.Rectangle2D z = getZoneForAnimalType(type);
                    if (z != null) {
                        startX = clampToAnimalZoneX(z.getMinX() + z.getWidth() * 0.60, type);
                        startY = clampToAnimalZoneY(z.getMinY() + z.getHeight() * 0.65, type);
                    }
                }
            }
            this.x = startX; this.y = startY;
            this.targetX = startX; this.targetY = startY;
            this.type = type;
            this.view = new javafx.scene.Group();
            
            double spriteWidth = 30;

            if (type == 0) { // Gà (Chicken) - 2 chân di chuyển
                javafx.scene.Group chickenModel = createChickenModel();
                spriteNode = chickenModel;
                spriteWidth = 30;
                view.getChildren().add(chickenModel);
            } else if (type == 1) { // Bò (Cow) - 4 chân di chuyển
                javafx.scene.Group cowModel = createCowModel();
                spriteNode = cowModel;
                spriteWidth = 46;
                view.getChildren().add(cowModel);
            } else if (type == 2) { // Vịt trời (Duck)
                javafx.scene.Group duckGroup = new javafx.scene.Group();
                javafx.scene.shape.Polygon body = new javafx.scene.shape.Polygon(0,5, 12,0, 20,5, 12,10);
                body.setFill(Color.SADDLEBROWN);
                javafx.scene.shape.Polygon wing = new javafx.scene.shape.Polygon(10,5, 15,-10, 18,5);
                wing.setFill(Color.DARKGREEN);
                javafx.scene.shape.Rectangle head = new javafx.scene.shape.Rectangle(6, 5, Color.DARKGREEN);
                head.setX(16); head.setY(0);
                javafx.scene.shape.Rectangle beak = new javafx.scene.shape.Rectangle(4, 2, Color.ORANGE);
                beak.setX(22); beak.setY(2);
                duckGroup.getChildren().addAll(body, wing, head, beak);
                duckGroup.setScaleX(-1.5); duckGroup.setScaleY(1.5); // Bay hướng trái
                spriteNode = duckGroup;
                view.getChildren().add(duckGroup);
                speed = 2.0 + Math.random() * 1.5;
            } else if (type == 3) { // Lợn (Pig) - 4 chân di chuyển
                javafx.scene.Group pigModel = createPigModel();
                spriteNode = pigModel;
                spriteWidth = 40;
                view.getChildren().add(pigModel);
            }
            
            if (type != 2) {
                // Biểu tượng tim khi được cho ăn
                heartEmoji = new javafx.scene.text.Text("❤️");
                heartEmoji.setStyle("-fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 1);");
                heartEmoji.setVisible(false);
                heartEmoji.setManaged(false);
                heartEmoji.setX(spriteWidth / 2.0 - 7);
                view.getChildren().add(heartEmoji);

                // Bảng hiệu báo đủ điều kiện bán
                saleBadge = new javafx.scene.control.Label("💰 BÁN (" + getSellPrice() + "$)");
                saleBadge.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFE066, #FF9800);" +
                    "-fx-text-fill: #3E2723;" +
                    "-fx-font-family: 'Segoe UI', 'Arial', sans-serif;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 10px;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #FFFFFF, #E65100;" +
                    "-fx-border-width: 1.2, 0.8;" +
                    "-fx-border-radius: 8, 7;" +
                    "-fx-padding: 1 5 1 5;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 4, 0, 0, 1);"
                );
                saleBadge.setVisible(false);
                saleBadge.setManaged(false);
                
                final double finalW = spriteWidth;
                saleBadge.widthProperty().addListener((obs, oldW, newW) -> {
                    saleBadge.setLayoutX((finalW / 2.0) - (newW.doubleValue() / 2.0));
                });
                view.getChildren().add(saleBadge);

                // Khởi tạo kích thước ban đầu (con non)
                applySpriteScale();
            }

            view.setTranslateX(x); view.setTranslateY(y);
            view.setManaged(false);
        }

        // --- NÓN RƠM PHONG CÁCH LUFFY CHO THÚ NUÔI ---
        private javafx.scene.Group createLuffyStrawHat(double x, double y, double scale) {
            javafx.scene.Group hat = new javafx.scene.Group();
            
            // Vành nón rộng màu vàng rơm
            Rectangle hatBrim = new Rectangle(20 * scale, 3 * scale, Color.GOLDENROD);
            hatBrim.setX(x - 10 * scale);
            hatBrim.setY(y);
            hatBrim.setArcWidth(2 * scale);
            hatBrim.setArcHeight(2 * scale);

            // Dải ruy băng đỏ hải tặc Luffy
            Rectangle hatBand = new Rectangle(12 * scale, 3 * scale, Color.CRIMSON);
            hatBand.setX(x - 6 * scale);
            hatBand.setY(y - 3 * scale);

            // Chóp nón rơm vàng
            Rectangle hatCrown = new Rectangle(10 * scale, 4 * scale, Color.GOLDENROD);
            hatCrown.setX(x - 5 * scale);
            hatCrown.setY(y - 7 * scale);
            hatCrown.setArcWidth(3 * scale);
            hatCrown.setArcHeight(3 * scale);

            hat.getChildren().addAll(hatBrim, hatBand, hatCrown);
            return hat;
        }

        // --- MODEL BÒ (COW) 4 CHÂN DI CHUYỂN PHONG CÁCH LUFFY ---
        private javafx.scene.Group createCowModel() {
            javafx.scene.Group cow = new javafx.scene.Group();

            // 1. Chân sau - phía xa (Back-Far Leg)
            javafx.scene.Group blGroup = new javafx.scene.Group();
            Rectangle blLeg = new Rectangle(4, 9, Color.rgb(215, 215, 215));
            blLeg.setX(11); blLeg.setY(19);
            Rectangle blHoof = new Rectangle(4, 3, Color.rgb(45, 45, 45));
            blHoof.setX(11); blHoof.setY(28);
            blGroup.getChildren().addAll(blLeg, blHoof);
            legBLSwing = new javafx.scene.transform.Rotate(0, 13, 19);
            blGroup.getTransforms().add(legBLSwing);

            // 2. Chân trước - phía xa (Front-Far Leg)
            javafx.scene.Group flGroup = new javafx.scene.Group();
            Rectangle flLeg = new Rectangle(4, 9, Color.rgb(215, 215, 215));
            flLeg.setX(30); flLeg.setY(19);
            Rectangle flHoof = new Rectangle(4, 3, Color.rgb(45, 45, 45));
            flHoof.setX(30); flHoof.setY(28);
            flGroup.getChildren().addAll(flLeg, flHoof);
            legFLSwing = new javafx.scene.transform.Rotate(0, 32, 19);
            flGroup.getTransforms().add(legFLSwing);

            // 3. Đuôi bò (Tail)
            javafx.scene.Group tailGroup = new javafx.scene.Group();
            Rectangle tailStem = new Rectangle(2, 8, Color.rgb(230, 230, 230));
            tailStem.setX(7); tailStem.setY(12);
            Rectangle tailTuft = new Rectangle(3, 4, Color.rgb(40, 40, 40));
            tailTuft.setX(6.5); tailTuft.setY(20);
            tailGroup.getChildren().addAll(tailStem, tailTuft);
            tailSwing = new javafx.scene.transform.Rotate(0, 8, 12);
            tailGroup.getTransforms().add(tailSwing);

            // 4. Thân bò (Torso & Patches)
            javafx.scene.Group bodyGroup = new javafx.scene.Group();
            Rectangle udder = new Rectangle(6, 3, Color.rgb(255, 192, 203));
            udder.setX(11); udder.setY(23);
            Rectangle body = new Rectangle(30, 18, Color.WHITE);
            body.setX(8); body.setY(8);
            body.setArcWidth(10); body.setArcHeight(10);
            // Đốm đen bò sữa
            Rectangle spot1 = new Rectangle(10, 8, Color.rgb(40, 40, 40));
            spot1.setX(12); spot1.setY(8);
            spot1.setArcWidth(6); spot1.setArcHeight(6);
            Rectangle spot2 = new Rectangle(8, 7, Color.rgb(40, 40, 40));
            spot2.setX(24); spot2.setY(14);
            spot2.setArcWidth(5); spot2.setArcHeight(5);
            bodyGroup.getChildren().addAll(udder, body, spot1, spot2);

            // 5. Đầu bò (Head, Snout, Eyes, Horns, Straw Hat)
            javafx.scene.Group headGroup = new javafx.scene.Group();
            Rectangle head = new Rectangle(14, 15, Color.WHITE);
            head.setX(29); head.setY(6);
            head.setArcWidth(8); head.setArcHeight(8);
            Rectangle headSpot = new Rectangle(6, 7, Color.rgb(40, 40, 40));
            headSpot.setX(30); headSpot.setY(7);
            headSpot.setArcWidth(4); headSpot.setArcHeight(4);
            // Mõm hồng
            Rectangle muzzle = new Rectangle(10, 8, Color.rgb(255, 182, 193));
            muzzle.setX(36); muzzle.setY(13);
            muzzle.setArcWidth(6); muzzle.setArcHeight(6);
            Rectangle nostril1 = new Rectangle(2, 2, Color.rgb(180, 100, 120));
            nostril1.setX(39); nostril1.setY(16);
            Rectangle nostril2 = new Rectangle(2, 2, Color.rgb(180, 100, 120));
            nostril2.setX(43); nostril2.setY(16);
            // Mắt to tròn hoạt hình
            Rectangle eye = new Rectangle(3, 4, Color.rgb(20, 20, 20));
            eye.setX(33); eye.setY(10);
            eye.setArcWidth(2); eye.setArcHeight(2);
            Rectangle pupil = new Rectangle(1, 1, Color.WHITE);
            pupil.setX(34); pupil.setY(11);
            // Sừng nhỏ
            Rectangle horn1 = new Rectangle(3, 4, Color.rgb(215, 190, 150));
            horn1.setX(30); horn1.setY(3);
            Rectangle horn2 = new Rectangle(3, 4, Color.rgb(215, 190, 150));
            horn2.setX(36); horn2.setY(3);
            // Tai
            Rectangle ear = new Rectangle(4, 5, Color.WHITE);
            ear.setX(26); ear.setY(9);
            ear.setArcWidth(3); ear.setArcHeight(3);
            Rectangle earInner = new Rectangle(2, 3, Color.rgb(255, 182, 193));
            earInner.setX(27); earInner.setY(10);
            // Nón rơm Luffy
            javafx.scene.Group hat = createLuffyStrawHat(35, 3, 0.75);

            headGroup.getChildren().addAll(ear, earInner, horn1, horn2, head, headSpot, eye, pupil, muzzle, nostril1, nostril2, hat);

            // 6. Chân sau - phía gần (Back-Near Leg)
            javafx.scene.Group brGroup = new javafx.scene.Group();
            Rectangle brLeg = new Rectangle(4, 9, Color.WHITE);
            brLeg.setX(15); brLeg.setY(20);
            Rectangle brHoof = new Rectangle(4, 3, Color.rgb(45, 45, 45));
            brHoof.setX(15); brHoof.setY(29);
            brGroup.getChildren().addAll(brLeg, brHoof);
            legBRSwing = new javafx.scene.transform.Rotate(0, 17, 20);
            brGroup.getTransforms().add(legBRSwing);

            // 7. Chân trước - phía gần (Front-Near Leg)
            javafx.scene.Group frGroup = new javafx.scene.Group();
            Rectangle frLeg = new Rectangle(4, 9, Color.WHITE);
            frLeg.setX(34); frLeg.setY(20);
            Rectangle frHoof = new Rectangle(4, 3, Color.rgb(45, 45, 45));
            frHoof.setX(34); frHoof.setY(29);
            frGroup.getChildren().addAll(frLeg, frHoof);
            legFRSwing = new javafx.scene.transform.Rotate(0, 36, 20);
            frGroup.getTransforms().add(legFRSwing);

            cow.getChildren().addAll(blGroup, flGroup, tailGroup, bodyGroup, headGroup, brGroup, frGroup);
            return cow;
        }

        // --- MODEL LỢN (PIG) 4 CHÂN DI CHUYỂN PHONG CÁCH LUFFY ---
        private javafx.scene.Group createPigModel() {
            javafx.scene.Group pig = new javafx.scene.Group();

            // 1. Chân sau - phía xa
            javafx.scene.Group blGroup = new javafx.scene.Group();
            Rectangle blLeg = new Rectangle(4, 7, Color.rgb(230, 145, 160));
            blLeg.setX(9); blLeg.setY(16);
            Rectangle blHoof = new Rectangle(4, 2.5, Color.rgb(190, 80, 105));
            blHoof.setX(9); blHoof.setY(23);
            blGroup.getChildren().addAll(blLeg, blHoof);
            legBLSwing = new javafx.scene.transform.Rotate(0, 11, 16);
            blGroup.getTransforms().add(legBLSwing);

            // 2. Chân trước - phía xa
            javafx.scene.Group flGroup = new javafx.scene.Group();
            Rectangle flLeg = new Rectangle(4, 7, Color.rgb(230, 145, 160));
            flLeg.setX(25); flLeg.setY(16);
            Rectangle flHoof = new Rectangle(4, 2.5, Color.rgb(190, 80, 105));
            flHoof.setX(25); flHoof.setY(23);
            flGroup.getChildren().addAll(flLeg, flHoof);
            legFLSwing = new javafx.scene.transform.Rotate(0, 27, 16);
            flGroup.getTransforms().add(legFLSwing);

            // 3. Đuôi xoăn
            javafx.scene.Group tailGroup = new javafx.scene.Group();
            Rectangle tail1 = new Rectangle(4, 2, Color.rgb(255, 150, 170));
            tail1.setX(4); tail1.setY(12);
            Rectangle tail2 = new Rectangle(2, 4, Color.rgb(255, 150, 170));
            tail2.setX(3); tail2.setY(10);
            tailGroup.getChildren().addAll(tail1, tail2);
            tailSwing = new javafx.scene.transform.Rotate(0, 6, 12);
            tailGroup.getTransforms().add(tailSwing);

            // 4. Thân lợn mũm mĩm
            javafx.scene.Group bodyGroup = new javafx.scene.Group();
            Rectangle body = new Rectangle(26, 15, Color.rgb(255, 182, 193));
            body.setX(7); body.setY(7);
            body.setArcWidth(12); body.setArcHeight(12);
            bodyGroup.getChildren().add(body);

            // 5. Đầu lợn & Mũ rơm Luffy
            javafx.scene.Group headGroup = new javafx.scene.Group();
            Rectangle head = new Rectangle(13, 13, Color.rgb(255, 182, 193));
            head.setX(24); head.setY(6);
            head.setArcWidth(10); head.setArcHeight(10);
            // Mũi heo
            Rectangle snout = new Rectangle(8, 7, Color.rgb(255, 130, 160));
            snout.setX(32); snout.setY(11);
            snout.setArcWidth(6); snout.setArcHeight(6);
            Rectangle nostril1 = new Rectangle(2, 2, Color.rgb(180, 50, 80));
            nostril1.setX(34); nostril1.setY(13);
            Rectangle nostril2 = new Rectangle(2, 2, Color.rgb(180, 50, 80));
            nostril2.setX(37); nostril2.setY(13);
            // Mắt to đen lấp lánh
            Rectangle eye = new Rectangle(3, 3.5, Color.rgb(20, 20, 20));
            eye.setX(28); eye.setY(9);
            eye.setArcWidth(2); eye.setArcHeight(2);
            Rectangle pupil = new Rectangle(1, 1, Color.WHITE);
            pupil.setX(29); pupil.setY(10);
            // Má hồng
            Rectangle blush = new Rectangle(4, 3, Color.rgb(255, 105, 180, 0.45));
            blush.setX(27); blush.setY(13);
            blush.setArcWidth(2); blush.setArcHeight(2);
            // Tai nhọn
            Rectangle ear1 = new Rectangle(4, 5, Color.rgb(255, 140, 165));
            ear1.setX(23); ear1.setY(3);
            ear1.setArcWidth(3); ear1.setArcHeight(3);
            Rectangle ear2 = new Rectangle(4, 5, Color.rgb(255, 140, 165));
            ear2.setX(29); ear2.setY(3);
            ear2.setArcWidth(3); ear2.setArcHeight(3);
            // Nón rơm Luffy
            javafx.scene.Group hat = createLuffyStrawHat(28, 3, 0.7);

            headGroup.getChildren().addAll(ear1, ear2, head, eye, pupil, blush, snout, nostril1, nostril2, hat);

            // 6. Chân sau - phía gần
            javafx.scene.Group brGroup = new javafx.scene.Group();
            Rectangle brLeg = new Rectangle(4, 7, Color.rgb(255, 182, 193));
            brLeg.setX(13); brLeg.setY(17);
            Rectangle brHoof = new Rectangle(4, 2.5, Color.rgb(210, 90, 120));
            brHoof.setX(13); brHoof.setY(24);
            brGroup.getChildren().addAll(brLeg, brHoof);
            legBRSwing = new javafx.scene.transform.Rotate(0, 15, 17);
            brGroup.getTransforms().add(legBRSwing);

            // 7. Chân trước - phía gần
            javafx.scene.Group frGroup = new javafx.scene.Group();
            Rectangle frLeg = new Rectangle(4, 7, Color.rgb(255, 182, 193));
            frLeg.setX(29); frLeg.setY(17);
            Rectangle frHoof = new Rectangle(4, 2.5, Color.rgb(210, 90, 120));
            frHoof.setX(29); frHoof.setY(24);
            frGroup.getChildren().addAll(frLeg, frHoof);
            legFRSwing = new javafx.scene.transform.Rotate(0, 31, 17);
            frGroup.getTransforms().add(legFRSwing);

            pig.getChildren().addAll(blGroup, flGroup, tailGroup, bodyGroup, headGroup, brGroup, frGroup);
            return pig;
        }

        // --- MODEL GÀ (CHICKEN) 2 CHÂN DI CHUYỂN PHONG CÁCH LUFFY ---
        private javafx.scene.Group createChickenModel() {
            javafx.scene.Group chicken = new javafx.scene.Group();

            // 1. Chân trái (Far Leg) - 2 chân di chuyển
            javafx.scene.Group lLegGroup = new javafx.scene.Group();
            Rectangle lStem = new Rectangle(2.5, 7, Color.rgb(220, 120, 20));
            lStem.setX(10); lStem.setY(18);
            Rectangle lFoot = new Rectangle(5, 2, Color.rgb(220, 120, 20));
            lFoot.setX(10); lFoot.setY(25);
            lLegGroup.getChildren().addAll(lStem, lFoot);
            legLSwing = new javafx.scene.transform.Rotate(0, 11, 18);
            lLegGroup.getTransforms().add(legLSwing);

            // 2. Lông đuôi gà
            javafx.scene.Group tailGroup = new javafx.scene.Group();
            Rectangle feather1 = new Rectangle(4, 7, Color.rgb(235, 235, 235));
            feather1.setX(3); feather1.setY(7);
            feather1.setArcWidth(3); feather1.setArcHeight(3);
            Rectangle feather2 = new Rectangle(4, 9, Color.WHITE);
            feather2.setX(5); feather2.setY(5);
            feather2.setArcWidth(3); feather2.setArcHeight(3);
            tailGroup.getChildren().addAll(feather1, feather2);

            // 3. Thân gà tròn
            javafx.scene.Group bodyGroup = new javafx.scene.Group();
            Rectangle body = new Rectangle(16, 13, Color.WHITE);
            body.setX(6); body.setY(9);
            body.setArcWidth(12); body.setArcHeight(12);
            bodyGroup.getChildren().add(body);

            // 4. Cánh gà
            javafx.scene.Group wingGroup = new javafx.scene.Group();
            Rectangle wing = new Rectangle(9, 8, Color.rgb(245, 245, 245));
            wing.setX(8); wing.setY(11);
            wing.setArcWidth(6); wing.setArcHeight(6);
            wing.setStroke(Color.rgb(220, 220, 220));
            wing.setStrokeWidth(0.8);
            wingGroup.getChildren().add(wing);
            wingSwing = new javafx.scene.transform.Rotate(0, 8, 11);
            wingGroup.getTransforms().add(wingSwing);

            // 5. Đầu gà, Mào đỏ, Mỏ vàng, Yếm đỏ & Mũ rơm Luffy
            javafx.scene.Group headGroup = new javafx.scene.Group();
            Rectangle head = new Rectangle(11, 11, Color.WHITE);
            head.setX(15); head.setY(5);
            head.setArcWidth(8); head.setArcHeight(8);
            // Mào gà đỏ
            Rectangle comb1 = new Rectangle(4, 5, Color.CRIMSON);
            comb1.setX(16); comb1.setY(1);
            comb1.setArcWidth(3); comb1.setArcHeight(3);
            Rectangle comb2 = new Rectangle(4, 6, Color.CRIMSON);
            comb2.setX(19); comb2.setY(0);
            comb2.setArcWidth(3); comb2.setArcHeight(3);
            // Mỏ vàng cam
            Rectangle beak = new Rectangle(5, 4, Color.rgb(255, 140, 0));
            beak.setX(23); beak.setY(8);
            beak.setArcWidth(2); beak.setArcHeight(2);
            // Yếm gà đỏ dưới mỏ
            Rectangle wattle = new Rectangle(3, 4, Color.CRIMSON);
            wattle.setX(22); wattle.setY(12);
            wattle.setArcWidth(2); wattle.setArcHeight(2);
            // Mắt gà
            Rectangle eye = new Rectangle(2.5, 3, Color.rgb(20, 20, 20));
            eye.setX(19); eye.setY(7);
            eye.setArcWidth(2); eye.setArcHeight(2);
            Rectangle pupil = new Rectangle(1, 1, Color.WHITE);
            pupil.setX(20); pupil.setY(7.5);
            // Nón rơm Luffy
            javafx.scene.Group hat = createLuffyStrawHat(18, 2, 0.6);

            headGroup.getChildren().addAll(comb1, comb2, head, eye, pupil, beak, wattle, hat);

            // 6. Chân phải (Near Leg) - 2 chân di chuyển
            javafx.scene.Group rLegGroup = new javafx.scene.Group();
            Rectangle rStem = new Rectangle(2.5, 7, Color.rgb(255, 150, 0));
            rStem.setX(15); rStem.setY(19);
            Rectangle rFoot = new Rectangle(6, 2, Color.rgb(255, 150, 0));
            rFoot.setX(15); rFoot.setY(26);
            rLegGroup.getChildren().addAll(rStem, rFoot);
            legRSwing = new javafx.scene.transform.Rotate(0, 16, 19);
            rLegGroup.getTransforms().add(legRSwing);

            chicken.getChildren().addAll(lLegGroup, tailGroup, bodyGroup, wingGroup, headGroup, rLegGroup);
            return chicken;
        }

        public double getGrowthScale() {
            // Ngày 0 (con non): 0.65
            // Mỗi ngày ăn no tăng +0.15 scale
            // Ngày 5 trở đi: 1.40 (trưởng thành tối đa)
            return 0.65 + (Math.min(ageDays, 5) * 0.15);
        }

        public int getSellPrice() {
            if (type == 0) return 150; // Gà
            if (type == 1) return 500; // Bò
            if (type == 3) return 300; // Lợn
            return 0;
        }

        public String getAnimalName() {
            if (type == 0) return "Gà";
            if (type == 1) return "Bò";
            if (type == 3) return "Lợn";
            return "Thú nuôi";
        }

        public void applySpriteScale() {
            if (type == 2 || spriteNode == null) return;
            double s = getGrowthScale();
            double spriteHeight = (type == 0 ? 30 : (type == 1 ? 34 : 28));
            
            // Giữ chân thú bám đất bằng cách dịch chuyển bù theo chiều cao scale
            spriteNode.setTranslateY(- (spriteHeight / 2.0) * (s - 1.0));
            
            // Tất cả model procedural đều mặc định quay mặt sang phải, nên lật sang trái bằng -s
            spriteNode.setScaleX(facingLeft ? -s : s);
            spriteNode.setScaleY(s);
            updateBadgePosition();
        }

        public void updateFacing(boolean movingLeft) {
            if (this.facingLeft != movingLeft) {
                this.facingLeft = movingLeft;
                applySpriteScale();
            }
        }

        public void updateBadgePosition() {
            if (type == 2) return;
            double s = getGrowthScale();
            double spriteHeight = (type == 0 ? 30 : (type == 1 ? 34 : 28));
            badgeBaseY = - spriteHeight * (s - 1.0) - 16;
            if (saleBadge != null) {
                saleBadge.setLayoutY(badgeBaseY);
                saleBadge.setVisible(ageDays >= 5);
            }
            if (heartEmoji != null) {
                heartEmoji.setY(badgeBaseY + (ageDays >= 5 ? -14 : 0));
            }
        }

        public void feed() {
            if (!isFed) {
                isFed = true;
                if (heartEmoji != null) heartEmoji.setVisible(true);
            }
        }

        public void update() {
            if (type == 2) { // Vịt bay chéo liên tục
                x -= speed;
                y -= speed * 0.3;
                if (x < -100) {
                    x = farmMap.getWidth() * Constant.CELL_SIZE + 100;
                    y = Math.random() * (farmMap.getHeight() * Constant.CELL_SIZE);
                }
                double bobbing = Math.sin(System.currentTimeMillis() * 0.01) * 10;
                view.setTranslateX(x);
                view.setTranslateY(y + bobbing);
                view.setUserData(y + 20.0);
                return;
            }

            long now = System.currentTimeMillis();

            // Hiệu ứng bảng hiệu 💰 BÁN lơ lửng bồng bềnh
            if (saleBadge != null && saleBadge.isVisible()) {
                double floatOffset = Math.sin((now + bobSeed) * 0.005) * 3.0;
                saleBadge.setLayoutY(badgeBaseY + floatOffset);
            }

            boolean isMoving = false;

            if (eatTrough != null) {
                if (now > eatUntil || !needsFeeding(this)) {
                    eatTrough = null;
                } else {
                    pauseUntil = 0;
                    targetX = eatTrough.spotX();
                    targetY = eatTrough.spotY();
                    updateFacing(targetX < x);
                    if (Math.hypot(targetX - x, targetY - y) < 10) {
                        if (eatTrough.take()) {
                            feed();
                        }
                        eatTrough = null;
                        pauseUntil = now + 2500;
                    }
                }
            }

            // Luôn đảm bảo vật nuôi nằm trong đúng phân khu của loài (CHICKEN / PIG / COW)
            x = clampToAnimalZoneX(x, type);
            y = clampToAnimalZoneY(y, type);

            if (now >= pauseUntil) {
                double dx = targetX - x;
                double dy = targetY - y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 5) {
                    targetX = clampToAnimalZoneX(x + (Math.random() - 0.5) * 180, type);
                    targetY = clampToAnimalZoneY(y + (Math.random() - 0.5) * 180, type);

                    if (isAnimalBlockedAt(targetX, targetY, type)) {
                        targetX = x;
                        targetY = y;
                    } else {
                        if (Math.random() < 0.6) {
                            pauseUntil = now + 1500 + (long)(Math.random() * 3000);
                        }
                        updateFacing(targetX < x);
                    }
                } else {
                    double nextX = clampToAnimalZoneX(x + (dx / dist) * speed, type);
                    double nextY = clampToAnimalZoneY(y + (dy / dist) * speed, type);
                    if (isAnimalBlockedAt(nextX, nextY, type)) {
                        // Đổi hướng ngẫu nhiên mới bên trong phân khu loài nếu gặp vật cản/rào
                        targetX = clampToAnimalZoneX(x + (Math.random() - 0.5) * 160, type);
                        targetY = clampToAnimalZoneY(y + (Math.random() - 0.5) * 160, type);
                    } else {
                        x = nextX;
                        y = nextY;
                        isMoving = true;
                        updateFacing(dx < 0);
                    }
                }
            }

            // Chuyển động bước đi (Wobble & Limb swings giống Luffy)
            double bobbing = 0;
            if (isMoving) {
                walkStep += 0.22 * (speed / 0.7);
                bobbing = Math.sin(walkStep * 2.0) * (type == 0 ? 2.5 : 1.5);
                double swingAngle = Math.sin(walkStep) * 26.0;

                if (type == 1 || type == 3) { // Bò và Lợn (4 chân di chuyển dáng đi tự nhiên)
                    if (legFLSwing != null) {
                        legFLSwing.setAngle(swingAngle);
                        legBRSwing.setAngle(swingAngle);
                        legFRSwing.setAngle(-swingAngle);
                        legBLSwing.setAngle(-swingAngle);
                    }
                    if (tailSwing != null) {
                        tailSwing.setAngle(Math.sin(walkStep * 2.0) * 15.0);
                    }
                } else if (type == 0) { // Gà (2 chân di chuyển so le)
                    if (legLSwing != null) {
                        legLSwing.setAngle(swingAngle);
                        legRSwing.setAngle(-swingAngle);
                    }
                    if (wingSwing != null) {
                        wingSwing.setAngle(Math.sin(walkStep * 2.0) * 8.0);
                    }
                }
            } else {
                // Đứng nghỉ: Chân thẳng đứng, nhấp nhô thở nhẹ
                bobbing = Math.sin(now * 0.003 + bobSeed) * 0.8;
                if (type == 1 || type == 3) {
                    if (legFLSwing != null) {
                        legFLSwing.setAngle(0);
                        legBRSwing.setAngle(0);
                        legFRSwing.setAngle(0);
                        legBLSwing.setAngle(0);
                    }
                    if (tailSwing != null) {
                        tailSwing.setAngle(0);
                    }
                } else if (type == 0) {
                    if (legLSwing != null) {
                        legLSwing.setAngle(0);
                        legRSwing.setAngle(0);
                    }
                    if (wingSwing != null) {
                        wingSwing.setAngle(0);
                    }
                }
            }

            view.setTranslateX(x);
            view.setTranslateY(y + bobbing);
            view.setUserData(y + (type == 0 ? 30.0 : (type == 1 ? 34.0 : 28.0)));
        }
    }
}
