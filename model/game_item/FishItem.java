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

    @Override
    public String getStats() {
        return "Cá sông tươi | Bán: " + price + "$";
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
