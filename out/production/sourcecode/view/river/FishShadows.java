package view.river;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hệ thống 7 Bóng Cá 5 Cấp Kích Thước (Size 1 nhỏ nhất -> Size 5 khổng lồ có vòng sáng):
 * - Size 1-2: Cá thường (Cá Rô, Cá Chép).
 * - Size 3: Cá vừa / Hiếm (Cá Chép Vàng, Lươn Mưa).
 * - Size 4: Cá lớn / Cực hiếm (Cá Đèn Lồng, Cá Trê Điện).
 * - Size 5: Khổng lồ có vòng sáng vàng kim nhẹ (Cổ Vật Đáy Sông / Cá Vương Miện).
 * - Hỗ trợ quay đầu bơi lại đối diện phao câu, động tác nhấp mồi giả và phóng nhanh biến mất khi bị giật hụt!
 */
public class FishShadows {

    private static final double LAYER_SORT_Y = -1010.0;
    private static final double[] SIZE_SCALES = { 0.0, 0.62, 0.90, 1.24, 1.58, 2.02 };

    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double mapWidth;

    private final Pane layer;
    private final List<FishShadow> fishes = new ArrayList<>();
    private final Random rng = new Random(7749L);

    // Trạng thái thu hút cá về phía phao câu
    private boolean lureActive = false;
    private double lureX, lureY;
    private FishShadow attractedFish = null;
    private boolean rareFishBuff = false;

    public void setRareFishBuff(boolean enabled) {
        this.rareFishBuff = enabled;
    }

    private static class FishShadow {
        final Group root;
        final Group tailGroup;
        final Circle auraGlow; // Chỉ hiện rõ ở bóng Size 4-5
        int sizeTier;          // 1..5
        double x, y;
        double angle;
        double speed;
        double baseSpeed;
        double turnTimer;
        double targetAngle;
        final double wagPhase;
        final double wagSpeed;
        double homeMinX;
        double homeMaxX;

        // Hiệu ứng nhấp mồi & phóng biến mất rồi respawn
        double nibbleLunge = 0.0;
        boolean fleeingVanish = false;
        double fleeTimer = 0.0;

        FishShadow(Group root, Group tailGroup, Circle auraGlow, int sizeTier,
                   double x, double y, double angle, double speed,
                   double wagPhase, double wagSpeed, double homeMinX, double homeMaxX) {
            this.root = root;
            this.tailGroup = tailGroup;
            this.auraGlow = auraGlow;
            this.sizeTier = sizeTier;
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.targetAngle = angle;
            this.speed = speed;
            this.baseSpeed = speed;
            this.wagPhase = wagPhase;
            this.wagSpeed = wagSpeed;
            this.homeMinX = homeMinX;
            this.homeMaxX = homeMaxX;
            this.turnTimer = 1.5;
        }
    }

    public FishShadows(double mapWidth, double riverTopY, double riverBotY, double boatX) {
        this.mapWidth = Math.max(800.0, mapWidth);
        this.minX = 48.0;
        this.maxX = Math.max(400.0, this.mapWidth - 48.0);
        this.minY = riverTopY + 28.0;
        this.maxY = riverBotY - 28.0;
        double riverH = Math.max(80.0, maxY - minY);

        layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setUserData(LAYER_SORT_Y);

        // Duy trì 7 bóng cá đủ cả 5 cấp kích thước dọc con sông (có sẵn bóng quanh thuyền & quanh vùng sủi bọt)
        int[] initialSizes = { 1, 2, 3, 4, 2, 3, 5 };
        double[][] spawnZones = {
            { Math.max(minX + 60, boatX - 110), minY + riverH * 0.62, Math.max(minX, boatX - 320), Math.min(maxX, boatX + 260) },
            { boatX + 90,                       minY + riverH * 0.78, Math.max(minX, boatX - 240), Math.min(maxX, boatX + 340) },
            { boatX + 220,                      minY + riverH * 0.56, Math.max(minX, boatX - 160), Math.min(maxX, boatX + 420) },
            { this.mapWidth * 0.22,             minY + riverH * 0.66, minX,                        this.mapWidth * 0.42 },
            { this.mapWidth * 0.52,             minY + riverH * 0.42, this.mapWidth * 0.36,        this.mapWidth * 0.74 },
            { this.mapWidth * 0.64,             minY + riverH * 0.68, this.mapWidth * 0.48,        this.mapWidth * 0.88 },
            { this.mapWidth * 0.82,             minY + riverH * 0.60, this.mapWidth * 0.58,        maxX }
        };

        for (int i = 0; i < initialSizes.length; i++) {
            double[] sp = spawnZones[i];
            FishShadow fish = createFishShadow(initialSizes[i], sp[0], sp[1], sp[2], sp[3], i);
            fishes.add(fish);
            layer.getChildren().add(fish.root);
        }
    }

