package view.mine;

import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.game_item.AutoSprinklerItem;
import model.game_item.Item;
import model.game_item.OreItem;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.FarmMap;
import utils.Constant;
import view.fx.ParticleSystem;
import view.fx.SoundHook;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Quản lý Khu Khai Khoáng Mini (MineManager) & Hệ thống Vòi Tưới Tự Động (AutoSprinkler):
 * 1. Biến các tảng đá mỏ (mine_rock) thành RockNode (HP = 3):
 *    - Cầm Cuốc (Hoe) bấm Space đập đá -> rung lắc, bắn tia lửa vàng cam, trừ -2 Đói / -2 Khát.
 *    - Đá vỡ rơi ngẫu nhiên Quặng Đồng ($35), Quặng Sắt ($65), Quặng Vàng ($120), Đá Quý ($240).
 *    - Sáng hôm sau (nextDay) toàn bộ mỏ đá tự mọc lại đầy đủ.
 * 2. Chế tạo & đặt Vòi Tưới Tự Động (AutoSprinkler):
 *    - Đổi 2 Quặng Sắt + 1 Quặng Vàng tại Đe rèn (mine_anvil) hoặc trong Sổ Quản Lý.
 *    - Đặt lên ruộng bằng phím [V] -> mỗi sáng tự động tưới ướt đẫm 8 ô xung quanh nó!
 */
public class MineManager {

    public static final class RockNode {
        public final String key;
        public final ImageView view;
        public final double centerX;
        public final double centerY;
        public int hp = 3;
        public boolean broken = false;

        RockNode(String key, ImageView view, double cx, double cy) {
            this.key = key;
            this.view = view;
            this.centerX = cx;
            this.centerY = cy;
        }
    }

    private final List<RockNode> rocks = new ArrayList<>();
    private final List<double[]> anvilPoints = new ArrayList<>();
    /** Các tọa độ ô (row,col -> "r,c") đã đặt Vòi Tưới Tự Động trên bản đồ. */
    private final Set<String> placedSprinklers = new LinkedHashSet<>();
    private final List<Group> sprinklerVisuals = new ArrayList<>();
    private final Random rng = new Random();

    public void registerMapObjects(List<ImageView> sceneryObjects, Pane entityPane) {
        rocks.clear();
        anvilPoints.clear();
        clearSprinklerNodes(entityPane);

        if (sceneryObjects == null) return;
        for (ImageView iv : sceneryObjects) {
            String id = iv.getId();
            if (id == null) continue;
            String low = id.toLowerCase();
            double cx = iv.getLayoutX() + iv.getFitWidth() * 0.5;
            double cy = iv.getLayoutY() + iv.getFitHeight() * 0.65;
            if (low.equals("mine_rock")) {
                String k = id + "@" + (int) iv.getLayoutX() + "," + (int) iv.getLayoutY();
                rocks.add(new RockNode(k, iv, cx, cy));
            } else if (low.equals("mine_anvil")) {
                anvilPoints.add(new double[] { cx, cy });
            }
        }
        rebuildAllSprinklerVisuals(entityPane);
    }

    public List<RockNode> getRocks() {
        return rocks;
    }

    public List<double[]> getAnvilPoints() {
        return anvilPoints;
    }

