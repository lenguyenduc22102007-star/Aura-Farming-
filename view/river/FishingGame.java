package view.river;

import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Rectangle;
import model.game_item.FishSpecies;
import model.weather.WeatherType;
import view.assets.Assets;
import view.fx.SoundHook;

import java.util.Random;

/**
 * Hệ thống Câu Cá chuẩn Play Together (3 Giai Đoạn Căng Não):
 * - Giai đoạn 0 (APPROACHING): Bóng cá gần nhất (Size 1..5) quay đầu bơi lại đối diện phao.
 *   Người chơi có thể bấm Space sớm để thu phao chọn bóng khác.
 * - Giai đoạn 1 (NIBBLING - Nhấp mồi giả): Cá nhấp nhẹ 1-4 lần (cách nhau 1.0-2.2s), phao chỉ rung nhẹ
 *   + vòng sóng nhỏ (KHÔNG có "!"). Bấm Space lúc này -> "Giật sớm quá! Cá chạy mất!".
 * - Giai đoạn 2 (REAL_BITE - Cắn thật "!"): Phao chìm hẳn + bắn nước + dấu "!" đỏ to viền sáng.
 *   Cửa sổ phản xạ: Size 1-2 (0.9s), Size 3 (0.65s), Size 4 (0.50s), Size 5 (0.38s).
 *   Bấm trễ -> "Quá chậm! Cá đã thoát!".
 * - Giai đoạn 3 (QUICK_REEL - Kéo cá 2.5s cho Size 3, 4, 5): Spam phím Space đầy 100% trước khi hết 2.5s.
 *   Không kịp -> "Đứt dây câu! Cá quá khỏe!".
 */
public class FishingGame {

    public enum State {
        IDLE,
        CASTING,
        APPROACHING,
        NIBBLING,
        REAL_BITE,
        QUICK_REEL
    }

    public interface FishingCallbacks {
        void onFishCaught(FishSpecies species, double sizeCm, boolean isCrown, int rewardMoney, int shadowSize);
        void onFishEscaped(String message);
        void onWaterSplash(double worldX, double worldY);
        WeatherType getCurrentWeather();
        int getCurrentHour();
    }

    private static final double LAYER_SORT_Y = 9985.0;
    private static final double BAR_W = 208.0;
    private static final double BAR_H = 18.0;
    private static final double QUICK_REEL_LIMIT_SEC = 2.50;

    private final BoatEntity boat;
    private final FishShadows fishShadows;
    private final RiverFlow riverFlow;
    private final FishingCallbacks callbacks;
    private final double riverTopY;
    private final double riverBotY;
    private final double mapWidth;
    private final Random rng = new Random();

    private final Group rootLayer;

    // Cần câu, dây câu, phao & gợn sóng quanh phao
    private final Group rodGroup;
    private final Line rodMain;
    private final Line rodTipAccent;
    private final QuadCurve fishingLine;
    private final Group bobberGroup;
    private final Circle bobberRipple1;
    private final Circle bobberRipple2;

    // Dấu "!" ĐỎ TO kèm viền sáng khi cá cắn thật (Giai đoạn 2)
    private final Group bigExclamationGroup;

    // Thanh Kéo Cần nhanh 2.5 giây cho Size 3, 4, 5 (Giai đoạn 3)
    private final Group quickReelGroup;
    private final Label reelTitleLabel;
    private final Label reelTimerLabel;
    private final Rectangle reelFillRect;
    private final StackPane spaceKeycap;

    // Hiệu ứng cá nhảy vòng cung khỏi sông
    private final ImageView leapingFishView;
    private boolean fishLeaping = false;
    private double leapElapsed = 0.0;
    private static final double LEAP_DURATION = 0.65;
    private double leapStartX, leapStartY, leapEndX, leapEndY;

    // Chữ nổi kết quả ngắn tại vị trí thuyền
    private final Label floatingLabel;
    private boolean floatingActive = false;
    private double floatingElapsed = 0.0;
    private static final double FLOATING_DURATION = 1.45;
    private double floatingBaseX, floatingBaseY;

