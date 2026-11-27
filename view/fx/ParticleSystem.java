package view.fx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Hệ thống hiệu ứng hạt (ParticleSystem) nhẹ dùng chung cho toàn game:
 * - Tưới nước: giọt nước xanh bắn lên và rơi xuống theo trọng lực.
 * - Thu hoạch: lá xanh xoay nhẹ + tia sáng vàng lấp lánh.
 * - Vật nuôi: icon trái tim hồng/đỏ bay nhẹ lên trên và mờ dần (fade out).
 */
public class ParticleSystem {

    private static final double LAYER_SORT_Y = 9975.0;

    private final Pane layer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    private static final class Particle {
        final Node node;
        double x, y;
        double vx, vy;
        double gravity;
        double drag;
        double rotSpeed;
        double swayAmp;
        double swayFreq;
        double swayPhase;
        double age;
        final double maxLife;
        final double startScale;
        final double endScale;

        Particle(Node node, double x, double y, double vx, double vy,
                 double gravity, double drag, double rotSpeed,
                 double swayAmp, double swayFreq, double maxLife,
                 double startScale, double endScale) {
            this.node = node;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.gravity = gravity;
            this.drag = drag;
            this.rotSpeed = rotSpeed;
            this.swayAmp = swayAmp;
            this.swayFreq = swayFreq;
            this.swayPhase = Math.random() * Math.PI * 2.0;
            this.maxLife = maxLife;
            this.startScale = startScale;
            this.endScale = endScale;
        }
    }

    public ParticleSystem() {
        layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setUserData(LAYER_SORT_Y);
    }

    public void attachTo(Pane entityPane) {
        if (entityPane != null && !entityPane.getChildren().contains(layer)) {
            entityPane.getChildren().add(layer);
        }
    }

    public Node getNode() {
        return layer;
    }

    /** Cuốc đất: bắn bung 5-7 cục đất tròn màu nâu nhỏ ra xung quanh ô đất rồi rơi xuống mờ dần. */
    public void spawnDirtClods(double worldX, double worldY) {
        int count = 5 + rng.nextInt(3); // 5-7 cục đất tròn màu nâu
        for (int i = 0; i < count; i++) {
            double radius = 2.6 + rng.nextDouble() * 1.9;
            Circle clod = new Circle(radius);
            Color fill = (i % 3 == 0)
                    ? Color.rgb(156, 102, 54)
                    : (i % 3 == 1 ? Color.rgb(118, 72, 36) : Color.rgb(86, 48, 24));
            clod.setFill(fill);
            clod.setStroke(Color.rgb(46, 26, 14, 0.88));
            clod.setStrokeWidth(1.0);

            double angle = -Math.PI * (0.14 + rng.nextDouble() * 0.72);
            double speed = 58.0 + rng.nextDouble() * 82.0;
            addParticle(new Particle(
                clod,
                worldX + (rng.nextDouble() - 0.5) * 24.0,
                worldY + (rng.nextDouble() - 0.5) * 14.0,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed - 44.0,
                470.0, 0.96, 0.0,
                0.0, 0.0,
                0.42 + rng.nextDouble() * 0.24,
                1.10, 0.35
            ));
        }
    }

    /** Chặt cây: bắn bung 8-11 mảnh vụn gỗ nâu sắc cạnh (wood chips) bay ra từ thân cây và rơi xuống. */
    public void spawnWoodChipBurst(double worldX, double worldY, boolean felled) {
        int count = felled ? 16 : 9;
        for (int i = 0; i < count; i++) {
            double w = 3.2 + rng.nextDouble() * 3.8;
            double h = 2.0 + rng.nextDouble() * 2.2;
            Rectangle chip = new Rectangle(w, h);
            chip.setArcWidth(1.2);
            chip.setArcHeight(1.2);
            Color fill = (i % 3 == 0)
                    ? Color.rgb(196, 134, 72)
                    : (i % 3 == 1 ? Color.rgb(138, 80, 38) : Color.rgb(94, 52, 24));
            chip.setFill(fill);
            chip.setStroke(Color.rgb(42, 22, 10, 0.85));
            chip.setStrokeWidth(0.9);

            double angle = -Math.PI * (0.08 + rng.nextDouble() * 0.84);
            double speed = (felled ? 85.0 : 62.0) + rng.nextDouble() * (felled ? 115.0 : 88.0);
            addParticle(new Particle(
                chip,
                worldX + (rng.nextDouble() - 0.5) * 26.0,
                worldY + (rng.nextDouble() - 0.5) * 18.0,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed - (felled ? 62.0 : 42.0),
                480.0, 0.96, (rng.nextDouble() - 0.5) * 460.0,
                0.0, 0.0,
                (felled ? 0.58 : 0.42) + rng.nextDouble() * 0.25,
                1.15, 0.45
            ));
        }
    }

