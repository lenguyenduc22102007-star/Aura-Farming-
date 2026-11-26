package view.npc;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.transform.Rotate;
import model.game_item.CornSeed;
import model.game_item.PotatoSeed;
import model.game_item.Seed;
import model.game_item.SunflowerSeed;
import model.game_item.TomatoSeed;
import model.game_item.WaterRiceSeed;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.FarmMap;
import model.soil.TerrainType;
import utils.Constant;
import view.assets.Assets;
import view.fx.ParticleSystem;
import view.fx.SoundHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Quản lý hệ thống 6 NPC sống động có kích thước đồng bộ chuẩn với Luffy (khung 108x108, cao 94px; riêng Chopper 64px)
 * và có hoạt ảnh cử động tay chân thực thụ (2 frame đứng thở + 4 frame bước đi vung tay & sải chân như Luffy):
 * 1. Nami & Hancock đi dạo thư giãn quanh khu vực Bể Bơi, ghế tắm nắng và dù biển.
 * 2. Zoro, Sanji, Chopper, Usopp đi dạo nhộn nhịp khắp nông trại, tương tác [E] trò chuyện & tặng quà mỗi ngày.
 */
public class NpcManager {

    public static class PoolLady {
        public final String name;
        public double x;
        public double y;
        public double targetX;
        public double targetY;
        public final double minX, maxX, minY, maxY;
        public final double moveSpeed = 52.0;
        public boolean facingLeft;
        public double pauseTimer;
        public double animTimer = 0.0;

        public final Image[] idleFrames;
        public final Image[] walkFrames;
        public final Group root;
        public final ImageView sprite;
        public final Rotate strideLean;
        public final Label nameTag;
        public final Label bubble;
        public final String[] idleLines;
        public double nextLineTimer;
        public double bubbleHideTimer = 0.0;

        PoolLady(String name, Image[] idleFrames, Image[] walkFrames,
                 double startX, double startY,
                 double minX, double maxX, double minY, double maxY,
                 boolean faceLeft, String tagColor,
                 String[] idleLines, double initialDelay) {
            this.name = name;
            this.idleFrames = idleFrames;
            this.walkFrames = walkFrames;
            this.x = startX;
            this.y = startY;
            this.targetX = startX;
            this.targetY = startY;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.facingLeft = faceLeft;
            this.pauseTimer = 1.5 + initialDelay * 0.4;
            this.idleLines = idleLines;
            this.nextLineTimer = initialDelay;

            // Bóng chân và khung 108x108 đồng bộ y hệt Luffy (tâm x=22, chân chạm y=68)
            Ellipse shadow = new Ellipse(22, 66, 21, 7);
            shadow.setFill(Color.rgb(18, 28, 36, 0.30));

            Image initImg = (idleFrames != null && idleFrames.length > 0) ? idleFrames[0] : null;
            this.sprite = new ImageView(initImg);
            this.sprite.setFitWidth(108);
            this.sprite.setFitHeight(108);
            this.sprite.setSmooth(false);
            this.sprite.setX(-32);
            this.sprite.setY(-38);
            this.sprite.setScaleX(faceLeft ? -1.0 : 1.0);
            this.strideLean = new Rotate(0.0, 22.0, 66.0);
            this.sprite.getTransforms().add(this.strideLean);

            this.nameTag = createNameTag(name, tagColor, -42.0);
            this.bubble = createSpeechBubble("#FFF5FA", "#D85A8A", "#4A182E");
            this.bubble.setVisible(false);

            this.root = new Group(shadow, this.sprite, this.nameTag);
            this.root.setManaged(false);
            this.root.setMouseTransparent(true);
            this.root.setTranslateX(startX);
            this.root.setTranslateY(startY);
            this.root.setUserData(startY + 50.0);
        }
    }

    public static class BotActor {
        public final String id;
        public final String name;
        public double x;
        public double y;
        public double targetX;
        public double targetY;
        public final double moveSpeed; // px / giây
        public final boolean isCompact; // true cho Chopper
        public boolean facingLeft = false;
        public double pauseTimer = 1.0;
        public double animTimer = 0.0;
        public double nextIdleChatTimer;
        public double bubbleVisibleTimer = 0.0;
        public int lastGiftDay = -1;

