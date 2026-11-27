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
import model.game_item.CookedFoodItem;
import model.game_item.CornSeed;
import model.game_item.FactoryProductItem;
import model.game_item.Item;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Quản lý hệ thống 6 NPC sống động + Độ Thân Mật (0..5 Tim ❤) + Tặng Quà [G] + Đặc Quyền >= 3 Tim + Tụ Hội Đêm Pháo Hoa:
 * 1. Nami (>=3 ❤: Giảm 15% giá Shop) & Hancock (>=3 ❤: Tắm nắng hồi Đói/Khát x2 & buff chạy x2 thời gian).
 * 2. Zoro (>=3 ❤: Giảm 80% sâu bệnh), Sanji (>=3 ❤: Tặng miễn phí 1 món ăn cao cấp mỗi ngày khi bấm [E]),
 *    Chopper (>=3 ❤: Giảm 25% tốc độ tụt Đói/Khát), Usopp (>=3 ❤: Tăng +20% tỉ lệ cá hiếm / Vương Miện).
 */
public class NpcManager {

    public static final String[] ALL_NPC_IDS = { "nami", "hancock", "zoro", "sanji", "chopper", "usopp" };

    /** Đại diện chung cho bất kỳ NPC nào trong 6 NPC khi đứng gần tương tác [E] hoặc [G]. */
    public static final class NpcTarget {
        public final String id;
        public final String name;
        public final double x;
        public final double y;
        public final PoolLady poolLady;
        public final BotActor botActor;

