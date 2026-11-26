package view.ui;

import controller.shopmanager.ItemManager;
import controller.shopmanager.ShopController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.game_item.AnimalFeedItem;
import model.game_item.AnimalProductItem;
import model.game_item.BabyChickenItem;
import model.game_item.BabyCowItem;
import model.game_item.BabyPigItem;
import model.game_item.CookedFoodItem;
import model.game_item.FishItem;
import model.game_item.FishSpecies;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import model.quest.Quest;
import model.quest.QuestManager;
import model.weather.WeatherType;
import view.assets.Assets;
import view.fx.SoundHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Cuốn Sổ Quản Lý Nông Trại duy nhất (GameBookOverlay — 920x580, phím TAB để mở/đóng, phím 1-5 chuyển Tab):
 * - Tab 1: Kho Đồ (Storage) — xem & bán nông sản, cá, sản phẩm thú, đồ ăn đã nấu, đổi thức ăn chăn nuôi.
 * - Tab 2: Cửa Hàng (Shop) — mua hạt giống, phân bón, vật nuôi & thức ăn chăn nuôi.
 * - Tab 3: Nhiệm Vụ (Quests) — chi tiết Nhiệm vụ #N/1000, thanh tiến độ, Nhận thưởng & xem trước 2 nhiệm vụ kế tiếp.
 * - Tab 4: Sổ Cá (Fishing Guide) — tra cứu cá theo thời tiết & khung giờ hiện tại + kỷ lục đã câu.
 * - Tab 5: Cài Đặt (Settings) — thanh trượt âm lượng tổng, bật/tắt SFX & nhạc nền, bảng phím tắt đầy đủ.
 */
public class GameBookOverlay extends StackPane {

    public static final int TAB_STORAGE  = 0;
    public static final int TAB_SHOP     = 1;
    public static final int TAB_QUESTS   = 2;
    public static final int TAB_FISHING  = 3;
    public static final int TAB_SETTINGS = 4;

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    private final Player player;
    private final ShopController shop;
    private final Supplier<WeatherType> weatherSupplier;
    private final IntSupplier hourSupplier;
    private final Supplier<String> clockSupplier;
    private final Map<FishSpecies, Integer> fishCaughtCounts;
    private final Map<FishSpecies, Double> fishMaxCm;
    private final Runnable onClaimQuestCallback;
    private final Runnable onCloseCallback;

    private final Button[] tabButtons = new Button[5];
    private final StackPane contentHolder = new StackPane();
    private final Label moneyHeaderLabel = new Label("$0");
    private final Label statusBannerLabel = new Label("Mẹo: Bấm phím [1]–[5] để chuyển trang nhanh, bấm [TAB] hoặc [ESC] để đóng sổ.");
    private int activeTabIndex = TAB_STORAGE;
    private int activeShopCategory = 0; // 0=Tất cả (chia khu riêng), 1=Cây trồng, 2=Con vật, 3=Thức ăn thú, 4=Phân bón

    public GameBookOverlay(Player player,
                           ShopController shop,
                           Supplier<WeatherType> weatherSupplier,
                           IntSupplier hourSupplier,
                           Supplier<String> clockSupplier,
                           Map<FishSpecies, Integer> fishCaughtCounts,
                           Map<FishSpecies, Double> fishMaxCm,
                           Runnable onClaimQuestCallback,
                           Runnable onCloseCallback,
                           int initialTab) {
        this.player = player;
        this.shop = shop;
        this.weatherSupplier = weatherSupplier;
        this.hourSupplier = hourSupplier;
        this.clockSupplier = clockSupplier;
        this.fishCaughtCounts = fishCaughtCounts;
        this.fishMaxCm = fishMaxCm;
        this.onClaimQuestCallback = onClaimQuestCallback;
        this.onCloseCallback = onCloseCallback;

        setStyle("-fx-background-color: rgba(16, 10, 6, 0.66);");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        // Khung Cuốn Sổ Gỗ / Giấy Ấm Áp chuẩn 920x580 căn giữa màn hình 1280x720
        BorderPane bookFrame = new BorderPane();
        bookFrame.setMaxSize(920, 580);
        bookFrame.setPrefSize(920, 580);
        bookFrame.setPadding(new Insets(14, 18, 14, 18));
        bookFrame.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #7E4E2B 0%, #5B351A 100%);" +
            "-fx-border-color: #2E1608;" +
            "-fx-border-width: 5px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.72), 24, 0.28, 0, 7);"
        );

        // ======= CẠNH TRÊN: 5 NÚT TAB + VÍ TIỀN + NÚT ĐÓNG =======
        HBox topTabsRow = buildTopTabBar();
        bookFrame.setTop(topTabsRow);

