package model.game_item;

/**
 * Vật phẩm Cá câu được từ con sông, lưu trong hành trang người chơi (Inventory).
 */
public class FishItem extends Item {
    public static final String DEFAULT_NAME = "River Fish";
    public static final int DEFAULT_PRICE = 50;

    public FishItem() {
        this(DEFAULT_NAME, DEFAULT_PRICE);
    }

    public FishItem(String name, int price) {
        super(name, price);
    }

    public static FishItem fromSpecies(FishSpecies species) {
        if (species == null) return new FishItem();
        return new FishItem(species.getDisplayName(), species.getPrice());
    }

    public FishSpecies getSpecies() {
        return FishSpecies.findByDisplayName(name);
    }

    /**
     * Xếp hạng độ hiếm của cá theo 3 mức dùng khi chế biến món ăn:
     * 1 = Thường (Cá Rô, Cá Chép...), 2 = Hiếm (Cá Chép Vàng, Lươn Mưa, Cá Đèn Lồng),
     * 3 = Cực hiếm / Siêu hiếm (Cá Trê Điện, Cá Vương Miện).
     */
    public int getRarityTier() {
        FishSpecies sp = getSpecies();
        if (sp != null) {
            String label = sp.getRarityLabel();
            if ("Siêu hiếm".equalsIgnoreCase(label) || "Cực hiếm".equalsIgnoreCase(label)) {
                return 3;
            }
            if ("Hiếm".equalsIgnoreCase(label)) {
                return 2;
            }
            return 1;
        }
        if (price >= 120) return 3;
        if (price >= 75) return 2;
        return 1;
    }

    @Override
    public String getStats() {
        FishSpecies sp = getSpecies();
        String rarity = (sp != null) ? sp.getRarityLabel() : "Thường";
        return "Cá sông tươi (" + rarity + ") | Bán: " + price + "$";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FishItem)) return false;
        FishItem that = (FishItem) o;
        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
