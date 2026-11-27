package view.screen_controller;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import view.fx.SoundHook;
import view.screen_util.GameLauncher;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Màn hình Bắt đầu 2D Pixel Art sinh động, ấm áp (lấy cảm hứng Farm Together):
 * - Nền toàn cảnh nông trại 2D Pixel Art phong phú: mây pixel bay (opacity 0.6), cối xay gió xoay 6 frame,
 *   khói ống khói nhà chính, giếng đá mái gỗ, luống lúa mì/bí ngô/cà chua có rào, cột đèn, lá rơi & đom đóm/bướm.
 * - Bước 1: Màn hình tiêu đề Logo 3D "SMART FARM" ("Nhấn Enter hoặc Click để Bắt đầu").
 * - Bước 2: Bảng menu gỗ pixel 7 nút bên trái có đinh tán 4 góc, lá xanh + quả đỏ, hover scale 1.05 + mầm lá + SFX pop,
 *   click nhún +2px; Hồ sơ Người chơi / Luffy góc trên phải; Bảng Sự kiện "7d" & Bảng Chào mừng cuộn mượt bên phải.
 * - Bước 3: Hộp thoại Tạo tên nông trại mới + các modal Tìm nông trại / Tùy chỉnh / Cài đặt / Thêm.
 * - Nhạc nền 8-bit thư giãn & hiệu ứng chuyển cảnh FadeOut 0.8s kèm âm thanh mở cổng gỗ.
 */
public final class MainMenuController {

    private static final double W = 1280.0;
    private static final double H = 720.0;
    private static final String FONT_PIXEL = "'Consolas', 'Segoe UI', monospace";
    private static final String FONT_UI    = "'Segoe UI', 'Tahoma', sans-serif";

    @FXML private StackPane rootStack;
    @FXML private Pane sceneryLayer;
    @FXML private AnchorPane dashboardLayer;
    @FXML private StackPane splashLayer;
    @FXML private StackPane modalOverlayLayer;
    @FXML private Rectangle transitionCurtain;

    private SceneRouter router;
    private GameLauncher launcher;

    // Nút chức năng & phần tử cập nhật động
    private Button btnCreateFarm;
    private Button btnMyFarm;
    private Label profileNameLabel;
    private Label profileLevelLabel;
    private Polygon profileStarShape;
    private Label welcomeFarmStatusLabel;
    private Label overwriteWarnLabel;
    private TextField farmNameInput;
    private StackPane createFarmModalCard;

    // Thành phần hỗ trợ co giãn mượt theo mọi độ phân giải cửa sổ (1280x720 -> Full HD / Maximized)
    private Pane sceneryCanvas;
    private javafx.scene.transform.Scale sceneryBgScale;
    private Rectangle sceneryClip;
    private Node dashProfileNode;
    private Node dashLeftMenuNode;
    private Node dashRightPanelNode;
    private Node splashLogoNode;

    private boolean continueEnabled = false;
    private boolean splashDismissed = false;
    private boolean transitionRunning = false;
    private String savedFarmName = "Nông Trại Vui Vẻ";
    private String farmerDisplayName = "LUFFY";
    private int savedDay = 1;
    private int savedMoney = 200;

    // Hoạt ảnh môi trường sống động
    private final Random rng = new Random(2026L);
    private final List<CloudActor> clouds = new ArrayList<>();
    private final List<LeafActor> fallingLeaves = new ArrayList<>();
    private final List<FireflyActor> fireflies = new ArrayList<>();
    private final List<SmokePuff> chimneySmokes = new ArrayList<>();
    private final List<Rectangle> pondGlints = new ArrayList<>();
    private ImageView windmillBladesView;
    private Image[] windmillFrames;
    private int windmillFrameIdx = 0;
    private double windmillTimer = 0.0;
    private double smokeSpawnTimer = 0.0;
    private Circle campfireGlow;
    private Pane dynamicFxPane;
    private AnimationTimer backdropTimer;
    private long lastNanos = 0L;
    private double elapsedSec = 0.0;

    private static final class CloudActor {
        final Node node;
        double speed;
        double baseY;
        final double width;

        CloudActor(Node node, double speed, double baseY, double width) {
            this.node = node;
            this.speed = speed;
            this.baseY = baseY;
            this.width = width;
        }
    }

    private static final class LeafActor {
        final Rectangle node;
        double x, y, vx, vy, swayPhase, swayAmp, rotSpeed;

        LeafActor(Rectangle node, double x, double y, double vx, double vy, double swayPhase, double swayAmp, double rotSpeed) {
            this.node = node;
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.swayPhase = swayPhase; this.swayAmp = swayAmp; this.rotSpeed = rotSpeed;
        }
    }

    private static final class FireflyActor {
        final Group node;
        final double anchorX, anchorY, ampX, ampY, freqX, freqY, phase;

        FireflyActor(Group node, double anchorX, double anchorY, double ampX, double ampY,
                     double freqX, double freqY, double phase) {
            this.node = node;
            this.anchorX = anchorX; this.anchorY = anchorY;
            this.ampX = ampX; this.ampY = ampY;
            this.freqX = freqX; this.freqY = freqY; this.phase = phase;
        }
    }

    private static final class SmokePuff {
        final Rectangle node;
        double x, y, vx, vy, age, maxAge;

        SmokePuff(Rectangle node, double x, double y, double vx, double vy, double maxAge) {
            this.node = node;
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.age = 0.0; this.maxAge = maxAge;
        }
    }