    private FishShadow createFishShadow(int sizeTier, double x, double y,
                                        double homeMinX, double homeMaxX, int idx) {
        int tier = Math.max(1, Math.min(5, sizeTier));
        double scale = SIZE_SCALES[tier];

        // Vòng sáng nhẹ đặc trưng dưới bóng Size 5 (vàng kim) và Size 4 (xanh sáng nhẹ)
        Circle auraGlow = new Circle(0, 0, 20.0 * scale);
        if (tier == 5) {
            auraGlow.setFill(Color.rgb(255, 224, 96, 0.20));
            auraGlow.setStroke(Color.rgb(255, 236, 138, 0.58));
            auraGlow.setStrokeWidth(2.2);
            auraGlow.setVisible(true);
        } else if (tier == 4) {
            auraGlow.setFill(Color.rgb(92, 224, 255, 0.12));
            auraGlow.setStroke(Color.rgb(138, 236, 255, 0.36));
            auraGlow.setStrokeWidth(1.5);
            auraGlow.setVisible(true);
        } else {
            auraGlow.setVisible(false);
        }

        Color bodyColor = Color.rgb(10, 42, 60, tier >= 4 ? 0.50 : 0.42);
        Color finColor  = Color.rgb(8, 34, 50, 0.36);

        Ellipse body = new Ellipse(0, 0, 15 * scale, 5.4 * scale);
        body.setFill(bodyColor);

        Polygon topFin = new Polygon(2 * scale, -4 * scale, -5 * scale, -10 * scale, -7 * scale, -4 * scale);
        topFin.setFill(finColor);

        Polygon botFin = new Polygon(2 * scale, 4 * scale, -5 * scale, 10 * scale, -7 * scale, 4 * scale);
        botFin.setFill(finColor);

        Polygon tail = new Polygon(0, 0, -11 * scale, -6.5 * scale, -8.5 * scale, 0, -11 * scale, 6.5 * scale);
        tail.setFill(bodyColor);

        Group tailGroup = new Group(tail);
        tailGroup.setLayoutX(-13.0 * scale);
        tailGroup.setLayoutY(0);

        Group root = new Group(auraGlow, topFin, botFin, tailGroup, body);
        root.setManaged(false);
        root.setLayoutX(x);
        root.setLayoutY(y);

        double angle = (idx % 2 == 0) ? 0.12 : Math.PI - 0.12;
        double speed = (tier >= 4) ? (19.0 + rng.nextDouble() * 10.0) : (23.0 + rng.nextDouble() * 14.0);
        double wagPhase = rng.nextDouble() * Math.PI * 2.0;
        double wagSpeed = 6.2 + rng.nextDouble() * 2.4;
        root.setRotate(Math.toDegrees(angle));

        return new FishShadow(root, tailGroup, auraGlow, tier, x, y, angle, speed, wagPhase, wagSpeed, homeMinX, homeMaxX);
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
     * Thu hút bóng cá gần phao câu nhất quay đầu bơi lại đối diện phao.
     * Nếu thả vào Vùng Nước Sủi Bọt, có thể nâng cấp/triệu hồi bóng Size 4-5!
     * Trả về cấp kích thước (1..5) của bóng cá đang bơi lại.
     */
    public int attractNearestFish(double bobberX, double bobberY, boolean inBubbleZone) {
        this.lureActive = true;
        this.lureX = bobberX;
        this.lureY = bobberY;

        FishShadow best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (FishShadow f : fishes) {
            if (f.fleeingVanish) continue;
            double dx = f.x - bobberX;
            double dy = f.y - bobberY;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                best = f;
            }
        }

        // Nếu thả trúng Vùng Nước Sủi Bọt hoặc đang có Buff Câu Cá Hiếm (từ món ăn), tăng mạnh tỉ lệ lên bóng Size 4-5!
        if (best != null && best.sizeTier < 4) {
            double upgradeChance = inBubbleZone ? (rareFishBuff ? 0.88 : 0.65) : (rareFishBuff ? 0.45 : 0.0);
            if (upgradeChance > 0.0 && rng.nextDouble() < upgradeChance) {
                double tier5Chance = rareFishBuff ? 0.55 : 0.42;
                int upgradedTier = (rng.nextDouble() < tier5Chance) ? 5 : 4;
                best = replaceFishWithTier(best, upgradedTier, best.x, best.y);
            }
        }

        this.attractedFish = best;
        return (best != null) ? best.sizeTier : 2;
    }