    /** Đập đá khai khoáng: bắn tia lửa kim loại vàng/cam sáng lóe + mảnh đá xám văng ra. */
    public void spawnOreSparkBurst(double worldX, double worldY, boolean shattered) {
        int count = shattered ? 18 : 10;
        for (int i = 0; i < count; i++) {
            boolean spark = (i % 2 == 0);
            Node pNode;
            if (spark) {
                Rectangle s = new Rectangle(3.2, 3.2);
                s.setFill(i % 4 == 0 ? Color.rgb(255, 248, 168) : Color.rgb(255, 174, 48));
                pNode = s;
            } else {
                Circle pebble = new Circle(2.4 + rng.nextDouble() * 1.8, Color.rgb(126, 134, 148));
                pebble.setStroke(Color.rgb(42, 46, 54));
                pebble.setStrokeWidth(0.8);
                pNode = pebble;
            }
            double angle = -Math.PI * (0.06 + rng.nextDouble() * 0.88);
            double speed = (shattered ? 95.0 : 68.0) + rng.nextDouble() * 105.0;
            addParticle(new Particle(
                pNode,
                worldX + (rng.nextDouble() - 0.5) * 20.0,
                worldY + (rng.nextDouble() - 0.5) * 14.0,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed - 48.0,
                460.0, 0.95, (rng.nextDouble() - 0.5) * 420.0,
                0.0, 0.0,
                0.42 + rng.nextDouble() * 0.25,
                1.15, 0.25
            ));
        }
    }

    /** Pháo hoa rực rỡ nhiều màu nổ tung trên bầu trời Khu Bể Bơi vào Đêm Lễ Hội (Day 14, 28...). */
    public void spawnFireworkBurst(double worldX, double worldY) {
        Color[][] palettes = {
            { Color.web("#FF4D6D"), Color.web("#FFB3C1"), Color.web("#FFF0F3") },
            { Color.web("#FFD166"), Color.web("#FFEE99"), Color.web("#FFFFFF") },
            { Color.web("#38BDF8"), Color.web("#7DD3FC"), Color.web("#F0F9FF") },
            { Color.web("#4ADE80"), Color.web("#BBF7D0"), Color.web("#FEF08A") },
            { Color.web("#C084FC"), Color.web("#F472B6"), Color.web("#FFF1F2") }
        };
        Color[] pal = palettes[rng.nextInt(palettes.length)];
        int rays = 20;
        for (int i = 0; i < rays; i++) {
            double angle = (Math.PI * 2.0 * i) / rays + (rng.nextDouble() - 0.5) * 0.14;
            double speed = 75.0 + rng.nextDouble() * 95.0;
            Circle spark = new Circle(2.8 + rng.nextDouble() * 1.8, pal[i % pal.length]);
            addParticle(new Particle(
                spark, worldX, worldY,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                95.0, 0.94, 0.0,
                0.0, 0.0,
                0.75 + rng.nextDouble() * 0.35,
                1.25, 0.20
            ));
        }
    }

    /** Tưới nước: bắn ra 11 giọt nước xanh nhỏ rơi xuống theo trọng lực. */
    public void spawnWaterSplash(double worldX, double worldY) {
        for (int i = 0; i < 11; i++) {
            double size = 3.5 + rng.nextDouble() * 3.0;
            boolean bright = (i % 3 == 0);
            Rectangle drop = new Rectangle(size, size + 1.5);
            drop.setArcWidth(2.0);
            drop.setArcHeight(2.0);
            drop.setFill(bright ? Color.rgb(210, 245, 255) : Color.rgb(74, 188, 252));
            drop.setStroke(Color.rgb(28, 108, 168, 0.55));
            drop.setStrokeWidth(0.8);

            double angle = -Math.PI * (0.20 + rng.nextDouble() * 0.60); // bắn vồng lên trên
            double speed = 55.0 + rng.nextDouble() * 85.0;
            double ox = worldX + (rng.nextDouble() - 0.5) * 26.0;
            double oy = worldY + (rng.nextDouble() - 0.5) * 14.0;

            addParticle(new Particle(
                drop, ox, oy,
                Math.cos(angle) * speed, Math.sin(angle) * speed - 35.0,
                420.0, 0.98, (rng.nextDouble() - 0.5) * 200.0,
                0.0, 0.0,
                0.42 + rng.nextDouble() * 0.24,
                1.0, 0.45
            ));
        }
    }

