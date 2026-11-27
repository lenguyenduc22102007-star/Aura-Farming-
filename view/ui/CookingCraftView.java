package view.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.cooking.CookingRecipeManager;
import model.cooking.CookingRecipeManager.CookingRecipe;
import model.cooking.CookingRecipeManager.IngredientKind;
import model.cooking.CookingRecipeManager.MatchResult;
import model.game_item.CookedFoodItem;
import model.game_item.Item;
import model.player_inventory.Player;
import view.assets.Assets;
import view.fx.SoundHook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Màn hình Ghép Đồ Ăn kiểu Bàn Chế Tạo Minecraft (3x3 Crafting Table) tại Đống Lửa Trại.
 * - Tách riêng hoàn toàn khỏi InGameController và dùng CookingRecipeManager cho logic khớp công thức.
 * - Mở toàn màn hình phủ lên nông trại (tạm dừng vòng lặp thời gian & tụt Đói/Khát).
 * - Trái: Túi Nguyên Liệu | Giữa: Bàn Ghép 3x3 | Phải: Mũi tên -> Ô Thành Phẩm & Nút CHẾ TẠO | Dưới: Sổ Công Thức Gợi Ý.
 */
public class CookingCraftView extends StackPane {

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    private final Player player;
    private final Supplier<String> activeBuffSupplier;
    private final Runnable onCraftSuccessFx;
    private final Consumer<CookedFoodItem> onEatFood;
    private final Consumer<String> onToast;
    private final Runnable onClose;

    /** 9 ô trên bàn ghép 3x3 (chỉ số 0..8; hàng 2 là ô 6, 7, 8 — hàng nhóm Củi khô). */
    private final Item[] craftGrid = new Item[9];
    private Item selectedInventoryItem = null;

    // UI Components
    private final Label activeBuffLabel;
    private final Label selectedHintLabel;
    private final GridPane inventoryGrid;
    private final StackPane[] craftSlotPanes = new StackPane[9];
    private final StackPane resultSlotPane;
    private final Rectangle resultFlashRect;
    private final Label resultNameLabel;
    private final Label resultStatsLabel;
    private final Label recipeStatusLabel;
    private final Button craftButton;
    private final Button eatNowButton;
    private final HBox recipeGuideRow;

    private MatchResult currentMatch = null;

