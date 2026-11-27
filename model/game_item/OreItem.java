package model.game_item;

/**
 * Khoáng sản khai thác từ Khu Mỏ Đá Mini (Quặng Đồng, Quặng Sắt, Quặng Vàng, Đá Quý).
 */
public class OreItem extends Item {

    public static final String ID_COPPER_ORE = "copper_ore";
    public static final String ID_IRON_ORE   = "iron_ore";
    public static final String ID_GOLD_ORE   = "gold_ore";
    public static final String ID_GEMSTONE   = "gemstone";

    private final String oreId;
    private final String description;

    public OreItem(String oreId, String name, int price, String description) {
        super(name, price);
        this.oreId = oreId;
        this.description = description;
    }

    public static OreItem copper() {
        return new OreItem(ID_COPPER_ORE, "Quặng Đồng", 35, "Khoáng sản cơ bản từ khu mỏ đá");
    }

    public static OreItem iron() {
        return new OreItem(ID_IRON_ORE, "Quặng Sắt", 65, "Nguyên liệu rèn Vòi Tưới Tự Động (cần 2 Sắt + 1 Vàng)");
    }

    public static OreItem gold() {
        return new OreItem(ID_GOLD_ORE, "Quặng Vàng", 120, "Kim loại quý hiếm dùng chế tạo Vòi Tưới Tự Động & bán lời cao");
    }

    public static OreItem gemstone() {
        return new OreItem(ID_GEMSTONE, "Đá Quý", 240, "Tinh thể đá quý lấp lánh cực kỳ giá trị");
    }

    public static OreItem findByName(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase();
        if (n.equals("quặng đồng") || n.equals("copper ore")) return copper();
        if (n.equals("quặng sắt") || n.equals("iron ore")) return iron();
        if (n.equals("quặng vàng") || n.equals("gold ore")) return gold();
        if (n.equals("đá quý") || n.equals("gemstone")) return gemstone();
        return null;
    }

    public String getOreId() {
        return oreId;
    }

    @Override
    public String getStats() {
        return description + " | Bán: $" + price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OreItem)) return false;
        OreItem that = (OreItem) o;
        return oreId.equals(that.oreId);
    }

    @Override
    public int hashCode() {
        return oreId.hashCode();
    }
}