        public final Image[] idleFrames;
        public final Image[] walkFrames;
        public final Group root;
        public final ImageView sprite;
        public final Rotate strideLean;
        public final Label nameTag;
        public final Label bubble;
        public final String[] idleLines;
        public final String[] talkLines;

        BotActor(String id, String name, Image[] idleFrames, Image[] walkFrames,
                 double startX, double startY,
                 double moveSpeed, boolean isCompact, String tagColor,
                 String[] idleLines, String[] talkLines, double firstChatDelay) {
            this.id = id;
            this.name = name;
            this.idleFrames = idleFrames;
            this.walkFrames = walkFrames;
            this.x = startX;
            this.y = startY;
            this.targetX = startX;
            this.targetY = startY;
            this.moveSpeed = moveSpeed;
            this.isCompact = isCompact;
            this.idleLines = idleLines;
            this.talkLines = talkLines;
            this.nextIdleChatTimer = firstChatDelay;

            // Khung nhân vật 108x108 đồng bộ 100% với Luffy (cao 94px, chân y=68; riêng Chopper cao 64px dễ thương)
            Ellipse shadow = new Ellipse(22, 66, isCompact ? 15.0 : 21.0, isCompact ? 5.5 : 7.0);
            shadow.setFill(Color.rgb(16, 24, 20, 0.30));

            Image initImg = (idleFrames != null && idleFrames.length > 0) ? idleFrames[0] : null;
            this.sprite = new ImageView(initImg);
            this.sprite.setFitWidth(108);
            this.sprite.setFitHeight(108);
            this.sprite.setSmooth(false);
            this.sprite.setX(-32);
            this.sprite.setY(-38);
            this.strideLean = new Rotate(0.0, 22.0, 66.0);
            this.sprite.getTransforms().add(this.strideLean);

            double tagTopY = isCompact ? -12.0 : -42.0;
            this.nameTag = createNameTag(name, tagColor, tagTopY);

            this.bubble = createSpeechBubble("#FFFDF2", "#7A4B24", "#2E1808");
            this.bubble.setVisible(false);

            this.root = new Group(shadow, this.sprite, this.nameTag);
            this.root.setManaged(false);
            this.root.setMouseTransparent(true);
            this.root.setTranslateX(startX);
            this.root.setTranslateY(startY);
            this.root.setUserData(startY + 50.0);
        }
    }

    private final FarmMap farmMap;
    private final BiPredicate<Double, Double> structureCollisionCheck;
    private final Random rng = new Random();
    private final List<PoolLady> poolLadies = new ArrayList<>();
    private final List<BotActor> bots = new ArrayList<>();

    public NpcManager(FarmMap farmMap, BiPredicate<Double, Double> structureCollisionCheck) {
        this.farmMap = farmMap;
        this.structureCollisionCheck = structureCollisionCheck;
    }