    /** Kiểm tra nhân vật có đang đứng cạnh tảng đá mỏ nào để đập bằng Cuốc hay không. */
    public RockNode findNearestActiveRock(double targetX, double targetY, double footX, double footY) {
        RockNode best = null;
        double bestDist = Double.MAX_VALUE;
        for (RockNode r : rocks) {
            if (r.broken || !r.view.isVisible()) continue;
            double dt = Math.hypot(targetX - r.centerX, targetY - r.centerY);
            double dp = Math.hypot(footX - r.centerX, footY - r.centerY);
            double d = Math.min(dt, dp);
            if ((dt <= 82.0 || dp <= 96.0) && d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }

    /** Thực hiện 1 nhát cuốc đập mỏ quặng; trả về true nếu có tảng đá trong tầm và đã đập trúng. */
    public boolean tryStrikeRock(double targetX,
                                 double targetY,
                                 double footX,
                                 double footY,
                                 Player player,
                                 ParticleSystem particles,
                                 BiConsumer<String, Color> floatingTextCb,
                                 Consumer<String> notifyCb) {
        RockNode rock = findNearestActiveRock(targetX, targetY, footX, footY);
        if (rock == null) return false;

        rock.hp = Math.max(0, rock.hp - 1);
        boolean shattered = (rock.hp <= 0);

        // Trừ -2 Đói & -2 Khát theo yêu cầu
        if (player != null) {
            player.addHunger(-2.0);
            player.addThirst(-2.0);
        }

        // Hiệu ứng rung lắc tảng đá + bắn tia lửa kim loại cam vàng
        shakeRock(rock.view);
        if (particles != null) {
            particles.spawnOreSparkBurst(rock.centerX, rock.centerY - 12.0, shattered);
        }

        if (shattered) {
            rock.broken = true;
            rock.view.setVisible(false);
            OreItem drop1 = rollRandomOre();
            int qty = (rng.nextInt(100) < 35) ? 2 : 1;
            if (player != null) {
                player.getInventory().addItem(drop1, qty);
            }
            SoundHook.playPickup();
            if (floatingTextCb != null) {
                floatingTextCb.accept("+" + qty + " " + drop1.getName() + "!", Color.rgb(255, 226, 92));
            }
            if (notifyCb != null) {
                notifyCb.accept("⛏ Đập vỡ tảng quặng! Thu được +" + qty + "× " + drop1.getName() + " (Bán $" + drop1.getPrice() + "/viên)!");
            }
        } else {
            SoundHook.playWoodGateOpen();
            if (floatingTextCb != null) {
                floatingTextCb.accept("⛏ Đập quặng (" + rock.hp + "/3 HP)", Color.rgb(255, 198, 112));
            }
            if (notifyCb != null) {
                notifyCb.accept("⛏ Đang khai khoáng... (Độ bền tảng đá còn " + rock.hp + "/3 | -2 Đói, -2 Khát)");
            }
        }
        return true;
    }

    private OreItem rollRandomOre() {
        int roll = rng.nextInt(100);
        if (roll < 36) return OreItem.copper();   // 36% Quặng Đồng ($35)
        if (roll < 70) return OreItem.iron();     // 34% Quặng Sắt ($65)
        if (roll < 90) return OreItem.gold();     // 20% Quặng Vàng ($120)
        return OreItem.gemstone();                // 10% Đá Quý ($240)
    }

    private void shakeRock(ImageView v) {
        if (v == null) return;
        v.setTranslateX(0.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(38), v);
        tt.setFromX(-4.5);
        tt.setToX(4.5);
        tt.setAutoReverse(true);
        tt.setCycleCount(4);
        tt.setOnFinished(e -> v.setTranslateX(0.0));
        tt.play();
    }

    /** Sáng hôm sau: hồi phục lại toàn bộ các tảng đá mỏ để người chơi tiếp tục đào quặng. */
    public void respawnAllRocks() {
        for (RockNode r : rocks) {
            r.hp = 3;
            r.broken = false;
            r.view.setVisible(true);
            r.view.setTranslateX(0.0);
        }
    }

    /** Kiểm tra một vật cản mine_rock đã bị đập vỡ hay chưa (để nhân vật đi qua chỗ đá đã vỡ mượt mà). */
    public boolean isRockBrokenView(ImageView view) {
        for (RockNode r : rocks) {
            if (r.view == view) {
                return r.broken;
            }
        }
        return false;
    }

    // ==================== CHẾ TẠO & ĐẶT VÒI TƯỚI TỰ ĐỘNG (AUTO SPRINKLER) ====================

    /** Đổi 2 Quặng Sắt + 1 Quặng Vàng -> 1 Vòi Tưới Tự Động trong túi đồ. */
    public static boolean craftAutoSprinkler(Player player, Consumer<String> feedbackCb) {
        if (player == null) return false;
        OreItem ironProto = OreItem.iron();
        OreItem goldProto = OreItem.gold();
        int haveIron = player.getInventory().getItemCount(ironProto);
        int haveGold = player.getInventory().getItemCount(goldProto);
        if (haveIron < 2 || haveGold < 1) {
            if (feedbackCb != null) {
                feedbackCb.accept("⚠ Cần 2 Quặng Sắt + 1 Quặng Vàng để chế tạo Vòi Tưới Tự Động! (Hiện có: "
                        + haveIron + "/2 Sắt, " + haveGold + "/1 Vàng)");
            }
            return false;
        }
        player.getInventory().removeItem(ironProto, 2);
        player.getInventory().removeItem(goldProto, 1);
        player.getInventory().addItem(new AutoSprinklerItem(), 1);
        SoundHook.playPickup();
        if (feedbackCb != null) {
            feedbackCb.accept("⚙ Đã đúc thành công 1× Vòi Tưới Tự Động! Đứng trên ruộng bấm [V] để lắp đặt.");
        }
        return true;
    }

    public boolean hasSprinklerAt(int row, int col) {
        return placedSprinklers.contains(row + "," + col);
    }

    /** Lắp đặt 1 Vòi Tưới Tự Động từ túi đồ xuống ô đất (row, col). */
    public boolean placeSprinklerAt(Player player, int row, int col, Pane entityPane, Consumer<String> notifyCb) {
        if (player == null) return false;
        String key = row + "," + col;
        if (placedSprinklers.contains(key)) {
            if (notifyCb != null) notifyCb.accept("Ô đất này đã lắp Vòi Tưới Tự Động rồi!");
            return false;
        }
        Item sprinklerItem = new AutoSprinklerItem();
        if (player.getInventory().getItemCount(sprinklerItem) <= 0) {
            if (notifyCb != null) notifyCb.accept("Túi đồ chưa có Vòi Tưới Tự Động! Hãy tới Khu Mỏ Đá đổi (2 Quặng Sắt + 1 Quặng Vàng).");
            return false;
        }
        if (!player.getInventory().removeItem(sprinklerItem, 1)) {
            return false;
        }
        placedSprinklers.add(key);
        addSprinklerVisual(row, col, entityPane);
        SoundHook.playPickup();
        if (notifyCb != null) {
            notifyCb.accept("💧 Đã lắp Vòi Tưới Tự Động tại ô (" + col + ", " + row + ")! Mỗi sáng sẽ tự tưới ướt 8 ô xung quanh.");
        }
        return true;
    }

    /** Mỗi sáng thức dậy: tự động tưới đẫm nước cho 8 ô xung quanh (kể cả tâm) của mọi Vòi Tưới Tự Động. */
    public int waterSurroundingCellsDaily(FarmMap map, ParticleSystem particles) {
        if (map == null || placedSprinklers.isEmpty()) return 0;
        int wateredCount = 0;
        for (String key : placedSprinklers) {
            String[] parts = key.split(",");
            if (parts.length < 2) continue;
            int r = Integer.parseInt(parts[0]);
            int c = Integer.parseInt(parts[1]);
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    Cell target = map.getCell(r + dr, c + dc);
                    if (target != null && target.isFarmable()) {
                        target.updateMoistureAmount(Constant.MAX_MOISTURE);
                        wateredCount++;
                    }
                }
            }
            if (particles != null) {
                double wx = c * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                double wy = r * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
                particles.spawnWaterSplash(wx, wy);
            }
        }
        return wateredCount;
    }

