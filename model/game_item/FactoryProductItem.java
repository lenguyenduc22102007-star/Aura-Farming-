package model.game_item;

/**
 * Thành phẩm chế biến công nghiệp từ 3 Dây Chuyền Nhà Máy (Factory Production Lines).
 * Có thể bán giá cao (nhân thêm 1.5x khi Sốt Giá), ăn/uống hồi Đói/Khát hoặc tặng NPC (+1 Tim).
 */
public class FactoryProductItem extends Item {

    public static final String ID_TOMATO_JUICE  = "factory_tomato_juice";
    public static final String ID_POTATO_SNACK  = "factory_potato_snack";
    public static final String ID_BUTTER_POPCORN = "factory_butter_popcorn";
    public static final String ID_CANNED_CHEESE = "factory_canned_cheese";
    public static final String ID_PACKED_RICE   = "factory_packed_rice";
    public static final String ID_SUNFLOWER_OIL = "factory_sunflower_oil";

    public static final String[] ALL_IDS = {
        ID_TOMATO_JUICE,
        ID_POTATO_SNACK,
        ID_BUTTER_POPCORN,
        ID_CANNED_CHEESE,
        ID_PACKED_RICE,
        ID_SUNFLOWER_OIL
    };

    private final String productId;
    private final int hungerRestore;
    private final int thirstRestore;
    private final String description;

    public FactoryProductItem(String productId, String name, int price,
                              int hungerRestore, int thirstRestore, String description) {
        super(name, price);
        this.productId = productId;
        this.hungerRestore = hungerRestore;
        this.thirstRestore = thirstRestore;
        this.description = description;
    }

    public static FactoryProductItem createById(String id) {
        if (id == null) return null;
        switch (id) {
            case ID_TOMATO_JUICE:
                return new FactoryProductItem(
                    ID_TOMATO_JUICE, "Nước Ép Cà Chua", 200,
                    0, 60, "Đóng chai từ Cà chua tươi | Uống hồi +60 Khát"
                );
            case ID_POTATO_SNACK:
                return new FactoryProductItem(
                    ID_POTATO_SNACK, "Snack Khoai Tây", 95,
                    30, 0, "Khoai tây chiên giòn đóng gói | Ăn hồi +30 Đói"
                );
            case ID_BUTTER_POPCORN:
                return new FactoryProductItem(
                    ID_BUTTER_POPCORN, "Bỏng Ngô Bơ", 160,
                    45, 0, "Ngô nổ bơ thơm lừng | Ăn hồi +45 Đói"
                );
            case ID_CANNED_CHEESE:
                return new FactoryProductItem(
                    ID_CANNED_CHEESE, "Phô Mai Đóng Hộp", 220,
                    75, 0, "Ủ cô đặc từ Sữa Bò | Ăn hồi +75 Đói"
                );
            case ID_PACKED_RICE:
                return new FactoryProductItem(
                    ID_PACKED_RICE, "Gạo Trắng Đóng Bao", 260,
                    40, 0, "Xay xát tinh chế từ Lúa nước | Giá trị xuất khẩu cao"
                );
            case ID_SUNFLOWER_OIL:
                return new FactoryProductItem(
                    ID_SUNFLOWER_OIL, "Dầu Hướng Dương", 130,
                    20, 0, "Tinh dầu ép lạnh nguyên chất từ Hoa hướng dương"
                );
            default:
                return null;
        }
    }

    public static FactoryProductItem findByName(String name) {
        if (name == null) return null;
        for (String id : ALL_IDS) {
            FactoryProductItem p = createById(id);
            if (p != null && p.getName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return null;
    }

    public String getProductId() {
        return productId;
    }

    public int getHungerRestore() {
        return hungerRestore;
    }

    public int getThirstRestore() {
        return thirstRestore;
    }

    @Override
    public String getStats() {
        return description + " | Bán: $" + price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactoryProductItem)) return false;
        FactoryProductItem that = (FactoryProductItem) o;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return productId.hashCode();
    }
}