    public CookingCraftView(Player player,
                            Supplier<String> activeBuffSupplier,
                            Runnable onCraftSuccessFx,
                            Consumer<CookedFoodItem> onEatFood,
                            Consumer<String> onToast,
                            Runnable onClose) {
        this.player = player;
        this.activeBuffSupplier = activeBuffSupplier;
        this.onCraftSuccessFx = onCraftSuccessFx;
        this.onEatFood = onEatFood;
        this.onToast = onToast;
        this.onClose = onClose;

        // Nền tối toàn màn hình phong cách bếp lửa đêm ấm áp
        setStyle("-fx-background-color: rgba(14, 9, 6, 0.84);");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                closeScreen();
            }
        });

        // ==================== KHUNG BÀN CHẾ TẠO CHÍNH (1060 x 650) ====================
        VBox rootBoard = new VBox(12);
        rootBoard.setPrefSize(1060, 650);
        rootBoard.setMaxSize(1060, 650);
        rootBoard.setPadding(new Insets(16, 20, 16, 20));
        rootBoard.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #6C3D1E 0%, #482611 55%, #371C0C 100%);" +
            "-fx-border-color: #210F06;" +
            "-fx-border-width: 4px;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.78), 24, 0.3, 0, 8);"
        );

        // -------------------- 1. HEADER --------------------
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label("🔥 BÀN GHÉP NẤU ĂN LỬA TRẠI (CRAFTING 3x3)");
        titleLbl.setMinWidth(Region.USE_PREF_SIZE);
        titleLbl.setTextOverrun(OverrunStyle.CLIP);
        titleLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE79A;" +
            "-fx-effect: dropshadow(one-pass-box, #241005, 2, 1.0, 0, 2);"
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        activeBuffLabel = new Label();
        activeBuffLabel.setMinWidth(Region.USE_PREF_SIZE);
        activeBuffLabel.setTextOverrun(OverrunStyle.CLIP);
        activeBuffLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF4D2;" +
            "-fx-background-color: rgba(34, 16, 7, 0.85);" +
            "-fx-border-color: #DF9E46;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 5 11 5 11;" +
            "-fx-background-radius: 7px;" +
            "-fx-border-radius: 7px;"
        );

        Button backBtn = new Button("← Quay lại nông trại [ESC]");
        backBtn.setFocusTraversable(false);
        backBtn.setMinWidth(Region.USE_PREF_SIZE);
        backBtn.setTextOverrun(OverrunStyle.CLIP);
        backBtn.setPrefHeight(34);
        backBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF6E0;" +
            "-fx-background-color: linear-gradient(to bottom, #B4412A, #862918);" +
            "-fx-border-color: #3E120A;" +
            "-fx-border-width: 2px;" +
            "-fx-padding: 0 14 0 14;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;" +
            "-fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> closeScreen());

        header.getChildren().addAll(titleLbl, headerSpacer, activeBuffLabel, backBtn);

        // -------------------- 2. WORKBENCH (TRÁI: TÚI | GIỮA: 3x3 | PHẢI: THÀNH PHẨM) --------------------
        HBox workbench = new HBox(16);
        workbench.setAlignment(Pos.CENTER);
        VBox.setVgrow(workbench, Priority.ALWAYS);

        // === BÊN TRÁI: TÚI NGUYÊN LIỆU ===
        VBox leftPanel = new VBox(8);
        leftPanel.setPrefWidth(360);
        leftPanel.setMinWidth(350);
        leftPanel.setPadding(new Insets(12));
        leftPanel.setStyle(
            "-fx-background-color: #2B170C;" +
            "-fx-border-color: #8E582C;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;"
        );

        Label invTitle = new Label("🎒 TÚI NGUYÊN LIỆU NẤU ĂN");
        invTitle.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 13.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFD97A;"
        );

        selectedHintLabel = new Label("Click chọn nguyên liệu rồi bấm ô 3x3 để đặt");
        selectedHintLabel.setWrapText(true);
        selectedHintLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #E7CCA6;"
        );

        inventoryGrid = new GridPane();
        inventoryGrid.setHgap(8);
        inventoryGrid.setVgap(8);
        inventoryGrid.setPadding(new Insets(4));

        ScrollPane invScroll = new ScrollPane(inventoryGrid);
        invScroll.setFitToWidth(true);
        invScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        invScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        invScroll.setStyle(
            "-fx-background: #3A2213;" +
            "-fx-background-color: #3A2213;" +
            "-fx-border-color: #6B3F1D;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );
        VBox.setVgrow(invScroll, Priority.ALWAYS);

        leftPanel.getChildren().addAll(invTitle, selectedHintLabel, invScroll);

        // === Ở GIỮA: BÀN GHÉP 3x3 KIỂU MINECRAFT ===
        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPrefWidth(310);
        centerPanel.setPadding(new Insets(12));
        centerPanel.setStyle(
            "-fx-background-color: #362012;" +
            "-fx-border-color: #A86B35;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;"
        );

        Label gridTitle = new Label("⚒ BÀN GHÉP 3 × 3");
        gridTitle.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE28E;"
        );

        Label gridSub = new Label("Chuột trái: Đặt vào ô  •  Chuột phải: Lấy lại");
        gridSub.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 10.5px;" +
            "-fx-text-fill: #E2C69F;"
        );

        GridPane craftGridPane = new GridPane();
        craftGridPane.setHgap(8);
        craftGridPane.setVgap(8);
        craftGridPane.setAlignment(Pos.CENTER);
        craftGridPane.setPadding(new Insets(10));
        craftGridPane.setStyle(
            "-fx-background-color: #5C4430;" +
            "-fx-border-color: #2A190E;" +
            "-fx-border-width: 3px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );

        for (int i = 0; i < 9; i++) {
            final int slotIdx = i;
            StackPane slot = new StackPane();
            slot.setPrefSize(76, 76);
            slot.setMinSize(76, 76);
            slot.setMaxSize(76, 76);
            slot.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.SECONDARY) {
                    removeFromGridSlot(slotIdx);
                } else if (ev.getButton() == MouseButton.PRIMARY) {
                    handleLeftClickGridSlot(slotIdx);
                }
            });
            craftSlotPanes[i] = slot;
            craftGridPane.add(slot, i % 3, i / 3);
        }

        Button clearGridBtn = new Button("🧹 Dọn bàn (trả về kho)");
        clearGridBtn.setFocusTraversable(false);
        clearGridBtn.setMinWidth(Region.USE_PREF_SIZE);
        clearGridBtn.setTextOverrun(OverrunStyle.CLIP);
        clearGridBtn.setPrefHeight(30);
        clearGridBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #F5E1BE;" +
            "-fx-background-color: #5A351D;" +
            "-fx-border-color: #2C160A;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 0 12 0 12;" +
            "-fx-background-radius: 7px;" +
            "-fx-border-radius: 7px;" +
            "-fx-cursor: hand;"
        );
        clearGridBtn.setOnAction(e -> {
            clearCraftGrid();
            SoundHook.playMenuClick();
        });

        centerPanel.getChildren().addAll(gridTitle, gridSub, craftGridPane, clearGridBtn);

        // === MŨI TÊN CHUYỂN TIẾP ===
        VBox arrowBox = new VBox();
        arrowBox.setAlignment(Pos.CENTER);
        Label arrowLbl = new Label("➡");
        arrowLbl.setStyle(
            "-fx-font-family: 'Segoe UI Symbol', 'Consolas', sans-serif;" +
            "-fx-font-size: 38px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFB838;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,152,32,0.55), 10, 0.3, 0, 0);"
        );
        arrowBox.getChildren().add(arrowLbl);

        // === BÊN PHẢI: Ô THÀNH PHẨM & NÚT CHẾ TẠO ===
        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setPrefWidth(295);
        rightPanel.setMinWidth(285);
        rightPanel.setPadding(new Insets(14));
        rightPanel.setStyle(
            "-fx-background-color: #2B170C;" +
            "-fx-border-color: #C78436;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;"
        );

        Label resTitle = new Label("🍲 Ô THÀNH PHẨM");
        resTitle.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE28E;"
        );

        resultSlotPane = new StackPane();
        resultSlotPane.setPrefSize(102, 102);
        resultSlotPane.setMinSize(102, 102);
        resultSlotPane.setMaxSize(102, 102);

        resultFlashRect = new Rectangle(96, 96);
        resultFlashRect.setArcWidth(12);
        resultFlashRect.setArcHeight(12);
        resultFlashRect.setFill(Color.rgb(255, 240, 148, 0.85));
        resultFlashRect.setOpacity(0.0);
        resultFlashRect.setMouseTransparent(true);

        resultNameLabel = new Label("Chưa có món ăn");
        resultNameLabel.setWrapText(true);
        resultNameLabel.setAlignment(Pos.CENTER);
        resultNameLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF1C9;" +
            "-fx-text-alignment: center;"
        );

        resultStatsLabel = new Label("Xếp nguyên liệu lên lưới 3x3");
        resultStatsLabel.setWrapText(true);
        resultStatsLabel.setAlignment(Pos.CENTER);
        resultStatsLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-text-fill: #B6E394;" +
            "-fx-text-alignment: center;"
        );

        recipeStatusLabel = new Label();
        recipeStatusLabel.setWrapText(true);
        recipeStatusLabel.setAlignment(Pos.CENTER);
        recipeStatusLabel.setMinHeight(34);
        recipeStatusLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #F4C77D;" +
            "-fx-text-alignment: center;"
        );

        craftButton = new Button("🔥 CHẾ TẠO (CRAFT)");
        craftButton.setFocusTraversable(false);
        craftButton.setPrefWidth(246);
        craftButton.setPrefHeight(42);
        craftButton.setMinWidth(Region.USE_PREF_SIZE);
        craftButton.setTextOverrun(OverrunStyle.CLIP);
        craftButton.setOnAction(e -> performCraft());

        eatNowButton = new Button("🍽 Ăn ngay món này");
        eatNowButton.setFocusTraversable(false);
        eatNowButton.setPrefWidth(246);
        eatNowButton.setPrefHeight(34);
        eatNowButton.setMinWidth(Region.USE_PREF_SIZE);
        eatNowButton.setTextOverrun(OverrunStyle.CLIP);
        eatNowButton.setOnAction(e -> performEatMatchedDish());

        rightPanel.getChildren().addAll(
            resTitle,
            resultSlotPane,
            resultNameLabel,
            resultStatsLabel,
            recipeStatusLabel,
            craftButton,
            eatNowButton
        );

        workbench.getChildren().addAll(leftPanel, centerPanel, arrowBox, rightPanel);

        // -------------------- 3. SỔ CÔNG THỨC GỢI Ý (GÓC DƯỚI) --------------------
        VBox guideBox = new VBox(6);
        guideBox.setPadding(new Insets(10, 12, 10, 12));
        guideBox.setStyle(
            "-fx-background-color: #241309;" +
            "-fx-border-color: #845026;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;"
        );

        Label guideHeader = new Label("📖 SỔ CÔNG THỨC GỢI Ý (Bấm vào thẻ công thức để xếp nhanh lên bàn 3x3 nếu đủ nguyên liệu)");
        guideHeader.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFD77A;"
        );

        recipeGuideRow = new HBox(8);
        recipeGuideRow.setAlignment(Pos.CENTER_LEFT);

        guideBox.getChildren().addAll(guideHeader, recipeGuideRow);

        rootBoard.getChildren().addAll(header, workbench, guideBox);
        getChildren().add(rootBoard);

        refreshAllUi();
    }

    // ======================== XỬ LÝ TƯƠNG TÁC LƯỚI 3x3 ========================

    private Map<Item, Integer> computeAvailableIngredients() {
        Map<Item, Integer> available = new LinkedHashMap<>();
        if (player == null) return available;

        // Lấy tất cả nguyên liệu nấu ăn trong Inventory
        for (Map.Entry<Item, Integer> entry : player.getInventory().getItems().entrySet()) {
            Item item = entry.getKey();
            int qty = entry.getValue();
            if (qty > 0 && CookingRecipeManager.isCookingIngredient(item)) {
                available.put(item, qty);
            }
        }
        // Trừ đi số lượng đang tạm xếp trên bàn 3x3
        for (Item staged : craftGrid) {
            if (staged != null && available.containsKey(staged)) {
                int left = available.get(staged) - 1;
                if (left <= 0) {
                    available.remove(staged);
                } else {
                    available.put(staged, left);
                }
            }
        }
        return available;
    }

    private void handleLeftClickGridSlot(int slotIdx) {
        Item existing = craftGrid[slotIdx];
        if (selectedInventoryItem != null) {
            Map<Item, Integer> avail = computeAvailableIngredients();
            int left = avail.getOrDefault(selectedInventoryItem, 0);
            if (left > 0) {
                if (existing != null && existing.equals(selectedInventoryItem)) {
                    // Đã có đúng món này ở ô này -> bỏ qua để tránh ghi đè vô ích
                    return;
                }
                craftGrid[slotIdx] = selectedInventoryItem;
                SoundHook.playMenuClick();
                if (left <= 1) {
                    // Hết món đang chọn trong kho tạm -> bỏ chọn để thao tác gọn
                    selectedInventoryItem = null;
                }
                refreshAllUi();
                return;
            }
        }
        // Nếu không có món được chọn hoặc ô đã chứa đồ -> bấm chuột trái cũng hỗ trợ trả đồ lại kho
        if (existing != null) {
            removeFromGridSlot(slotIdx);
        } else if (onToast != null) {
            onToast.accept("Hãy click chọn một nguyên liệu ở Túi bên trái trước rồi bấm vào ô 3x3!");
        }
    }

    private void removeFromGridSlot(int slotIdx) {
        if (craftGrid[slotIdx] == null) return;
        craftGrid[slotIdx] = null;
        SoundHook.playMenuClick();
        refreshAllUi();
    }

    private void clearCraftGrid() {
        for (int i = 0; i < 9; i++) {
            craftGrid[i] = null;
        }
        refreshAllUi();
    }

    /** Xếp nhanh một công thức từ Sổ Công Thức Gợi Ý (Củi khô đặt chuẩn ô dưới giữa #7, nguyên liệu còn lại ở hàng giữa). */
    private void quickFillRecipe(CookingRecipe recipe) {
        clearCraftGrid();
        Map<Item, Integer> pool = computeAvailableIngredients();
        List<Item> chosen = new ArrayList<>();
        List<IngredientKind> missing = new ArrayList<>();

        for (IngredientKind kind : recipe.getRequiredIngredients()) {
            Item picked = CookingRecipeManager.findMatchingItem(
                pool.keySet(),
                it -> pool.getOrDefault(it, 0) > 0,
                kind
            );
            if (picked == null) {
                missing.add(kind);
            } else {
                chosen.add(picked);
                int left = pool.get(picked) - 1;
                if (left <= 0) pool.remove(picked);
                else pool.put(picked, left);
            }
        }

        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder("Thiếu nguyên liệu: ");
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(missing.get(i).getDisplayLabel());
            }
            if (onToast != null) onToast.accept(sb.toString());
            SoundHook.playMenuClick();
            refreshAllUi();
            return;
        }

        // Đặt Củi khô vào ô dưới giữa (7), các nguyên liệu khác vào hàng trên/giữa đẹp mắt (4, 3, 5, 1)
        int[] foodSlots = { 4, 3, 5, 1, 0, 2 };
        int foodCursor = 0;
        for (Item it : chosen) {
            if (CookingRecipeManager.matchesKind(it, IngredientKind.FIREWOOD) && craftGrid[7] == null) {
                craftGrid[7] = it;
            } else if (foodCursor < foodSlots.length) {
                craftGrid[foodSlots[foodCursor++]] = it;
            }
        }
        SoundHook.playMenuClick();
        refreshAllUi();
    }

    private void performCraft() {
        if (player == null || currentMatch == null || !currentMatch.isMatched()) return;

        // Gom số lượng từng nguyên liệu cần trừ
        Map<Item, Integer> needed = new LinkedHashMap<>();
        for (Item it : craftGrid) {
            if (it != null) {
                needed.merge(it, 1, Integer::sum);
            }
        }
        // Xác nhận đủ trong Inventory
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            if (player.getInventory().getItemCount(e.getKey()) < e.getValue()) {
                if (onToast != null) onToast.accept("Không đủ " + CookingRecipeManager.getVietnameseDisplayName(e.getKey()) + " trong kho!");
                refreshAllUi();
                return;
            }
        }
        // Trừ nguyên liệu
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            player.getInventory().removeItem(e.getKey(), e.getValue());
        }
        // Cộng món ăn vào kho
        CookedFoodItem output = currentMatch.getOutput();
        player.getInventory().addItem(output, 1);

        // Xóa lưới 3x3 sau khi chế tạo xong
        for (int i = 0; i < 9; i++) {
            craftGrid[i] = null;
        }

        // Hiệu ứng sáng ô thành phẩm + âm thanh + nhiệm vụ nấu ăn
        playCraftResultFlash();
        if (onCraftSuccessFx != null) {
            onCraftSuccessFx.run();
        }
        if (onToast != null) {
            onToast.accept("🔥 Chế tạo thành công: " + output.getName() + "! (" + output.getBuffDescription() + ")");
        }
        refreshAllUi();
    }

    private void performEatMatchedDish() {
        if (player == null || currentMatch == null || !currentMatch.isMatched()) return;
        CookedFoodItem target = currentMatch.getOutput();
        if (player.getInventory().getItemCount(target) <= 0) return;
        if (player.getInventory().removeItem(target, 1)) {
            if (onEatFood != null) {
                onEatFood.accept(target);
            }
            refreshAllUi();
        }
    }

    private void playCraftResultFlash() {
        resultFlashRect.setOpacity(0.92);
        FadeTransition ft = new FadeTransition(Duration.millis(420), resultFlashRect);
        ft.setFromValue(0.92);
        ft.setToValue(0.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), resultSlotPane);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.13);
        st.setToY(1.13);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        new ParallelTransition(ft, st).play();
    }

    // ======================== CẬP NHẬT GIAO DIỆN ========================

    private void refreshAllUi() {
        updateBuffHeader();
        rebuildInventoryPanel();
        rebuildCraftGridSlots();
        evaluateAndRenderResult();
        rebuildRecipeGuide();
    }

    private void updateBuffHeader() {
        String buff = (activeBuffSupplier != null) ? activeBuffSupplier.get() : "";
        activeBuffLabel.setText((buff == null || buff.isEmpty()) ? "Buff hiện tại: Chưa có" : ("Buff: " + buff));
    }

    private void rebuildInventoryPanel() {
        inventoryGrid.getChildren().clear();
        Map<Item, Integer> available = computeAvailableIngredients();

        if (selectedInventoryItem != null && available.getOrDefault(selectedInventoryItem, 0) <= 0) {
            selectedInventoryItem = null;
        }

        if (selectedInventoryItem != null) {
            selectedHintLabel.setText("Đang cầm: [" + CookingRecipeManager.getVietnameseDisplayName(selectedInventoryItem)
                + "] -> Click vào ô 3x3 để đặt!");
            selectedHintLabel.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8CFF98;"
            );
        } else {
            selectedHintLabel.setText("Click chọn nguyên liệu bên dưới rồi click vào bàn 3x3");
            selectedHintLabel.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #E7CCA6;"
            );
        }

        if (available.isEmpty()) {
            Label emptyLbl = new Label("Kho nguyên liệu nấu ăn đang trống!\n\nHãy nhặt Củi khô, Nấm dại, Thảo mộc quanh khu trại, câu cá ở sông, thu hoạch nông sản hoặc thu sản phẩm từ Gà / Lợn / Bò.");
            emptyLbl.setWrapText(true);
            emptyLbl.setPrefWidth(295);
            emptyLbl.setPadding(new Insets(12));
            emptyLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-text-fill: #D8BA92;"
            );
            inventoryGrid.add(emptyLbl, 0, 0);
            return;
        }

        int idx = 0;
        for (Map.Entry<Item, Integer> e : available.entrySet()) {
            Item item = e.getKey();
            int qty = e.getValue();
            boolean isSelected = item.equals(selectedInventoryItem);

            VBox card = new VBox(3);
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(98, 92);
            card.setMinSize(98, 92);
            card.setPadding(new Insets(5, 4, 5, 4));
            card.setStyle(isSelected
                ? "-fx-background-color: #6B471C; -fx-border-color: #FFD95A; -fx-border-width: 2.5px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
                : "-fx-background-color: #4E2F1A; -fx-border-color: #84522A; -fx-border-width: 1.5px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
            );

            StackPane iconBox = new StackPane();
            iconBox.setPrefSize(42, 42);
            Image img = Assets.getShopIcon(item);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(36);
                iv.setFitHeight(36);
                iv.setPreserveRatio(true);
                iv.setSmooth(false);
                iconBox.getChildren().add(iv);
            }

            Label qtyBadge = new Label("x" + qty);
            qtyBadge.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: #FFF6D6; -fx-background-color: rgba(24,12,6,0.86);" +
                "-fx-padding: 1 5 1 5; -fx-background-radius: 5px;"
            );
            StackPane.setAlignment(qtyBadge, Pos.BOTTOM_RIGHT);
            iconBox.getChildren().add(qtyBadge);

            Label nameLbl = new Label(CookingRecipeManager.getVietnameseDisplayName(item));
            nameLbl.setWrapText(true);
            nameLbl.setAlignment(Pos.CENTER);
            nameLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isSelected ? "#FFF2A6" : "#F6DFC2") + "; -fx-text-alignment: center;"
            );

            card.getChildren().addAll(iconBox, nameLbl);
            Tooltip.install(card, new Tooltip(CookingRecipeManager.getVietnameseDisplayName(item) + " (Còn " + qty + ")\n" + item.getStats()));
            card.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    selectedInventoryItem = isSelected ? null : item;
                    SoundHook.playMenuClick();
                    refreshAllUi();
                }
            });

            inventoryGrid.add(card, idx % 3, idx / 3);
            idx++;
        }
    }

    private void rebuildCraftGridSlots() {
        for (int i = 0; i < 9; i++) {
            StackPane slot = craftSlotPanes[i];
            slot.getChildren().clear();

            boolean isBottomFireRow = (i >= 6);
            Item item = craftGrid[i];

            if (item == null) {
                slot.setStyle(isBottomFireRow
                    ? "-fx-background-color: #462D1D; -fx-border-color: #C46F2D; -fx-border-width: 2px; -fx-border-style: dashed; -fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;"
                    : "-fx-background-color: #422F22; -fx-border-color: #7D5B3E; -fx-border-width: 2px; -fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;"
                );
                if (isBottomFireRow) {
                    Label fireMark = new Label(i == 7 ? "🔥 Củi" : "🔥");
                    fireMark.setStyle(
                        "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10.5px; -fx-text-fill: rgba(255,180,96,0.48);"
                    );
                    slot.getChildren().add(fireMark);
                }
            } else {
                slot.setStyle(
                    "-fx-background-color: #6B4524; -fx-border-color: #F4BE5E; -fx-border-width: 2.2px;" +
                    "-fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;"
                );
                VBox content = new VBox(2);
                content.setAlignment(Pos.CENTER);
                Image img = Assets.getShopIcon(item);
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(42);
                    iv.setFitHeight(42);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(false);
                    content.getChildren().add(iv);
                }
                Label lbl = new Label(CookingRecipeManager.getVietnameseDisplayName(item));
                lbl.setMaxWidth(68);
                lbl.setAlignment(Pos.CENTER);
                lbl.setStyle(
                    "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #FFF3D2;"
                );
                content.getChildren().add(lbl);
                slot.getChildren().add(content);
            }
        }
    }

    private void evaluateAndRenderResult() {
        currentMatch = CookingRecipeManager.evaluateGrid(craftGrid);
        resultSlotPane.getChildren().clear();

        if (currentMatch.isMatched()) {
            CookedFoodItem out = currentMatch.getOutput();
            int owned = (player != null) ? player.getInventory().getItemCount(out) : 0;

            resultSlotPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #7F4E1E, #542F10);" +
                "-fx-border-color: #FFD95A;" +
                "-fx-border-width: 3px;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,204,72,0.55), 14, 0.35, 0, 0);"
            );
            Image img = Assets.getShopIcon(out);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(68);
                iv.setFitHeight(68);
                iv.setPreserveRatio(true);
                iv.setSmooth(false);
                resultSlotPane.getChildren().add(iv);
            }
            resultSlotPane.getChildren().add(resultFlashRect);

            resultNameLabel.setText(out.getName() + (owned > 0 ? " (Kho: " + owned + ")" : ""));
            resultStatsLabel.setText(out.getBuffDescription() + "  •  Bán $" + out.getPrice());
            recipeStatusLabel.setText(currentMatch.getHintMessage());
            recipeStatusLabel.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #98FF98; -fx-text-alignment: center;"
            );

            craftButton.setDisable(false);
            craftButton.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: #2B1304;" +
                "-fx-background-color: linear-gradient(to bottom, #FFD95A, #F29B2E);" +
                "-fx-border-color: #5A2B0A; -fx-border-width: 2.5px;" +
                "-fx-background-radius: 9px; -fx-border-radius: 9px; -fx-cursor: hand;"
            );

            eatNowButton.setDisable(owned <= 0);
            eatNowButton.setText(owned > 0 ? ("🍽 Ăn ngay (" + owned + " sẵn có)") : "🍽 Ăn ngay (Chưa nấu sẵn)");
            eatNowButton.setStyle(owned > 0
                ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FFF8E6; -fx-background-color: linear-gradient(to bottom, #4DA648, #2F752B); -fx-border-color: #1A4418; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
                : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #9A846F; -fx-background-color: #3D2719; -fx-border-color: #26160D; -fx-border-width: 1.5px; -fx-background-radius: 8px; -fx-border-radius: 8px;"
            );
        } else {
            resultSlotPane.setStyle(
                "-fx-background-color: #3C2618;" +
                "-fx-border-color: #7A5434;" +
                "-fx-border-width: 2.5px;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;"
            );
            Label qMark = new Label("?");
            qMark.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: rgba(255,220,168,0.32);"
            );
            resultSlotPane.getChildren().addAll(qMark, resultFlashRect);

            resultNameLabel.setText("Chưa khớp công thức");
            resultStatsLabel.setText("Đặt đủ nguyên liệu + 1 Củi khô");
            recipeStatusLabel.setText(currentMatch.getHintMessage());
            recipeStatusLabel.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #F5C374; -fx-text-alignment: center;"
            );

            craftButton.setDisable(true);
            craftButton.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: #A48E78; -fx-background-color: #462D1D;" +
                "-fx-border-color: #2B170C; -fx-border-width: 2px;" +
                "-fx-background-radius: 9px; -fx-border-radius: 9px;"
            );

            eatNowButton.setDisable(true);
            eatNowButton.setText("🍽 Ăn ngay (hoặc bấm [Q] ngoài đồng)");
            eatNowButton.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: #9A846F; -fx-background-color: #3D2719;" +
                "-fx-border-color: #26160D; -fx-border-width: 1.5px;" +
                "-fx-background-radius: 8px; -fx-border-radius: 8px;"
            );
        }
    }

    private void rebuildRecipeGuide() {
        recipeGuideRow.getChildren().clear();
        for (CookingRecipe recipe : CookingRecipeManager.getAllRecipes()) {
            CookedFoodItem dish = recipe.createOutput();
            VBox card = new VBox(3);
            card.setPrefWidth(190);
            card.setPadding(new Insets(6, 7, 6, 7));
            card.setStyle(
                "-fx-background-color: #392112;" +
                "-fx-border-color: #8E592C;" +
                "-fx-border-width: 1.5px;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;" +
                "-fx-cursor: hand;"
            );

            HBox topLine = new HBox(6);
            topLine.setAlignment(Pos.CENTER_LEFT);
            Image img = Assets.getShopIcon(dish);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(24);
                iv.setFitHeight(24);
                iv.setPreserveRatio(true);
                iv.setSmooth(false);
                topLine.getChildren().add(iv);
            }
            Label nameLbl = new Label(dish.getName());
            nameLbl.setWrapText(true);
            nameLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #FFE79A;"
            );
            topLine.getChildren().add(nameLbl);

            Label formulaLbl = new Label(recipe.getFormulaSummary());
            formulaLbl.setWrapText(true);
            formulaLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 10px; -fx-text-fill: #F2D8B3;"
            );

            Label statLbl = new Label(recipe.getStatSummary());
            statLbl.setWrapText(true);
            statLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 9.8px; -fx-text-fill: #9EE88E;"
            );

            card.getChildren().addAll(topLine, formulaLbl, statLbl);
            card.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    quickFillRecipe(recipe);
                }
            });

            HBox.setHgrow(card, Priority.ALWAYS);
            recipeGuideRow.getChildren().add(card);
        }
    }

    public void closeScreen() {
        clearCraftGrid();
        if (onClose != null) {
            onClose.run();
        }
    }
}
