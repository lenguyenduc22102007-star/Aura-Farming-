package model.cooking;

import model.game_item.AnimalProductItem;
import model.game_item.CookedFoodItem;
import model.game_item.FishItem;
import model.game_item.FishSpecies;
import model.game_item.ForageItem;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bộ quản lý công thức ghép món ăn kiểu Minecraft (3x3 Crafting Grid).
 * Tách riêng hoàn toàn khỏi giao diện để dễ bảo trì và bổ sung công thức mới.
 *
 * Hỗ trợ cả 2 kiểu xếp:
 * - Xếp đúng hàng chuẩn (Củi khô đặt ở hàng dưới cùng ô 6/7/8, nguyên liệu ở phía trên), HOẶC
 * - Xếp linh hoạt ở bất kỳ ô nào trên lưới 3x3 miễn là có đúng tổ hợp nguyên liệu + Củi khô.
 */
public final class CookingRecipeManager {

    /** Nhóm nguyên liệu dùng trong công thức nấu ăn. */
    public enum IngredientKind {
        FIREWOOD("Củi khô"),
        HERB_OR_MUSHROOM("Thảo mộc / Nấm dại"),
        CHICKEN_OR_EGG("Trứng / Thịt Gà"),
        PORK_OR_TRUFFLE("Thịt Lợn (Nấm cục)"),
        MILK("Sữa Bò"),
        ANY_MEAT_OR_ANIMAL("Thịt / SP Gà-Lợn-Bò"),
        CORN_OR_RICE("Ngô / Lúa nước"),
        TOMATO("Cà chua"),
        POTATO("Khoai tây"),
        FISH("Cá sông tươi");

        private final String displayLabel;

        IngredientKind(String displayLabel) {
            this.displayLabel = displayLabel;
        }

        public String getDisplayLabel() {
            return displayLabel;
        }
    }

    /** Mô tả một công thức nấu ăn hoàn chỉnh trên bàn ghép 3x3. */
    public static final class CookingRecipe {
        private final String recipeId;
        private final String formulaSummary;
        private final String statSummary;
        private final List<IngredientKind> requiredIngredients;

        public CookingRecipe(String recipeId, String formulaSummary, String statSummary, List<IngredientKind> requiredIngredients) {
            this.recipeId = recipeId;
            this.formulaSummary = formulaSummary;
            this.statSummary = statSummary;
            this.requiredIngredients = Collections.unmodifiableList(new ArrayList<>(requiredIngredients));
        }

        public String getRecipeId() {
            return recipeId;
        }

        public String getFormulaSummary() {
            return formulaSummary;
        }

        public String getStatSummary() {
            return statSummary;
        }

        public List<IngredientKind> getRequiredIngredients() {
            return requiredIngredients;
        }

        public CookedFoodItem createOutput() {
            return CookedFoodItem.createById(recipeId);
        }
    }

    /** Kết quả đánh giá lưới 3x3: khớp công thức + trạng thái gợi ý (ví dụ: quên đặt Củi khô). */
    public static final class MatchResult {
        private final CookingRecipe recipe;
        private final CookedFoodItem output;
        private final boolean firewoodAtBottomRow;
        private final String hintMessage;

        public MatchResult(CookingRecipe recipe, CookedFoodItem output, boolean firewoodAtBottomRow, String hintMessage) {
            this.recipe = recipe;
            this.output = output;
            this.firewoodAtBottomRow = firewoodAtBottomRow;
            this.hintMessage = hintMessage;
        }

        public boolean isMatched() {
            return recipe != null && output != null;
        }

        public CookingRecipe getRecipe() {
            return recipe;
        }

        public CookedFoodItem getOutput() {
            return output;
        }

        public boolean isFirewoodAtBottomRow() {
            return firewoodAtBottomRow;
        }

        public String getHintMessage() {
            return hintMessage;
        }
    }

    private static final List<CookingRecipe> RECIPES = new ArrayList<>();