    private FishShadow replaceFishWithTier(FishShadow oldFish, int newTier, double nx, double ny) {
        int idx = fishes.indexOf(oldFish);
        if (idx < 0) return oldFish;
        layer.getChildren().remove(oldFish.root);
        FishShadow fresh = createFishShadow(newTier, nx, ny, oldFish.homeMinX, oldFish.homeMaxX, idx);
        fishes.set(idx, fresh);
        layer.getChildren().add(fresh.root);
        return fresh;
    }

    /** Kích hoạt động tác chúi nhẹ đầu chạm phao khi cá nhấp mồi giả (Fake Bite). */
    public void playNibbleNudge() {
        if (attractedFish != null && !attractedFish.fleeingVanish) {
            attractedFish.nibbleLunge = 1.0;
        }
    }

    /** Cho bóng cá đang cắn phóng nhanh biến mất (khi giật sớm, giật chậm, đứt dây hoặc bắt xong) rồi respawn bóng mới. */
    public void vanishAttractedFish() {
        if (attractedFish != null) {
            attractedFish.fleeingVanish = true;
            attractedFish.fleeTimer = 0.0;
            attractedFish.angle += Math.PI + (rng.nextDouble() - 0.5) * 0.4;
            attractedFish.targetAngle = attractedFish.angle;
            attractedFish.speed = 195.0;
        }
        this.lureActive = false;
        this.attractedFish = null;
    }

    /** Hủy thu hút nhẹ nhàng (khi người chơi kéo phao lên sớm để chọn bóng khác). */
    public void releaseAttraction() {
        this.lureActive = false;
        this.attractedFish = null;
    }

    /** Kiểm tra bóng cá đã bơi tới đối diện sát phao chưa. */
    public boolean isAttractedFishFacingBobber(double thresholdPx) {
        if (!lureActive || attractedFish == null || attractedFish.fleeingVanish) return false;
        double dx = attractedFish.x - lureX;
        double dy = attractedFish.y - lureY;
        return (dx * dx + dy * dy) <= (thresholdPx * thresholdPx);
    }

    public int getAttractedFishSize() {
        return (attractedFish != null) ? attractedFish.sizeTier : 2;
    }

    private int rollRandomShadowTier() {
        double r = rng.nextDouble();
        if (r < 0.28) return 1;
        if (r < 0.56) return 2;
        if (r < 0.80) return 3;
        if (r < 0.93) return 4;
        return 5;
    }

