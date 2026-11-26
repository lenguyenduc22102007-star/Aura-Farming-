package view.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.player_inventory.Player;
import view.assets.Assets;
import view.fx.SoundHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Sòng Bạc Đánh Bài (CasinoOverlay) — Bàn nỉ xanh viền gỗ sang trọng:
 * 1. Xì Dách (Blackjack 21): Cược $20, $50, $100, $500, ALL-IN. Thắng ăn x2, Xì Dách 2 lá ăn x2.5, Hòa hoàn tiền, Quắc (>21) thua.
 * 2. Đoán Bài Cao/Thấp (High-Low): Đoán lá tiếp theo CAO HƠN hay THẤP HƠN, thắng có thể rút ngay hoặc cược tiếp nhân đôi liên hoàn.
 */
public class CasinoOverlay extends StackPane {

    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

    private static class Card {
        final int rankIndex; // 0=A, 1=2, ..., 9=10, 10=J, 11=Q, 12=K
        final int suitIndex; // 0=♠, 1=♥, 2=♦, 3=♣

        Card(int rankIndex, int suitIndex) {
            this.rankIndex = rankIndex;
            this.suitIndex = suitIndex;
        }

        String rankText() { return RANKS[rankIndex]; }
        String suitText() { return SUITS[suitIndex]; }
        boolean isRed() { return suitIndex == 1 || suitIndex == 2; }
        int highLowValue() { return rankIndex + 1; } // A=1 .. K=13
    }

    private final Player player;
    private final IntConsumer onNetProfit;
    private final Consumer<String> notifier;
    private final Runnable onCloseCallback;

    private Label moneyBadgeLabel;
    private Button tabBlackjackBtn;
    private Button tabHighLowBtn;
    private Button tabSlotBtn;
    private StackPane gameBodyPane;
    private int activeMode = 0; // 0 = Blackjack, 1 = High-Low, 2 = Slot Machine

    // Trạng thái mức cược đang chọn (-1 = ALL-IN)
    private int selectedBetOption = 100;
    private final List<Button> betButtons = new ArrayList<>();

    // ====== Trạng thái Xì Dách (Blackjack 21) ======
    private final List<Card> bjDeck = new ArrayList<>();
    private final List<Card> bjPlayerHand = new ArrayList<>();
    private final List<Card> bjDealerHand = new ArrayList<>();
    private boolean bjRoundActive = false;
    private int bjActiveBet = 0;
    private HBox bjDealerCardsBox;
    private HBox bjPlayerCardsBox;
    private Label bjDealerScoreLabel;
    private Label bjPlayerScoreLabel;
    private Label bjStatusLabel;
    private Button bjDealBtn;
    private Button bjHitBtn;
    private Button bjStandBtn;
    private VBox blackjackView;

    // ====== Trạng thái Đoán Bài Cao/Thấp (High-Low) ======
    private final List<Card> hlDeck = new ArrayList<>();
    private Card hlCurrentCard = null;
    private Card hlPreviousCard = null;
    private boolean hlRoundActive = false;
    private int hlInitialBet = 0;
    private int hlCurrentPot = 0;
    private int hlStreak = 0;
    private StackPane hlPrevCardSlot;
    private StackPane hlCurrCardSlot;
    private Label hlPotLabel;
    private Label hlStatusLabel;
    private Button hlStartBtn;
    private Button hlHigherBtn;
    private Button hlLowerBtn;
    private Button hlCashOutBtn;
    private VBox highLowView;

    // ====== Trạng thái Máy Xèng 3 Cuộn (3-Reel Slot Machine) ======
    private enum SlotSymbol {
        TOMATO("CÀ CHUA", "#E53935"),
        CORN("BẮP NGÔ", "#FBC02D"),
        FISH("CÁ SÔNG", "#29B6F6"),
        MONEY_BAG("TÚI TIỀN", "#FFB300"),
        DIAMOND("KIM CƯƠNG", "#00E5FF"),
        LUCKY_SEVEN("SỐ 7", "#FF1744");

        final String viLabel;
        final String accentHex;
        SlotSymbol(String viLabel, String accentHex) {
            this.viLabel = viLabel;
            this.accentHex = accentHex;
        }
    }

    private static final SlotSymbol[] WEIGHTED_REEL_STRIP = {
        SlotSymbol.TOMATO, SlotSymbol.TOMATO, SlotSymbol.TOMATO,
        SlotSymbol.CORN, SlotSymbol.CORN, SlotSymbol.CORN,
        SlotSymbol.FISH, SlotSymbol.FISH, SlotSymbol.FISH,
        SlotSymbol.MONEY_BAG, SlotSymbol.MONEY_BAG,
        SlotSymbol.DIAMOND, SlotSymbol.DIAMOND,
        SlotSymbol.LUCKY_SEVEN, SlotSymbol.LUCKY_SEVEN
    };

    private final Random slotRng = new Random();
    private final Image[] slotSymbolImages = new Image[SlotSymbol.values().length];
    private final VBox[] slotReelBoxes = new VBox[3];
    private final ImageView[] slotReelIcons = new ImageView[3];
    private final Label[] slotReelNames = new Label[3];
    private final SlotSymbol[] slotFinalSymbols = new SlotSymbol[3];
    private boolean slotSpinning = false;
    private int slotActiveBet = 0;
    private double slotElapsedSec = 0.0;
    private boolean reel0Stopped = false;
    private boolean reel1Stopped = false;
    private Timeline slotSpinTimeline = null;
    private Timeline jackpotBlinkTimeline = null;
    private Label slotStatusLabel;
    private Button slotSpinBtn;
    private VBox slotMachineView;