    public void attachTo(Pane entityPane, double poolLoungerX, double poolTopLoungerY) {
        poolLadies.clear();
        bots.clear();

        // 1. Nami & Hancock đi dạo quanh sân nắng cạnh Bể Bơi (giữa hàng ghế tắm nắng, dù che nắng và lối dạo bể bơi)
        double deckMinX = poolLoungerX - 28.0;
        double deckMaxX = poolLoungerX + 148.0;
        double deckMinY = poolTopLoungerY - 36.0;
        double deckMaxY = poolTopLoungerY + 132.0;

        PoolLady nami = new PoolLady(
            "Nami",
            Assets.NPC_NAMI_IDLE, Assets.NPC_NAMI_WALK,
            poolLoungerX + 52.0, poolTopLoungerY - 22.0,
            deckMinX, deckMaxX, deckMinY, deckMaxY - 56.0,
            true, "#FFB74D",
            new String[] {
                "Nami: Hôm nay thu hoạch được bao nhiêu Berry rồi?",
                "Nami: Nước hồ bơi mát lạnh thích thật đấy~!",
                "Nami: Chăm chỉ kiếm tiền rồi qua Casino thử vận may xem!"
            },
            2.5
        );
        PoolLady hancock = new PoolLady(
            "Hancock",
            Assets.NPC_HANCOCK_IDLE, Assets.NPC_HANCOCK_WALK,
            poolLoungerX + 68.0, poolTopLoungerY + 54.0,
            deckMinX, deckMaxX, deckMinY + 42.0, deckMaxY,
            true, "#FF80AB",
            new String[] {
                "Hancock: Luffy, lại đây nghỉ ngơi cùng thiếp đi! ♥",
                "Hancock: Ánh nắng ở nông trại này thật xứng với sắc đẹp của thiếp!",
                "Hancock: Bấm [E] nằm ghế cạnh thiếp cho đỡ mỏi chân nào Luffy~!"
            },
            5.5
        );
        poolLadies.add(nami);
        poolLadies.add(hancock);

        for (PoolLady lady : poolLadies) {
            pickNewPoolLadySpot(lady);
            entityPane.getChildren().addAll(lady.root, lady.bubble);
        }

        // 2. 4 Bot đi dạo khắp nông trại: Zoro, Sanji, Chopper (nhỏ nhắn), Usopp — tất cả đều vung tay chân như Luffy!
        double[] zoroSpawn    = findSafeWalkableSpot(1100, 740);
        double[] sanjiSpawn   = findSafeWalkableSpot(820, 960);
        double[] chopperSpawn = findSafeWalkableSpot(1120, 1460);
        double[] usoppSpawn   = findSafeWalkableSpot(1180, 2660);

        bots.add(new BotActor(
            "zoro", "Zoro", Assets.NPC_ZORO_IDLE, Assets.NPC_ZORO_WALK,
            zoroSpawn[0], zoroSpawn[1],
            92.0, false, "#95F985",
            new String[] {
                "Quái lạ, nhà chính ở hướng nào nhỉ?",
                "Rõ ràng ta đi thẳng mà sao lại lạc nữa rồi?!",
                "Kiếm sĩ thực thụ không bao giờ cần bản đồ!"
            },
            new String[] {
                "Zoro: Này Luffy! Cẩn thận kẻo lạc đường giống... à không, quên đi!",
                "Zoro: Trồng trọt xong nhớ rèn luyện thể lực đấy!",
                "Zoro: Sòng bạc mới mở bên kia cầu đông vui phết!"
            },
            3.0
        ));

        bots.add(new BotActor(
            "sanji", "Sanji", Assets.NPC_SANJI_IDLE, Assets.NPC_SANJI_WALK,
            sanjiSpawn[0], sanjiSpawn[1],
            102.0, false, "#FFE066",
            new String[] {
                "Ai cần nấu đồ ăn thì gọi tôi!",
                "Nami-san~ Hancock-sama~ nước trái cây mát lạnh đây! ♥",
                "Nguyên liệu nông trại tươi thế này nấu Lẩu là tuyệt hảo!"
            },
            new String[] {
                "Sanji: Đói bụng thì ghé đống lửa trại làm món nướng nhé Luffy!",
                "Sanji: Đừng bao giờ lãng phí thức ăn trên nông trại!",
                "Sanji: Tôi vừa mang đồ uống ra hồ bơi cho quý cô Nami-san~!"
            },
            5.2
        ));

        bots.add(new BotActor(
            "chopper", "Chopper", Assets.NPC_CHOPPER_IDLE, Assets.NPC_CHOPPER_WALK,
            chopperSpawn[0], chopperSpawn[1],
            88.0, true, "#FFB3D9",
            new String[] {
                "Nông trại hôm nay đẹp quá!",
                "Lalala~ không khí trong lành thật dễ chịu!",
                "Nhớ uống nước đầy đủ khi làm đồng nhiều nhé!"
            },
            new String[] {
                "Chopper: Hihi khen tớ nữa đi đồ ngốc~ tớ không vui đâu mà!!",
                "Chopper: Bác sĩ Chopper nhắc cậu nhớ ăn no trước khi cuốc đất nhé!",
                "Chopper: Mấy chú bò với gà trong chuồng dễ thương cực kỳ luôn!"
            },
            4.0
        ));

        bots.add(new BotActor(
            "usopp", "Usopp", Assets.NPC_USOPP_IDLE, Assets.NPC_USOPP_WALK,
            usoppSpawn[0], usoppSpawn[1],
            96.0, false, "#FFD280",
            new String[] {
                "Ta là Thuyền trưởng Usopp chỉ huy 8000 nông dân!",
                "Hôm qua ta mới câu được con Thủy Quái dài 10 mét dưới sông!",
                "Cần hạt giống quý hiếm thì cứ tìm Xạ thủ Usopp!"
            },
            new String[] {
                "Usopp: Hehe Luffy! Nghe chiến tích thắng Xì Dách 10 ván thông của ta chưa?!",
                "Usopp: Ra bờ sông câu cá đi, nay cá cắn câu nhiều lắm!",
                "Usopp: Luống đất 2x2 mới quy hoạch nhìn chuyên nghiệp thật đấy!"
            },
            6.5
        ));

        for (BotActor bot : bots) {
            pickNewBotDestination(bot);
            entityPane.getChildren().addAll(bot.root, bot.bubble);
        }
    }

