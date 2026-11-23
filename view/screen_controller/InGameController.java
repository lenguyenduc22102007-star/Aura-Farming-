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

    // --- layout + labels ---
    @FXML private BorderPane rootPane;
    @FXML private GridPane farmGrid;
    @FXML private javafx.scene.layout.Pane weatherOverlay;
    private javafx.scene.shape.Rectangle timeOverlayRect;
    @FXML private Label toolLabel;
    @FXML private TextField notificationField;
    @FXML private TextField dayField;
    @FXML private Label moneyField;
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
    private javafx.scene.transform.Rotate leftArmSwing, rightArmSwing, leftLegSwing, rightLegSwing;
    private ImageView weaponView; // Vũ khí cầm tay
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

    private void setupTimeOverlay() {
        if (rootPane == null) return;
        if (timeOverlayRect == null) {
            timeOverlayRect = new javafx.scene.shape.Rectangle();
            timeOverlayRect.setMouseTransparent(true);
            timeOverlayRect.setManaged(false);
            timeOverlayRect.setX(0);
            timeOverlayRect.setY(0);
            timeOverlayRect.widthProperty().bind(rootPane.widthProperty());
            timeOverlayRect.heightProperty().bind(rootPane.heightProperty());
            timeOverlayRect.setFill(getTimeOverlayColor(0.0));
            rootPane.getChildren().add(timeOverlayRect);
        } else {
            if (weatherOverlay != null) {
                weatherOverlay.getChildren().remove(timeOverlayRect);
            }
            if (!rootPane.getChildren().contains(timeOverlayRect)) {
                rootPane.getChildren().add(timeOverlayRect);
            }
            timeOverlayRect.setManaged(false);
            timeOverlayRect.setX(0);
            timeOverlayRect.setY(0);
            timeOverlayRect.toFront();
        }
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

        wireButtons();
        setKeyHandlers();
        
        // Thay thế hàm buildGrid bằng hàm xây bản đồ tự do
        buildFreeMap(); 

        selectTool(hand, handTool);
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

        rootPane.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode()); // Thêm phím vào danh sách để di chuyển

            switch (event.getCode()) {
                case ESCAPE:  togglePause();   break;
                case DIGIT1:  openSeedPicker(); break;
                case DIGIT2:  selectTool(hoe, hoeTool);            break;
                case DIGIT3:  selectTool(wateringCan, waterTool);  break;
                case DIGIT4:  selectTool(hand, handTool);          break;
                case DIGIT5:  selectTool(fertilizerBag, fertilizerTool); break;
                case DIGIT6:  openFeedBagPicker();                 break;
                case J:       // Nút Action (Cuốc đất, trồng cây)
                case SPACE:
                    interactWithFacingCell();
                    break;
                case F:
                    demoController.fastForwardGrowthMap();
                    for (Animal a : animals) {
                        if (a.type != 2 && a.ageDays < 5) {
                            a.ageDays++;
                            a.applySpriteScale();
                        }
                    }
                    notify("Tua nhanh cây trồng & thú cưng (+1 ngày tuổi)");
                    refreshAll();
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
            case "UP":    row -= 1; break;
            case "DOWN":  row += 1; break;
            case "LEFT":  col -= 1; break;
            case "RIGHT": col += 1; break;
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
                idLower.contains("windmill") || idLower.contains("garden_bed")) {
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
        
        actionTime = System.currentTimeMillis(); // Kích hoạt hiệu ứng vung vũ khí

        double targetX = playerX + 22;
        double targetY = playerY + 50;
        double reach = 40;
        switch(facing) {
            case "UP":    targetY -= reach; break;
            case "DOWN":  targetY += reach; break;
            case "LEFT":  targetX -= reach; break;
            case "RIGHT": targetX += reach; break;
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
        
        // Đập phá vật cản (Lúa dại ở FarmZone, Đá, Cây, Bụi cỏ, Khúc gỗ...) bằng Cuốc
        if (playerController.getCurrentTool().getName().equals("Hoe")) {
            // Ưu tiên dọn đúng ô lúa dại (wheat_patch) ở ô đang hướng tới để lộ mặt đất trồng cây
            Cell facingForWheat = getFacingCell();
            if (facingForWheat != null) {
                double fcx = facingForWheat.getX() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
                double fcy = facingForWheat.getY() * Constant.CELL_SIZE + Constant.CELL_SIZE / 2.0;
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
                    rootPane.requestFocus();
                    return;
                }
            }

            ImageView toDestroy = null;
            for (ImageView objView : sceneryObjects) {
                String id = objView.getId();
                if (id != null) {
                    String idLower = id.toLowerCase();
                    // Không phá nhà, lều, lửa trại, cối xay gió, cầu, hàng rào, cờ, giếng, chuồng, đèn đường, biển chỉ dẫn, hoa/đất trang trí
                    if (idLower.contains("house") || idLower.contains("tent") || idLower.contains("campfire") ||
                        idLower.contains("windmill") || idLower.contains("streetlamp") || idLower.contains("lantern") ||
                        idLower.contains("signpost") || idLower.contains("garden_bed") || idLower.contains("construction") ||
                        idLower.contains("wheat") || idLower.contains("bridge") || idLower.contains("fence") ||
                        idLower.contains("flag") || idLower.contains("well") || idLower.contains("barn") ||
                        idLower.contains("dirt_spot") || idLower.contains("flower") || idLower.contains("pebble") ||
                        idLower.contains("grass_patch")) {
                        continue;
                    }
                }
                javafx.geometry.Bounds b = objView.getBoundsInParent();
                javafx.geometry.Rectangle2D bRect = new javafx.geometry.Rectangle2D(b.getMinX(), b.getMinY(), Math.max(1, b.getWidth()), Math.max(1, b.getHeight()));
                if (hitBox.intersects(bRect)) {
                    toDestroy = objView;
                    break;
                }
            }
            if (toDestroy != null) {
                clearedSceneryKeys.add(sceneryKey(toDestroy));
                sceneryObjects.remove(toDestroy);
                if (entityPane != null) {
                    entityPane.getChildren().remove(toDestroy);
                } else {
                    mapPane.getChildren().remove(toDestroy);
                }
                notify("Đã dọn dẹp vật cản!");
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

            boolean ok = playerController.interactWithCell(targetCell);
            notify(ok ? "Thao tác thành công!" : "Không thể làm điều này ở đây");
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
        p.setProperty("view.playerX", Double.toString(playerX));
        p.setProperty("view.playerY", Double.toString(playerY));
        p.setProperty("view.facing", facing);
        p.setProperty("view.elapsed", Long.toString(elapsedTimeInDay));
        p.setProperty("view.helperDays", Integer.toString(helperDaysLeft));

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
        try {
            playerX = Double.parseDouble(p.getProperty("view.playerX", Double.toString(playerX)));
            playerY = Double.parseDouble(p.getProperty("view.playerY", Double.toString(playerY)));
            facing = p.getProperty("view.facing", facing);
            elapsedTimeInDay = Long.parseLong(p.getProperty("view.elapsed", "0"));
            helperDaysLeft = Integer.parseInt(p.getProperty("view.helperDays", "0"));
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
                notify("💰 Đã bán " + name + " trưởng thành thành công: +" + money + "$!");
                rootPane.requestFocus();
                return true;
            }
        }

        // 2. Kiểm tra cho thú ăn nếu chưa trưởng thành (ageDays < 5)
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
            if (feedItem != null && player.getInventory().getItemCount(feedItem) >= 1) {
                if (!toFeed.isFed) {
                    player.getInventory().removeItem(feedItem, 1);
                    toFeed.feed();
                    revertBagToolIfEmpty();
                    notify("Đã cho " + aName + " ăn " + feedItem.getName() + "! (Tiến độ: Ngày " + toFeed.ageDays + "/5 - Mai sẽ lớn thêm)");
                } else {
                    notify(aName + " đã no hôm nay! (Ngày " + toFeed.ageDays + "/5 - Còn " + (5 - toFeed.ageDays) + " ngày để bán)");
                }
            } else {
                model.game_item.Item rawCrop = null;
                for (model.game_item.Item item : player.getInventory().getItems().keySet()) {
                    if (item instanceof model.game_item.HarvestedCropItem && player.getInventory().getItemCount(item) > 0) {
                        rawCrop = item;
                        break;
                    }
                }
                if (rawCrop != null) {
                    if (!toFeed.isFed) {
                        player.getInventory().removeItem(rawCrop, 1);
                        toFeed.feed();
                        revertBagToolIfEmpty();
                        notify("Đã cho " + aName + " ăn bằng " + rawCrop.getName() + "! (Tiến độ: Ngày " + toFeed.ageDays + "/5)");
                    } else {
                        notify(aName + " đã no hôm nay! (Ngày " + toFeed.ageDays + "/5 - Còn " + (5 - toFeed.ageDays) + " ngày để bán)");
                    }
                } else {
                    if (toFeed.isFed) {
                        notify(aName + " đã no hôm nay! (Ngày " + toFeed.ageDays + "/5 - Còn " + (5 - toFeed.ageDays) + " ngày để bán)");
                    } else {
                        notify(aName + " đang đói! Bấm vào Cặp (Bag - phím 6) hoặc vào Shop mua thức ăn nhé!");
                    }
                }
            }
            rootPane.requestFocus();
            return true;
        }

        return false;
    }

    private long lastFrameTime = 0;
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
        elapsedTimeInDay = 0;
        Platform.runLater(() -> rootPane.requestFocus());
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                try {
                    long currentMillis = System.currentTimeMillis();
                    long delta = currentMillis - lastFrameTime;
                    lastFrameTime = currentMillis;

                    if (rootPane.getParent() == null) {
                        // Don't stop the timer, just pause updates.
                        return;
                    }
                    if (!router.hasOverlay()) {
                        updatePlayerPosition();
                        updateRainEffect();
                        
                        // Tự động qua ngày sau mỗi 135 giây (135000 ms)
                        elapsedTimeInDay += delta;
                        if (elapsedTimeInDay >= 135000) {
                            onNextDay(); // onNextDay tự động reset elapsedTimeInDay về 0 và cập nhật sky/clock
                        } else {
                            // Cập nhật hiệu ứng thời gian trong ngày (Sáng, Trưa, Tối) mượt mà
                            if (timeOverlayRect != null) {
                                double progress = elapsedTimeInDay / 135000.0;
                                timeOverlayRect.setFill(getTimeOverlayColor(progress));
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
        int min = (totalMins % 60) / 5 * 5;
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

    private void updateRainEffect() {
        if (weatherOverlay == null) return;
        
        model.weather.WeatherType cur = gameManager.getCurrentWeather() != null
                ? gameManager.getCurrentWeather().getType() : null;
        
        if (cur == model.weather.WeatherType.RAINY) {
            double w = Math.max(1200, rootPane.getWidth());
            // Spawn 2-3 new raindrops every frame
            for (int i = 0; i < 3; i++) {
                javafx.scene.shape.Line drop = new javafx.scene.shape.Line(0, 0, 0, 15);
                drop.setStroke(javafx.scene.paint.Color.rgb(180, 220, 255, 0.8));
                drop.setStrokeWidth(2.5);
                
                // Vì mưa bay xiên từ phải sang trái, ta cần mở rộng vùng spawn bên phải
                drop.setLayoutX(Math.random() * (w + 400));
                drop.setLayoutY(-20);
                drop.setUserData(18 + Math.random() * 12); // Tốc độ rơi khá nhanh
                weatherOverlay.getChildren().add(drop);
            }
        }
        
        // Move drops and remove off-screen ones
        double h = Math.max(800, rootPane.getHeight());
        java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
        for (javafx.scene.Node node : weatherOverlay.getChildren()) {
            if (node instanceof javafx.scene.shape.Line) {
                javafx.scene.shape.Line drop = (javafx.scene.shape.Line) node;
                double speed = (double) drop.getUserData();
                drop.setLayoutY(drop.getLayoutY() + speed);
                drop.setLayoutX(drop.getLayoutX() - speed * 0.3); // Mưa bay xiên do gió
                if (drop.getLayoutY() > h + 50) {
                    toRemove.add(drop);
                }
            }
        }
        weatherOverlay.getChildren().removeAll(toRemove);
    }

    private void updatePlayerPosition() {
        double dx = 0;
        double dy = 0;
        isWalking = false;

        if (activeKeys.contains(KeyCode.W)) { dy -= speed; facing = "UP"; isWalking = true; }
        if (activeKeys.contains(KeyCode.S)) { dy += speed; facing = "DOWN"; isWalking = true; }
        if (activeKeys.contains(KeyCode.A)) { dx -= speed; facing = "LEFT"; isWalking = true; }
        if (activeKeys.contains(KeyCode.D)) { dx += speed; facing = "RIGHT"; isWalking = true; }

        double nextX = playerX + dx;
        double nextY = playerY + dy;

        // Giới hạn không cho nhân vật đi ra ngoài rìa bản đồ
        double mapW = farmMap.getWidth() * Constant.CELL_SIZE;
        double mapH = farmMap.getHeight() * Constant.CELL_SIZE;
        
        if (nextX < 0) nextX = 0;
        if (nextY < 0) nextY = 0;
        if (nextX > mapW - 40) nextX = mapW - 40;
        if (nextY > mapH - 40) nextY = mapH - 40;

        int colX = (int) ((nextX + 20) / Constant.CELL_SIZE);
        int rowX = (int) ((playerY + 20) / Constant.CELL_SIZE);
        boolean waterX = farmMap.getCell(rowX, colX) != null && farmMap.getCell(rowX, colX).getTerrain() == TerrainType.WATER;
        
        int colY = (int) ((playerX + 20) / Constant.CELL_SIZE);
        int rowY = (int) ((nextY + 20) / Constant.CELL_SIZE);
        boolean waterY = farmMap.getCell(rowY, colY) != null && farmMap.getCell(rowY, colY).getTerrain() == TerrainType.WATER;

        // TMX Object Collision
        javafx.geometry.Rectangle2D oldBox = new javafx.geometry.Rectangle2D(playerX + 10, playerY + 20, 20, 20);
        javafx.geometry.Rectangle2D playerBoxX = new javafx.geometry.Rectangle2D(nextX + 10, playerY + 20, 20, 20);
        javafx.geometry.Rectangle2D playerBoxY = new javafx.geometry.Rectangle2D(playerX + 10, nextY + 20, 20, 20);
        
        boolean colX_obj = false;
        boolean colY_obj = false;
        boolean onBridgeX = false;
        boolean onBridgeY = false;
        
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
                    double railingWidth = 30;
                    javafx.geometry.Rectangle2D leftRailing = new javafx.geometry.Rectangle2D(ox, oy + 12, railingWidth, Math.max(10, oh - 24));
                    javafx.geometry.Rectangle2D rightRailing = new javafx.geometry.Rectangle2D(ox + ow - railingWidth, oy + 12, railingWidth, Math.max(10, oh - 24));
                    // Mở rộng sàn cầu lên/xuống thêm 30px để bước lên/xuống hai đầu cầu mượt mà, không bị kẹt mép nước
                    javafx.geometry.Rectangle2D walkableDeck = new javafx.geometry.Rectangle2D(ox + railingWidth, oy - 30, ow - 2 * railingWidth, oh + 60);

                    if (playerBoxX.intersects(walkableDeck)) onBridgeX = true;
                    if (playerBoxY.intersects(walkableDeck)) onBridgeY = true;

                    if (playerBoxX.intersects(leftRailing) && !oldBox.intersects(leftRailing)) colX_obj = true;
                    if (playerBoxY.intersects(leftRailing) && !oldBox.intersects(leftRailing)) colY_obj = true;

                    if (playerBoxX.intersects(rightRailing) && !oldBox.intersects(rightRailing)) colX_obj = true;
                    if (playerBoxY.intersects(rightRailing) && !oldBox.intersects(rightRailing)) colY_obj = true;
                    
                    continue;
                }
                if (idLower.isEmpty() || idLower.contains("flag") || idLower.contains("crop") || idLower.contains("bush") ||
                    idLower.contains("pumpkin") || idLower.contains("dirt") || idLower.contains("path") ||
                    idLower.contains("terrain") || idLower.contains("wheat") || idLower.contains("construction") ||
                    idLower.contains("farm_rake") || idLower.contains("farm_hoe") || idLower.contains("farm_shovel") ||
                    idLower.contains("flower") || idLower.contains("pebble") || idLower.contains("broken_") ||
                    idLower.contains("wood_sticks") || idLower.contains("clay_pot") || idLower.contains("grass_patch") ||
                    idLower.contains("windmill_blades")) continue;
            }

            // Tự động điều chỉnh hitbox siêu khít (Tight hitbox) để xóa "block tàng hình"
            double hw, hh, cx, cy;
            if (objId != null && (objId.toLowerCase().contains("house") || objId.toLowerCase().contains("barn") || objId.toLowerCase().contains("windmill"))) {
                // Những công trình lớn: hitbox thu gọn xuống phần móng (đáy 25%)
                hw = ow * 0.8;
                hh = oh * 0.25;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 5;
            } else if (objId != null && objId.toLowerCase().contains("tent")) {
                // Lều trại: hitbox bao phủ phần thân lều để chặn nhân vật đi xuyên qua
                hw = ow * 0.85;
                hh = oh * 0.72;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else if (objId != null && objId.toLowerCase().contains("campfire")) {
                // Lửa trại: hitbox bao phủ vòng đá lửa trại
                hw = ow * 0.8;
                hh = oh * 0.65;
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 2;
            } else {
                // Các vật thể nhỏ (cây, đá, bụi rậm, gốc cây): hitbox siêu nhỏ ở chính giữa gốc
                hw = Math.min(30, ow * 0.4);
                hh = Math.min(20, oh * 0.2);
                cx = ox + (ow - hw) / 2;
                cy = oy + oh - hh - 5;
            }
            
            javafx.geometry.Rectangle2D objBox = new javafx.geometry.Rectangle2D(cx, cy, hw, hh);
            
            if (playerBoxX.intersects(objBox) && !oldBox.intersects(objBox)) colX_obj = true;
            if (playerBoxY.intersects(objBox) && !oldBox.intersects(objBox)) colY_obj = true;
            
            if (colX_obj && colY_obj) break;
        }
        
        if (waterX && !onBridgeX) nextX = playerX;
        if (waterY && !onBridgeY) nextY = playerY;
        
        if (colX_obj) nextX = playerX;
        if (colY_obj) nextY = playerY;

        playerX = nextX;
        playerY = nextY;

        // Xử lý Flip (Quay mặt)
        if (facing.equals("LEFT")) {
            playerCharacter.setScaleX(-1);
            if (weaponView != null) weaponView.setScaleX(-1);
        } else if (facing.equals("RIGHT")) {
            playerCharacter.setScaleX(1);
            if (weaponView != null) weaponView.setScaleX(1);
        }

        // Wobble Animation (nhún nhảy khi đi)
        double bobbing = 0;
        if (isWalking) {
            lastMoveTime++;
            bobbing = Math.sin(lastMoveTime * 0.4) * 4.0; // Nhún lên xuống 4 pixel
            
            // Xoay tay chân (Swing)
            double swingAngle = Math.sin(lastMoveTime * 0.6) * 30; // Góc xoay tối đa 30 độ
            if (leftArmSwing != null) {
                leftArmSwing.setAngle(swingAngle);
                rightArmSwing.setAngle(-swingAngle);
                leftLegSwing.setAngle(-swingAngle);
                rightLegSwing.setAngle(swingAngle);
            }
        } else {
            if (leftArmSwing != null) {
                leftArmSwing.setAngle(0);
                rightArmSwing.setAngle(0);
                leftLegSwing.setAngle(0);
                rightLegSwing.setAngle(0);
            }
        }

        // Cập nhật vị trí nhân vật trên bản đồ (Dùng setTranslate để tối ưu FPS, không làm lag 600 ô đất)
        playerCharacter.setTranslateX(playerX);
        playerCharacter.setTranslateY(playerY + bobbing);

        // Weapon Position & Animation
        if (weaponView != null) {
            double weaponBaseX = facing.equals("LEFT") ? playerX - 10 : playerX + 30;
            double weaponBaseY = playerY + 25 + bobbing; // Cầm ở ngang tay
            
            // Swing Animation (Cuốc đất)
            double weaponRotation = 0;
            if (System.currentTimeMillis() - actionTime < 250) {
                // Đang vung vũ khí (kéo dài 250ms)
                double progress = (System.currentTimeMillis() - actionTime) / 250.0;
                
                // Hiệu ứng chém: Giơ lên (-60 độ) rồi bổ mạnh xuống (90 độ)
                double angle;
                if (progress < 0.3) {
                    angle = -60 * (progress / 0.3); // Giơ cuốc lên
                } else {
                    angle = -60 + 150 * ((progress - 0.3) / 0.7); // Bổ xuống
                }
                
                weaponRotation = facing.equals("LEFT") ? -angle : angle;
                // Hơi nhích vũ khí tới trước một chút khi bổ
                weaponBaseX += (facing.equals("LEFT") ? -10 : 10) * Math.sin(progress * Math.PI);
            } else {
                // Bình thường giữ vũ khí chếch lên
                weaponRotation = facing.equals("LEFT") ? -20 : 20;
            }

            weaponView.setTranslateX(weaponBaseX);
            weaponView.setTranslateY(weaponBaseY);
            
            // Dùng hàm xoay mặc định (tâm ở giữa vũ khí) vì Transform phức tạp hơn
            weaponView.setRotate(weaponRotation);
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
        
        // Cập nhật vị trí ô đang hướng tới (Highlight) để dễ thao tác
        Cell fCell = getFacingCell();
        if (fCell != null && facingHighlight != null && !isCellBlockedByStructure(fCell)) {
            facingHighlight.setVisible(true);
            facingHighlight.setTranslateX(fCell.getX() * Constant.CELL_SIZE);
            facingHighlight.setTranslateY(fCell.getY() * Constant.CELL_SIZE);
        } else if (facingHighlight != null) {
            facingHighlight.setVisible(false);
        }
        
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
        playerCharacter.setUserData(playerY + 50.0);
        if (weaponView != null) {
            weaponView.setUserData(playerY + 51.0);
        }
        
        // Sắp xếp động toàn bộ vật thể (Depth Sorting)
        if (entityPane != null) {
            javafx.collections.FXCollections.sort(entityPane.getChildren(), (n1, n2) -> {
                double y1 = n1.getUserData() instanceof Double ? (Double)n1.getUserData() : 0.0;
                double y2 = n2.getUserData() instanceof Double ? (Double)n2.getUserData() : 0.0;
                return Double.compare(y1, y2);
            });
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
        
        // Xác định AnimalZone trước để mọi con vật tạo ra đều nằm gọn bên trong hàng rào AnimalZone
        if (tmxMap != null && tmxMap.objects != null) {
            double zs = (tmxMap.tileWidth > 0) ? (double) Constant.CELL_SIZE / tmxMap.tileWidth : 1.0;
            for (view.screen_util.TiledMap.TiledObject obj : tmxMap.objects) {
                String objName = obj.name != null ? obj.name.toLowerCase() : "";
                String type = obj.type != null ? obj.type.toLowerCase() : "";
                if (("zone".equals(type) || objName.endsWith("zone")) && objName.equals("animalzone")) {
                    // Giới hạn chặt vào bên trong hàng rào chuồng thú (bên phải rào dọc x=985, dưới rào ngang y=948, trên bờ sông y=1253)
                    double zx = Math.max(obj.x, 985) * zs;
                    double zy = Math.max(obj.y, 948) * zs;
                    double maxZx = Math.min(obj.x + obj.width, 1540) * zs;
                    double maxZy = Math.min(obj.y + obj.height, 1253) * zs;
                    animalZone = new javafx.geometry.Rectangle2D(zx, zy, Math.max(200, maxZx - zx), Math.max(180, maxZy - zy));
                    break;
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
        buildTroughs();

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
            // --- Đầu và Mũ rơm ---
            javafx.scene.Group headGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle face = new javafx.scene.shape.Rectangle(20, 20, Color.PEACHPUFF);
            face.setX(12); face.setY(2);
            javafx.scene.shape.Rectangle eyeL = new javafx.scene.shape.Rectangle(3, 4, Color.BLACK);
            eyeL.setX(15); eyeL.setY(10);
            javafx.scene.shape.Rectangle eyeR = new javafx.scene.shape.Rectangle(3, 4, Color.BLACK);
            eyeR.setX(26); eyeR.setY(10);
            javafx.scene.shape.Rectangle hair = new javafx.scene.shape.Rectangle(22, 8, Color.rgb(30, 30, 30));
            hair.setX(11); hair.setY(0);
            javafx.scene.shape.Rectangle hatBrim = new javafx.scene.shape.Rectangle(34, 4, Color.GOLDENROD);
            hatBrim.setX(5); hatBrim.setY(-2);
            javafx.scene.shape.Rectangle hatBand = new javafx.scene.shape.Rectangle(20, 4, Color.CRIMSON);
            hatBand.setX(12); hatBand.setY(-6);
            javafx.scene.shape.Rectangle hatTop = new javafx.scene.shape.Rectangle(18, 6, Color.GOLDENROD);
            hatTop.setX(13); hatTop.setY(-12);
            headGroup.getChildren().addAll(face, hair, eyeL, eyeR, hatTop, hatBand, hatBrim);

            // --- Thân (Ngực trần + Áo khoác đỏ + Thắt lưng vàng) ---
            javafx.scene.Group bodyGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle chest = new javafx.scene.shape.Rectangle(24, 24, Color.PEACHPUFF);
            chest.setX(10); chest.setY(20);
            javafx.scene.shape.Line scar1 = new javafx.scene.shape.Line(16, 26, 28, 38);
            scar1.setStroke(Color.SADDLEBROWN); scar1.setStrokeWidth(2);
            javafx.scene.shape.Line scar2 = new javafx.scene.shape.Line(28, 26, 16, 38);
            scar2.setStroke(Color.SADDLEBROWN); scar2.setStrokeWidth(2);
            javafx.scene.shape.Rectangle jacketL = new javafx.scene.shape.Rectangle(6, 22, Color.CRIMSON);
            jacketL.setX(10); jacketL.setY(20);
            javafx.scene.shape.Rectangle jacketR = new javafx.scene.shape.Rectangle(6, 22, Color.CRIMSON);
            jacketR.setX(28); jacketR.setY(20);
            javafx.scene.shape.Rectangle sash = new javafx.scene.shape.Rectangle(26, 6, Color.GOLD);
            sash.setX(9); sash.setY(40);
            bodyGroup.getChildren().addAll(chest, scar1, scar2, jacketL, jacketR, sash);

            // --- Tay (Tay áo đỏ, bàn tay màu da) ---
            javafx.scene.Group leftArmGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle lSleeve = new javafx.scene.shape.Rectangle(8, 10, Color.CRIMSON);
            lSleeve.setX(2); lSleeve.setY(22);
            javafx.scene.shape.Rectangle lHand = new javafx.scene.shape.Rectangle(6, 8, Color.PEACHPUFF);
            lHand.setX(3); lHand.setY(32);
            leftArmGroup.getChildren().addAll(lSleeve, lHand);
            leftArmSwing = new javafx.scene.transform.Rotate(0, 6, 22);
            leftArmGroup.getTransforms().add(leftArmSwing);

            javafx.scene.Group rightArmGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle rSleeve = new javafx.scene.shape.Rectangle(8, 10, Color.CRIMSON);
            rSleeve.setX(34); rSleeve.setY(22);
            javafx.scene.shape.Rectangle rHand = new javafx.scene.shape.Rectangle(6, 8, Color.PEACHPUFF);
            rHand.setX(35); rHand.setY(32);
            rightArmGroup.getChildren().addAll(rSleeve, rHand);
            rightArmSwing = new javafx.scene.transform.Rotate(0, 38, 22);
            rightArmGroup.getTransforms().add(rightArmSwing);

            // --- Chân (Quần short xanh dương, ống cuộn trắng, chân màu da, dép đen) ---
            javafx.scene.Group leftLegGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle lShorts = new javafx.scene.shape.Rectangle(10, 10, Color.ROYALBLUE);
            lShorts.setX(10); lShorts.setY(46);
            javafx.scene.shape.Rectangle lCuff = new javafx.scene.shape.Rectangle(12, 4, Color.WHITE);
            lCuff.setX(9); lCuff.setY(54);
            javafx.scene.shape.Rectangle lCalf = new javafx.scene.shape.Rectangle(6, 8, Color.PEACHPUFF);
            lCalf.setX(12); lCalf.setY(58);
            javafx.scene.shape.Rectangle lSandal = new javafx.scene.shape.Rectangle(8, 2, Color.BLACK);
            lSandal.setX(11); lSandal.setY(66);
            leftLegGroup.getChildren().addAll(lShorts, lCuff, lCalf, lSandal);
            leftLegSwing = new javafx.scene.transform.Rotate(0, 15, 46);
            leftLegGroup.getTransforms().add(leftLegSwing);

            javafx.scene.Group rightLegGroup = new javafx.scene.Group();
            javafx.scene.shape.Rectangle rShorts = new javafx.scene.shape.Rectangle(10, 10, Color.ROYALBLUE);
            rShorts.setX(24); rShorts.setY(46);
            javafx.scene.shape.Rectangle rCuff = new javafx.scene.shape.Rectangle(12, 4, Color.WHITE);
            rCuff.setX(23); rCuff.setY(54);
            javafx.scene.shape.Rectangle rCalf = new javafx.scene.shape.Rectangle(6, 8, Color.PEACHPUFF);
            rCalf.setX(26); rCalf.setY(58);
            javafx.scene.shape.Rectangle rSandal = new javafx.scene.shape.Rectangle(8, 2, Color.BLACK);
            rSandal.setX(25); rSandal.setY(66);
            rightLegGroup.getChildren().addAll(rShorts, rCuff, rCalf, rSandal);
            rightLegSwing = new javafx.scene.transform.Rotate(0, 29, 46);
            rightLegGroup.getTransforms().add(rightLegSwing);

            playerCharacter.getChildren().addAll(leftArmGroup, leftLegGroup, rightLegGroup, bodyGroup, headGroup, rightArmGroup);
            playerCharacter.setManaged(false);
            if (entityPane != null) entityPane.getChildren().add(playerCharacter);
        }
        playerCharacter.setTranslateX(playerX);
        playerCharacter.setTranslateY(playerY);

        if (weaponView == null) {
            weaponView = new ImageView();
            weaponView.setFitWidth(30);
            weaponView.setFitHeight(30);
            weaponView.setPreserveRatio(true);
            weaponView.setManaged(false);
            if (entityPane != null) entityPane.getChildren().add(weaponView);
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
        // Reset thời gian ngày về ban đầu (0ms - Sáng sớm 06:00)
        elapsedTimeInDay = 0;
        resetFeedingSchedule(0.0);
        lastFrameTime = System.currentTimeMillis();
        lastDisplayedMinute = -1;
        for (Animal a : animals) a.eatTrough = null;
        if (timeOverlayRect != null) {
            timeOverlayRect.setFill(getTimeOverlayColor(0.0));
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
        refreshAll();
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
            else if (cur instanceof Hand) weaponView.setImage(Assets.HAND_ICON);
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
        refreshAll();
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

        boolean ok = playerController.interactWithCell(cell);
        notify(ok ? "" : "Can't use this tool here");
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
        if (router.hasOverlay()) {
            router.closeOverlay();
        } else {
            router.showOverlay(router.getScreen(Screens.PAUSE));
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

    // ======================== rendering (Giữ nguyên) ========================
    private void refreshAll() {
        if (tiles != null) {
            for (Tile[] row : tiles) {
                for (Tile t : row) t.style();
            }
        }
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
        final String cls;
        switch (type) {
            case RAINY:   cls = "weather-rainy";   break;
            case DROUGHT: cls = "weather-drought"; break;
            case SUNNY:
            default:      cls = "weather-sunny";   break;
        }
        setWeatherClass(rootPane, cls);
        setWeatherClass(weatherOverlay, cls);
    }

    private static void setWeatherClass(Region node, String cls) {
        if (node == null) return;
        node.getStyleClass().removeAll("weather-sunny", "weather-rainy", "weather-drought");
        if (!node.getStyleClass().contains(cls)) {
            node.getStyleClass().add(cls);
        }
    }

    private void updateHover(Cell cell) {
        hovered = cell;
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

    // ======================== cell tile (Giữ nguyên) ========================
private final class Tile {
        final Cell cell;
        final StackPane pane = new StackPane();
        final ImageView backgroundView = new ImageView(); // THÊM DÒNG NÀY: Khai báo lớp ảnh nền
        final ImageView pestMark = Assets.imageView(Assets.BEETLE_ICON, 20);
        final javafx.scene.shape.Rectangle dirtOverlay = new javafx.scene.shape.Rectangle(Constant.CELL_SIZE + 1.5, Constant.CELL_SIZE + 1.5, Color.rgb(0, 0, 0, 0.2));
        final ImageView cropView = new ImageView();
        final ProgressBar progressBar = new ProgressBar();

        Tile(final Cell cell) {
            this.cell = cell;
            
            // Cài đặt kích thước cho ảnh nền lấp đầy ô đất, cộng thêm 1 pixel để đè lên viền/rãnh gạch (grid lines)
            backgroundView.setFitWidth(Constant.CELL_SIZE + 1.5);
            backgroundView.setFitHeight(Constant.CELL_SIZE + 1.5);
            backgroundView.setSmooth(false); // Fix: Tắt chế độ làm mờ để pixel art luôn sắc nét

            cropView.setFitWidth(54);
            cropView.setFitHeight(54);
            cropView.setSmooth(false); // Fix: Tắt chế độ làm mờ để pixel art sắc nét
            // ... (các cài đặt cropView, progressBar giữ nguyên)

            pane.setMinSize(Constant.CELL_SIZE, Constant.CELL_SIZE);
            pane.setPrefSize(Constant.CELL_SIZE, Constant.CELL_SIZE);
            pane.setCursor(Cursor.HAND);
            
            // QUAN TRỌNG: Thêm backgroundView vào đầu tiên để nó nằm dưới cùng (làm nền)
            dirtOverlay.setVisible(false);
            pane.getChildren().addAll(backgroundView, dirtOverlay, cropView, progressBar, pestMark);

            pane.setOnMouseClicked(e -> onCellClick(cell));
            pane.setOnMouseEntered(e -> updateHover(cell));

            style();
        }
// ...

void style() {
            // 1. Dán ảnh đồ họa Pixel từ TMX Map thay vì dùng mã màu Hex
            backgroundView.setEffect(null);
            pane.setStyle("");

            int gid = 0;
            if (tmxMap != null && !tmxMap.layers.isEmpty()) {
                gid = tmxMap.layers.get(0).data[cell.getY()][cell.getX()];
            }
            
            // Nếu ô đất đã được xới hoặc được định nghĩa là Đất Trồng, luôn luôn hiển thị là Đất
            if (cell.getTerrain() == TerrainType.DIRT) {
                gid = 3; 
            }

            if (gid > 0 && tmxMap != null) {
                javafx.scene.image.Image img = tmxMap.getTileImage(gid);
                if (img != null) {
                    backgroundView.setImage(img);
                } else {
                    backgroundView.setImage(null);
                }
            } else {
                backgroundView.setImage(null);
            }

            // Xử lý khi ô đất bị xới (TILLED)
            if (cell.getCellState() == CellState.TILLED) {
                // Đất đã cuốc: Trông đậm hơn (Bật lớp phủ đen thay vì dùng ColorAdjust gây lag fps)
                double darkness = 0.2 + (cell.getMoistureLevel() / 200.0); // Tối thiểu giảm 20% độ sáng
                dirtOverlay.setFill(Color.rgb(0, 0, 0, Math.min(1.0, darkness)));
                dirtOverlay.setVisible(true);
                
                // Thu nhỏ tile 5% để tạo khe hở (cảm giác đất bị xới nứt nẻ)
                backgroundView.setScaleX(0.95);
                backgroundView.setScaleY(0.95);
                dirtOverlay.setScaleX(0.95);
                dirtOverlay.setScaleY(0.95);
            } else {
                dirtOverlay.setVisible(false);
                backgroundView.setScaleX(1.0);
                backgroundView.setScaleY(1.0);
            }

            // 2. Viền chọn ô đất (Highlight)
            String border = "transparent";
            double bw = 0;
            Crop crop = cell.getCrop();
            
            if (crop != null && crop.getCurrentState() == GrowthState.HARVEST) {
                border = "#F6D46F"; bw = 3;
            } else if (crop != null && (crop.getCurrentState() == GrowthState.DEAD
                    || crop.getCurrentState() == GrowthState.ROTTEN)) {
                border = "#B23A2A"; bw = 2.5;
            }

            pane.setBorder(new Border(
                    new BorderStroke(Color.web(border), BorderStrokeStyle.SOLID,
                            TILE_RADII, new BorderWidths(bw))));

            // 3. Hiển thị cây trồng
            if (crop == null) {
                cropView.setImage(null);
                progressBar.setVisible(false);
            } else {
                cropView.setImage(Assets.getCropSprite(crop));
                progressBar.setVisible(
                        crop.getCurrentState() != GrowthState.HARVEST
                                && crop.getCurrentState() != GrowthState.DEAD
                                && crop.getCurrentState() != GrowthState.ROTTEN
                );
                progressBar.setProgress(crop.getCurrentGrowthProgress() / 100.0);
            }
            
            pestMark.setVisible(cell.getPest() != null);
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