    public CasinoOverlay(Player player,
                         IntConsumer onNetProfit,
                         Consumer<String> notifier,
                         Runnable onCloseCallback) {
        this.player = player;
        this.onNetProfit = onNetProfit;
        this.notifier = notifier;
        this.onCloseCallback = onCloseCallback;

        setStyle("-fx-background-color: rgba(8, 12, 10, 0.72);");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        BorderPane casinoBoard = new BorderPane();
        casinoBoard.setMaxSize(960, 635);
        casinoBoard.setPrefSize(960, 635);
        casinoBoard.setPadding(new Insets(14, 24, 16, 24));
        casinoBoard.setStyle(
            "-fx-background-color: radial-gradient(center 50% 45%, radius 75%, #1E7A44 0%, #12522D 68%, #0B381E 100%);" +
            "-fx-border-color: #6B3E1B #4A2910 #381D0A #6B3E1B;" +
            "-fx-border-width: 8px;" +
            "-fx-background-radius: 18px;" +
            "-fx-border-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.78), 26, 0.3, 0, 8);"
        );

        // ======= TOP HEADER (Tách riêng hàng Tab trò chơi & hàng Mức cược rộng rãi, không bao giờ bị dấu ...) =======
        HBox topBar = buildTopHeader();
        VBox topSection = new VBox(9, topBar, buildTabRow(), buildBetRow());
        casinoBoard.setTop(topSection);

        // ======= CENTER GAME AREA =======
        initSlotSymbolArt();
        blackjackView = buildBlackjackPane();
        highLowView = buildHighLowPane();
        slotMachineView = buildSlotMachinePane();
        gameBodyPane = new StackPane(blackjackView, highLowView, slotMachineView);
        BorderPane.setMargin(gameBodyPane, new Insets(10, 0, 0, 0));
        casinoBoard.setCenter(gameBodyPane);

        getChildren().add(casinoBoard);

        switchMode(0);
        refreshMoneyHeader();
        refreshBetButtonStyles();

        setOnMouseClicked(ev -> {
            if (ev.getTarget() == this) {
                requestSafeClose();
            }
        });
        setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                requestSafeClose();
            }
        });
    }

    private HBox buildTopHeader() {
        Circle bulbL = new Circle(6, Color.web("#FFD54F"));
        bulbL.setStroke(Color.web("#7A4500"));
        Circle bulbR = new Circle(6, Color.web("#FF6B6B"));
        bulbR.setStroke(Color.web("#6B1111"));

        Label title = new Label("🎰 GRAND ROYAL CASINO • SÒNG BẠC NÔNG TRẠI");
        lockNoEllipsis(title);
        title.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 18.5px; -fx-font-weight: bold; -fx-text-fill: #FFE066;" +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.85), 2, 1.0, 0, 1);"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        moneyBadgeLabel = new Label("$0");
        lockNoEllipsis(moneyBadgeLabel);
        moneyBadgeLabel.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 15.5px; -fx-font-weight: bold; -fx-text-fill: #FFE885;" +
            "-fx-background-color: rgba(24, 14, 8, 0.86);" +
            "-fx-border-color: #E2A93B; -fx-border-width: 2px;" +
            "-fx-padding: 5 14 5 14; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setFocusTraversable(false);
        lockNoEllipsis(closeBtn);
        closeBtn.setMinSize(38, 34);
        closeBtn.setPrefSize(38, 34);
        closeBtn.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-padding: 0;" +
            "-fx-text-fill: #FFF4E6; -fx-background-color: linear-gradient(to bottom, #D64545, #9E2626);" +
            "-fx-border-color: #4D0F0F; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> requestSafeClose());

        HBox bar = new HBox(10, bulbL, bulbR, title, spacer, moneyBadgeLabel, closeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 7, 12));
        bar.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(66,34,14,0.95), rgba(40,20,8,0.95));" +
            "-fx-border-color: #D49B38; -fx-border-width: 2px; -fx-background-radius: 10px; -fx-border-radius: 10px;"
        );
        return bar;
    }

    private HBox buildTabRow() {
        tabBlackjackBtn = new Button("🃏 XÌ DÁCH (BLACKJACK 21)");
        tabBlackjackBtn.setFocusTraversable(false);
        lockNoEllipsis(tabBlackjackBtn);
        tabBlackjackBtn.setPrefWidth(285);
        tabBlackjackBtn.setOnAction(e -> {
            if (hlRoundActive || slotSpinning) {
                notifyMsg("Hãy hoàn tất lượt chơi hiện tại trước khi đổi bàn!");
                return;
            }
            SoundHook.playMenuClick();
            switchMode(0);
        });

        tabHighLowBtn = new Button("🎲 ĐOÁN BÀI CAO / THẤP");
        tabHighLowBtn.setFocusTraversable(false);
        lockNoEllipsis(tabHighLowBtn);
        tabHighLowBtn.setPrefWidth(285);
        tabHighLowBtn.setOnAction(e -> {
            if (bjRoundActive || slotSpinning) {
                notifyMsg("Hãy hoàn tất lượt chơi hiện tại trước khi đổi bàn!");
                return;
            }
            SoundHook.playMenuClick();
            switchMode(1);
        });

        tabSlotBtn = new Button("🎰 MÁY XÈNG (SLOT)");
        tabSlotBtn.setFocusTraversable(false);
        lockNoEllipsis(tabSlotBtn);
        tabSlotBtn.setPrefWidth(285);
        tabSlotBtn.setOnAction(e -> {
            if (bjRoundActive || hlRoundActive) {
                notifyMsg("Hãy hoàn tất ván bài hiện tại trước khi qua Máy Xèng!");
                return;
            }
            SoundHook.playMenuClick();
            switchMode(2);
        });

        HBox tabRow = new HBox(14, tabBlackjackBtn, tabHighLowBtn, tabSlotBtn);
        tabRow.setAlignment(Pos.CENTER);
        return tabRow;
    }

    private HBox buildBetRow() {
        Label betTitle = new Label("MỨC CƯỢC:");
        lockNoEllipsis(betTitle);
        betTitle.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFE8A3;");

        HBox chipBox = new HBox(10, betTitle);
        chipBox.setAlignment(Pos.CENTER);
        chipBox.setPadding(new Insets(6, 16, 6, 16));
        chipBox.setStyle(
            "-fx-background-color: rgba(8, 30, 17, 0.72);" +
            "-fx-border-color: rgba(226, 180, 78, 0.45); -fx-border-width: 1.5px;" +
            "-fx-background-radius: 12px; -fx-border-radius: 12px;"
        );

        int[] amounts = {20, 50, 100, 200, 500, -1};
        for (int amt : amounts) {
            String txt = (amt == -1) ? "ALL-IN" : ("$" + amt);
            Button b = new Button(txt);
            b.setFocusTraversable(false);
            lockNoEllipsis(b);
            b.setUserData(amt);
            b.setOnAction(e -> {
                if (bjRoundActive || hlRoundActive || slotSpinning) {
                    notifyMsg("Không thể đổi mức cược khi đang trong lượt chơi!");
                    return;
                }
                selectedBetOption = amt;
                SoundHook.playMenuClick();
                refreshBetButtonStyles();
            });
            betButtons.add(b);
            chipBox.getChildren().add(b);
        }

        return chipBox;
    }

    public void switchMode(int mode) {
        this.activeMode = mode;
        blackjackView.setVisible(mode == 0);
        blackjackView.setManaged(mode == 0);
        highLowView.setVisible(mode == 1);
        highLowView.setManaged(mode == 1);
        slotMachineView.setVisible(mode == 2);
        slotMachineView.setManaged(mode == 2);

        String activeTabStyle =
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-text-fill: #2B1404; -fx-background-color: linear-gradient(to bottom, #FFE066, #E5A92E);" +
            "-fx-border-color: #FFF3B0; -fx-border-width: 2px; -fx-padding: 7 14 7 14;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;";
        String idleTabStyle =
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-text-fill: #DCEDE2; -fx-background-color: rgba(10, 34, 20, 0.78);" +
            "-fx-border-color: #3E8E5E; -fx-border-width: 1.8px; -fx-padding: 7 14 7 14;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;";

        tabBlackjackBtn.setStyle(mode == 0 ? activeTabStyle : idleTabStyle);
        tabHighLowBtn.setStyle(mode == 1 ? activeTabStyle : idleTabStyle);
        tabSlotBtn.setStyle(mode == 2 ? activeTabStyle : idleTabStyle);
    }

    private void refreshBetButtonStyles() {
        for (Button b : betButtons) {
            int val = (Integer) b.getUserData();
            boolean sel = (val == selectedBetOption);
            b.setStyle(
                "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (sel ? "#241102;" : "#FFF6E0;") +
                "-fx-background-color: " + (sel ? "linear-gradient(to bottom, #FFE066, #E09B1B);" : "rgba(22, 62, 38, 0.92);") +
                "-fx-border-color: " + (sel ? "#FFF8CC;" : "#74C692;") +
                "-fx-border-width: 2px; -fx-padding: 6 16 6 16; -fx-background-radius: 18px; -fx-border-radius: 18px; -fx-cursor: hand;"
            );
        }
    }

    private int resolveSelectedBet() {
        if (player == null) return 0;
        int balance = player.getMoney();
        if (selectedBetOption == -1) {
            return balance;
        }
        return selectedBetOption;
    }

    private void refreshMoneyHeader() {
        int m = (player != null) ? player.getMoney() : 0;
        if (moneyBadgeLabel != null) {
            moneyBadgeLabel.setText("💰 Số dư: $" + m);
        }
    }

    // =========================================================================
    // TRÒ 1: XÌ DÁCH (BLACKJACK 21)
    // =========================================================================

    private VBox buildBlackjackPane() {
        bjDealerScoreLabel = new Label("NHÀ CÁI (DEALER): --");
        bjDealerScoreLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFD97D;");

        bjDealerCardsBox = new HBox(10);
        bjDealerCardsBox.setAlignment(Pos.CENTER);
        bjDealerCardsBox.setMinHeight(124);

        bjStatusLabel = new Label("Chọn mức cược phía trên rồi bấm [Chia bài] để bắt đầu! (Xì Dách ăn x2.5 • Thắng ăn x2)");
        bjStatusLabel.setWrapText(true);
        bjStatusLabel.setAlignment(Pos.CENTER);
        bjStatusLabel.setMaxWidth(860);
        bjStatusLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFF8DC;" +
            "-fx-background-color: rgba(8, 28, 16, 0.72); -fx-border-color: rgba(255,215,90,0.45); -fx-border-width: 1.5px;" +
            "-fx-padding: 8 16 8 16; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );

        bjPlayerScoreLabel = new Label("BÀI CỦA BẠN: --");
        bjPlayerScoreLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #98FB98;");

        bjPlayerCardsBox = new HBox(10);
        bjPlayerCardsBox.setAlignment(Pos.CENTER);
        bjPlayerCardsBox.setMinHeight(124);

        bjDealBtn = createActionBtn("🃏 Chia bài", "#E59819", "#B56E08");
        bjHitBtn  = createActionBtn("➕ Rút (Hit)", "#2E9E54", "#1D7039");
        bjStandBtn = createActionBtn("✋ Dằn (Stand)", "#3B7DD8", "#22539A");

        bjDealBtn.setOnAction(e -> startBlackjackRound());
        bjHitBtn.setOnAction(e -> onBlackjackHit());
        bjStandBtn.setOnAction(e -> onBlackjackStand());

        HBox controls = new HBox(16, bjDealBtn, bjHitBtn, bjStandBtn);
        controls.setAlignment(Pos.CENTER);

        VBox box = new VBox(11,
            bjDealerScoreLabel, bjDealerCardsBox,
            bjStatusLabel,
            bjPlayerScoreLabel, bjPlayerCardsBox,
            controls
        );
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle(
            "-fx-background-color: rgba(12, 46, 26, 0.52);" +
            "-fx-border-color: rgba(235, 196, 98, 0.42); -fx-border-width: 2px;" +
            "-fx-background-radius: 14px; -fx-border-radius: 14px;"
        );

        updateBlackjackButtons();
        renderPlaceholderCards(bjDealerCardsBox, 2);
        renderPlaceholderCards(bjPlayerCardsBox, 2);
        return box;
    }

    private void startBlackjackRound() {
        if (bjRoundActive || player == null) return;
        int bet = resolveSelectedBet();
        if (bet <= 0 || player.getMoney() < bet) {
            bjStatusLabel.setText("⚠ Bạn không đủ tiền để đặt mức cược này! Hãy giảm mức cược.");
            return;
        }
        player.deductMoney(bet);
        bjActiveBet = bet;
        bjRoundActive = true;
        refreshMoneyHeader();
        SoundHook.playMenuClick();

        resetAndShuffleDeck(bjDeck);
        bjPlayerHand.clear();
        bjDealerHand.clear();

        // Chia bài ngửa 2 lá cho người chơi và 2 lá cho nhà cái
        bjPlayerHand.add(drawCard(bjDeck));
        bjDealerHand.add(drawCard(bjDeck));
        bjPlayerHand.add(drawCard(bjDeck));
        bjDealerHand.add(drawCard(bjDeck));

        renderHand(bjDealerCardsBox, bjDealerHand);
        renderHand(bjPlayerCardsBox, bjPlayerHand);
        updateBlackjackScores();
        updateBlackjackButtons();

        boolean playerBJ = isNaturalBlackjack(bjPlayerHand);
        boolean dealerBJ = isNaturalBlackjack(bjDealerHand);
        if (playerBJ || dealerBJ) {
            finishBlackjackNatural(playerBJ, dealerBJ);
        } else {
            bjStatusLabel.setText("Đã cược $" + bjActiveBet + "! Chọn [Rút (Hit)] để lấy thêm bài hoặc [Dằn (Stand)] để so điểm.");
        }
    }

    private void onBlackjackHit() {
        if (!bjRoundActive) return;
        SoundHook.playMenuClick();
        bjPlayerHand.add(drawCard(bjDeck));
        renderHand(bjPlayerCardsBox, bjPlayerHand);
        updateBlackjackScores();

        int pScore = scoreBlackjack(bjPlayerHand);
        if (pScore > 21) {
            bjRoundActive = false;
            updateBlackjackButtons();
            bjStatusLabel.setText("💥 QUẮC (" + pScore + " điểm > 21)! Bạn thua $" + bjActiveBet + ". Bấm [Chia bài] để gỡ lại!");
        } else if (pScore == 21) {
            onBlackjackStand();
        } else {
            bjStatusLabel.setText("Điểm hiện tại: " + pScore + ". Bạn muốn [Rút (Hit)] tiếp hay [Dằn (Stand)]?");
        }
    }

    private void onBlackjackStand() {
        if (!bjRoundActive) return;
        SoundHook.playMenuClick();
        while (scoreBlackjack(bjDealerHand) < 17) {
            bjDealerHand.add(drawCard(bjDeck));
        }
        renderHand(bjDealerCardsBox, bjDealerHand);
        updateBlackjackScores();

        int pScore = scoreBlackjack(bjPlayerHand);
        int dScore = scoreBlackjack(bjDealerHand);
        bjRoundActive = false;
        updateBlackjackButtons();

        if (dScore > 21) {
            int payout = bjActiveBet * 2;
            creditWinnings(payout, payout - bjActiveBet);
            bjStatusLabel.setText("🎉 NHÀ CÁI QUẮC (" + dScore + " điểm)! Bạn thắng x2 nhận +$" + payout + " (lời +$" + bjActiveBet + ")!");
        } else if (pScore > dScore) {
            int payout = bjActiveBet * 2;
            creditWinnings(payout, payout - bjActiveBet);
            bjStatusLabel.setText("🎉 CHIẾN THẮNG (" + pScore + " vs " + dScore + ")! Bạn ăn x2 nhận +$" + payout + " (lời +$" + bjActiveBet + ")!");
        } else if (pScore == dScore) {
            player.addMoney(bjActiveBet);
            refreshMoneyHeader();
            bjStatusLabel.setText("🤝 HÒA ĐIỂM (" + pScore + " vs " + dScore + ")! Hoàn lại $" + bjActiveBet + ".");
        } else {
            bjStatusLabel.setText("😢 THUA CUỘC (" + pScore + " vs " + dScore + ")! Nhà cái thắng $" + bjActiveBet + ".");
        }
    }

    private void finishBlackjackNatural(boolean playerBJ, boolean dealerBJ) {
        bjRoundActive = false;
        updateBlackjackButtons();
        if (playerBJ && dealerBJ) {
            player.addMoney(bjActiveBet);
            refreshMoneyHeader();
            bjStatusLabel.setText("🤝 Cả hai cùng XÌ DÁCH 21! Hòa — hoàn tiền $" + bjActiveBet + ".");
        } else if (playerBJ) {
            int payout = (int) Math.round(bjActiveBet * 2.5);
            creditWinnings(payout, payout - bjActiveBet);
            bjStatusLabel.setText("👑 XÌ DÁCH 21 HOÀN HẢO (2 lá)! Ăn thưởng x2.5 nhận +$" + payout + "!");
        } else {
            bjStatusLabel.setText("⚡ Nhà cái có XÌ DÁCH 21 ngay từ đầu! Bạn thua $" + bjActiveBet + ".");
        }
    }

    private void updateBlackjackScores() {
        bjDealerScoreLabel.setText("NHÀ CÁI (DEALER): " + (bjDealerHand.isEmpty() ? "--" : (scoreBlackjack(bjDealerHand) + " điểm")));
        bjPlayerScoreLabel.setText("BÀI CỦA BẠN: " + (bjPlayerHand.isEmpty() ? "--" : (scoreBlackjack(bjPlayerHand) + " điểm")));
    }

    private void updateBlackjackButtons() {
        bjDealBtn.setDisable(bjRoundActive);
        bjHitBtn.setDisable(!bjRoundActive);
        bjStandBtn.setDisable(!bjRoundActive);
        bjDealBtn.setOpacity(bjRoundActive ? 0.50 : 1.0);
        bjHitBtn.setOpacity(!bjRoundActive ? 0.50 : 1.0);
        bjStandBtn.setOpacity(!bjRoundActive ? 0.50 : 1.0);
    }

    private static boolean isNaturalBlackjack(List<Card> hand) {
        return hand.size() == 2 && scoreBlackjack(hand) == 21;
    }

    private static int scoreBlackjack(List<Card> hand) {
        int sum = 0;
        int aces = 0;
        for (Card c : hand) {
            if (c.rankIndex == 0) {
                sum += 11;
                aces++;
            } else if (c.rankIndex >= 9) {
                sum += 10;
            } else {
                sum += (c.rankIndex + 1);
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    // =========================================================================
    // TRÒ 2: ĐOÁN BÀI CAO / THẤP (HIGH - LOW)
    // =========================================================================

    private VBox buildHighLowPane() {
        Label ruleHint = new Label("Thứ tự bài: A(1) < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J(11) < Q(12) < K(13)");
        lockNoEllipsis(ruleHint);
        ruleHint.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #CBEBD6;");

        hlPotLabel = new Label("TIỀN THƯỞNG TÍCH LŨY (POT): $0");
        lockNoEllipsis(hlPotLabel);
        hlPotLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFE066;"
        );

        VBox prevCol = new VBox(6);
        prevCol.setAlignment(Pos.CENTER);
        Label prevLbl = new Label("LÁ TRƯỚC");
        prevLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #B8DFC6;");
        hlPrevCardSlot = new StackPane(createEmptyCardSlot("?"));
        prevCol.getChildren().addAll(prevLbl, hlPrevCardSlot);

        Label arrowLbl = new Label("➡");
        arrowLbl.setStyle("-fx-font-size: 28px; -fx-text-fill: #FFD54F;");

        VBox currCol = new VBox(6);
        currCol.setAlignment(Pos.CENTER);
        Label currLbl = new Label("LÁ HIỆN TẠI");
        currLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FFE885;");
        hlCurrCardSlot = new StackPane(createEmptyCardSlot("🃏"));
        currCol.getChildren().addAll(currLbl, hlCurrCardSlot);

        HBox cardsStage = new HBox(34, prevCol, arrowLbl, currCol);
        cardsStage.setAlignment(Pos.CENTER);
        cardsStage.setMinHeight(156);

        hlStatusLabel = new Label("Chọn mức cược rồi bấm [Đặt cược & Rút bài] để bắt đầu chuỗi nhân đôi!");
        hlStatusLabel.setWrapText(true);
        hlStatusLabel.setAlignment(Pos.CENTER);
        hlStatusLabel.setMaxWidth(860);
        hlStatusLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFF8DC;" +
            "-fx-background-color: rgba(8, 28, 16, 0.72); -fx-border-color: rgba(255,215,90,0.45); -fx-border-width: 1.5px;" +
            "-fx-padding: 8 16 8 16; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );

        hlStartBtn   = createActionBtn("🎲 Đặt cược & Rút bài", "#E59819", "#B56E08");
        hlHigherBtn  = createActionBtn("▲ CAO HƠN (x2)", "#2E9E54", "#1D7039");
        hlLowerBtn   = createActionBtn("▼ THẤP HƠN (x2)", "#D9534F", "#9E2A27");
        hlCashOutBtn = createActionBtn("💰 Nhận Tiền", "#2FA4E7", "#19679E");

        hlStartBtn.setOnAction(e -> startHighLowRound());
        hlHigherBtn.setOnAction(e -> guessHighLow(true));
        hlLowerBtn.setOnAction(e -> guessHighLow(false));
        hlCashOutBtn.setOnAction(e -> cashOutHighLow());

        HBox actions = new HBox(14, hlStartBtn, hlHigherBtn, hlLowerBtn, hlCashOutBtn);
        actions.setAlignment(Pos.CENTER);

        VBox box = new VBox(14, ruleHint, hlPotLabel, cardsStage, hlStatusLabel, actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12, 14, 10, 14));
        box.setStyle(
            "-fx-background-color: rgba(12, 46, 26, 0.52);" +
            "-fx-border-color: rgba(235, 196, 98, 0.42); -fx-border-width: 2px;" +
            "-fx-background-radius: 14px; -fx-border-radius: 14px;"
        );

        updateHighLowButtons();
        return box;
    }

    private void startHighLowRound() {
        if (hlRoundActive || player == null) return;
        int bet = resolveSelectedBet();
        if (bet <= 0 || player.getMoney() < bet) {
            hlStatusLabel.setText("⚠ Bạn không đủ tiền cho mức cược này!");
            return;
        }
        player.deductMoney(bet);
        refreshMoneyHeader();
        SoundHook.playMenuClick();

        resetAndShuffleDeck(hlDeck);
        hlPreviousCard = null;
        hlCurrentCard = drawCard(hlDeck);
        hlInitialBet = bet;
        hlCurrentPot = bet;
        hlStreak = 0;
        hlRoundActive = true;

        hlPrevCardSlot.getChildren().setAll(createEmptyCardSlot("-"));
        hlCurrCardSlot.getChildren().setAll(renderCardNode(hlCurrentCard));
        updateHighLowButtons();
        hlStatusLabel.setText("Lá hiện tại là [" + hlCurrentCard.rankText() + hlCurrentCard.suitText() +
                " = " + hlCurrentCard.highLowValue() + "]. Lá tiếp theo sẽ CAO HƠN hay THẤP HƠN?");
    }

    private void guessHighLow(boolean guessHigher) {
        if (!hlRoundActive || hlCurrentCard == null) return;
        if (hlDeck.isEmpty()) resetAndShuffleDeck(hlDeck);

        hlPreviousCard = hlCurrentCard;
        hlCurrentCard = drawCard(hlDeck);

        hlPrevCardSlot.getChildren().setAll(renderCardNode(hlPreviousCard));
        hlCurrCardSlot.getChildren().setAll(renderCardNode(hlCurrentCard));

        int prevVal = hlPreviousCard.highLowValue();
        int nextVal = hlCurrentCard.highLowValue();

        if (nextVal == prevVal) {
            SoundHook.playMenuClick();
            hlStatusLabel.setText("🤝 BẰNG NHAU (" + hlPreviousCard.rankText() + " = " + hlCurrentCard.rankText() +
                    ")! Giữ nguyên Pot $" + hlCurrentPot + " — đoán tiếp hoặc Nhận Tiền!");
            updateHighLowButtons();
            return;
        }

        boolean win = guessHigher ? (nextVal > prevVal) : (nextVal < prevVal);
        if (win) {
            hlStreak++;
            try {
                hlCurrentPot = Math.multiplyExact(hlCurrentPot, 2);
            } catch (ArithmeticException ex) {
                hlCurrentPot = 1_000_000_000;
            }
            SoundHook.playPickup();
            hlStatusLabel.setText("🔥 CHÍNH XÁC! Chuỗi thắng x" + hlStreak + " — Pot tăng lên $" + hlCurrentPot +
                    "! Bấm [Nhận Tiền] để bỏ túi hoặc đoán tiếp để x2!");
            updateHighLowButtons();
        } else {
            hlCurrentPot = 0;
            hlRoundActive = false;
            SoundHook.playMenuClick();
            updateHighLowButtons();
            hlStatusLabel.setText("💥 ĐOÁN SAI (" + hlPreviousCard.rankText() + " ➡ " + hlCurrentCard.rankText() +
                    ")! Mất mức cược $" + hlInitialBet + ". Bấm [Đặt cược & Rút bài] để thử lại!");
        }
    }

    private void cashOutHighLow() {
        if (!hlRoundActive || hlCurrentPot <= 0) return;
        int payout = hlCurrentPot;
        int netProfit = Math.max(0, payout - hlInitialBet);
        hlCurrentPot = 0;
        hlRoundActive = false;
        creditWinnings(payout, netProfit);
        updateHighLowButtons();
        hlStatusLabel.setText("💰 ĐÃ THU VỀ $" + payout + " (lời +$" + netProfit + ")! Bấm [Đặt cược & Rút bài] để chơi ván mới.");
    }

    private void updateHighLowButtons() {
        hlPotLabel.setText("TIỀN THƯỞNG TÍCH LŨY (POT): $" + hlCurrentPot +
                (hlStreak > 0 ? ("  •  Chuỗi thắng: " + hlStreak) : ""));
        hlStartBtn.setDisable(hlRoundActive);
        hlHigherBtn.setDisable(!hlRoundActive);
        hlLowerBtn.setDisable(!hlRoundActive);
        boolean canCash = hlRoundActive && hlStreak >= 1;
        hlCashOutBtn.setDisable(!canCash);
        hlCashOutBtn.setText(canCash ? ("💰 Nhận $" + hlCurrentPot) : "💰 Nhận Tiền");

        hlStartBtn.setOpacity(hlRoundActive ? 0.50 : 1.0);
        hlHigherBtn.setOpacity(!hlRoundActive ? 0.50 : 1.0);
        hlLowerBtn.setOpacity(!hlRoundActive ? 0.50 : 1.0);
        hlCashOutBtn.setOpacity(!canCash ? 0.50 : 1.0);
    }

    // =========================================================================
    // TRÒ 3: MÁY XÈNG 3 CUỘN QUAY (3-REEL SLOT MACHINE)
    // =========================================================================

    private VBox buildSlotMachinePane() {
        Label banner = new Label("✨ ROYAL JACKPOT 777 • QUAY 3 CUỘN SĂN NỔ HŨ x15 ✨");
        lockNoEllipsis(banner);
        banner.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFE066;"
        );

        // Khung máy xèng viền vàng kim sang trọng ở giữa bàn nỉ xanh
        HBox reelsRow = new HBox(20);
        reelsRow.setAlignment(Pos.CENTER);
        reelsRow.setPadding(new Insets(14, 26, 14, 26));
        reelsRow.setMaxWidth(520);
        reelsRow.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #5C161E 0%, #380B10 100%);" +
            "-fx-border-color: #FFD54F #E09B1B #B87708 #FFD54F;" +
            "-fx-border-width: 4.5px;" +
            "-fx-background-radius: 16px; -fx-border-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.62), 14, 0.25, 0, 4);"
        );

        SlotSymbol[] initSyms = {SlotSymbol.LUCKY_SEVEN, SlotSymbol.DIAMOND, SlotSymbol.LUCKY_SEVEN};
        for (int i = 0; i < 3; i++) {
            ImageView iv = new ImageView(slotSymbolImages[initSyms[i].ordinal()]);
            iv.setFitWidth(58);
            iv.setFitHeight(58);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);

            Label symLbl = new Label(initSyms[i].viLabel);
            lockNoEllipsis(symLbl);
            symLbl.setStyle(
                "-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4E2A0C;"
            );

            VBox reelWindow = new VBox(6, iv, symLbl);
            reelWindow.setAlignment(Pos.CENTER);
            reelWindow.setMinSize(128, 118);
            reelWindow.setPrefSize(128, 118);
            reelWindow.setMaxSize(128, 118);
            styleSlotReelBox(reelWindow, false);

            slotReelBoxes[i] = reelWindow;
            slotReelIcons[i] = iv;
            slotReelNames[i] = symLbl;
            reelsRow.getChildren().add(reelWindow);
        }

        slotStatusLabel = new Label("Chọn mức cược phía trên rồi bấm [🎰 QUAY NGAY (SPIN)] để thử vận may!");
        slotStatusLabel.setWrapText(true);
        slotStatusLabel.setAlignment(Pos.CENTER);
        slotStatusLabel.setMaxWidth(860);
        styleSlotStatusDefault();

        slotSpinBtn = new Button("🎰 QUAY NGAY (SPIN)");
        slotSpinBtn.setFocusTraversable(false);
        lockNoEllipsis(slotSpinBtn);
        slotSpinBtn.setPrefHeight(44);
        slotSpinBtn.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 15.5px; -fx-font-weight: bold; -fx-text-fill: #FFFDF2;" +
            "-fx-padding: 8 30 8 30;" +
            "-fx-background-color: linear-gradient(to bottom, #FF5252 0%, #C62828 100%);" +
            "-fx-border-color: #FFE066; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 22px; -fx-border-radius: 22px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 10, 0.25, 0, 3);"
        );
        slotSpinBtn.setOnAction(e -> startSlotSpin());

        // Bảng Trả Thưởng (Payout Table) gọn đẹp bên dưới
        HBox payoutTable = buildSlotPayoutTable();

        VBox box = new VBox(11, banner, reelsRow, slotStatusLabel, slotSpinBtn, payoutTable);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle(
            "-fx-background-color: rgba(12, 46, 26, 0.52);" +
            "-fx-border-color: rgba(235, 196, 98, 0.42); -fx-border-width: 2px;" +
            "-fx-background-radius: 14px; -fx-border-radius: 14px;"
        );
        return box;
    }

    private HBox buildSlotPayoutTable() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(6, 12, 6, 12));
        row.setStyle(
            "-fx-background-color: rgba(8, 28, 16, 0.80);" +
            "-fx-border-color: rgba(255, 214, 102, 0.40); -fx-border-width: 1.4px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        row.getChildren().addAll(
            createPayoutBadge(slotSymbolImages[SlotSymbol.LUCKY_SEVEN.ordinal()], "3 Ô SỐ 7: x15 (JACKPOT)", "#FFD54F"),
            createPayoutBadge(slotSymbolImages[SlotSymbol.DIAMOND.ordinal()],     "3 KIM CƯƠNG / TIỀN: x8",  "#80DEEA"),
            createPayoutBadge(slotSymbolImages[SlotSymbol.TOMATO.ordinal()],      "3 NÔNG SẢN / CÁ: x4",     "#A5D6A7"),
            createPayoutBadge(slotSymbolImages[SlotSymbol.MONEY_BAG.ordinal()],   "2 Ô GIỐNG NHAU: x1.5",    "#FFE0B2")
        );
        return row;
    }

    private HBox createPayoutBadge(Image iconImg, String text, String textHex) {
        ImageView iv = new ImageView(iconImg);
        iv.setFitWidth(20);
        iv.setFitHeight(20);
        iv.setPreserveRatio(true);
        iv.setSmooth(false);

        Label lbl = new Label(text);
        lockNoEllipsis(lbl);
        lbl.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + textHex + ";"
        );

        HBox badge = new HBox(6, iv, lbl);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(4, 9, 4, 9));
        badge.setStyle(
            "-fx-background-color: rgba(22, 58, 34, 0.85);" +
            "-fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 1px;" +
            "-fx-background-radius: 7px; -fx-border-radius: 7px;"
        );
        return badge;
    }

    private void styleSlotReelBox(VBox reelBox, boolean highlightGold) {
        reelBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #FFFDF5 0%, #F5E6C8 100%);" +
            "-fx-border-color: " + (highlightGold ? "#FFEA00;" : "#C78D2B;") +
            "-fx-border-width: " + (highlightGold ? "3.5px;" : "2.5px;") +
            "-fx-background-radius: 12px; -fx-border-radius: 11px;" +
            "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.22), 7, 0.1, 0, 2);"
        );
    }

    private void styleSlotStatusDefault() {
        slotStatusLabel.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFF8DC;" +
            "-fx-background-color: rgba(8, 28, 16, 0.72); -fx-border-color: rgba(255,215,90,0.45); -fx-border-width: 1.5px;" +
            "-fx-padding: 7 16 7 16; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
    }

    private void startSlotSpin() {
        if (slotSpinning || player == null) return;
        int bet = resolveSelectedBet();
        if (bet <= 0 || player.getMoney() < bet) {
            styleSlotStatusDefault();
            slotStatusLabel.setText("⚠ Không đủ tiền cho mức cược này! Hãy chọn mức cược nhỏ hơn.");
            return;
        }

        if (jackpotBlinkTimeline != null) {
            jackpotBlinkTimeline.stop();
            jackpotBlinkTimeline = null;
        }
        for (VBox rb : slotReelBoxes) {
            styleSlotReelBox(rb, false);
            rb.setTranslateY(0);
        }
        styleSlotStatusDefault();

        // Trừ tiền cược ngay khi bấm QUAY
        player.deductMoney(bet);
        slotActiveBet = bet;
        refreshMoneyHeader();
        SoundHook.playMenuClick();

        // Quyết định kết quả ngẫu nhiên cho 3 cuộn
        for (int i = 0; i < 3; i++) {
            slotFinalSymbols[i] = WEIGHTED_REEL_STRIP[slotRng.nextInt(WEIGHTED_REEL_STRIP.length)];
        }

        slotSpinning = true;
        slotElapsedSec = 0.0;
        reel0Stopped = false;
        reel1Stopped = false;
        slotSpinBtn.setDisable(true);
        slotSpinBtn.setOpacity(0.55);
        slotStatusLabel.setText("🎰 Đang quay với mức cược $" + slotActiveBet + " ... Chờ các cuộn dừng lại!");

        slotSpinTimeline = new Timeline(new KeyFrame(Duration.millis(60), ev -> tickSlotSpin()));
        slotSpinTimeline.setCycleCount(Animation.INDEFINITE);
        slotSpinTimeline.play();
    }

    private void tickSlotSpin() {
        slotElapsedSec += 0.06;

        // Cuộn 1 dừng sau 0.8s
        if (!reel0Stopped) {
            if (slotElapsedSec >= 0.80) {
                reel0Stopped = true;
                setReelDisplay(0, slotFinalSymbols[0]);
                slotReelBoxes[0].setTranslateY(0);
                styleSlotReelBox(slotReelBoxes[0], true);
                SoundHook.playMenuClick();
            } else {
                setReelDisplay(0, WEIGHTED_REEL_STRIP[slotRng.nextInt(WEIGHTED_REEL_STRIP.length)]);
                slotReelBoxes[0].setTranslateY((slotRng.nextBoolean() ? 1 : -1) * 3.0);
            }
        }

        // Cuộn 2 dừng sau 1.3s
        if (!reel1Stopped) {
            if (slotElapsedSec >= 1.30) {
                reel1Stopped = true;
                setReelDisplay(1, slotFinalSymbols[1]);
                slotReelBoxes[1].setTranslateY(0);
                styleSlotReelBox(slotReelBoxes[1], true);
                SoundHook.playMenuClick();
            } else {
                setReelDisplay(1, WEIGHTED_REEL_STRIP[slotRng.nextInt(WEIGHTED_REEL_STRIP.length)]);
                slotReelBoxes[1].setTranslateY((slotRng.nextBoolean() ? 1 : -1) * 3.0);
            }
        }

        // Cuộn 3 dừng sau 1.9s -> kết thúc & tính thưởng
        if (slotElapsedSec >= 1.90) {
            if (slotSpinTimeline != null) {
                slotSpinTimeline.stop();
                slotSpinTimeline = null;
            }
            setReelDisplay(2, slotFinalSymbols[2]);
            slotReelBoxes[2].setTranslateY(0);
            styleSlotReelBox(slotReelBoxes[2], true);

            slotSpinning = false;
            slotSpinBtn.setDisable(false);
            slotSpinBtn.setOpacity(1.0);
            evaluateSlotResult();
        } else {
            setReelDisplay(2, WEIGHTED_REEL_STRIP[slotRng.nextInt(WEIGHTED_REEL_STRIP.length)]);
            slotReelBoxes[2].setTranslateY((slotRng.nextBoolean() ? 1 : -1) * 3.0);
        }
    }

    private void setReelDisplay(int reelIdx, SlotSymbol sym) {
        slotReelIcons[reelIdx].setImage(slotSymbolImages[sym.ordinal()]);
        slotReelNames[reelIdx].setText(sym.viLabel);
    }

    private void evaluateSlotResult() {
        SlotSymbol s0 = slotFinalSymbols[0];
        SlotSymbol s1 = slotFinalSymbols[1];
        SlotSymbol s2 = slotFinalSymbols[2];

        boolean allThreeSame = (s0 == s1 && s1 == s2);
        boolean twoSame = !allThreeSame && (s0 == s1 || s1 == s2 || s0 == s2);

        if (allThreeSame && s0 == SlotSymbol.LUCKY_SEVEN) {
            // JACKPOT 7-7-7 -> x15
            int payout = slotActiveBet * 15;
            creditWinnings(payout, payout - slotActiveBet);
            startJackpotFlashAnimation(payout);
        } else if (allThreeSame && (s0 == SlotSymbol.DIAMOND || s0 == SlotSymbol.MONEY_BAG)) {
            // 3 Kim cương hoặc 3 Túi tiền -> x8
            int payout = slotActiveBet * 8;
            creditWinnings(payout, payout - slotActiveBet);
            slotStatusLabel.setText("💎 THẮNG LỚN x8 (" + s0.viLabel + ")! Nhận ngay +$" + payout + " (lời +$" + (payout - slotActiveBet) + ")!");
        } else if (allThreeSame) {
            // 3 Nông sản / Cá -> x4
            int payout = slotActiveBet * 4;
            creditWinnings(payout, payout - slotActiveBet);
            slotStatusLabel.setText("🎉 TRÚNG BỘ BA " + s0.viLabel + " (x4)! Nhận +$" + payout + " (lời +$" + (payout - slotActiveBet) + ")!");
        } else if (twoSame) {
            // 2 ô giống nhau bất kỳ -> x1.5
            int payout = (int) Math.round(slotActiveBet * 1.5);
            creditWinnings(payout, Math.max(0, payout - slotActiveBet));
            slotStatusLabel.setText("✨ TRÚNG CẶP ĐÔI (x1.5)! Nhận +$" + payout + " (lời +$" + (payout - slotActiveBet) + ")!");
        } else {
            SoundHook.playMenuClick();
            for (VBox rb : slotReelBoxes) styleSlotReelBox(rb, false);
            slotStatusLabel.setText("🎲 3 ô khác nhau — Chưa trúng lượt này (-$" + slotActiveBet + "). Bấm [QUAY NGAY] để thử lại!");
        }
    }

    private void startJackpotFlashAnimation(int payout) {
        final boolean[] goldPhase = {true};
        slotStatusLabel.setText("🔥 NỔ HŨ JACKPOT! 7 - 7 - 7 ĂN x15 NHẬN +$" + payout + "! 🔥");
        jackpotBlinkTimeline = new Timeline(new KeyFrame(Duration.millis(220), ev -> {
            goldPhase[0] = !goldPhase[0];
            slotStatusLabel.setStyle(
                "-fx-font-family: 'Consolas', monospace; -fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (goldPhase[0] ? "#2B1000;" : "#FFF8B8;") +
                "-fx-background-color: " + (goldPhase[0]
                        ? "linear-gradient(to bottom, #FFE666, #FFA000);"
                        : "linear-gradient(to bottom, #D50000, #8E0000);") +
                "-fx-border-color: #FFFDE7; -fx-border-width: 2.5px;" +
                "-fx-padding: 7 16 7 16; -fx-background-radius: 8px; -fx-border-radius: 8px;"
            );
        }));
        jackpotBlinkTimeline.setCycleCount(18);
        jackpotBlinkTimeline.play();
    }

    /** Khởi tạo bộ 6 icon pixel-art sắc nét cho máy xèng (không dùng emoji lỗi ô vuông trên Windows). */
    private void initSlotSymbolArt() {
        Image tomatoAsset = (Assets.TOMATO_STAGES != null && Assets.TOMATO_STAGES[3] != null)
                ? Assets.TOMATO_STAGES[3] : Assets.getShopIcon("tomato");
        Image cornAsset = (Assets.CORN_STAGES != null && Assets.CORN_STAGES[3] != null)
                ? Assets.CORN_STAGES[3] : Assets.getShopIcon("corn");
        Image fishAsset = Assets.FISH_ICON;

        slotSymbolImages[SlotSymbol.TOMATO.ordinal()] = (tomatoAsset != null) ? tomatoAsset : drawSlotPixelArt(new String[] {
            "......GGGG......",
            "....GGGGGGGG....",
            "...ooRRRRRRoo...",
            "..oRRRRRRRRRRo..",
            ".oRRRLLRRRRRRRo.",
            ".oRRLLRRRRRRRRo.",
            ".oRRRRRRRRRRRRo.",
            ".oRRRRRRRRRRDDo.",
            "..oRRRRRRRRDDo..",
            "...ooRRRRDDoo...",
            ".....oooooo....."
        }, ch -> ch == 'G' ? Color.web("#2E7D32") : ch == 'o' ? Color.web("#3E1414")
               : ch == 'R' ? Color.web("#E53935") : ch == 'L' ? Color.web("#FF8A80")
               : ch == 'D' ? Color.web("#B71C1C") : null);

        slotSymbolImages[SlotSymbol.CORN.ordinal()] = (cornAsset != null) ? cornAsset : drawSlotPixelArt(new String[] {
            ".....oooo.......",
            "....oYYYYo......",
            "...oYYLLYYo.....",
            "...oYYYYYYo.....",
            "..GoYYYYYYoG....",
            ".GGoYYYYYYoGG...",
            ".GGGoYYYYoGGG...",
            "..GGGoYYoGGG....",
            "...GGGGGGGG.....",
            "....GGGGGG......"
        }, ch -> ch == 'o' ? Color.web("#4E342E") : ch == 'Y' ? Color.web("#FDD835")
               : ch == 'L' ? Color.web("#FFF59D") : ch == 'G' ? Color.web("#43A047") : null);

        slotSymbolImages[SlotSymbol.FISH.ordinal()] = (fishAsset != null) ? fishAsset : drawSlotPixelArt(new String[] {
            ".....oooooo.....",
            "...ooBBBBBBoo...",
            "..oBBLLLLLLBBo..",
            ".oBBLWKBBBBLLBo.",
            "oBBBBKKBBBBLLLBo",
            ".oBBBBBBBBLLLBo.",
            "..ooBBBBBBLLoo..",
            "....oooooooo...."
        }, ch -> ch == 'o' ? Color.web("#0D3B66") : ch == 'B' ? Color.web("#29B6F6")
               : ch == 'L' ? Color.web("#B3E5FC") : ch == 'W' ? Color.WHITE
               : ch == 'K' ? Color.web("#102027") : null);

        // Túi Tiền Vàng ($)
        slotSymbolImages[SlotSymbol.MONEY_BAG.ordinal()] = drawSlotPixelArt(new String[] {
            "....oooooooo....",
            "...oYYRRRRYYo...",
            "....ooRRRRoo....",
            "...oYYYYYYYYo...",
            "..oYYLLYYYYYYo..",
            ".oYYLYYSSYYYYYo.",
            ".oYYYYSSSSYYYYo.",
            ".oYYYYSSYYYYYYo.",
            ".oYYYYSSSSYYYYo.",
            ".oYYYYYYSSYYYYo.",
            "..oYYYYSSYYYYo..",
            "...oYYYYYYYYo...",
            "....oooooooo...."
        }, ch -> ch == 'o' ? Color.web("#3E2723") : ch == 'R' ? Color.web("#D32F2F")
               : ch == 'Y' ? Color.web("#FFB300") : ch == 'L' ? Color.web("#FFE57F")
               : ch == 'S' ? Color.web("#5D4037") : null);

        // Kim Cương Xanh Ngọc Lấp Lánh
        slotSymbolImages[SlotSymbol.DIAMOND.ordinal()] = drawSlotPixelArt(new String[] {
            "...oooooooooo...",
            "..oWWWWWWWWWWo..",
            ".oWWCCCCCCCCLLo.",
            "oWWCCWWWWCCCCDDo",
            "oCCCCCCCCCCCCDDo",
            ".oCCCCWWWWCCDDo.",
            "..oCCCCCCCCDDo..",
            "...oCCCCCCCDo...",
            "....oCCCCCDo....",
            ".....oCCCDo.....",
            "......oCDo......",
            ".......oo......."
        }, ch -> ch == 'o' ? Color.web("#004D60") : ch == 'W' ? Color.web("#E0FFFF")
               : ch == 'C' ? Color.web("#00E5FF") : ch == 'L' ? Color.web("#84FFFF")
               : ch == 'D' ? Color.web("#0097A7") : null);

        // Số 7 Đỏ Viền Vàng Hoàng Gia (Jackpot)
        slotSymbolImages[SlotSymbol.LUCKY_SEVEN.ordinal()] = drawSlotPixelArt(new String[] {
            ".oooooooooooooo.",
            "oGGGGGGGGGGGGGG0",
            "oGLLLLLLLLLLLLGo",
            "oGLRRRRRRRRRRLGo",
            "oGLRGGGGGGGGRLGo",
            "oooo......oGRLGo",
            ".........oGRLGo.",
            "........oGRLGo..",
            ".......oGRLGo...",
            "......oGRLGo....",
            ".....oGRLGo.....",
            ".....oGGGGo.....",
            ".....oooooo....."
        }, ch -> (ch == 'o' || ch == '0') ? Color.web("#3B0A0A")
               : ch == 'G' ? Color.web("#FFD54F")
               : ch == 'L' ? Color.web("#FFF59D")
               : ch == 'R' ? Color.web("#E51C23") : null);
    }

    private static Image drawSlotPixelArt(String[] rows, java.util.function.Function<Character, Color> palette) {
        int scale = 4;
        int h = rows.length;
        int w = rows[0].length();
        WritableImage img = new WritableImage(w * scale, h * scale);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                Color col = palette.apply(row.charAt(x));
                if (col != null) {
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            pw.setColor(x * scale + dx, y * scale + dy, col);
                        }
                    }
                }
            }
        }
        return img;
    }

    // =========================================================================
    // HELPERS: CHIA BÀI, VẼ LÁ BÀI & ĐÓNG BẢNG AN TOÀN
    // =========================================================================

    private void creditWinnings(int totalPayout, int netProfit) {
        if (player != null && totalPayout > 0) {
            player.addMoney(totalPayout);
        }
        if (netProfit > 0 && onNetProfit != null) {
            onNetProfit.accept(netProfit);
        }
        SoundHook.playPickup();
        refreshMoneyHeader();
    }

    public void requestSafeClose() {
        if (slotSpinTimeline != null) {
            slotSpinTimeline.stop();
            slotSpinTimeline = null;
        }
        if (jackpotBlinkTimeline != null) {
            jackpotBlinkTimeline.stop();
            jackpotBlinkTimeline = null;
        }
        if (slotSpinning) {
            // Nếu đóng bảng ngay giữa lúc máy đang quay -> hoàn lại tiền cược lượt đó để không mất oan
            slotSpinning = false;
            if (player != null && slotActiveBet > 0) {
                player.addMoney(slotActiveBet);
                notifyMsg("Đã hoàn lại $" + slotActiveBet + " tiền cược lượt quay chưa kết thúc!");
            }
        }
        // Nếu người chơi đang có Pot thắng ở bàn High-Low mà đóng sòng bạc -> tự thu tiền về ví để không mất oan
        if (hlRoundActive && hlCurrentPot > 0) {
            int payout = hlCurrentPot;
            int netProfit = Math.max(0, payout - hlInitialBet);
            hlCurrentPot = 0;
            hlRoundActive = false;
            if (player != null) player.addMoney(payout);
            if (netProfit > 0 && onNetProfit != null) onNetProfit.accept(netProfit);
            notifyMsg("Sòng bạc tự động thanh toán Pot $" + payout + " vào ví của bạn!");
        }
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private void notifyMsg(String msg) {
        if (notifier != null && msg != null) notifier.accept(msg);
    }

    private static void resetAndShuffleDeck(List<Card> deck) {
        deck.clear();
        for (int s = 0; s < 4; s++) {
            for (int r = 0; r < 13; r++) {
                deck.add(new Card(r, s));
            }
        }
        Collections.shuffle(deck);
    }

    private static Card drawCard(List<Card> deck) {
        if (deck.isEmpty()) resetAndShuffleDeck(deck);
        return deck.remove(deck.size() - 1);
    }

    private static void lockNoEllipsis(javafx.scene.control.Labeled node) {
        node.setMinWidth(Region.USE_PREF_SIZE);
        node.setTextOverrun(OverrunStyle.CLIP);
    }

    private static Button createActionBtn(String text, String topHex, String botHex) {
        Button b = new Button(text);
        b.setFocusTraversable(false);
        lockNoEllipsis(b);
        b.setPrefHeight(42);
        b.setStyle(
            "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFFDF5;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-background-color: linear-gradient(to bottom, " + topHex + ", " + botHex + ");" +
            "-fx-border-color: rgba(255,255,255,0.55); -fx-border-width: 2px;" +
            "-fx-background-radius: 9px; -fx-border-radius: 9px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 6, 0.2, 0, 2);"
        );
        return b;
    }

    private static void renderHand(HBox box, List<Card> hand) {
        box.getChildren().clear();
        for (Card c : hand) {
            box.getChildren().add(renderCardNode(c));
        }
    }

    private static void renderPlaceholderCards(HBox box, int count) {
        box.getChildren().clear();
        for (int i = 0; i < count; i++) {
            box.getChildren().add(createEmptyCardSlot("♠"));
        }
    }

    private static StackPane createEmptyCardSlot(String symbol) {
        Rectangle bg = new Rectangle(82, 114);
        bg.setArcWidth(12);
        bg.setArcHeight(12);
        bg.setFill(Color.rgb(10, 38, 22, 0.65));
        bg.setStroke(Color.rgb(168, 218, 184, 0.45));
        bg.setStrokeWidth(2.0);
        bg.getStrokeDashArray().addAll(6.0, 5.0);

        Label sym = new Label(symbol);
        sym.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 26px; -fx-text-fill: rgba(210,245,220,0.45);");
        return new StackPane(bg, sym);
    }

    private static StackPane renderCardNode(Card c) {
        Rectangle bg = new Rectangle(82, 114);
        bg.setArcWidth(12);
        bg.setArcHeight(12);
        bg.setFill(Color.web("#FFFDF7"));
        bg.setStroke(Color.web("#2B1D0E"));
        bg.setStrokeWidth(2.4);

        String colHex = c.isRed() ? "#D62828" : "#1B263B";

        Label topCorner = new Label(c.rankText() + "\n" + c.suitText());
        lockNoEllipsis(topCorner);
        topCorner.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-line-spacing: -4px; -fx-text-fill: " + colHex + ";"
        );
        StackPane.setAlignment(topCorner, Pos.TOP_LEFT);
        StackPane.setMargin(topCorner, new Insets(5, 0, 0, 7));

        Label centerSuit = new Label(c.suitText());
        lockNoEllipsis(centerSuit);
        centerSuit.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + colHex + ";");

        Label botCorner = new Label(c.rankText());
        lockNoEllipsis(botCorner);
        botCorner.setStyle(
            "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colHex + ";"
        );
        StackPane.setAlignment(botCorner, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(botCorner, new Insets(0, 8, 5, 0));

        StackPane cardPane = new StackPane(bg, topCorner, centerSuit, botCorner);
        cardPane.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 8, 0.2, 0, 3);");
        return cardPane;
    }
}
