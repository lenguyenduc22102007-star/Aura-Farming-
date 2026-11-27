package model.game_item;

/**
 * Vật phẩm tự nhiên nhặt được quanh khu vực trại / đống lửa mỗi ngày (Foraging Items):
 * - Củi khô (Firewood): nguyên liệu bắt buộc để nhóm lửa khi ghép món ăn kiểu Minecraft.
 * - Nấm dại (WildMushroom) & Thảo mộc (Herb): gia vị tự nhiên tăng giá trị & hiệu quả món ăn.
 */
public class ForageItem extends Item {

    public static final String ID_FIREWOOD      = "firewood";
    public static final String ID_WILD_MUSHROOM = "wild_mushroom";
    public static final String ID_HERB          = "herb";

    private final String forageId;
    private final String description;

    public ForageItem(String forageId, String name, int price, String description) {
        super(name, price);
        this.forageId = forageId;
        this.description = description;
    }

    public static ForageItem firewood() {
        return new ForageItem(
            ID_FIREWOOD,
            "Củi khô",
            10,
            "Nhiên liệu bắt buộc để nhóm lửa khi ghép món ăn tại đống lửa"
        );
    }

    public static ForageItem wildMushroom() {
        return new ForageItem(
            ID_WILD_MUSHROOM,
            "Nấm dại",
            18,
            "Nấm rừng thơm ngọt, dùng làm gia vị nướng thịt tại bếp lửa"
        );
    }

    public static ForageItem herb() {
        return new ForageItem(
            ID_HERB,
            "Thảo mộc",
            16,
            "Lá thảo mộc tươi quanh khu trại, tăng hương vị & buff chạy nhanh"
        );
    }

    public static ForageItem createById(String id) {
        if (id == null) return null;
        switch (id.toLowerCase()) {
            case ID_FIREWOOD:      return firewood();
            case ID_WILD_MUSHROOM: return wildMushroom();
            case ID_HERB:          return herb();
            default:               return null;
        }
    }

    public static ForageItem findByName(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase();
        if (n.equals("củi khô") || n.equals("firewood")) return firewood();
        if (n.equals("nấm dại") || n.equals("wild mushroom") || n.equals("wildmushroom")) return wildMushroom();
        if (n.equals("thảo mộc") || n.equals("herb")) return herb();
        return null;
    }

    public String getForageId() {
        return forageId;
    }

    @Override
    public String getStats() {
        return description + " | Bán: $" + price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForageItem)) return false;
        ForageItem that = (ForageItem) o;
        return forageId.equals(that.forageId);
    }

    @Override
    public int hashCode() {
        return forageId.hashCode();
    }
}
