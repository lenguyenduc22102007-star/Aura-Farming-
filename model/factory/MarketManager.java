package model.factory;

import model.game_item.FactoryProductItem;
import model.game_item.FishItem;
import model.game_item.FishSpecies;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;

import java.util.Properties;
import java.util.Random;

/**
 * Quản lý Thị Trường Biến Động Sốt Giá Theo Ngày (Hot Market +50%):
 * - Mỗi sáng (nextDay), tự động chọn 2 mặt hàng đang SỐT GIÁ (+50% = 1.5x giá bán):
 *   1. Một nông sản hoặc thành phẩm Nhà Máy.
 *   2. Một loài cá trên sông.
 */
public class MarketManager {

    private static final String[][] PRODUCE_POOL = {
        { "crop:Tomato",     "Cà chua (Tomato)" },
        { "crop:Potato",     "Khoai tây (Potato)" },
        { "crop:Corn",       "Ngô (Corn)" },
        { "crop:Sunflower",  "Hướng dương (Sunflower)" },
        { "crop:Water Rice", "Lúa nước (Water Rice)" },
        { "crop:Orange",     "Quả Cam (Orange)" },
        { "crop:Apple",      "Quả Táo (Apple)" },
        { "crop:Coconut",    "Quả Dừa (Coconut)" },
        { "factory:" + FactoryProductItem.ID_TOMATO_JUICE,   "Nước Ép Cà Chua" },
        { "factory:" + FactoryProductItem.ID_POTATO_SNACK,   "Snack Khoai Tây" },
        { "factory:" + FactoryProductItem.ID_BUTTER_POPCORN, "Bỏng Ngô Bơ" },
        { "factory:" + FactoryProductItem.ID_CANNED_CHEESE,  "Phô Mai Đóng Hộp" },
        { "factory:" + FactoryProductItem.ID_PACKED_RICE,    "Gạo Trắng Đóng Bao" },
        { "factory:" + FactoryProductItem.ID_SUNFLOWER_OIL,  "Dầu Hướng Dương" }
    };

    private String hotProduceKey = "factory:" + FactoryProductItem.ID_TOMATO_JUICE;
    private String hotProduceLabel = "Nước Ép Cà Chua";
    private FishSpecies hotFishSpecies = FishSpecies.CA_CHEP_VANG;

    public MarketManager() {
        rollForDay(1, false);
    }

    /** Đổ xúc xắc chọn 2 mặt hàng sốt giá cho ngày mới (nếu chưa mở vườn cây ăn quả thì tránh chọn Cam/Táo/Dừa). */
    public void rollForDay(int day, boolean orchardUnlocked) {
        Random rng = new Random(day * 104729L + 202605L);
        int pickedIdx = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            int cand = rng.nextInt(PRODUCE_POOL.length);
            String k = PRODUCE_POOL[cand][0];
            if (!orchardUnlocked && (k.equals("crop:Orange") || k.equals("crop:Apple") || k.equals("crop:Coconut"))) {
                continue;
            }
            pickedIdx = cand;
            break;
        }
        this.hotProduceKey = PRODUCE_POOL[pickedIdx][0];
        this.hotProduceLabel = PRODUCE_POOL[pickedIdx][1];

        FishSpecies[] allFish = FishSpecies.values();
        if (allFish.length > 0) {
            this.hotFishSpecies = allFish[rng.nextInt(allFish.length)];
        }
    }

    public boolean isHotItem(Item item) {
        if (item == null) return false;
        if (item instanceof HarvestedCropItem) {
            String key = "crop:" + ((HarvestedCropItem) item).getCropName();
            return key.equalsIgnoreCase(hotProduceKey);
        }
        if (item instanceof FactoryProductItem) {
            String key = "factory:" + ((FactoryProductItem) item).getProductId();
            return key.equalsIgnoreCase(hotProduceKey);
        }
        if (item instanceof FishItem && hotFishSpecies != null) {
            return item.getName().equalsIgnoreCase(hotFishSpecies.getDisplayName());
        }
        return false;
    }

    /** Trả về giá bán thực tế của 1 đơn vị vật phẩm (tự động nhân 1.5x nếu đang SỐT GIÁ hôm nay). */
    public int getSellUnitPrice(Item item) {
        if (item == null) return 0;
        int base = item.getPrice();
        if (base <= 0) return 0;
        if (isHotItem(item)) {
            return (int) Math.round(base * 1.5);
        }
        return base;
    }

    public String getHotProduceLabel() {
        return hotProduceLabel;
    }

    public String getHotFishLabel() {
        return hotFishSpecies != null ? hotFishSpecies.getDisplayName() : "Cá Chép Vàng";
    }

    public String getMorningBannerText() {
        return "🔥 SỐT GIÁ HÔM NAY (+50%): " + getHotProduceLabel() + "  &  " + getHotFishLabel();
    }

    public void save(Properties p) {
        if (p == null) return;
        p.setProperty("market.hotProduceKey", hotProduceKey != null ? hotProduceKey : "");
        p.setProperty("market.hotProduceLabel", hotProduceLabel != null ? hotProduceLabel : "");
        if (hotFishSpecies != null) {
            p.setProperty("market.hotFish", hotFishSpecies.name());
        }
    }

    public void load(Properties p) {
        if (p == null) return;
        String pk = p.getProperty("market.hotProduceKey");
        String pl = p.getProperty("market.hotProduceLabel");
        if (pk != null && !pk.isEmpty() && pl != null && !pl.isEmpty()) {
            this.hotProduceKey = pk;
            this.hotProduceLabel = pl;
        }
        String fs = p.getProperty("market.hotFish");
        if (fs != null && !fs.isEmpty()) {
            try {
                this.hotFishSpecies = FishSpecies.valueOf(fs);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