    /**
     * Hiệu ứng phun nước xoay 360° cực dày và tỏa rộng của Vòi Tưới Tự Động:
     * 4 tia nước lớn xoay tròn bắn xa 65–75px phủ kín toàn bộ 8 ô đất xung quanh (3x3 = 120x120px) + vòng sóng nước lan tỏa.
     */
    public void spawnSprinklerSprayPulse(double worldX, double worldY, double spinAngleRad) {
        // 4 cánh tia nước xoay quanh tâm vòi (mỗi cánh gồm 3 giọt gần-giữa-xa tạo thành dải nước dày đặc)
        for (int arm = 0; arm < 4; arm++) {
            double baseAngle = spinAngleRad + arm * (Math.PI * 0.5);
            for (int tier = 0; tier < 3; tier++) {
                double jitter = (rng.nextDouble() - 0.5) * 0.22;
                double a = baseAngle + jitter;
                double speed = 82.0 + tier * 36.0 + rng.nextDouble() * 22.0; // bay xa tới 65..78px phủ trọn 8 ô xung quanh
                double sz = 4.8 + rng.nextDouble() * 3.0;

                Circle drop = new Circle(sz * 0.58);
                Color fill = (tier == 0)
                        ? Color.rgb(224, 248, 255, 0.96)
                        : ((tier == 1) ? Color.rgb(96, 212, 255, 0.92) : Color.rgb(42, 168, 246, 0.88));
                drop.setFill(fill);
                drop.setStroke(Color.rgb(18, 96, 162, 0.55));
                drop.setStrokeWidth(0.9);

                addParticle(new Particle(
                    drop,
                    worldX + Math.cos(a) * 6.0,
                    worldY - 10.0 + Math.sin(a) * 4.0,
                    Math.cos(a) * speed,
                    Math.sin(a) * speed * 0.86 - 16.0,
                    95.0, 0.97, 0.0,
                    0.0, 0.0,
                    0.48 + tier * 0.08 + rng.nextDouble() * 0.12,
                    1.15, 0.55
                ));
            }
        }

        // Vòng sương nước mảnh lan rộng trên vùng đất 3x3 xung quanh vòi
        if (rng.nextDouble() < 0.42) {
            Circle ring = new Circle(22.0 + rng.nextDouble() * 14.0, Color.TRANSPARENT);
            ring.setStroke(Color.rgb(165, 236, 255, 0.55));
            ring.setStrokeWidth(2.6);
            addParticle(new Particle(
                ring,
                worldX, worldY,
                0.0, 0.0,
                0.0, 1.0, 0.0,
                0.0, 0.0,
                0.55,
                0.45, 2.35
            ));
        }
    }

    /** Thu hoạch: bắn ra lá xanh xoay nhẹ + tia sáng vàng lấp lánh tại ô đất. */
    public void spawnHarvestSparkles(double worldX, double worldY) {
        // 6 chiếc lá xanh pixel bung tỏa
        for (int i = 0; i < 6; i++) {
            Polygon leaf = new Polygon(0, -4.5, 4.5, 0, 0, 4.5, -3.5, 0);
            leaf.setFill(i % 2 == 0 ? Color.rgb(104, 196, 76) : Color.rgb(154, 218, 92));
            leaf.setStroke(Color.rgb(44, 96, 36));
            leaf.setStrokeWidth(1.0);

            double angle = -Math.PI * (0.15 + rng.nextDouble() * 0.70);
            double speed = 50.0 + rng.nextDouble() * 65.0;
            addParticle(new Particle(
                leaf,
                worldX + (rng.nextDouble() - 0.5) * 18.0,
                worldY + (rng.nextDouble() - 0.5) * 14.0,
                Math.cos(angle) * speed, Math.sin(angle) * speed - 25.0,
                165.0, 0.94, (rng.nextDouble() - 0.5) * 320.0,
                14.0, 6.0,
                0.60 + rng.nextDouble() * 0.28,
                1.05, 0.55
            ));
        }

        // 8 tia sáng lấp lánh vàng/trắng
        for (int i = 0; i < 8; i++) {
            Node star = createSparkleNode(i % 2 == 0 ? Color.rgb(255, 236, 108) : Color.rgb(255, 252, 232));
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 35.0 + rng.nextDouble() * 60.0;
            addParticle(new Particle(
                star,
                worldX + (rng.nextDouble() - 0.5) * 20.0,
                worldY + (rng.nextDouble() - 0.5) * 18.0,
                Math.cos(angle) * speed, Math.sin(angle) * speed - 20.0,
                -15.0, 0.92, (rng.nextDouble() - 0.5) * 180.0,
                0.0, 0.0,
                0.45 + rng.nextDouble() * 0.28,
                1.15, 0.25
            ));
        }
    }