        NpcTarget(String id, String name, double x, double y, PoolLady poolLady, BotActor botActor) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.poolLady = poolLady;
            this.botActor = botActor;
        }
    }

    public static class PoolLady {
        public final String id;
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

        PoolLady(String id, String name, Image[] idleFrames, Image[] walkFrames,
                 double startX, double startY,
                 double minX, double maxX, double minY, double maxY,
                 boolean faceLeft, String tagColor,
                 String[] idleLines, double initialDelay) {
            this.id = id;
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

            this.nameTag = createNameTag(name + " ♡♡♡♡♡", tagColor, -42.0);
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
        public final double moveSpeed;
        public final boolean isCompact;
        public boolean facingLeft = false;
        public double pauseTimer = 1.0;
        public double animTimer = 0.0;
        public double nextIdleChatTimer;
        public double bubbleVisibleTimer = 0.0;
        public int lastTalkRewardDay = -1;

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
            this.nameTag = createNameTag(name + " ♡♡♡♡♡", tagColor, tagTopY);

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

    /** Độ thân mật (0.0 .. 5.0 Tim) và ngày gần nhất nhận quà của 6 NPC. */
    private final Map<String, Double> heartsMap = new LinkedHashMap<>();
    private final Map<String, Integer> lastGiftedByPlayerDay = new LinkedHashMap<>();
    private boolean fireworksGatheringActive = false;
    private double gatheringCenterX = 520.0;
    private double gatheringCenterY = 2580.0;

    public NpcManager(FarmMap farmMap, BiPredicate<Double, Double> structureCollisionCheck) {
        this.farmMap = farmMap;
        this.structureCollisionCheck = structureCollisionCheck;
        for (String id : ALL_NPC_IDS) {
            heartsMap.put(id, 0.0);
            lastGiftedByPlayerDay.put(id, -1);
        }
    }

    public double getHearts(String npcId) {
        if (npcId == null) return 0.0;
        return heartsMap.getOrDefault(npcId.toLowerCase(), 0.0);
    }

    public boolean hasPerkUnlocked(String npcId) {
        return getHearts(npcId) >= 3.0;
    }

    public boolean hasGiftedToday(String npcId, int currentDay) {
        if (npcId == null) return false;
        return lastGiftedByPlayerDay.getOrDefault(npcId.toLowerCase(), -1) == currentDay;
    }

    public static String formatHeartBar(double hearts) {
        StringBuilder sb = new StringBuilder();
        int full = (int) Math.floor(hearts + 0.001);
        boolean half = (hearts - full) >= 0.45;
        for (int i = 0; i < 5; i++) {
            if (i < full) sb.append("❤");
            else if (i == full && half) sb.append("❥");
            else sb.append("♡");
        }
        return sb.toString();
    }

    public static String getPerkDescription(String npcId) {
        if (npcId == null) return "";
        switch (npcId.toLowerCase()) {
            case "nami":    return "Giảm giá 15% toàn bộ mặt hàng trong Shop";
            case "sanji":   return "Mỗi ngày bấm [E] được tặng 1 món ăn cao cấp miễn phí";
            case "zoro":    return "Giảm 80% tỉ lệ sâu bệnh phá ruộng mỗi ngày";
            case "hancock": return "Tắm nắng Bể Bơi hồi Đói/Khát x2 & buff chạy nhanh lâu gấp đôi";
            case "chopper": return "Giảm 25% tốc độ tụt Đói & Khát của nhân vật";
            case "usopp":   return "Tăng +20% tỉ lệ câu cá hiếm & cá Vương Miện";
            default:        return "";
        }
    }

    public void attachTo(Pane entityPane, double poolLoungerX, double poolTopLoungerY) {
        poolLadies.clear();
        bots.clear();

        double deckMinX = poolLoungerX - 28.0;
        double deckMaxX = poolLoungerX + 148.0;
        double deckMinY = poolTopLoungerY - 36.0;
        double deckMaxY = poolTopLoungerY + 132.0;

        PoolLady nami = new PoolLady(
            "nami", "Nami",
            Assets.NPC_NAMI_IDLE, Assets.NPC_NAMI_WALK,
            poolLoungerX + 52.0, poolTopLoungerY - 22.0,
            deckMinX, deckMaxX, deckMinY, deckMaxY - 56.0,
            true, "#FFB74D",
            new String[] {
                "Nami: Hôm nay giá thị trường có món đang SỐT GIÁ +50% đấy!",
                "Nami: Đạt 3 Tim với tớ là được giảm giá 15% cả cửa hàng luôn nha~!",
                "Nami: Nước hồ bơi mát lạnh thích thật đấy~!"
            },
            2.5
        );
        PoolLady hancock = new PoolLady(
            "hancock", "Hancock",
            Assets.NPC_HANCOCK_IDLE, Assets.NPC_HANCOCK_WALK,
            poolLoungerX + 68.0, poolTopLoungerY + 54.0,
            deckMinX, deckMaxX, deckMinY + 42.0, deckMaxY,
            true, "#FF80AB",
            new String[] {
                "Hancock: Luffy, lại đây nghỉ ngơi cùng thiếp đi! ♥",
                "Hancock: Tặng quà cho thiếp lên 3 Tim để tắm nắng hồi sức gấp đôi nhé~!",
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

        double[] zoroSpawn    = findSafeWalkableSpot(1100, 740);
        double[] sanjiSpawn   = findSafeWalkableSpot(820, 960);
        double[] chopperSpawn = findSafeWalkableSpot(1120, 1460);
        double[] usoppSpawn   = findSafeWalkableSpot(1180, 2660);

        bots.add(new BotActor(
            "zoro", "Zoro", Assets.NPC_ZORO_IDLE, Assets.NPC_ZORO_WALK,
            zoroSpawn[0], zoroSpawn[1],
            92.0, false, "#95F985",
            new String[] {
                "Quái lạ, Nhà Máy ở hướng nào nhỉ?",
                "Có ta canh ruộng (3 Tim) thì lũ sâu bọ đừng hòng bén mảng!",
                "Kiếm sĩ thực thụ không bao giờ cần bản đồ!"
            },
            new String[] {
                "Zoro: Này Luffy! Lên 3 Tim ta sẽ chém bay 80% sâu bệnh cho ruộng của cậu!",
                "Zoro: Khu mỏ đá phía Đông Nam có nhiều quặng quý đấy!",
                "Zoro: Sòng bạc với Nhà Máy dạo này nhộn nhịp phết!"
            },
            3.0
        ));

        bots.add(new BotActor(
            "sanji", "Sanji", Assets.NPC_SANJI_IDLE, Assets.NPC_SANJI_WALK,
            sanjiSpawn[0], sanjiSpawn[1],
            102.0, false, "#FFE066",
            new String[] {
                "Ai cần nấu đồ ăn thì gọi tôi!",
                "Nami-san~ Hancock-sama~ nước ép cà chua mát lạnh đây! ♥",
                "Thân với tôi đủ 3 Tim, mỗi ngày tôi nấu tặng 1 đại tiệc miễn phí!"
            },
            new String[] {
                "Sanji: Đói bụng thì ghé đống lửa hoặc Nhà Máy chế biến nhé Luffy!",
                "Sanji: Đủ 3 Tim cứ gặp tôi mỗi ngày nhận ngay 1 món đại tiệc!",
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
                "Bác sĩ Chopper sẽ giúp cậu dẻo dai giảm 25% đói khát khi đạt 3 Tim!",
                "Nhớ uống nước đầy đủ khi làm đồng nhiều nhé!"
            },
            new String[] {
                "Chopper: Hihi tặng quà cho tớ nữa đi đồ ngốc~ tớ không vui đâu mà!!",
                "Chopper: Bác sĩ Chopper nhắc cậu nhớ ăn no trước khi đập đá mỏ nhé!",
                "Chopper: Vườn cây ăn quả phía Tây Nam trồng Cam Táo thơm lắm!"
            },
            4.0
        ));

        bots.add(new BotActor(
            "usopp", "Usopp", Assets.NPC_USOPP_IDLE, Assets.NPC_USOPP_WALK,
            usoppSpawn[0], usoppSpawn[1],
            96.0, false, "#FFD280",
            new String[] {
                "Ta là Thuyền trưởng Usopp chỉ huy 8000 nông dân!",
                "Thân thiết 3 Tim ta chỉ bí kíp +20% câu cá Vương Miện cho!",
                "Ngày 7 với 21 nhớ ra sông thi câu cá giật giải $1.000 nhé!"
            },
            new String[] {
                "Usopp: Hehe Luffy! Lên 3 Tim với Xạ thủ Usopp là câu cá hiếm dễ như trở bàn tay!",
                "Usopp: Ra bờ sông câu cá đi, nay cá cắn câu nhiều lắm!",
                "Usopp: Đổi Sắt với Vàng ở khu mỏ làm Vòi Tưới Tự Động xịn cực!"
            },
            6.5
        ));

        for (BotActor bot : bots) {
            pickNewBotDestination(bot);
            entityPane.getChildren().addAll(bot.root, bot.bubble);
        }

        refreshAllNameTags();
    }

    public void refreshAllNameTags() {
        for (PoolLady lady : poolLadies) {
            String txt = lady.name + " " + formatHeartBar(getHearts(lady.id));
            lady.nameTag.setText(txt);
            lady.nameTag.setLayoutX(22.0 - txt.length() * 3.4 - 4.0);
        }
        for (BotActor bot : bots) {
            String txt = bot.name + " " + formatHeartBar(getHearts(bot.id));
            bot.nameTag.setText(txt);
            bot.nameTag.setLayoutX(22.0 - txt.length() * 3.4 - 4.0);
        }
    }

    /** Chế độ Đêm Hội Pháo Hoa (từ 18:00 ngày 14, 28...): toàn bộ 6 NPC tập trung vui hội quanh Khu Bể Bơi! */
    public void setFireworksGathering(boolean active, double poolCenterX, double poolCenterY) {
        if (this.fireworksGatheringActive == active) return;
        this.fireworksGatheringActive = active;
        this.gatheringCenterX = poolCenterX;
        this.gatheringCenterY = poolCenterY;
        if (active) {
            double[][] offsets = {
                { -95.0,  96.0 },
                {  -25.0, 108.0 },
                {   48.0, 108.0 },
                {  118.0,  96.0 }
            };
            for (int i = 0; i < bots.size(); i++) {
                BotActor b = bots.get(i);
                double[] off = offsets[i % offsets.length];
                double[] safe = findSafeWalkableSpot(poolCenterX + off[0], poolCenterY + off[1]);
                b.x = safe[0];
                b.y = safe[1];
                b.targetX = safe[0];
                b.targetY = safe[1];
                b.pauseTimer = 3.0;
                b.root.setTranslateX(b.x);
                b.root.setTranslateY(b.y);
                b.root.setUserData(b.y + 50.0);
                showBubble(b.bubble, "🎆 Woa!! Pháo hoa trên bể bơi đẹp tuyệt vời!!", b.x + 22.0, b.y - 64.0);
                b.bubbleVisibleTimer = 6.0;
            }
        }
    }

    /** Cộng thêm Tim cho toàn bộ 6 NPC cùng lúc (ví dụ tham dự Đêm Pháo Hoa +0.5 Tim). */
    public void addHeartsToAll(double delta) {
        for (String id : ALL_NPC_IDS) {
            double next = Math.max(0.0, Math.min(5.0, getHearts(id) + delta));
            heartsMap.put(id, next);
        }
        refreshAllNameTags();
    }

    public void update(double dtSec, double animTimeSec) {
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
                    String line = fireworksGatheringActive
                            ? (lady.name + ": 🎆 Pháo hoa lung linh quá đi mất~!")
                            : lady.idleLines[rng.nextInt(lady.idleLines.length)];
                    showBubble(lady.bubble, line, lady.x + 22.0, headTopY);
                    lady.bubbleHideTimer = 4.6;
                    lady.nextLineTimer = 8.0 + rng.nextDouble() * 6.5;
                }
            }
        }

        for (int i = 0; i < bots.size(); i++) {
            BotActor bot = bots.get(i);
            boolean moving = false;

            if (fireworksGatheringActive) {
                // Giữ 4 Bot đứng quây quần thưởng thức pháo hoa quanh Bể Bơi
                bot.pauseTimer = 2.0;
            } else if (bot.pauseTimer > 0.0) {
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
                    String line = fireworksGatheringActive
                            ? (bot.name + ": 🎆 Đêm hội pháo hoa tuyệt đỉnh quá anh em ơi!!")
                            : bot.idleLines[rng.nextInt(bot.idleLines.length)];
                    showBubble(bot.bubble, line, bot.x + 22.0, bubbleTopY);
                    bot.bubbleVisibleTimer = 4.2;
                    bot.nextIdleChatTimer = 8.5 + rng.nextDouble() * 7.5;
                }
            }
        }
    }

    /** Tìm NPC gần nhất trong cả 6 NPC (Nami, Hancock, Zoro, Sanji, Chopper, Usopp). */
    public NpcTarget findNearestNpc(double playerFootX, double playerFootY, double maxDist) {
        NpcTarget best = null;
        double bestD2 = maxDist * maxDist;
        for (PoolLady l : poolLadies) {
            double lx = l.x + 22.0;
            double ly = l.y + 54.0;
            double d2 = (playerFootX - lx) * (playerFootX - lx) + (playerFootY - ly) * (playerFootY - ly);
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = new NpcTarget(l.id, l.name, l.x, l.y, l, null);
            }
        }
        for (BotActor b : bots) {
            double bx = b.x + 22.0;
            double by = b.y + 54.0;
            double d2 = (playerFootX - bx) * (playerFootX - bx) + (playerFootY - by) * (playerFootY - by);
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = new NpcTarget(b.id, b.name, b.x, b.y, null, b);
            }
        }
        return best;
    }

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

    /** Tương tác trò chuyện [E] với bất kỳ ai trong 6 NPC (hiển thị thanh Tim + đặc quyền Sanji >= 3 Tim tặng đồ ăn cao cấp!). */
    public void interactWithTarget(NpcTarget target,
                                   double playerFootX,
                                   int currentDay,
                                   Player player,
                                   ParticleSystem particles,
                                   Consumer<String> floatingTextCb,
                                   Consumer<String> notifyCb) {
        if (target == null) return;
        double h = getHearts(target.id);
        String heartStr = formatHeartBar(h);

        if (target.poolLady != null) {
            PoolLady lady = target.poolLady;
            lady.pauseTimer = 5.0;
            lady.facingLeft = (playerFootX < lady.x + 22.0);
            lady.sprite.setScaleX(lady.facingLeft ? -1.0 : 1.0);
            String perkNote = (h >= 3.0) ? " [Đã mở: " + getPerkDescription(lady.id) + "]" : " (Tặng quà [G] lên 3❤ mở đặc quyền!)";
            String msg = "[" + heartStr + "]\n" + lady.idleLines[rng.nextInt(lady.idleLines.length)];
            showBubble(lady.bubble, msg, lady.x + 22.0, lady.y - 72.0);
            lady.bubbleHideTimer = 5.2;
            SoundHook.playMenuClick();
            if (notifyCb != null) notifyCb.accept(lady.name + " " + heartStr + perkNote);
            return;
        }

        BotActor bot = target.botActor;
        if (bot == null) return;
        bot.pauseTimer = 5.0;
        double bx = bot.x + 22.0;
        bot.facingLeft = (playerFootX < bx);
        bot.sprite.setScaleX(bot.facingLeft ? -1.0 : 1.0);
        double bubbleTopY = bot.y + (bot.isCompact ? -42.0 : -72.0);

        // ĐẶC QUYỀN SANJI >= 3 TIM: Mỗi ngày 1 lần tặng miễn phí 1 món ăn cao cấp!
        if ("sanji".equals(bot.id) && h >= 3.0 && player != null && bot.lastTalkRewardDay < currentDay) {
            bot.lastTalkRewardDay = currentDay;
            CookedFoodItem feast = CookedFoodItem.createById(CookedFoodItem.ID_FARM_HOTPOT);
            player.getInventory().addItem(feast, 1);
            SoundHook.playPickup();
            if (particles != null) particles.spawnHeartBurst(bx, bot.y + 8.0);
            String msg = "[" + heartStr + "]\nSanji: Quý khách thân thiết! Tặng cậu 1 phần Lẩu Nông Trại Đặc Biệt nóng hổi!";
            showBubble(bot.bubble, msg, bx, bubbleTopY);
            bot.bubbleVisibleTimer = 5.5;
            if (floatingTextCb != null) floatingTextCb.accept("+1 Lẩu Đặc Biệt (Đặc quyền Sanji 3❤)!");
            if (notifyCb != null) notifyCb.accept("🍲 Sanji (3❤) tặng miễn phí 1× Lẩu Nông Trại Đặc Biệt!");
            return;
        }

        if (player != null && bot.lastTalkRewardDay < currentDay) {
            bot.lastTalkRewardDay = currentDay;
            SoundHook.playPickup();
            if (particles != null) particles.spawnHeartBurst(bx, bot.y + 8.0);
            if (rng.nextBoolean()) {
                int cash = 5 + rng.nextInt(11);
                player.addMoney(cash);
                String msg = "[" + heartStr + "]\n" + bot.name + ": Tặng cậu $" + cash + " uống nước nhé!";
                showBubble(bot.bubble, msg, bx, bubbleTopY);
                bot.bubbleVisibleTimer = 5.0;
                if (floatingTextCb != null) floatingTextCb.accept("+$" + cash + " từ " + bot.name + "!");
                if (notifyCb != null) notifyCb.accept("🎁 " + bot.name + " (" + heartStr + ") tặng bạn +$" + cash + "!");
            } else {
                Seed giftSeed = createRandomGiftSeed();
                player.getInventory().addItem(giftSeed, 1);
                String msg = "[" + heartStr + "]\n" + bot.name + ": Tặng cậu 1 " + giftSeed.getName() + " trồng thử nè!";
                showBubble(bot.bubble, msg, bx, bubbleTopY);
                bot.bubbleVisibleTimer = 5.0;
                if (floatingTextCb != null) floatingTextCb.accept("+1 " + giftSeed.getName() + "!");
                if (notifyCb != null) notifyCb.accept("🌱 " + bot.name + " (" + heartStr + ") tặng bạn 1× " + giftSeed.getName() + "!");
            }
        } else {
            SoundHook.playMenuClick();
            String line = "[" + heartStr + "]\n" + bot.talkLines[rng.nextInt(bot.talkLines.length)];
            showBubble(bot.bubble, line, bx, bubbleTopY);
            bot.bubbleVisibleTimer = 4.8;
            if (notifyCb != null) notifyCb.accept(bot.name + " " + heartStr + " — Đặc quyền 3❤: " + getPerkDescription(bot.id));
        }
    }

    /** Tặng 1 vật phẩm từ túi người chơi cho NPC (tối đa 1 lần/ngày/NPC: Đồ nấu/Nhà máy +1 Tim, Nông sản +0.5 Tim). */
    public boolean giveGiftToNpc(NpcTarget target,
                                 Item giftItem,
                                 int currentDay,
                                 Player player,
                                 ParticleSystem particles,
                                 Runnable onPerkChanged,
                                 Consumer<String> floatingTextCb,
                                 Consumer<String> notifyCb) {
        if (target == null || giftItem == null || player == null) return false;
        if (hasGiftedToday(target.id, currentDay)) {
            if (notifyCb != null) notifyCb.accept("Hôm nay bạn đã tặng quà cho " + target.name + " rồi! Hãy quay lại vào ngày mai.");
            return false;
        }
        if (!player.getInventory().removeItem(giftItem, 1)) {
            return false;
        }

        boolean highTier = (giftItem instanceof CookedFoodItem) || (giftItem instanceof FactoryProductItem);
        double gain = highTier ? 1.0 : 0.5;
        double before = getHearts(target.id);
        double after = Math.min(5.0, before + gain);
        heartsMap.put(target.id.toLowerCase(), after);
        lastGiftedByPlayerDay.put(target.id.toLowerCase(), currentDay);
        refreshAllNameTags();

        SoundHook.playHeart();
        double cx = target.x + 22.0;
        if (particles != null) {
            particles.spawnHeartBurst(cx, target.y + 4.0);
        }

        String thankLine = buildThankYouLine(target.id, target.name, giftItem.getName(), highTier)
                + "\n[" + formatHeartBar(after) + "]";
        if (target.poolLady != null) {
            showBubble(target.poolLady.bubble, thankLine, cx, target.y - 74.0);
            target.poolLady.bubbleHideTimer = 5.5;
        } else if (target.botActor != null) {
            double topY = target.y + (target.botActor.isCompact ? -44.0 : -74.0);
            showBubble(target.botActor.bubble, thankLine, cx, topY);
            target.botActor.bubbleVisibleTimer = 5.5;
        }

        if (floatingTextCb != null) {
            floatingTextCb.accept("+" + (highTier ? "1" : "0.5") + " ❤ " + target.name + " (" + formatHeartBar(after) + ")");
        }

        boolean newlyUnlocked = (before < 3.0 && after >= 3.0);
        if (onPerkChanged != null) {
            onPerkChanged.run();
        }
        if (notifyCb != null) {
            if (newlyUnlocked) {
                notifyCb.accept("💖 ĐẠT 3 TIM VỚI " + target.name.toUpperCase() + "! Mở khóa đặc quyền: " + getPerkDescription(target.id) + "!");
            } else {
                notifyCb.accept("🎁 Đã tặng " + giftItem.getName() + " cho " + target.name + " (+" + (highTier ? "1" : "0.5") + " Tim ➡ " + formatHeartBar(after) + ")!");
            }
        }
        return true;
    }

    private String buildThankYouLine(String id, String name, String itemName, boolean highTier) {
        switch (id.toLowerCase()) {
            case "nami":    return name + ": Á à, " + itemName + " xịn quá! Cảm ơn cậu nhiều nha Luffy~ ♥";
            case "hancock": return name + ": Ôi... Luffy tặng quà cho thiếp ư?! Thiếp hạnh phúc ngất mất!! ♥♥";
            case "zoro":    return name + ": Hừm, " + itemName + " à? Cũng biết điều đấy Luffy!";
            case "sanji":   return name + ": Hương vị của " + itemName + (highTier ? " chế biến quá chuẩn!" : " tươi ngon đấy!");
            case "chopper": return name + ": Kyaa~ cảm ơn cậu nha!! Nhưng tớ không sướng rơn đâu đồ ngốc~ ♥";
            default:        return name + ": Tuyệt cú mèo!! " + itemName + " đúng món Thuyền trưởng Usopp thích!";
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

    public void save(Properties p) {
        if (p == null) return;
        for (String id : ALL_NPC_IDS) {
            p.setProperty("npc." + id + ".hearts", Double.toString(getHearts(id)));
            p.setProperty("npc." + id + ".giftDay", Integer.toString(lastGiftedByPlayerDay.getOrDefault(id, -1)));
        }
        for (BotActor b : bots) {
            p.setProperty("npc." + b.id + ".talkRewardDay", Integer.toString(b.lastTalkRewardDay));
        }
    }

    public void load(Properties p) {
        if (p == null) return;
        for (String id : ALL_NPC_IDS) {
            double h = Double.parseDouble(p.getProperty("npc." + id + ".hearts", "0.0"));
            heartsMap.put(id, Math.max(0.0, Math.min(5.0, h)));
            lastGiftedByPlayerDay.put(id, Integer.parseInt(p.getProperty("npc." + id + ".giftDay", "-1")));
        }
        for (BotActor b : bots) {
            b.lastTalkRewardDay = Integer.parseInt(p.getProperty("npc." + b.id + ".talkRewardDay", "-1"));
        }
        refreshAllNameTags();
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
        lbl.setLayoutX(22.0 - name.length() * 3.4 - 4.0);
        lbl.setLayoutY(layoutY);
        return lbl;
    }

    private static Label createSpeechBubble(String bgHex, String borderHex, String textHex) {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.setMaxWidth(246);
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
        double bw = bubble.getWidth() > 10 ? bubble.getWidth() : Math.min(246.0, Math.max(125.0, bubble.getText().length() * 6.2));
        bubble.setLayoutX(centerX - bw * 0.5);
        bubble.setLayoutY(topY);
    }
}