    // Trạng thái logic 3 giai đoạn
    private State state = State.IDLE;
    private double stateElapsed = 0.0;
    private double castBobberX = 820.0;
    private double castBobberY = 2240.0;
    private double curBobberX = 820.0;
    private double curBobberY = 2240.0;

    // Giai đoạn 1: Nhấp mồi giả
    private int currentShadowSize = 2;
    private int fakeNibblesRemaining = 0;
    private double nextNibbleTimer = 0.0;
    private double nibbleBobberAnim = 0.0; // > 0 khi phao đang rung nhẹ sau 1 cú nhấp giả
    private boolean usoppRareBoost = false;

    public void setUsoppRareBoost(boolean enabled) {
        this.usoppRareBoost = enabled;
    }

    // Giai đoạn 2: Cửa sổ phản xạ cắn thật
    private double reactionWindowSec = 0.90;

    // Giai đoạn 3: Quick Reel 2.5 giây (Size 3, 4, 5)
    private double reelProgress = 0.0;
    private double reelTimeLeft = QUICK_REEL_LIMIT_SEC;
    private double tapBoost = 0.12;
    private double reelDrain = 0.10;
    private double tapKickAnim = 0.0;

    public FishingGame(BoatEntity boat, FishShadows fishShadows, RiverFlow riverFlow,
                       FishingCallbacks callbacks, double riverTopY, double riverBotY, double mapWidth) {
        this.boat = boat;
        this.fishShadows = fishShadows;
        this.riverFlow = riverFlow;
        this.callbacks = callbacks;
        this.riverTopY = riverTopY;
        this.riverBotY = riverBotY;
        this.mapWidth = Math.max(800.0, mapWidth);

        rootLayer = new Group();
        rootLayer.setManaged(false);
        rootLayer.setMouseTransparent(true);
        rootLayer.setUserData(LAYER_SORT_Y);

        // 1. Cần câu gỗ pixel
        rodMain = new Line();
        rodMain.setStroke(Color.rgb(118, 70, 36));
        rodMain.setStrokeWidth(3.5);
        rodTipAccent = new Line();
        rodTipAccent.setStroke(Color.rgb(186, 134, 78));
        rodTipAccent.setStrokeWidth(2.0);
        rodGroup = new Group(rodMain, rodTipAccent);
        rodGroup.setVisible(false);

        // 2. Dây câu
        fishingLine = new QuadCurve();
        fishingLine.setFill(Color.TRANSPARENT);
        fishingLine.setStroke(Color.rgb(245, 250, 255, 0.92));
        fishingLine.setStrokeWidth(1.6);
        fishingLine.setVisible(false);

        // 3. Phao đỏ trắng + 2 vòng gợn sóng
        bobberRipple1 = new Circle(0, 3, 8);
        bobberRipple1.setFill(Color.TRANSPARENT);
        bobberRipple1.setStroke(Color.rgb(235, 252, 255, 0.80));
        bobberRipple1.setStrokeWidth(1.8);

        bobberRipple2 = new Circle(0, 3, 13);
        bobberRipple2.setFill(Color.TRANSPARENT);
        bobberRipple2.setStroke(Color.rgb(175, 236, 250, 0.55));
        bobberRipple2.setStrokeWidth(1.4);

        Circle bobberOutline = new Circle(0, 0, 6.2, Color.rgb(34, 24, 24));
        Arc topRed = new Arc(0, 0, 5.0, 5.0, 0, 180);
        topRed.setType(ArcType.CHORD);
        topRed.setFill(Color.rgb(228, 52, 46));
        Arc botWhite = new Arc(0, 0, 5.0, 5.0, 180, 180);
        botWhite.setType(ArcType.CHORD);
        botWhite.setFill(Color.rgb(248, 250, 255));
        Rectangle bobberPin = new Rectangle(-1.2, -8.5, 2.4, 3.5);
        bobberPin.setFill(Color.rgb(245, 224, 160));

        bobberGroup = new Group(bobberRipple2, bobberRipple1, bobberOutline, topRed, botWhite, bobberPin);
        bobberGroup.setVisible(false);

        // 4. Dấu "!" ĐỎ TO kèm viền sáng rực (Giai đoạn 2 - Cắn thật!)
        Circle glowHalo = new Circle(0, 0, 24, Color.rgb(255, 238, 120, 0.42));
        Circle bubbleOuter = new Circle(0, 0, 19, Color.rgb(255, 252, 242));
        bubbleOuter.setStroke(Color.rgb(255, 214, 64));
        bubbleOuter.setStrokeWidth(3.2);
        bubbleOuter.setEffect(new DropShadow(10.0, Color.rgb(255, 200, 40, 0.90)));

        Rectangle excStem = new Rectangle(-3.4, -12.5, 6.8, 15.5);
        excStem.setArcWidth(2.5);
        excStem.setArcHeight(2.5);
        excStem.setFill(Color.rgb(228, 26, 26));
        excStem.setStroke(Color.rgb(92, 10, 10));
        excStem.setStrokeWidth(1.2);

        Circle excDot = new Circle(0, 8.5, 3.6, Color.rgb(228, 26, 26));
        excDot.setStroke(Color.rgb(92, 10, 10));
        excDot.setStrokeWidth(1.2);

        bigExclamationGroup = new Group(glowHalo, bubbleOuter, excStem, excDot);
        bigExclamationGroup.setVisible(false);

        // 5. Thanh Kéo Cần Nhanh 2.5 giây (Giai đoạn 3 cho Size 3, 4, 5)
        quickReelGroup = new Group();
        quickReelGroup.setVisible(false);

        reelTitleLabel = new Label("SPAM [SPACE] KÉO CÁ!");
        reelTitleLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF4B8;"
        );
        reelTitleLabel.setEffect(new DropShadow(2.0, 1.0, 1.4, Color.rgb(24, 12, 8, 0.95)));
        reelTitleLabel.setLayoutX(4);
        reelTitleLabel.setLayoutY(-22);

