package view.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import model.game_item.FishSpecies;
import model.weather.WeatherType;

import java.util.Map;

/**
 * Giao diện Popup Sổ Tay & Hướng Dẫn Câu Cá (Fishing Guide Overlay):
 * - Thiết kế gỗ ấm, viền nâu đậm, chữ pixel/monospace sắc nét, vừa vặn hoàn hảo trong 1280x720.
 * - Gồm 2 Tab: (A) Hướng Dẫn & Mẹo Săn Kho Báu và (B) Danh Sách 7 Loài Cá Theo Thời Tiết & Giờ.
 * - Tự động làm nổi bật các loài đang xuất hiện ngay lúc này + đếm số lượng đã bắt trong phiên chơi.
 */
public class FishingGuideOverlay extends StackPane {

    private static final String FONT_MONO = "'Consolas', 'Courier New', monospace";

    private final Runnable onClose;
    private final VBox guideTabContent;
    private final ScrollPane fishScrollPane;
    private final Button tabFishBtn;
    private final Button tabGuideBtn;

    public FishingGuideOverlay(WeatherType currentWeather,
                               int currentHour,
                               String clockDisplay,
                               Map<FishSpecies, Integer> caughtCounts,
                               Map<FishSpecies, Double> maxCmRecords,
                               Runnable onClose) {
        this.onClose = onClose;

        // Nền tối bán trong suốt phía sau popup; bấm ra ngoài sẽ đóng sổ
        setStyle("-fx-background-color: rgba(18, 12, 8, 0.62);");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) {
                close();
            }
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.G || e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                close();
            }
        });

        // Khung gỗ chính gọn gàng cho màn hình 1280x720 (840 x 590)
        VBox panel = new VBox(12);
        panel.setMaxWidth(850);
        panel.setPrefWidth(850);
        panel.setMaxHeight(600);
        panel.setPrefHeight(600);
        panel.setPadding(new Insets(16, 18, 16, 18));
        panel.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #7A4B2A 0%, #5B351B 100%);" +
            "-fx-border-color: #2E180B;" +
            "-fx-border-width: 4px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.25, 0, 6);"
        );

        // ==================== HEADER ====================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 6, 8, 6));
        header.setStyle("-fx-border-color: transparent transparent #3E210E transparent; -fx-border-width: 0 0 2 0;");

        Label titleLbl = new Label("SỔ TAY & HƯỚNG DẪN CÂU CÁ");
        titleLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF3D1;"
        );

        // Nhãn trạng thái thời tiết & giờ hiện tại
        String weatherLabel = formatWeatherVietnamese(currentWeather);
        int totalCaught = 0;
        if (caughtCounts != null) {
            for (int v : caughtCounts.values()) totalCaught += v;
        }
        Label envBadge = new Label(weatherLabel + "  •  " + clockDisplay + "  •  Đã bắt: " + totalCaught);
        envBadge.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFE8A3;" +
            "-fx-background-color: rgba(34, 18, 10, 0.72);" +
            "-fx-border-color: #C7924F;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;"
        );

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("X");
        closeBtn.setFocusTraversable(false);
        closeBtn.setPrefSize(34, 34);
        closeBtn.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFF7EB;" +
            "-fx-background-color: #B63A2E;" +
            "-fx-border-color: #3A120E;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;" +
            "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> close());

        header.getChildren().addAll(titleLbl, headerSpacer, envBadge, closeBtn);

        // ==================== TAB BAR ====================
        HBox tabBar = new HBox(10);
        tabBar.setAlignment(Pos.CENTER_LEFT);

        tabFishBtn = new Button("Danh Sách Cá (" + FishSpecies.values().length + " Loài)");
        tabGuideBtn = new Button("Hướng Dẫn Play Together");
        tabFishBtn.setFocusTraversable(false);
        tabGuideBtn.setFocusTraversable(false);

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);

        Label shortcutHint = new Label("Nhấn [G] hoặc [ESC] để đóng");
        shortcutHint.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #E7CCA0;"
        );

        tabBar.getChildren().addAll(tabFishBtn, tabGuideBtn, tabSpacer, shortcutHint);

        // ==================== BODY CONTENT (2 TABS) ====================
        StackPane bodyStack = new StackPane();
        bodyStack.setStyle(
            "-fx-background-color: #F4E4C1;" +
            "-fx-border-color: #3E2210;" +
            "-fx-border-width: 2.5px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );
        VBox.setVgrow(bodyStack, Priority.ALWAYS);

        fishScrollPane = buildFishListTab(currentWeather, currentHour, caughtCounts, maxCmRecords);
        guideTabContent = buildGuideTab();

        bodyStack.getChildren().addAll(fishScrollPane, guideTabContent);

        tabFishBtn.setOnAction(e -> switchTab(true));
        tabGuideBtn.setOnAction(e -> switchTab(false));

        // Mặc định mở tab Danh Sách Cá để thấy ngay cá đang xuất hiện, chuyển 1 click sang Hướng Dẫn
        switchTab(true);

        panel.getChildren().addAll(header, tabBar, bodyStack);
        getChildren().add(panel);
    }

    private void switchTab(boolean showFish) {
        fishScrollPane.setVisible(showFish);
        fishScrollPane.setManaged(showFish);
        guideTabContent.setVisible(!showFish);
        guideTabContent.setManaged(!showFish);

        String activeStyle =
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #3B200E;" +
            "-fx-background-color: #F6DC9C;" +
            "-fx-border-color: #2E180B;" +
            "-fx-border-width: 2px;" +
            "-fx-padding: 6 14 6 14;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;" +
            "-fx-cursor: hand;";

        String idleStyle =
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #EED9B4;" +
            "-fx-background-color: #4A2A15;" +
            "-fx-border-color: #2E180B;" +
            "-fx-border-width: 2px;" +
            "-fx-padding: 6 14 6 14;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;" +
            "-fx-cursor: hand;";

        tabFishBtn.setStyle(showFish ? activeStyle : idleStyle);
        tabGuideBtn.setStyle(showFish ? idleStyle : activeStyle);
    }

    // ==================== PHẦN A: HƯỚNG DẪN & MẸO SĂN KHO BÁU ====================
    private VBox buildGuideTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 18, 16, 18));
        box.setAlignment(Pos.TOP_LEFT);

        box.getChildren().addAll(
            makeTipCard(
                "1. CHÈO THUYỀN & CHỌN BÓNG CÁ (5 CẤP KÍCH THƯỚC)",
                "#2E6B8A",
                "• Bấm [E] gần thuyền để lên/xuống -> Dùng [A / D] chèo dọc sông tìm bóng cá lớn (Size 1 nhỏ nhất -> Size 5 khổng lồ có vòng sáng).\n" +
                "• Bấm [Space] thả phao. Nếu bóng cá bơi tới quá nhỏ, bấm [Space] sớm lúc cá đang bơi lại để thu cần thả lại!"
            ),
            makeTipCard(
                "2. CƠ CHẾ NHẤP MỒI GIẢ & PHẢN XẠ GIẬT CẦN (!)",
                "#B8342A",
                "• Giai đoạn 1 (Nhấp mồi giả): Cá nhấp phao 1–4 lần (phao rung nhẹ, KHÔNG có '!'). Tuyệt đối KHÔNG bấm [Space] sớm kẻo cá chạy mất!\n" +
                "• Giai đoạn 2 (Cắn thật '!'): Phao chìm hẳn + hiện dấu [!] ĐỎ TO -> bấm [Space] thật nhanh! (Size 1-2: 0.9s | Size 3: 0.65s | Size 4: 0.50s | Size 5: 0.38s).\n" +
                "• Giai đoạn 3 (Size 3–5): Spam [Space] liên tục làm đầy thanh kéo lên 100% trong 2.5 giây kẻo đứt dây!"
            ),
            makeTipCard(
                "3. VƯƠNG MIỆN (x1.5 TIỀN) & VÙNG NƯỚC SỦI BỌT",
                "#B86B1A",
                "• Cá có kích thước thuộc TOP 15% lớn nhất sẽ đạt danh hiệu [VƯƠNG MIỆN] và nhân 1.5x tiền thưởng!\n" +
                "• Chèo thuyền tới các Vùng Nước Sủi Bọt lấp lánh trên sông để tăng mạnh tỉ lệ xuất hiện bóng Size 4–5 siêu hiếm."
            )
        );
        return box;
    }

    private VBox makeTipCard(String heading, String accentHex, String body) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(
            "-fx-background-color: #FFF8E7;" +
            "-fx-border-color: " + accentHex + ";" +
            "-fx-border-width: 0 0 0 5;" +
            "-fx-background-radius: 6px;" +
            "-fx-effect: dropshadow(gaussian, rgba(50,30,10,0.12), 4, 0, 0, 2);"
        );

        Label head = new Label(heading);
        head.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + accentHex + ";"
        );

        Label content = new Label(body);
        content.setWrapText(true);
        content.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #3A2312;" +
            "-fx-line-spacing: 3px;"
        );

        card.getChildren().addAll(head, content);
        return card;
    }

    /** Minh họa nhanh Thanh Lực ngay trong Sổ Hướng Dẫn để người chơi nhận diện vùng Xanh & Vàng kim. */
    private HBox makeTimingBarLegend() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));
        row.setStyle(
            "-fx-background-color: #EBD4A7;" +
            "-fx-border-color: #9C7240;" +
            "-fx-border-width: 1.5px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;"
        );

        HBox bar = new HBox(0);
        bar.setStyle("-fx-border-color: #2B160B; -fx-border-width: 2px;");
        bar.getChildren().addAll(
            new Rectangle(34, 14, Color.rgb(214, 54, 48)),
            new Rectangle(24, 14, Color.rgb(236, 186, 48)),
            new Rectangle(20, 14, Color.rgb(66, 204, 86)),
            new Rectangle(14, 14, Color.rgb(255, 218, 64)), // Ô Vàng kim Kho Báu
            new Rectangle(20, 14, Color.rgb(66, 204, 86)),
            new Rectangle(24, 14, Color.rgb(236, 186, 48)),
            new Rectangle(34, 14, Color.rgb(164, 78, 52))
        );

        Label legend = new Label("Xanh lá: Bắt cá   |   Ô Vàng kim giữa: Rương Kho Báu (+100$–200$ / hạt giống)");
        legend.setWrapText(true);
        legend.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #3F2411;"
        );

        row.getChildren().addAll(bar, legend);
        return row;
    }

    // ==================== PHẦN B: DANH SÁCH CÁ THEO THỜI TIẾT & GIỜ ====================
    private ScrollPane buildFishListTab(WeatherType weather, int hour,
                                        Map<FishSpecies, Integer> caughtCounts,
                                        Map<FishSpecies, Double> maxCmRecords) {
        VBox list = new VBox(8);
        list.setPadding(new Insets(12, 14, 12, 14));
        list.setStyle("-fx-background-color: #F4E4C1;");

        for (FishSpecies sp : FishSpecies.values()) {
            boolean available = sp.isAvailable(weather, hour);
            int caught = (caughtCounts != null) ? caughtCounts.getOrDefault(sp, 0) : 0;
            double maxCm = (maxCmRecords != null) ? maxCmRecords.getOrDefault(sp, 0.0) : 0.0;
            list.getChildren().add(makeFishCard(sp, available, caught, maxCm));
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #F4E4C1; -fx-background-color: transparent;");
        return scroll;
    }

    private HBox makeFishCard(FishSpecies sp, boolean available, int caughtCount, double maxCm) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(9, 12, 9, 12));
        card.setOpacity(available ? 1.0 : 0.60);
        card.setStyle(
            "-fx-background-color: " + (available ? "#FFFDF4" : "#E8D7B4") + ";" +
            "-fx-border-color: " + (available ? "#3D8E46" : "#A68960") + ";" +
            "-fx-border-width: " + (available ? "2px" : "1.5px") + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );

        // 1. Huy hiệu hình cá / cổ vật pixel màu theo loài
        StackPane badge = makeSpeciesBadge(sp);

        // 2. Thông tin chính (Tên + Độ hiếm + Điều kiện + Ghi chú)
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(sp.getDisplayName());
        nameLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2E190C;"
        );

        Label rarityLbl = new Label(sp.getRarityLabel());
        rarityLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: " + rarityColorHex(sp.getRarityLabel()) + ";" +
            "-fx-padding: 2 7 2 7;" +
            "-fx-background-radius: 4px;"
        );

        Label priceLbl = new Label("+" + sp.getPrice() + "$");
        priceLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #A46208;" +
            "-fx-background-color: #FFF0BE;" +
            "-fx-border-color: #D4A13A;" +
            "-fx-border-width: 1px;" +
            "-fx-padding: 1 6 1 6;" +
            "-fx-background-radius: 4px;" +
            "-fx-border-radius: 4px;"
        );

        titleRow.getChildren().addAll(nameLbl, rarityLbl, priceLbl);

        Label condLbl = new Label("Xuất hiện: " + sp.getConditionText());
        condLbl.setWrapText(true);
        condLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #54371F;"
        );

        Label noteLbl = new Label("Đặc điểm: " + sp.getNote());
        noteLbl.setWrapText(true);
        noteLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-text-fill: #694A2E;"
        );

        info.getChildren().addAll(titleRow, condLbl, noteLbl);

        // 3. Cột phải: Trạng thái hiện tại (Đang xuất hiện / Chưa đúng điều kiện) + Số lượng đã bắt
        VBox rightCol = new VBox(6);
        rightCol.setAlignment(Pos.CENTER_RIGHT);
        rightCol.setMinWidth(152);

        Label statusLbl = new Label(available ? "● Đang xuất hiện" : "○ Chưa đúng điều kiện");
        statusLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + (available ? "#1B5E20" : "#6D5B45") + ";" +
            "-fx-background-color: " + (available ? "#C8F3CC" : "#DFCFAF") + ";" +
            "-fx-border-color: " + (available ? "#3D8E46" : "#A68F6B") + ";" +
            "-fx-border-width: 1.2px;" +
            "-fx-padding: 3 8 3 8;" +
            "-fx-background-radius: 5px;" +
            "-fx-border-radius: 5px;"
        );

        Label caughtLbl = new Label("Đã bắt: " + caughtCount);
        caughtLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11.5px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + (caughtCount > 0 ? "#8B4E00" : "#7B654B") + ";"
        );

        String maxText = (maxCm > 0.0)
                ? String.format("Max: %.1f cm%s", maxCm, sp.isCrownSize(maxCm) ? " 👑" : "")
                : "Max: --";
        Label maxCmLbl = new Label(maxText);
        maxCmLbl.setStyle(
            "-fx-font-family: " + FONT_MONO + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + (maxCm > 0.0 ? "#2E6A36" : "#8A745B") + ";"
        );

        rightCol.getChildren().addAll(statusLbl, caughtLbl, maxCmLbl);

        card.getChildren().addAll(badge, info, rightCol);
        return card;
    }

    private StackPane makeSpeciesBadge(FishSpecies sp) {
        StackPane pane = new StackPane();
        pane.setMinSize(44, 44);
        pane.setPrefSize(44, 44);
        pane.setStyle(
            "-fx-background-color: #3A2212;" +
            "-fx-border-color: #B48448;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;"
        );

        if (sp == FishSpecies.CO_VAT_DAY_SONG) {
            // Biểu tượng cổ vật rương/bình cổ
            Rectangle box = new Rectangle(18, 16, sp.getBadgeColor());
            box.setStroke(Color.rgb(42, 24, 12));
            box.setStrokeWidth(1.5);
            Rectangle band = new Rectangle(18, 3, Color.rgb(255, 232, 130));
            pane.getChildren().addAll(box, band);
        } else {
            // Hình cá pixel gọn đẹp với màu đặc trưng theo loài
            Polygon body = new Polygon(
                10, 0,
                3, -7,
                -7, -5,
                -11, 0,
                -7, 5,
                3, 7
            );
            body.setFill(sp.getBadgeColor());
            body.setStroke(Color.rgb(24, 18, 14));
            body.setStrokeWidth(1.4);

            Polygon tail = new Polygon(
                -9, 0,
                -16, -6,
                -14, 0,
                -16, 6
            );
            tail.setFill(sp.getBadgeColor().darker());
            tail.setStroke(Color.rgb(24, 18, 14));
            tail.setStrokeWidth(1.2);

            Circle eye = new Circle(2.0, Color.WHITE);
            eye.setTranslateX(5);
            eye.setTranslateY(-2);
            Circle pupil = new Circle(1.0, Color.rgb(20, 20, 24));
            pupil.setTranslateX(5.5);
            pupil.setTranslateY(-2);

            pane.getChildren().addAll(tail, body, eye, pupil);
        }
        return pane;
    }

    private static String rarityColorHex(String rarity) {
        if ("Siêu hiếm".equals(rarity)) return "#C98A00";
        if ("Cực hiếm".equals(rarity))  return "#8E35B8";
        if ("Hiếm".equals(rarity))      return "#2A72B8";
        return "#5B8246";
    }

    private static String formatWeatherVietnamese(WeatherType w) {
        if (w == WeatherType.RAINY)   return "Trời Mưa";
        if (w == WeatherType.DROUGHT) return "Trời Hạn hán";
        return "Trời Nắng";
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
