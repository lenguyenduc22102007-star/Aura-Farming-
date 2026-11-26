package view.fx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import model.game_item.Item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Hệ thống vật phẩm bật nảy & tự hút (Drop Bounce & Magnet Pickup):
 * - Vật phẩm rơi ra mặt đất theo quỹ đạo parabol ngắn, nảy 2 nhịp có bóng mờ.
 * - Khi nhân vật bước vào bán kính hút (MAGNET_RADIUS), vật phẩm bay mượt về phía nhân vật,
 *   phát SoundHook.playPickup(), cộng vào túi đồ qua callback và hiện chữ nổi "+1 [Tên vật phẩm]".
 */
public class DroppedItemSystem {

    public interface PickupCallback {
        void onItemPickedUp(Item item, int quantity, double worldX, double worldY);
    }

    private static final double DROP_LAYER_SORT_Y = 9960.0;
    private static final double TEXT_LAYER_SORT_Y = 9992.0;

    private static final double MAGNET_RADIUS = 124.0;
    private static final double PICKUP_RADIUS = 24.0;
    private static final double PICKUP_GRACE_SEC = 0.28; // Thời gian ngắn để người chơi thấy nhịp nảy đầu tiên
    private static final double GRAVITY = 640.0;

    private final Pane dropLayer;
    private final Pane textLayer;
    private final List<DroppedItem> drops = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final PickupCallback pickupCallback;
    private final ParticleSystem particleSystem;
    private final Random rng = new Random();

    private static final class DroppedItem {
        final Item item;
        final int quantity;
        final Group root;
        final Node iconNode;
        final Ellipse shadow;

        double x;       // tọa độ X mặt đất
        double groundY; // tọa độ Y mặt đất
        double z;       // độ cao so với mặt đất (>= 0)
        double vx;
        double vy;      // tốc độ trượt trên mặt đất
        double vz;      // vận tốc theo trục đứng (nhảy lên > 0)
        int bounces = 0;
        double age = 0.0;
        boolean magnetizing = false;
        double magnetSpeed = 160.0;
        final double bobPhase;

        DroppedItem(Item item, int quantity, Group root, Node iconNode, Ellipse shadow,
                    double x, double groundY, double vx, double vy, double vz) {
            this.item = item;
            this.quantity = quantity;
            this.root = root;
            this.iconNode = iconNode;
            this.shadow = shadow;
            this.x = x;
            this.groundY = groundY;
            this.z = 8.0;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.bobPhase = Math.random() * Math.PI * 2.0;
        }
    }

    private static final class FloatingText {
        final Label label;
        final double baseX;
        final double baseY;
        double age = 0.0;
        final double maxLife = 1.15;

        FloatingText(Label label, double baseX, double baseY) {
            this.label = label;
            this.baseX = baseX;
            this.baseY = baseY;
        }
    }

    public DroppedItemSystem(PickupCallback pickupCallback, ParticleSystem particleSystem) {
        this.pickupCallback = pickupCallback;
        this.particleSystem = particleSystem;

        dropLayer = new Pane();
        dropLayer.setManaged(false);
        dropLayer.setMouseTransparent(true);
        dropLayer.setUserData(DROP_LAYER_SORT_Y);

        textLayer = new Pane();
        textLayer.setManaged(false);
        textLayer.setMouseTransparent(true);
        textLayer.setUserData(TEXT_LAYER_SORT_Y);
    }

    public void attachTo(Pane entityPane) {
        if (entityPane == null) return;
        if (!entityPane.getChildren().contains(dropLayer)) {
            entityPane.getChildren().add(dropLayer);
        }
        if (!entityPane.getChildren().contains(textLayer)) {
            entityPane.getChildren().add(textLayer);
        }
    }

    /**
     * Spawn một vật phẩm bật nảy parabol từ tọa độ (worldX, worldY).
     */
    public void spawnDrop(Item item, Image icon, double worldX, double worldY, int quantity) {
        if (item == null || quantity <= 0) return;

        Ellipse shadow = new Ellipse(0, 14, 12, 4.5);
        shadow.setFill(Color.rgb(18, 24, 34, 0.34));

        Node iconNode;
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(34);
            iv.setFitHeight(34);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            iv.setX(-17);
            iv.setY(-20);
            iconNode = iv;
        } else {
            // Fallback hình tròn vàng nổi bật nếu item chưa có icon
            Circle badge = new Circle(0, -4, 12, Color.rgb(246, 206, 78));
            badge.setStroke(Color.rgb(92, 54, 22));
            badge.setStrokeWidth(2.0);
            iconNode = badge;
        }

        Group root = new Group(shadow, iconNode);
        root.setManaged(false);
        root.setLayoutX(worldX);
        root.setLayoutY(worldY);

        // Vận tốc nảy ngẫu nhiên ngắn gọn để đồ nằm ngay quanh ô thu hoạch
        double angle = rng.nextDouble() * Math.PI * 2.0;
        double horizSpeed = 38.0 + rng.nextDouble() * 44.0;
        double vx = Math.cos(angle) * horizSpeed;
        double vy = Math.sin(angle) * horizSpeed * 0.55;
        double vz = 165.0 + rng.nextDouble() * 45.0;