        // ======= TRANG GIẤY NỘI DUNG Ở GIỮA =======
        contentHolder.setPadding(new Insets(12, 14, 12, 14));
        contentHolder.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #FBF2DC 0%, #F2E2BD 100%);" +
            "-fx-border-color: #9C6B3B;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;"
        );
        BorderPane.setMargin(contentHolder, new Insets(10, 0, 8, 0));
        bookFrame.setCenter(contentHolder);

        // ======= THANH TRẠNG THÁI DƯỚI CÙNG =======
        lockNoEllipsis(statusBannerLabel);
        statusBannerLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FFEBC2;"
        );
        HBox footer = new HBox(statusBannerLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(2, 4, 0, 4));
        bookFrame.setBottom(footer);

        getChildren().add(bookFrame);

        if (player != null) {
            moneyHeaderLabel.textProperty().bind(player.moneyProperty().asString("💰 $%d"));
        }

        selectTab(initialTab);

        setOnMouseClicked(ev -> {
            if (ev.getTarget() == this) {
                closeBook();
            }
        });

        // Bắt phím ở mức EventFilter để phím TAB không bị JavaFX nuốt làm chuyển focus
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            KeyCode code = ev.getCode();
            if (code == KeyCode.TAB || code == KeyCode.ESCAPE) {
                ev.consume();
                closeBook();
            } else if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) {
                ev.consume();
                SoundHook.playMenuClick();
                selectTab(TAB_STORAGE);
            } else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) {
                ev.consume();
                SoundHook.playMenuClick();
                selectTab(TAB_SHOP);
            } else if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) {
                ev.consume();
                SoundHook.playMenuClick();
                selectTab(TAB_QUESTS);
            } else if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) {
                ev.consume();
                SoundHook.playMenuClick();
                selectTab(TAB_FISHING);
            } else if (code == KeyCode.DIGIT5 || code == KeyCode.NUMPAD5) {
                ev.consume();
                SoundHook.playMenuClick();
                selectTab(TAB_SETTINGS);
            }
        });
    }

    private HBox buildTopTabBar() {
        String[] labels = {
            "1. Kho Đồ",
            "2. Cửa Hàng",
            "3. Nhiệm Vụ",
            "4. Sổ Cá",
            "5. Cài Đặt"
        };

        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < labels.length; i++) {
            final int tabIdx = i;
            Button btn = new Button(labels[i]);
            btn.setFocusTraversable(false);
            lockNoEllipsis(btn);
            btn.setPrefHeight(36);
            btn.setOnAction(e -> {
                SoundHook.playMenuClick();
                selectTab(tabIdx);
            });
            tabButtons[i] = btn;
            row.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lockNoEllipsis(moneyHeaderLabel);
        moneyHeaderLabel.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #FFE680;" +
            "-fx-background-color: rgba(30, 16, 8, 0.88);" +
            "-fx-border-color: #DDA344; -fx-border-width: 2px;" +
            "-fx-padding: 5 12 5 12; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setFocusTraversable(false);
        lockNoEllipsis(closeBtn);
        closeBtn.setMinSize(36, 36);
        closeBtn.setPrefSize(36, 36);
        closeBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0;" +
            "-fx-text-fill: #FFF7EB; -fx-background-color: linear-gradient(to bottom, #D24338, #9C261D);" +
            "-fx-border-color: #42100C; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> closeBook());

        row.getChildren().addAll(spacer, moneyHeaderLabel, closeBtn);
        return row;
    }

    public void selectTab(int tabIndex) {
        this.activeTabIndex = Math.max(0, Math.min(4, tabIndex));
        for (int i = 0; i < tabButtons.length; i++) {
            boolean active = (i == activeTabIndex);
            tabButtons[i].setStyle(
                "-fx-font-family: " + FONT_MONO + ";" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-padding: 6 13 6 13;" +
                "-fx-text-fill: " + (active ? "#3B1D06;" : "#FCEFD4;") +
                "-fx-background-color: " + (active
                        ? "linear-gradient(to bottom, #FFE27A 0%, #F2B33D 100%);"
                        : "linear-gradient(to bottom, #6A3E20 0%, #4E2C15 100%);") +
                "-fx-border-color: " + (active ? "#FFF6C2;" : "#331B0B;") +
                "-fx-border-width: 2px; -fx-background-radius: 9px; -fx-border-radius: 9px; -fx-cursor: hand;"
            );
        }

        switch (activeTabIndex) {
            case TAB_STORAGE:  contentHolder.getChildren().setAll(buildStorageTab());  break;
            case TAB_SHOP:     contentHolder.getChildren().setAll(buildShopTab());     break;
            case TAB_QUESTS:   contentHolder.getChildren().setAll(buildQuestsTab());   break;
            case TAB_FISHING:  contentHolder.getChildren().setAll(buildFishingTab());  break;
            case TAB_SETTINGS: contentHolder.getChildren().setAll(buildSettingsTab()); break;
            default: break;
        }
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void refreshCurrentTab() {
        selectTab(activeTabIndex);
    }

    private void closeBook() {
        SoundHook.playMenuClick();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    // =========================================================================
    // TAB 1: KHO ĐỒ (STORAGE)
    // =========================================================================

    private VBox buildStorageTab() {
        Label header = sectionTitle("📦 KHO ĐỒ NÔNG TRẠI — XEM & BÁN VẬT PHẨM");

        Button sellAllBtn = createWoodActionBtn("💰 Bán hết Nông sản/Cá/Đồ ăn", "#E59819", "#B56E08");
        Button convertCropsBtn = createWoodActionBtn("🌾 Đổi Nông sản -> Thức ăn thú", "#4A934A", "#2F6B31");

        sellAllBtn.setOnAction(e -> {
            if (player == null) return;
            List<Item> sellables = new ArrayList<>();
            for (Map.Entry<Item, Integer> en : player.getInventory().getItems().entrySet()) {
                Item it = en.getKey();
                if (en.getValue() > 0 && isSellableProduce(it)) {
                    sellables.add(it);
                }
            }
            if (sellables.isEmpty()) {
                setStatus("Kho chưa có nông sản, cá hoặc món ăn nào để bán!");
                return;
            }
            int totalEarned = 0;
            int totalCount = 0;
            for (Item it : sellables) {
                int q = player.getInventory().getItemCount(it);
                if (q > 0) {
                    totalEarned += q * it.getPrice();
                    totalCount += q;
                    player.getInventory().removeItem(it, q);
                }
            }
            player.addMoney(totalEarned);
            SoundHook.playPickup();
            setStatus("Đã bán tổng cộng " + totalCount + " vật phẩm thu về +$" + totalEarned + "!");
            selectTab(TAB_STORAGE);
        });

        convertCropsBtn.setOnAction(e -> {
            if (player == null) return;
            List<HarvestedCropItem> crops = new ArrayList<>();
            for (Map.Entry<Item, Integer> en : player.getInventory().getItems().entrySet()) {
                if (en.getValue() > 0 && en.getKey() instanceof HarvestedCropItem) {
                    crops.add((HarvestedCropItem) en.getKey());
                }
            }
            if (crops.isEmpty()) {
                setStatus("Chưa có nông sản thu hoạch nào để chế biến thành thức ăn chăn nuôi!");
                return;
            }
            int total = 0;
            for (HarvestedCropItem c : crops) {
                int q = player.getInventory().getItemCount(c);
                if (q > 0) {
                    player.getInventory().removeItem(c, q);
                    total += q;
                }
            }
            Item feed = player.getInventory().findByName("Animal Feed");
            if (feed == null) feed = new AnimalFeedItem();
            player.getInventory().addItem(feed, total);
            SoundHook.playPickup();
            setStatus("Đã chế biến " + total + " nông sản thành " + total + " phần Animal Feed!");
            selectTab(TAB_STORAGE);
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(10, header, topSpacer, convertCropsBtn, sellAllBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane grid = new FlowPane(12, 10);
        grid.setPrefWrapLength(780);

        int countShown = 0;
        if (player != null) {
            for (Map.Entry<Item, Integer> en : new ArrayList<>(player.getInventory().getItems().entrySet())) {
                Item item = en.getKey();
                int qty = en.getValue();
                if (qty <= 0) continue;
                countShown++;
                grid.getChildren().add(createStorageItemCard(item, qty));
            }
        }

        if (countShown == 0) {
            Label empty = new Label("Kho đồ đang trống. Hãy ra đồng thu hoạch nông sản, câu cá dưới sông hoặc ghé Tab [2. Cửa Hàng] mua hạt giống nhé!");
            empty.setWrapText(true);
            empty.setMaxWidth(780);
            empty.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-text-fill: #6B4423; -fx-padding: 24;");
            grid.getChildren().add(empty);
        }

        ScrollPane scroll = wrapScroll(grid);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(10, topRow, scroll);
    }

    private boolean isSellableProduce(Item item) {
        return item instanceof HarvestedCropItem
                || item instanceof FishItem
                || item instanceof AnimalProductItem
                || item instanceof CookedFoodItem;
    }

    private HBox createStorageItemCard(Item item, int qty) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(item), 34);

        Label name = new Label(item.getName() + "  ×" + qty);
        lockNoEllipsis(name);
        name.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #3A1F0A;");

        Label sub = new Label("Giá bán: $" + item.getPrice() + "/món  •  Tổng: $" + (item.getPrice() * qty));
        lockNoEllipsis(sub);
        sub.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-text-fill: #6D4626;");

        HBox btnRow = new HBox(6);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        boolean canSell = isSellableProduce(item) || item instanceof AnimalFeedItem;
        if (canSell && item.getPrice() > 0) {
            Button sellOne = createSmallBtn("Bán 1 (+$" + item.getPrice() + ")", "#E5A124", "#B8780E");
            sellOne.setOnAction(e -> {
                if (player.getInventory().removeItem(item, 1)) {
                    player.addMoney(item.getPrice());
                    SoundHook.playPickup();
                    setStatus("Đã bán 1× " + item.getName() + " nhận +$" + item.getPrice() + "!");
                    selectTab(TAB_STORAGE);
                }
            });

            Button sellAll = createSmallBtn("Bán hết (+$" + (item.getPrice() * qty) + ")", "#2F9A4E", "#1E6E36");
            sellAll.setOnAction(e -> {
                int cur = player.getInventory().getItemCount(item);
                if (cur > 0 && player.getInventory().removeItem(item, cur)) {
                    int gain = cur * item.getPrice();
                    player.addMoney(gain);
                    SoundHook.playPickup();
                    setStatus("Đã bán " + cur + "× " + item.getName() + " nhận +$" + gain + "!");
                    selectTab(TAB_STORAGE);
                }
            });
            btnRow.getChildren().addAll(sellOne, sellAll);

            if (item instanceof HarvestedCropItem) {
                Button toFeed = createSmallBtn("Đổi Thức ăn", "#7B5B3A", "#573E26");
                toFeed.setOnAction(e -> {
                    if (player.getInventory().removeItem(item, 1)) {
                        Item feed = player.getInventory().findByName("Animal Feed");
                        if (feed == null) feed = new AnimalFeedItem();
                        player.getInventory().addItem(feed, 1);
                        SoundHook.playMenuClick();
                        setStatus("Đã đổi 1× " + item.getName() + " thành 1 Animal Feed!");
                        selectTab(TAB_STORAGE);
                    }
                });
                btnRow.getChildren().add(toFeed);
            }
        } else {
            Label supplyBadge = new Label("Vật tư canh tác / Hạt giống");
            lockNoEllipsis(supplyBadge);
            supplyBadge.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #2E6F40;");
            btnRow.getChildren().add(supplyBadge);
        }

        VBox info = new VBox(4, name, sub, btnRow);
        HBox card = new HBox(12, icon, info);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(382);
        card.setPadding(new Insets(9, 12, 9, 12));
        card.setStyle(
            "-fx-background-color: #FFFDF7;" +
            "-fx-border-color: #C8A273; -fx-border-width: 1.8px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );
        return card;
    }

    // =========================================================================
    // TAB 2: CỬA HÀNG (SHOP — TÁCH RIÊNG KHU CÂY TRỒNG, CON VẬT, THỨC ĂN THÚ, PHÂN BÓN)
    // =========================================================================

    private static final class ShopEntryPair {
        final ItemManager manager;
        final Item proto;

        ShopEntryPair(ItemManager manager, Item proto) {
            this.manager = manager;
            this.proto = proto;
        }
    }

    private int classifyShopItem(Item proto) {
        if (proto instanceof Seed) {
            return 1; // Cây trồng (Hạt giống)
        }
        if (proto instanceof BabyChickenItem || proto instanceof BabyPigItem || proto instanceof BabyCowItem
                || proto.getName().toLowerCase().contains("baby")) {
            return 2; // Con vật nuôi
        }
        if (proto instanceof AnimalFeedItem || proto.getName().toLowerCase().contains("feed")) {
            return 3; // Thức ăn cho vật nuôi
        }
        return 4; // Phân bón & Vật tư khác
    }

    private VBox buildShopTab() {
        List<ShopEntryPair> seedsList = new ArrayList<>();
        List<ShopEntryPair> animalsList = new ArrayList<>();
        List<ShopEntryPair> feedsList = new ArrayList<>();
        List<ShopEntryPair> fertsList = new ArrayList<>();

        if (shop != null) {
            for (ItemManager entry : shop.getCatalog()) {
                Item proto = entry.create();
                if (proto == null) continue;
                ShopEntryPair pair = new ShopEntryPair(entry, proto);
                switch (classifyShopItem(proto)) {
                    case 1:  seedsList.add(pair);   break;
                    case 2:  animalsList.add(pair); break;
                    case 3:  feedsList.add(pair);   break;
                    default: fertsList.add(pair);   break;
                }
            }
        }

        // Thanh chọn nhanh danh mục riêng biệt ngay đầu Cửa Hàng
        HBox catBar = new HBox(8);
        catBar.setAlignment(Pos.CENTER_LEFT);

        String[] catNames = {
            "📋 Tất Cả Khu",
            "🌱 Cây Trồng (" + seedsList.size() + ")",
            "🐄 Con Vật (" + animalsList.size() + ")",
            "🌾 Thức Ăn Thú (" + feedsList.size() + ")",
            "🧪 Phân Bón (" + fertsList.size() + ")"
        };

        for (int ci = 0; ci < catNames.length; ci++) {
            final int catIdx = ci;
            boolean sel = (activeShopCategory == catIdx);
            Button cBtn = new Button(catNames[ci]);
            cBtn.setFocusTraversable(false);
            lockNoEllipsis(cBtn);
            cBtn.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-padding: 5 12 5 12;" +
                "-fx-text-fill: " + (sel ? "#FFFDF7;" : "#4A270D;") +
                "-fx-background-color: " + (sel
                        ? "linear-gradient(to bottom, #3A8E42, #25642B);"
                        : "linear-gradient(to bottom, #FFF8E8, #EDD8B4);") +
                "-fx-border-color: " + (sel ? "#1B4720;" : "#B88E58;") +
                "-fx-border-width: 1.8px; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-cursor: hand;"
            );
            cBtn.setOnAction(e -> {
                activeShopCategory = catIdx;
                SoundHook.playMenuClick();
                selectTab(TAB_SHOP);
            });
            catBar.getChildren().add(cBtn);
        }

        VBox sectionsContainer = new VBox(14);
        sectionsContainer.setPadding(new Insets(2, 4, 8, 2));

        if ((activeShopCategory == 0 || activeShopCategory == 1) && !seedsList.isEmpty()) {
            sectionsContainer.getChildren().add(buildShopCategorySection(
                "🌱 HẠT GIỐNG CÂY TRỒNG",
                "Gieo trồng trên các luống đất đã cuốc trong khu trồng trọt",
                "#2E7D32", "#E8F5E9", seedsList
            ));
        }
        if ((activeShopCategory == 0 || activeShopCategory == 2) && !animalsList.isEmpty()) {
            sectionsContainer.getChildren().add(buildShopCategorySection(
                "🐄 CON VẬT NUÔI (THÚ NON)",
                "Mua thú non rồi cầm Tay không bấm vào bãi cỏ khu chuồng để thả nuôi",
                "#C65D17", "#FFF3E0", animalsList
            ));
        }
        if ((activeShopCategory == 0 || activeShopCategory == 3) && !feedsList.isEmpty()) {
            sectionsContainer.getChildren().add(buildShopCategorySection(
                "🌾 THỨC ĂN CHĂN NUÔI",
                "Cho gà, lợn, bò ăn mỗi ngày hoặc đổ vào máng để thú mau lớn & cho sản phẩm",
                "#8D5B24", "#FBF3E4", feedsList
            ));
        }
        if ((activeShopCategory == 0 || activeShopCategory == 4) && !fertsList.isEmpty()) {
            sectionsContainer.getChildren().add(buildShopCategorySection(
                "🧪 PHÂN BÓN & VẬT TƯ CANH TÁC",
                "Bổ sung dinh dưỡng cho đất giúp cây trồng phát triển khỏe mạnh",
                "#1E6B7B", "#E6F4F7", fertsList
            ));
        }

        ScrollPane scroll = wrapScroll(sectionsContainer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(10, catBar, scroll);
    }

    private VBox buildShopCategorySection(String titleText,
                                          String subtitleText,
                                          String accentHex,
                                          String headerBgHex,
                                          List<ShopEntryPair> entries) {
        Label secTitle = new Label(titleText + " (" + entries.size() + " mặt hàng)");
        lockNoEllipsis(secTitle);
        secTitle.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label secSub = new Label(subtitleText);
        lockNoEllipsis(secSub);
        secSub.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #634427;");

        HBox secHeader = new HBox(10, secTitle, spacer, secSub);
        secHeader.setAlignment(Pos.CENTER_LEFT);
        secHeader.setPadding(new Insets(6, 12, 6, 12));
        secHeader.setStyle(
            "-fx-background-color: " + headerBgHex + ";" +
            "-fx-border-color: " + accentHex + ";" +
            "-fx-border-width: 0 0 0 4px;" +
            "-fx-background-radius: 6px; -fx-border-radius: 4px;"
        );

        FlowPane grid = new FlowPane(12, 10);
        grid.setPrefWrapLength(765);
        for (ShopEntryPair pair : entries) {
            grid.getChildren().add(createShopCard(pair.manager, pair.proto, accentHex));
        }

        VBox box = new VBox(9, secHeader, grid);
        box.setPadding(new Insets(10, 12, 12, 12));
        box.setStyle(
            "-fx-background-color: rgba(255, 252, 244, 0.82);" +
            "-fx-border-color: #D2B48C; -fx-border-width: 1.5px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );
        return box;
    }

    private HBox createShopCard(ItemManager entry, Item proto, String accentHex) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(proto), 38);
        int owned = countOwnedByName(proto.getName());

        Label name = new Label(proto.getName() + "  •  $" + proto.getPrice());
        lockNoEllipsis(name);
        name.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #3A1F0A;");

        Label stats = new Label("Đang có trong kho: " + owned);
        lockNoEllipsis(stats);
        stats.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";");

        Label detail = new Label(proto.getStats());
        detail.setWrapText(true);
        detail.setMaxWidth(295);
        detail.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11px; -fx-text-fill: #7B5230;");

        Button buy1 = createSmallBtn("Mua ×1 ($" + proto.getPrice() + ")", "#E59819", "#B56E08");
        buy1.setOnAction(e -> {
            shop.buyItem(entry, 1);
            SoundHook.playPickup();
            setStatus(shop.getLastMessage());
            selectTab(TAB_SHOP);
        });

        Button buy5 = createSmallBtn("Mua ×5 ($" + (proto.getPrice() * 5) + ")", "#388E3C", "#236326");
        buy5.setOnAction(e -> {
            shop.buyItem(entry, 5);
            SoundHook.playPickup();
            setStatus(shop.getLastMessage());
            selectTab(TAB_SHOP);
        });

        HBox actions = new HBox(8, buy1, buy5);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox right = new VBox(4, name, stats, detail, actions);
        HBox card = new HBox(12, icon, right);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(374);
        card.setPadding(new Insets(9, 12, 9, 12));
        card.setStyle(
            "-fx-background-color: #FFFDF7;" +
            "-fx-border-color: #C8A273; -fx-border-width: 1.8px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );
        return card;
    }

    private int countOwnedByName(String itemName) {
        if (player == null || itemName == null) return 0;
        Item found = player.getInventory().findByName(itemName);
        return (found != null) ? player.getInventory().getItemCount(found) : 0;
    }

    // =========================================================================
    // TAB 3: NHIỆM VỤ (QUESTS #1 -> #1000 & XEM TRƯỚC 2 NHIỆM VỤ KẾ TIẾP)
    // =========================================================================

    private VBox buildQuestsTab() {
        Label header = sectionTitle("📜 CHUỖI 1000 NHIỆM VỤ NÔNG TRẠI TỰ ĐỘNG");

        QuestManager qm = (player != null) ? player.getQuestManager() : null;
        if (qm == null || qm.isAllCompleted()) {
            Label doneLbl = new Label("👑 CHÚC MỪNG! Bạn đã hoàn thành xuất sắc trọn bộ 1000 / 1000 Nhiệm vụ Nông Trại!");
            doneLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #B86208; -fx-padding: 30;");
            return new VBox(14, header, doneLbl);
        }

        Quest cur = qm.getCurrentQuest();
        int prog = qm.getCurrentQuestProgress();
        int target = cur.getTargetAmount();
        boolean ready = (prog >= target);
        double ratio = Math.max(0.0, Math.min(1.0, (double) prog / target));

        // --- Thẻ Nhiệm Vụ Hiện Tại ---
        Label badge = new Label("NHIỆM VỤ HIỆN TẠI  •  #" + cur.getIndex() + " / " + QuestManager.MAX_QUESTS +
                (cur.isSpecialMilestone() ? "   ★ MỐC ĐẶC BIỆT (THƯỞNG x3)" : ""));
        lockNoEllipsis(badge);
        badge.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (cur.isSpecialMilestone() ? "#8C2300;" : "#5C300E;")
        );

        Label desc = new Label("🎯 Mục tiêu: " + cur.getDescription());
        lockNoEllipsis(desc);
        desc.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2B1506;");

        // Thanh tiến độ trực quan rộng 520px
        double barW = 520.0;
        Rectangle track = new Rectangle(barW, 18);
        track.setArcWidth(9); track.setArcHeight(9);
        track.setFill(Color.web("#E2CCA8"));
        track.setStroke(Color.web("#8E6234"));
        track.setStrokeWidth(1.6);

        Rectangle fill = new Rectangle(barW * ratio, 18);
        fill.setArcWidth(9); fill.setArcHeight(9);
        fill.setFill(ready ? Color.web("#F4B400") : Color.web("#389E48"));

        StackPane barStack = new StackPane(track, fill);
        barStack.setAlignment(Pos.CENTER_LEFT);
        barStack.setMaxWidth(barW);

        Label progTxt = new Label("Tiến độ: " + prog + " / " + target + " (" + (int) Math.round(ratio * 100) + "%)");
        lockNoEllipsis(progTxt);
        progTxt.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #1F6332;");

        HBox progRow = new HBox(14, barStack, progTxt);
        progRow.setAlignment(Pos.CENTER_LEFT);

        Label rewardLbl = new Label("💰 Phần thưởng: +$" + cur.getRewardMoney() +
                (cur.isSpecialMilestone() ? " (Đã nhân x3 thưởng mốc chia hết cho 10!)" : ""));
        lockNoEllipsis(rewardLbl);
        rewardLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #B85600;");

        Button claimBtn = createWoodActionBtn(
            ready ? ("🎁 Nhận Thưởng Ngay (+$" + cur.getRewardMoney() + ")") : "Chưa đủ tiến độ để nhận thưởng",
            ready ? "#FFB800" : "#9E8B74",
            ready ? "#D98200" : "#7A6954"
        );
        claimBtn.setDisable(!ready);
        claimBtn.setOnAction(e -> {
            if (onClaimQuestCallback != null) {
                onClaimQuestCallback.run();
                setStatus("🎉 Đã nhận thưởng thành công và mở nhiệm vụ tiếp theo!");
                selectTab(TAB_QUESTS);
            }
        });

        VBox activeCard = new VBox(11, badge, desc, progRow, rewardLbl, claimBtn);
        activeCard.setPadding(new Insets(16, 20, 16, 20));
        activeCard.setStyle(
            "-fx-background-color: " + (ready ? "linear-gradient(to bottom, #FFF9DB, #FFECB3);" : "#FFFDF7;") +
            "-fx-border-color: " + (ready ? "#FFA000;" : (cur.isSpecialMilestone() ? "#D46A1E;" : "#B88A52;")) +
            "-fx-border-width: 2.5px; -fx-background-radius: 12px; -fx-border-radius: 12px;"
        );

        // --- Khung Xem Trước 2 Nhiệm Vụ Tiếp Theo ---
        Label nextHeader = new Label("🔮 XEM TRƯỚC 2 NHIỆM VỤ TIẾP THEO:");
        lockNoEllipsis(nextHeader);
        nextHeader.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #5C3414;");

        HBox upcomingBox = new HBox(14);
        upcomingBox.setAlignment(Pos.CENTER_LEFT);
        List<Quest> nextQuests = qm.peekUpcomingQuests(2);
        if (nextQuests.isEmpty()) {
            Label noneMore = new Label("Đây đã là nhiệm vụ cuối cùng #1000!");
            noneMore.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13px; -fx-text-fill: #6D4626;");
            upcomingBox.getChildren().add(noneMore);
        } else {
            for (Quest nq : nextQuests) {
                Label nTitle = new Label(nq.getTitle() + (nq.isSpecialMilestone() ? "  [Thưởng x3]" : ""));
                lockNoEllipsis(nTitle);
                nTitle.setStyle(
                    "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (nq.isSpecialMilestone() ? "#B83300;" : "#4A270C;")
                );

                Label nDesc = new Label("• " + nq.getDescription());
                nDesc.setWrapText(true);
                nDesc.setMaxWidth(380);
                nDesc.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-text-fill: #3A210D;");

                Label nRew = new Label("Thưởng dự kiến: +$" + nq.getRewardMoney());
                lockNoEllipsis(nRew);
                nRew.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2B7A3B;");

                VBox nCard = new VBox(6, nTitle, nDesc, nRew);
                nCard.setPrefWidth(410);
                nCard.setPadding(new Insets(11, 14, 11, 14));
                nCard.setStyle(
                    "-fx-background-color: rgba(255, 250, 238, 0.88);" +
                    "-fx-border-color: " + (nq.isSpecialMilestone() ? "#E08A2E;" : "#C9AA82;") +
                    "-fx-border-width: 1.8px; -fx-background-radius: 10px; -fx-border-radius: 10px;"
                );
                upcomingBox.getChildren().add(nCard);
            }
        }

        return new VBox(12, header, activeCard, nextHeader, upcomingBox);
    }

    // =========================================================================
    // TAB 4: SỔ CÁ (FISHING GUIDE THEO THỜI TIẾT & GIỜ HIỆN TẠI)
    // =========================================================================

    private VBox buildFishingTab() {
        WeatherType w = (weatherSupplier != null && weatherSupplier.get() != null) ? weatherSupplier.get() : WeatherType.SUNNY;
        int hour = (hourSupplier != null) ? hourSupplier.getAsInt() : 8;
        String clock = (clockSupplier != null) ? clockSupplier.get() : String.format("%02d:00", hour);

        int totalCaught = 0;
        if (fishCaughtCounts != null) {
            for (int c : fishCaughtCounts.values()) totalCaught += c;
        }

        String wVi = (w == WeatherType.RAINY) ? "🌧 Trời Mưa" : (w == WeatherType.DROUGHT ? "🔥 Hạn Hán" : "☀ Trời Nắng");
        Label header = sectionTitle("🎣 SỔ CÁ NÔNG TRẠI  —  " + wVi + " • Giờ: " + clock + " • Đã câu: " + totalCaught + " con");

        VBox list = new VBox(8);
        for (FishSpecies sp : FishSpecies.values()) {
            boolean activeNow = sp.isAvailable(w, hour);
            int caught = (fishCaughtCounts != null) ? fishCaughtCounts.getOrDefault(sp, 0) : 0;
            double maxRecord = (fishMaxCm != null) ? fishMaxCm.getOrDefault(sp, 0.0) : 0.0;

            Circle dot = new Circle(7, sp.getBadgeColor());
            dot.setStroke(Color.web("#2B1A0E"));

            Label name = new Label(sp.getDisplayName() + "  [" + sp.getRarityLabel() + "]  •  Bóng Size " + sp.getShadowSize() + "  •  $" + sp.getPrice());
            lockNoEllipsis(name);
            name.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #2E1808;");

            Region spc = new Region();
            HBox.setHgrow(spc, Priority.ALWAYS);

            Label availBadge = new Label(activeNow ? "● ĐANG XUẤT HIỆN" : "○ Chờ đúng thời tiết/giờ");
            lockNoEllipsis(availBadge);
            availBadge.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (activeNow ? "#0F6B28;" : "#7A5A3A;") +
                "-fx-background-color: " + (activeNow ? "#D9F7BE;" : "#EFE3CE;") +
                "-fx-padding: 2 9 2 9; -fx-background-radius: 10px;"
            );

            HBox topLine = new HBox(8, dot, name, spc, availBadge);
            topLine.setAlignment(Pos.CENTER_LEFT);

            String recStr = (caught > 0)
                    ? String.format("Đã bắt: %d con  |  Kỷ lục: %.1f cm", caught, maxRecord)
                    : "Chưa câu được lần nào";
            Label cond = new Label("Điều kiện: " + sp.getConditionText() + "   •   " + recStr);
            lockNoEllipsis(cond);
            cond.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-text-fill: #5C3B1E;");

            VBox rowCard = new VBox(4, topLine, cond);
            rowCard.setPadding(new Insets(8, 12, 8, 12));
            rowCard.setStyle(
                "-fx-background-color: " + (activeNow ? "#FFFFFF;" : "rgba(250,242,226,0.75);") +
                "-fx-border-color: " + (activeNow ? "#4CAF50;" : "#C9AA82;") +
                "-fx-border-width: " + (activeNow ? "2px;" : "1.5px;") +
                "-fx-background-radius: 9px; -fx-border-radius: 9px;"
            );
            list.getChildren().add(rowCard);
        }

        ScrollPane scroll = wrapScroll(list);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(10, header, scroll);
    }

    // =========================================================================
    // TAB 5: CÀI ĐẶT (SETTINGS — ÂM LƯỢNG & BẢNG PHÍM TẮT)
    // =========================================================================

    private VBox buildSettingsTab() {
        Label header = sectionTitle("⚙ CÀI ĐẶT ÂM THANH & BẢNG PHÍM TẮT ĐIỀU KHIỂN");

        int curVolPct = (int) Math.round(SoundHook.getMasterVolume() * 100.0);
        Label volValueLbl = new Label("🔊 Âm lượng tổng: " + curVolPct + "%");
        lockNoEllipsis(volValueLbl);
        volValueLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3A1F0A;");

        Slider volSlider = new Slider(0, 100, curVolPct);
        volSlider.setPrefWidth(320);
        volSlider.setShowTickMarks(true);
        volSlider.setMajorTickUnit(25);
        volSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int pct = (int) Math.round(newV.doubleValue());
            SoundHook.setMasterVolume(pct / 100.0);
            volValueLbl.setText("🔊 Âm lượng tổng: " + pct + "%");
        });

        Button testSoundBtn = createSmallBtn("🔔 Nghe thử", "#3B7DD8", "#22539A");
        testSoundBtn.setOnAction(e -> SoundHook.playPickup());

        Button sfxToggleBtn = createSmallBtn(
            SoundHook.isEnabled() ? "Hiệu ứng SFX: ĐANG BẬT" : "Hiệu ứng SFX: ĐANG TẮT",
            SoundHook.isEnabled() ? "#2E9E54" : "#8E5A45",
            SoundHook.isEnabled() ? "#1D7039" : "#663D2D"
        );
        sfxToggleBtn.setOnAction(e -> {
            SoundHook.setEnabled(!SoundHook.isEnabled());
            if (SoundHook.isEnabled()) SoundHook.playMenuClick();
            selectTab(TAB_SETTINGS);
        });

        Button bgmToggleBtn = createSmallBtn(
            SoundHook.isBgmEnabled() ? "Nhạc nền Menu: ĐANG BẬT" : "Nhạc nền Menu: ĐANG TẮT",
            SoundHook.isBgmEnabled() ? "#2E9E54" : "#8E5A45",
            SoundHook.isBgmEnabled() ? "#1D7039" : "#663D2D"
        );
        bgmToggleBtn.setOnAction(e -> {
            SoundHook.setBgmEnabled(!SoundHook.isBgmEnabled());
            SoundHook.playMenuClick();
            selectTab(TAB_SETTINGS);
        });

        HBox sliderRow = new HBox(14, volValueLbl, volSlider, testSoundBtn);
        sliderRow.setAlignment(Pos.CENTER_LEFT);
        HBox toggleRow = new HBox(12, sfxToggleBtn, bgmToggleBtn);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        VBox audioCard = new VBox(10, sliderRow, toggleRow);
        audioCard.setPadding(new Insets(12, 16, 12, 16));
        audioCard.setStyle(
            "-fx-background-color: #FFFDF7; -fx-border-color: #C8A273; -fx-border-width: 1.8px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        // --- Bảng phím tắt ---
        Label keysTitle = new Label("⌨ BẢNG PHÍM TẮT TRONG GAME:");
        lockNoEllipsis(keysTitle);
        keysTitle.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #4E280D;");

        String[][] shortcuts = {
            {"W / A / S / D  hoặc  ▲▼◄►", "Di chuyển nhân vật 8 hướng / Chèo thuyền trên sông"},
            {"Phím số 1 – 6",              "Chọn nhanh: Hạt giống, Cuốc, Bình tưới, Tay, Phân bón, Túi thức ăn"},
            {"SPACE  hoặc  J",             "Cuốc đất / Gieo hạt / Tưới cây / Thu hoạch / Quăng cần & Kéo cá"},
            {"TAB",                        "Mở / Đóng Cuốn Sổ Quản Lý (bấm 1–5 chuyển nhanh các Tab)"},
            {"E",                          "Tương tác gần nhất (Ngủ, Nấu ăn, Tắm nắng, Sòng bạc, Bot, Thuyền)"},
            {"Q  /  R",                    "Ăn nhanh món đã nấu (Q)  •  Uống nước mát tại bờ sông (R)"},
            {"ESC",                        "Mở Menu Tạm Dừng (Tiếp tục, Lưu Game, Cài Đặt, Về Menu Chính)"}
        };

        GridPane keyGrid = new GridPane();
        keyGrid.setHgap(18);
        keyGrid.setVgap(7);
        keyGrid.setPadding(new Insets(12, 16, 12, 16));
        keyGrid.setStyle(
            "-fx-background-color: #FFFDF7; -fx-border-color: #C8A273; -fx-border-width: 1.8px;" +
            "-fx-background-radius: 10px; -fx-border-radius: 10px;"
        );

        for (int r = 0; r < shortcuts.length; r++) {
            Label kLbl = new Label(shortcuts[r][0]);
            lockNoEllipsis(kLbl);
            kLbl.setStyle(
                "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #5C2E0A;" +
                "-fx-background-color: #F4DFBA; -fx-border-color: #BA8C4E; -fx-border-width: 1.2px;" +
                "-fx-padding: 3 9 3 9; -fx-background-radius: 6px; -fx-border-radius: 6px;"
            );

            Label dLbl = new Label(shortcuts[r][1]);
            lockNoEllipsis(dLbl);
            dLbl.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-text-fill: #2E1A0B;");

            keyGrid.add(kLbl, 0, r);
            keyGrid.add(dLbl, 1, r);
        }

        return new VBox(10, header, audioCard, keysTitle, keyGrid);
    }

    // =========================================================================
    // UI HELPERS (KHÔNG BAO GIỜ ĐỂ DẤU ...)
    // =========================================================================

    private void setStatus(String text) {
        if (text != null && !text.isEmpty()) {
            statusBannerLabel.setText("✦ " + text);
        }
    }

    private static void lockNoEllipsis(javafx.scene.control.Labeled node) {
        node.setMinWidth(Region.USE_PREF_SIZE);
        node.setTextOverrun(OverrunStyle.CLIP);
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        lockNoEllipsis(l);
        l.setStyle("-fx-font-family: " + FONT_MONO + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #5A2D0C;");
        return l;
    }

    private static ScrollPane wrapScroll(javafx.scene.Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        return sp;
    }

    private static Button createWoodActionBtn(String text, String topHex, String botHex) {
        Button b = new Button(text);
        b.setFocusTraversable(false);
        lockNoEllipsis(b);
        b.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #FFFDF7;" +
            "-fx-padding: 6 14 6 14;" +
            "-fx-background-color: linear-gradient(to bottom, " + topHex + ", " + botHex + ");" +
            "-fx-border-color: rgba(58,28,8,0.65); -fx-border-width: 1.8px;" +
            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        );
        return b;
    }

    private static Button createSmallBtn(String text, String topHex, String botHex) {
        Button b = new Button(text);
        b.setFocusTraversable(false);
        lockNoEllipsis(b);
        b.setStyle(
            "-fx-font-family: " + FONT_MONO + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #FFFDF7;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-color: linear-gradient(to bottom, " + topHex + ", " + botHex + ");" +
            "-fx-border-color: rgba(48,22,6,0.55); -fx-border-width: 1.5px;" +
            "-fx-background-radius: 6px; -fx-border-radius: 6px; -fx-cursor: hand;"
        );
        return b;
    }
}
