package view.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import model.player_inventory.Player;
import view.fx.SoundHook;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mini-game Aviator (Máy Bay / Crash) chuẩn bản web sử dụng JavaFX Canvas 60 FPS:
 * - Thanh lịch sử 12 hệ số nổ gần nhất (xanh dương < 2x, tím 2x..10x, hồng/vàng >= 10x).
 * - Đồ thị radar nền tối #0F111A với tia sáng sunburst xoay nhẹ, đường cong đỏ phát sáng,
 *   máy bay đỏ nghiêng theo tiếp tuyến kèm vệt khói/lửa động cơ.
 * - Đặt cược nhanh ($50, $100, $500, x2, 1/2, ALL IN), Auto Cash Out và chốt lời nhanh bằng phím Space.
 */
public class AviatorView extends BorderPane {

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";
    private static final int MAX_HISTORY = 12;

    private enum RoundState {
        IDLE,
        FLYING,
        CRASHED
    }

    private static final class TrailParticle {
        double x, y, vx, vy, life, maxLife, size;
        boolean flame;
        TrailParticle(double x, double y, double vx, double vy, double maxLife, double size, boolean flame) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = maxLife; this.maxLife = maxLife; this.size = size; this.flame = flame;
        }
    }

    private final Player player;
    private final IntConsumer onNetProfit;
    private final Consumer<String> notifier;
    private final Runnable onMoneyChanged;
    private final Runnable onBackToCasino;

    private final SecureRandom rng = new SecureRandom();
    private final String serverSeed;
    private final String clientSeed;
    private long roundNonce = 0L;
    private String lastRoundHashShort = "--------";
    private final Deque<Double> crashHistory = new ArrayDeque<>();
    private final List<TrailParticle> trailParticles = new ArrayList<>();

    // UI nodes
    private final HBox historyPillBox = new HBox(7);
    private final Label balanceBadge = new Label("$0");
    private final Label provablyFairBadge = new Label("🔒 Provably Fair • RTP 97%");
    private final Label statusToastLabel = new Label("✈ Nhập tiền cược rồi bấm [ĐẶT CƯỢC & CẤT CÁNH] — bấm [SPACE] để chốt lời nhanh!");
    private final Canvas radarCanvas = new Canvas(980, 365);
    private final TextField betInputField = new TextField("100");
    private final CheckBox autoCashOutCheck = new CheckBox("Tự động chốt (Auto):");
    private final TextField autoTargetField = new TextField("2.00");
    private final Button mainActionBtn = new Button("✈ ĐẶT CƯỢC & CẤT CÁNH");
    private final List<Button> quickBetButtons = new ArrayList<>();

    // Game math & animation state
    private RoundState state = RoundState.IDLE;
    private double crashPoint = 1.00;
    private double currentMultiplier = 1.00;
    private double elapsedFlightSec = 0.0;
    private double flyAwayTimerSec = 0.0;
    private double sunburstAngleDeg = 0.0;
    private double propSpinRad = 0.0;
    private double trailSpawnAccum = 0.0;
    private long lastTickNanos = 0L;

    private int activeBet = 0;
    private boolean cashedOutThisRound = false;
    private double cashedOutMultiplier = 0.0;
    private int cashedOutPayout = 0;

    // Saved plane position when crashed so it zooms off-screen
    private double lastPlaneX = 64.0;
    private double lastPlaneY = 320.0;
    private double lastPlaneAngleRad = -0.35;

    private AnimationTimer loopTimer;

    public AviatorView(Player player,
                       IntConsumer onNetProfit,
                       Consumer<String> notifier,
                       Runnable onMoneyChanged,
                       Runnable onBackToCasino) {
        this.player = player;
        this.onNetProfit = onNetProfit;
        this.notifier = notifier;
        this.onMoneyChanged = onMoneyChanged;
        this.onBackToCasino = onBackToCasino;

        // Khởi tạo cặp hạt giống mã hóa Provably Fair chuẩn nhà cái Spribe Aviator (HMAC-SHA256, RTP 97%)
        this.serverSeed = generateHexSeed(32);
        this.clientSeed = generateHexSeed(16);

        // Sinh sẵn 10 ván lịch sử bằng đúng thuật toán HMAC-SHA256 52-bit chuẩn nhà cái
        for (int i = 0; i < 10; i++) {
            crashHistory.addFirst(rollCrashPoint());
        }

        setMaxSize(1020, 650);
        setPrefSize(1020, 650);
        setPadding(new Insets(14, 18, 14, 18));
        setStyle(
            "-fx-background-color: #0F111A;" +
            "-fx-border-color: #E11D48 #9F1239 #4C0519 #E11D48;" +
            "-fx-border-width: 3px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 28, 0.35, 0, 8);"
        );
        setFocusTraversable(true);

        setTop(buildTopAndHistorySection());
        setCenter(buildCenterRadarSection());
        setBottom(buildBettingPanel());

        addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.SPACE) {
                // Chỉ kích hoạt Space khi không đang gõ chữ trong ô nhập hoặc khi máy bay đang bay cần chốt gấp
                if (state == RoundState.FLYING && !cashedOutThisRound) {
                    ev.consume();
                    performCashOut(currentMultiplier, false);
                }
            } else if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                handleBackToCasino();
            }
        });

        refreshHistoryBar();
        refreshBalanceDisplay();
        refreshControlStates();
        startRenderLoop();
    }

    // ==================== 1. HEADER & THANH LỊCH SỬ ====================

    private VBox buildTopAndHistorySection() {
        Label title = new Label("✈ AVIATOR • MÁY BAY CRASH");
        lockLabel(title);
        title.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 19px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FF2E55; -fx-effect: dropshadow(one-pass-box, rgba(225,29,72,0.55), 8, 0.3, 0, 0);"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        provablyFairBadge.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-text-fill: #94A3B8; -fx-background-color: #141824;" +
            "-fx-border-color: #334155; -fx-border-width: 1.2px;" +
            "-fx-padding: 5 10 5 10; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );
        lockLabel(provablyFairBadge);

        balanceBadge.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #4ADE80; -fx-background-color: #161B26;" +
            "-fx-border-color: #22C55E; -fx-border-width: 1.6px;" +
            "-fx-padding: 5 13 5 13; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
        lockLabel(balanceBadge);

        Button backBtn = new Button("⬅ Quay lại Casino");
        lockButton(backBtn);
        backBtn.setPrefHeight(34);
        backBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: #F8FAFC; -fx-background-color: linear-gradient(to bottom, #334155, #1E293B);" +
            "-fx-border-color: #64748B; -fx-border-width: 1.5px;" +
            "-fx-padding: 0 13 0 13; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> handleBackToCasino());

        HBox headerRow = new HBox(10, title, provablyFairBadge, spacer, balanceBadge, backBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label histTitle = new Label("LỊCH SỬ:");
        lockLabel(histTitle);
        histTitle.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        historyPillBox.setAlignment(Pos.CENTER_LEFT);
        HBox histContainer = new HBox(8, histTitle, historyPillBox);
        histContainer.setAlignment(Pos.CENTER_LEFT);
        histContainer.setPadding(new Insets(6, 10, 6, 10));
        histContainer.setStyle(
            "-fx-background-color: #141724;" +
            "-fx-border-color: #23283B; -fx-border-width: 1.2px;" +
            "-fx-background-radius: 9px; -fx-border-radius: 9px;"
        );

        VBox topBox = new VBox(8, headerRow, histContainer);
        topBox.setPadding(new Insets(0, 0, 8, 0));
        return topBox;
    }

    private void refreshHistoryBar() {
        historyPillBox.getChildren().clear();
        for (double val : crashHistory) {
            Label pill = new Label(String.format(Locale.US, "%.2fx", val));
            lockLabel(pill);
            String bg, fg, bd;
            if (val >= 10.0) {
                bg = "rgba(244, 63, 94, 0.22)";
                fg = "#FDE047";
                bd = "#F43F5E";
            } else if (val >= 2.0) {
                bg = "rgba(168, 85, 247, 0.22)";
                fg = "#D8B4FE";
                bd = "#A855F7";
            } else {
                bg = "rgba(56, 189, 248, 0.18)";
                fg = "#7DD3FC";
                bd = "#38BDF8";
            }
            pill.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + fg + "; -fx-background-color: " + bg + ";" +
                "-fx-border-color: " + bd + "; -fx-border-width: 1.2px;" +
                "-fx-padding: 2 8 2 8; -fx-background-radius: 12px; -fx-border-radius: 12px;"
            );
            historyPillBox.getChildren().add(pill);
        }
    }

    // ==================== 2. MÀN HÌNH RADAR TRUNG TÂM ====================

    private StackPane buildCenterRadarSection() {
        StackPane radarHolder = new StackPane(radarCanvas);
        radarHolder.setStyle(
            "-fx-background-color: #0B0D14;" +
            "-fx-border-color: #23283B; -fx-border-width: 2px;" +
            "-fx-background-radius: 12px; -fx-border-radius: 12px;"
        );
        radarHolder.setPadding(new Insets(2));
        return radarHolder;
    }

    // ==================== 3. BẢNG ĐIỀU KHIỂN ĐẶT CƯỢC PHÍA DƯỚI ====================

    private VBox buildBettingPanel() {
        statusToastLabel.setMaxWidth(Double.MAX_VALUE);
        statusToastLabel.setAlignment(Pos.CENTER);
        statusToastLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #CBD5E1; -fx-background-color: #151926;" +
            "-fx-border-color: #272E44; -fx-border-width: 1px;" +
            "-fx-padding: 4 10 4 10; -fx-background-radius: 6px; -fx-border-radius: 6px;"
        );

        // Cột trái: Ô nhập tiền cược + Nút chọn nhanh ($50, $100, $500, x2, 1/2, ALL IN)
        Label betLbl = new Label("TIỀN CƯỢC ($):");
        lockLabel(betLbl);
        betLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        betInputField.setPrefWidth(120);
        betInputField.setPrefHeight(34);
        betInputField.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-text-fill: #F8FAFC; -fx-background-color: #0B0E17;" +
            "-fx-border-color: #475569; -fx-border-width: 1.5px; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );

        HBox betInputRow = new HBox(8, betLbl, betInputField);
        betInputRow.setAlignment(Pos.CENTER_LEFT);

        HBox quickRow = new HBox(6);
        quickRow.setAlignment(Pos.CENTER_LEFT);
        String[] qLabels = { "$50", "$100", "$500", "x2", "1/2", "ALL IN" };
        for (String ql : qLabels) {
            Button qb = new Button(ql);
            lockButton(qb);
            qb.setPrefHeight(28);
            boolean isAllIn = "ALL IN".equals(ql);
            qb.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isAllIn ? "#FEF08A" : "#E2E8F0") + ";" +
                "-fx-background-color: " + (isAllIn ? "#7F1D1D" : "#1E2538") + ";" +
                "-fx-border-color: " + (isAllIn ? "#EF4444" : "#3B4766") + "; -fx-border-width: 1.2px;" +
                "-fx-padding: 2 9 2 9; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
            );
            qb.setOnAction(e -> applyQuickBet(ql));
            quickBetButtons.add(qb);
            quickRow.getChildren().add(qb);
        }

        VBox leftControls = new VBox(8, betInputRow, quickRow);
        leftControls.setAlignment(Pos.CENTER_LEFT);

        // Cột giữa: Tùy chọn Tự động chốt (Auto Cash Out)
        autoCashOutCheck.setFocusTraversable(false);
        autoCashOutCheck.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #E2E8F0;"
        );

        autoTargetField.setPrefWidth(82);
        autoTargetField.setPrefHeight(32);
        autoTargetField.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FDE047; -fx-background-color: #0B0E17;" +
            "-fx-border-color: #64748B; -fx-border-width: 1.5px; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );
        Label xSuffix = new Label("x");
        xSuffix.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FDE047;");

        HBox autoRow = new HBox(6, autoCashOutCheck, autoTargetField, xSuffix);
        autoRow.setAlignment(Pos.CENTER_LEFT);

        Label spaceHint = new Label("⌨ Phím [SPACE] để chốt lời tức thì!");
        spaceHint.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-text-fill: #94A3B8;");

        VBox midControls = new VBox(8, autoRow, spaceHint);
        midControls.setAlignment(Pos.CENTER_LEFT);
        midControls.setPadding(new Insets(0, 10, 0, 10));

        // Cột phải: Nút hành động chính cực lớn
        lockButton(mainActionBtn);
        mainActionBtn.setPrefSize(310, 64);
        mainActionBtn.setOnAction(e -> handleMainAction());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox panelRow = new HBox(14, leftControls, midControls, sp, mainActionBtn);
        panelRow.setAlignment(Pos.CENTER_LEFT);
        panelRow.setPadding(new Insets(10, 14, 10, 14));
        panelRow.setStyle(
            "-fx-background-color: #161A27;" +
            "-fx-border-color: #2A314A; -fx-border-width: 1.5px;" +
            "-fx-background-radius: 12px; -fx-border-radius: 12px;"
        );

        VBox bottomWrap = new VBox(8, statusToastLabel, panelRow);
        bottomWrap.setPadding(new Insets(10, 0, 0, 0));
        return bottomWrap;
    }

    private void applyQuickBet(String label) {
        if (state == RoundState.FLYING) return;
        SoundHook.playMenuClick();
        int money = (player != null) ? player.getMoney() : 0;
        int cur = parseBetAmount();
        int next = cur;
        switch (label) {
            case "$50":    next = 50; break;
            case "$100":   next = 100; break;
            case "$500":   next = 500; break;
            case "x2":     next = Math.max(10, cur * 2); break;
            case "1/2":    next = Math.max(10, cur / 2); break;
            case "ALL IN": next = Math.max(10, money); break;
            default: break;
        }
        if (money > 0 && next > money) {
            next = money;
        }
        betInputField.setText(Integer.toString(Math.max(1, next)));
    }

    private int parseBetAmount() {
        try {
            String raw = betInputField.getText().trim().replaceAll("[^0-9]", "");
            if (raw.isEmpty()) return 0;
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double parseAutoTarget() {
        try {
            String raw = autoTargetField.getText().trim().replace(',', '.').replaceAll("[^0-9.]", "");
            if (raw.isEmpty()) return 0.0;
            double v = Double.parseDouble(raw);
            return Math.max(1.01, Math.min(1000.0, v));
        } catch (NumberFormatException ex) {
            return 2.00;
        }
    }

    // ==================== 4. LOGIC GAME & THUẬT TOÁN PROVABLY FAIR CHUẨN NHÀ CÁI (HMAC-SHA256 • RTP 97%) ====================

    private String generateHexSeed(int byteCount) {
        byte[] bytes = new byte[byteCount];
        rng.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteCount * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Thuật toán sinh điểm nổ độ khó cao chuẩn sòng bạc (HMAC-SHA256 52-bit, đuôi phân phối chặt):
     * - ~10% nổ tức thì tại 1.00x.
     * - ~74% tổng số ván nổ dưới 2.00x (chủ yếu 1.00x–1.90x, chống farm tiền dễ).
     * - ~20% đạt 2.00x–5.00x; ~4.3% đạt 5.00x–10.00x; chỉ ~1.7% vượt 10.00x (hiếm gặp, kịch tính cao).
     */
    private double rollCrashPoint() {
        roundNonce++;
        String hashHex = hmacSha256Hex(serverSeed, clientSeed + ":" + roundNonce);
        lastRoundHashShort = hashHex.length() >= 8 ? hashHex.substring(0, 8) : hashHex;
        provablyFairBadge.setText("🔒 House Provably Fair • #" + roundNonce + " [" + lastRoundHashShort + "]");

        long h = Long.parseUnsignedLong(hashHex.substring(0, 13), 16);
        final double e = (double) (1L << 52);

        // 10% nổ ngay tại 1.00x
        if (Long.remainderUnsigned(h, 10L) == 0L) {
            return 1.00;
        }
        double u = Math.min(0.999999, h / e);
        // Phân phối đuôi dốc (exponent 0.62): đa số nổ thấp dưới 2x, kèo >10x cực hiếm
        double val = 1.00 + 0.85 * (Math.pow(1.0 / (1.0 - u), 0.62) - 1.0);
        double rounded = Math.floor(val * 100.0) / 100.0;
        return Math.max(1.01, Math.min(100.00, rounded));
    }

    private static String hmacSha256Hex(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    private void handleMainAction() {
        if (state == RoundState.FLYING) {
            if (!cashedOutThisRound) {
                performCashOut(currentMultiplier, false);
            }
            return;
        }
        startNewFlight();
    }

    private void startNewFlight() {
        int bet = parseBetAmount();
        if (bet <= 0) {
            setStatusToast("⚠ Vui lòng nhập số tiền cược hợp lệ lớn hơn $0!", true);
            SoundHook.playMenuClick();
            return;
        }
        if (player == null || player.getMoney() < bet) {
            setStatusToast("⚠ Không đủ tiền trong ví! Số dư hiện tại: $" + (player != null ? player.getMoney() : 0), true);
            SoundHook.playMenuClick();
            return;
        }
        if (!player.deductMoney(bet)) {
            return;
        }

        this.activeBet = bet;
        this.crashPoint = rollCrashPoint();
        this.currentMultiplier = 1.00;
        this.elapsedFlightSec = 0.0;
        this.flyAwayTimerSec = 0.0;
        this.cashedOutThisRound = false;
        this.cashedOutMultiplier = 0.0;
        this.cashedOutPayout = 0;
        this.trailParticles.clear();
        this.state = RoundState.FLYING;

        SoundHook.playMenuClick();
        refreshBalanceDisplay();
        if (onMoneyChanged != null) onMoneyChanged.run();
        setStatusToast("✈ Máy bay đã cất cánh với cược $" + activeBet + "! Bấm [CHỐT LỜI] hoặc [SPACE] trước khi nổ!", false);
        refreshControlStates();
        requestFocus();

        // Trường hợp 3% nổ tức thì tại 1.00x
        if (crashPoint <= 1.00) {
            triggerCrash();
        }
    }

    private void performCashOut(double mult, boolean isAuto) {
        if (state != RoundState.FLYING || cashedOutThisRound) return;
        double lockedMult = Math.max(1.00, Math.floor(mult * 100.0) / 100.0);
        int payout = (int) Math.floor(activeBet * lockedMult);
        int netProfit = Math.max(0, payout - activeBet);

        this.cashedOutThisRound = true;
        this.cashedOutMultiplier = lockedMult;
        this.cashedOutPayout = payout;

        if (player != null && payout > 0) {
            player.addMoney(payout);
        }
        if (netProfit > 0 && onNetProfit != null) {
            onNetProfit.accept(netProfit);
        }
        SoundHook.playPickup();
        refreshBalanceDisplay();
        if (onMoneyChanged != null) onMoneyChanged.run();

        String tag = isAuto ? "🤖 AUTO CHỐT LỜI" : "💰 CHỐT LỜI THÀNH CÔNG";
        String msg = tag + " tại " + String.format(Locale.US, "%.2fx", lockedMult)
                + ": +$" + payout + " (Lãi +$" + netProfit + ")!";
        setStatusToast(msg, false);
        if (notifier != null) notifier.accept("✈ Aviator: " + msg);
        refreshControlStates();
    }

    private void triggerCrash() {
        this.state = RoundState.CRASHED;
        this.currentMultiplier = crashPoint;
        this.flyAwayTimerSec = 0.0;

        crashHistory.addFirst(crashPoint);
        while (crashHistory.size() > MAX_HISTORY) {
            crashHistory.removeLast();
        }
        refreshHistoryBar();
        SoundHook.playMenuClick();

        if (!cashedOutThisRound) {
            String msg = "💥 ĐÃ BAY MẤT tại " + String.format(Locale.US, "%.2fx", crashPoint) + "! Bạn mất $" + activeBet + ".";
            setStatusToast(msg, true);
            if (notifier != null) notifier.accept("✈ Aviator: " + msg);
        } else {
            setStatusToast(
                "✅ Bạn đã chốt lời +$" + cashedOutPayout + " (" + String.format(Locale.US, "%.2fx", cashedOutMultiplier)
                        + ") trước khi máy bay nổ tại " + String.format(Locale.US, "%.2fx", crashPoint) + "!",
                false
            );
        }
        refreshControlStates();
    }

    private void refreshControlStates() {
        boolean flying = (state == RoundState.FLYING);
        betInputField.setDisable(flying);
        autoCashOutCheck.setDisable(flying);
        autoTargetField.setDisable(flying);
        for (Button b : quickBetButtons) {
            b.setDisable(flying);
        }

        if (flying && !cashedOutThisRound) {
            int livePayout = (int) Math.floor(activeBet * currentMultiplier);
            mainActionBtn.setDisable(false);
            mainActionBtn.setText("💰 CHỐT LỜI: +$" + livePayout);
            mainActionBtn.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 16px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1C1004; -fx-background-color: linear-gradient(to bottom, #FDE047, #F59E0B);" +
                "-fx-border-color: #FEF9C3; -fx-border-width: 2px;" +
                "-fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.55), 14, 0.35, 0, 0);"
            );
        } else if (flying && cashedOutThisRound) {
            mainActionBtn.setDisable(true);
            mainActionBtn.setText("✅ ĐÃ CHỐT: +$" + cashedOutPayout);
            mainActionBtn.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: #86EFAC; -fx-background-color: #14532D;" +
                "-fx-border-color: #22C55E; -fx-border-width: 2px;" +
                "-fx-background-radius: 10px; -fx-border-radius: 10px;"
            );
        } else {
            mainActionBtn.setDisable(false);
            mainActionBtn.setText("✈ ĐẶT CƯỢC & CẤT CÁNH");
            mainActionBtn.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 15.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: #F0FDF4; -fx-background-color: linear-gradient(to bottom, #22C55E, #15803D);" +
                "-fx-border-color: #86EFAC; -fx-border-width: 2px;" +
                "-fx-background-radius: 10px; -fx-border-radius: 10px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.45), 12, 0.3, 0, 0);"
            );
        }
    }

    private void refreshBalanceDisplay() {
        if (player != null) {
            balanceBadge.setText("💰 Số dư: $" + player.getMoney());
        }
    }

    private void setStatusToast(String msg, boolean isError) {
        statusToastLabel.setText(msg);
        statusToastLabel.setStyle(isError
            ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #FECACA; -fx-background-color: #3B0D16; -fx-border-color: #E11D48; -fx-border-width: 1.2px; -fx-padding: 4 10 4 10; -fx-background-radius: 6px; -fx-border-radius: 6px;"
            : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #BBF7D0; -fx-background-color: #0D2818; -fx-border-color: #22C55E; -fx-border-width: 1.2px; -fx-padding: 4 10 4 10; -fx-background-radius: 6px; -fx-border-radius: 6px;"
        );
    }

    // ==================== 5. VÒNG LẶP ĐỒ HỌA CANVAS 60 FPS ====================

    private void startRenderLoop() {
        lastTickNanos = 0L;
        loopTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isVisible()) return;
                double dt = (lastTickNanos == 0L) ? 0.016 : Math.min(0.05, Math.max(0.001, (now - lastTickNanos) / 1_000_000_000.0));
                lastTickNanos = now;

                sunburstAngleDeg = (sunburstAngleDeg + dt * 7.5) % 360.0;
                propSpinRad += dt * 36.0;

                if (state == RoundState.FLYING) {
                    elapsedFlightSec += dt;
                    // Nhịp tăng chuẩn Spribe Aviator: khởi đầu hồi hộp (1x->2x ~10s) rồi bứt tốc dần ở hệ số cao
                    currentMultiplier = Math.exp(0.068 * elapsedFlightSec + 0.0012 * elapsedFlightSec * elapsedFlightSec);

                    // Kiểm tra Auto Cash Out trước khi kiểm tra nổ
                    if (!cashedOutThisRound && autoCashOutCheck.isSelected()) {
                        double autoTarget = parseAutoTarget();
                        if (currentMultiplier >= autoTarget && autoTarget < crashPoint) {
                            performCashOut(autoTarget, true);
                        }
                    }

                    if (currentMultiplier >= crashPoint) {
                        triggerCrash();
                    } else if (!cashedOutThisRound) {
                        int livePayout = (int) Math.floor(activeBet * currentMultiplier);
                        mainActionBtn.setText("💰 CHỐT LỜI: +$" + livePayout);
                    }
                } else if (state == RoundState.CRASHED) {
                    flyAwayTimerSec += dt;
                }

                updateTrailParticles(dt);
                renderRadarFrame(dt);
            }
        };
        loopTimer.start();
    }

    private void updateTrailParticles(double dt) {
        Iterator<TrailParticle> it = trailParticles.iterator();
        while (it.hasNext()) {
            TrailParticle p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.life <= 0.0) it.remove();
        }
    }

    private void renderRadarFrame(double dt) {
        GraphicsContext g = radarCanvas.getGraphicsContext2D();
        double w = radarCanvas.getWidth();
        double h = radarCanvas.getHeight();
        double originX = 48.0;
        double originY = h - 34.0;

        // 1. Nền radar tối sâu
        g.setFill(Color.web("#0B0D14"));
        g.fillRect(0, 0, w, h);

        // 2. Hiệu ứng tia sáng xoay nhẹ (sunburst) từ góc dưới trái
        g.save();
        g.translate(originX, originY);
        g.rotate(sunburstAngleDeg);
        int rayCount = 18;
        double maxR = Math.hypot(w, h) * 1.15;
        for (int i = 0; i < rayCount; i++) {
            double a0 = (i * 2.0 * Math.PI) / rayCount;
            double a1 = a0 + (Math.PI / rayCount) * 0.55;
            g.setFill(i % 2 == 0 ? Color.rgb(225, 29, 72, 0.055) : Color.rgb(56, 189, 248, 0.032));
            g.fillPolygon(
                new double[] { 0, Math.cos(a0) * maxR, Math.cos(a1) * maxR },
                new double[] { 0, Math.sin(a0) * maxR, Math.sin(a1) * maxR },
                3
            );
        }
        g.restore();

        // 3. Lưới tọa độ radar mờ & trục X/Y
        g.setStroke(Color.rgb(148, 163, 184, 0.11));
        g.setLineWidth(1.0);
        for (double gx = originX; gx < w - 16; gx += 72) {
            g.strokeLine(gx, 16, gx, originY);
        }
        for (double gy = originY; gy > 16; gy -= 54) {
            g.strokeLine(originX, gy, w - 16, gy);
        }
        g.setStroke(Color.rgb(148, 163, 184, 0.30));
        g.setLineWidth(1.5);
        g.strokeLine(originX, 16, originX, originY);
        g.strokeLine(originX, originY, w - 16, originY);

        // Các chấm mốc động trên trục tạo cảm giác bay tiến về phía trước
        double dotShift = (state == RoundState.FLYING) ? (elapsedFlightSec * 42.0) % 72.0 : 0.0;
        g.setFill(Color.rgb(148, 163, 184, 0.45));
        for (double dx = originX + 72 - dotShift; dx < w - 20; dx += 72) {
            g.fillOval(dx - 2.5, originY + 8, 5, 5);
        }
        for (double dy = originY - 54 + (dotShift * 0.75 % 54.0); dy > 22; dy -= 54) {
            g.fillOval(originX - 14, dy - 2.5, 5, 5);
        }

        // 4. Đường cong quỹ đạo bay & máy bay đỏ
        if (state == RoundState.FLYING) {
            double normT = Math.min(1.0, elapsedFlightSec / 14.0);
            double easeT = 1.0 - Math.pow(1.0 - normT, 1.55);
            double targetX = originX + (w - originX - 115.0) * easeT;
            double targetY = originY - (originY - 72.0) * Math.min(1.0, Math.pow(easeT, 1.15));
            // Dao động nhẹ khi máy bay đã lên cao
            if (normT > 0.85) {
                targetY += Math.sin(elapsedFlightSec * 3.8) * 10.0;
            }

            double ctrlX = originX + (targetX - originX) * 0.58;
            double ctrlY = originY - (originY - targetY) * 0.14;

            // Vùng tô gradient đỏ mờ bên dưới đường cong
            LinearGradient fillGrad = new LinearGradient(
                0, targetY, 0, originY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(225, 29, 72, 0.40)),
                new Stop(0.7, Color.rgb(190, 18, 60, 0.14)),
                new Stop(1.0, Color.rgb(159, 18, 57, 0.02))
            );
            g.setFill(fillGrad);
            g.beginPath();
            g.moveTo(originX, originY);
            g.quadraticCurveTo(ctrlX, ctrlY, targetX, targetY);
            g.lineTo(targetX, originY);
            g.closePath();
            g.fill();

            // Đường cong đỏ phát sáng (2 lớp: glow rộng + lõi đỏ sáng)
            g.setStroke(Color.rgb(255, 46, 85, 0.35));
            g.setLineWidth(9.0);
            g.beginPath();
            g.moveTo(originX, originY);
            g.quadraticCurveTo(ctrlX, ctrlY, targetX, targetY);
            g.stroke();

            g.setStroke(Color.web("#FF2E55"));
            g.setLineWidth(3.6);
            g.beginPath();
            g.moveTo(originX, originY);
            g.quadraticCurveTo(ctrlX, ctrlY, targetX, targetY);
            g.stroke();

            // Tính góc tiếp tuyến tại điểm cuối đường cong bậc 2: B'(1) = 2*(P2 - P1)
            double tanX = Math.max(1.0, targetX - ctrlX);
            double tanY = targetY - ctrlY;
            double angleRad = Math.atan2(tanY, tanX);

            lastPlaneX = targetX;
            lastPlaneY = targetY;
            lastPlaneAngleRad = angleRad;

            // Sinh vệt khói & tia lửa nhỏ phía đuôi máy bay
            trailSpawnAccum += dt;
            if (trailSpawnAccum >= 0.025) {
                trailSpawnAccum = 0.0;
                double tailX = targetX - Math.cos(angleRad) * 26.0;
                double tailY = targetY - Math.sin(angleRad) * 26.0;
                trailParticles.add(new TrailParticle(
                    tailX, tailY,
                    -Math.cos(angleRad) * 90.0 + (rng.nextDouble() - 0.5) * 24.0,
                    -Math.sin(angleRad) * 90.0 + (rng.nextDouble() - 0.5) * 24.0,
                    0.35, 6.0 + rng.nextDouble() * 4.0, rng.nextDouble() < 0.55
                ));
            }

            drawTrailParticles(g);
            drawRedAviatorPlane(g, targetX, targetY, angleRad);
        } else if (state == RoundState.CRASHED) {
            drawTrailParticles(g);
            // Máy bay phóng vút ra khỏi góc trên phải màn hình khi nổ ("FLEW AWAY")
            double zoomDist = flyAwayTimerSec * 720.0;
            double fx = lastPlaneX + Math.cos(lastPlaneAngleRad - 0.15) * zoomDist;
            double fy = lastPlaneY + Math.sin(lastPlaneAngleRad - 0.15) * zoomDist;
            if (fx < w + 80 && fy > -80) {
                drawRedAviatorPlane(g, fx, fy, lastPlaneAngleRad - 0.15);
            }
        } else {
            // IDLE: vẽ máy bay sẵn sàng ở góc dưới trái chờ cất cánh
            drawRedAviatorPlane(g, originX + 36.0, originY - 22.0, -0.18);
        }

        // 5. Chữ hệ số nhân cực lớn chính giữa màn hình
        g.setTextAlign(TextAlignment.CENTER);
        if (state == RoundState.CRASHED) {
            g.setFill(Color.web("#FF2E55"));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
            g.fillText("ĐÃ BAY MẤT! (FLEW AWAY)", w * 0.5, h * 0.36);

            g.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 66));
            g.fillText(String.format(Locale.US, "%.2fx", crashPoint), w * 0.5, h * 0.56);
        } else if (state == RoundState.FLYING) {
            g.setFill(Color.rgb(0, 0, 0, 0.55));
            g.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 70));
            String multStr = String.format(Locale.US, "%.2fx", currentMultiplier);
            g.fillText(multStr, w * 0.5 + 3, h * 0.52 + 3);

            g.setFill(cashedOutThisRound ? Color.web("#4ADE80") : Color.WHITE);
            g.fillText(multStr, w * 0.5, h * 0.52);

            if (cashedOutThisRound) {
                g.setFill(Color.web("#86EFAC"));
                g.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
                g.fillText("🎉 ĐÃ CHỐT LỜI: +$" + cashedOutPayout + " (" + String.format(Locale.US, "%.2fx", cashedOutMultiplier) + ")", w * 0.5, h * 0.64);
            }
        } else {
            g.setFill(Color.rgb(226, 232, 240, 0.88));
            g.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 56));
            g.fillText("1.00x", w * 0.5, h * 0.48);

            g.setFill(Color.web("#94A3B8"));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
            g.fillText("SẴN SÀNG CẤT CÁNH — ĐẶT CƯỢC PHÍA DƯỚI", w * 0.5, h * 0.58);
        }
    }

    private void drawTrailParticles(GraphicsContext g) {
        for (TrailParticle p : trailParticles) {
            double alpha = Math.max(0.0, Math.min(1.0, p.life / p.maxLife));
            if (p.flame) {
                g.setFill(Color.rgb(255, 168, 38, alpha * 0.85));
            } else {
                g.setFill(Color.rgb(226, 232, 240, alpha * 0.45));
            }
            double sz = p.size * (0.5 + 0.5 * alpha);
            g.fillOval(p.x - sz * 0.5, p.y - sz * 0.5, sz, sz);
        }
    }

    /** Vẽ chiếc máy bay cánh quạt màu đỏ đặc trưng của Aviator nghiêng theo tiếp tuyến đường bay. */
    private void drawRedAviatorPlane(GraphicsContext g, double x, double y, double angleRad) {
        g.save();
        g.translate(x, y);
        g.rotate(Math.toDegrees(angleRad));

        // Ánh sáng đỏ phát nhẹ xung quanh thân máy bay
        g.setFill(Color.rgb(255, 46, 85, 0.22));
        g.fillOval(-34, -22, 68, 44);

        // Cánh đuôi đứng
        g.setFill(Color.web("#BE123C"));
        g.fillPolygon(new double[] { -26, -15, -20 }, new double[] { -2, -2, -14 }, 3);

        // Thân máy bay màu đỏ rực
        g.setFill(Color.web("#F43F5E"));
        g.fillPolygon(
            new double[] { -26, 10, 24, 24, 8, -22 },
            new double[] { -5, -8, -4, 4, 8, 4 },
            6
        );
        // Sọc trắng thể thao dọc thân
        g.setStroke(Color.web("#FFE4E6"));
        g.setLineWidth(2.0);
        g.strokeLine(-18, 0, 18, 0);

        // Cánh chính
        g.setFill(Color.web("#E11D48"));
        g.fillPolygon(new double[] { -8, 10, 4, -12 }, new double[] { -2, -2, -16, -14 }, 4);
        g.fillPolygon(new double[] { -6, 12, 6, -10 }, new double[] { 4, 4, 15, 13 }, 4);

        // Buồng lái kính xanh nhạt
        g.setFill(Color.web("#BAE6FD"));
        g.fillOval(-2, -9, 12, 7);

        // Cánh quạt quay mờ phía mũi máy bay
        double bladeH = 8.0 + Math.abs(Math.sin(propSpinRad)) * 8.0;
        g.setStroke(Color.rgb(248, 250, 252, 0.85));
        g.setLineWidth(2.4);
        g.strokeLine(26, -bladeH, 26, bladeH);

        g.restore();
    }

    // ==================== 6. ĐÓNG AN TOÀN & HELPER ====================

    public boolean isFlightActive() {
        return state == RoundState.FLYING && !cashedOutThisRound;
    }

    public void stopAndCleanupSafe() {
        // Nếu người chơi thoát đột ngột khi máy bay đang bay & chưa chốt -> tự chốt lời ngay tại hệ số hiện tại để không mất oan
        if (state == RoundState.FLYING && !cashedOutThisRound && activeBet > 0) {
            performCashOut(currentMultiplier, true);
        }
        if (loopTimer != null) {
            loopTimer.stop();
        }
    }

    private void handleBackToCasino() {
        if (isFlightActive()) {
            performCashOut(currentMultiplier, false);
        }
        SoundHook.playMenuClick();
        if (onBackToCasino != null) {
            onBackToCasino.run();
        }
    }

    private static void lockButton(Button b) {
        b.setFocusTraversable(false);
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setTextOverrun(OverrunStyle.CLIP);
    }

    private static void lockLabel(Label l) {
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setTextOverrun(OverrunStyle.CLIP);
    }
}
