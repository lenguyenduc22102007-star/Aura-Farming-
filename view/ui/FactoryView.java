package view.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import model.factory.FactoryManager;
import model.factory.FactoryManager.LineRecipe;
import model.factory.FactoryManager.ProductionLine;
import model.factory.MarketManager;
import model.game_item.AnimalProductItem;
import model.game_item.FactoryProductItem;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.player_inventory.Player;
import view.assets.Assets;
import view.fx.SoundHook;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Màn hình Xưởng Chế Biến Nông Sản (FactoryView) đồng bộ phong cách Gỗ Ấm & Giấy Kem
 * (Stardew / Smart Farm Cozy Theme) với toàn bộ hệ thống UI của dự án:
 * - Khung gỗ sồi nâu ấm (#7E4E2B -> #5B351A, viền #2E1608) & nền giấy kem (#FBF3DF / #F4E6C8).
 * - Trái: Kho nguyên liệu túi đồ (click trực tiếp để tự chọn dây chuyền + nạp nhanh +1).
 * - Giữa: 3 dây chuyền chế biến hoạt ảnh 60 FPS (Phễu -> Máy ép/xay -> Khay Ra Lò), nút Nạp +1 / +5 / Nạp Hết & Nâng cấp tốc độ.
 * - Phải: Kho thành phẩm đầu ra (gắn nhãn +50% HOT từ MarketManager) + Xuất bán tất cả + Cất vào túi đồ.
 */
public class FactoryView extends StackPane {

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    private final Player player;
    private final FactoryManager factoryManager;
    private final MarketManager marketManager;
    private final IntConsumer onMoneyEarnedQuestHook;
    private final Consumer<String> onToast;
    private final Runnable onExitFactory;

    private final Label balanceLabel = new Label();
    private final Label hotMarketBanner = new Label();
    private final Label factoryStatusToast = new Label(
        "💡 Mẹo: Click trực tiếp vào nguyên liệu ở cột trái, bấm [Nạp +1 / +5 / Hết], hoặc click vào băng chuyền để chạy máy!"
    );
    private final VBox rawInventoryBox = new VBox(8);
    private final VBox outputWarehouseBox = new VBox(8);
    private final Button sellAllOutputBtn = new Button();
    private final Button collectToBagBtn = new Button("🎒 Cất tất cả vào túi đồ");

    private final LineUiWidgets[] lineWidgets = new LineUiWidgets[3];
    private AnimationTimer conveyorTimer;
    private long lastTickNanos = 0L;
    private double beltScrollPx = 0.0;
    private int lastOutputSnapshotHash = -999;

    private static final class LineUiWidgets {
        Circle statusLed;
        Label statusText;
        Label queueBadge;
        Canvas beltCanvas;
        ProgressBar machineBar;
        Label timerLabel;
        Button upgradeBtn;
        HBox recipeSelectRow;
    }

    public FactoryView(Player player,
                       FactoryManager factoryManager,
                       MarketManager marketManager,
                       IntConsumer onMoneyEarnedQuestHook,
                       Consumer<String> onToast,
                       Runnable onExitFactory) {
        this.player = player;
        this.factoryManager = factoryManager;
        this.marketManager = marketManager;
        this.onMoneyEarnedQuestHook = onMoneyEarnedQuestHook;
        this.onToast = onToast;
        this.onExitFactory = onExitFactory;

        // Nền phủ mờ nâu ấm giống GameBookOverlay & CookingCraftView
        setStyle("-fx-background-color: rgba(18, 11, 6, 0.72);");
        setPadding(new Insets(14, 18, 14, 18));
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                requestExit();
            }
        });

        // Khung gỗ sồi ấm đồng bộ với toàn bộ Smart Farm
        VBox woodenFrame = new VBox(10);
        woodenFrame.setMaxSize(1240, 690);
        woodenFrame.setPrefSize(1240, 690);
        woodenFrame.setPadding(new Insets(14, 18, 14, 18));
        woodenFrame.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #7E4E2B 0%, #5B351A 100%);" +
            "-fx-border-color: #2E1608;" +
            "-fx-border-width: 5px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 24, 0.25, 0, 8);"
        );

        // ==================== 1. THANH TIÊU ĐỀ GỖ ẤM ====================
        HBox topBar = buildCozyHeader();

        // Thanh nẹp đồng-gỗ trang trí (thay cho sọc vàng-đen lạnh lẽo)
        Canvas trimBar = new Canvas(1196, 8);
        drawWoodenBrassTrim(trimBar.getGraphicsContext2D(), 1196, 8);

        // Thanh thông báo trạng thái ngay trong Xưởng
        factoryStatusToast.setMaxWidth(Double.MAX_VALUE);
        factoryStatusToast.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #4A2810; -fx-background-color: #FFF7E0;" +
            "-fx-border-color: #C88A3B; -fx-border-width: 1.5px;" +
            "-fx-padding: 6 12 6 12; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );

        // ==================== 2. KHU VỰC 3 CỘT GIẤY KEM ====================
        HBox bodyRow = new HBox(12);
        bodyRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(bodyRow, Priority.ALWAYS);

        VBox leftCol = buildRawMaterialColumn();
        VBox centerCol = buildConveyorCenterColumn();
        VBox rightCol = buildOutputWarehouseColumn();
        HBox.setHgrow(centerCol, Priority.ALWAYS);

        bodyRow.getChildren().addAll(leftCol, centerCol, rightCol);

        woodenFrame.getChildren().addAll(topBar, trimBar, factoryStatusToast, bodyRow);
        getChildren().add(woodenFrame);

        refreshStaticPanels();
        startConveyorLoop();
    }

    private HBox buildCozyHeader() {
        Label title = new Label("🏡 XƯỞNG CHẾ BIẾN NÔNG SẢN TỰ ĐỘNG");
        lockLabel(title);
        title.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 18px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FFF6DC; -fx-effect: dropshadow(one-pass-box, #2B1406, 2, 1.0, 0, 2);"
        );

        hotMarketBanner.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-text-fill: #7C2D12; -fx-background-color: linear-gradient(to bottom, #FDE68A, #F59E0B);" +
            "-fx-border-color: #92400E; -fx-border-width: 1.5px;" +
            "-fx-padding: 5 10 5 10; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
        lockLabel(hotMarketBanner);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        balanceLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FEF3C7; -fx-background-color: #3D210E;" +
            "-fx-border-color: #D99B38; -fx-border-width: 2px;" +
            "-fx-padding: 5 12 5 12; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
        lockLabel(balanceLabel);

        Button exitBtn = new Button("⬅ Rời Xưởng (ESC)");
        lockButton(exitBtn);
        exitBtn.setPrefHeight(34);
        exitBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FFFBEB; -fx-background-color: linear-gradient(to bottom, #D9534F, #A93226);" +
            "-fx-border-color: #5E1914; -fx-border-width: 2px;" +
            "-fx-padding: 0 13 0 13; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        exitBtn.setOnAction(e -> requestExit());

        HBox row = new HBox(10, title, hotMarketBanner, spacer, balanceLabel, exitBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildRawMaterialColumn() {
        VBox col = new VBox(8);
        col.setPrefWidth(235);
        col.setMinWidth(225);
        col.setPadding(new Insets(10));
        col.setStyle(
            "-fx-background-color: #FBF3DF;" +
            "-fx-border-color: #8C582B; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        Label hdr = new Label("🌾 KHO NGUYÊN LIỆU");
        hdr.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4A2810;");
        Label sub = new Label("Click nguyên liệu để nạp nhanh +1 vào đúng dây chuyền");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-text-fill: #7A5033;");

        rawInventoryBox.setPadding(new Insets(4));
        ScrollPane sp = new ScrollPane(rawInventoryBox);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: #F2E2C1; -fx-background-color: #F2E2C1; -fx-border-color: #C49A6C; -fx-border-radius: 7px;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        col.getChildren().addAll(hdr, sub, sp);
        return col;
    }

    private VBox buildConveyorCenterColumn() {
        VBox col = new VBox(9);
        col.setAlignment(Pos.TOP_CENTER);

        List<ProductionLine> lines = factoryManager.getLines();
        for (int i = 0; i < lines.size(); i++) {
            VBox card = buildSingleLineCard(lines.get(i), i);
            VBox.setVgrow(card, Priority.ALWAYS);
            col.getChildren().add(card);
        }
        return col;
    }

    private VBox buildSingleLineCard(ProductionLine line, int idx) {
        LineUiWidgets w = new LineUiWidgets();
        lineWidgets[idx] = w;

        VBox card = new VBox(6);
        card.setPadding(new Insets(9, 11, 9, 11));
        card.setStyle(
            "-fx-background-color: #FBF3DF;" +
            "-fx-border-color: #8C582B; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        // Dòng trên: Đèn báo + Tên dây chuyền + Phễu hàng đợi + Nút nâng cấp tốc độ
        w.statusLed = new Circle(6.0, Color.web("#B45309"));
        w.statusLed.setStroke(Color.web("#3D2412"));
        w.statusLed.setStrokeWidth(1.4);

        Label lineTitle = new Label(line.getLineTitle());
        lockLabel(lineTitle);
        lineTitle.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #3D200C;");

        w.statusText = new Label("CHỜ NGUYÊN LIỆU");
        lockLabel(w.statusText);
        w.statusText.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #8C582B;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        w.queueBadge = new Label("Phễu: 0/20");
        lockLabel(w.queueBadge);
        w.queueBadge.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-text-fill: #4A2810; -fx-background-color: #EEDBB8;" +
            "-fx-border-color: #B5844E; -fx-border-width: 1.2px; -fx-padding: 3 8 3 8; -fx-background-radius: 6px; -fx-border-radius: 6px;"
        );

        w.upgradeBtn = new Button();
        lockButton(w.upgradeBtn);
        w.upgradeBtn.setPrefHeight(27);
        w.upgradeBtn.setOnAction(e -> {
            if (factoryManager.upgradeLineSpeed(player, idx)) {
                SoundHook.playPickup();
                showInlineToast("⚡ Đã nâng cấp tốc độ " + line.getLineTitle() + " lên Cấp " + line.getSpeedLevel() + "!", false);
                refreshStaticPanels();
            } else {
                SoundHook.playMenuClick();
                showInlineToast("⚠ Không đủ tiền để nâng cấp tốc độ dây chuyền!", true);
            }
        });

        HBox headerRow = new HBox(8, w.statusLed, lineTitle, w.statusText, topSpacer, w.queueBadge, w.upgradeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Dòng chọn công thức + Nút Nạp +1 / +5 / Nạp Hết
        w.recipeSelectRow = new HBox(5);
        w.recipeSelectRow.setAlignment(Pos.CENTER_LEFT);
        rebuildRecipeRowForLine(line, idx);

        // Băng chuyền gỗ-đồng phong cách Xưởng Nông Trại (click trực tiếp để nạp +1)
        w.beltCanvas = new Canvas(600, 58);
        w.beltCanvas.setStyle("-fx-cursor: hand;");
        w.beltCanvas.setOnMouseClicked(ev -> handleFeedLine(idx, 1));

        w.machineBar = new ProgressBar(0.0);
        w.machineBar.setPrefWidth(210);
        w.machineBar.setPrefHeight(13);
        w.machineBar.setStyle("-fx-accent: #3F9E4A; -fx-control-inner-background: #E7D2AE;");

        w.timerLabel = new Label("Sẵn sàng • Tốc độ x1");
        lockLabel(w.timerLabel);
        w.timerLabel.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #5C3418;");

        Label progLbl = new Label("⚙ Tiến trình:");
        progLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6E4423;");
        HBox progressRow = new HBox(10, progLbl, w.machineBar, w.timerLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(headerRow, w.recipeSelectRow, w.beltCanvas, progressRow);
        return card;
    }

    private void rebuildRecipeRowForLine(ProductionLine line, int lineIdx) {
        HBox row = lineWidgets[lineIdx].recipeSelectRow;
        row.getChildren().clear();

        for (LineRecipe rec : line.getRecipes()) {
            boolean sel = rec.getRecipeId().equals(line.getSelectedRecipe().getRecipeId());
            FactoryProductItem outItem = rec.createOutputProduct();
            int haveRaw = factoryManager.countAvailableInput(player, rec);
            boolean isHot = marketManager != null && marketManager.isHotItem(outItem);
            int unitPrice = marketManager != null ? marketManager.getSellUnitPrice(outItem) : outItem.getPrice();

            String btnTxt = rec.getInputDisplayName() + " (" + haveRaw + ") ➡ " + outItem.getName()
                + " ($" + unitPrice + (isHot ? " 🔥" : "") + ")";
            Button recBtn = new Button(btnTxt);
            lockButton(recBtn);
            recBtn.setPrefHeight(26);
            recBtn.setStyle(sel
                ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FFFBEB; -fx-background-color: linear-gradient(to bottom, #E87A24, #C05A11); -fx-border-color: #6B2E08; -fx-border-width: 1.5px; -fx-padding: 2 7 2 7; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
                : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #4A2810; -fx-background-color: #EEDBB8; -fx-border-color: #B5844E; -fx-border-width: 1.2px; -fx-padding: 2 7 2 7; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
            );
            recBtn.setOnAction(e -> {
                line.setSelectedRecipeId(rec.getRecipeId());
                SoundHook.playMenuClick();
                refreshStaticPanels();
            });
            row.getChildren().add(recBtn);
        }

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button feed1Btn = new Button("Nạp +1");
        lockButton(feed1Btn);
        styleFeedBtn(feed1Btn, "#4BA3D9", "#2A78AD", "#1B4F72");
        feed1Btn.setOnAction(e -> handleFeedLine(lineIdx, 1));

        Button feed5Btn = new Button("Nạp +5");
        lockButton(feed5Btn);
        styleFeedBtn(feed5Btn, "#52B75B", "#2E7D32", "#1B4D1E");
        feed5Btn.setOnAction(e -> handleFeedLine(lineIdx, 5));

        Button feedAllBtn = new Button("Nạp Hết");
        lockButton(feedAllBtn);
        styleFeedBtn(feedAllBtn, "#F4B942", "#D48B1F", "#78480A");
        feedAllBtn.setOnAction(e -> handleFeedLine(lineIdx, FactoryManager.MAX_QUEUE_SIZE));

        row.getChildren().addAll(sp, feed1Btn, feed5Btn, feedAllBtn);
    }

    private void showInlineToast(String msg, boolean isError) {
        factoryStatusToast.setText(msg);
        factoryStatusToast.setStyle(isError
            ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #7F1D1D; -fx-background-color: #FEE2E2; -fx-border-color: #DC2626; -fx-border-width: 1.5px; -fx-padding: 6 12 6 12; -fx-background-radius: 8px; -fx-border-radius: 8px;"
            : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #14532D; -fx-background-color: #DCFCE7; -fx-border-color: #2E7D32; -fx-border-width: 1.5px; -fx-padding: 6 12 6 12; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
        if (onToast != null) onToast.accept(msg);
    }

    private void handleFeedLine(int lineIdx, int count) {
        int added = factoryManager.enqueueInput(player, lineIdx, count);
        ProductionLine ln = factoryManager.getLines().get(lineIdx);
        if (added > 0) {
            SoundHook.playMenuClick();
            showInlineToast("✅ Đã nạp +" + added + " " + ln.getSelectedRecipe().getInputDisplayName() + " vào " + ln.getLineTitle() + "!", false);
            refreshStaticPanels();
        } else {
            SoundHook.playMenuClick();
            if (ln.getQueueSize() >= FactoryManager.MAX_QUEUE_SIZE) {
                showInlineToast("⚠ Phễu nạp đã đầy tối đa 20 nguyên liệu!", true);
            } else {
                showInlineToast("⚠ Trong túi không có [" + ln.getSelectedRecipe().getInputDisplayName() + "]! Hãy chọn công thức khác hoặc click nguyên liệu bên cột trái.", true);
            }
        }
    }

    /** Click thẳng vào 1 nguyên liệu ở Cột Trái để tự chọn đúng dây chuyền + nạp ngay +1 món. */
    private void quickFeedFromRawInventory(Item rawItem) {
        List<ProductionLine> lines = factoryManager.getLines();
        for (int i = 0; i < lines.size(); i++) {
            ProductionLine ln = lines.get(i);
            for (LineRecipe rec : ln.getRecipes()) {
                if (rec.matchesInventoryItem(rawItem)) {
                    ln.setSelectedRecipeId(rec.getRecipeId());
                    handleFeedLine(i, 1);
                    return;
                }
            }
        }
        showInlineToast("⚠ Nguyên liệu [" + rawItem.getName().replace(" (Crop)", "") + "] hiện chưa có dây chuyền chế biến.", true);
    }

    private VBox buildOutputWarehouseColumn() {
        VBox col = new VBox(9);
        col.setPrefWidth(275);
        col.setMinWidth(265);
        col.setPadding(new Insets(10));
        col.setStyle(
            "-fx-background-color: #FBF3DF;" +
            "-fx-border-color: #8C582B; -fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        Label hdr = new Label("📦 KHO THÀNH PHẨM");
        hdr.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4A2810;");

        outputWarehouseBox.setPadding(new Insets(4));
        ScrollPane sp = new ScrollPane(outputWarehouseBox);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: #F2E2C1; -fx-background-color: #F2E2C1; -fx-border-color: #C49A6C; -fx-border-radius: 7px;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        lockButton(sellAllOutputBtn);
        sellAllOutputBtn.setMaxWidth(Double.MAX_VALUE);
        sellAllOutputBtn.setPrefHeight(42);
        sellAllOutputBtn.setOnAction(e -> {
            int earned = factoryManager.sellAllOutputs(player, marketManager);
            if (earned > 0) {
                SoundHook.playPickup();
                if (onMoneyEarnedQuestHook != null) {
                    onMoneyEarnedQuestHook.accept(earned);
                }
                showInlineToast("💰 Xuất bán lô hàng Xưởng thành công: +$" + earned + "!", false);
                refreshStaticPanels();
            }
        });

        lockButton(collectToBagBtn);
        collectToBagBtn.setMaxWidth(Double.MAX_VALUE);
        collectToBagBtn.setPrefHeight(34);
        collectToBagBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FFFBEB; -fx-background-color: linear-gradient(to bottom, #4BA3D9, #2A78AD);" +
            "-fx-border-color: #1B4F72; -fx-border-width: 1.8px;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        collectToBagBtn.setOnAction(e -> {
            int moved = factoryManager.collectAllToInventory(player);
            if (moved > 0) {
                SoundHook.playPickup();
                showInlineToast("🎒 Đã cất " + moved + " thành phẩm vào túi đồ (dùng [Q] ăn/uống hoặc [G] tặng NPC +1 Tim)!", false);
                refreshStaticPanels();
            }
        });

        col.getChildren().addAll(hdr, sp, sellAllOutputBtn, collectToBagBtn);
        return col;
    }

    // ==================== VÒNG LẶP HOẠT ẢNH BĂNG CHUYỀN 60 FPS ====================

    private void startConveyorLoop() {
        lastTickNanos = 0L;
        conveyorTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (getScene() == null || !isVisible()) return;
                double dt = (lastTickNanos == 0L) ? 0.016 : Math.min(0.05, Math.max(0.001, (now - lastTickNanos) / 1_000_000_000.0));
                lastTickNanos = now;

                factoryManager.update(dt);
                beltScrollPx = (beltScrollPx + dt * 68.0) % 24.0;

                List<ProductionLine> lines = factoryManager.getLines();
                for (int i = 0; i < lines.size(); i++) {
                    renderConveyorLineFrame(lines.get(i), lineWidgets[i]);
                }

                int hash = factoryManager.getOutputStorage().hashCode() * 31 + factoryManager.getTotalOutputCount();
                if (hash != lastOutputSnapshotHash) {
                    lastOutputSnapshotHash = hash;
                    rebuildOutputWarehouse();
                    for (int i = 0; i < lines.size(); i++) {
                        lineWidgets[i].queueBadge.setText("Phễu: " + lines.get(i).getQueueSize() + "/" + FactoryManager.MAX_QUEUE_SIZE);
                    }
                }
            }
        };
        conveyorTimer.start();
    }

    private void renderConveyorLineFrame(ProductionLine line, LineUiWidgets w) {
        if (w == null || w.beltCanvas == null) return;
        boolean active = line.isRunning();
        double ratio = line.getProgressRatio();

        w.statusLed.setFill(active ? Color.web("#2E9E44") : Color.web("#D97706"));
        w.statusText.setText(active ? ("ĐANG CHẠY (x" + line.getSpeedLevel() + ")") : "CHỜ NẠP LIỆU");
        w.statusText.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: "
                + (active ? "#15803D;" : "#9A6332;"));
        w.queueBadge.setText("Phễu: " + line.getQueueSize() + "/" + FactoryManager.MAX_QUEUE_SIZE);
        w.machineBar.setProgress(active ? ratio : 0.0);
        w.timerLabel.setText(active
            ? String.format("Còn %.1fs  •  Tốc độ Cấp %d (x%d)", line.getRemainingSec(), line.getSpeedLevel(), line.getSpeedLevel())
            : ("Tốc độ Cấp " + line.getSpeedLevel() + " (x" + line.getSpeedLevel() + ")"));

        GraphicsContext g = w.beltCanvas.getGraphicsContext2D();
        double cw = w.beltCanvas.getWidth();
        double ch = w.beltCanvas.getHeight();
        g.clearRect(0, 0, cw, ch);

        // Nền băng tải gỗ sồi ấm
        g.setFill(Color.web("#5C371C"));
        g.fillRoundRect(4, 15, cw - 8, 28, 10, 10);
        g.setStroke(Color.web("#331B0B"));
        g.setLineWidth(2.0);
        g.strokeRoundRect(4, 15, cw - 8, 28, 10, 10);

        // Các thanh gỗ-đồng chuyển động sang phải
        double shift = active ? (beltScrollPx * line.getSpeedLevel()) % 24.0 : 0.0;
        g.setStroke(active ? Color.web("#F6C358", 0.85) : Color.web("#8D613B", 0.55));
        g.setLineWidth(2.2);
        for (double x = 14 + shift; x < cw - 14; x += 24) {
            g.strokeLine(x, 18, x + 6, 29);
            g.strokeLine(x + 6, 29, x, 40);
        }

        // Phễu gỗ đầu vào bên trái
        g.setFill(Color.web("#B45309"));
        g.fillRoundRect(8, 7, 66, 44, 8, 8);
        g.setStroke(Color.web("#451A03"));
        g.setLineWidth(2.0);
        g.strokeRoundRect(8, 7, 66, 44, 8, 8);
        g.setFill(Color.web("#FFFBEB"));
        g.fillText("PHỄU", 24, 33);

        // Buồng máy xay/ép viền đồng ở trung tâm
        double mx = cw * 0.41;
        double mw = 112;
        g.setFill(active ? Color.web("#854D27") : Color.web("#6B4226"));
        g.fillRoundRect(mx, 4, mw, 50, 10, 10);
        g.setStroke(active ? Color.web("#F59E0B") : Color.web("#3B200D"));
        g.setLineWidth(2.5);
        g.strokeRoundRect(mx, 4, mw, 50, 10, 10);
        g.setFill(active ? Color.web("#FEF08A") : Color.web("#FDE68A"));
        g.fillText(active ? String.format("⚙ MÁY %d%%", (int) Math.round(ratio * 100)) : "⚙ MÁY CHỜ", mx + 18, 33);

        // Giỏ/Khay thành phẩm xanh lá bên phải
        g.setFill(Color.web("#2E7D32"));
        g.fillRoundRect(cw - 82, 7, 74, 44, 8, 8);
        g.setStroke(Color.web("#14532D"));
        g.setLineWidth(2.0);
        g.strokeRoundRect(cw - 82, 7, 74, 44, 8, 8);
        g.setFill(Color.web("#F0FDF4"));
        g.fillText("RA LÒ", cw - 64, 33);

        // Vẽ nguyên liệu trượt từ Phễu -> Máy và Thành phẩm trượt từ Máy -> Ra Lò
        if (active && line.getActiveRecipeId() != null) {
            LineRecipe rec = line.findRecipe(line.getActiveRecipeId());
            if (rec != null) {
                if (ratio < 0.50) {
                    double t = ratio / 0.50;
                    double ix = 76 + (mx - 28 - 76) * t;
                    Image rawImg = Assets.getShopIcon(rec.createInputPrototype());
                    if (rawImg != null) {
                        g.drawImage(rawImg, ix, 13, 32, 32);
                    }
                } else {
                    double t = (ratio - 0.50) / 0.50;
                    double ox = (mx + mw + 4) + ((cw - 116) - (mx + mw + 4)) * t;
                    Image outImg = Assets.getShopIcon(rec.createOutputProduct());
                    if (outImg != null) {
                        g.drawImage(outImg, ox, 13, 32, 32);
                    }
                }
            }
        }
    }

    // ==================== LÀM MỚI DỮ LIỆU TĨNH ====================

    private void refreshStaticPanels() {
        if (player != null) {
            balanceLabel.setText("💰 Số dư: $" + player.getMoney());
        }
        if (marketManager != null) {
            hotMarketBanner.setText(marketManager.getMorningBannerText());
        }

        // Cột trái: nguyên liệu thô (click để nạp nhanh +1)
        rawInventoryBox.getChildren().clear();
        int shown = 0;
        if (player != null) {
            for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
                Item it = e.getKey();
                int qty = e.getValue();
                if (qty <= 0) continue;
                boolean rawInput = (it instanceof HarvestedCropItem)
                        || (it instanceof AnimalProductItem && it.getName().equalsIgnoreCase("Fresh Milk"));
                if (!rawInput) continue;
                shown++;
                HBox itemRow = new HBox(8);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                itemRow.setPadding(new Insets(6, 8, 6, 8));
                itemRow.setStyle(
                    "-fx-background-color: #FFFBF0;" +
                    "-fx-border-color: #C49A6C; -fx-border-width: 1.5px;" +
                    "-fx-border-radius: 7px; -fx-background-radius: 7px; -fx-cursor: hand;"
                );
                itemRow.setOnMouseClicked(ev -> quickFeedFromRawInventory(it));

                ImageView iv = Assets.imageView(Assets.getShopIcon(it), 28);
                Label lbl = new Label(it.getName().replace(" (Crop)", "") + " ×" + qty);
                lbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3D200C;");
                Region spRow = new Region();
                HBox.setHgrow(spRow, Priority.ALWAYS);
                Label quickTag = new Label("+1 ➡");
                quickTag.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #C05A11;");
                if (iv != null) itemRow.getChildren().add(iv);
                itemRow.getChildren().addAll(lbl, spRow, quickTag);
                rawInventoryBox.getChildren().add(itemRow);
            }
        }
        if (shown == 0) {
            Label empty = new Label("Túi chưa có Cà chua, Khoai tây, Ngô, Lúa nước, Hướng dương hoặc Sữa bò.\n\nHãy ra đồng thu hoạch rồi quay lại nạp vào phễu!");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #7A5033; -fx-padding: 8;");
            rawInventoryBox.getChildren().add(empty);
        }

        // Trung tâm: nút nâng cấp tốc độ & công thức
        List<ProductionLine> lines = factoryManager.getLines();
        for (int i = 0; i < lines.size(); i++) {
            ProductionLine ln = lines.get(i);
            LineUiWidgets w = lineWidgets[i];
            if (w == null) continue;
            rebuildRecipeRowForLine(ln, i);

            if (ln.getSpeedLevel() >= FactoryManager.MAX_SPEED_LEVEL) {
                w.upgradeBtn.setText("⚡ Cấp 3 TỐI ĐA (x3)");
                w.upgradeBtn.setDisable(true);
                w.upgradeBtn.setStyle(
                    "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #14532D; -fx-background-color: #BBF7D0; -fx-border-color: #2E7D32; -fx-border-width: 1.3px; -fx-border-radius: 6px; -fx-background-radius: 6px;"
                );
            } else {
                int cost = ln.getUpgradeCost();
                w.upgradeBtn.setText("⚡ Nâng cấp ($" + cost + ")");
                w.upgradeBtn.setDisable(player == null || player.getMoney() < cost);
                w.upgradeBtn.setStyle(
                    "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #4A2810; -fx-background-color: linear-gradient(to bottom, #FDE047, #EAB308);" +
                    "-fx-border-color: #92400E; -fx-border-width: 1.4px; -fx-padding: 2 9 2 9; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;"
                );
            }
        }

        rebuildOutputWarehouse();
    }

    private void rebuildOutputWarehouse() {
        outputWarehouseBox.getChildren().clear();
        Map<String, Integer> outMap = factoryManager.getOutputStorage();
        int count = 0;
        for (Map.Entry<String, Integer> e : outMap.entrySet()) {
            int qty = e.getValue();
            if (qty <= 0) continue;
            FactoryProductItem prod = FactoryProductItem.createById(e.getKey());
            if (prod == null) continue;
            count += qty;

            boolean hot = marketManager != null && marketManager.isHotItem(prod);
            int unitPrice = marketManager != null ? marketManager.getSellUnitPrice(prod) : prod.getPrice();

            HBox card = new HBox(8);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(7, 9, 7, 9));
            card.setStyle(hot
                ? "-fx-background-color: #FEF3C7; -fx-border-color: #EA580C; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px;"
                : "-fx-background-color: #FFFBF0; -fx-border-color: #C49A6C; -fx-border-width: 1.4px; -fx-background-radius: 8px; -fx-border-radius: 8px;"
            );

            ImageView iv = Assets.imageView(Assets.getShopIcon(prod), 30);
            VBox meta = new VBox(2);
            Label nm = new Label(prod.getName() + " ×" + qty);
            nm.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #3D200C;");
            Label pr = new Label("$" + unitPrice + "/món" + (hot ? " [+50% HOT 🔥]" : ""));
            pr.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: "
                    + (hot ? "#C2410C;" : "#15803D;"));
            meta.getChildren().addAll(nm, pr);

            if (iv != null) card.getChildren().add(iv);
            card.getChildren().add(meta);
            outputWarehouseBox.getChildren().add(card);
        }

        if (count == 0) {
            Label empty = new Label("Kho thành phẩm đang trống.\n\nDây chuyền sẽ tự động đẩy sản phẩm hoàn tất sang đây (kể cả khi bạn rời Xưởng)!");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #7A5033; -fx-padding: 8;");
            outputWarehouseBox.getChildren().add(empty);
        }

        int totalVal = factoryManager.computeTotalOutputValue(marketManager);
        sellAllOutputBtn.setText("💰 XUẤT BÁN TẤT CẢ (+$" + totalVal + ")");
        sellAllOutputBtn.setDisable(totalVal <= 0);
        sellAllOutputBtn.setStyle(totalVal > 0
            ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFBEB; -fx-background-color: linear-gradient(to bottom, #52B75B, #2E7D32); -fx-border-color: #1B4D1E; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
            : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #9A7B5C; -fx-background-color: #EAD8B8; -fx-border-color: #C49A6C; -fx-border-width: 1.5px; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );
        collectToBagBtn.setDisable(count <= 0);
    }

    /** Vẽ thanh nẹp gỗ sồi với đinh tán đồng ấm áp thay cho dải sọc cảnh báo tối màu. */
    private static void drawWoodenBrassTrim(GraphicsContext g, double w, double h) {
        g.setFill(Color.web("#3D200C"));
        g.fillRoundRect(0, 0, w, h, 4, 4);
        g.setFill(Color.web("#D99B38"));
        g.fillRect(4, 2, w - 8, 2);
        for (double x = 14; x < w - 10; x += 48) {
            g.fillOval(x, 1.5, 5, 5);
        }
    }

    private static void styleFeedBtn(Button btn, String topHex, String botHex, String borderHex) {
        btn.setPrefHeight(26);
        btn.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: #FFFBEB; -fx-background-color: linear-gradient(to bottom, " + topHex + ", " + botHex + ");" +
            "-fx-border-color: " + borderHex + "; -fx-border-width: 1.3px;" +
            "-fx-padding: 2 8 2 8; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
        );
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

    public void requestExit() {
        if (conveyorTimer != null) {
            conveyorTimer.stop();
        }
        SoundHook.playMenuClick();
        if (onExitFactory != null) {
            onExitFactory.run();
        }
    }
}
