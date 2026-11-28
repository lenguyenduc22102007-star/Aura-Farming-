package view.river;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Module quản lý chiếc thuyền gỗ Pixel Art neo đậu ven sông:
 * - Tách 2 lớp Z-ordering (boatBackLayer & boatFrontLayer) để nhân vật đứng lọt bên trong khoang thuyền,
 *   nằm trên sàn gỗ và sau lan can trước + cột buồm + cánh buồm.
 * - Hoạt ảnh dập dềnh mượt mà theo hàm sin kết hợp gợn sóng pixel trắng/xanh nhạt quanh thân thuyền.
 * - Quản lý cầu tàu gỗ (dock) đi từ bờ bắc xuống sát mạn thuyền và logic Lên/Xuống thuyền mượt mà.
 */
public class BoatEntity {

    // Kích thước chuẩn khớp sprite pixel-art đã tách lớp (view/assets/river/)
    public static final double BOAT_WIDTH  = 186.0;
    public static final double BOAT_HEIGHT = 153.0;
    public static final double DOCK_WIDTH  = 72.0;
    public static final double DOCK_HEIGHT = 86.0;

    // Vị trí neo đậu mặc định theo tọa độ thế giới (CELL_SIZE = 64 -> sông hàng 32..36 = y 2048..2368, cầu x = 1048..1392)
    public static final double DEFAULT_BOAT_X = 754.0;
    public static final double DEFAULT_BOAT_Y = 2032.0;
    public static final double DEFAULT_DOCK_X = 766.0;
    public static final double DEFAULT_DOCK_Y = 2022.0;

    // Độ lệch tọa độ nhân vật (playerX, playerY) khi đứng trong khoang sau thuyền (khớp sprite 108x108)
    // Luffy sprite tâm ngang = playerX + 22; vị trí khoang sau thuyền = boatX + 44 -> playerX = boatX + 22
    // Luffy dép = playerY + 68; độ sâu lọt sau lan can trước = boatY + 114 -> playerY = boatY + 46
    private static final double PLAYER_OFFSET_X = 22.0;
    private static final double PLAYER_OFFSET_Y = 46.0;

    // Độ sâu sắp xếp 2.5D (UserData) trong entityPane
    private static final double DOCK_SORT_Y   = -950.0;
    private static final double RIPPLE_SORT_Y = -880.0;
    private static final double PROMPT_SORT_Y = 9999.0;

    // Tham số dập dềnh theo sóng nước & chèo thuyền
    private static final double BOB_AMPLITUDE = 2.6;
    private static final double BOB_SPEED     = 2.2;
    private static final double ROW_SPEED     = 168.0; // px/giây khi chèo bằng A/D

    // Thời gian chuyển tiếp mượt khi bước lên/xuống thuyền và cooldown chống bật ngược
    private static final long TRANSITION_MS = 170L;
    private static final long COOLDOWN_MS   = 380L;

    private double boatX;
    private final double boatY;
    private final double dockX;
    private final double dockY;
    private final double boatSortY;
    private boolean boatFacingLeft = false;
    private boolean isRowing = false;

    // Các vùng va chạm / tương tác
    private final Rectangle2D walkableDockBounds;
    private final Rectangle2D boardingZoneBounds;
    private Rectangle2D boatSolidBounds;

    // Các node hiển thị (add trực tiếp vào entityPane để tự động depth-sort cùng nhân vật)
    private final ImageView dockView;
    private final Group rippleLayer;
    private final Group boatBackLayer;
    private final Group boatFrontLayer;
    private final StackPane promptBubble;
    private final Label promptLabel;

    // Danh sách các pixel gợn sóng quanh thân thuyền
    private final List<WaterRipplePixel> ripples = new ArrayList<>();

    // Trạng thái hiện tại
    private double bobOffsetY = 0.0;
    private boolean playerAboard = false;
    private boolean transitioning = false;
    private boolean transitionTargetAboard = false;
    private long transitionStartMs = 0L;
    private double transStartX, transStartY;
    private double transEndX, transEndY;
    private long lastBoardToggleMs = 0L;

    private static class WaterRipplePixel {
        final Rectangle rect;
        final double baseX;
        final double baseY;
        final double baseWidth;
        final double driftX;
        final double driftY;
        final double phase;
        final double speed;