    /** Vật nuôi: bật lên 3 icon trái tim hồng/đỏ bay nhẹ lên trên và mờ dần. */
    public void spawnHeartBurst(double worldX, double worldY) {
        for (int i = 0; i < 3; i++) {
            double scale = (i == 0) ? 1.05 : (0.78 + rng.nextDouble() * 0.22);
            Color fill = (i % 2 == 0) ? Color.rgb(255, 86, 124) : Color.rgb(255, 134, 166);
            Node heart = createHeartNode(fill);

            double ox = worldX + (i - 1) * 12.0 + (rng.nextDouble() - 0.5) * 6.0;
            double oy = worldY - 8.0 - i * 4.0;
            addParticle(new Particle(
                heart, ox, oy,
                (i - 1) * 10.0, -42.0 - rng.nextDouble() * 18.0,
                -8.0, 0.98, 0.0,
                16.0, 5.2,
                0.85 + rng.nextDouble() * 0.25,
                scale * 0.75, scale * 1.15
            ));
        }
    }

    /** Tia sáng nhỏ bật lên khi nhân vật hút/nhặt vật phẩm thành công. */
    public void spawnPickupPop(double worldX, double worldY) {
        for (int i = 0; i < 6; i++) {
            Circle dot = new Circle(2.8, i % 2 == 0 ? Color.rgb(255, 240, 120) : Color.WHITE);
            double angle = (Math.PI * 2.0 * i) / 6.0 + rng.nextDouble() * 0.3;
            double speed = 42.0 + rng.nextDouble() * 28.0;
            addParticle(new Particle(
                dot, worldX, worldY,
                Math.cos(angle) * speed, Math.sin(angle) * speed,
                0.0, 0.90, 0.0,
                0.0, 0.0,
                0.30, 1.1, 0.2
            ));
        }
    }

    /** Hạt lửa hồng & khói pixel bay nhẹ lên từ đống lửa trại. */
    public void spawnCampfireEmber(double worldX, double worldY) {
        // 1 hạt tàn lửa cam/vàng bay lơ lửng lên trên
        double size = 3.0 + rng.nextDouble() * 2.5;
        Rectangle ember = new Rectangle(size, size);
        ember.setFill(rng.nextBoolean() ? Color.rgb(255, 196, 62) : Color.rgb(255, 114, 38));
        addParticle(new Particle(
            ember,
            worldX + (rng.nextDouble() - 0.5) * 20.0,
            worldY - 6.0 + (rng.nextDouble() - 0.5) * 8.0,
            (rng.nextDouble() - 0.5) * 14.0,
            -28.0 - rng.nextDouble() * 24.0,
            -6.0, 0.98, (rng.nextDouble() - 0.5) * 90.0,
            8.0, 4.5,
            0.75 + rng.nextDouble() * 0.45,
            1.0, 0.25
        ));

        // 40% xác suất kèm theo một cụm khói xám ấm nhẹ
        if (rng.nextDouble() < 0.42) {
            double s = 5.5 + rng.nextDouble() * 3.5;
            Rectangle smoke = new Rectangle(s, s);
            smoke.setArcWidth(2.5);
            smoke.setArcHeight(2.5);
            smoke.setFill(Color.rgb(215, 205, 195, 0.48));
            addParticle(new Particle(
                smoke,
                worldX + (rng.nextDouble() - 0.5) * 14.0,
                worldY - 14.0,
                (rng.nextDouble() - 0.5) * 10.0,
                -22.0 - rng.nextDouble() * 14.0,
                -4.0, 0.99, 0.0,
                10.0, 3.0,
                1.05 + rng.nextDouble() * 0.45,
                0.75, 1.55
            ));
        }
    }

