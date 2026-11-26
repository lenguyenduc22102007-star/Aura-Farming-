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

    // injected domain
    private FarmMap farmMap;
    private Player player;
    private GameManager gameManager;
    private PlayerController playerController;
    private DemoController demoController;

    private int helperDaysLeft = 0;
    
    // tool strategy instances
    private Hoe hoe;
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
    private view.ui.CampfireCookingOverlay activeCookingOverlay = null;
    private view.ui.CasinoOverlay activeCasinoOverlay = null;
    private view.npc.NpcManager npcManager = null;
    private javafx.scene.layout.StackPane activeSleepDialog = null;
    private boolean sleepTransitionRunning = false;
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
    private double campfireEmberTimer = 0.0;
    private Label interactPromptLabel = null;
    private Label survivalWarningLabel = null;
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
    private ImageView bridgeCanopyView = null;
    private final javafx.scene.effect.ColorAdjust underBridgePlayerShade = new javafx.scene.effect.ColorAdjust();
    private double lastPlayerBridgeShade = 0.0;
    private long lastFishLaborMs = 0L;
    private static final double HUNGER_FULL_GRACE_SEC = 135.0 * 4.0 / 16.0; // ~33.75s thực = đúng 4 tiếng trong game mới bắt đầu đói
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
            currentTimeTint = getTimeOverlayColor(0.0);
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
        waterTool.setFocusTraversable(false);
        seedTool.setFocusTraversable(false);
        handTool.setFocusTraversable(false);
        fertilizerTool.setFocusTraversable(false);
        if (bagTool != null) bagTool.setFocusTraversable(false);
        sunWeatherButton.setFocusTraversable(false);
        rainWeatherButton.setFocusTraversable(false);
        dryWeatherButton.setFocusTraversable(false);
        if (fishingGuideButton != null) fishingGuideButton.setFocusTraversable(false);

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
                case DIGIT3:  selectTool(wateringCan, waterTool);  break;
                case DIGIT4:  selectTool(hand, handTool);          break;
                case DIGIT5:  selectTool(fertilizerBag, fertilizerTool); break;
                case DIGIT6:  openFeedBagPicker();                 break;
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
                    toggleFishingGuide();
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
        
        // Khóa cuốc đất & tưới nước khi kiệt sức (Đói <= 0 hoặc Khát <= 0)
        ToolStrategy curToolCheck = playerController.getCurrentTool();
        if (player != null && player.isExhausted() && (curToolCheck instanceof Hoe || curToolCheck instanceof WateringCan)) {
            spawnFloatingText("Quá đói/khát! Hãy ăn uống trước!", Color.rgb(255, 86, 86));
            notify("Quá đói/khát! Hãy ăn uống trước!");
            rootPane.requestFocus();
            return;
        }

        // Cuốc (Hoe) CHỈ hoạt động trên các luống đất 2x2 (TerrainType.DIRT) trong Farming Zone — giữ nguyên lối đi cỏ xanh!
        if (curToolCheck instanceof Hoe) {
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

        // 1. Thu hoạch cây trồng bằng Tay: spawn hiệu ứng lá/tia sáng & vật phẩm bật nảy tự hút
        if (curTool instanceof Hand && cell.getPest() == null && cell.getCrop() != null && cell.getCrop().isHarvestable()) {
            model.crop.Crop harvested = cell.harvestCrop();
            if (harvested != null) {
                model.game_item.HarvestedCropItem item = new model.game_item.HarvestedCropItem(
                    harvested.getName(), harvested.getCropData().getValue()
                );
                if (particleSystem != null) {
                    particleSystem.spawnHarvestSparkles(wx, wy);
                }
                view.fx.SoundHook.playHarvest();
                if (droppedItemSystem != null) {
                    droppedItemSystem.spawnDrop(item, Assets.getShopIcon(item), wx, wy, 1);
                } else {
                    player.getInventory().addItem(item, 1);
                }
                onQuestCropHarvested(harvested.getName());
                return true;
            }
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
        toolActionKind = kind;
        toolActionTimer = 0.0;
        toolImpactFired = (targetWx < 0); // Không bắn hạt nếu không có ô mục tiêu hợp lệ
        toolTargetWorldX = targetWx;
        toolTargetWorldY = targetWy;
        if ("HOE".equals(kind)) {
            toolActionDuration = 0.32;      // 4 frame cuốc đất (0.08s/frame): 0 -> 1 -> 2 -> 3 -> 0
        } else if ("WATER".equals(kind)) {
            toolActionDuration = 0.40;      // 4 bước tưới nước (0.10s/frame): 0 -> 1 -> 2 -> 1 -> 0
        } else {
            toolActionDuration = 0.28;      // Lắc qua lại (-18° <-> +18°) rắc hạt / rải phân
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

    public void setAutosave(Runnable autosave) {
        this.autosave = autosave;
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
        p.setProperty("view.helperDays", Integer.toString(helperDaysLeft));
        p.setProperty("view.foodRareFish", Boolean.toString(foodRareFishBuff));
        p.setProperty("view.poolRelaxSec", Double.toString(poolRelaxSpeedBuffSec));
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
    }

    /** Restore the state written by {@link #exportViewState}. Call after setGame. */
    public void importViewState(java.util.Properties p) {
        suppressQuestMoneyTracking = true;
        try {
            playerX = Double.parseDouble(p.getProperty("view.playerX", Double.toString(playerX)));
            playerY = Double.parseDouble(p.getProperty("view.playerY", Double.toString(playerY)));
            facing = p.getProperty("view.facing", facing);
            elapsedTimeInDay = Long.parseLong(p.getProperty("view.elapsed", "0"));
            helperDaysLeft = Integer.parseInt(p.getProperty("view.helperDays", "0"));
            foodRareFishBuff = Boolean.parseBoolean(p.getProperty("view.foodRareFish", "false"));
            poolRelaxSpeedBuffSec = Double.parseDouble(p.getProperty("view.poolRelaxSec", "0.0"));
            if (player != null && player.getQuestManager() != null && p.getProperty("quest.currentQuestIndex") != null) {
                int qIdx = Integer.parseInt(p.getProperty("quest.currentQuestIndex", "1"));
                int qProg = Integer.parseInt(p.getProperty("quest.currentQuestProgress", "0"));
                player.getQuestManager().restoreState(qIdx, qProg);
            }
            if (fishShadows != null) fishShadows.setRareFishBuff(foodRareFishBuff);
            updateFoodBuffHud();
            updateSurvivalHud(animTimeSec);
            updateQuestTrackerUi();
            lastFrameTime = System.currentTimeMillis();
            resetFeedingSchedule(elapsedTimeInDay / 135000.0);
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
                onQuestAnimalHarvested();
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
                    model.game_item.AnimalProductItem prod = model.game_item.AnimalProductItem.forAnimalType(toFeed.type);
                    if (droppedItemSystem != null) {
                        droppedItemSystem.spawnDrop(prod, Assets.getShopIcon(prod), toFeed.x + 18, toFeed.y + 12, 1);
                    } else {
                        player.getInventory().addItem(prod, 1);
                    }
                    onQuestAnimalHarvested();
                    notify("♥ Đã vuốt ve " + aName + " và thu được " + prod.getName() + "!");
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
    private long elapsedTimeInDay = 0;

    private Color getTimeOverlayColor(double progress) {
        Color nightColor = Color.rgb(10, 10, 30, 0.6); // slightly less dark for playability
        Color sunriseColor = Color.rgb(255, 120, 50, 0.3);
        Color morningClear = Color.rgb(255, 120, 50, 0.0);
        Color afternoonClear = Color.rgb(255, 80, 20, 0.0);
        Color sunsetColor = Color.rgb(255, 80, 20, 0.35);

        if (progress < 0.1) {
            // 0% - 10%: Dawn (Night -> Sunrise)
            return nightColor.interpolate(sunriseColor, progress / 0.1);
        } else if (progress < 0.25) {
            // 10% - 25%: Morning (Sunrise -> Clear)
            return sunriseColor.interpolate(morningClear, (progress - 0.1) / 0.15);
        } else if (progress < 0.55) {
            // 25% - 55%: Midday (Clear)
            return Color.TRANSPARENT;
        } else if (progress < 0.7) {
            // 55% - 70%: Afternoon (Clear -> Sunset)
            return afternoonClear.interpolate(sunsetColor, (progress - 0.55) / 0.15);
        } else if (progress < 0.85) {
            // 70% - 85%: Dusk (Sunset -> Night)
            return sunsetColor.interpolate(nightColor, (progress - 0.7) / 0.15);
        } else {
            // 85% - 100%: Night
            return nightColor;
        }
    }

    private void startGameLoop() {
        lastFrameTime = System.currentTimeMillis();
        lastAnimNanos = 0L;
        elapsedTimeInDay = 0;
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
                    if (!router.hasOverlay() && !sleepTransitionRunning) {
                        updatePlayerPosition(dtSec, animTimeSec);
                        updateRainEffect(dtSec);

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
                        
                        // Tự động qua ngày sau mỗi 135 giây (135000 ms)
                        elapsedTimeInDay += delta;
                        if (elapsedTimeInDay >= 135000) {
                            onNextDay();
                        } else {
                            // Nội suy mượt màu sáng/trưa/chiều/tối mỗi frame
                            if (timeOverlayRect != null) {
                                double progress = elapsedTimeInDay / 135000.0;
                                Color targetTime = getTimeOverlayColor(progress);
                                currentTimeTint = currentTimeTint.interpolate(targetTime, Math.min(1.0, dtSec * 5.0));
                                timeOverlayRect.setFill(currentTimeTint);
                            }
                            updateTimeDisplay();
                            updateFeedingSchedule();
                        }
                        updateFlagsAnimation();
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
        double progress = Math.min(1.0, Math.max(0.0, elapsedTimeInDay / 135000.0));
        // Giả lập từ 06:00 đến 22:00 (16 tiếng một ngày)
        int totalMins = (int) (progress * 16 * 60);
        int hour = 6 + totalMins / 60;
        int min = (totalMins % 60) / 10 * 10;
        int currentMinuteHash = hour * 60 + min;
        if (currentMinuteHash != lastDisplayedMinute) {
            lastDisplayedMinute = currentMinuteHash;
            String timeStr = String.format("%02d:%02d", hour, min);
            dayField.setText("DAY " + gameManager.getCurrentDay() + "   " + timeStr);
        }
    }

    private void updateFlagsAnimation() {
        if (animatedFlags == null || animatedFlags.isEmpty()) return;
        int frameCount = 6;
        int frameIndex = (int) ((System.currentTimeMillis() / 150) % frameCount);
        for (ImageView flag : animatedFlags) {
            javafx.scene.image.Image img = flag.getImage();
            if (img != null) {
                double frameWidth = img.getWidth() / frameCount;
                double frameHeight = img.getHeight();
                javafx.geometry.Rectangle2D viewport = new javafx.geometry.Rectangle2D(frameIndex * frameWidth, 0, frameWidth, frameHeight);
                flag.setViewport(viewport);
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
            // Đống lửa trại sinh động: ánh sáng cam nhấp nháy & tàn lửa/khói pixel bay lên
            if (!campfirePoints.isEmpty()) {
                campfireEmberTimer += deltaRiverSec;
                if (campfireEmberTimer >= 0.16) {
                    campfireEmberTimer = 0.0;
                    for (double[] cp : campfirePoints) {
                        particleSystem.spawnCampfireEmber(cp[0], cp[1]);
                    }
                }
                for (int i = 0; i < campfireGlows.size(); i++) {
                    javafx.scene.shape.Circle glow = campfireGlows.get(i);
                    double pulse = 0.88 + 0.14 * Math.sin(timeSec * 6.8 + i * 1.3);
                    glow.setScaleX(pulse);
                    glow.setScaleY(pulse * 0.82);
                    glow.setOpacity(0.72 + 0.18 * Math.sin(timeSec * 9.2 + i));
                }
            }
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
                player.addHunger(6.5 * deltaRiverSec);
                player.addThirst(8.0 * deltaRiverSec);
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
                if (recentLaborDecaySec > 0.0) {
                    recentLaborDecaySec = Math.max(0.0, recentLaborDecaySec - deltaRiverSec);
                    if (recentLaborDecaySec <= 0.0) {
                        recentLaborStreak = 0;
                    }
                }
                if (hungerGraceTimerSec > 0.0) {
                    hungerGraceTimerSec = Math.max(0.0, hungerGraceTimerSec - deltaRiverSec);
                } else {
                    player.addHunger(-0.12 * deltaRiverSec);
                }
                WeatherType wType = (gameManager != null && gameManager.getCurrentWeather() != null)
                        ? gameManager.getCurrentWeather().getType()
                        : WeatherType.SUNNY;
                double thirstDrainRate = (wType == WeatherType.DROUGHT) ? 0.18 : (wType == WeatherType.RAINY ? 0.04 : 0.10);
                player.addThirst(-thirstDrainRate * deltaRiverSec);
                updateSurvivalHud(timeSec);
            }
        }

        if (!lockedOnBoat && !isSunbathing) {
            if (dirX != 0 || dirY != 0) {
                isWalking = true;
                // Chuẩn hóa tốc độ chạy chéo (8 hướng mượt mà) + Phạt kiệt sức (55%) + Buff Thư giãn bể bơi (+20%)
                double norm = (dirX != 0 && dirY != 0) ? 0.70710678 : 1.0;
                double exhaustionFactor = (player != null && player.isExhausted()) ? 0.55 : 1.0;
                double relaxFactor = (poolRelaxSpeedBuffSec > 0.0) ? 1.20 : 1.0;
                double effectiveSpeed = speed * exhaustionFactor * relaxFactor;
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

            // Kiểm tra địa hình nước theo đúng vị trí bàn chân nhân vật (y + 54)
            int colX = (int) ((nextX + 22) / Constant.CELL_SIZE);
            int rowX = (int) ((playerY + 54) / Constant.CELL_SIZE);
            boolean waterX = farmMap.getCell(rowX, colX) != null && farmMap.getCell(rowX, colX).getTerrain() == TerrainType.WATER;

            int colY = (int) ((playerX + 22) / Constant.CELL_SIZE);
            int rowY = (int) ((nextY + 54) / Constant.CELL_SIZE);
            boolean waterY = farmMap.getCell(rowY, colY) != null && farmMap.getCell(rowY, colY).getTerrain() == TerrainType.WATER;

            // TMX Object Collision — khớp chính xác vùng chân nhân vật (x+12..32, y+42..64)
            javafx.geometry.Rectangle2D oldBox = new javafx.geometry.Rectangle2D(playerX + 12, playerY + 42, 20, 22);
            javafx.geometry.Rectangle2D playerBoxX = new javafx.geometry.Rectangle2D(nextX + 12, playerY + 42, 20, 22);
            javafx.geometry.Rectangle2D playerBoxY = new javafx.geometry.Rectangle2D(playerX + 12, nextY + 42, 20, 22);

            boolean colX_obj = false;
            boolean colY_obj = false;
            boolean onBridgeX = false;
            boolean onBridgeY = false;
            boolean onDockX = (boatEntity != null && boatEntity.isOnWalkableDock(playerBoxX));
            boolean onDockY = (boatEntity != null && boatEntity.isOnWalkableDock(playerBoxY));

            if (boatEntity != null) {
                if (boatEntity.collidesWithHull(playerBoxX) && !boatEntity.collidesWithHull(oldBox)) colX_obj = true;
                if (boatEntity.collidesWithHull(playerBoxY) && !boatEntity.collidesWithHull(oldBox)) colY_obj = true;
            }

            for (ImageView objView : sceneryObjects) {
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
                    // Chi tiết hóa Collision THÀNH CẦU khớp từng pixel của bridge.png (200x190):
                    // - Hai mép ngoài (0..16% và 85.7..100%) là mặt nước/bờ ngoài cầu, KHÔNG đi xuyên qua thành gỗ
                    // - Thành cầu trái (16%..24.2%) & thành cầu phải (77.5%..85.7%) + 6 trụ gỗ đầu/giữa/cuối cầu
                    // - 2 lan can cánh sát bờ dưới (y=83%..100%) hai bên đầu cầu nam
                    // - Chỉ phần mặt ván gỗ bên trong (24.4%..77.2%) mới cho phép đi băng qua sông
                    double leftRailX  = ox + ow * 0.160;
                    double leftRailW  = ow * 0.082;
                    double rightRailX = ox + ow * 0.775;
                    double rightRailW = ow * 0.082;

                    javafx.geometry.Rectangle2D[] bridgeBlockers = new javafx.geometry.Rectangle2D[] {
                        // 1. Dầm lan can trái & phải chạy dọc toàn thân cầu
                        new javafx.geometry.Rectangle2D(leftRailX,  oy + oh * 0.02, leftRailW,  oh * 0.96),
                        new javafx.geometry.Rectangle2D(rightRailX, oy + oh * 0.02, rightRailW, oh * 0.96),
                        // 2. Ba trụ gỗ nhô rộng trên thành cầu trái (đầu cầu bắc, giữa cầu, đầu cầu nam)
                        new javafx.geometry.Rectangle2D(ox + ow * 0.150, oy,             ow * 0.100, oh * 0.13),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.150, oy + oh * 0.45, ow * 0.100, oh * 0.17),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.150, oy + oh * 0.80, ow * 0.100, oh * 0.20),
                        // 3. Ba trụ gỗ nhô rộng trên thành cầu phải (đầu cầu bắc, giữa cầu, đầu cầu nam)
                        new javafx.geometry.Rectangle2D(ox + ow * 0.768, oy,             ow * 0.100, oh * 0.13),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.768, oy + oh * 0.45, ow * 0.100, oh * 0.17),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.768, oy + oh * 0.80, ow * 0.100, oh * 0.20),
                        // 4. Hai thanh lan can cánh nối ngang dọc bờ nam hai bên chân cầu
                        new javafx.geometry.Rectangle2D(ox,              oy + oh * 0.83, ow * 0.165, oh * 0.17),
                        new javafx.geometry.Rectangle2D(ox + ow * 0.855, oy + oh * 0.83, ow * 0.145, oh * 0.17)
                    };

                    // Sàn ván gỗ đi được (chỉ nằm lọt lòng giữa 2 thành cầu, mở rộng 28px hai đầu bắc/nam để bước lên mượt)
                    double deckLeft = ox + ow * 0.244;
                    double deckWidth = ow * 0.528;
                    javafx.geometry.Rectangle2D walkableDeck = new javafx.geometry.Rectangle2D(deckLeft, oy - 28, deckWidth, oh + 56);

                    if (playerBoxX.intersects(walkableDeck)) onBridgeX = true;
                    if (playerBoxY.intersects(walkableDeck)) onBridgeY = true;

                    for (javafx.geometry.Rectangle2D blocker : bridgeBlockers) {
                        if (playerBoxX.intersects(blocker) && !oldBox.intersects(blocker)) colX_obj = true;
                        if (playerBoxY.intersects(blocker) && !oldBox.intersects(blocker)) colY_obj = true;
                    }
                    continue;
                }
                if (idLower.isEmpty() || idLower.contains("flag") || idLower.contains("crop") || idLower.contains("bush") ||
                    idLower.contains("pumpkin") || idLower.contains("dirt") || idLower.contains("path") ||
                    idLower.contains("terrain") || idLower.contains("wheat") || idLower.contains("construction") ||
                    idLower.contains("farm_rake") || idLower.contains("farm_hoe") || idLower.contains("farm_shovel") ||
                    idLower.contains("flower") || idLower.contains("pebble") || idLower.contains("broken_") ||
                    idLower.contains("wood_sticks") || idLower.contains("clay_pot") || idLower.contains("grass_patch") ||
                    idLower.contains("windmill_blades") || idLower.contains("pool_lounger") || idLower.contains("pool_umbrella")) continue;
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
            } else if (idLow.contains("house") || idLow.contains("barn") || idLow.contains("pet_shop") || idLow.contains("windmill") || idLow.contains("casino")) {
                // Công trình lớn (Nhà, Nhà kho, Pet Shop, Cối xay gió): chặn toàn bộ phần thân & tường nhà
                hw = ow * 0.88;
                hh = oh * 0.62;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("tent")) {
                // Lều trại: chặn toàn bộ phần thân lều
                hw = ow * 0.85;
                hh = oh * 0.72;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("campfire")) {
                // Lửa trại: chặn vòng đá lửa trại
                hw = ow * 0.80;
                hh = oh * 0.68;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("fence_v") || idLow.contains("wire_fence_v")) {
                // Hàng rào dọc: chặn dọc toàn bộ chiều dài hàng rào
                hw = Math.max(14, ow * 0.85);
                hh = oh * 0.92;
                cx = ox + (ow - hw) / 2;
                cy = oy + (oh - hh) / 2;
            } else if (idLow.contains("fence")) {
                // Hàng rào ngang / xiên / rào dây ngang: chặn hết bề ngang
                hw = ow * 0.95;
                hh = Math.max(16, oh * 0.75);
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh;
            } else if (idLow.contains("garden_bed")) {
                // Luống gỗ trồng bí ngô: chặn toàn bộ khung gỗ
                hw = ow * 0.92;
                hh = oh * 0.88;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh;
            } else if (idLow.contains("tree")) {
                // Cây sồi: chặn phần thân & rễ cây (tán cây phía trên vẫn cho hiệu ứng 2.5D đi sau tán)
                hw = ow * 0.42;
                hh = oh * 0.28;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("rock") || idLow.contains("stump") || idLow.contains("crate") ||
                       idLow.contains("well") || idLow.contains("water_trough") || idLow.contains("wood_plank") ||
                       idLow.contains("log")) {
                // Đá, gốc cây, thùng gỗ, giếng, máng nước, khúc gỗ: chặn chắc chắn phần thân
                hw = ow * 0.82;
                hh = oh * 0.68;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("npc")) {
                // NPC (Zoro): chặn phần chân nhân vật, vẫn đứng sát đối thoại được
                hw = ow * 0.55;
                hh = oh * 0.42;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (idLow.contains("streetlamp") || idLow.contains("lantern") || idLow.contains("signpost")) {
                // Cột đèn, biển chỉ dẫn: chặn phần chân cột
                hw = Math.max(14, ow * 0.45);
                hh = oh * 0.28;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else {
                // Vật thể khác
                hw = Math.min(40, ow * 0.65);
                hh = Math.min(30, oh * 0.45);
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            }
            
            javafx.geometry.Rectangle2D objBox = new javafx.geometry.Rectangle2D(cx, cy, hw, hh);
            
            if (playerBoxX.intersects(objBox) && !oldBox.intersects(objBox)) colX_obj = true;
            if (playerBoxY.intersects(objBox) && !oldBox.intersects(objBox)) colY_obj = true;
            
            if (colX_obj && colY_obj) break;
        }
        
            if (waterX && !onBridgeX && !onDockX) nextX = playerX;
            if (waterY && !onBridgeY && !onDockY) nextY = playerY;
            
            if (colX_obj) nextX = playerX;
            if (colY_obj) nextY = playerY;

            playerX = nextX;
            playerY = nextY;
        }

        // Ẩn bóng chân mặt đất & dụng cụ đất liền khi đang ở trong khoang thuyền
        if (luffyShadow != null) {
            luffyShadow.setVisible(!lockedOnBoat);
        }
        if (weaponView != null) {
            weaponView.setVisible(!lockedOnBoat && !isSunbathing && weaponView.getImage() != null);
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

        // Sprite Animation đầy đủ (Đứng yên + Chạy bộ + Làm việc) cho Cuốc (Hoe) và Bình tưới (WateringCan)
        ToolStrategy activeTool = (playerController != null) ? playerController.getCurrentTool() : null;
        boolean hasHoeFrames = (Assets.LUFFY_HOE_FRAMES != null && Assets.LUFFY_HOE_FRAMES.length == 4 && Assets.LUFFY_HOE_FRAMES[0] != null);
        boolean hasHoeRunFrames = (Assets.LUFFY_HOE_RUN_FRAMES != null && Assets.LUFFY_HOE_RUN_FRAMES.length == 4 && Assets.LUFFY_HOE_RUN_FRAMES[0] != null);
        boolean hasWaterFrames = (Assets.LUFFY_WATER_FRAMES != null && Assets.LUFFY_WATER_FRAMES.length == 3 && Assets.LUFFY_WATER_FRAMES[0] != null);
        boolean hasWaterRunFrames = (Assets.LUFFY_WATER_RUN_FRAMES != null && Assets.LUFFY_WATER_RUN_FRAMES.length == 4 && Assets.LUFFY_WATER_RUN_FRAMES[0] != null);
        boolean usingNativeHoe = !lockedOnBoat && (activeTool instanceof Hoe) && hasHoeFrames;
        boolean usingNativeWater = !lockedOnBoat && (activeTool instanceof WateringCan) && hasWaterFrames;
        boolean usingNativeActionTool = usingNativeHoe || usingNativeWater;

        // Cập nhật timer hành động công cụ (khóa đổi frame chạy trong suốt thời gian vung cuốc / tưới nước)
        boolean nativeActionPlaying = false;
        if (usingNativeActionTool && toolActionTimer >= 0.0) {
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
            if (usingNativeActionTool) {
                boolean isSideSheet = nativeActionPlaying || "side".equals(dirKey);
                if (isSideSheet) {
                    // Sheet 72x72 (ngang & bổ cuốc / tưới nước): scale 120x120 cho chiều cao cơ thể 90px khớp sprite 8 hướng
                    if (luffySpriteView.getFitWidth() != 120.0) {
                        luffySpriteView.setFitWidth(120.0);
                        luffySpriteView.setFitHeight(120.0);
                        luffySpriteView.setX(-38.0);
                        luffySpriteView.setY(-50.0);
                    }
                } else {
                    // Sprite 8 hướng cầm công cụ (down / up / down_right / up_right): đúng kích thước 108x108 đồng bộ tuyệt đối
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
                if (luffySpriteView.getFitWidth() != 108.0) {
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
                    long interval = isWalking ? 95L : 380L;
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

        // Cập nhật vị trí nhân vật trên bản đồ (Dùng setTranslate để tối ưu FPS, không làm lag 600 ô đất)
        playerCharacter.setTranslateX(playerX);
        playerCharacter.setTranslateY(playerY + bobbing);
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

                if (curTool instanceof Hoe) {
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

                    if ("HOE".equals(toolActionKind)) {
                        // Đào đất (0.07s + 0.10s + 0.12s): Giơ -65° sau vai -> Bổ mạnh +55° bắn hạt đất -> Thu mượt về
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
                                if (particleSystem != null && toolTargetWorldX >= 0) {
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

                luffyToolRig.setTranslateX(playerX);
                luffyToolRig.setTranslateY(playerY + bobbing);
            }
        }

        // Bỏ hiệu ứng nghiêng/nhún giả lập khi dùng sprite hành động gốc (hoặc ngả lưng thư giãn khi tắm nắng!)
        if (luffyBodyRotate != null && luffySpriteView != null) {
            if (isSunbathing) {
                luffyBodyRotate.setAngle(72.0);
                luffySpriteView.setScaleY(1.0);
                luffySpriteView.setTranslateY(-6.0);
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
                        if (lowerName.contains("bridge")) {
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
        bridgeCanopyView = null;
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
        riverFlow = new view.river.RiverFlow(worldMapW, riverTopY, riverBotY);
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
                    player.addMoney(rewardMoney);
                }
                onQuestFishCaught();
                if (particleSystem != null && fishingGame != null) {
                    particleSystem.spawnWaterSplash(fishingGame.getLastBobberX(), fishingGame.getLastBobberY());
                    particleSystem.spawnHarvestSparkles(fishingGame.getLastBobberX(), fishingGame.getLastBobberY() - 8.0);
                    if (isCrown) {
                        particleSystem.spawnHeartBurst(boatEntity.getBoardedPlayerX() + 22.0, boatEntity.getBoatY() - 10.0);
                    }
                }
                view.fx.SoundHook.playPickup();
                showFishCatchCard(species, sizeCm, isCrown, isNewRecord, rewardMoney);

                String msg = String.format("Bắt được %s (%.1f cm%s): +%d$",
                        species.getDisplayName(), sizeCm, isCrown ? " 👑 VƯƠNG MIỆN" : "", rewardMoney);
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

    // ======================== actions (Giữ nguyên) ========================
    @FXML
    private void openShop() {
        router.show(Screens.SHOP);
    }

    @FXML
    private void openStorageMenu() {
        router.show(Screens.STORAGE);
    }

    @FXML
    private void onNextDay() {
        advanceDayCore(true);
    }

    /**
     * Logic chuyển ngày dùng chung cho cả nút NEXT DAY và tính năng Đi ngủ tại nhà.
     * @param useQuickFade true nếu chạy fade nhanh mặc định, false nếu đã có màn hình đen Sleep Transition bao bọc.
     */
    private void advanceDayCore(boolean useQuickFade) {
        // Reset thời gian ngày về ban đầu (0ms - Sáng sớm 06:00)
        if (droppedItemSystem != null) {
            droppedItemSystem.collectAllImmediately();
        }
        elapsedTimeInDay = 0;
        if (isSunbathing) stopSunbathing();
        clearFoodBuffs();
        updateQuestTrackerUi();
        // Khi qua ngày mới (đi ngủ), đảm bảo chỉ số buổi sáng tối thiểu 60 và giữ no 4 tiếng đầu buổi sáng
        hungerGraceTimerSec = HUNGER_FULL_GRACE_SEC;
        recentLaborStreak = 0;
        recentLaborDecaySec = 0.0;
        if (player != null) {
            if (player.getHunger() < 60.0) player.setHunger(60.0);
            if (player.getThirst() < 60.0) player.setThirst(60.0);
            updateSurvivalHud(animTimeSec);
        }
        resetFeedingSchedule(0.0);
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
        notify("Day " + gameManager.getCurrentDay() + "   " + weatherName() + growthMsg);
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

        if (curTool instanceof Hoe) {
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
        if (activeCookingOverlay != null) {
            if (!activeCookingOverlay.isCookingInProgress()) {
                closeCookingOverlay();
            }
            return;
        }
        if (activeSleepDialog != null) {
            closeSleepDialog();
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
            router.showOverlay(router.getScreen(Screens.PAUSE));
        }
    }

    // ======================== ĐI NGỦ TẠI NHÀ & NẤU ĂN LỬA TRẠI ========================

    /** Quét tự động vị trí cửa nhà, đống lửa, Khu Bể Bơi (Nami & Hancock), Sòng Bạc Casino và khởi tạo 4 Bot đi dạo. */
    private void setupHouseAndCampfireInteractions() {
        houseDoorPoints.clear();
        campfirePoints.clear();
        campfireGlows.clear();
        casinoDoorPoints.clear();
        poolSunbathePoints.clear();
        poolWaveLines.clear();
        poolSparkleDots.clear();
        casinoMarqueeBulbs.clear();
        isSunbathing = false;

        double[] mainHouseDoor = null;
        double topLoungerX = 672.0;
        double topLoungerY = 2476.0;
        boolean foundFirstLounger = false;

        for (ImageView objView : sceneryObjects) {
            String id = objView.getId();
            if (id == null) continue;
            String idLow = id.toLowerCase();
            double ox = objView.getLayoutX();
            double oy = objView.getLayoutY();
            double ow = objView.getFitWidth();
            double oh = objView.getFitHeight();

            if (idLow.contains("house_main") || idLow.contains("house_small") || idLow.contains("house_bottom")) {
                double[] door = new double[] { ox + ow * 0.50, oy + oh + 10.0 };
                if (idLow.contains("house_main")) {
                    mainHouseDoor = door;
                } else {
                    houseDoorPoints.add(door);
                }
            } else if (idLow.equals("campfire")) {
                double cx = ox + ow * 0.50;
                double cy = oy + oh * 0.58;
                campfirePoints.add(new double[] { cx, cy });

                if (entityPane != null) {
                    javafx.scene.paint.RadialGradient fireGrad = new javafx.scene.paint.RadialGradient(
                        0, 0, 0.5, 0.5, 0.5, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0.0, Color.rgb(255, 168, 56, 0.42)),
                        new javafx.scene.paint.Stop(0.55, Color.rgb(255, 114, 32, 0.18)),
                        new javafx.scene.paint.Stop(1.0, Color.rgb(255, 92, 20, 0.0))
                    );
                    javafx.scene.shape.Circle glow = new javafx.scene.shape.Circle(0, 0, 86.0);
                    glow.setFill(fireGrad);
                    glow.setManaged(false);
                    glow.setMouseTransparent(true);
                    glow.setLayoutX(cx);
                    glow.setLayoutY(cy + 4.0);
                    double sortY = (objView.getUserData() instanceof Double) ? ((Double) objView.getUserData()) - 2.0 : (oy + oh - 2.0);
                    glow.setUserData(sortY);
                    entityPane.getChildren().add(glow);
                    campfireGlows.add(glow);
                }
            } else if (idLow.equals("swimming_pool")) {
                poolWaterX = ox + 26.0;
                poolWaterY = oy + 26.0;
                poolWaterW = Math.max(60.0, ow - 52.0);
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

    private void onQuestFishCaught() {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onFishCaught()) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestFoodCooked() {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onFoodCooked()) {
            onQuestProgressUpdated();
        }
    }

    private void onQuestAnimalHarvested() {
        if (player != null && player.getQuestManager() != null && player.getQuestManager().onAnimalHarvested()) {
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
     * Dựng lớp cầu gỗ che phía trên (chỉ cắt đúng phần gỗ + 2 thành cầu, xóa sạch viền nước ngoài)
     * và 2 dải bóng râm đổ xuống mặt nước hai bên gầm cầu để tạo cảm giác thuyền chui lọt bên dưới cầu.
     */
    private void setupBridgeUnderpassVisuals(ImageView bridgeObj, double riverTopY, double riverBotY) {
        if (bridgeObj == null || entityPane == null) return;
        double ox = bridgeObj.getLayoutX();
        double oy = bridgeObj.getLayoutY();
        double ow = bridgeObj.getFitWidth();
        double oh = bridgeObj.getFitHeight();
        if (ow <= 0 || oh <= 0) return;

        // Tỉ lệ phần gỗ thực của bridge.png: x = 15.5% .. 86.2% (trừ sạch nước 2 bên)
        final double leftFrac = 0.155;
        final double rightFrac = 0.862;
        bridgeWoodLeftX = ox + ow * leftFrac;
        bridgeWoodRightX = ox + ow * rightFrac;

        Image src = bridgeObj.getImage();
        if (src != null && src.getPixelReader() != null) {
            int imgW = (int) src.getWidth();
            int imgH = (int) src.getHeight();
            int sx0 = Math.max(0, (int) Math.round(imgW * leftFrac));
            int sx1 = Math.min(imgW, (int) Math.round(imgW * rightFrac));
            int cw = Math.max(1, sx1 - sx0);
            javafx.scene.image.WritableImage canopyImg = new javafx.scene.image.WritableImage(cw, imgH);
            javafx.scene.image.PixelReader pr = src.getPixelReader();
            javafx.scene.image.PixelWriter pw = canopyImg.getPixelWriter();
            for (int y = 0; y < imgH; y++) {
                for (int x = 0; x < cw; x++) {
                    Color col = pr.getColor(sx0 + x, y);
                    // Xóa các pixel nước xanh sót lại ở 6px mép ngoài 2 thành gỗ để viền cầu sắc nét tuyệt đối
                    boolean onOuterEdge = (x < 6 || x >= cw - 6);
                    boolean isBlueWater = (col.getOpacity() > 0.1 && col.getBlue() > col.getRed() + 0.14 && col.getBlue() > 0.50);
                    if (onOuterEdge && isBlueWater) {
                        pw.setColor(x, y, Color.TRANSPARENT);
                    } else {
                        pw.setColor(x, y, col);
                    }
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
        }

        // Dải bóng râm mềm của gầm cầu đổ xuống mặt nước ở hai bên trái & phải cầu
        double shadowTopY = Math.max(oy + 24.0, riverTopY);
        double shadowH = Math.max(60.0, Math.min(oy + oh - 14.0, riverBotY) - shadowTopY);
        javafx.scene.shape.Rectangle leftShadow = new javafx.scene.shape.Rectangle(bridgeWoodLeftX - 28.0, shadowTopY, 34.0, shadowH);
        leftShadow.setFill(new javafx.scene.paint.LinearGradient(
            0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0.0, Color.rgb(6, 18, 28, 0.0)),
            new javafx.scene.paint.Stop(1.0, Color.rgb(6, 18, 28, 0.48))
        ));
        leftShadow.setMouseTransparent(true);
        leftShadow.setManaged(false);
        leftShadow.setUserData(-890.0);

        javafx.scene.shape.Rectangle rightShadow = new javafx.scene.shape.Rectangle(bridgeWoodRightX - 6.0, shadowTopY, 34.0, shadowH);
        rightShadow.setFill(new javafx.scene.paint.LinearGradient(
            0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0.0, Color.rgb(6, 18, 28, 0.48)),
            new javafx.scene.paint.Stop(1.0, Color.rgb(6, 18, 28, 0.0))
        ));
        rightShadow.setMouseTransparent(true);
        rightShadow.setManaged(false);
        rightShadow.setUserData(-890.0);

        entityPane.getChildren().addAll(leftShadow, rightShadow);
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

            double[] nearFire = findNearestPoint(campfirePoints, px, py, 80.0);
            if (nearFire != null) {
                double d = Math.hypot(px - nearFire[0], py - nearFire[1]);
                if (d < bestDist) {
                    bestDist = d;
                    promptText = "[E] Nấu ăn tại đống lửa";
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

            if (npcManager != null) {
                view.npc.NpcManager.BotActor nearBot = npcManager.findNearestBot(px, py, 80.0);
                if (nearBot != null) {
                    double d = Math.hypot(px - (nearBot.x + 22.0), py - (nearBot.y + 54.0));
                    if (d < bestDist) {
                        bestDist = d;
                        promptText = "[E] Trò chuyện với " + nearBot.name;
                    }
                }
            }

            if (promptText == null && isNearRiverBankForDrink()) {
                promptText = "[R] Uống nước sông";
            }
        }

        boolean promptShown = false;
        if (promptText != null) {
            if (!promptText.equals(interactPromptLabel.getText())) {
                interactPromptLabel.setText(promptText);
                interactPromptLabel.autosize();
            }
            double w = interactPromptLabel.getWidth() > 10 ? interactPromptLabel.getWidth() : Math.max(196.0, promptText.length() * 6.8);
            interactPromptLabel.setLayoutX(playerX + 22.0 - w * 0.5);
            interactPromptLabel.setLayoutY(playerY - 48.0);
            interactPromptLabel.setVisible(true);
            promptShown = true;
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
                survivalWarningLabel.setLayoutY(playerY - (promptShown ? 76.0 : 48.0));
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
            if (!activeCookingOverlay.isCookingInProgress()) {
                closeCookingOverlay();
            }
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
        int bestAction = 0; // 1=sleep, 2=cook, 3=boat, 4=pool, 5=casino, 6=bot
        double[] bestPt = null;
        view.npc.NpcManager.BotActor bestBot = null;

        double[] nearDoor = findNearestPoint(houseDoorPoints, px, py, 75.0);
        if (nearDoor != null) {
            double d = Math.hypot(px - nearDoor[0], py - nearDoor[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 1;
                bestPt = nearDoor;
            }
        }

        double[] nearFire = findNearestPoint(campfirePoints, px, py, 80.0);
        if (nearFire != null) {
            double d = Math.hypot(px - nearFire[0], py - nearFire[1]);
            if (d < bestDist) {
                bestDist = d;
                bestAction = 2;
                bestPt = nearFire;
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

        if (npcManager != null) {
            view.npc.NpcManager.BotActor nearBot = npcManager.findNearestBot(px, py, 80.0);
            if (nearBot != null) {
                double d = Math.hypot(px - (nearBot.x + 22.0), py - (nearBot.y + 54.0));
                if (d < bestDist) {
                    bestDist = d;
                    bestAction = 6;
                    bestBot = nearBot;
                }
            }
        }

        if (bestAction == 1 && bestPt != null) {
            openSleepDialog(bestPt[0], bestPt[1]);
        } else if (bestAction == 2 && bestPt != null) {
            openCookingOverlay(bestPt[0], bestPt[1]);
        } else if (bestAction == 3 && boatEntity != null) {
            boatEntity.startBoarding(playerX, playerY);
        } else if (bestAction == 4) {
            startSunbathing();
        } else if (bestAction == 5) {
            openCasinoOverlay();
        } else if (bestAction == 6 && bestBot != null) {
            int curDay = (gameManager != null) ? gameManager.getCurrentDay() : 1;
            npcManager.interactWithBot(
                bestBot,
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

    /** Nằm tắm nắng cạnh bể bơi cùng Nami & Hancock: hồi Đói/Khát + nhận Buff "Thư giãn +20% tốc độ chạy" trong 2 phút. */
    private void startSunbathing() {
        isSunbathing = true;
        sunbatheSparkleTimer = 0.0;
        activeKeys.clear();
        playerX = sunbatheLoungerX + 20.0;
        playerY = sunbatheLoungerY - 22.0;
        facing = "LEFT";
        luffyFacingLeft = true;
        if (playerCharacter != null) {
            playerCharacter.setTranslateX(playerX);
            playerCharacter.setTranslateY(playerY);
        }
        poolRelaxSpeedBuffSec = 120.0;
        lastRenderedRelaxSec = 120;
        updateFoodBuffHud();
        view.fx.SoundHook.playHeart();
        if (particleSystem != null) {
            particleSystem.spawnHeartBurst(playerX + 22.0, playerY + 6.0);
        }
        spawnFloatingText("☀ Thư giãn +20% tốc độ chạy (2 phút)!", Color.rgb(255, 224, 102));
        notify("☀ Đang nằm tắm nắng cạnh Nami & Hancock! Hồi dần Đói/Khát & nhận Buff +20% tốc độ chạy (2 phút).");
    }

    private void stopSunbathing() {
        if (!isSunbathing) return;
        isSunbathing = false;
        if (luffyBodyRotate != null) luffyBodyRotate.setAngle(0.0);
        if (luffySpriteView != null) luffySpriteView.setTranslateY(0.0);
        notify("Đã đứng dậy khỏi ghế tắm nắng! (Buff +20% tốc độ chạy còn " + (int) Math.ceil(poolRelaxSpeedBuffSec) + "s)");
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

    /** Hiệu ứng màn hình tối dần 0.6s ("Đang ngủ... Zzz" + Ngày X) -> Chuyển ngày -> Sáng dần 0.6s trước cửa nhà. */
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
        curtain.prefWidthProperty().bind(rootPane.widthProperty());
        curtain.prefHeightProperty().bind(rootPane.heightProperty());
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

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.6), curtain);
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
            advanceDayCore(false);
            updateTimeDisplay();
            dayText.setText("Ngày " + gameManager.getCurrentDay() + "  •  06:00 Sáng");

            javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.55));
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.6), curtain);
            fadeIn.setFromValue(1.0);
            fadeIn.setToValue(0.0);
            fadeIn.setOnFinished(done -> {
                rootPane.getChildren().remove(curtain);
                sleepTransitionRunning = false;
                activeKeys.clear();
                Platform.runLater(rootPane::requestFocus);
            });
            new javafx.animation.SequentialTransition(hold, fadeIn).play();
        });
        fadeOut.play();
    }

    /** Mở bảng Nấu Ăn tại Đống Lửa Trại. */
    private void openCookingOverlay(double fireX, double fireY) {
        if (router == null || router.hasOverlay() || sleepTransitionRunning) return;
        activeKeys.clear();
        activeCookingOverlay = new view.ui.CampfireCookingOverlay(
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
            Platform.runLater(rootPane::requestFocus);
        }
    }

    /** Áp dụng hồi Đói/Khát + Buff tỉ lệ câu cá hiếm (nếu có) khi ăn món nấu từ đống lửa (KHÔNG tăng tốc độ chạy). */
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
    }

    // ======================== HỆ THỐNG SINH TỒN: ĐÓI & KHÁT ========================

    /** Khởi tạo khung HUD gỗ/pixel 2 thanh ĐÓI & NƯỚC có tiêu đề rõ ràng + icon vẽ nét (không dùng emoji lỗi font). */
    private void setupSurvivalHud() {
        if (rootPane == null) return;
        if (survivalHudBox != null) {
            if (!rootPane.getChildren().contains(survivalHudBox)) {
                rootPane.getChildren().add(survivalHudBox);
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
        survivalHudBox.setPrefWidth(226.0);
        survivalHudBox.setMouseTransparent(true);
        survivalHudBox.setManaged(false);
        survivalHudBox.setLayoutX(16.0);
        survivalHudBox.setLayoutY(12.0);
        survivalHudBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(114, 68, 34, 0.93), rgba(74, 42, 20, 0.95));" +
            "-fx-border-color: #2E170A;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 8, 0.2, 0, 2);"
        );
        rootPane.getChildren().add(survivalHudBox);
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

        player.addHunger(-hungerCost);
        player.addThirst(-thirstCost);
        updateSurvivalHud(animTimeSec);
    }

    /** Kiểm tra nhân vật có đang đứng sát bờ con sông (không ở trên thuyền) để uống nước hay không. */
    private boolean isNearRiverBankForDrink() {
        if (farmMap == null || isOnBoat) return false;
        if (boatEntity != null && (boatEntity.isPlayerAboard() || boatEntity.isTransitioning())) return false;

        double footX = playerX + 22.0;
        double footY = playerY + 54.0;
        int col = (int) (footX / Constant.CELL_SIZE);
        int row = (int) (footY / Constant.CELL_SIZE);

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                Cell c = farmMap.getCell(row + dr, col + dc);
                if (c != null && c.getTerrain() == TerrainType.WATER) {
                    return true;
                }
            }
        }
        return (Math.abs(footY - worldRiverTopY) <= 58.0 || Math.abs(footY - worldRiverBotY) <= 58.0);
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
    }

    /** Ăn nhanh 1 món đã nấu sẵn trong Inventory khi đang đi ngoài đồng bằng phím [Q]. */
    private void tryQuickEatFood() {
        if (player == null) return;
        model.game_item.CookedFoodItem chosen = null;
        boolean needWater = player.getThirst() < 45.0;
        for (java.util.Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey() instanceof model.game_item.CookedFoodItem) {
                model.game_item.CookedFoodItem cand = (model.game_item.CookedFoodItem) e.getKey();
                if (chosen == null) {
                    chosen = cand;
                } else if (needWater && cand.getThirstRestore() > chosen.getThirstRestore()) {
                    chosen = cand;
                }
            }
        }
        if (chosen == null) {
            spawnFloatingText("Hết đồ ăn! Hãy nấu tại đống lửa!", Color.rgb(255, 206, 102));
            notify("Không có món ăn đã nấu trong kho! Hãy tới đống lửa trại để nấu.");
            return;
        }
        if (player.getInventory().removeItem(chosen, 1)) {
            if (particleSystem != null) {
                particleSystem.spawnPickupPop(playerX + 22.0, playerY + 18.0);
            }
            applyCookedFoodBuff(chosen);
        }
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
        if (foodBuffHudPill == null) {
            foodBuffHudPill = new Label();
            foodBuffHudPill.setMouseTransparent(true);
            foodBuffHudPill.setManaged(false);
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
            rootPane.getChildren().add(foodBuffHudPill);
        }
        foodBuffHudPill.setText("BUFF: " + summary);
        foodBuffHudPill.setLayoutX(16.0);
        foodBuffHudPill.setLayoutY(130.0);
        foodBuffHudPill.setVisible(true);
        foodBuffHudPill.toFront();
    }

    /** Mở / đóng Sổ Tay & Hướng Dẫn Câu Cá (Nút SỔ CÁ hoặc phím G). */
    @FXML
    private void toggleFishingGuide() {
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
        if (router == null || router.hasOverlay()) return;

        activeKeys.clear(); // Tạm dừng nhận phím di chuyển khi mở sổ
        WeatherType weather = (gameManager != null && gameManager.getCurrentWeather() != null)
                ? gameManager.getCurrentWeather().getType()
                : WeatherType.SUNNY;
        int curHour = getCurrentGameHour();
        String clockStr = clockText(Math.min(1.0, Math.max(0.0, elapsedTimeInDay / 135000.0)));

        activeFishingGuide = new view.ui.FishingGuideOverlay(
                weather, curHour, clockStr, fishCaughtCounts, fishMaxCm, this::closeFishingGuide);
        router.showOverlay(activeFishingGuide);
        Platform.runLater(activeFishingGuide::requestFocus);
    }

    /**
     * Hiển thị thẻ Khoe Cá phía trên màn hình (tự mờ ẩn sau 2.5 giây) với Tên cá, Độ hiếm, cm, Vương Miện & Kỷ lục mới.
     */
    private void showFishCatchCard(model.game_item.FishSpecies species, double sizeCm,
                                   boolean isCrown, boolean isNewRecord, int rewardMoney) {
        if (rootPane == null) return;
        if (fishCatchCard == null) {
            fishCatchCard = new javafx.scene.layout.VBox(4);
            fishCatchCard.setAlignment(Pos.CENTER);
            fishCatchCard.setMouseTransparent(true);
            fishCatchCard.setManaged(false);
            fishCatchCard.setPrefWidth(420);
            fishCatchCard.setPadding(new javafx.geometry.Insets(10, 18, 10, 18));
            rootPane.getChildren().add(fishCatchCard);
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

        String rewardTxt = isCrown
                ? ("+" + rewardMoney + "$ (x1.5 Vương Miện!)  •  +1 " + species.getDisplayName() + " vào kho")
                : ("+" + rewardMoney + "$  •  +1 " + species.getDisplayName() + " vào kho");
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
        double progress = Math.min(1.0, Math.max(0.0, elapsedTimeInDay / 135000.0));
        int totalMins = (int) (progress * 16 * 60);
        return (6 + totalMins / 60) % 24;
    }

    /** Thanh gợi ý nhỏ phía dưới màn hình khi người chơi đứng gần hoặc bước lên thuyền. */
    private void updateBoatHintBanner(boolean show) {
        if (show == boatHintVisible) return;
        boatHintVisible = show;
        if (rootPane == null) return;
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
            boatHintBanner.setManaged(false);
            boatHintBanner.setPrefWidth(560);
            boatHintBanner.setAlignment(Pos.CENTER);
            rootPane.getChildren().add(boatHintBanner);
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
                targetWeatherTint = Color.rgb(34, 60, 96, 0.14);
                break;
            case DROUGHT:
                targetWeatherTint = Color.rgb(228, 142, 42, 0.11);
                break;
            case SUNNY:
            default:
                targetWeatherTint = Color.rgb(255, 245, 220, 0.0);
                break;
        }
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
                lastSoilPanelVisible = false;
            }
            return;
        }
        hovered = cell;
        if (soilStatusPanel != null && !soilStatusPanel.isVisible()) {
            soilStatusPanel.setVisible(true);
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
        waterTool.setText(null);
        seedTool.setText(null);
        handTool.setText(null);
        fertilizerTool.setText(null);
        hoeTool.setGraphic(Assets.imageView(Assets.HOE_ICON, 48));
        waterTool.setGraphic(Assets.imageView(Assets.WATER_ICON, 48));
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
                if (tilled) {
                    img = wet ? Assets.TILLED_WET_TILE : Assets.TILLED_DRY_TILE;
                    backgroundView.setScaleX(0.96);
                    backgroundView.setScaleY(0.96);
                } else if (gid == 3 && Assets.UNPLOWED_BED_TILE != null) {
                    img = Assets.UNPLOWED_BED_TILE;
                    backgroundView.setScaleX(1.0);
                    backgroundView.setScaleY(1.0);
                } else {
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
            if (cropImg != lastCropImg) {
                lastCropImg = cropImg;
                cropView.setImage(cropImg);
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
    /** Khu vực chăn thả (AnimalZone trong TMX); null nếu map không định nghĩa. */
    private javafx.geometry.Rectangle2D animalZone;
    /** Khu vực canh tác (FarmZone trong TMX); giới hạn phạm vi hoạt động của Cuốc. */
    private javafx.geometry.Rectangle2D farmZone;

    private boolean isCellInFarmZone(Cell cell) {
        if (cell == null) return false;
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
    private static final double[] MEAL_TIMES = {0.25, 0.60}; // ~10:00 và ~15:36
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
            double sx = x + 15;
            return animalZone != null ? Math.min(sx, animalZone.getMaxX() - 45) : sx;
        }

        double spotY() {
            double sy = y + 30;
            return animalZone != null ? Math.min(sy, animalZone.getMaxY() - 45) : sy;
        }

        javafx.geometry.Rectangle2D bounds() {
            return new javafx.geometry.Rectangle2D(x - 6, y - 14, 82, 46);
        }
    }

    private void buildTroughs() {
        troughs = new java.util.ArrayList<>();
        if (animalZone == null || entityPane == null) return;
        // Đặt máng ăn hoàn toàn bên trong khu chăn nuôi (AnimalZone)
        double ty = animalZone.getMinY() + 14;
        double w = animalZone.getWidth();
        troughs.add(new FeedTrough(0, "\uD83D\uDC14", "Gà", animalZone.getMinX() + w * 0.15, ty));
        troughs.add(new FeedTrough(1, "\uD83D\uDC04", "Bò", animalZone.getMinX() + w * 0.45, ty));
        troughs.add(new FeedTrough(3, "\uD83D\uDC16", "Lợn", animalZone.getMinX() + w * 0.75, ty));
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
        int mins = 6 * 60 + (int) Math.round(progress * 16 * 60);
        return String.format("%02d:%02d", (mins / 60) % 24, mins % 60);
    }

    private void resetFeedingSchedule(double progress) {
        for (int i = 0; i < MEAL_TIMES.length; i++) {
            boolean passed = progress >= MEAL_TIMES[i];
            mealStarted[i] = passed;
            mealReminded[i] = passed || progress >= MEAL_TIMES[i] - 0.05;
        }
        eveningWarned = progress >= 0.85;
    }

    private void updateFeedingSchedule() {
        if (troughs.isEmpty()) return;
        double p = elapsedTimeInDay / 135000.0;
        long now = System.currentTimeMillis();

        for (int i = 0; i < MEAL_TIMES.length; i++) {
            if (!mealReminded[i] && p >= MEAL_TIMES[i] - 0.05) {
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

        if (!eveningWarned && p >= 0.85) {
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
        if (rootPane == null) return;
        if (toastLabel == null) {
            toastLabel = new Label();
            toastLabel.getStyleClass().add("npc-name");
            toastLabel.setWrapText(true);
            toastLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            toastLabel.setAlignment(Pos.CENTER);
            toastLabel.setStyle("-fx-font-size: 16px;");
            toastLabel.setMouseTransparent(true);
            toastLabel.setManaged(false);
            toastLabel.setPrefWidth(620);
            toastLabel.setLayoutY(80);
            rootPane.getChildren().add(toastLabel);
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

    private double clampToAnimalZoneX(double px) {
        if (animalZone != null) {
            double minX = animalZone.getMinX() + 12;
            double maxX = Math.max(minX, animalZone.getMaxX() - 48);
            return Math.max(minX, Math.min(px, maxX));
        }
        double mapW = farmMap.getWidth() * Constant.CELL_SIZE;
        return Math.max(0, Math.min(px, mapW - 40));
    }

    private double clampToAnimalZoneY(double py) {
        if (animalZone != null) {
            double minY = animalZone.getMinY() + 18;
            double maxY = Math.max(minY, animalZone.getMaxY() - 42);
            return Math.max(minY, Math.min(py, maxY));
        }
        double mapH = farmMap.getHeight() * Constant.CELL_SIZE;
        return Math.max(0, Math.min(py, mapH - 40));
    }

    private boolean isAnimalBlockedAt(double px, double py) {
        if (animalZone != null) {
            double minX = animalZone.getMinX() + 10;
            double maxX = animalZone.getMaxX() - 45;
            double minY = animalZone.getMinY() + 16;
            double maxY = animalZone.getMaxY() - 40;
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
                startX = clampToAnimalZoneX(startX);
                startY = clampToAnimalZoneY(startY);
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

            // Luôn đảm bảo vật nuôi nằm trong vùng AnimalZone
            x = clampToAnimalZoneX(x);
            y = clampToAnimalZoneY(y);

            if (now >= pauseUntil) {
                double dx = targetX - x;
                double dy = targetY - y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 5) {
                    targetX = clampToAnimalZoneX(x + (Math.random() - 0.5) * 180);
                    targetY = clampToAnimalZoneY(y + (Math.random() - 0.5) * 180);

                    if (isAnimalBlockedAt(targetX, targetY)) {
                        targetX = x;
                        targetY = y;
                    } else {
                        if (Math.random() < 0.6) {
                            pauseUntil = now + 1500 + (long)(Math.random() * 3000);
                        }
                        updateFacing(targetX < x);
                    }
                } else {
                    double nextX = clampToAnimalZoneX(x + (dx / dist) * speed);
                    double nextY = clampToAnimalZoneY(y + (dy / dist) * speed);
                    if (isAnimalBlockedAt(nextX, nextY)) {
                        // Đổi hướng ngẫu nhiên mới bên trong AnimalZone nếu gặp vật cản/rào
                        targetX = clampToAnimalZoneX(x + (Math.random() - 0.5) * 160);
                        targetY = clampToAnimalZoneY(y + (Math.random() - 0.5) * 160);
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