        WaterRipplePixel(Rectangle rect, double baseX, double baseY, double baseWidth,
                         double driftX, double driftY, double phase, double speed) {
            this.rect = rect;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseWidth = baseWidth;
            this.driftX = driftX;
            this.driftY = driftY;
            this.phase = phase;
            this.speed = speed;
        }
    }

    public BoatEntity() {
        this(DEFAULT_BOAT_X, DEFAULT_BOAT_Y, DEFAULT_DOCK_X, DEFAULT_DOCK_Y);
    }

    public BoatEntity(double boatX, double boatY, double dockX, double dockY) {
        this.boatX = boatX;
        this.boatY = boatY;
        this.dockX = dockX;
        this.dockY = dockY;
        // Mốc Z-order: boatBack = boatSortY - 2, Luffy trên thuyền = boatSortY, boatFront = boatSortY + 2
        this.boatSortY = boatY + 108.0;

        // Vùng sàn gỗ cầu tàu cho phép đi bộ qua mặt nước ven bờ (mở rộng nhẹ lên bờ cỏ 16px để bước mượt)
        this.walkableDockBounds = new Rectangle2D(dockX + 4, dockY - 16, DOCK_WIDTH - 8, DOCK_HEIGHT + 22);

        // Vùng tương tác ở nửa cuối cầu tàu (sát mạn thuyền)
        this.boardingZoneBounds = new Rectangle2D(dockX - 8, dockY + 26, DOCK_WIDTH + 16, DOCK_HEIGHT - 12);

        // Hitbox cứng của thân thuyền khi đang ở dưới bờ (ngăn đi xuyên thuyền từ hai bên)
        this.boatSolidBounds = new Rectangle2D(boatX + 10, boatY + 86, BOAT_WIDTH - 20, 62);

        // 1. Cầu tàu gỗ (dock)
        Image dockImg = loadImg("/view/assets/river/boat_dock.png");
        dockView = new ImageView(dockImg);
        dockView.setFitWidth(DOCK_WIDTH);
        dockView.setFitHeight(DOCK_HEIGHT);
        dockView.setSmooth(false);
        dockView.setLayoutX(dockX);
        dockView.setLayoutY(dockY);
        dockView.setUserData(DOCK_SORT_Y);

        // 2. Lớp gợn sóng pixel quanh thân thuyền
        rippleLayer = new Group();
        rippleLayer.setManaged(false);
        rippleLayer.setUserData(RIPPLE_SORT_Y);
        buildWaterRipples();

        // 3. Lớp sau thuyền (sàn gỗ khoang thuyền + lan can sau + dây buồm sau) -> vẽ DƯỚI nhân vật
        Image backImg = loadImg("/view/assets/river/boat_back.png");
        if (backImg == null) backImg = view.assets.Assets.BOAT_IMG;
        ImageView backView = new ImageView(backImg);
        backView.setFitWidth(BOAT_WIDTH);
        backView.setFitHeight(BOAT_HEIGHT);
        backView.setSmooth(false);
        boatBackLayer = new Group(backView);
        boatBackLayer.setManaged(false);
        boatBackLayer.setLayoutX(boatX);
        boatBackLayer.setLayoutY(boatY);
        boatBackLayer.setUserData(boatSortY - 2.0);

        // 4. Lớp trước thuyền (mạn gỗ trước + cột buồm + cánh buồm trắng + dây buộc trước) -> vẽ TRÊN nhân vật
        Image frontImg = loadImg("/view/assets/river/boat_front.png");
        ImageView frontView = new ImageView(frontImg);
        frontView.setFitWidth(BOAT_WIDTH);
        frontView.setFitHeight(BOAT_HEIGHT);
        frontView.setSmooth(false);
        boatFrontLayer = new Group(frontView);
        boatFrontLayer.setManaged(false);
        boatFrontLayer.setLayoutX(boatX);
        boatFrontLayer.setLayoutY(boatY);
        boatFrontLayer.setUserData(boatSortY + 2.0);

        // 5. Nhãn gợi ý nhỏ gọn phong cách pixel khi người chơi đứng gần cầu tàu hoặc đang ở trên thuyền
        promptLabel = new Label("[E / ↓] Lên thuyền");
        promptLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF8E7;"
        );
        promptBubble = new StackPane(promptLabel);
        promptBubble.setAlignment(Pos.CENTER);
        promptBubble.setPadding(new Insets(3, 8, 3, 8));
        promptBubble.setStyle(
            "-fx-background-color: rgba(38, 24, 16, 0.88);" +
            "-fx-border-color: #E8C07D;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 4px;" +
            "-fx-border-radius: 4px;"
        );
        promptBubble.setManaged(false);
        promptBubble.setVisible(false);
        promptBubble.setLayoutX(boatX + 18);
        promptBubble.setLayoutY(boatY - 22);
        promptBubble.setUserData(PROMPT_SORT_Y);
    }

    private Image loadImg(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Khởi tạo các vệt bọt & gợn sóng pixel trắng/xanh nhạt ôm quanh mạn dưới của thân thuyền.
     */
    private void buildWaterRipples() {
        // Đường chân đáy thuyền nằm tại y = boatY + 148..153, x = boatX + 12..155
        double waterLineY = boatY + BOAT_HEIGHT - 3.0;

        // Các vệt bọt ngang sát mạn đáy thuyền (trắng sáng & xanh băng nhạt)
        double[][] specs = {
            // {dx, dy, width, height, driftX, driftY, phase, speed, isWhite}
            {  16,  0, 26, 3, -6,  3, 0.00, 1.35, 1 },
            {  54,  2, 34, 3,  0,  4, 1.15, 1.25, 1 },
            { 102,  1, 30, 3,  5,  3, 2.30, 1.30, 1 },
            { 138, -4, 18, 3,  8,  2, 3.45, 1.50, 1 },
            {   4, -7, 15, 3, -8,  2, 4.10, 1.45, 0 },
            // Vòng gợn sóng mở rộng mềm phía dưới đáy thuyền (xanh sáng nhạt)
            {  28,  7, 42, 3, -4,  6, 0.65, 1.10, 0 },
            {  84,  8, 46, 3,  4,  6, 2.05, 1.10, 0 },
            {  52, 13, 36, 2,  0,  7, 3.60, 1.00, 0 },
            // Chấm bọt nhỏ li ti sủi ở mũi và đuôi thuyền
            {  10,  4,  6, 3, -7,  4, 1.80, 1.65, 1 },
            { 148,  2,  6, 3,  9,  4, 5.10, 1.65, 1 }
        };

        for (double[] s : specs) {
            Rectangle r = new Rectangle(s[2], s[3]);
            r.setFill(s[8] > 0.5 ? Color.rgb(236, 252, 255, 0.82) : Color.rgb(154, 232, 248, 0.65));
            // Lưu offset tương đối so với boatX để gợn sóng di chuyển mượt cùng thân thuyền khi chèo
            double relX = s[0];
            double by = waterLineY + s[1];
            r.setLayoutX(boatX + relX);
            r.setLayoutY(by);
            rippleLayer.getChildren().add(r);
            ripples.add(new WaterRipplePixel(r, relX, by, s[2], s[4], s[5], s[6], s[7]));
        }
    }

    /**
     * Gắn toàn bộ các layer của thuyền vào entityPane.
     */
    public void attachTo(Pane entityPane) {
        if (entityPane == null) return;
        List<Node> nodes = getNodes();
        for (Node n : nodes) {
            if (!entityPane.getChildren().contains(n)) {
                entityPane.getChildren().add(n);
            }
        }
    }

    public List<Node> getNodes() {
        List<Node> list = new ArrayList<>(5);
        list.add(dockView);
        list.add(rippleLayer);
        list.add(boatBackLayer);
        list.add(boatFrontLayer);
        list.add(promptBubble);
        return list;
    }

    /**
     * Cập nhật hoạt ảnh dập dềnh theo trục Y (hàm sin) và gợn sóng pixel mỗi frame.
     */
    public void update(double timeSeconds, double playerX, double playerY) {
        // Dập dềnh cực kỳ nhẹ nhàng và tự nhiên (sóng chính + hòa âm nhỏ)
        bobOffsetY = Math.sin(timeSeconds * BOB_SPEED) * BOB_AMPLITUDE
                   + Math.sin(timeSeconds * BOB_SPEED * 2.0 + 0.7) * 0.45;

        boatBackLayer.setTranslateY(bobOffsetY);
        boatFrontLayer.setTranslateY(bobOffsetY);

        // Cập nhật từng vệt bọt & gợn sóng sủi quanh thân thuyền (di chuyển đồng bộ theo boatX)
        double rippleRate = isRowing ? 0.72 : 0.45;
        for (WaterRipplePixel wp : ripples) {
            double t = ((timeSeconds * wp.speed * rippleRate) + wp.phase / (Math.PI * 2.0)) % 1.0;
            if (t < 0) t += 1.0;

            double expand = 1.0 + 0.35 * t;
            double newW = Math.round(wp.baseWidth * expand);
            wp.rect.setWidth(newW);
            double worldBaseX = boatX + wp.baseX;
            wp.rect.setLayoutX(Math.round(worldBaseX - (newW - wp.baseWidth) * 0.5 + wp.driftX * t));
            wp.rect.setLayoutY(Math.round(wp.baseY + wp.driftY * t + bobOffsetY * 0.35));

            double alpha = Math.sin(t * Math.PI);
            wp.rect.setOpacity(0.15 + 0.80 * alpha);
        }

        // Hiển thị nhãn gợi ý tinh tế khi người chơi đang ở gần thuyền/cầu tàu hoặc đang đứng trên thuyền
        if (playerAboard && !transitioning) {
            if (!suppressPrompt) {
                if (underBridgeShadeRatio > 0.35) {
                    promptLabel.setText("Đang chui dưới gầm cầu  |  [A/D] Chèo qua");
                } else {
                    promptLabel.setText("[A/D] Chèo  |  [Space/F] Câu cá  |  [E] Rời thuyền");
                }
                promptBubble.setVisible(true);
                promptBubble.setLayoutX(boatX - 58);
                promptBubble.setLayoutY(boatY - 26 + bobOffsetY);
            } else {
                promptBubble.setVisible(false);
            }
        } else if (!playerAboard && !transitioning && canInteractFromShore(playerX, playerY)) {
            promptLabel.setText("[E] Lên thuyền");
            promptBubble.setVisible(true);
            promptBubble.setLayoutX(boatX + 36);
            promptBubble.setLayoutY(boatY - 18 + Math.sin(timeSeconds * 3.2) * 2.0);
        } else {
            promptBubble.setVisible(false);
        }
        isRowing = false;
    }

    /**
     * Chèo thuyền di chuyển mượt theo trục X bằng A / D (hoặc LEFT / RIGHT), tự động lật hướng thuyền.
     */
    public void rowHorizontal(double dirX, double dtSec, double mapWidth) {
        if (Math.abs(dirX) < 0.01) {
            isRowing = false;
            return;
        }
        isRowing = true;
        double minX = 40.0;
        double maxX = Math.max(minX, mapWidth - BOAT_WIDTH - 40.0);
        boatX = Math.max(minX, Math.min(maxX, boatX + dirX * ROW_SPEED * dtSec));

        boatFacingLeft = (dirX < 0);
        double sx = boatFacingLeft ? -1.0 : 1.0;
        boatBackLayer.setScaleX(sx);
        boatFrontLayer.setScaleX(sx);
        boatBackLayer.setLayoutX(boatX);
        boatFrontLayer.setLayoutX(boatX);

        boatSolidBounds = new Rectangle2D(boatX + 10, boatY + 86, BOAT_WIDTH - 20, 62);
    }

    public boolean isBoatFacingLeft() {
        return boatFacingLeft;
    }

    private boolean suppressPrompt = false;
    private double underBridgeShadeRatio = 0.0;
    private final javafx.scene.effect.ColorAdjust underBridgeColorAdjust = new javafx.scene.effect.ColorAdjust();

    public void setSuppressPrompt(boolean suppress) {
        this.suppressPrompt = suppress;
        if (suppress) {
            promptBubble.setVisible(false);
        }
    }

    /**
     * Tạo cảm giác bóng râm khi thuyền chui dưới gầm cầu (ratio: 0.0 ngoài nắng -> 1.0 giữa gầm cầu).
     */
    public void setUnderBridgeShade(double ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        if (Math.abs(clamped - underBridgeShadeRatio) < 0.015) return;
        underBridgeShadeRatio = clamped;
        if (clamped <= 0.01) {
            boatBackLayer.setEffect(null);
            boatFrontLayer.setEffect(null);
        } else {
            underBridgeColorAdjust.setBrightness(-0.38 * clamped);
            underBridgeColorAdjust.setSaturation(-0.18 * clamped);
            if (boatBackLayer.getEffect() != underBridgeColorAdjust) {
                boatBackLayer.setEffect(underBridgeColorAdjust);
            }
            if (boatFrontLayer.getEffect() != underBridgeColorAdjust) {
                boatFrontLayer.setEffect(underBridgeColorAdjust);
            }
        }
    }

    public double getUnderBridgeShadeRatio() {
        return underBridgeShadeRatio;
    }

    /** Kiểm tra người chơi đứng trên cầu tàu HOẶC ở sát bờ trong phạm vi < 80px quanh thuyền. */
    public boolean canInteractFromShore(double playerX, double playerY) {
        if (isPlayerInBoardingZone(playerX, playerY)) return true;
        double px = playerX + 22.0;
        double py = playerY + 54.0;
        double nearX = Math.max(boatX, Math.min(px, boatX + BOAT_WIDTH));
        double nearY = Math.max(boatY + 40.0, Math.min(py, boatY + BOAT_HEIGHT));
        return Math.hypot(px - nearX, py - nearY) < 80.0;
    }

    /**
     * Bắt đầu bước lên thuyền (có nội suy mượt ngắn để nhân vật bước vào khoang gọn gàng).
     */
    public boolean startBoarding(double currentPlayerX, double currentPlayerY) {
        long now = System.currentTimeMillis();
        if (playerAboard || transitioning || (now - lastBoardToggleMs) < COOLDOWN_MS) {
            return false;
        }
        transitioning = true;
        transitionTargetAboard = true;
        transitionStartMs = now;
        lastBoardToggleMs = now;
        transStartX = currentPlayerX;
        transStartY = currentPlayerY;
        transEndX = getBoardedPlayerX();
        transEndY = getBoardedPlayerY();
        return true;
    }

    /**
     * Bắt đầu rời thuyền bước trở lại cầu tàu / đất liền.
     */
    public boolean startDisembarking() {
        long now = System.currentTimeMillis();
        if (!playerAboard || transitioning || (now - lastBoardToggleMs) < COOLDOWN_MS) {
            return false;
        }
        transitioning = true;
        transitionTargetAboard = false;
        transitionStartMs = now;
        lastBoardToggleMs = now;
        transStartX = getBoardedPlayerX();
        transStartY = getBoardedPlayerY() + bobOffsetY;
        transEndX = getDisembarkPlayerX();
        transEndY = getDisembarkPlayerY();
        return true;
    }

    /**
     * Tính tọa độ nhân vật trong khi đang chuyển tiếp bước lên/xuống thuyền (smoothstep).
     * Trả về mảng [x, y], và tự động hoàn tất chuyển trạng thái khi hết TRANSITION_MS.
     */
    public double[] stepTransition() {
        long elapsed = System.currentTimeMillis() - transitionStartMs;
        double p = Math.min(1.0, Math.max(0.0, (double) elapsed / TRANSITION_MS));
        // Smoothstep easing cho chuyển động tự nhiên
        double eased = p * p * (3.0 - 2.0 * p);

        double targetEndY = transitionTargetAboard ? (transEndY + bobOffsetY) : transEndY;
        double x = transStartX + (transEndX - transStartX) * eased;
        double y = transStartY + (targetEndY - transStartY) * eased;

        if (p >= 1.0) {
            transitioning = false;
            playerAboard = transitionTargetAboard;
            lastBoardToggleMs = System.currentTimeMillis();
        }
        return new double[] { x, y };
    }

    /**
     * Kiểm tra ô bàn chân nhân vật có nằm trọn trên sàn cầu tàu gỗ hay không (cho phép đi qua tile nước ven bờ).
     */
    public boolean isOnWalkableDock(Rectangle2D playerFootBox) {
        return playerFootBox.getMinX() >= walkableDockBounds.getMinX()
                && playerFootBox.getMaxX() <= walkableDockBounds.getMaxX()
                && playerFootBox.getMaxY() >= walkableDockBounds.getMinY()
                && playerFootBox.getMaxY() <= walkableDockBounds.getMaxY();
    }

    /**
     * Kiểm tra nhân vật có chạm hitbox thân thuyền từ đất liền/cầu tàu hay không.
     */
    public boolean collidesWithHull(Rectangle2D playerFootBox) {
        return boatSolidBounds.intersects(playerFootBox);
    }

    /**
     * Kiểm tra nhân vật đang đứng ở cuối cầu tàu sát mạn thuyền (đủ điều kiện lên thuyền).
     */
    public boolean isPlayerInBoardingZone(double playerX, double playerY) {
        double footX = playerX + 22.0;
        double footY = playerY + 54.0;
        return boardingZoneBounds.contains(footX, footY);
    }

    /**
     * Kiểm tra nhân vật đang ở rất sát mép cuối cầu tàu để tự động bước lên khi bấm xuống (S / DOWN).
     */
    public boolean isAtDockEdgeForWalkBoard(double playerX, double playerY) {
        double footX = playerX + 22.0;
        double footY = playerY + 54.0;
        return footX >= dockX && footX <= (dockX + DOCK_WIDTH)
            && footY >= (dockY + DOCK_HEIGHT - 22.0) && footY <= (dockY + DOCK_HEIGHT + 14.0);
    }

    public boolean canToggleNow() {
        return !transitioning && (System.currentTimeMillis() - lastBoardToggleMs) >= COOLDOWN_MS;
    }

    public boolean isPlayerAboard() {
        return playerAboard;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public boolean isLockedToBoat() {
        return playerAboard || transitioning;
    }

    /** Tọa độ X khóa (bind) của nhân vật khi đứng trên sàn thuyền (tự căn khớp khoang sau khi lật hướng thuyền). */
    public double getBoardedPlayerX() {
        double cockpitCenterOffset = boatFacingLeft ? (BOAT_WIDTH - 44.0) : 44.0;
        return boatX + cockpitCenterOffset - 22.0;
    }

    /** Tọa độ Y gốc (chưa cộng bobOffsetY) của nhân vật khi đứng trên sàn thuyền. */
    public double getBoardedPlayerY() {
        return boatY + PLAYER_OFFSET_Y;
    }

    /** Tọa độ Y thực tế đã đồng bộ 100% với nhịp dập dềnh của thuyền. */
    public double getSyncedPlayerY() {
        return getBoardedPlayerY() + bobOffsetY;
    }

    /** Vị trí an toàn trên cầu tàu (nếu gần cầu tàu) hoặc trực tiếp lên bờ bắc ngay phía trên thuyền. */
    public double getDisembarkPlayerX() {
        double boatCenterX = boatX + BOAT_WIDTH * 0.5;
        double dockCenterX = dockX + DOCK_WIDTH * 0.5;
        if (Math.abs(boatCenterX - dockCenterX) < 130.0) {
            return dockCenterX - 22.0;
        }
        return boatCenterX - 22.0;
    }

    public double getDisembarkPlayerY() {
        double boatCenterX = boatX + BOAT_WIDTH * 0.5;
        double dockCenterX = dockX + DOCK_WIDTH * 0.5;
        if (Math.abs(boatCenterX - dockCenterX) < 130.0) {
            return dockY - 12.0;
        }
        // Bờ cỏ bắc nằm ngay phía trên viền cỏ + mô đất nâu (boatY + 16 = riverTopY; đáy dép tại y + 68 -> riverTopY - 8 <= riverTopY - 3.2)
        return boatY - 60.0;
    }

    public double getBobOffsetY() {
        return bobOffsetY;
    }

    /** Giá trị UserData Z-order dành cho nhân vật khi đứng trong khoang thuyền (giữa boatBack và boatFront). */
    public double getBoardedPlayerSortY() {
        return boatSortY;
    }

    public double getBoatX() { return boatX; }
    public double getBoatY() { return boatY; }
    public double getDockX() { return dockX; }
    public double getDockY() { return dockY; }

    /** Điểm xuất phát cần câu (tự đảo theo hướng chèo của thuyền). */
    public double getRodOriginX() {
        return boatFacingLeft ? (boatX + BOAT_WIDTH - 54.0) : (boatX + 54.0);
    }

    public double getRodOriginY() {
        return boatY + 86.0 + bobOffsetY;
    }
}