    static {
        // 1. 1 Củi khô + 1 Thịt (Gà/Lợn/Bò) + 1 Cà chua + 1 Khoai tây -> Lẩu Nông Trại Đặc Biệt (+100 Đói, +50 Khát)
        List<IngredientKind> hotpot = new ArrayList<>();
        hotpot.add(IngredientKind.FIREWOOD);
        hotpot.add(IngredientKind.ANY_MEAT_OR_ANIMAL);
        hotpot.add(IngredientKind.TOMATO);
        hotpot.add(IngredientKind.POTATO);
        RECIPES.add(new CookingRecipe(
            CookedFoodItem.ID_FARM_HOTPOT,
            "1 Củi khô + 1 SP Gà/Lợn/Bò + 1 Cà chua + 1 Khoai tây",
            "+100 Đói, +50 Khát • Tăng cá hiếm • Bán $260",
            hotpot
        ));

        // 2. 1 Củi khô + 1 Thịt Lợn + 1 Thảo mộc/Nấm -> Thịt Nướng Thảo Mộc (+70 Đói, buff chạy nhanh)
        List<IngredientKind> porkHerb = new ArrayList<>();
        porkHerb.add(IngredientKind.FIREWOOD);
        porkHerb.add(IngredientKind.PORK_OR_TRUFFLE);
        porkHerb.add(IngredientKind.HERB_OR_MUSHROOM);
        RECIPES.add(new CookingRecipe(
            CookedFoodItem.ID_PORK_GRILL,
            "1 Củi khô + 1 Thịt/Nấm Lợn + 1 Thảo mộc/Nấm dại",
            "+70 Đói • Buff +20% tốc độ chạy • Bán $125",
            porkHerb
        ));

        // 3. 1 Củi khô + 1 Sữa Bò + 1 Ngô/Lúa -> Cháo Sữa Nóng (+55 Đói, +50 Khát)
        List<IngredientKind> milkPorridge = new ArrayList<>();
        milkPorridge.add(IngredientKind.FIREWOOD);
        milkPorridge.add(IngredientKind.MILK);
        milkPorridge.add(IngredientKind.CORN_OR_RICE);
        RECIPES.add(new CookingRecipe(
            CookedFoodItem.ID_MILK_STEAK,
            "1 Củi khô + 1 Sữa Bò + 1 Ngô/Lúa nước",
            "+55 Đói, +50 Khát • Tăng cá hiếm • Bán $130",
            milkPorridge
        ));

        // 4. 1 Củi khô + 1 Trứng/Thịt Gà -> Gà Nướng Lửa Trại (+45 Đói, bán lời x2)
        List<IngredientKind> roastChicken = new ArrayList<>();
        roastChicken.add(IngredientKind.FIREWOOD);
        roastChicken.add(IngredientKind.CHICKEN_OR_EGG);
        RECIPES.add(new CookingRecipe(
            CookedFoodItem.ID_EGG_FRY,
            "1 Củi khô + 1 Trứng/Thịt Gà",
            "+45 Đói • Bán $60 (lời >x2)",
            roastChicken
        ));

        // 5. 1 Củi khô + 1 Cá sông -> Cá Rán (+20/+30/+40 Đói & bán $65/$175/$290 tùy độ hiếm cá)
        List<IngredientKind> friedFish = new ArrayList<>();
        friedFish.add(IngredientKind.FIREWOOD);
        friedFish.add(IngredientKind.FISH);
        RECIPES.add(new CookingRecipe(
            CookedFoodItem.ID_FRIED_FISH_COMMON,
            "1 Củi khô + 1 Cá bất kỳ",
            "+20/30/40 Đói • Bán $65/$175/$290",
            friedFish
        ));
    }

    private CookingRecipeManager() {}

    public static List<CookingRecipe> getAllRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    /** Kiểm tra 1 Item có phải cá có thể đem rán không (loại trừ Cổ Vật Đáy Sông). */
    public static boolean isCookableFish(Item item) {
        if (!(item instanceof FishItem)) return false;
        return FishSpecies.findByDisplayName(item.getName()) != FishSpecies.CO_VAT_DAY_SONG;
    }

