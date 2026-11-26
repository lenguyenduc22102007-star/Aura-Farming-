package view.river;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Module hiệu ứng dòng nước chảy pixel-art cho khu vực con sông (hàng 32-36, y = 1280..1480):
 * - Các vệt gợn sóng ngang trắng & xanh nhạt bán trong suốt trôi liên tục từ trái sang phải.
 * - Các vệt bọt nước nhỏ ven hai bờ sông.
 * - Đặt dưới cầu gỗ (-1000), cầu tàu (-950), thuyền và nhân vật.
 */
public class RiverFlow {

    public static final double RIVER_MIN_X = 0.0;
    public static final double RIVER_MAX_X = 2560.0; // 40 * CELL_SIZE (64)
    public static final double RIVER_TOP_Y = 2048.0; // Hàng 32 * 64
    public static final double RIVER_BOT_Y = 2368.0; // Hết hàng 36 * 64

    private static final double LAYER_SORT_Y = -1020.0;

    private final double maxX;
    private final Pane layer;
    private final List<WaveStreak> streaks = new ArrayList<>();
    private final List<FoamBubble> bubbles = new ArrayList<>();
    private final List<BubbleSpot> bubbleSpots = new ArrayList<>();

    private static class BubbleSpot {
        final double x, y, radius;
        final Circle ring1, ring2;
        final double phase;

