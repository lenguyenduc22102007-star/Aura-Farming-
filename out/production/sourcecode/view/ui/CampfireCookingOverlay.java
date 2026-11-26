package view.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.game_item.AnimalProductItem;
import model.game_item.CookedFoodItem;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.player_inventory.Inventory;
import model.player_inventory.Player;
import view.assets.Assets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Giao diện Bảng Nấu Ăn tại Đống Lửa Trại (Campfire Cooking Overlay):
 * - Thiết kế khung gỗ ấm / giấy da pixel vừa vặn hoàn hảo trong cửa sổ 1280x720.
 * - Hiển thị 4 công thức nấu ăn gắn trực tiếp với sản phẩm Gà/Lợn/Bò và Nông sản trong Inventory.
 * - Hỗ trợ thanh tiến trình nấu ăn 1.2 giây kèm hiệu ứng khói/tia lửa và nút [Ăn ngay] kích hoạt Buff trong ngày.
 */
public class CampfireCookingOverlay extends StackPane {

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    private enum RecipeKind {
        EGG_FRY,
        PORK_GRILL,
        MILK_STEAK,
        FARM_HOTPOT
    }

    private static final class RecipeSpec {
        final RecipeKind kind;
        final String recipeId;
        final String ingredientLabel;
        final String benefitBadge;

        RecipeSpec(RecipeKind kind, String recipeId, String ingredientLabel, String benefitBadge) {
            this.kind = kind;
            this.recipeId = recipeId;
            this.ingredientLabel = ingredientLabel;
            this.benefitBadge = benefitBadge;
        }
    }

    private final Player player;
    private final Runnable onClose;
    private final Runnable onCookingFx;
    private final Consumer<CookedFoodItem> onEatFood;
    private final Consumer<String> onToast;
    private final Supplier<String> activeBuffSupplier;

    private final VBox recipeListBox;
    private final Label activeBuffLabel;
    private final Label cookingStatusLabel;
    private final ProgressBar cookingBar;
    private final HBox cookingProgressBox;
    private final Button closeBtn;

    private boolean isCooking = false;
    private Timeline cookTimeline = null;

    private final List<RecipeSpec> recipes = new ArrayList<>();

    public CampfireCookingOverlay(Player player,
                                  Supplier<String> activeBuffSupplier,
                                  Runnable onCookingFx,
                                  Consumer<CookedFoodItem> onEatFood,
                                  Consumer<String> onToast,
                                  Runnable onClose) {
        this.player = player;
        this.activeBuffSupplier = activeBuffSupplier;
        this.onCookingFx = onCookingFx;
        this.onEatFood = onEatFood;
        this.onToast = onToast;
        this.onClose = onClose;

        recipes.add(new RecipeSpec(
            RecipeKind.EGG_FRY,
            CookedFoodItem.ID_EGG_FRY,
            "1x Trứng Gà (Fresh Egg)",
            "Hồi +45 Đói  |  Bán $55 (>x2)"
        ));
        recipes.add(new RecipeSpec(
            RecipeKind.PORK_GRILL,
            CookedFoodItem.ID_PORK_GRILL,
            "1x Nấm / Thịt Lợn (Farm Truffle)",
            "Hồi +65 Đói  |  Bán $90 (>x2.2)"
        ));
        recipes.add(new RecipeSpec(
            RecipeKind.MILK_STEAK,
            CookedFoodItem.ID_MILK_STEAK,
            "1x Sữa Bò (Fresh Milk)",
            "Hồi +55 Đói, +45 Nước  |  Tăng cá hiếm  |  Bán $115 (>x2.5)"
        ));
        recipes.add(new RecipeSpec(
            RecipeKind.FARM_HOTPOT,
            CookedFoodItem.ID_FARM_HOTPOT,
            "1x SP Gà/Lợn/Bò + 1x Nông sản (Cà chua/Khoai/Ngô...)",
            "Hồi +100 Đói, +50 Nước  |  Tăng mạnh cá hiếm  |  Bán $220 (x3)"
        ));

        setStyle("-fx-background-color: rgba(18, 10, 6, 0.64);");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        setOnMouseClicked(e -> {
            if (e.getTarget() == this && !isCooking) {
                close();
            }
        });
        setOnKeyPressed(e -> {
            if ((e.getCode() == KeyCode.E || e.getCode() == KeyCode.ESCAPE) && !isCooking) {
                e.consume();
                close();
            }
        });

        // Khung gỗ chính gọn gàng 860 x 585 (vừa khít trong 1280x720, không che nút X)
        VBox panel = new VBox(10);
        panel.setMaxWidth(860);
        panel.setPrefWidth(860);
        panel.setMaxHeight(585);
        panel.setPrefHeight(585);
        panel.setPadding(new Insets(15, 18, 15, 18));
        panel.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #7E4822 0%, #562E14 100%);" +
            "-fx-border-color: #2A1408;" +
            "-fx-border-width: 4px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.68), 18, 0.25, 0, 6);"
        );