    public void update(double dtSec, double animTimeSec) {
        // 1. Cập nhật Nami & Hancock đi dạo quanh khu vực bể bơi (cử động tay chân 4 frame như Luffy)
        for (int i = 0; i < poolLadies.size(); i++) {
            PoolLady lady = poolLadies.get(i);
            boolean moving = false;

            if (lady.pauseTimer > 0.0) {
                lady.pauseTimer -= dtSec;
                if (lady.pauseTimer <= 0.0) {
                    pickNewPoolLadySpot(lady);
                }
            } else {
                double dx = lady.targetX - lady.x;
                double dy = lady.targetY - lady.y;
                double dist = Math.hypot(dx, dy);
                if (dist <= 4.0) {
                    lady.pauseTimer = 3.2 + rng.nextDouble() * 4.0;
                    // Khi đứng nghỉ bên hồ bơi, thường quay mặt về phía bể bơi để thư giãn
                    lady.facingLeft = true;
                    lady.sprite.setScaleX(-1.0);
                } else {
                    double step = Math.min(dist, lady.moveSpeed * dtSec);
                    double nx = lady.x + (dx / dist) * step;
                    double ny = lady.y + (dy / dist) * step;
                    if (!isPoolLadyBlocked(nx, ny)) {
                        lady.x = nx;
                        lady.y = ny;
                        moving = true;
                        if (Math.abs(dx) > 0.35) {
                            lady.facingLeft = (dx < 0);
                            lady.sprite.setScaleX(lady.facingLeft ? -1.0 : 1.0);
                        }
                    } else {
                        lady.pauseTimer = 1.5 + rng.nextDouble() * 2.0;
                        pickNewPoolLadySpot(lady);
                    }
                }
            }

            lady.animTimer += dtSec;
            double bobY = 0.0;
            if (moving && lady.walkFrames != null && lady.walkFrames.length == 4) {
                int fIdx = ((int) (lady.animTimer / 0.12)) % 4;
                lady.sprite.setImage(lady.walkFrames[fIdx]);
                bobY = -Math.abs(Math.sin(lady.animTimer * 13.0)) * 2.2;
                lady.strideLean.setAngle(Math.sin(lady.animTimer * 13.0) * 2.0);
            } else if (lady.idleFrames != null && lady.idleFrames.length >= 2) {
                int iIdx = ((int) ((animTimeSec + i * 0.5) / 0.45)) % 2;
                lady.sprite.setImage(lady.idleFrames[iIdx]);
                lady.strideLean.setAngle(0.0);
            }

            lady.root.setTranslateX(lady.x);
            lady.root.setTranslateY(lady.y + bobY);
            lady.root.setUserData(lady.y + 50.0);

            double headTopY = lady.y - 66.0 + bobY;
            if (lady.bubbleHideTimer > 0.0) {
                lady.bubbleHideTimer -= dtSec;
                positionBubbleAbove(lady.bubble, lady.x + 22.0, headTopY);
                if (lady.bubbleHideTimer <= 0.0) {
                    lady.bubble.setVisible(false);
                }
            } else {
                lady.nextLineTimer -= dtSec;
                if (lady.nextLineTimer <= 0.0) {
                    String line = lady.idleLines[rng.nextInt(lady.idleLines.length)];
                    showBubble(lady.bubble, line, lady.x + 22.0, headTopY);
                    lady.bubbleHideTimer = 4.6;
                    lady.nextLineTimer = 8.0 + rng.nextDouble() * 6.5;
                }
            }
        }

        // 2. Cập nhật 4 Bot Zoro, Sanji, Chopper, Usopp đi dạo vung tay chân 4 frame y hệt Luffy
        for (int i = 0; i < bots.size(); i++) {
            BotActor bot = bots.get(i);
            boolean moving = false;

            if (bot.pauseTimer > 0.0) {
                bot.pauseTimer -= dtSec;
                if (bot.pauseTimer <= 0.0) {
                    pickNewBotDestination(bot);
                }
            } else {
                double dx = bot.targetX - bot.x;
                double dy = bot.targetY - bot.y;
                double dist = Math.hypot(dx, dy);
                if (dist <= 6.0) {
                    bot.pauseTimer = 2.0 + rng.nextDouble() * 2.8;
                } else {
                    double step = Math.min(dist, bot.moveSpeed * dtSec);
                    double nx = bot.x + (dx / dist) * step;
                    double ny = bot.y + (dy / dist) * step;
                    if (isWalkableForBot(nx, ny)) {
                        bot.x = nx;
                        bot.y = ny;
                        moving = true;
                        if (Math.abs(dx) > 0.4) {
                            bot.facingLeft = (dx < 0);
                            bot.sprite.setScaleX(bot.facingLeft ? -1.0 : 1.0);
                        }
                    } else {
                        bot.pauseTimer = 0.5 + rng.nextDouble() * 1.0;
                        pickNewBotDestination(bot);
                    }
                }
            }

            bot.animTimer += dtSec;
            double bobY = 0.0;
            if (moving && bot.walkFrames != null && bot.walkFrames.length == 4) {
                // Đổi 4 frame sải chân & đánh tay (0.105s / frame — đồng tốc độ chạy của Luffy!)
                int fIdx = ((int) (bot.animTimer / 0.105)) % 4;
                bot.sprite.setImage(bot.walkFrames[fIdx]);
                bobY = -Math.abs(Math.sin(bot.animTimer * 15.0)) * (bot.isCompact ? 3.2 : 2.5);
                bot.strideLean.setAngle(Math.sin(bot.animTimer * 15.0) * (bot.isCompact ? 3.2 : 2.2));
            } else if (bot.idleFrames != null && bot.idleFrames.length >= 2) {
                int iIdx = ((int) ((animTimeSec + i * 0.35) / 0.42)) % 2;
                bot.sprite.setImage(bot.idleFrames[iIdx]);
                bot.strideLean.setAngle(0.0);
            }

            bot.root.setTranslateX(bot.x);
            bot.root.setTranslateY(bot.y + bobY);
            bot.root.setUserData(bot.y + 50.0);

            double bubbleTopY = bot.y + (bot.isCompact ? -36.0 : -66.0) + bobY;
            if (bot.bubbleVisibleTimer > 0.0) {
                bot.bubbleVisibleTimer -= dtSec;
                positionBubbleAbove(bot.bubble, bot.x + 22.0, bubbleTopY);
                if (bot.bubbleVisibleTimer <= 0.0) {
                    bot.bubble.setVisible(false);
                }
            } else {
                bot.nextIdleChatTimer -= dtSec;
                if (bot.nextIdleChatTimer <= 0.0) {
                    String line = bot.idleLines[rng.nextInt(bot.idleLines.length)];
                    showBubble(bot.bubble, line, bot.x + 22.0, bubbleTopY);
                    bot.bubbleVisibleTimer = 4.2;
                    bot.nextIdleChatTimer = 8.5 + rng.nextDouble() * 7.5;
                }
            }
        }
    }