        reelTimerLabel = new Label("2.5s");
        reelTimerLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FF947A;"
        );
        reelTimerLabel.setEffect(new DropShadow(2.0, 1.0, 1.4, Color.rgb(24, 12, 8, 0.95)));
        reelTimerLabel.setLayoutX(BAR_W - 34);
        reelTimerLabel.setLayoutY(-22);

        Rectangle outerWood = new Rectangle(-5, -4, BAR_W + 10, BAR_H + 8);
        outerWood.setArcWidth(8);
        outerWood.setArcHeight(8);
        outerWood.setFill(Color.rgb(78, 44, 22));
        outerWood.setStroke(Color.rgb(30, 16, 8));
        outerWood.setStrokeWidth(2.4);

        Rectangle trackBg = new Rectangle(0, 0, BAR_W, BAR_H);
        trackBg.setArcWidth(5);
        trackBg.setArcHeight(5);
        trackBg.setFill(Color.rgb(32, 22, 16));

        reelFillRect = new Rectangle(1, 1, 20, BAR_H - 2);
        reelFillRect.setArcWidth(4);
        reelFillRect.setArcHeight(4);
        reelFillRect.setFill(Color.rgb(74, 226, 98));

        Rectangle gloss = new Rectangle(2, 2, BAR_W - 4, 4);
        gloss.setFill(Color.rgb(255, 255, 255, 0.24));

        Label keyLbl = new Label("SPAM SPACE!");
        keyLbl.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #3B200C;"
        );
        spaceKeycap = new StackPane(keyLbl);
        spaceKeycap.setAlignment(Pos.CENTER);
        spaceKeycap.setPrefSize(94, 20);
        spaceKeycap.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #FFF5D2 0%, #E7C374 100%);" +
            "-fx-border-color: #4A2810;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 5px;" +
            "-fx-border-radius: 5px;"
        );
        spaceKeycap.setLayoutX((BAR_W - 94) * 0.5);
        spaceKeycap.setLayoutY(BAR_H + 7);

        quickReelGroup.getChildren().addAll(
            outerWood, trackBg, reelFillRect, gloss, reelTitleLabel, reelTimerLabel, spaceKeycap
        );

        // 6. Cá nhảy khỏi mặt nước
        leapingFishView = new ImageView(Assets.FISH_ICON);
        leapingFishView.setFitWidth(44);
        leapingFishView.setFitHeight(34);
        leapingFishView.setSmooth(false);
        leapingFishView.setCache(true);
        leapingFishView.setCacheHint(CacheHint.SPEED);
        leapingFishView.setVisible(false);

        // 7. Chữ nổi nhanh (báo lỗi giật sớm / quá chậm / đứt dây / thu cần)
        floatingLabel = new Label();
        floatingLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE869;"
        );
        floatingLabel.setEffect(new DropShadow(2.5, 1.0, 1.5, Color.rgb(20, 12, 8, 0.95)));
        floatingLabel.setCache(true);
        floatingLabel.setCacheHint(CacheHint.SPEED);
        floatingLabel.setVisible(false);

        rootLayer.getChildren().addAll(
            bobberGroup, fishingLine, rodGroup,
            leapingFishView, bigExclamationGroup, quickReelGroup, floatingLabel
        );
    }

    public void attachTo(Pane entityPane) {
        if (entityPane != null && !entityPane.getChildren().contains(rootLayer)) {
            entityPane.getChildren().add(rootLayer);
        }
    }

    public Node getNode() {
        return rootLayer;
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public State getState() {
        return state;
    }

    public double getLastBobberX() { return curBobberX; }
    public double getLastBobberY() { return curBobberY; }

    /**
     * Xử lý MỖI LẦN người chơi nhấn mới phím Space / F (hoặc J) khi đang trên thuyền.
     */
    public void onActionPressed() {
        switch (state) {
            case IDLE:
                startCast();
                break;
            case CASTING:
                break;
            case APPROACHING:
                // Bóng cá mới đang bơi lại -> cho phép thu cần sớm không bị phạt để chọn bóng khác!
                stopFishingVisuals();
                if (fishShadows != null) fishShadows.releaseAttraction();
                showFloatingText("Thu cần — hãy thả lại chỗ khác!", Color.rgb(220, 238, 255));
                state = State.IDLE;
                break;
            case NIBBLING:
                // Luật phạt Giai đoạn 1: bấm lúc cá mới nhấp mồi giả -> cá chạy mất!
                stopFishingVisuals();
                if (fishShadows != null) fishShadows.vanishAttractedFish();
                showFloatingText("Giật sớm quá! Cá chạy mất!", Color.rgb(255, 118, 102));
                if (callbacks != null) callbacks.onFishEscaped("Giật sớm quá! Cá chạy mất!");
                state = State.IDLE;
                break;
            case REAL_BITE:
                // Phản xạ trúng dấu "!" trong cửa sổ thời gian!
                handleRealBiteHooked();
                break;
            case QUICK_REEL:
                // Giai đoạn 3: Spam Space để làm đầy thanh kéo trong 2.5s!
                handleQuickReelTap();
                break;
        }
    }

    public void cancel() {
        if (state != State.IDLE) {
            stopFishingVisuals();
            if (fishShadows != null) fishShadows.releaseAttraction();
            state = State.IDLE;
        }
    }

    private void startCast() {
        state = State.CASTING;
        stateElapsed = 0.0;
        nibbleBobberAnim = 0.0;

        double boatCenter = boat.getBoatX() + BoatEntity.BOAT_WIDTH * 0.5;
        double offsetDir = boat.isBoatFacingLeft() ? -1.0 : 1.0;
        double minX = Math.max(80.0, boatCenter + offsetDir * 35.0 - 95.0);
        double maxX = Math.min(mapWidth - 80.0, boatCenter + offsetDir * 35.0 + 95.0);
        if (maxX < minX + 30.0) maxX = minX + 30.0;
        double minY = Math.max(riverTopY + 125.0, boat.getBoatY() + BoatEntity.BOAT_HEIGHT + 24.0);
        double maxY = Math.max(minY + 20.0, riverBotY - 28.0);

        castBobberX = minX + rng.nextDouble() * (maxX - minX);
        castBobberY = minY + rng.nextDouble() * (maxY - minY);

        curBobberX = boat.getRodOriginX() + offsetDir * 34.0;
        curBobberY = boat.getRodOriginY() - 22.0;

        rodGroup.setVisible(true);
        fishingLine.setVisible(true);
        bobberGroup.setVisible(true);
        bigExclamationGroup.setVisible(false);
        quickReelGroup.setVisible(false);

        boolean inBubble = (riverFlow != null && riverFlow.isInBubbleZone(castBobberX, castBobberY));
        currentShadowSize = (fishShadows != null)
                ? fishShadows.attractNearestFish(castBobberX, castBobberY, inBubble)
                : 2;
    }

    private void beginNibblingPhase() {
        state = State.NIBBLING;
        stateElapsed = 0.0;
        if (fishShadows != null) {
            currentShadowSize = fishShadows.getAttractedFishSize();
        }
        // Nhấp mồi giả từ 1 đến 4 lần ngẫu nhiên (cách nhau 1.0 - 2.2 giây)
        fakeNibblesRemaining = 1 + rng.nextInt(4);
        nextNibbleTimer = 0.65 + rng.nextDouble() * 0.55;
    }

    private void triggerRealBite() {
        state = State.REAL_BITE;
        stateElapsed = 0.0;

        // Cửa sổ phản xạ chuẩn theo cấp kích thước bóng cá
        switch (currentShadowSize) {
            case 1:
            case 2:  reactionWindowSec = 0.90; break;
            case 3:  reactionWindowSec = 0.65; break;
            case 4:  reactionWindowSec = 0.50; break;
            default: reactionWindowSec = 0.38; break; // Size 5 khổng lồ
        }

        bigExclamationGroup.setVisible(true);
        SoundHook.playBiteAlert();
        if (callbacks != null) {
            callbacks.onWaterSplash(curBobberX, curBobberY);
        }
    }

    private void handleRealBiteHooked() {
        bigExclamationGroup.setVisible(false);
        tapKickAnim = 1.0;

        if (currentShadowSize <= 2) {
            // Size 1-2: bắt gọn ngay khi phản xạ đúng dấu "!"
            finishCatchSuccess();
            return;
        }

        // Size 3, 4, 5 -> bước vào Giai đoạn 3: Quick Reel 2.5 giây!
        state = State.QUICK_REEL;
        stateElapsed = 0.0;
        reelTimeLeft = QUICK_REEL_LIMIT_SEC;
        if (currentShadowSize == 3) {
            reelProgress = 0.18;
            tapBoost = 0.125;
            reelDrain = 0.08;
        } else if (currentShadowSize == 4) {
            reelProgress = 0.12;
            tapBoost = 0.105;
            reelDrain = 0.17;
        } else {
            // Size 5 khổng lồ: phải spam thật nhanh!
            reelProgress = 0.08;
            tapBoost = 0.092;
            reelDrain = 0.24;
        }

        reelTitleLabel.setText("SIZE " + currentShadowSize + " — SPAM [SPACE]!");
        quickReelGroup.setVisible(true);
        SoundHook.playReelTap(reelProgress);
    }

    private void handleQuickReelTap() {
        reelProgress = Math.min(1.0, reelProgress + tapBoost);
        tapKickAnim = 1.0;
        SoundHook.playReelTap(reelProgress);
        if (reelProgress >= 1.0) {
            finishCatchSuccess();
        }
    }

    private void finishCatchSuccess() {
        stopFishingVisuals();
        state = State.IDLE;
        if (fishShadows != null) fishShadows.vanishAttractedFish();

        WeatherType w = (callbacks != null) ? callbacks.getCurrentWeather() : WeatherType.SUNNY;
        int hr = (callbacks != null) ? callbacks.getCurrentHour() : 8;
        int effectiveShadow = (usoppRareBoost && currentShadowSize < 5 && rng.nextDouble() < 0.20)
                ? (currentShadowSize + 1)
                : currentShadowSize;
        FishSpecies species = FishSpecies.rollForShadowSize(effectiveShadow, w, hr, rng);
        double sizeCm = species.rollSizeCm(rng);
        boolean isCrown = species.isCrownSize(sizeCm) || (usoppRareBoost && rng.nextDouble() < 0.20);
        int rewardMoney = species.computeReward(isCrown);

        // Hiệu ứng cá nhảy vòng cung từ sông vào tay nhân vật
        leapStartX = curBobberX;
        leapStartY = curBobberY;
        leapEndX = boat.getBoardedPlayerX() + 24.0;
        leapEndY = boat.getSyncedPlayerY() + 16.0;
        leapElapsed = 0.0;
        fishLeaping = true;
        leapingFishView.setTranslateX(leapStartX - 22.0);
        leapingFishView.setTranslateY(leapStartY - 17.0);
        leapingFishView.setScaleX(0.85);
        leapingFishView.setScaleY(0.85);
        leapingFishView.setRotate(-35.0);
        leapingFishView.setOpacity(1.0);
        leapingFishView.setVisible(true);

        if (callbacks != null) {
            callbacks.onFishCaught(species, sizeCm, isCrown, rewardMoney, currentShadowSize);
        }
    }

    private void stopFishingVisuals() {
        rodGroup.setVisible(false);
        fishingLine.setVisible(false);
        bobberGroup.setVisible(false);
        bigExclamationGroup.setVisible(false);
        quickReelGroup.setVisible(false);
    }

    private void showFloatingText(String text, Color color) {
        floatingLabel.setText(text);
        floatingLabel.setTextFill(color);
        floatingBaseX = boat.getBoatX() + 4.0;
        floatingBaseY = boat.getBoatY() - 32.0;
        floatingElapsed = 0.0;
        floatingActive = true;
        floatingLabel.setTranslateX(floatingBaseX);
        floatingLabel.setTranslateY(floatingBaseY);
        floatingLabel.setOpacity(1.0);
        floatingLabel.setVisible(true);
    }

    public void update(double deltaSeconds, double timeSeconds) {
        double dt = Math.min(0.05, Math.max(0.0, deltaSeconds));
        stateElapsed += dt;

        double originX = boat.getRodOriginX();
        double originY = boat.getRodOriginY();
        boolean castRight = (castBobberX >= originX);

        tapKickAnim = Math.max(0.0, tapKickAnim - dt * 7.5);
        double kickPull = tapKickAnim * 10.0;
        double tipX = originX + (castRight ? (38.0 - kickPull * 0.4) : (-38.0 + kickPull * 0.4));
        double tipY = originY - 24.0 - kickPull
                + ((state == State.REAL_BITE || state == State.QUICK_REEL) ? Math.sin(timeSeconds * 28.0) * 3.5 + 5.0 : 0.0);

        if (state != State.IDLE) {
            rodMain.setStartX(originX);
            rodMain.setStartY(originY);
            rodMain.setEndX(tipX);
            rodMain.setEndY(tipY);

            rodTipAccent.setStartX(originX + (tipX - originX) * 0.55);
            rodTipAccent.setStartY(originY + (tipY - originY) * 0.55);
            rodTipAccent.setEndX(tipX);
            rodTipAccent.setEndY(tipY);

            if (state == State.CASTING) {
                double p = Math.min(1.0, stateElapsed / 0.28);
                double eased = 1.0 - Math.pow(1.0 - p, 2.0);
                curBobberX = tipX + (castBobberX - tipX) * eased;
                double arcLift = Math.sin(p * Math.PI) * 46.0;
                curBobberY = tipY + (castBobberY - tipY) * eased - arcLift;
                bobberRipple1.setVisible(false);
                bobberRipple2.setVisible(false);
                if (p >= 1.0) {
                    state = State.APPROACHING;
                    stateElapsed = 0.0;
                }
            } else if (state == State.APPROACHING) {
                curBobberX = castBobberX;
                curBobberY = castBobberY + Math.sin(timeSeconds * 3.0) * 1.6;
                animateGentleRipples(timeSeconds, 0.70, 9.0, 0.55);

                boolean arrived = (fishShadows != null && fishShadows.isAttractedFishFacingBobber(44.0));
                if ((arrived && stateElapsed >= 0.45) || stateElapsed >= 1.85) {
                    beginNibblingPhase();
                }
            } else if (state == State.NIBBLING) {
                // Giai đoạn 1: Nhấp mồi giả 1..4 lần (KHÔNG có dấu "!")
                nextNibbleTimer -= dt;
                if (nextNibbleTimer <= 0.0) {
                    if (fakeNibblesRemaining > 0) {
                        fakeNibblesRemaining--;
                        nibbleBobberAnim = 1.0;
                        nextNibbleTimer = 1.0 + rng.nextDouble() * 1.2; // 1.0 - 2.2 giây
                        if (fishShadows != null) fishShadows.playNibbleNudge();
                        SoundHook.playNibblePlop();
                    } else {
                        triggerRealBite();
                    }
                }

                if (state == State.NIBBLING) {
                    nibbleBobberAnim = Math.max(0.0, nibbleBobberAnim - dt * 2.8);
                    double jiggleY = Math.sin(nibbleBobberAnim * Math.PI) * 4.5;
                    double jiggleX = (nibbleBobberAnim > 0.1) ? Math.sin(timeSeconds * 42.0) * 1.8 : 0.0;
                    curBobberX = castBobberX + jiggleX;
                    curBobberY = castBobberY + Math.sin(timeSeconds * 3.2) * 1.5 + jiggleY;

                    if (nibbleBobberAnim > 0.05) {
                        double p = 1.0 - nibbleBobberAnim;
                        bobberRipple1.setVisible(true);
                        bobberRipple2.setVisible(false);
                        bobberRipple1.setRadius(6.0 + p * 12.0);
                        bobberRipple1.setOpacity(0.85 * (1.0 - p));
                    } else {
                        animateGentleRipples(timeSeconds, 0.65, 8.0, 0.42);
                    }
                }
            } else if (state == State.REAL_BITE) {
                // Giai đoạn 2: Phao chìm hẳn + dấu "!" đỏ to viền sáng + cửa sổ phản xạ cực ngắn!
                curBobberX = castBobberX + Math.sin(timeSeconds * 46.0) * 2.6;
                curBobberY = castBobberY + 9.5 + Math.sin(timeSeconds * 30.0) * 2.2;
                animateGentleRipples(timeSeconds, 2.6, 20.0, 0.95);

                double bobY = boat.getBobOffsetY();
                double headCenterX = boat.getBoardedPlayerX() + 22.0;
                double headTopY = boat.getBoatY() - 26.0 + bobY;
                double pulse = 1.08 + 0.14 * Math.sin(timeSeconds * 32.0);
                bigExclamationGroup.setLayoutX(headCenterX);
                bigExclamationGroup.setLayoutY(headTopY);
                bigExclamationGroup.setScaleX(pulse);
                bigExclamationGroup.setScaleY(pulse);

                if (stateElapsed >= reactionWindowSec) {
                    stopFishingVisuals();
                    if (fishShadows != null) fishShadows.vanishAttractedFish();
                    state = State.IDLE;
                    showFloatingText("Quá chậm! Cá đã thoát!", Color.rgb(255, 126, 108));
                    if (callbacks != null) callbacks.onFishEscaped("Quá chậm! Cá đã thoát!");
                }
            } else if (state == State.QUICK_REEL) {
                // Giai đoạn 3: Kéo cá trong 2.5 giây cho Size 3, 4, 5
                reelTimeLeft = Math.max(0.0, reelTimeLeft - dt);
                reelProgress = Math.max(0.0, reelProgress - reelDrain * dt);

                if (reelTimeLeft <= 0.0) {
                    stopFishingVisuals();
                    if (fishShadows != null) fishShadows.vanishAttractedFish();
                    state = State.IDLE;
                    showFloatingText("Đứt dây câu! Cá quá khỏe!", Color.rgb(255, 104, 96));
                    if (callbacks != null) callbacks.onFishEscaped("Đứt dây câu! Cá quá khỏe!");
                } else {
                    double pullLerp = Math.max(0.0, Math.min(0.60, reelProgress * 0.60));
                    double targetBoatY = boat.getBoatY() + BoatEntity.BOAT_HEIGHT + 18.0;
                    curBobberX = originX + (castBobberX - originX) * (1.0 - pullLerp) + Math.sin(timeSeconds * 40.0) * 2.8;
                    curBobberY = targetBoatY + (castBobberY - targetBoatY) * (1.0 - pullLerp) + 6.0;
                    animateGentleRipples(timeSeconds, 2.5, 18.0, 0.92);

                    reelFillRect.setWidth(Math.max(2.0, (BAR_W - 2.0) * reelProgress));
                    reelFillRect.setFill(reelProgress < 0.45 ? Color.rgb(246, 186, 54) : Color.rgb(66, 228, 96));
                    reelTimerLabel.setText(String.format("%.1fs", reelTimeLeft));

                    double keyScale = (tapKickAnim > 0.05) ? (1.0 - tapKickAnim * 0.14) : (1.0 + Math.sin(timeSeconds * 13.0) * 0.06);
                    spaceKeycap.setScaleX(keyScale);
                    spaceKeycap.setScaleY(keyScale);

                    double bobY = boat.getBobOffsetY();
                    double headCenterX = boat.getBoardedPlayerX() + 22.0;
                    double headTopY = boat.getBoatY() - 56.0 + bobY;
                    double shakeX = Math.sin(timeSeconds * 55.0) * (reelTimeLeft < 0.9 ? 3.0 : 1.5);
                    quickReelGroup.setLayoutX(headCenterX - BAR_W * 0.5 + shakeX);
                    quickReelGroup.setLayoutY(headTopY);
                }
            }

            if (state != State.IDLE) {
                bobberGroup.setLayoutX(curBobberX);
                bobberGroup.setLayoutY(curBobberY);

                fishingLine.setStartX(tipX);
                fishingLine.setStartY(tipY);
                fishingLine.setEndX(curBobberX);
                fishingLine.setEndY(curBobberY - 4.0);
                double midX = (tipX + curBobberX) * 0.5;
                double sag = (state == State.REAL_BITE || state == State.QUICK_REEL) ? 2.5 : 18.0;
                fishingLine.setControlX(midX);
                fishingLine.setControlY((tipY + curBobberY) * 0.5 + sag);
            }
        }

        // Cá nhảy mượt 60 FPS
        if (fishLeaping) {
            leapElapsed += dt;
            double p = Math.min(1.0, leapElapsed / LEAP_DURATION);
            double easedX = p * p * (3.0 - 2.0 * p);
            double x = leapStartX + (leapEndX - leapStartX) * easedX;
            double arc = Math.sin(p * Math.PI) * 108.0;
            double y = leapStartY + (leapEndY - leapStartY) * p - arc;
            double popScale = 0.85 + 0.38 * Math.sin(p * Math.PI);
            leapingFishView.setTranslateX(x - 22.0);
            leapingFishView.setTranslateY(y - 17.0);
            leapingFishView.setScaleX(popScale);
            leapingFishView.setScaleY(popScale);
            leapingFishView.setRotate(-35.0 + p * 185.0);
            leapingFishView.setOpacity(p > 0.82 ? Math.max(0.0, 1.0 - (p - 0.82) / 0.18) : 1.0);
            if (p >= 1.0) {
                fishLeaping = false;
                leapingFishView.setVisible(false);
            }
        }

        // Chữ nổi
        if (floatingActive) {
            floatingElapsed += dt;
            double p = Math.min(1.0, floatingElapsed / FLOATING_DURATION);
            double easedY = 1.0 - Math.pow(1.0 - p, 2.0);
            floatingLabel.setTranslateX(floatingBaseX);
            floatingLabel.setTranslateY(floatingBaseY - easedY * 38.0);
            floatingLabel.setOpacity(p < 0.68 ? 1.0 : Math.max(0.0, 1.0 - (p - 0.68) / 0.32));
            if (p >= 1.0) {
                floatingActive = false;
                floatingLabel.setVisible(false);
            }
        }
    }

    private void animateGentleRipples(double timeSeconds, double freq, double maxExpand, double peakOpacity) {
        double rp1 = (timeSeconds * freq) % 1.0;
        bobberRipple1.setVisible(true);
        bobberRipple1.setRadius(6.0 + rp1 * maxExpand);
        bobberRipple1.setOpacity(peakOpacity * (1.0 - rp1));

        double rp2 = (timeSeconds * freq + 0.5) % 1.0;
        bobberRipple2.setVisible(true);
        bobberRipple2.setRadius(6.0 + rp2 * (maxExpand + 4.0));
        bobberRipple2.setOpacity((peakOpacity * 0.78) * (1.0 - rp2));
    }
}