    private void clearSprinklerNodes(Pane entityPane) {
        if (entityPane != null) {
            entityPane.getChildren().removeAll(sprinklerVisuals);
        }
        sprinklerVisuals.clear();
    }

    private void rebuildAllSprinklerVisuals(Pane entityPane) {
        clearSprinklerNodes(entityPane);
        for (String key : placedSprinklers) {
            String[] parts = key.split(",");
            if (parts.length >= 2) {
                addSprinklerVisual(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), entityPane);
            }
        }
    }

    private void addSprinklerVisual(int row, int col, Pane entityPane) {
        if (entityPane == null) return;
        double cx = col * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.5;
        double cy = row * Constant.CELL_SIZE + Constant.CELL_SIZE * 0.58;

        Ellipse shadow = new Ellipse(0, 8, 13, 5);
        shadow.setFill(Color.rgb(12, 22, 28, 0.38));
        Rectangle basePlate = new Rectangle(-10, 2, 20, 7);
        basePlate.setArcWidth(4); basePlate.setArcHeight(4);
        basePlate.setFill(Color.web("#64748B"));
        basePlate.setStroke(Color.web("#1E293B")); basePlate.setStrokeWidth(1.5);

        Rectangle pipe = new Rectangle(-3.5, -10, 7, 13);
        pipe.setFill(Color.web("#FACC15"));
        pipe.setStroke(Color.web("#713F12")); pipe.setStrokeWidth(1.3);

        Rectangle crossBar = new Rectangle(-12, -13, 24, 4.5);
        crossBar.setArcWidth(3); crossBar.setArcHeight(3);
        crossBar.setFill(Color.web("#38BDF8"));
        crossBar.setStroke(Color.web("#0C4A6E")); crossBar.setStrokeWidth(1.2);

        Circle nozzle = new Circle(0, -11, 4.0, Color.web("#E0F2FE"));
        nozzle.setStroke(Color.web("#0369A1")); nozzle.setStrokeWidth(1.3);

        Group g = new Group(shadow, basePlate, pipe, crossBar, nozzle);
        g.setManaged(false);
        g.setMouseTransparent(true);
        g.setLayoutX(cx);
        g.setLayoutY(cy);
        g.setUserData(cy + 4.0);

        sprinklerVisuals.add(g);
        entityPane.getChildren().add(g);
    }

    public void save(Properties p) {
        if (p == null) return;
        p.setProperty("mine.sprinklers", String.join(";", placedSprinklers));
        int idx = 0;
        for (RockNode r : rocks) {
            if (r.broken || r.hp < 3) {
                p.setProperty("mine.rock." + (idx++), r.key + "|" + r.hp + "|" + (r.broken ? 1 : 0));
            }
        }
        p.setProperty("mine.rock.count", Integer.toString(idx));
    }

    public void load(Properties p, Pane entityPane) {
        if (p == null) return;
        placedSprinklers.clear();
        String spRaw = p.getProperty("mine.sprinklers", "");
        if (!spRaw.isEmpty()) {
            for (String token : spRaw.split(";")) {
                String t = token.trim();
                if (!t.isEmpty()) placedSprinklers.add(t);
            }
        }
        rebuildAllSprinklerVisuals(entityPane);

        int rCount = Integer.parseInt(p.getProperty("mine.rock.count", "0"));
        for (int i = 0; i < rCount; i++) {
            String raw = p.getProperty("mine.rock." + i);
            if (raw == null || raw.isEmpty()) continue;
            String[] parts = raw.split("\\|");
            if (parts.length >= 3) {
                for (RockNode r : rocks) {
                    if (r.key.equals(parts[0])) {
                        r.hp = Integer.parseInt(parts[1]);
                        r.broken = (Integer.parseInt(parts[2]) == 1);
                        r.view.setVisible(!r.broken);
                    }
                }
            }
        }
    }
}