        DroppedItem d = new DroppedItem(item, quantity, root, iconNode, shadow, worldX, worldY, vx, vy, vz);
        drops.add(d);
        dropLayer.getChildren().add(root);
    }

    /** Hiển thị chữ nổi bay lên tại (worldX, worldY) như "+1 Tomato". */
    public void spawnFloatingText(String text, double worldX, double worldY, Color color) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;"
        );
        lbl.setTextFill(color);
        lbl.setEffect(new DropShadow(2.2, 1.0, 1.4, Color.rgb(22, 14, 10, 0.92)));
        lbl.setLayoutX(worldX - 38.0);
        lbl.setLayoutY(worldY - 26.0);
        textLayer.getChildren().add(lbl);
        texts.add(new FloatingText(lbl, worldX - 38.0, worldY - 26.0));
    }

    /**
     * Cập nhật vật lý bật nảy, tự hút về phía nhân vật và chữ nổi mỗi frame.
     */
    public void update(double deltaSeconds, double timeSeconds, double playerCenterX, double playerCenterY) {
        double dt = Math.min(0.05, Math.max(0.0, deltaSeconds));

        Iterator<DroppedItem> it = drops.iterator();
        while (it.hasNext()) {
            DroppedItem d = it.next();
            d.age += dt;

            // Kiểm tra bán kính hút nam châm sau khoảng grace ngắn
            if (!d.magnetizing && d.age >= PICKUP_GRACE_SEC) {
                double dist = Math.hypot(playerCenterX - d.x, playerCenterY - d.groundY);
                if (dist <= MAGNET_RADIUS) {
                    d.magnetizing = true;
                }
            }

            if (d.magnetizing) {
                // Gia tốc hút mượt về tâm nhân vật
                d.magnetSpeed = Math.min(520.0, d.magnetSpeed + 720.0 * dt);
                double dx = playerCenterX - d.x;
                double dy = (playerCenterY - 8.0) - (d.groundY - d.z);
                double dist = Math.hypot(dx, dy);

                if (dist <= PICKUP_RADIUS || dist <= d.magnetSpeed * dt) {
                    completePickup(d, playerCenterX, playerCenterY);
                    dropLayer.getChildren().remove(d.root);
                    it.remove();
                    continue;
                }

                double step = d.magnetSpeed * dt;
                d.x += (dx / dist) * step;
                d.groundY += (dy / dist) * step;
                d.z = Math.max(0.0, d.z * (1.0 - Math.min(1.0, dt * 8.0)));

                d.root.setLayoutX(d.x);
                d.root.setLayoutY(d.groundY);
                d.iconNode.setTranslateY(-d.z);
                d.shadow.setOpacity(0.14);
                continue;
            }

            // Giai đoạn rơi parabol & nảy 2 nhịp
            if (d.z > 0.0 || d.vz > 0.0) {
                d.vz -= GRAVITY * dt;
                d.z += d.vz * dt;
                d.x += d.vx * dt;
                d.groundY += d.vy * dt;

                if (d.z <= 0.0) {
                    d.z = 0.0;
                    if (d.bounces < 2 && Math.abs(d.vz) > 45.0) {
                        d.vz = -d.vz * 0.44;
                        d.vx *= 0.55;
                        d.vy *= 0.55;
                        d.bounces++;
                    } else {
                        d.vz = 0.0;
                        d.vx = 0.0;
                        d.vy = 0.0;
                    }
                }
            }

            // Khi đã nằm yên trên đất: nhấp nhô rất nhẹ để dễ nhìn
            double idleBob = (d.z == 0.0 && d.vz == 0.0) ? (Math.sin(timeSeconds * 4.2 + d.bobPhase) * 2.5 + 2.5) : 0.0;
            double visualLift = d.z + idleBob;

            d.root.setLayoutX(d.x);
            d.root.setLayoutY(d.groundY);
            d.iconNode.setTranslateY(-visualLift);

            // Bóng mờ co nhỏ lại khi vật phẩm bay cao
            double shadowScale = Math.max(0.45, 1.0 - visualLift / 85.0);
            d.shadow.setScaleX(shadowScale);
            d.shadow.setScaleY(shadowScale);
            d.shadow.setOpacity(0.32 * shadowScale);
        }

        // Cập nhật chữ nổi "+1 ..."
        Iterator<FloatingText> tit = texts.iterator();
        while (tit.hasNext()) {
            FloatingText ft = tit.next();
            ft.age += dt;
            if (ft.age >= ft.maxLife) {
                textLayer.getChildren().remove(ft.label);
                tit.remove();
                continue;
            }
            double p = ft.age / ft.maxLife;
            ft.label.setLayoutX(ft.baseX);
            ft.label.setLayoutY(ft.baseY - p * 34.0);
            ft.label.setOpacity(p < 0.65 ? 1.0 : Math.max(0.0, 1.0 - (p - 0.65) / 0.35));
        }
    }

    private void completePickup(DroppedItem d, double px, double py) {
        SoundHook.playPickup();
        if (particleSystem != null) {
            particleSystem.spawnPickupPop(px, py - 10.0);
        }
        String displayName = d.item.getName().replace(" (Crop)", "");
        spawnFloatingText("+" + d.quantity + " " + displayName, px, py - 24.0, Color.rgb(255, 244, 128));
        if (pickupCallback != null) {
            pickupCallback.onItemPickedUp(d.item, d.quantity, px, py);
        }
    }

    /**
     * Thu gom tức thì mọi vật phẩm đang rơi trên đất (gọi trước khi lưu game / qua ngày để không mất đồ).
     */
    public void collectAllImmediately() {
        if (drops.isEmpty()) return;
        for (DroppedItem d : drops) {
            if (pickupCallback != null) {
                pickupCallback.onItemPickedUp(d.item, d.quantity, d.x, d.groundY);
            }
        }
        drops.clear();
        dropLayer.getChildren().clear();
    }

    public boolean hasActiveDrops() {
        return !drops.isEmpty();
    }
}