    /** Hiệu ứng khói thơm & tia lửa vàng bung lên từ đống lửa khi đang/vừa nấu món ăn. */
    public void spawnCookingBurst(double worldX, double worldY) {
        for (int i = 0; i < 8; i++) {
            double size = 3.5 + rng.nextDouble() * 2.8;
            Rectangle spark = new Rectangle(size, size);
            spark.setFill(i % 2 == 0 ? Color.rgb(255, 224, 92) : Color.rgb(255, 124, 40));
            double angle = -Math.PI * (0.20 + rng.nextDouble() * 0.60);
            double spd = 38.0 + rng.nextDouble() * 48.0;
            addParticle(new Particle(
                spark,
                worldX + (rng.nextDouble() - 0.5) * 18.0,
                worldY - 8.0,
                Math.cos(angle) * spd,
                Math.sin(angle) * spd - 16.0,
                35.0, 0.96, (rng.nextDouble() - 0.5) * 180.0,
                6.0, 5.0,
                0.65 + rng.nextDouble() * 0.35,
                1.1, 0.25
            ));
        }
        for (int i = 0; i < 5; i++) {
            double s = 7.0 + rng.nextDouble() * 4.0;
            Rectangle aroma = new Rectangle(s, s);
            aroma.setArcWidth(4.0);
            aroma.setArcHeight(4.0);
            aroma.setFill(Color.rgb(255, 246, 226, 0.62));
            addParticle(new Particle(
                aroma,
                worldX + (rng.nextDouble() - 0.5) * 22.0,
                worldY - 16.0,
                (rng.nextDouble() - 0.5) * 14.0,
                -34.0 - rng.nextDouble() * 18.0,
                -8.0, 0.98, 0.0,
                14.0, 3.8,
                1.05 + rng.nextDouble() * 0.45,
                0.70, 1.65
            ));
        }
    }

    private Node createSparkleNode(Color color) {
        Polygon star = new Polygon(
            0, -5.5, 1.6, -1.6, 5.5, 0, 1.6, 1.6,
            0, 5.5, -1.6, 1.6, -5.5, 0, -1.6, -1.6
        );
        star.setFill(color);
        star.setStroke(Color.rgb(218, 164, 34, 0.7));
        star.setStrokeWidth(0.7);
        return star;
    }

    private Node createHeartNode(Color fill) {
        // Icon trái tim pixel-friendly cân đối, có viền đậm rõ nét trên nền cỏ
        Polygon heart = new Polygon(
            0, 7,
            -7, 0,
            -7, -4,
            -4, -7,
            -1.5, -7,
            0, -4.5,
            1.5, -7,
            4, -7,
            7, -4,
            7, 0
        );
        heart.setFill(fill);
        heart.setStroke(Color.rgb(122, 22, 48));
        heart.setStrokeWidth(1.4);

        Rectangle shine = new Rectangle(-4.5, -5.0, 2.2, 2.2);
        shine.setFill(Color.rgb(255, 232, 240, 0.88));
        return new Group(heart, shine);
    }

    private void addParticle(Particle p) {
        p.node.setLayoutX(p.x);
        p.node.setLayoutY(p.y);
        p.node.setScaleX(p.startScale);
        p.node.setScaleY(p.startScale);
        particles.add(p);
        layer.getChildren().add(p.node);
    }

    /** Cập nhật toàn bộ hạt mỗi frame. */
    public void update(double deltaSeconds) {
        if (particles.isEmpty()) return;
        double dt = Math.min(0.05, Math.max(0.0, deltaSeconds));
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age += dt;
            if (p.age >= p.maxLife) {
                layer.getChildren().remove(p.node);
                it.remove();
                continue;
            }
            double progress = p.age / p.maxLife;
            p.vy += p.gravity * dt;
            double dragFactor = Math.pow(p.drag, dt * 60.0);
            p.vx *= dragFactor;
            p.vy *= dragFactor;

            double swayOffset = (p.swayAmp > 0)
                ? Math.sin(p.age * p.swayFreq + p.swayPhase) * p.swayAmp * dt
                : 0.0;
            p.x += p.vx * dt + swayOffset;
            p.y += p.vy * dt;

            p.node.setLayoutX(p.x);
            p.node.setLayoutY(p.y);
            if (p.rotSpeed != 0.0) {
                p.node.setRotate(p.node.getRotate() + p.rotSpeed * dt);
            }
            double sc = p.startScale + (p.endScale - p.startScale) * progress;
            p.node.setScaleX(sc);
            p.node.setScaleY(sc);
            // Giữ rõ 55% đầu rồi mờ dần tự nhiên
            double alpha = (progress < 0.55) ? 1.0 : Math.max(0.0, 1.0 - (progress - 0.55) / 0.45);
            p.node.setOpacity(alpha);
        }
    }

    public void clear() {
        particles.clear();
        layer.getChildren().clear();
    }
}