    public void update(double deltaSeconds, double timeSeconds) {
        double dt = Math.min(0.05, Math.max(0.0, deltaSeconds));

        for (int i = 0; i < fishes.size(); i++) {
            FishShadow f = fishes.get(i);

            if (f.fleeingVanish) {
                f.fleeTimer += dt;
                f.x += Math.cos(f.angle) * f.speed * dt;
                f.y += Math.sin(f.angle) * f.speed * dt;
                f.root.setLayoutX(f.x);
                f.root.setLayoutY(f.y);
                f.root.setRotate(Math.toDegrees(f.angle));
                f.tailGroup.setRotate(Math.sin(timeSeconds * 26.0 + f.wagPhase) * 28.0);
                f.root.setOpacity(Math.max(0.0, 1.0 - f.fleeTimer / 0.55));

                if (f.fleeTimer >= 0.55) {
                    // Respawn 1 bóng cá mới (Size 1..5) ở vùng tuần tra để sông luôn có 7 bóng cá sống động
                    int newTier = rollRandomShadowTier();
                    double rx = f.homeMinX + 40.0 + rng.nextDouble() * Math.max(60.0, (f.homeMaxX - f.homeMinX - 80.0));
                    double ry = minY + 18.0 + rng.nextDouble() * Math.max(30.0, (maxY - minY - 36.0));
                    replaceFishWithTier(f, newTier, rx, ry);
                }
                continue;
            }

            boolean isChasingLure = (lureActive && f == attractedFish);

            if (isChasingLure) {
                double dx = lureX - f.x;
                double dy = lureY - f.y;
                double dist = Math.hypot(dx, dy);
                double desired = Math.atan2(dy, dx);
                f.angle = lerpAngle(f.angle, desired, Math.min(1.0, dt * 5.2));

                // Dừng đối diện sát phao (khoảng cách ~20 * scale), khi nhấp mồi giả thì chúi đầu chạm phao
                double stopDist = 14.0 + SIZE_SCALES[f.sizeTier] * 14.0;
                if (dist > stopDist) {
                    f.speed = Math.max(f.baseSpeed * 2.2, 68.0);
                } else {
                    f.speed = 0.0;
                }
            } else {
                f.speed += (f.baseSpeed - f.speed) * Math.min(1.0, dt * 1.8);
                f.turnTimer -= dt;
                if (f.turnTimer <= 0) {
                    f.turnTimer = 1.6 + rng.nextDouble() * 2.4;
                    f.targetAngle = f.angle + (rng.nextDouble() - 0.5) * 1.1;
                }

                boolean nearBound = false;
                double vx = Math.cos(f.angle);
                double vy = Math.sin(f.angle);

                if (f.y < minY + 14.0 && vy < 0) { vy = Math.abs(vy) + 0.35; nearBound = true; }
                else if (f.y > maxY - 14.0 && vy > 0) { vy = -Math.abs(vy) - 0.35; nearBound = true; }

                double leftBound = Math.max(minX, f.homeMinX);
                double rightBound = Math.min(maxX, f.homeMaxX);
                if (f.x < leftBound + 24.0 && vx < 0) { vx = Math.abs(vx) + 0.45; nearBound = true; }
                else if (f.x > rightBound - 24.0 && vx > 0) { vx = -Math.abs(vx) - 0.45; nearBound = true; }

                if (nearBound) {
                    f.targetAngle = Math.atan2(vy, vx);
                    f.turnTimer = 1.2;
                }
                f.angle = lerpAngle(f.angle, f.targetAngle, Math.min(1.0, dt * 2.8));
            }

            f.x += Math.cos(f.angle) * f.speed * dt;
            f.y += Math.sin(f.angle) * f.speed * dt;

            if (f.x < minX) f.x = minX;
            if (f.x > maxX) f.x = maxX;
            if (f.y < minY) f.y = minY;
            if (f.y > maxY) f.y = maxY;

            // Động tác chúi đầu nhấp mồi giả
            double lungeOffset = 0.0;
            if (f.nibbleLunge > 0.0) {
                f.nibbleLunge = Math.max(0.0, f.nibbleLunge - dt * 4.5);
                lungeOffset = Math.sin(f.nibbleLunge * Math.PI) * 14.0;
            }

            f.root.setLayoutX(f.x + Math.cos(f.angle) * lungeOffset);
            f.root.setLayoutY(f.y + Math.sin(f.angle) * lungeOffset);
            f.root.setRotate(Math.toDegrees(f.angle));

            double wagRate = f.wagSpeed * (0.7 + 0.5 * (f.speed / Math.max(1.0, f.baseSpeed)));
            f.tailGroup.setRotate(Math.sin(timeSeconds * wagRate + f.wagPhase) * 19.0);

            if (f.auraGlow.isVisible()) {
                double pulse = 0.88 + 0.16 * Math.sin(timeSeconds * 3.6 + f.wagPhase);
                f.auraGlow.setScaleX(pulse);
                f.auraGlow.setScaleY(pulse);
            }
        }
    }

    private static double lerpAngle(double current, double target, double t) {
        double diff = target - current;
        while (diff > Math.PI) diff -= Math.PI * 2.0;
        while (diff < -Math.PI) diff += Math.PI * 2.0;
        return current + diff * t;
    }
}