    /** Kiểm tra 1 Item trong kho có phải nguyên liệu có thể đưa vào bàn nấu ăn hay không. */
    public static boolean isCookingIngredient(Item item) {
        if (item == null) return false;
        return (item instanceof ForageItem)
            || (item instanceof AnimalProductItem)
            || (item instanceof HarvestedCropItem)
            || isCookableFish(item);
    }

    /** Tên hiển thị tiếng Việt ngắn gọn, rõ ràng trên bàn ghép nấu ăn. */
    public static String getVietnameseDisplayName(Item item) {
        if (item == null) return "";
        if (item instanceof ForageItem) {
            return item.getName();
        }
        if (item instanceof FishItem) {
            if (FishItem.DEFAULT_NAME.equalsIgnoreCase(item.getName())) return "Cá sông";
            return item.getName();
        }
        if (item instanceof AnimalProductItem) {
            String low = item.getName().toLowerCase();
            if (low.contains("egg")) return "Trứng / Thịt Gà";
            if (low.contains("truffle") || low.contains("pork")) return "Thịt Lợn";
            if (low.contains("milk")) return "Sữa Bò";
            return item.getName();
        }
        if (item instanceof HarvestedCropItem) {
            String crop = ((HarvestedCropItem) item).getCropName().toLowerCase();
            if (crop.contains("tomato")) return "Cà chua";
            if (crop.contains("potato")) return "Khoai tây";
            if (crop.contains("corn")) return "Ngô";
            if (crop.contains("water rice") || crop.contains("rice")) return "Lúa nước";
            if (crop.contains("sunflower")) return "Hướng dương";
            return ((HarvestedCropItem) item).getCropName();
        }
        return item.getName();
    }

    /** Kiểm tra một Item có khớp với loại nguyên liệu yêu cầu hay không. */
    public static boolean matchesKind(Item item, IngredientKind kind) {
        if (item == null || kind == null) return false;
        switch (kind) {
            case FIREWOOD:
                return (item instanceof ForageItem)
                    && ForageItem.ID_FIREWOOD.equals(((ForageItem) item).getForageId());

            case HERB_OR_MUSHROOM:
                if (item instanceof ForageItem) {
                    String fid = ((ForageItem) item).getForageId();
                    return ForageItem.ID_HERB.equals(fid) || ForageItem.ID_WILD_MUSHROOM.equals(fid);
                }
                return false;

            case CHICKEN_OR_EGG:
                if (item instanceof AnimalProductItem) {
                    String low = item.getName().toLowerCase();
                    return low.contains("egg") || low.contains("chicken") || low.contains("gà");
                }
                return false;

            case PORK_OR_TRUFFLE:
                if (item instanceof AnimalProductItem) {
                    String low = item.getName().toLowerCase();
                    return low.contains("truffle") || low.contains("pork") || low.contains("lợn");
                }
                return false;

            case MILK:
                if (item instanceof AnimalProductItem) {
                    String low = item.getName().toLowerCase();
                    return low.contains("milk") || low.contains("sữa");
                }
                return false;

            case ANY_MEAT_OR_ANIMAL:
                return item instanceof AnimalProductItem;

            case CORN_OR_RICE:
                if (item instanceof HarvestedCropItem) {
                    String crop = ((HarvestedCropItem) item).getCropName().toLowerCase();
                    return crop.contains("corn") || crop.contains("rice") || crop.contains("ngô") || crop.contains("lúa");
                }
                return false;

            case TOMATO:
                if (item instanceof HarvestedCropItem) {
                    String crop = ((HarvestedCropItem) item).getCropName().toLowerCase();
                    return crop.contains("tomato") || crop.contains("cà chua");
                }
                return false;

            case POTATO:
                if (item instanceof HarvestedCropItem) {
                    String crop = ((HarvestedCropItem) item).getCropName().toLowerCase();
                    return crop.contains("potato") || crop.contains("khoai");
                }
                return false;

            case FISH:
                return isCookableFish(item);

            default:
                return false;
        }
    }

