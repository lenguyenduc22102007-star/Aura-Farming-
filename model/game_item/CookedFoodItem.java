package model.game_item;

/**
 * Món ăn đã nấu chín tại Đống Lửa Trại (Campfire Cooking System).
 * Có thể bán trong Kho/Shop để kiếm lời cao hơn nguyên liệu gốc hoặc ăn ngay để nhận Buff trong ngày.
 */
public class CookedFoodItem extends Item {

    public static final String ID_EGG_FRY           = "food_egg_fry";
    public static final String ID_PORK_GRILL        = "food_pork_grill";
    public static final String ID_MILK_STEAK        = "food_milk_steak";
    public static final String ID_FARM_HOTPOT       = "food_farm_hotpot";
    public static final String ID_FRIED_FISH_COMMON = "food_fried_fish_common";
    public static final String ID_FRIED_FISH_RARE   = "food_fried_fish_rare";
    public static final String ID_FRIED_FISH_EPIC   = "food_fried_fish_epic";

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
                    "Gà Nướng Lửa Trại",
                    60,
                    45, 0,
                    0,
                    false,
                    "Hồi +45 Đói | Bán lời x2"
                );
            case ID_PORK_GRILL:
                return new CookedFoodItem(
                    ID_PORK_GRILL,
                    "Thịt Nướng Thảo Mộc",
                    125,
                    70, 0,
                    20,
                    false,
                    "Hồi +70 Đói | Buff +20% tốc độ chạy"
                );
            case ID_MILK_STEAK:
                return new CookedFoodItem(
                    ID_MILK_STEAK,
                    "Cháo Sữa Nóng",
                    130,
                    55, 50,
                    0,
                    true,
                    "Hồi +55 Đói, +50 Khát | Tăng cá hiếm"
                );
            case ID_FARM_HOTPOT:
                return new CookedFoodItem(
                    ID_FARM_HOTPOT,
                    "Lẩu Nông Trại Đặc Biệt",
                    260,
                    100, 50,
                    0,
                    true,
                    "Hồi +100 Đói, +50 Khát | Tăng mạnh cá hiếm"
                );
            case ID_FRIED_FISH_COMMON:
                return new CookedFoodItem(
                    ID_FRIED_FISH_COMMON,
                    "Cá Rán",
                    65,
                    20, 0,
                    0,
                    false,
                    "Hồi +20 Đói | Cá Thường"
                );
            case ID_FRIED_FISH_RARE:
                return new CookedFoodItem(
                    ID_FRIED_FISH_RARE,
                    "Cá Rán Thượng Hạng",
                    175,
                    30, 0,
                    0,
                    false,
                    "Hồi +30 Đói | Cá Hiếm"
                );
            case ID_FRIED_FISH_EPIC:
                return new CookedFoodItem(
                    ID_FRIED_FISH_EPIC,
                    "Cá Rán Hoàng Gia",
                    290,
                    40, 0,
                    0,
                    false,
                    "Hồi +40 Đói | Cá Cực/Siêu Hiếm"
                );
            default:
                return null;
        }
    }

    /** Tạo món Cá Rán đúng bậc (+20/+30/+40 Đói và giá bán $65/$175/$290) theo độ hiếm của cá nguyên liệu. */
    public static CookedFoodItem createFriedFishFor(FishItem fish) {
        int tier = (fish != null) ? fish.getRarityTier() : 1;
        if (tier >= 3) return createById(ID_FRIED_FISH_EPIC);
        if (tier == 2) return createById(ID_FRIED_FISH_RARE);
        return createById(ID_FRIED_FISH_COMMON);
    }

    public static CookedFoodItem findByName(String name) {
        if (name == null) return null;
        String[] ids = {
            ID_EGG_FRY, ID_PORK_GRILL, ID_MILK_STEAK, ID_FARM_HOTPOT,
            ID_FRIED_FISH_COMMON, ID_FRIED_FISH_RARE, ID_FRIED_FISH_EPIC
        };
        for (String id : ids) {
            CookedFoodItem candidate = createById(id);
            if (candidate != null && candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        // Tương thích ngược với file save cũ
        String low = name.trim().toLowerCase();
        if (low.contains("trứng ốp la")) return createById(ID_EGG_FRY);
        if (low.contains("thịt lợn nướng")) return createById(ID_PORK_GRILL);
        if (low.contains("sữa nóng") || low.contains("bò bít tết")) return createById(ID_MILK_STEAK);
        if (low.contains("lẩu nông trại")) return createById(ID_FARM_HOTPOT);
        if (low.contains("cá rán hoàng gia")) return createById(ID_FRIED_FISH_EPIC);
        if (low.contains("cá rán thượng hạng")) return createById(ID_FRIED_FISH_RARE);
        if (low.contains("cá rán")) return createById(ID_FRIED_FISH_COMMON);
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