    private void pickNewPoolLadySpot(PoolLady lady) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double tx = lady.minX + rng.nextDouble() * Math.max(20.0, lady.maxX - lady.minX);
            double ty = lady.minY + rng.nextDouble() * Math.max(20.0, lady.maxY - lady.minY);
            if (!isPoolLadyBlocked(tx, ty)) {
                lady.targetX = tx;
                lady.targetY = ty;
                return;
            }
        }
        lady.targetX = lady.x;
        lady.targetY = lady.y;
    }

    private boolean isPoolLadyBlocked(double lx, double ly) {
        if (farmMap == null) return false;
        int col = (int) ((lx + 22.0) / Constant.CELL_SIZE);
        int row = (int) ((ly + 54.0) / Constant.CELL_SIZE);
        Cell cell = farmMap.getCell(row, col);
        if (cell == null || cell.getTerrain() == TerrainType.WATER) return true;
        return structureCollisionCheck != null && structureCollisionCheck.test(lx + 22.0, ly + 54.0);
    }

    /** Tìm Bot gần người chơi nhất trong bán kính maxDist (tính từ tâm bàn chân x+22, y+54). */
    public BotActor findNearestBot(double playerFootX, double playerFootY, double maxDist) {
        BotActor nearest = null;
        double bestD2 = maxDist * maxDist;
        for (BotActor b : bots) {
            double bx = b.x + 22.0;
            double by = b.y + 54.0;
            double d2 = (playerFootX - bx) * (playerFootX - bx) + (playerFootY - by) * (playerFootY - by);
            if (d2 <= bestD2) {
                bestD2 = d2;
                nearest = b;
            }
        }
        return nearest;
    }

    /**
     * Tương tác [E] với Bot: dừng lại, quay mặt về phía người chơi, nói chuyện vui vẻ
     * và tặng quà ngẫu nhiên ($5-$15 hoặc 1 hạt giống) mỗi ngày 1 lần cho mỗi Bot!
     */
    public void interactWithBot(BotActor bot,
                                double playerFootX,
                                int currentDay,
                                Player player,
                                ParticleSystem particles,
                                Consumer<String> floatingTextCb,
                                Consumer<String> notifyCb) {
        if (bot == null) return;
        bot.pauseTimer = 5.0;
        double bx = bot.x + 22.0;
        bot.facingLeft = (playerFootX < bx);
        bot.sprite.setScaleX(bot.facingLeft ? -1.0 : 1.0);
        double bubbleTopY = bot.y + (bot.isCompact ? -36.0 : -66.0);

        if (player != null && bot.lastGiftDay < currentDay) {
            bot.lastGiftDay = currentDay;
            SoundHook.playPickup();
            if (particles != null) {
                particles.spawnHeartBurst(bx, bot.y + 8.0);
            }
            if (rng.nextBoolean()) {
                int cash = 5 + rng.nextInt(11); // $5 .. $15
                player.addMoney(cash);
                String msg = bot.name + ": Tặng cậu $" + cash + " uống nước nhé!";
                showBubble(bot.bubble, msg, bx, bubbleTopY);
                bot.bubbleVisibleTimer = 5.0;
                if (floatingTextCb != null) floatingTextCb.accept("+$" + cash + " từ " + bot.name + "!");
                if (notifyCb != null) notifyCb.accept("🎁 " + bot.name + " tặng bạn +$" + cash + " quà gặp mặt hôm nay!");
            } else {
                Seed giftSeed = createRandomGiftSeed();
                player.getInventory().addItem(giftSeed, 1);
                String msg = bot.name + ": Tặng cậu 1 " + giftSeed.getName() + " trồng thử nè!";
                showBubble(bot.bubble, msg, bx, bubbleTopY);
                bot.bubbleVisibleTimer = 5.0;
                if (floatingTextCb != null) floatingTextCb.accept("+1 " + giftSeed.getName() + "!");
                if (notifyCb != null) notifyCb.accept("🌱 " + bot.name + " tặng bạn 1× " + giftSeed.getName() + "!");
            }
        } else {
            SoundHook.playMenuClick();
            String line = bot.talkLines[rng.nextInt(bot.talkLines.length)];
            showBubble(bot.bubble, line, bx, bubbleTopY);
            bot.bubbleVisibleTimer = 4.6;
            if (notifyCb != null) notifyCb.accept(line);
        }
    }

    private Seed createRandomGiftSeed() {
        int pick = rng.nextInt(5);
        switch (pick) {
            case 0:  return new TomatoSeed();
            case 1:  return new PotatoSeed();
            case 2:  return new CornSeed();
            case 3:  return new SunflowerSeed();
            default: return new WaterRiceSeed();
        }
    }

    private void pickNewBotDestination(BotActor bot) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = 150.0 + rng.nextDouble() * 360.0;
            double candX = bot.x + Math.cos(angle) * dist;
            double candY = bot.y + Math.sin(angle) * dist;
            if (crossesRiverInvalidly(bot.y, candY, candX)) {
                continue;
            }
            if (isWalkableForBot(candX, candY)) {
                bot.targetX = candX;
                bot.targetY = candY;
                return;
            }
        }
        bot.targetX = bot.x;
        bot.targetY = bot.y;
    }

    private boolean crossesRiverInvalidly(double curY, double nextY, double nextX) {
        double riverMidY = 34.5 * Constant.CELL_SIZE;
        boolean diffSide = (curY < riverMidY && nextY > riverMidY) || (curY > riverMidY && nextY < riverMidY);
        if (!diffSide) return false;
        return nextX < 1080.0 || nextX > 1280.0;
    }

    private double[] findSafeWalkableSpot(double prefX, double prefY) {
        if (isWalkableForBot(prefX, prefY)) {
            return new double[]{prefX, prefY};
        }
        for (int r = 1; r <= 8; r++) {
            for (int a = 0; a < 8; a++) {
                double rad = a * (Math.PI / 4.0);
                double tx = prefX + Math.cos(rad) * r * 48.0;
                double ty = prefY + Math.sin(rad) * r * 48.0;
                if (isWalkableForBot(tx, ty)) {
                    return new double[]{tx, ty};
                }
            }
        }
        return new double[]{prefX, prefY};
    }

    private boolean isWalkableForBot(double bx, double by) {
        if (farmMap == null) return false;
        double mapW = farmMap.getWidth() * Constant.CELL_SIZE;
        double mapH = farmMap.getHeight() * Constant.CELL_SIZE;
        if (bx < 64.0 || bx > mapW - 96.0 || by < 64.0 || by > mapH - 110.0) {
            return false;
        }
        int col = (int) ((bx + 22.0) / Constant.CELL_SIZE);
        int row = (int) ((by + 54.0) / Constant.CELL_SIZE);
        Cell cell = farmMap.getCell(row, col);
        if (cell == null) return false;
        if (cell.getTerrain() == TerrainType.WATER || cell.getTerrain() == TerrainType.DIRT || cell.getCrop() != null) {
            return false;
        }
        if (structureCollisionCheck != null && structureCollisionCheck.test(bx + 22.0, by + 54.0)) {
            return false;
        }
        return true;
    }

    private static Label createNameTag(String name, String tagColor, double layoutY) {
        Label lbl = new Label(name);
        lbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + tagColor + ";" +
            "-fx-background-color: rgba(20, 14, 10, 0.78);" +
            "-fx-padding: 1 6 1 6; -fx-background-radius: 5px;"
        );
        lbl.setMouseTransparent(true);
        lbl.setManaged(false);
        lbl.setLayoutX(22.0 - name.length() * 3.8 - 6.0);
        lbl.setLayoutY(layoutY);
        return lbl;
    }

    private static Label createSpeechBubble(String bgHex, String borderHex, String textHex) {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.setMaxWidth(226);
        lbl.setAlignment(Pos.CENTER);
        lbl.setMouseTransparent(true);
        lbl.setManaged(false);
        lbl.setUserData(9992.0);
        lbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + textHex + ";" +
            "-fx-background-color: " + bgHex + ";" +
            "-fx-border-color: " + borderHex + "; -fx-border-width: 1.8px;" +
            "-fx-padding: 4 8 4 8; -fx-background-radius: 9px; -fx-border-radius: 9px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 5, 0.2, 0, 2);"
        );
        return lbl;
    }

    private static void showBubble(Label bubble, String text, double centerX, double topY) {
        bubble.setText(text);
        bubble.autosize();
        positionBubbleAbove(bubble, centerX, topY);
        bubble.setVisible(true);
    }

    private static void positionBubbleAbove(Label bubble, double centerX, double topY) {
        double bw = bubble.getWidth() > 10 ? bubble.getWidth() : Math.min(226.0, Math.max(115.0, bubble.getText().length() * 6.5));
        bubble.setLayoutX(centerX - bw * 0.5);
        bubble.setLayoutY(topY);
    }
}