        // ==================== HEADER ====================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🔥 BẾP LỬA TRẠI — NẤU ĂN");
        title.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE9A8;" +
            "-fx-effect: dropshadow(one-pass-box, #2B1306, 2, 1.0, 0, 2);"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        activeBuffLabel = new Label();
        activeBuffLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF6D6;" +
            "-fx-background-color: rgba(44, 22, 10, 0.78);" +
            "-fx-border-color: #E0A24C;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 7px;" +
            "-fx-border-radius: 7px;"
        );

        closeBtn = new Button("✕");
        closeBtn.setFocusTraversable(false);
        closeBtn.setMinWidth(34);
        closeBtn.setPrefSize(34, 32);
        closeBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF3D6;" +
            "-fx-background-color: #B43B2A;" +
            "-fx-border-color: #4D150D;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 7px;" +
            "-fx-border-radius: 7px;" +
            "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> {
            if (!isCooking) close();
        });

        header.getChildren().addAll(title, spacer, activeBuffLabel, closeBtn);

        // ==================== SUBTITLE + COOKING PROGRESS BAR ====================
        Label hint = new Label("Nấu sản phẩm Gà/Lợn/Bò để bán giá cao hơn trong Kho [I] hoặc bấm [Ăn ngay] nhận Buff cả ngày!");
        hint.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #F5DEC0;"
        );

        cookingStatusLabel = new Label("🔥 Đang nhóm lửa nấu ăn...");
        cookingStatusLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE89A;"
        );

        cookingBar = new ProgressBar(0.0);
        cookingBar.setPrefWidth(360);
        cookingBar.setPrefHeight(16);
        cookingBar.setStyle("-fx-accent: #FF942B;");

        cookingProgressBox = new HBox(12, cookingStatusLabel, cookingBar);
        cookingProgressBox.setAlignment(Pos.CENTER_LEFT);
        cookingProgressBox.setPadding(new Insets(6, 12, 6, 12));
        cookingProgressBox.setStyle(
            "-fx-background-color: rgba(42, 18, 6, 0.85);" +
            "-fx-border-color: #F2A93B;" +
            "-fx-border-width: 1.5px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );
        cookingProgressBox.setVisible(false);
        cookingProgressBox.setManaged(false);

        // ==================== RECIPE LIST ====================
        recipeListBox = new VBox(9);
        recipeListBox.setPadding(new Insets(4, 4, 4, 4));

        ScrollPane scroll = new ScrollPane(recipeListBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
            "-fx-background: #F4E2BE;" +
            "-fx-background-color: #F4E2BE;" +
            "-fx-border-color: #3A200E;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ==================== FOOTER ====================
        Label footer = new Label("Mẹo: Vuốt ve Gà / Lợn / Bò mỗi ngày để lấy Trứng, Nấm, Sữa  •  [E] hoặc [ESC] để đóng bếp");
        footer.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-text-fill: #E6C89F;"
        );

        panel.getChildren().addAll(header, hint, cookingProgressBox, scroll, footer);
        getChildren().add(panel);

        rebuildRecipes();
    }

    private void rebuildRecipes() {
        updateBuffHeader();
        recipeListBox.getChildren().clear();
        for (RecipeSpec spec : recipes) {
            recipeListBox.getChildren().add(createRecipeCard(spec));
        }
    }

    private void updateBuffHeader() {
        String buff = (activeBuffSupplier != null) ? activeBuffSupplier.get() : "";
        if (buff == null || buff.isEmpty()) {
            activeBuffLabel.setText("Buff hôm nay: Chưa có");
        } else {
            activeBuffLabel.setText("Buff: " + buff);
        }
    }

    private HBox createRecipeCard(RecipeSpec spec) {
        CookedFoodItem outputItem = CookedFoodItem.createById(spec.recipeId);
        boolean canCook = hasIngredients(spec.kind);
        int ownedDish = (player != null) ? player.getInventory().getItemCount(outputItem) : 0;

        // Icon món ăn pixel sắc nét
        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(56, 56);
        iconWrap.setMinSize(56, 56);
        iconWrap.setStyle(
            "-fx-background-color: #E7CE9F;" +
            "-fx-border-color: #7A4C24;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );
        Image foodImg = Assets.getShopIcon(outputItem);
        if (foodImg != null) {
            ImageView iv = new ImageView(foodImg);
            iv.setFitWidth(42);
            iv.setFitHeight(42);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            iconWrap.getChildren().add(iv);
        }

        // Thông tin món ăn
        Label nameLbl = new Label(outputItem.getName() + (ownedDish > 0 ? "   (Trong kho: " + ownedDish + ")" : ""));
        nameLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #3B1E0B;"
        );

        Label ingLbl = new Label("Nguyên liệu: " + spec.ingredientLabel + "  [" + ingredientStockSummary(spec.kind) + "]");
        ingLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + (canCook ? "#1E6B2C" : "#9E2A22") + ";"
        );

        Label benefitLbl = new Label(spec.benefitBadge);
        benefitLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-text-fill: #6B411E;"
        );

        VBox infoBox = new VBox(3, nameLbl, ingLbl, benefitLbl);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Nút [Nấu] và [Ăn ngay]
        Button cookBtn = new Button("🔥 Nấu");
        cookBtn.setFocusTraversable(false);
        cookBtn.setMinWidth(92);
        cookBtn.setPrefHeight(34);
        cookBtn.setDisable(!canCook || isCooking);
        cookBtn.setStyle(canCook && !isCooking
            ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; " +
              "-fx-text-fill: #FFF8E6; -fx-background-color: linear-gradient(to bottom, #EA7B26, #C45214); " +
              "-fx-border-color: #6B2608; -fx-border-width: 2px; -fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;"
            : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; " +
              "-fx-text-fill: #8E7F6F; -fx-background-color: #D2C3AC; " +
              "-fx-border-color: #9A8B76; -fx-border-width: 1.5px; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );
        cookBtn.setOnAction(e -> startCooking(spec, outputItem));

        Button eatBtn = new Button("🍽 Ăn ngay");
        eatBtn.setFocusTraversable(false);
        eatBtn.setMinWidth(104);
        eatBtn.setPrefHeight(34);
        eatBtn.setDisable(ownedDish <= 0 || isCooking);
        eatBtn.setStyle(ownedDish > 0 && !isCooking
            ? "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; " +
              "-fx-text-fill: #FFFDF5; -fx-background-color: linear-gradient(to bottom, #429E4B, #2B7634); " +
              "-fx-border-color: #194820; -fx-border-width: 2px; -fx-background-radius: 7px; -fx-border-radius: 7px; -fx-cursor: hand;"
            : "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; " +
              "-fx-text-fill: #8E7F6F; -fx-background-color: #D2C3AC; " +
              "-fx-border-color: #9A8B76; -fx-border-width: 1.5px; -fx-background-radius: 7px; -fx-border-radius: 7px;"
        );
        eatBtn.setOnAction(e -> eatDish(outputItem));

        HBox btnBox = new HBox(8, cookBtn, eatBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(12, iconWrap, infoBox, btnBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle(
            "-fx-background-color: #FBF1DA;" +
            "-fx-border-color: " + (canCook ? "#B77A38" : "#C9B38D") + ";" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 9px;" +
            "-fx-border-radius: 9px;"
        );
        return card;
    }

    private void startCooking(RecipeSpec spec, CookedFoodItem outputItem) {
        if (isCooking || player == null || !hasIngredients(spec.kind)) return;
        if (!consumeIngredients(spec.kind)) return;

        isCooking = true;
        closeBtn.setDisable(true);
        cookingStatusLabel.setText("🔥 Đang nấu " + outputItem.getName() + "...");
        cookingBar.setProgress(0.0);
        cookingProgressBox.setVisible(true);
        cookingProgressBox.setManaged(true);
        rebuildRecipes();

        if (onCookingFx != null) onCookingFx.run();

        cookTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(cookingBar.progressProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.6), ev -> {
                if (onCookingFx != null) onCookingFx.run();
            }, new KeyValue(cookingBar.progressProperty(), 0.55)),
            new KeyFrame(Duration.seconds(1.2), ev -> {
                player.getInventory().addItem(outputItem, 1);
                if (onCookingFx != null) onCookingFx.run();
                isCooking = false;
                closeBtn.setDisable(false);
                cookingProgressBox.setVisible(false);
                cookingProgressBox.setManaged(false);
                rebuildRecipes();
                if (onToast != null) {
                    onToast.accept("🔥 Nấu thành công: " + outputItem.getName() + " (Giá bán $" + outputItem.getPrice() + ")!");
                }
            }, new KeyValue(cookingBar.progressProperty(), 1.0))
        );
        cookTimeline.play();
    }

    private void eatDish(CookedFoodItem dish) {
        if (isCooking || player == null || dish == null) return;
        if (player.getInventory().getItemCount(dish) <= 0) return;
        if (player.getInventory().removeItem(dish, 1)) {
            if (onEatFood != null) {
                onEatFood.accept(dish);
            }
            rebuildRecipes();
        }
    }

    // ==================== INVENTORY MATCHING HELPERS ====================

    private Item findAnimalProduct(String exactName) {
        if (player == null) return null;
        Inventory inv = player.getInventory();
        for (Map.Entry<Item, Integer> e : inv.getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey().getName().equalsIgnoreCase(exactName)) {
                return e.getKey();
            }
        }
        return null;
    }

    private int countAnimalProduct(String exactName) {
        Item it = findAnimalProduct(exactName);
        return (it != null && player != null) ? player.getInventory().getItemCount(it) : 0;
    }

    private Item findAnyAnimalProduct() {
        if (player == null) return null;
        // Ưu tiên nguyên liệu rẻ nhất trước (Egg -> Truffle -> Milk)
        String[] order = { "Fresh Egg", "Farm Truffle", "Fresh Milk" };
        for (String name : order) {
            Item found = findAnimalProduct(name);
            if (found != null) return found;
        }
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey() instanceof AnimalProductItem) {
                return e.getKey();
            }
        }
        return null;
    }

    private int countAllAnimalProducts() {
        if (player == null) return 0;
        int sum = 0;
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey() instanceof AnimalProductItem) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    private Item findAnyHarvestedCrop() {
        if (player == null) return null;
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey() instanceof HarvestedCropItem) {
                return e.getKey();
            }
        }
        return null;
    }

    private int countAllHarvestedCrops() {
        if (player == null) return 0;
        int sum = 0;
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && e.getKey() instanceof HarvestedCropItem) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    private boolean hasIngredients(RecipeKind kind) {
        switch (kind) {
            case EGG_FRY:     return countAnimalProduct("Fresh Egg") >= 1;
            case PORK_GRILL:  return countAnimalProduct("Farm Truffle") >= 1;
            case MILK_STEAK:  return countAnimalProduct("Fresh Milk") >= 1;
            case FARM_HOTPOT: return countAllAnimalProducts() >= 1 && countAllHarvestedCrops() >= 1;
            default:          return false;
        }
    }

    private String ingredientStockSummary(RecipeKind kind) {
        switch (kind) {
            case EGG_FRY:
                return "Có " + countAnimalProduct("Fresh Egg") + "/1";
            case PORK_GRILL:
                return "Có " + countAnimalProduct("Farm Truffle") + "/1";
            case MILK_STEAK:
                return "Có " + countAnimalProduct("Fresh Milk") + "/1";
            case FARM_HOTPOT:
                return "SP Thú: " + countAllAnimalProducts() + "/1, Nông sản: " + countAllHarvestedCrops() + "/1";
            default:
                return "";
        }
    }

    private boolean consumeIngredients(RecipeKind kind) {
        Inventory inv = player.getInventory();
        switch (kind) {
            case EGG_FRY:
                return inv.removeItem(findAnimalProduct("Fresh Egg"), 1);
            case PORK_GRILL:
                return inv.removeItem(findAnimalProduct("Farm Truffle"), 1);
            case MILK_STEAK:
                return inv.removeItem(findAnimalProduct("Fresh Milk"), 1);
            case FARM_HOTPOT: {
                Item animalItem = findAnyAnimalProduct();
                Item cropItem = findAnyHarvestedCrop();
                if (animalItem == null || cropItem == null) return false;
                if (!inv.removeItem(animalItem, 1)) return false;
                if (!inv.removeItem(cropItem, 1)) {
                    inv.addItem(animalItem, 1); // rollback an toàn
                    return false;
                }
                return true;
            }
            default:
                return false;
        }
    }

    public boolean isCookingInProgress() {
        return isCooking;
    }

    private void close() {
        if (cookTimeline != null) {
            cookTimeline.stop();
        }
        if (onClose != null) {
            onClose.run();
        }
    }
}
