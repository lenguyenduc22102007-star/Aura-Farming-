package view.river;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Module hiển thị bề mặt nước sông liền mạch (không lộ đường nối lưới ô vuông 64x64),
 * viền bờ cỏ + mô đất nâu (shoreline cliff ~14-16px) chạy xuyên suốt chiều dài bờ sông
 * nối khớp 100% với mố cầu phía Bắc (river_north_abutment), cùng sóng & bọt nước động.
 */
public class RiverFlow {

    public static final double RIVER_MIN_X = 0.0;
    public static final double RIVER_MAX_X = 2560.0; // 40 * CELL_SIZE (64)
    public static final double RIVER_TOP_Y = 2048.0; // Hàng 32 * 64
    public static final double RIVER_BOT_Y = 2368.0; // Hết hàng 36 * 64

    private static final double TEXEL_SCALE = 1.6;        // 64 world px / 40 TMX texel px
    private static final double LAYER_SORT_Y = -1020.0;   // Nền nước liền mạch + gợn sóng (dưới bóng cá -1010)
    private static final double SHORE_SORT_Y = -1009.0;   // Viền cỏ + vách đất bờ sông (trên bóng cá -1010, khớp mố cầu -1008, dưới cầu -1000 & cầu tàu -950)

    private final double maxX;
    private final double riverTopY;
    private final double riverBotY;
    private final double abutMinX;
    private final double abutMaxX;

    private final Pane layer;
    private final Pane shorelineLayer;
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
        final boolean hugNorthShore;
        final double shoreOffset;

        WaveStreak(Rectangle rect, double x, double baseY, double speed, double width,
                   double phase, double baseOpacity, boolean hugNorthShore, double shoreOffset) {
            this.rect = rect;
            this.x = x;
            this.baseY = baseY;
            this.speed = speed;
            this.width = width;
            this.phase = phase;
            this.baseOpacity = baseOpacity;
            this.hugNorthShore = hugNorthShore;
            this.shoreOffset = shoreOffset;
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
        this(RIVER_MAX_X, RIVER_TOP_Y, RIVER_BOT_Y, 960.0, 1472.0);
    }

    public RiverFlow(double mapWidth, double riverTopY, double riverBotY) {
        this(mapWidth, riverTopY, riverBotY, 960.0, 1472.0);
    }