    @FXML
    private void initialize() {
        rootStack.setFocusTraversable(true);

        // Khung nền 1280x720 đặt bên trong sceneryLayer + scale theo gốc (0,0) để phủ kín 100% không bị lệch khi mây/lá trôi
        sceneryCanvas = new Pane();
        sceneryCanvas.setPrefSize(W, H);
        sceneryCanvas.setMinSize(W, H);
        sceneryCanvas.setMaxSize(W, H);
        sceneryBgScale = new javafx.scene.transform.Scale(1.0, 1.0, 0.0, 0.0);
        sceneryCanvas.getTransforms().add(sceneryBgScale);
        sceneryLayer.getChildren().setAll(sceneryCanvas);

        sceneryClip = new Rectangle(W, H);
        sceneryClip.widthProperty().bind(rootStack.widthProperty());
        sceneryClip.heightProperty().bind(rootStack.heightProperty());
        sceneryLayer.setClip(sceneryClip);

        transitionCurtain.widthProperty().bind(rootStack.widthProperty());
        transitionCurtain.heightProperty().bind(rootStack.heightProperty());

        populateScenicFarmBackdrop(sceneryCanvas);
        populateTitleSplashLayer(splashLayer);
        populateDashboardLayer(dashboardLayer);

        rootStack.widthProperty().addListener((obs, ov, nv) -> updateResponsiveLayout());
        rootStack.heightProperty().addListener((obs, ov, nv) -> updateResponsiveLayout());
        Platform.runLater(this::updateResponsiveLayout);

        modalOverlayLayer.setAlignment(Pos.CENTER);
        modalOverlayLayer.setOnMouseClicked(e -> {
            if (e.getTarget() == modalOverlayLayer) closeModal();
        });
        createFarmModalCard = buildCreateFarmModalCard();

        rootStack.setOnKeyPressed(e -> {
            if (transitionRunning) {
                e.consume();
                return;
            }
            if (!splashDismissed && (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE)) {
                e.consume();
                enterDashboardFromSplash();
            } else if (modalOverlayLayer.isVisible() && e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                closeModal();
            }
        });

        // Tự động phát nhạc nền 8-bit khi màn hình chính xuất hiện và tắt khi rời đi
        rootStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(rootStack::requestFocus);
                if (rootStack.getParent() != null) {
                    SoundHook.startMenuBgm();
                }
            }
        });
        rootStack.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null && rootStack.getScene() != null) {
                transitionRunning = false;
                transitionCurtain.setVisible(false);
                transitionCurtain.setOpacity(0.0);
                SoundHook.startMenuBgm();
                Platform.runLater(rootStack::requestFocus);
            } else if (newParent == null) {
                SoundHook.stopMenuBgm();
            }
        });

        startBackdropAnimation();
    }

    public void init(SceneRouter router, GameLauncher launcher) {
        this.router = router;
        this.launcher = launcher;
    }

    public void setContinueEnabled(boolean enabled) {
        this.continueEnabled = enabled;
        refreshMainButtons();
    }

    public void updateFarmPreview(boolean hasSave, String farmName, int day, int money) {
        this.continueEnabled = hasSave;
        this.savedFarmName = (farmName != null && !farmName.trim().isEmpty()) ? farmName.trim() : "Nông Trại Vui Vẻ";
        this.savedDay = Math.max(1, day);
        this.savedMoney = Math.max(0, money);

        refreshMainButtons();
        refreshProfileHeader();

        if (welcomeFarmStatusLabel != null) {
            if (hasSave) {
                welcomeFarmStatusLabel.setText(
                    "▶ Nông trại đang lưu: " + savedFarmName + "\n" +
                    "▶ Tiến độ: Ngày " + savedDay + "  •  Tài sản: $" + savedMoney
                );
            } else {
                welcomeFarmStatusLabel.setText(
                    "▶ Chưa có nông trại nào được lưu.\n" +
                    "▶ Bấm [Tạo nông trại] để đặt tên và bắt đầu ngay!"
                );
            }
        }
        if (overwriteWarnLabel != null) {
            overwriteWarnLabel.setVisible(hasSave);
            overwriteWarnLabel.setManaged(hasSave);
        }
    }

    private void refreshProfileHeader() {
        if (profileNameLabel != null) {
            profileNameLabel.setText("NGƯỜI CHƠI / " + farmerDisplayName.toUpperCase());
        }
        if (profileLevelLabel != null) {
            int lv = Math.max(1, continueEnabled ? (savedDay + 3) : 4);
            profileLevelLabel.setText(Integer.toString(lv));
        }
    }

    /** Chuyển thẳng vào Bước 2 (Dashboard) khi người chơi quay về Main Menu từ trong game. */
    public void showDashboardDirectly() {
        splashDismissed = true;
        transitionRunning = false;
        transitionCurtain.setVisible(false);
        transitionCurtain.setOpacity(0.0);
        splashLayer.setVisible(false);
        splashLayer.setOpacity(0.0);
        modalOverlayLayer.setVisible(false);
        modalOverlayLayer.setOpacity(0.0);
        dashboardLayer.setVisible(true);
        dashboardLayer.setOpacity(1.0);
        SoundHook.startMenuBgm();
        Platform.runLater(rootStack::requestFocus);
    }

    // ==================== 1. HÌNH NỀN TOÀN CẢNH NÔNG TRẠI 2D PIXEL ART ĐỘNG ====================

    private void populateScenicFarmBackdrop(Pane bg) {
        bg.getChildren().clear();

        // 1. Bầu trời xanh ngọc có nắng nhẹ
        Rectangle sky = new Rectangle(W, H * 0.56);
        sky.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#0A84A4")),
            new Stop(0.46, Color.web("#2EC3D8")),
            new Stop(0.82, Color.web("#82E7E0")),
            new Stop(1.0, Color.web("#CCF8EE"))
        ));
        bg.getChildren().add(sky);

        // Mặt trời pixel ấm áp ở góc trên trái
        Circle sunHalo = new Circle(150, 82, 88);
        sunHalo.setFill(new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(255, 248, 178, 0.45)),
            new Stop(1.0, Color.TRANSPARENT)
        ));
        Rectangle sunCore = new Rectangle(132, 64, 36, 36);
        sunCore.setArcWidth(6); sunCore.setArcHeight(6);
        sunCore.setFill(Color.web("#FFF3A6", 0.85));
        bg.getChildren().addAll(sunHalo, sunCore);

        // 4 Đám mây pixel (opacity 0.6) trôi chậm rãi từ trái sang phải
        addPixelCloud(bg, 85, 58, 1.15, 13.0);
        addPixelCloud(bg, 430, 96, 1.00, 9.5);
        addPixelCloud(bg, 790, 48, 1.24, 15.0);
        addPixelCloud(bg, 1100, 146, 0.92, 11.0);

        // Dãy núi sương mờ phía xa
        Polygon farMountLeft = new Polygon(
            -40, 410, 140, 260, 270, 310, 420, 232, 590, 360, 660, 415
        );
        farMountLeft.setFill(Color.web("#74B2A5", 0.55));

        Polygon farMountRight = new Polygon(
            680, 405, 860, 215, 940, 148, 1015, 185, 1090, 138, 1230, 275, 1300, 340, 1300, 420
        );
        farMountRight.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#8DBDB3", 0.82)),
            new Stop(1.0, Color.web("#66A188", 0.88))
        ));
        bg.getChildren().addAll(farMountLeft, farMountRight);

        // Các tầng đồi cỏ xanh tươi
        Path farHill = makeHillPath(0, 352, 340, 314, 760, 360, 1280, 326, H);
        farHill.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#4D9C38")), new Stop(1.0, Color.web("#3C802C"))));

        Path midMeadow = makeHillPath(0, 394, 420, 362, 860, 400, 1280, 374, H);
        midMeadow.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#67B944")), new Stop(0.55, Color.web("#58A638")), new Stop(1.0, Color.web("#448B2D"))));

        Path frontGrass = makeHillPath(0, 480, 460, 446, 920, 486, 1280, 466, H);
        frontGrass.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#76C84E")), new Stop(1.0, Color.web("#529E36"))));
        bg.getChildren().addAll(farHill, midMeadow, frontGrass);

        // Cụm cỏ pixel & hoa dại trải khắp đồng cỏ tạo độ sâu 2D Pixel Art
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_md.png", 390, 404, 44, 24);
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_sm.png", 475, 456, 36, 20);
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_md.png", 618, 412, 44, 24);
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_sm.png", 802, 470, 36, 20);
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_lg.png", 365, 622, 54, 28);
        placeSprite(bg, "/view/assets/tiled/objects/grass_patch_md.png", 688, 636, 48, 26);

        // Hồ nước xanh ngọc góc phải dưới
        Ellipse pondRim = new Ellipse(1170, 650, 215, 118);
        pondRim.setFill(Color.web("#A89262"));
        Ellipse pondWater = new Ellipse(1175, 654, 198, 104);
        pondWater.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#32B0CE")), new Stop(0.6, Color.web("#258FB0")), new Stop(1.0, Color.web("#1A708F"))));
        bg.getChildren().addAll(pondRim, pondWater);

        double[][] glints = { {1065, 618, 44}, {1125, 646, 56}, {1088, 678, 38}, {1180, 612, 48} };
        for (double[] g : glints) {
            Rectangle r = new Rectangle(g[0], g[1], g[2], 3.5);
            r.setArcWidth(3); r.setArcHeight(3);
            r.setFill(Color.rgb(220, 252, 255, 0.58));
            pondGlints.add(r);
            bg.getChildren().add(r);
        }

        // Con đường đá cuội uốn lượn từ khu cối xay gió & nhà kho xuống tiền cảnh
        double[][] stones = {
            {635, 438, 34, 15}, {602, 460, 40, 17}, {568, 486, 44, 19},
            {536, 514, 46, 20}, {508, 546, 48, 21}, {486, 582, 52, 22},
            {468, 622, 54, 23}, {456, 666, 58, 25},
            {672, 456, 36, 16}, {715, 482, 40, 18}, {762, 514, 44, 19},
            {812, 548, 48, 21}, {858, 586, 50, 22}
        };
        for (double[] st : stones) {
            Ellipse stone = new Ellipse(st[0], st[1], st[2] * 0.5, st[3] * 0.5);
            stone.setFill(Color.web("#D8CCA8"));
            stone.setStroke(Color.web("#8E8468"));
            stone.setStrokeWidth(1.6);
            bg.getChildren().add(stone);
        }

        // Hàng cây pixel nhiều sắc thái (thông, hoa anh đào, lá thu, sồi)
        placeSprite(bg, "/view/assets/tiled/objects/tree_pine.png", 16, 240, 96, 128);
        placeSprite(bg, "/view/assets/tiled/objects/tree_blossom.png", 112, 262, 108, 122);
        placeSprite(bg, "/view/assets/tiled/objects/tree_small.png", 224, 286, 72, 86);
        placeSprite(bg, "/view/assets/tiled/objects/tree_autumn.png", 342, 266, 98, 114);
        placeSprite(bg, "/view/assets/tiled/objects/tree.png", 1018, 270, 108, 124);
        placeSprite(bg, "/view/assets/tiled/objects/tree_blossom.png", 1142, 254, 124, 140);

        // Cối xay gió pixel với 6 frame cánh quạt tự động xoay lặp lại nhẹ nhàng
        placeSprite(bg, "/view/assets/tiled/objects/windmill_tower.png", 486, 212, 126, 174);
        windmillFrames = new Image[6];
        for (int i = 0; i < 6; i++) {
            InputStream is = getClass().getResourceAsStream("/view/assets/tiled/objects/windmill_blades_" + i + ".png");
            if (is != null) windmillFrames[i] = new Image(is);
        }
        if (windmillFrames[0] != null) {
            windmillBladesView = new ImageView(windmillFrames[0]);
            windmillBladesView.setFitWidth(164);
            windmillBladesView.setFitHeight(164);
            windmillBladesView.setPreserveRatio(true);
            windmillBladesView.setSmooth(false);
            windmillBladesView.setLayoutX(467);
            windmillBladesView.setLayoutY(188);
            bg.getChildren().add(windmillBladesView);
        }

        // Nhà kho & Nhà chính (có khói ống khói pixel bay lên)
        placeSprite(bg, "/view/assets/tiled/objects/barn.png", 662, 282, 174, 158);
        placeSprite(bg, "/view/assets/tiled/objects/house_main.png", 844, 302, 166, 162);

        // Lửa trại bên trái có ánh sáng cam ấm
        campfireGlow = new Circle(266, 444, 62);
        campfireGlow.setFill(new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(255, 178, 58, 0.48)),
            new Stop(0.6, Color.rgb(255, 114, 32, 0.18)),
            new Stop(1.0, Color.TRANSPARENT)
        ));
        bg.getChildren().add(campfireGlow);
        placeSprite(bg, "/view/assets/tiled/objects/campfire.png", 232, 406, 70, 64);

        // 2 Luống nông sản 2D Pixel Art SẮC NÉT (đất cày + rào gỗ + lúa mì vàng + cà chua đỏ + bí ngô cam!)
        buildPixelCropPlot(bg, 560, 488, 176, 108, true);
        buildPixelCropPlot(bg, 756, 514, 156, 98, false);

        // Giếng nước pixel mái gỗ thủ công + Cột đèn đường + Biển chỉ dẫn dọc lối đi (giống ảnh mẫu!)
        bg.getChildren().add(buildPixelWell(462, 424));
        placeSprite(bg, "/view/assets/tiled/objects/streetlamp.png", 554, 418, 38, 74);
        placeSprite(bg, "/view/assets/tiled/objects/signpost_right.png", 640, 404, 38, 44);

        // Hàng rào gỗ, bụi cây, hoa dại & cụm bí ngô cam rực rỡ ở tiền cảnh (giống ảnh mẫu!)
        placeSprite(bg, "/view/assets/tiled/objects/fence_h.png", 358, 466, 92, 36);
        placeSprite(bg, "/view/assets/tiled/objects/bush1.png", 356, 396, 52, 44);
        placeSprite(bg, "/view/assets/tiled/objects/bush2.png", 760, 444, 54, 44);
        placeSprite(bg, "/view/assets/tiled/objects/flower_patch.png", 386, 578, 60, 40);
        placeSprite(bg, "/view/assets/tiled/objects/flower_patch.png", 734, 634, 62, 42);

        // Cụm bí ngô tươi ở ven đường đá phía dưới giống ảnh mẫu Farm Together
        placeSprite(bg, "/view/assets/tiled/objects/pumpkin_vine.png", 564, 626, 74, 48);
        placeSprite(bg, "/view/assets/tiled/objects/pumpkin_big.png", 614, 624, 52, 46);
        placeSprite(bg, "/view/assets/tiled/objects/pumpkin.png", 584, 646, 34, 32);
        placeSprite(bg, "/view/assets/tiled/objects/pumpkin.png", 662, 648, 32, 30);

        // Đàn bò & gà gặm cỏ trên đồng
        placeSprite(bg, "/view/assets/tiled/objects/cow.png", 388, 492, 64, 54);
        placeSprite(bg, "/view/assets/tiled/objects/chicken.png", 498, 548, 36, 36);
        placeSprite(bg, "/view/assets/tiled/objects/chicken.png", 542, 570, 34, 34);

        // Lớp hạt động (khói ống khói, lá rơi pixel & đom đóm/bướm bay)
        dynamicFxPane = new Pane();
        dynamicFxPane.setPrefSize(W, H);
        dynamicFxPane.setMouseTransparent(true);
        bg.getChildren().add(dynamicFxPane);

        // 12 lá pixel rơi nhẹ (lá xanh non + cánh hoa anh đào hồng + lá thu vàng)
        Color[] leafPalette = {
            Color.web("#74DF48"), Color.web("#4EBA32"), Color.web("#FFB2C8"), Color.web("#FFE066")
        };
        for (int i = 0; i < 12; i++) {
            Rectangle leaf = new Rectangle(6 + (i % 3) * 2, 4 + (i % 2) * 2);
            leaf.setFill(leafPalette[i % leafPalette.length]);
            leaf.setStroke(Color.rgb(36, 58, 22, 0.55));
            leaf.setStrokeWidth(0.8);
            double lx = rng.nextDouble() * W;
            double ly = rng.nextDouble() * H;
            leaf.setLayoutX(lx);
            leaf.setLayoutY(ly);
            dynamicFxPane.getChildren().add(leaf);
            fallingLeaves.add(new LeafActor(
                leaf, lx, ly,
                10.0 + rng.nextDouble() * 14.0,
                16.0 + rng.nextDouble() * 18.0,
                rng.nextDouble() * Math.PI * 2.0,
                14.0 + rng.nextDouble() * 16.0,
                (rng.nextBoolean() ? 1 : -1) * (35.0 + rng.nextDouble() * 55.0)
            ));
        }

        // 8 đom đóm / bướm nhỏ bay lượn ngẫu nhiên trên đồng cỏ
        for (int i = 0; i < 8; i++) {
            boolean isButterfly = (i % 2 == 0);
            Group bug = new Group();
            if (isButterfly) {
                Rectangle wL = new Rectangle(-4, -3, 4, 4);
                Rectangle wR = new Rectangle(1, -3, 4, 4);
                Color wingCol = (i % 4 == 0) ? Color.web("#FFE966") : Color.web("#FFF8E8");
                wL.setFill(wingCol); wR.setFill(wingCol);
                bug.getChildren().addAll(wL, wR);
            } else {
                Circle glow = new Circle(0, 0, 5.5, Color.rgb(255, 246, 146, 0.35));
                Rectangle core = new Rectangle(-2, -2, 4, 4);
                core.setFill(Color.web("#FFFAC2"));
                bug.getChildren().addAll(glow, core);
            }
            double ax = 360 + rng.nextDouble() * (W - 520);
            double ay = 260 + rng.nextDouble() * 340;
            bug.setLayoutX(ax); bug.setLayoutY(ay);
            dynamicFxPane.getChildren().add(bug);
            fireflies.add(new FireflyActor(
                bug, ax, ay,
                22 + rng.nextDouble() * 32,
                12 + rng.nextDouble() * 20,
                0.7 + rng.nextDouble() * 0.9,
                1.1 + rng.nextDouble() * 1.1,
                rng.nextDouble() * Math.PI * 2.0
            ));
        }

        // Nắng ấm lan nhẹ từ góc trên trái
        Rectangle sunWash = new Rectangle(W, H);
        sunWash.setMouseTransparent(true);
        sunWash.setFill(new RadialGradient(
            0, 0, 0.16, 0.12, 0.72, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(255, 250, 200, 0.16)),
            new Stop(1.0, Color.TRANSPARENT)
        ));
        bg.getChildren().add(sunWash);
    }

    /** Vẽ luống đất trồng rau 2D Pixel Art sắc nét (đất cày + rãnh + rào gỗ + lúa mì / cà chua / bí ngô). */
    private void buildPixelCropPlot(Pane parent, double x, double y, double w, double h, boolean wheatAndTomato) {
        Group plot = new Group();
        plot.setLayoutX(x);
        plot.setLayoutY(y);

        // Khung gỗ & nền đất cày pixel sắc nét
        Rectangle outerWood = new Rectangle(0, 0, w, h);
        outerWood.setFill(Color.web("#7B4B24"));
        outerWood.setStroke(Color.web("#3D210C"));
        outerWood.setStrokeWidth(2.5);

        Rectangle soil = new Rectangle(6, 6, w - 12, h - 12);
        soil.setFill(Color.web("#6B4223"));
        plot.getChildren().addAll(outerWood, soil);

        // Các rãnh đất cày ngang
        for (double ry = 18; ry < h - 14; ry += 20) {
            Rectangle furrow = new Rectangle(10, ry, w - 20, 8);
            furrow.setFill(Color.web("#523118"));
            plot.getChildren().add(furrow);
        }

        // Cây trồng pixel nở rộ trên từng luống
        for (int row = 0; row < 3; row++) {
            double cy = 14 + row * 28;
            for (int col = 0; col < 4; col++) {
                double cx = 18 + col * ((w - 42) / 3.2);
                if (wheatAndTomato && row < 2) {
                    // Bụi lúa mì vàng pixel
                    Rectangle stalk = new Rectangle(cx, cy + 4, 14, 14);
                    stalk.setFill(Color.web("#62B834"));
                    stalk.setStroke(Color.web("#2B5C16")); stalk.setStrokeWidth(1.2);
                    Rectangle ear1 = new Rectangle(cx + 1, cy - 4, 5, 11);
                    Rectangle ear2 = new Rectangle(cx + 8, cy - 6, 5, 13);
                    ear1.setFill(Color.web("#F5C431")); ear1.setStroke(Color.web("#8C5C0E")); ear1.setStrokeWidth(1.0);
                    ear2.setFill(Color.web("#FFE15C")); ear2.setStroke(Color.web("#8C5C0E")); ear2.setStrokeWidth(1.0);
                    plot.getChildren().addAll(stalk, ear1, ear2);
                } else {
                    // Bụi cà chua đỏ / bí ngô pixel
                    Rectangle bush = new Rectangle(cx, cy + 2, 18, 14);
                    bush.setArcWidth(4); bush.setArcHeight(4);
                    bush.setFill(Color.web("#3FA632"));
                    bush.setStroke(Color.web("#1E5216")); bush.setStrokeWidth(1.2);
                    Circle f1 = new Circle(cx + 5, cy + 9, 3.6, wheatAndTomato ? Color.web("#E83B2E") : Color.web("#F58220"));
                    Circle f2 = new Circle(cx + 13, cy + 7, 3.2, wheatAndTomato ? Color.web("#FF5242") : Color.web("#FFA234"));
                    f1.setStroke(Color.web("#5C180E")); f1.setStrokeWidth(0.9);
                    f2.setStroke(Color.web("#5C180E")); f2.setStrokeWidth(0.9);
                    plot.getChildren().addAll(bush, f1, f2);
                }
            }
        }

        // Cọc gỗ 4 góc luống
        double[][] posts = { {-2, -4}, {w - 6, -4}, {-2, h - 10}, {w - 6, h - 10} };
        for (double[] p : posts) {
            Rectangle post = new Rectangle(p[0], p[1], 8, 14);
            post.setFill(Color.web("#A86B34"));
            post.setStroke(Color.web("#3D210C")); post.setStrokeWidth(1.5);
            plot.getChildren().add(post);
        }

        plot.setEffect(new DropShadow(8, 0, 3, Color.rgb(18, 38, 12, 0.35)));
        parent.getChildren().add(plot);
    }

    /** Vẽ chiếc giếng đá mái gỗ 2D Pixel Art xinh xắn bên lối đi (giống giếng nước trong ảnh mẫu). */
    private Group buildPixelWell(double x, double y) {
        Group g = new Group();
        g.setLayoutX(x);
        g.setLayoutY(y);

        // Thành giếng đá tròn/pixel
        Rectangle stoneBase = new Rectangle(6, 36, 40, 22);
        stoneBase.setArcWidth(8); stoneBase.setArcHeight(8);
        stoneBase.setFill(Color.web("#9AA3AB"));
        stoneBase.setStroke(Color.web("#3B4248")); stoneBase.setStrokeWidth(2.0);
        Rectangle waterInner = new Rectangle(11, 38, 30, 6);
        waterInner.setFill(Color.web("#38A8CC"));
        Rectangle brickLine = new Rectangle(8, 47, 36, 2);
        brickLine.setFill(Color.web("#666F78"));

        // 2 Cột gỗ & trục quay dây
        Rectangle postL = new Rectangle(8, 14, 5, 24);
        Rectangle postR = new Rectangle(39, 14, 5, 24);
        postL.setFill(Color.web("#8E5326")); postL.setStroke(Color.web("#3D1E0A")); postL.setStrokeWidth(1.4);
        postR.setFill(Color.web("#8E5326")); postR.setStroke(Color.web("#3D1E0A")); postR.setStrokeWidth(1.4);
        Rectangle bar = new Rectangle(11, 22, 30, 3);
        bar.setFill(Color.web("#C79252"));

        // Mái gỗ tam giác pixel
        Polygon roof = new Polygon(0, 18, 26, 0, 52, 18);
        roof.setFill(Color.web("#B8622A"));
        roof.setStroke(Color.web("#421E0A")); roof.setStrokeWidth(2.2);

        g.getChildren().addAll(postL, postR, bar, stoneBase, waterInner, brickLine, roof);
        g.setEffect(new DropShadow(8, 0, 3, Color.rgb(18, 38, 12, 0.35)));
        return g;
    }

    /** Đám mây pixel bậc thang (opacity 0.6) trôi chậm rãi từ trái sang phải. */
    private void addPixelCloud(Pane parent, double x, double y, double scale, double speed) {
        Group g = new Group();
        Color hi = Color.web("#E6FFF9");
        Color lo = Color.web("#9CECE3");

        Rectangle rBottom = new Rectangle(0, 28, 128, 18);
        Rectangle rMid    = new Rectangle(16, 14, 96, 18);
        Rectangle rTop1   = new Rectangle(30, 2, 42, 16);
        Rectangle rTop2   = new Rectangle(66, 8, 32, 12);
        Rectangle rShade  = new Rectangle(8, 40, 112, 6);

        rBottom.setFill(hi); rMid.setFill(hi); rTop1.setFill(hi); rTop2.setFill(hi);
        rShade.setFill(lo);

        g.getChildren().addAll(rBottom, rMid, rTop1, rTop2, rShade);
        g.setOpacity(0.60);
        g.setScaleX(scale); g.setScaleY(scale);
        g.setLayoutX(x); g.setLayoutY(y);
        parent.getChildren().add(g);
        clouds.add(new CloudActor(g, speed, y, 132 * scale));
    }

    private Path makeHillPath(double x0, double y0, double cx1, double cy1, double cx2, double cy2, double xEnd, double yEnd, double bottomY) {
        Path p = new Path();
        p.getElements().addAll(
            new MoveTo(x0, y0),
            new CubicCurveTo(cx1, cy1, cx2, cy2, xEnd, yEnd),
            new LineTo(xEnd, bottomY),
            new LineTo(x0, bottomY),
            new LineTo(x0, y0)
        );
        p.setStroke(null);
        return p;
    }

    private void placeSprite(Pane parent, String resourcePath, double x, double y, double w, double h) {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) return;
        Image img = new Image(is);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w); iv.setFitHeight(h);
        iv.setPreserveRatio(true); iv.setSmooth(false);
        iv.setLayoutX(x); iv.setLayoutY(y);
        iv.setEffect(new DropShadow(8, 0, 3, Color.rgb(18, 42, 16, 0.32)));
        parent.getChildren().add(iv);
    }

    // ==================== BƯỚC 1: MÀN HÌNH TIÊU ĐỀ (LOGO 3D SMART FARM) ====================

    private void populateTitleSplashLayer(StackPane layer) {
        layer.getChildren().clear();
        layer.setStyle("-fx-cursor: hand;");
        layer.setOnMouseClicked(e -> enterDashboardFromSplash());

        Group logo3D = buildFarmTogetherLogo();
        splashLogoNode = logo3D;
        StackPane.setAlignment(logo3D, Pos.CENTER);
        StackPane.setMargin(logo3D, new Insets(0, 0, 42, 0));

        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(2.1), logo3D);
        floatAnim.setFromY(-6); floatAnim.setToY(7);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.play();

        Label pressPrompt = new Label("Nhấn  ↵ Enter  hoặc  Click để Bắt đầu");
        pressPrompt.setStyle(
            "-fx-font-family: " + FONT_UI + ";" +
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;" +
            "-fx-background-color: rgba(24, 28, 30, 0.76);" +
            "-fx-border-color: rgba(255, 255, 255, 0.28); -fx-border-width: 1.5px;" +
            "-fx-padding: 8 24 8 24; -fx-background-radius: 10px; -fx-border-radius: 10px;"
        );
        StackPane.setAlignment(pressPrompt, Pos.BOTTOM_CENTER);
        StackPane.setMargin(pressPrompt, new Insets(0, 0, 26, 0));

        FadeTransition pulse = new FadeTransition(Duration.seconds(1.05), pressPrompt);
        pulse.setFromValue(0.62); pulse.setToValue(1.0);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        layer.getChildren().addAll(logo3D, pressPrompt);
    }

    private Group buildFarmTogetherLogo() {
        Group logo = new Group();
        Group shadowBack = new Group();
        for (int depth = 12; depth >= 1; depth--) {
            Text t1 = styledLogoText("SMART", 118, Color.web("#2B2F36"), Color.web("#22252B"), 16);
            t1.setX(-depth * 0.55); t1.setY(108 + depth * 1.15);
            Text t2 = styledLogoText("FARM", 92, Color.web("#2B2F36"), Color.web("#22252B"), 15);
            t2.setX(66 - depth * 0.55); t2.setY(194 + depth * 1.15);
            shadowBack.getChildren().addAll(t1, t2);
        }

        Text smartFront = styledLogoText(
            "SMART", 118,
            new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFEE4A")),
                new Stop(0.55, Color.web("#FFC116")),
                new Stop(1.0, Color.web("#F28B0C"))),
            Color.web("#33230A"), 4.5
        );
        smartFront.setX(0); smartFront.setY(108);

        Text farmFront = styledLogoText(
            "FARM", 92,
            new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFFFFF")),
                new Stop(0.72, Color.web("#ECF3F7")),
                new Stop(1.0, Color.web("#C7D4DC"))),
            Color.web("#2C323B"), 4.0
        );
        farmFront.setX(66); farmFront.setY(194);

        Group leftLeaves = createLeafCluster(-24, 34, -36, 1.05, false);
        Group rightLeaves = createLeafCluster(404, 18, 28, 1.15, false);
        Group lowLeftLeaf = createLeafCluster(-8, 146, -54, 0.72, false);

        logo.getChildren().addAll(shadowBack, leftLeaves, rightLeaves, lowLeftLeaf, smartFront, farmFront);
        logo.setRotate(-3.5);
        logo.setEffect(new DropShadow(24, 0, 10, Color.rgb(12, 24, 18, 0.55)));
        return logo;
    }

    private Text styledLogoText(String str, double size, javafx.scene.paint.Paint fill, Color stroke, double strokeW) {
        Text t = new Text(str);
        t.setFont(Font.font("Arial Black", FontWeight.EXTRA_BOLD, size));
        t.setFill(fill); t.setStroke(stroke); t.setStrokeWidth(strokeW);
        return t;
    }

    private Group createLeafCluster(double x, double y, double rotateDeg, double scale, boolean withBerries) {
        Group g = new Group();
        Polygon bigLeaf = new Polygon(0, 0, -12, -26, 0, -48, 16, -24);
        bigLeaf.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#7BFF3B")), new Stop(1.0, Color.web("#31B422"))));
        bigLeaf.setStroke(Color.web("#27302A")); bigLeaf.setStrokeWidth(4.0);

        Polygon sideLeaf = new Polygon(4, -4, 24, -18, 34, -34, 10, -28);
        sideLeaf.setFill(Color.web("#5AE62D"));
        sideLeaf.setStroke(Color.web("#27302A")); sideLeaf.setStrokeWidth(3.5);

        g.getChildren().addAll(sideLeaf, bigLeaf);
        if (withBerries) {
            Circle b1 = new Circle(-4, 2, 5.5, Color.web("#E63B28"));
            Circle b2 = new Circle(6, 5, 4.8, Color.web("#FF5742"));
            b1.setStroke(Color.web("#54120B")); b1.setStrokeWidth(1.8);
            b2.setStroke(Color.web("#54120B")); b2.setStrokeWidth(1.8);
            g.getChildren().addAll(b1, b2);
        }
        g.setLayoutX(x); g.setLayoutY(y);
        g.setRotate(rotateDeg);
        g.setScaleX(scale); g.setScaleY(scale);
        return g;
    }

    private void enterDashboardFromSplash() {
        if (splashDismissed) return;
        splashDismissed = true;
        SoundHook.playMenuClick();
        SoundHook.startMenuBgm();

        dashboardLayer.setVisible(true);
        dashboardLayer.setOpacity(0.0);

        FadeTransition fadeOutSplash = new FadeTransition(Duration.millis(280), splashLayer);
        fadeOutSplash.setFromValue(1.0); fadeOutSplash.setToValue(0.0);
        fadeOutSplash.setOnFinished(ev -> splashLayer.setVisible(false));

        FadeTransition fadeInDash = new FadeTransition(Duration.millis(340), dashboardLayer);
        fadeInDash.setFromValue(0.0); fadeInDash.setToValue(1.0);

        new ParallelTransition(fadeOutSplash, fadeInDash).play();
    }

    // ==================== 2, 3, 4. DASHBOARD CHÍNH (BẢNG GỖ 7 NÚT + SỰ KIỆN 7d + CHÀO MỪNG + PROFILE) ====================

    private void populateDashboardLayer(AnchorPane pane) {
        pane.getChildren().clear();

        // 4. Góc trên phải: Hồ sơ Người chơi / Luffy thanh mảnh + Sao lấp lánh + Avatar cận mặt
        HBox profileRibbon = buildTopRightProfileBadge();
        dashProfileNode = profileRibbon;
        Group profileWrap = new Group(profileRibbon);
        AnchorPane.setTopAnchor(profileWrap, 18.0);
        AnchorPane.setRightAnchor(profileWrap, 24.0);

        // 2. Bên trái: Bảng gỗ pixel lớn chứa 7 nút chức năng
        StackPane leftWoodBoard = buildLeftWoodMenuBoard();
        dashLeftMenuNode = leftWoodBoard;
        Group leftWrap = new Group(leftWoodBoard);
        AnchorPane.setBottomAnchor(leftWrap, 22.0);
        AnchorPane.setLeftAnchor(leftWrap, 24.0);

        // 3. Bên phải: Bảng Sự Kiện ("7d" trồi mép phải) phía trên + Bảng Chào Mừng cuộn mượt phía dưới
        VBox rightPanels = buildRightAnnouncementPanels();
        dashRightPanelNode = rightPanels;
        Group rightWrap = new Group(rightPanels);
        AnchorPane.setBottomAnchor(rightWrap, 22.0);
        AnchorPane.setRightAnchor(rightWrap, 24.0);

        pane.getChildren().addAll(profileWrap, leftWrap, rightWrap);
    }

    /**
     * Tự động co giãn nền nông trại (chế độ COVER phủ kín 100% không lộ viền trống khi phóng to cửa sổ)
     * và phóng cân đối nhẹ các bảng giao diện bên ngoài theo kích thước màn hình.
     */
    private void updateResponsiveLayout() {
        if (rootStack == null) return;
        double vw = rootStack.getWidth();
        double vh = rootStack.getHeight();
        if (vw < 100 || vh < 100) {
            vw = W;
            vh = H;
        }

        if (sceneryCanvas != null && sceneryBgScale != null) {
            double bgScale = Math.max(vw / W, vh / H);
            sceneryBgScale.setX(bgScale);
            sceneryBgScale.setY(bgScale);
            sceneryCanvas.setLayoutX((vw - W * bgScale) * 0.5);
            sceneryCanvas.setLayoutY((vh - H * bgScale) * 0.5);
        }

        double fitScale = Math.min(vw / W, vh / H);
        double uiScale = Math.max(0.85, Math.min(1.22, fitScale));
        if (dashProfileNode != null) {
            dashProfileNode.setScaleX(uiScale);
            dashProfileNode.setScaleY(uiScale);
        }
        if (dashLeftMenuNode != null) {
            dashLeftMenuNode.setScaleX(uiScale);
            dashLeftMenuNode.setScaleY(uiScale);
        }
        if (dashRightPanelNode != null) {
            dashRightPanelNode.setScaleX(uiScale);
            dashRightPanelNode.setScaleY(uiScale);
        }
        if (splashLogoNode != null) {
            splashLogoNode.setScaleX(uiScale);
            splashLogoNode.setScaleY(uiScale);
        }
    }

    private HBox buildTopRightProfileBadge() {
        profileNameLabel = new Label("NGƯỜI CHƠI / LUFFY");
        profileNameLabel.setStyle(
            "-fx-font-family: " + FONT_PIXEL + ";" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFF7DF;" +
            "-fx-effect: dropshadow(one-pass-box, #3E1D08, 2, 1.0, 0, 1);"
        );

        // Thanh tên gỗ cam thanh mảnh giống ảnh mẫu
        HBox woodStrip = new HBox(profileNameLabel);
        woodStrip.setAlignment(Pos.CENTER_LEFT);
        woodStrip.setMaxHeight(32);
        woodStrip.setPrefHeight(32);
        woodStrip.setPadding(new Insets(4, 20, 4, 14));
        woodStrip.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #D67F34 0%, #9E521C 100%);" +
            "-fx-border-color: #4D2409; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 9px; -fx-border-radius: 9px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.44), 8, 0.2, 0, 3);"
        );

        // Huy hiệu sao vàng cấp độ lấp lánh
        StackPane starBadge = new StackPane();
        starBadge.setPrefSize(44, 44);
        profileStarShape = new Polygon(
            0, -21, 6, -7, 21, -7, 9, 2, 14, 17,
            0, 8, -14, 17, -9, 2, -21, -7, -6, -7
        );
        profileStarShape.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FFF066")), new Stop(1.0, Color.web("#E69614"))));
        profileStarShape.setStroke(Color.web("#5B2E0A"));
        profileStarShape.setStrokeWidth(2.0);

        profileLevelLabel = new Label("4");
        profileLevelLabel.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #3E1B05;"
        );
        starBadge.getChildren().addAll(profileStarShape, profileLevelLabel);

        // Khung avatar tròn cận mặt mũ rơm Luffy
        StackPane avatarFrame = new StackPane();
        avatarFrame.setPrefSize(66, 66);
        avatarFrame.setStyle(
            "-fx-background-color: radial-gradient(center 50% 50%, radius 65%, #FFF4D6, #D9B47E);" +
            "-fx-border-color: #965120; -fx-border-width: 4px;" +
            "-fx-background-radius: 33px; -fx-border-radius: 33px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 10, 0.22, 0, 3);"
        );
        InputStream luffyIs = getClass().getResourceAsStream("/view/assets/player/luffy_idle_down_0.png");
        if (luffyIs != null) {
            Image img = new Image(luffyIs);
            ImageView iv = new ImageView(img);
            // Cắt cận phần đầu + mũ rơm + vai Luffy để hiển thị nổi bật trong khung tròn
            iv.setViewport(new Rectangle2D(22, 8, 64, 64));
            iv.setFitWidth(54); iv.setFitHeight(54);
            iv.setPreserveRatio(true); iv.setSmooth(false);
            avatarFrame.getChildren().add(iv);
        }

        HBox row = new HBox(-10, woodStrip, starBadge, avatarFrame);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    /** Bảng gỗ pixel lớn bên trái chứa đủ 7 nút kèm 4 đinh tán góc & cành lá quả mọng. */
    private StackPane buildLeftWoodMenuBoard() {
        VBox buttonRows = new VBox(8);
        buttonRows.setAlignment(Pos.CENTER);

        btnCreateFarm = buildInteractiveWoodRow(buttonRows, "Tạo nông trại", true, false, this::openCreateFarmModal);
        btnMyFarm     = buildInteractiveWoodRow(buttonRows, "Nông trại của tôi", false, false, this::onContinue);
        buildInteractiveWoodRow(buttonRows, "Tìm nông trại", false, false, this::openExploreFarmsModal);
        buildInteractiveWoodRow(buttonRows, "Tùy chỉnh", false, true, this::openCustomizeModal);
        buildInteractiveWoodRow(buttonRows, "Cài đặt", false, false, this::openSettingsModal);
        buildInteractiveWoodRow(buttonRows, "Thêm", false, true, this::openMoreInfoModal);
        buildInteractiveWoodRow(buttonRows, "Thoát", false, false, this::onQuit);

        // Khung giấy da bên trong có 4 đinh tán bạc pixel ở 4 góc giống image_15.png
        StackPane parchmentWrap = new StackPane();
        parchmentWrap.getStyleClass().add("pixel-menu-inner");
        parchmentWrap.setPadding(new Insets(16, 15, 16, 15));
        parchmentWrap.getChildren().add(buttonRows);

        Pos[] corners = { Pos.TOP_LEFT, Pos.TOP_RIGHT, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT };
        for (Pos corner : corners) {
            Circle nail = new Circle(3.6, Color.web("#A8B4BE"));
            nail.setStroke(Color.web("#4B545C"));
            nail.setStrokeWidth(1.3);
            nail.setMouseTransparent(true);
            parchmentWrap.getChildren().add(nail);
            StackPane.setAlignment(nail, corner);
            StackPane.setMargin(nail, new Insets(-9));
        }

        VBox woodFrame = new VBox(parchmentWrap);
        woodFrame.getStyleClass().add("pixel-menu-board");
        woodFrame.setPrefWidth(338);
        woodFrame.setPadding(new Insets(12));

        // Cành lá xanh + quả mọng đỏ trang trí góc bảng gỗ giống ảnh mẫu
        Group topRightLeaf = createLeafCluster(0, 0, 42, 0.58, true);
        topRightLeaf.setMouseTransparent(true);
        Group bottomLeftLeaf = createLeafCluster(0, 0, -142, 0.48, false);
        bottomLeftLeaf.setMouseTransparent(true);

        StackPane wrap = new StackPane(woodFrame, topRightLeaf, bottomLeftLeaf);
        StackPane.setAlignment(topRightLeaf, Pos.TOP_RIGHT);
        StackPane.setMargin(topRightLeaf, new Insets(-6, -12, 0, 0));
        StackPane.setAlignment(bottomLeftLeaf, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bottomLeftLeaf, new Insets(0, 0, -8, -8));

        refreshMainButtons();
        return wrap;
    }

    /**
     * Nút bảng gỗ tương tác:
     * - Hover: scale 1.0 -> 1.05, tiếng pop pixel nhẹ, bật mầm lá xanh bên trái và làm nổi bật nút.
     * - Click: nhún xuống translateY +2px + tiếng gõ gỗ.
     */
    private Button buildInteractiveWoodRow(VBox container, String label, boolean orangeStyle, boolean hasRedBadge, Runnable action) {
        Button btn = new Button(label);
        btn.setPrefSize(288, 41);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFocusTraversable(false);
        btn.getStyleClass().add(orangeStyle ? "pixel-menu-btn-orange" : "pixel-menu-btn-blue");

        Group hoverSprout = createMiniPixelSprout();
        hoverSprout.setMouseTransparent(true);
        hoverSprout.setVisible(false);
        hoverSprout.setScaleX(0.2); hoverSprout.setScaleY(0.2);

        StackPane rowStack = new StackPane(btn, hoverSprout);
        StackPane.setAlignment(hoverSprout, Pos.CENTER_LEFT);
        StackPane.setMargin(hoverSprout, new Insets(0, 0, 0, 10));

        if (hasRedBadge) {
            Label excl = new Label("!");
            excl.setMouseTransparent(true);
            excl.setAlignment(Pos.CENTER);
            excl.setPrefSize(22, 22);
            excl.setStyle(
                "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-text-fill: #FFF8E6; -fx-background-color: #E23B2B;" +
                "-fx-border-color: #5C120B; -fx-border-width: 2px;" +
                "-fx-background-radius: 11px; -fx-border-radius: 11px;"
            );
            rowStack.getChildren().add(excl);
            StackPane.setAlignment(excl, Pos.CENTER_RIGHT);
            StackPane.setMargin(excl, new Insets(0, 10, 0, 0));
        }

        btn.setOnMouseEntered(e -> {
            if (btn.isDisabled()) return;
            SoundHook.playMenuHoverPop();
            ScaleTransition st = new ScaleTransition(Duration.millis(110), rowStack);
            st.setToX(1.05); st.setToY(1.05);
            st.play();

            hoverSprout.setVisible(true);
            ScaleTransition leafPop = new ScaleTransition(Duration.millis(130), hoverSprout);
            leafPop.setFromX(0.3); leafPop.setFromY(0.3);
            leafPop.setToX(1.0); leafPop.setToY(1.0);
            leafPop.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), rowStack);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
            rowStack.setTranslateY(0);
            hoverSprout.setVisible(false);
        });

        btn.setOnMousePressed(e -> {
            if (btn.isDisabled()) return;
            rowStack.setTranslateY(2.0);
            SoundHook.playMenuClick();
        });

        btn.setOnMouseReleased(e -> rowStack.setTranslateY(0.0));

        btn.setOnAction(e -> {
            if (!btn.isDisabled() && !transitionRunning && action != null) {
                action.run();
            }
        });

        container.getChildren().add(rowStack);
        return btn;
    }

    private Group createMiniPixelSprout() {
        Rectangle stem = new Rectangle(5, 8, 2.5, 7);
        stem.setFill(Color.web("#2B7A1E"));
        Rectangle leafL = new Rectangle(0, 4, 6, 5);
        leafL.setArcWidth(2); leafL.setArcHeight(2);
        leafL.setFill(Color.web("#8DFF4A"));
        leafL.setStroke(Color.web("#1D4E12")); leafL.setStrokeWidth(1.0);
        Rectangle leafR = new Rectangle(6, 2, 6, 5);
        leafR.setArcWidth(2); leafR.setArcHeight(2);
        leafR.setFill(Color.web("#5CE62E"));
        leafR.setStroke(Color.web("#1D4E12")); leafR.setStrokeWidth(1.0);
        return new Group(stem, leafL, leafR);
    }

    private void refreshMainButtons() {
        if (btnMyFarm != null) {
            btnMyFarm.setDisable(!continueEnabled);
            btnMyFarm.getStyleClass().removeAll("pixel-menu-btn-blue", "pixel-menu-btn-disabled");
            btnMyFarm.getStyleClass().add(continueEnabled ? "pixel-menu-btn-blue" : "pixel-menu-btn-disabled");
        }
    }

    /** 3. Bảng Sự Kiện ("7d" trồi mép phải) phía trên + Bảng Chào Mừng cuộn mượt phía dưới. */
    private VBox buildRightAnnouncementPanels() {
        // ---- BẢNG SỰ KIỆN + ĐỒNG HỒ TRÒN "7d" TRỒI NỔI MÉP PHẢI GIỐNG ẢNH MẪU ----
        Label eventTitle = new Label("SỰ KIỆN MỚI BẮT ĐẦU TRONG");
        eventTitle.setStyle(
            "-fx-font-family: " + FONT_PIXEL + ";" +
            "-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #FFF7DE;" +
            "-fx-effect: dropshadow(one-pass-box, #4A1908, 2, 1.0, 0, 1);"
        );
        Label eventSub = new Label("Mùa Thu Hoạch & Săn Cá Vương Miện");
        eventSub.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FFE0A3;"
        );
        VBox eventTextCol = new VBox(1, eventTitle, eventSub);
        eventTextCol.setAlignment(Pos.CENTER_LEFT);

        HBox eventPlank = new HBox(eventTextCol);
        eventPlank.setAlignment(Pos.CENTER_LEFT);
        eventPlank.setPrefSize(356, 54);
        eventPlank.setMaxSize(356, 54);
        eventPlank.setPadding(new Insets(8, 42, 8, 16));
        eventPlank.setRotate(-2.0);
        eventPlank.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #D66B2C 0%, #9E4016 100%);" +
            "-fx-border-color: #4D1B07; -fx-border-width: 3px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 12, 0.22, 0, 4);"
        );

        // Vòng tròn đồng hồ đếm ngược pixel "7d" viền vàng + xanh dương trồi nổi ngoài mép phải
        StackPane badge7d = new StackPane();
        badge7d.setPrefSize(60, 60);
        badge7d.setMinSize(60, 60);
        badge7d.setMaxSize(60, 60);
        badge7d.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #54B8EA 0%, #2678A8 100%);" +
            "-fx-border-color: #F4C44E; -fx-border-width: 4px;" +
            "-fx-background-radius: 30px; -fx-border-radius: 30px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.46), 10, 0.25, 0, 3);"
        );
        Label countTxt = new Label("7d");
        countTxt.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;" +
            "-fx-effect: dropshadow(one-pass-box, #164462, 2, 1.0, 0, 1);"
        );
        badge7d.getChildren().add(countTxt);

        Group eventLeaf = createLeafCluster(0, 0, 22, 0.56, false);
        eventLeaf.setMouseTransparent(true);

        StackPane eventWrap = new StackPane(eventPlank, eventLeaf, badge7d);
        eventWrap.setPrefWidth(392);
        StackPane.setAlignment(eventPlank, Pos.CENTER_LEFT);
        StackPane.setAlignment(badge7d, Pos.CENTER_RIGHT);
        StackPane.setAlignment(eventLeaf, Pos.TOP_RIGHT);
        StackPane.setMargin(eventLeaf, new Insets(-14, 10, 0, 0));

        // ---- BẢNG CHÀO MỪNG / GIỚI THIỆU (GIẤY CŨ CUỘN MƯỢT) ----
        Label welcomeTitle = new Label("Chào mừng đến với Smart Farm!");
        welcomeTitle.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7B2D14;"
        );

        Label welcomeIntro = new Label(
            "Chúng tôi đã nỗ lực hết sức để tạo ra một trò chơi nông trại vui vẻ và thoải mái cho mọi người. " +
            "Hy vọng bạn sẽ có khoảng thời gian tuyệt vời khi chơi game!"
        );
        welcomeIntro.setWrapText(true);
        welcomeIntro.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 12.5px; -fx-text-fill: #4A2912; -fx-line-spacing: 2px;"
        );

        welcomeFarmStatusLabel = new Label();
        welcomeFarmStatusLabel.setWrapText(true);
        welcomeFarmStatusLabel.setPadding(new Insets(7, 10, 7, 10));
        welcomeFarmStatusLabel.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #3B200B;" +
            "-fx-background-color: rgba(186, 134, 72, 0.24);" +
            "-fx-border-color: #B9864E; -fx-border-width: 1.5px;" +
            "-fx-background-radius: 7px; -fx-border-radius: 7px;"
        );

        Label changelogHeader = new Label("★ CẬP NHẬT MỚI & MẸO CHƠI (CUỘN XUỐNG)");
        changelogHeader.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8E4418;"
        );

        Label changelogBody = new Label(
            "• Câu cá sông Play Together: Bóng cá 5 cấp, nhấp mồi giả, phản xạ '!' và spam [Space] kéo cá khổng lồ!\n" +
            "• Nấu ăn lửa trại [E]: Nấu Trứng, Nấm, Sữa và Lẩu Nông Trại bán giá x2–x3 hoặc ăn hồi Đói/Nước!\n" +
            "• Sinh tồn Đói & Khát: Ra bờ sông bấm [R] uống nước mát, hoặc bấm [Q] ăn nhanh ngoài đồng.\n" +
            "• Đi ngủ tại nhà [E]: Nghỉ ngơi qua ngày mới với hiệu ứng tối dần cực chill và tự động lưu game.\n" +
            "• Điều khiển: [WASD] chạy 8 hướng • [Space] cuốc/tưới/câu • [G] Sổ cá • [I] Kho đồ."
        );
        changelogBody.setWrapText(true);
        changelogBody.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 11.8px; -fx-text-fill: #5B3519; -fx-line-spacing: 2px;"
        );

        VBox scrollContent = new VBox(8, welcomeTitle, welcomeIntro, welcomeFarmStatusLabel, changelogHeader, changelogBody);
        scrollContent.setPadding(new Insets(2, 4, 6, 2));

        ScrollPane scroll = new ScrollPane(scrollContent);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(220);
        scroll.setMaxHeight(225);
        scroll.getStyleClass().add("pixel-welcome-scroll");

        VBox parchmentCard = new VBox(scroll);
        parchmentCard.setPrefWidth(392);
        parchmentCard.setPadding(new Insets(14, 16, 12, 16));
        parchmentCard.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #F8E8CB 0%, #EAD2AB 100%);" +
            "-fx-border-color: #9C6736; -fx-border-width: 3px;" +
            "-fx-background-radius: 12px; -fx-border-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 14, 0.22, 0, 5);"
        );

        VBox rightColumn = new VBox(10, eventWrap, parchmentCard);
        rightColumn.setAlignment(Pos.BOTTOM_RIGHT);
        return rightColumn;
    }

    // ==================== CÁC HỘP THOẠI PHỤ (TẠO NÔNG TRẠI / TÌM / TÙY CHỈNH / CÀI ĐẶT / THÊM) ====================

    private StackPane buildCreateFarmModalCard() {
        Label header = new Label("TẠO NÔNG TRẠI MỚI");
        header.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #FFF7DC;" +
            "-fx-effect: dropshadow(one-pass-box, #3C1B07, 2, 1.0, 0, 2);"
        );

        Label prompt = new Label("Đặt tên cho nông trại mới của bạn:");
        prompt.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #4A2710;");

        farmNameInput = new TextField("Nông Trại Vui Vẻ");
        farmNameInput.setPromptText("Nhập tên nông trại...");
        farmNameInput.setPrefHeight(40);
        farmNameInput.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #3A1F0C;" +
            "-fx-background-color: #FFFDF7; -fx-border-color: #9A5E2B; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 5 12 5 12;"
        );
        farmNameInput.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.length() > 26) farmNameInput.setText(newV.substring(0, 26));
        });
        farmNameInput.setOnAction(e -> confirmCreateNewFarm());

        HBox chips = new HBox(8);
        String[] presets = { "Thung Lũng Xanh", "Nông Trại Luffy", "Đồi Hướng Dương" };
        for (String pr : presets) {
            Button chip = new Button(pr);
            chip.setFocusTraversable(false);
            chip.setStyle(
                "-fx-font-family: " + FONT_UI + "; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: #5C3214; -fx-background-color: #EAD2AC;" +
                "-fx-border-color: #B4824E; -fx-border-width: 1.5px;" +
                "-fx-padding: 4 10 4 10; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-cursor: hand;"
            );
            chip.setOnAction(e -> {
                SoundHook.playMenuClick();
                farmNameInput.setText(pr);
                farmNameInput.positionCaret(pr.length());
            });
            chips.getChildren().add(chip);
        }

        overwriteWarnLabel = new Label("Lưu ý: Tạo nông trại mới sẽ thay thế bản lưu nông trại cũ.");
        overwriteWarnLabel.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #A83A1C;");
        overwriteWarnLabel.setVisible(false);
        overwriteWarnLabel.setManaged(false);

        VBox parchment = new VBox(10, prompt, farmNameInput, chips, overwriteWarnLabel);
        parchment.getStyleClass().add("pixel-menu-inner");
        parchment.setPadding(new Insets(16, 18, 16, 18));

        Button btnGo = new Button("BẮT ĐẦU NÔNG TRẠI");
        btnGo.setPrefSize(210, 42);
        btnGo.setFocusTraversable(false);
        btnGo.getStyleClass().add("pixel-menu-btn-orange");
        btnGo.setOnAction(e -> confirmCreateNewFarm());

        Button btnBack = new Button("QUAY LẠI");
        btnBack.setPrefSize(130, 42);
        btnBack.setFocusTraversable(false);
        btnBack.getStyleClass().add("pixel-menu-btn-blue");
        btnBack.setOnAction(e -> closeModal());

        HBox actions = new HBox(14, btnGo, btnBack);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(14, header, parchment, actions);
        card.getStyleClass().add("pixel-menu-board");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(500); card.setPrefWidth(500);
        card.setMaxHeight(325);
        card.setPadding(new Insets(18, 22, 20, 22));
        return new StackPane(card);
    }

    private void openCreateFarmModal() {
        if (overwriteWarnLabel != null) {
            overwriteWarnLabel.setVisible(continueEnabled);
            overwriteWarnLabel.setManaged(continueEnabled);
        }
        showModalNode(createFarmModalCard);
        Platform.runLater(() -> {
            farmNameInput.requestFocus();
            farmNameInput.selectAll();
        });
    }

    private void confirmCreateNewFarm() {
        String raw = (farmNameInput != null) ? farmNameInput.getText() : "";
        String chosenName = (raw == null || raw.trim().isEmpty()) ? "Nông Trại Vui Vẻ" : raw.trim();
        closeModal();
        playGateFadeAndRun(() -> {
            if (launcher != null) launcher.startNewFarm(chosenName);
        });
    }

    private void openExploreFarmsModal() {
        VBox list = new VBox(10);
        String[][] themes = {
            { "Nông Trại Biển Xanh", "Bắt đầu nhanh nông trại ven sông chuyên câu cá & nấu lẩu." },
            { "Thung Lũng Hướng Dương", "Khởi tạo nông trại nắng ấm thích hợp trồng hoa & ngô." },
            { "Trang Trại Lửa Trại", "Trải nghiệm chăn nuôi Gà/Lợn/Bò và ẩm thực lửa trại." }
        };
        for (String[] th : themes) {
            Label nm = new Label(th[0]);
            nm.setStyle("-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3B1E0B;");
            Label ds = new Label(th[1]);
            ds.setWrapText(true);
            ds.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 12px; -fx-text-fill: #5E391C;");
            VBox info = new VBox(3, nm, ds);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button enter = new Button("Vào ngay");
            enter.setFocusTraversable(false);
            enter.setPrefSize(102, 34);
            enter.getStyleClass().add("pixel-menu-btn-orange");
            enter.setOnAction(ev -> {
                closeModal();
                playGateFadeAndRun(() -> {
                    if (launcher != null) launcher.startNewFarm(th[0]);
                });
            });

            HBox row = new HBox(12, info, enter);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setStyle("-fx-background-color: #FFF4DC; -fx-border-color: #C4955A; -fx-border-width: 1.8px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
            list.getChildren().add(row);
        }
        showSimpleWoodModal("TÌM NÔNG TRẠI — KHÁM PHÁ NHANH", list, 540, 360);
    }

    private void openCustomizeModal() {
        Label lbl = new Label("Tên hiển thị nhân vật (góc phải màn hình):");
        lbl.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4A2710;");

        TextField nameTf = new TextField(farmerDisplayName);
        nameTf.setPrefHeight(38);
        nameTf.setStyle(
            "-fx-font-family: " + FONT_UI + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3A1F0C;" +
            "-fx-background-color: #FFFDF7; -fx-border-color: #9A5E2B; -fx-border-width: 2px; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );

        Label info = new Label("Trang phục hiện tại: Monkey D. Luffy (Áo gile đỏ, mũ rơm, hoạt ảnh 8 hướng Cuốc & Bình tưới).");
        info.setWrapText(true);
        info.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 12px; -fx-text-fill: #643B1B;");

        Button saveBtn = new Button("LƯU TÙY CHỈNH");
        saveBtn.setPrefSize(180, 38);
        saveBtn.setFocusTraversable(false);
        saveBtn.getStyleClass().add("pixel-menu-btn-orange");
        saveBtn.setOnAction(e -> {
            String v = nameTf.getText() != null ? nameTf.getText().trim() : "";
            if (!v.isEmpty()) {
                farmerDisplayName = v.length() > 16 ? v.substring(0, 16) : v;
                refreshProfileHeader();
            }
            SoundHook.playPickup();
            closeModal();
        });

        VBox content = new VBox(12, lbl, nameTf, info, saveBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        showSimpleWoodModal("TÙY CHỈNH NÔNG DÂN", content, 480, 300);
    }

    private void openSettingsModal() {
        Button bgmToggle = new Button();
        bgmToggle.setPrefSize(220, 38);
        bgmToggle.setFocusTraversable(false);
        Runnable syncBgmBtn = () -> {
            boolean on = SoundHook.isBgmEnabled();
            bgmToggle.setText("Nhạc nền 8-bit: " + (on ? "BẬT" : "TẮT"));
            bgmToggle.getStyleClass().setAll(on ? "pixel-menu-btn-orange" : "pixel-menu-btn-blue");
        };
        syncBgmBtn.run();
        bgmToggle.setOnAction(e -> {
            boolean next = !SoundHook.isBgmEnabled();
            SoundHook.setBgmEnabled(next);
            if (next) SoundHook.startMenuBgm();
            SoundHook.playMenuClick();
            syncBgmBtn.run();
        });

        Button sfxToggle = new Button();
        sfxToggle.setPrefSize(220, 38);
        sfxToggle.setFocusTraversable(false);
        Runnable syncSfxBtn = () -> {
            boolean on = SoundHook.isEnabled();
            sfxToggle.setText("Hiệu ứng âm thanh: " + (on ? "BẬT" : "TẮT"));
            sfxToggle.getStyleClass().setAll(on ? "pixel-menu-btn-orange" : "pixel-menu-btn-blue");
        };
        syncSfxBtn.run();
        sfxToggle.setOnAction(e -> {
            SoundHook.setEnabled(!SoundHook.isEnabled());
            SoundHook.playMenuClick();
            syncSfxBtn.run();
        });

        HBox audioRow = new HBox(12, bgmToggle, sfxToggle);
        audioRow.setAlignment(Pos.CENTER);

        Label keys = new Label(
            "PHÍM ĐIỀU KHIỂN:\n" +
            "• W / A / S / D hoặc Mũi tên: Di chuyển 8 hướng / Chèo thuyền (A/D)\n" +
            "• Space / J: Cuốc đất, Tưới nước, Thu hoạch, Câu cá (Spam Space)\n" +
            "• E: Lên/Xuống thuyền, Đi ngủ trước cửa nhà, Nấu ăn tại đống lửa\n" +
            "• R: Uống nước bờ sông  •  Q: Ăn nhanh  •  G: Sổ Cá  •  I: Kho đồ"
        );
        keys.setWrapText(true);
        keys.setStyle("-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 11.5px; -fx-text-fill: #3E210D; -fx-line-spacing: 3px;");

        VBox content = new VBox(14, audioRow, keys);
        showSimpleWoodModal("CÀI ĐẶT & PHÍM BẤM", content, 540, 330);
    }

    private void openMoreInfoModal() {
        Label about = new Label(
            "SMART FARM — 2D PIXEL FARM SIMULATOR (JavaFX)\n\n" +
            "Phiên bản: 2.5 — Panorama & Survival Edition\n" +
            "Tính năng nổi bật: Trồng trọt theo thời tiết, Chăn nuôi Gà/Lợn/Bò, Câu cá sông Play Together, " +
            "Bếp lửa trại, Sinh tồn Đói/Khát và Lưu game tự động.\n\n" +
            "Cảm ơn bạn đã trải nghiệm và đồng hành cùng đội ngũ phát triển!"
        );
        about.setWrapText(true);
        about.setStyle("-fx-font-family: " + FONT_UI + "; -fx-font-size: 13px; -fx-text-fill: #4A2912; -fx-line-spacing: 3px;");

        Button helpBtn = new Button("MỞ HƯỚNG DẪN CHI TIẾT (HELP)");
        helpBtn.setPrefSize(260, 40);
        helpBtn.setFocusTraversable(false);
        helpBtn.getStyleClass().add("pixel-menu-btn-orange");
        helpBtn.setOnAction(e -> {
            closeModal();
            onHelp();
        });

        VBox content = new VBox(14, about, helpBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        showSimpleWoodModal("THÔNG TIN & ĐỘI NGŨ PHÁT TRIỂN", content, 520, 330);
    }

    private void showSimpleWoodModal(String titleText, Node bodyContent, double width, double height) {
        Label header = new Label(titleText);
        header.setStyle(
            "-fx-font-family: " + FONT_PIXEL + "; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #FFF7DC;" +
            "-fx-effect: dropshadow(one-pass-box, #3C1B07, 2, 1.0, 0, 2);"
        );

        VBox inner = new VBox(bodyContent);
        inner.getStyleClass().add("pixel-menu-inner");
        inner.setPadding(new Insets(15, 16, 15, 16));
        VBox.setVgrow(inner, Priority.ALWAYS);

        Button closeBtn = new Button("ĐÓNG");
        closeBtn.setPrefSize(128, 38);
        closeBtn.setFocusTraversable(false);
        closeBtn.getStyleClass().add("pixel-menu-btn-blue");
        closeBtn.setOnAction(e -> closeModal());

        VBox card = new VBox(12, header, inner, closeBtn);
        card.getStyleClass().add("pixel-menu-board");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(width); card.setPrefWidth(width);
        card.setMaxHeight(height);
        card.setPadding(new Insets(16, 20, 18, 20));

        showModalNode(card);
    }

    private void showModalNode(Node content) {
        modalOverlayLayer.getChildren().setAll(content);
        modalOverlayLayer.setVisible(true);
        modalOverlayLayer.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), modalOverlayLayer);
        ft.setFromValue(0.0); ft.setToValue(1.0);
        ft.play();
    }

    private void closeModal() {
        modalOverlayLayer.setVisible(false);
        modalOverlayLayer.setOpacity(0.0);
        Platform.runLater(rootStack::requestFocus);
    }

    // ==================== 5. HIỆU ỨNG CHUYỂN CẢNH FADEOUT 0.8s + MỞ CỔNG GỖ ====================

    private void playGateFadeAndRun(Runnable onComplete) {
        if (transitionRunning) return;
        transitionRunning = true;
        SoundHook.stopMenuBgm();
        SoundHook.playWoodGateOpen();

        transitionCurtain.setVisible(true);
        transitionCurtain.setOpacity(0.0);
        transitionCurtain.toFront();

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), transitionCurtain);
        fadeOut.setFromValue(0.0); fadeOut.setToValue(1.0);
        fadeOut.setOnFinished(e -> {
            try {
                if (onComplete != null) onComplete.run();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                transitionRunning = false;
                transitionCurtain.setVisible(false);
                transitionCurtain.setOpacity(0.0);
            }
        });
        fadeOut.play();
    }

    // ==================== HOẠT ẢNH NỀN SỐNG ĐỘNG (60 FPS) ====================

    private void startBackdropAnimation() {
        if (backdropTimer != null) backdropTimer.stop();
        lastNanos = 0L;
        backdropTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (rootStack.getScene() == null || rootStack.getParent() == null) return;
                double dt = (lastNanos == 0L) ? 0.016 : Math.min(0.05, (now - lastNanos) / 1_000_000_000.0);
                lastNanos = now;
                elapsedSec += dt;

                // 1. Mây pixel bay từ trái sang phải, hết màn tự spawn lại bên trái với độ cao ngẫu nhiên
                for (CloudActor c : clouds) {
                    double nx = c.node.getLayoutX() + c.speed * dt;
                    if (nx > W + 40) {
                        nx = -c.width - 24;
                        c.baseY = 34.0 + rng.nextDouble() * 145.0;
                    }
                    c.node.setLayoutX(nx);
                    c.node.setLayoutY(c.baseY + Math.sin(elapsedSec * 0.65 + c.baseY * 0.02) * 2.5);
                }

                // 2. Cánh quạt cối xay gió xoay 6 frame liên tục
                if (windmillBladesView != null && windmillFrames != null) {
                    windmillTimer += dt;
                    if (windmillTimer >= 0.145) {
                        windmillTimer = 0.0;
                        windmillFrameIdx = (windmillFrameIdx + 1) % windmillFrames.length;
                        if (windmillFrames[windmillFrameIdx] != null) {
                            windmillBladesView.setImage(windmillFrames[windmillFrameIdx]);
                        }
                    }
                }

                // 3. Khói ống khói nhà chính bốc nhẹ lên bầu trời
                smokeSpawnTimer += dt;
                if (smokeSpawnTimer >= 0.52 && dynamicFxPane != null) {
                    smokeSpawnTimer = 0.0;
                    Rectangle puff = new Rectangle(9, 9);
                    puff.setArcWidth(3); puff.setArcHeight(3);
                    puff.setFill(Color.rgb(248, 245, 238, 0.72));
                    double sx = 958 + (rng.nextDouble() - 0.5) * 6.0;
                    double sy = 308;
                    puff.setLayoutX(sx); puff.setLayoutY(sy);
                    dynamicFxPane.getChildren().add(puff);
                    chimneySmokes.add(new SmokePuff(puff, sx, sy, 8.0 + rng.nextDouble() * 8.0, -24.0 - rng.nextDouble() * 10.0, 2.2));
                }
                for (int i = chimneySmokes.size() - 1; i >= 0; i--) {
                    SmokePuff sp = chimneySmokes.get(i);
                    sp.age += dt;
                    if (sp.age >= sp.maxAge) {
                        dynamicFxPane.getChildren().remove(sp.node);
                        chimneySmokes.remove(i);
                    } else {
                        double p = sp.age / sp.maxAge;
                        sp.x += sp.vx * dt;
                        sp.y += sp.vy * dt;
                        sp.node.setLayoutX(sp.x + Math.sin(sp.age * 3.0) * 4.0);
                        sp.node.setLayoutY(sp.y);
                        double sc = 0.8 + p * 1.4;
                        sp.node.setScaleX(sc); sp.node.setScaleY(sc);
                        sp.node.setOpacity((1.0 - p) * 0.68);
                    }
                }

                // 4. Lá pixel rơi nhẹ & xoay theo gió
                for (LeafActor l : fallingLeaves) {
                    l.x += l.vx * dt;
                    l.y += l.vy * dt;
                    if (l.y > H + 16 || l.x > W + 24) {
                        l.x = rng.nextDouble() * W * 0.9;
                        l.y = -12.0 - rng.nextDouble() * 40.0;
                    }
                    l.node.setLayoutX(l.x + Math.sin(elapsedSec * 1.8 + l.swayPhase) * l.swayAmp);
                    l.node.setLayoutY(l.y);
                    l.node.setRotate(l.node.getRotate() + l.rotSpeed * dt);
                }

                // 5. Đom đóm & bướm bay lượn trên đồng cỏ
                for (FireflyActor f : fireflies) {
                    double fx = f.anchorX + Math.sin(elapsedSec * f.freqX + f.phase) * f.ampX;
                    double fy = f.anchorY + Math.cos(elapsedSec * f.freqY + f.phase * 1.3) * f.ampY;
                    f.node.setLayoutX(fx); f.node.setLayoutY(fy);
                    f.node.setOpacity(0.50 + 0.48 * Math.abs(Math.sin(elapsedSec * 2.4 + f.phase)));
                }

                // 6. Ánh lửa trại, mặt hồ & sao cấp độ lấp lánh
                if (campfireGlow != null) {
                    double pulse = 0.90 + 0.12 * Math.sin(elapsedSec * 6.5);
                    campfireGlow.setScaleX(pulse); campfireGlow.setScaleY(pulse * 0.85);
                    campfireGlow.setOpacity(0.76 + 0.20 * Math.sin(elapsedSec * 9.0));
                }
                for (int i = 0; i < pondGlints.size(); i++) {
                    pondGlints.get(i).setOpacity(0.28 + 0.45 * Math.abs(Math.sin(elapsedSec * 1.8 + i * 1.1)));
                }
                if (profileStarShape != null) {
                    double sp = 1.0 + 0.05 * Math.sin(elapsedSec * 3.6);
                    profileStarShape.setScaleX(sp); profileStarShape.setScaleY(sp);
                }
            }
        };
        backdropTimer.start();
    }

    // ==================== CÁC HÀM CHỨC NĂNG GỐC GIỮ NGUYÊN ====================

    @FXML
    private void onContinue() {
        if (launcher != null && continueEnabled && !transitionRunning) {
            playGateFadeAndRun(() -> {
                try {
                    launcher.continueFarm();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void onStartNew() {
        openCreateFarmModal();
    }

    @FXML
    private void onHelp() {
        if (router != null) {
            try {
                router.show(Screens.HELP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onQuit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Thoát Smart Farm");
        alert.setHeaderText("Bạn có chắc muốn thoát trò chơi không?");
        alert.setContentText("Mọi tiến độ chưa lưu sẽ không được giữ lại.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            SoundHook.stopMenuBgm();
            if (launcher != null) {
                launcher.quit();
            } else {
                Platform.exit();
                System.exit(0);
            }
        }
    }
}