        BubbleSpot(double x, double y, double radius, Circle ring1, Circle ring2, double phase) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.ring1 = ring1;
            this.ring2 = ring2;
            this.phase = phase;
        }
    }

    private static class WaveStreak {
        final Rectangle rect;
        double x;
        final double baseY;
        final double speed;
        final double width;
        final double phase;
        final double baseOpacity;

        WaveStreak(Rectangle rect, double x, double baseY, double speed, double width, double phase, double baseOpacity) {
            this.rect = rect;
            this.x = x;
            this.baseY = baseY;
            this.speed = speed;
            this.width = width;
            this.phase = phase;
            this.baseOpacity = baseOpacity;
        }
    }

    private static class FoamBubble {
        final Circle circle;
        double x;
        final double y;
        final double speed;
        final double phase;

        FoamBubble(Circle circle, double x, double y, double speed, double phase) {
            this.circle = circle;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.phase = phase;
        }
    }

    public RiverFlow() {
        this(RIVER_MAX_X, RIVER_TOP_Y, RIVER_BOT_Y);
    }

    public RiverFlow(double mapWidth, double riverTopY, double riverBotY) {
        this.maxX = Math.max(400.0, mapWidth);
        layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setUserData(LAYER_SORT_Y);

        Random rng = new Random(3236L);
        double riverH = Math.max(40.0, riverBotY - riverTopY);

        // 1. 64 vệt gợn sóng ngang pixel trôi liên tục từ trái sang phải dọc con sông
        for (int i = 0; i < 64; i++) {
            boolean bright = (i % 3 == 0);
            double w = bright ? (14 + rng.nextInt(18)) : (22 + rng.nextInt(28));
            double h = bright ? 3.0 : 3.5;
            double x = rng.nextDouble() * this.maxX;
            double y = riverTopY + 14.0 + rng.nextDouble() * (riverH - 28.0);
            double speed = 22.0 + rng.nextDouble() * 26.0; // px/giây
            double phase = rng.nextDouble() * Math.PI * 2.0;
            double baseAlpha = bright ? 0.55 : 0.34;

            Rectangle r = new Rectangle(w, h);
            r.setFill(bright ? Color.rgb(238, 252, 255) : Color.rgb(165, 236, 250));
            r.setOpacity(baseAlpha);
            r.setLayoutX(x);
            r.setLayoutY(y);
            layer.getChildren().add(r);
            streaks.add(new WaveStreak(r, x, y, speed, w, phase, baseAlpha));
        }

        // 2. 26 vệt bọt trắng nhỏ sát mép bờ bắc & bờ nam
        for (int i = 0; i < 26; i++) {
            boolean northBank = (i % 2 == 0);
            double w = 8 + rng.nextInt(12);
            double x = rng.nextDouble() * this.maxX;
            double y = northBank
                ? (riverTopY + 4.0 + rng.nextDouble() * 10.0)
                : (riverBotY - 14.0 + rng.nextDouble() * 10.0);
            double speed = 13.0 + rng.nextDouble() * 12.0;
            double phase = rng.nextDouble() * Math.PI * 2.0;

            Rectangle r = new Rectangle(w, 3.0);
            r.setFill(Color.rgb(244, 253, 255));
            r.setOpacity(0.48);
            r.setLayoutX(x);
            r.setLayoutY(y);
            layer.getChildren().add(r);
            streaks.add(new WaveStreak(r, x, y, speed, w, phase, 0.48));
        }

        // 3. 18 bong bóng nước tròn nhỏ li ti nổi lấp lánh trên mặt sông
        for (int i = 0; i < 18; i++) {
            double x = rng.nextDouble() * this.maxX;
            double y = riverTopY + 24.0 + rng.nextDouble() * (riverH - 48.0);
            double radius = 2.4 + rng.nextDouble() * 2.6;
            Circle c = new Circle(radius);
            c.setFill(Color.TRANSPARENT);
            c.setStroke(Color.rgb(225, 248, 255, 0.55));
            c.setStrokeWidth(1.6);
            c.setLayoutX(x);
            c.setLayoutY(y);
            layer.getChildren().add(c);
            bubbles.add(new FoamBubble(c, x, y, 14.0 + rng.nextDouble() * 12.0, rng.nextDouble() * Math.PI * 2.0));
        }

        // 4. 3 Vùng Nước Sủi Bọt lấp lánh dọc con sông (tăng tỉ lệ xuất hiện bóng cá Size 4-5!)
        double[][] spotCoords = {
            { this.maxX * 0.22, riverTopY + riverH * 0.66, 76.0 },
            { this.maxX * 0.62, riverTopY + riverH * 0.60, 78.0 },
            { this.maxX * 0.84, riverTopY + riverH * 0.64, 76.0 }
        };
        for (int i = 0; i < spotCoords.length; i++) {
            double sx = spotCoords[i][0];
            double sy = spotCoords[i][1];
            double sr = spotCoords[i][2];

            Circle r1 = new Circle(22);
            r1.setFill(Color.rgb(185, 246, 255, 0.16));
            r1.setStroke(Color.rgb(235, 253, 255, 0.72));
            r1.setStrokeWidth(2.2);
            r1.setLayoutX(sx);
            r1.setLayoutY(sy);

            Circle r2 = new Circle(34);
            r2.setFill(Color.TRANSPARENT);
            r2.setStroke(Color.rgb(160, 242, 255, 0.55));
            r2.setStrokeWidth(1.8);
            r2.setLayoutX(sx);
            r2.setLayoutY(sy);

            layer.getChildren().addAll(r2, r1);
            bubbleSpots.add(new BubbleSpot(sx, sy, sr, r1, r2, i * 2.1));
        }
    }

    /** Kiểm tra tọa độ (x, y) có nằm trong Vùng Nước Sủi Bọt hay không. */
    public boolean isInBubbleZone(double worldX, double worldY) {
        for (BubbleSpot bs : bubbleSpots) {
            if (Math.hypot(worldX - bs.x, worldY - bs.y) <= bs.radius) {
                return true;
            }
        }
        return false;
    }

    public void attachTo(Pane entityPane) {
        if (entityPane != null && !entityPane.getChildren().contains(layer)) {
            entityPane.getChildren().add(layer);
        }
    }

    public Node getNode() {
        return layer;
    }

    /**
     * Cập nhật vị trí các vệt sóng & bọt nước mỗi frame (deltaSeconds ~ 0.016s).
     */
    public void update(double deltaSeconds, double timeSeconds) {
        double dt = Math.min(0.05, Math.max(0.0, deltaSeconds));
        for (WaveStreak ws : streaks) {
            ws.x += ws.speed * dt;
            if (ws.x > maxX + 20.0) {
                ws.x = -ws.width - 10.0;
            }
            ws.rect.setLayoutX(Math.round(ws.x));
            ws.rect.setLayoutY(Math.round(ws.baseY + Math.sin(timeSeconds * 1.8 + ws.phase) * 1.4));
            ws.rect.setOpacity(ws.baseOpacity * (0.65 + 0.35 * Math.sin(timeSeconds * 2.1 + ws.phase)));
        }

        for (FoamBubble fb : bubbles) {
            fb.x += fb.speed * dt;
            if (fb.x > maxX + 12.0) {
                fb.x = -12.0;
            }
            double pulse = (Math.sin(timeSeconds * 2.4 + fb.phase) + 1.0) * 0.5;
            fb.circle.setLayoutX(fb.x);
            fb.circle.setLayoutY(fb.y + Math.sin(timeSeconds * 1.6 + fb.phase) * 1.5);
            fb.circle.setScaleX(0.75 + 0.55 * pulse);
            fb.circle.setScaleY(0.75 + 0.55 * pulse);
            fb.circle.setOpacity(0.15 + 0.55 * (1.0 - Math.abs(pulse - 0.5) * 1.6));
        }

        // Hoạt ảnh vòng sóng lan tỏa lấp lánh của Vùng Nước Sủi Bọt
        for (BubbleSpot bs : bubbleSpots) {
            double p1 = ((timeSeconds * 0.75) + bs.phase) % 1.0;
            bs.ring1.setRadius(14.0 + p1 * 34.0);
            bs.ring1.setOpacity(0.85 * (1.0 - p1));
            double p2 = ((timeSeconds * 0.75) + bs.phase + 0.5) % 1.0;
            bs.ring2.setRadius(14.0 + p2 * 46.0);
            bs.ring2.setOpacity(0.65 * (1.0 - p2));
        }
    }
}