    public RiverFlow(double mapWidth, double riverTopY, double riverBotY,
                     double northAbutmentMinX, double northAbutmentMaxX) {
        this.maxX = Math.max(400.0, mapWidth);
        this.riverTopY = riverTopY;
        this.riverBotY = riverBotY;
        this.abutMinX = northAbutmentMinX;
        this.abutMaxX = northAbutmentMaxX;

        layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.setUserData(LAYER_SORT_Y);

        shorelineLayer = new Pane();
        shorelineLayer.setManaged(false);
        shorelineLayer.setMouseTransparent(true);
        shorelineLayer.setUserData(SHORE_SORT_Y);

        Random rng = new Random(3236L);
        double riverH = Math.max(40.0, riverBotY - riverTopY);

        // 0. Bề mặt nước sông pixel-art liền mạch toàn chiều rộng (xóa triệt để đường lưới hình vuông 64x64 giữa các ô nước)
        ImageView waterSurfaceView = buildSeamlessWaterSurface(riverH);
        layer.getChildren().add(waterSurfaceView);

        // 0b. Lớp viền mép cỏ + mô đất nâu 3D (~14-16px) dọc toàn bộ bờ Bắc & viền cỏ mềm bờ Nam
        buildShorelineEdges();

        // 1. 64 vệt gợn sóng ngang pixel trôi liên tục từ trái sang phải dọc con sông
        for (int i = 0; i < 64; i++) {
            boolean bright = (i % 3 == 0);
            double w = bright ? (14 + rng.nextInt(18)) : (22 + rng.nextInt(28));
            double h = bright ? 3.0 : 3.5;
            double x = rng.nextDouble() * this.maxX;
            double y = riverTopY + 24.0 + rng.nextDouble() * Math.max(12.0, riverH - 42.0);
            double speed = 22.0 + rng.nextDouble() * 26.0; // px/giây
            double phase = rng.nextDouble() * Math.PI * 2.0;
            double baseAlpha = bright ? 0.52 : 0.32;

            Rectangle r = new Rectangle(w, h);
            r.setFill(bright ? Color.rgb(238, 252, 255) : Color.rgb(165, 236, 250));
            r.setOpacity(baseAlpha);
            r.setLayoutX(x);
            r.setLayoutY(y);
            layer.getChildren().add(r);
            streaks.add(new WaveStreak(r, x, y, speed, w, phase, baseAlpha, false, 0.0));
        }

        // 2. 36 vệt sóng/bọt trắng nhỏ vỗ nhẹ sát chân vách đất bờ Bắc (tự lượn theo đường cong mố cầu) & sát mép bờ Nam
        for (int i = 0; i < 36; i++) {
            boolean northBank = (i < 20);
            double w = 9 + rng.nextInt(13);
            double x = rng.nextDouble() * this.maxX;
            double shoreOffset = 1.5 + rng.nextDouble() * 6.5;
            double y = northBank
                ? (getNorthWaterlineWorldY(x + w * 0.5) + shoreOffset)
                : (riverBotY - 11.0 + rng.nextDouble() * 6.5);
            double speed = 12.0 + rng.nextDouble() * 11.0;
            double phase = rng.nextDouble() * Math.PI * 2.0;

            Rectangle r = new Rectangle(w, 2.8);
            r.setFill(Color.rgb(242, 253, 255));
            r.setOpacity(0.50);
            r.setLayoutX(x);
            r.setLayoutY(y);
            layer.getChildren().add(r);
            streaks.add(new WaveStreak(r, x, y, speed, w, phase, 0.50, northBank, shoreOffset));
        }

        // 3. 18 bong bóng nước tròn nhỏ li ti nổi lấp lánh trên mặt sông
        for (int i = 0; i < 18; i++) {
            double x = rng.nextDouble() * this.maxX;
            double y = riverTopY + 30.0 + rng.nextDouble() * Math.max(12.0, riverH - 54.0);
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

    /** Hàm băm tất định khớp bit-for-bit với script dựng river_north_abutment.png. */
    private static int pixHash(int x, int y) {
        long n = ((long) x * 374761393L + (long) y * 668265263L) & 0x7fffffffL;
        n = ((n ^ (n >> 13)) * 1274126177L) & 0x7fffffffL;
        return (int) (n % 100L);
    }

    private static int clampByte(int v) {
        return v < 0 ? 0 : (Math.min(v, 255));
    }

    /** Tính độ nhô ra của mố cầu phía Bắc tại tọa độ thế giới worldX (0.0 ngoài mố cầu). */
    private double getAbutmentBulge(double worldX) {
        if (abutMaxX <= abutMinX || worldX < abutMinX || worldX > abutMaxX) {
            return 0.0;
        }
        double lx = ((worldX - abutMinX) / (abutMaxX - abutMinX)) * 320.0;
        double dL = Math.abs(lx - 86.0) / 58.0;
        double lobeL = (dL < 1.0) ? Math.pow(Math.cos(dL * Math.PI * 0.5), 1.35) : 0.0;
        double dR = Math.abs(lx - 234.0) / 58.0;
        double lobeR = (dR < 1.0) ? Math.pow(Math.cos(dR * Math.PI * 0.5), 1.35) : 0.0;
        double dC = Math.abs(lx - 160.0) / 64.0;
        double bridgeSpanFill = (dC < 1.0) ? 0.42 * Math.cos(dC * Math.PI * 0.5) : 0.0;
        return Math.max(Math.max(lobeL, lobeR), bridgeSpanFill);
    }

    /** Tọa độ Y thế giới nơi chân vách đất bờ Bắc chạm mặt nước tại worldX. */
    private double getNorthWaterlineWorldY(double worldX) {
        double bulge = getAbutmentBulge(worldX);
        double cliffTexels = 8.4 + bulge * 1.6;
        double grassOffsetTexels = -0.6 + bulge * 21.0;
        return riverTopY + (grassOffsetTexels + cliffTexels) * TEXEL_SCALE;
    }

    /**
     * Dựng bề mặt nước pixel-art liền mạch toàn chiều ngang sông (tỉ lệ 1 texel = 1.6px thế giới),
     * loại bỏ hoàn toàn hiện tượng lộ đường nối hình vuông giữa các ô tile nước gid=11/12.
     */
    private ImageView buildSeamlessWaterSurface(double riverH) {
        int texW = Math.max(16, (int) Math.round(this.maxX / TEXEL_SCALE));
        int texH = Math.max(16, (int) Math.round(riverH / TEXEL_SCALE));
        WritableImage waterImg = new WritableImage(texW, texH);
        PixelWriter pw = waterImg.getPixelWriter();

        for (int ty = 0; ty < texH; ty++) {
            double vFrac = ty / (double) Math.max(1, texH - 1);
            for (int tx = 0; tx < texW; tx++) {
                double wx = tx * TEXEL_SCALE;
                double northBulge = getAbutmentBulge(wx);
                double northCliffBottomTy = 8.0 + northBulge * 22.6;

                // Bóng nước sâu mát tự nhiên ngay dưới chân vách đất bờ Bắc & mố cầu, chuyển mượt ra giữa dòng
                double distFromNorthCliff = ty - northCliffBottomTy;
                double northDepth = (distFromNorthCliff < 18.0)
                        ? Math.max(0.0, Math.min(1.0, (18.0 - Math.max(0.0, distFromNorthCliff)) / 18.0))
                        : 0.0;
                double southShallow = (ty > texH - 14)
                        ? Math.max(0.0, Math.min(1.0, (ty - (texH - 14.0)) / 14.0))
                        : 0.0;

                // Dải dòng chảy ngang hữu cơ liên tục (không lặp chu kỳ ô vuông 40x40)
                double waveBand = Math.sin(tx * 0.048 + Math.sin(ty * 0.14) * 1.6)
                        + 0.65 * Math.cos(tx * 0.023 - ty * 0.19)
                        + 0.40 * Math.sin(tx * 0.095 + ty * 0.31);
                int grain = (pixHash(tx / 2, ty / 2) % 11) - 5;

                int r = 41;
                int g = 171;
                int b = 236;

                if (waveBand > 0.92 && distFromNorthCliff > 4.0) {
                    r += 7;
                    g += 8;
                    b += 5;
                } else if (waveBand < -0.88) {
                    r -= 4;
                    g -= 7;
                    b -= 4;
                }

                // Lượng tử hóa nhẹ tạo chất liệu pixel-art mịn màng không gãy lưới
                if (grain >= 4 && (ty % 2 == 0)) {
                    r += 2;
                    g += 3;
                    b += 2;
                } else if (grain <= -4 && (ty % 2 == 1)) {
                    r -= 2;
                    g -= 3;
                    b -= 2;
                }

                // Áp bóng sâu sát chân bờ Bắc & độ nông nhẹ ven bờ Nam
                r -= (int) Math.round(northDepth * 10.0 + southShallow * 4.0);
                g -= (int) Math.round(northDepth * 22.0 + southShallow * 10.0);
                b -= (int) Math.round(northDepth * 18.0 + southShallow * 6.0);

                int argb = (255 << 24) | (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
                pw.setArgb(tx, ty, argb);
            }
        }

        ImageView iv = new ImageView(waterImg);
        iv.setLayoutX(0.0);
        iv.setLayoutY(riverTopY);
        iv.setFitWidth(this.maxX);
        iv.setFitHeight(riverH);
        iv.setSmooth(false);
        iv.setManaged(false);
        iv.setMouseTransparent(true);
        return iv;
    }

    /**
     * Dựng dải viền bờ Bắc (mép cỏ rủ + vách đất nâu cao ~13-16px + bóng nước + bọt trắng)
     * nối liền mạch 100% với hai đầu river_north_abutment.png, kèm viền mép cỏ tự nhiên phía bờ Nam.
     */
    private void buildShorelineEdges() {
        int texW = Math.max(16, (int) Math.round(this.maxX / TEXEL_SCALE));

        // Bảng màu khớp chính xác 100% từng giá trị RGB với river_north_abutment.png & terrain_atlas
        final int cGrassMain  = (255 << 24) | (156 << 16) | (197 << 8) | 84;
        final int cGrassLit   = (255 << 24) | (170 << 16) | (208 << 8) | 96;
        final int cGrassMid   = (255 << 24) | (128 << 16) | (172 << 8) | 66;
        final int cGrassLip1  = (255 << 24) | (96  << 16) | (138 << 8) | 48;
        final int cGrassLip2  = (255 << 24) | (62  << 16) | (94  << 8) | 34;

        final int cCliffHi    = (255 << 24) | (148 << 16) | (106 << 8) | 70;
        final int cCliffMid   = (255 << 24) | (122 << 16) | (84  << 8) | 54;
        final int cCliffShad  = (255 << 24) | (94  << 16) | (62  << 8) | 42;
        final int cCliffBase  = (255 << 24) | (64  << 16) | (42  << 8) | 32;

        final int cFoam1      = (155 << 24) | (168 << 16) | (234 << 8) | 252;
        final int cFoam2      = (85  << 24) | (104 << 16) | (198 << 8) | 244;
        final int cWaterShad1 = (70  << 24) | (18  << 16) | (88  << 8) | 142;
        final int cWaterShad2 = (34  << 24) | (18  << 16) | (88  << 8) | 142;

        // 1. Dải viền bờ Bắc: cao 24 texels (tương ứng tmxY = 1275..1298, đặt tại riverTopY - 5 * 1.6)
        final int northTexH = 24;
        final int baseTmxY = (int) Math.round(riverTopY / TEXEL_SCALE) - 5; // 1280 - 5 = 1275
        final int abutTmxMin = (abutMaxX > abutMinX) ? (int) Math.round(abutMinX / TEXEL_SCALE) : -9999;
        final int abutTmxMax = (abutMaxX > abutMinX) ? (int) Math.round(abutMaxX / TEXEL_SCALE) : -9999;

        WritableImage northImg = new WritableImage(texW, northTexH);
        PixelWriter npw = northImg.getPixelWriter();

        for (int tx = 0; tx < texW; tx++) {
            boolean insideAbutment = (tx >= abutTmxMin && tx < abutTmxMax);
            // Chồng lấp đúng 2 texel đặc (opaque) ở sát mép nối mố cầu để chống khe hở subpixel khi camera di chuyển
            boolean seamGuardOpaqueOnly = insideAbutment && (tx < abutTmxMin + 2 || tx >= abutTmxMax - 2);
            if (insideAbutment && !seamGuardOpaqueOnly) {
                continue;
            }

            int seg3 = tx / 3;
            int seg5 = tx / 5;
            int wave = pixHash(tx / 6, 31);
            int tooth = ((seg3 % 3 == 0) && wave < 65) ? 1 : (((seg5 % 4 == 1) && wave > 30) ? -1 : 0);

            // Trong river_north_abutment.png (top = 1234), grassBotY = 45 + tooth -> worldTmxY = 1279 + tooth -> sy = 4 + tooth
            int grassBotSy = 4 + tooth;
            int cliffH = 8 + ((pixHash(tx / 7, 53) < 30) ? 1 : 0);

            for (int sy = 0; sy <= grassBotSy; sy++) {
                int worldTmxY = baseTmxY + sy;
                if (sy >= grassBotSy - 1) {
                    npw.setArgb(tx, sy, cGrassLip2);
                } else if (sy == grassBotSy - 2) {
                    npw.setArgb(tx, sy, cGrassLip1);
                } else if (sy >= 4) {
                    int ph = pixHash(tx / 2, worldTmxY / 2);
                    int gCol = (ph < 8) ? cGrassLit : ((ph < 15) ? cGrassMid : cGrassMain);
                    npw.setArgb(tx, sy, gCol);
                }
            }

            for (int dy = 1; dy <= cliffH; dy++) {
                int cy = grassBotSy + dy;
                if (cy >= northTexH) break;
                int worldCy = baseTmxY + cy;
                int stripe = pixHash(tx / 2, 91);
                int detail = pixHash(tx, worldCy);
                int col;
                if (dy == 1) {
                    col = (stripe < 35) ? cCliffMid : cCliffHi;
                } else if (dy == cliffH) {
                    col = cCliffBase;
                } else if (dy >= cliffH - 1) {
                    col = cCliffShad;
                } else {
                    col = (stripe < 28) ? cCliffShad
                        : ((stripe > 76) ? cCliffHi : ((detail < 18) ? cCliffShad : cCliffMid));
                }
                npw.setArgb(tx, cy, col);
            }

            if (!seamGuardOpaqueOnly) {
                int fy1 = grassBotSy + cliffH + 1;
                int fy2 = grassBotSy + cliffH + 2;
                if (fy1 < northTexH) {
                    npw.setArgb(tx, fy1, (pixHash(tx / 3, 7) < 78) ? cFoam1 : cWaterShad1);
                }
                if (fy2 < northTexH) {
                    npw.setArgb(tx, fy2, (pixHash(tx / 4, 13) < 45) ? cFoam2 : cWaterShad2);
                }
            }
        }

        ImageView northView = new ImageView(northImg);
        northView.setLayoutX(0.0);
        northView.setLayoutY(riverTopY - 5.0 * TEXEL_SCALE);
        northView.setFitWidth(this.maxX);
        northView.setFitHeight(northTexH * TEXEL_SCALE);
        northView.setSmooth(false);
        northView.setManaged(false);
        northView.setMouseTransparent(true);
        shorelineLayer.getChildren().add(northView);

        // 2. Viền mép cỏ & bọt nước mềm dọc bờ Nam (ngoài đường dẫn đầu cầu Nam tx = 638..882)
        final int southTexH = 6;
        WritableImage southImg = new WritableImage(texW, southTexH);
        PixelWriter spw = southImg.getPixelWriter();
        int southBridgeRoadMinTx = (abutMaxX > abutMinX) ? (abutTmxMin + 38) : -9999;
        int southBridgeRoadMaxTx = (abutMaxX > abutMinX) ? (abutTmxMax - 38) : -9999;

        for (int tx = 0; tx < texW; tx++) {
            if (tx >= southBridgeRoadMinTx && tx < southBridgeRoadMaxTx) {
                continue;
            }
            int waveS = pixHash(tx / 5, 73);
            int lipTop = ((tx / 4) % 3 == 0 && waveS < 60) ? 3 : 4; // nhô nhẹ 1-2 texel lên mép nước bờ Nam
            if (lipTop - 2 >= 0 && pixHash(tx / 3, 19) < 62) {
                spw.setArgb(tx, lipTop - 2, cFoam2);
            }
            if (lipTop - 1 >= 0 && pixHash(tx / 3, 29) < 80) {
                spw.setArgb(tx, lipTop - 1, cFoam1);
            }
            spw.setArgb(tx, lipTop, cGrassLip1);
            for (int sy = lipTop + 1; sy < southTexH; sy++) {
                int ph = pixHash(tx / 2, 500 + sy);
                spw.setArgb(tx, sy, (ph < 20) ? cGrassMid : cGrassMain);
            }
        }

        ImageView southView = new ImageView(southImg);
        southView.setLayoutX(0.0);
        southView.setLayoutY(riverBotY - 5.0 * TEXEL_SCALE);
        southView.setFitWidth(this.maxX);
        southView.setFitHeight(southTexH * TEXEL_SCALE);
        southView.setSmooth(false);
        southView.setManaged(false);
        southView.setMouseTransparent(true);
        shorelineLayer.getChildren().add(southView);
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
        if (entityPane == null) return;
        if (!entityPane.getChildren().contains(layer)) {
            entityPane.getChildren().add(layer);
        }
        if (!entityPane.getChildren().contains(shorelineLayer)) {
            entityPane.getChildren().add(shorelineLayer);
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
            double targetY = ws.hugNorthShore
                    ? (getNorthWaterlineWorldY(ws.x + ws.width * 0.5) + ws.shoreOffset)
                    : ws.baseY;
            ws.rect.setLayoutX(Math.round(ws.x));
            ws.rect.setLayoutY(Math.round(targetY + Math.sin(timeSeconds * 1.8 + ws.phase) * 1.3));
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
