package model.game_item;

import javafx.scene.paint.Color;
import model.weather.WeatherType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Danh mục các loài cá / cổ vật trên sông kèm cấp kích thước bóng (Size 1..5),
 * dải kích thước thực tế (cm), cơ chế Vương Miện (top 15% lớn nhất -> x1.5 tiền
 * thưởng)
 * và điều kiện Thời tiết / Khung giờ.
 */
public enum FishSpecies {

    CA_RO(
            "Cá Rô",
            "Trời Nắng (Sunny) – Cả ngày",
            "Thường",
            25,
            "Bóng Size 1 nhỏ, dễ bắt.",
            WeatherType.SUNNY,
            0,
            1, 12.0, 26.0,
            Color.rgb(92, 168, 118)),
    CA_CHEP(
            "Cá Chép",
            "Trời Nắng (Sunny) – Cả ngày",
            "Thường",
            30,
            "Bóng Size 2 vừa, bơi chậm.",
            WeatherType.SUNNY,
            0,
            2, 24.0, 48.0,
            Color.rgb(214, 134, 72)),
    CA_CHEP_VANG(
            "Cá Chép Vàng",
            "Trời Nắng (Sunny) – Cả ngày (dễ gặp hơn ở Vùng sủi bọt)",
            "Hiếm",
            80,
            "Bóng Size 3 lấp lánh, cần kéo nhanh tay.",
            WeatherType.SUNNY,
            0,
            3, 35.0, 68.0,
            Color.rgb(248, 204, 62)),
    LUON_MUA(
            "Lươn Mưa",
            "Trời Mưa (Rainy) – Cả ngày",
            "Hiếm",
            90,
            "Bóng Size 3 dài, chỉ ra khỏi hang khi trời đổ mưa.",
            WeatherType.RAINY,
            0,
            3, 45.0, 85.0,
            Color.rgb(88, 152, 212)),
    CA_TRE_DIEN(
            "Cá Trê Điện",
            "Trời Mưa (Rainy) – Cả ngày",
            "Cực hiếm",
            120,
            "Bóng Size 4 có viền xanh/tia chớp sáng dưới nước.",
            WeatherType.RAINY,
            0,
            4, 65.0, 115.0,
            Color.rgb(72, 218, 252)),
    CA_DEN_LONG(
            "Cá Đèn Lồng",
            "Mọi thời tiết – Chiều tối (từ 17:00 trở đi)",
            "Hiếm",
            110,
            "Bóng Size 4 phát sáng màu vàng cam lập lòe.",
            null,
            17,
            4, 50.0, 92.0,
            Color.rgb(255, 162, 56)),
    CO_VAT_DAY_SONG(
            "Cổ Vật Đáy Sông",
            "Trời Hạn hán (Drought) – Cả ngày",
            "Cực hiếm",
            150,
            "Bóng Size 5 khổng lồ, mực nước cạn lộ ra cổ vật chìm.",
            WeatherType.DROUGHT,
            0,
            5, 80.0, 135.0,
            Color.rgb(212, 166, 84)),
    CA_VUONG_MIEN(
            "Cá Vương Miện",
            "Mọi thời tiết – Bóng Size 5 khổng lồ (tỉ lệ cao ở Vùng sủi bọt)",
            "Siêu hiếm",
            180,
            "Bóng Size 5 có vòng sáng vàng kim, phản xạ 0.38s cực gắt!",
            null,
            0,
            5, 110.0, 175.0,
            Color.rgb(255, 215, 70));

    private final String displayName;
    private final String conditionText;
    private final String rarityLabel;
    private final int price;
    private final String note;
    private final WeatherType requiredWeather;
    private final int minHour;
    private final int shadowSize; // 1..5
    private final double minCm;
    private final double maxCm;
    private final Color badgeColor;

    FishSpecies(String displayName, String conditionText, String rarityLabel, int price,
            String note, WeatherType requiredWeather, int minHour,
            int shadowSize, double minCm, double maxCm, Color badgeColor) {
        this.displayName = displayName;
        this.conditionText = conditionText;
        this.rarityLabel = rarityLabel;
        this.price = price;
        this.note = note;
        this.requiredWeather = requiredWeather;
        this.minHour = minHour;
        this.shadowSize = shadowSize;
        this.minCm = minCm;
        this.maxCm = maxCm;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConditionText() {
        return conditionText;
    }

    public String getRarityLabel() {
        return rarityLabel;
    }

    public int getPrice() {
        return price;
    }

    public String getNote() {
        return note;
    }

    public int getShadowSize() {
        return shadowSize;
    }

    public double getMinCm() {
        return minCm;
    }

    public double getMaxCm() {
        return maxCm;
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    public boolean isRareOrHigher() {
        return !"Thường".equals(rarityLabel);
    }

    /**
     * Kiểm tra loài cá có đang xuất hiện theo thời tiết và giờ hiện tại hay không.
     */
    public boolean isAvailable(WeatherType weather, int currentHour) {
        WeatherType curW = (weather != null) ? weather : WeatherType.SUNNY;
        if (requiredWeather != null && requiredWeather != curW) {
            return false;
        }
        return currentHour >= minHour;
    }

    /** Random kích thước thực tế (cm), làm tròn 1 chữ số thập phân. */
    public double rollSizeCm(Random rng) {
        double raw = minCm + rng.nextDouble() * (maxCm - minCm);
        return Math.round(raw * 10.0) / 10.0;
    }

    /**
     * Top 15% lớn nhất của loài đạt danh hiệu Vương Miện (👑) và nhận x1.5 tiền
     * thưởng.
     */
    public boolean isCrownSize(double sizeCm) {
        double threshold = minCm + (maxCm - minCm) * 0.85;
        return sizeCm >= threshold;
    }

    public int computeReward(boolean isCrown) {
        return isCrown ? (int) Math.round(price * 1.5) : price;
    }

    /**
     * Chọn đúng loài cá tương ứng với cấp kích thước bóng (Size 1..5), thời tiết và
     * khung giờ hiện tại.
     */
    public static FishSpecies rollForShadowSize(int sizeTier, WeatherType weather, int currentHour, Random rng) {
        int clamped = Math.max(1, Math.min(5, sizeTier));
        List<FishSpecies> exact = new ArrayList<>();
        for (FishSpecies sp : values()) {
            if (sp.shadowSize == clamped && sp.isAvailable(weather, currentHour)) {
                exact.add(sp);
            }
        }
        if (!exact.isEmpty()) {
            return exact.get(rng.nextInt(exact.size()));
        }
        // Fallback hợp lý khi thời tiết không trùng hoàn toàn (VD: Size 4 vào buổi sáng
        // trời nắng -> Cá Chép Vàng lớn)
        if (clamped == 1)
            return CA_RO;
        if (clamped == 2)
            return CA_CHEP;
        if (clamped == 3)
            return (weather == WeatherType.RAINY) ? LUON_MUA : CA_CHEP_VANG;
        if (clamped == 4) {
            if (currentHour >= 17)
                return CA_DEN_LONG;
            if (weather == WeatherType.RAINY)
                return CA_TRE_DIEN;
            return CA_CHEP_VANG;
        }
        // Size 5
        if (weather == WeatherType.DROUGHT) {
            return rng.nextDouble() < 0.65 ? CO_VAT_DAY_SONG : CA_VUONG_MIEN;
        }
        return CA_VUONG_MIEN;
    }

    public static FishSpecies findByDisplayName(String name) {
        if (name == null)
            return null;
        for (FishSpecies sp : values()) {
            if (sp.displayName.equalsIgnoreCase(name))
                return sp;
        }
        return null;
    }
}
