package model.game_item;

/**
 * Món ăn đã nấu chín tại Đống Lửa Trại (Campfire Cooking System).
 * Có thể bán trong Kho/Shop để kiếm lời cao hơn nguyên liệu gốc hoặc ăn ngay để nhận Buff trong ngày.
 */
public class CookedFoodItem extends Item {

    public static final String ID_EGG_FRY     = "food_egg_fry";
    public static final String ID_PORK_GRILL  = "food_pork_grill";
    public static final String ID_MILK_STEAK  = "food_milk_steak";
    public static final String ID_FARM_HOTPOT = "food_farm_hotpot";

    private final String recipeId;
    private final int hungerRestore;
    private final int thirstRestore;
    private final int speedBonusPercent;
    private final boolean rareFishBoost;
    private final String buffDescription;

    public CookedFoodItem(String recipeId, String name, int price,
                          int hungerRestore, int thirstRestore,
                          int speedBonusPercent, boolean rareFishBoost, String buffDescription) {
        super(name, price);
        this.recipeId = recipeId;
        this.hungerRestore = hungerRestore;
        this.thirstRestore = thirstRestore;
        this.speedBonusPercent = speedBonusPercent;
        this.rareFishBoost = rareFishBoost;
        this.buffDescription = buffDescription;
    }

    public static CookedFoodItem createById(String recipeId) {
        if (recipeId == null) return null;
        switch (recipeId) {
            case ID_EGG_FRY:
                return new CookedFoodItem(
                    ID_EGG_FRY,
                    "Trứng Ốp La Lửa Trại",
                    55,
                    45, 0,
                    0,
                    false,
                    "Hồi +45 Đói"
                );
            case ID_PORK_GRILL:
                return new CookedFoodItem(
                    ID_PORK_GRILL,
                    "Thịt Lợn Nướng Than",
                    90,
                    65, 0,
                    0,
                    false,
                    "Hồi +65 Đói"
                );
            case ID_MILK_STEAK:
                return new CookedFoodItem(
                    ID_MILK_STEAK,
                    "Sữa Nóng & Bò Bít Tết",
                    115,
                    55, 45,
                    0,
                    true,
                    "Hồi +55 Đói, +45 Nước | Tăng tỉ lệ cá hiếm"
                );
            case ID_FARM_HOTPOT:
                return new CookedFoodItem(
                    ID_FARM_HOTPOT,
                    "Lẩu Nông Trại Đặc Biệt",
                    220,
                    100, 50,
                    0,
                    true,
                    "Hồi +100 Đói, +50 Nước | Tăng mạnh cá hiếm"
                );
            default:
                return null;
        }
    }

    public static CookedFoodItem findByName(String name) {
        if (name == null) return null;
        String[] ids = { ID_EGG_FRY, ID_PORK_GRILL, ID_MILK_STEAK, ID_FARM_HOTPOT };
        for (String id : ids) {
            CookedFoodItem candidate = createById(id);
            if (candidate != null && candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public int getHungerRestore() {
        return hungerRestore;
    }

    public int getThirstRestore() {
        return thirstRestore;
    }

    public int getSpeedBonusPercent() {
        return speedBonusPercent;
    }

    public boolean hasRareFishBoost() {
        return rareFishBoost;
    }

    public String getBuffDescription() {
        return buffDescription;
    }

    @Override
    public String getStats() {
        return "Món nấu lửa trại | Bán: $" + price + " | Ăn: " + buffDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CookedFoodItem)) return false;
        CookedFoodItem that = (CookedFoodItem) o;
        return recipeId.equals(that.recipeId);
    }

    @Override
    public int hashCode() {
        return recipeId.hashCode();
    }
}