    /**
     * Đánh giá lưới 3x3 (9 ô, chỉ số 0..8; hàng dưới cùng là 6, 7, 8).
     * Khớp công thức chính xác theo số lượng ô đang đặt (không có nguyên liệu thừa sai).
     */
    public static MatchResult evaluateGrid(Item[] grid) {
        if (grid == null || grid.length != 9) {
            return new MatchResult(null, null, false, "Đặt nguyên liệu vào bàn 3x3 để ghép món ăn.");
        }

        List<Item> placed = new ArrayList<>();
        boolean hasFirewood = false;
        boolean firewoodAtBottom = false;
        for (int i = 0; i < 9; i++) {
            if (grid[i] != null) {
                placed.add(grid[i]);
                if (matchesKind(grid[i], IngredientKind.FIREWOOD)) {
                    hasFirewood = true;
                    if (i >= 6) {
                        firewoodAtBottom = true;
                    }
                }
            }
        }

        if (placed.isEmpty()) {
            return new MatchResult(null, null, false, "Chọn nguyên liệu bên trái rồi click vào ô 3x3 (đặt Củi khô ở hàng dưới).");
        }

        // Thử ghép từng công thức (kiểm tra khớp song ánh đúng số lượng nguyên liệu)
        for (CookingRecipe recipe : RECIPES) {
            List<IngredientKind> req = recipe.getRequiredIngredients();
            if (placed.size() != req.size()) continue;
            if (canAssignBijectively(placed, req, 0, new boolean[placed.size()])) {
                CookedFoodItem out;
                if (CookedFoodItem.ID_FRIED_FISH_COMMON.equals(recipe.getRecipeId())) {
                    FishItem placedFish = null;
                    for (Item it : placed) {
                        if (it instanceof FishItem) {
                            placedFish = (FishItem) it;
                            break;
                        }
                    }
                    out = CookedFoodItem.createFriedFishFor(placedFish);
                } else {
                    out = recipe.createOutput();
                }
                String note = firewoodAtBottom
                    ? "🔥 Nhóm lửa chuẩn hàng dưới! Sẵn sàng chế tạo."
                    : "🔥 Công thức hợp lệ! (Mẹo: đặt Củi khô ở hàng dưới cho chuẩn bếp lửa)";
                return new MatchResult(recipe, out, firewoodAtBottom, note);
            }
        }

        if (!hasFirewood) {
            return new MatchResult(null, null, false, "⚠ Thiếu Củi khô! Hãy đặt 1 Củi khô ở hàng dưới cùng để nhóm lửa.");
        }
        return new MatchResult(null, null, firewoodAtBottom, "Chưa khớp công thức — xem Sổ Công Thức Gợi Ý bên dưới!");
    }

    /** Tìm phép gán 1-1 giữa các nguyên liệu đặt trên bàn và danh sách yêu cầu của công thức. */
    private static boolean canAssignBijectively(List<Item> placed, List<IngredientKind> req, int reqIdx, boolean[] used) {
        if (reqIdx >= req.size()) {
            return true;
        }
        IngredientKind needed = req.get(reqIdx);
        for (int i = 0; i < placed.size(); i++) {
            if (!used[i] && matchesKind(placed.get(i), needed)) {
                used[i] = true;
                if (canAssignBijectively(placed, req, reqIdx + 1, used)) {
                    return true;
                }
                used[i] = false;
            }
        }
        return false;
    }

    /** Tìm nguyên liệu phù hợp đầu tiên trong danh sách item đang có của người chơi (hỗ trợ nút Xếp nhanh công thức). */
    public static Item findMatchingItem(Iterable<Item> availableItems, Predicate<Item> extraFilter, IngredientKind kind) {
        if (availableItems == null) return null;
        for (Item item : availableItems) {
            if (matchesKind(item, kind) && (extraFilter == null || extraFilter.test(item))) {
                return item;
            }
        }
        return null;
    }
}
