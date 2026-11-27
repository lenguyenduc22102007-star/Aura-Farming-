package model.factory;

import model.game_item.AnimalProductItem;
import model.game_item.FactoryProductItem;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.player_inventory.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Quản lý 3 Dây Chuyền Sản Xuất Chạy Ngầm Theo Thời Gian Thực (Factory Production Lines):
 * - Tiếp tục vận hành ngay cả khi người chơi rời khỏi Nhà Máy ra đồng làm việc.
 * - Dây chuyền 1 (Ép & Đóng Chai): 1 Cà chua -> Nước Ép Cà Chua (8s, $200, +60 Khát).
 * - Dây chuyền 2 (Chế Biến Thực Phẩm): 1 Khoai tây -> Snack Khoai Tây (8s, $95) | 1 Ngô -> Bỏng Ngô Bơ (9s, $160) | 1 Sữa Bò -> Phô Mai Đóng Hộp (10s, $220, +75 Đói).
 * - Dây chuyền 3 (Xay Xát & Tinh Chế): 1 Lúa nước -> Gạo Trắng Đóng Bao (10s, $260) | 1 Hướng dương -> Dầu Hướng Dương (9s, $130).
 */
public class FactoryManager {

    public static final int MAX_QUEUE_SIZE = 20;
    public static final int MAX_SPEED_LEVEL = 3;

    /** Công thức chuyển đổi 1 nguyên liệu thô thành 1 thành phẩm đóng gói trên dây chuyền. */
    public static final class LineRecipe {
        private final String recipeId;
        private final String inputDisplayName;
        private final String inputCropOrProductName; // "Tomato", "Potato", "Corn", "Fresh Milk", "Water Rice", "Sunflower"
        private final boolean animalProductInput;
        private final String outputProductId;
        private final double baseDurationSec;

        public LineRecipe(String recipeId, String inputDisplayName, String inputCropOrProductName,
                          boolean animalProductInput, String outputProductId, double baseDurationSec) {
            this.recipeId = recipeId;
            this.inputDisplayName = inputDisplayName;
            this.inputCropOrProductName = inputCropOrProductName;
            this.animalProductInput = animalProductInput;
            this.outputProductId = outputProductId;
            this.baseDurationSec = baseDurationSec;
        }

        public String getRecipeId() { return recipeId; }
        public String getInputDisplayName() { return inputDisplayName; }
        public String getInputCropOrProductName() { return inputCropOrProductName; }
        public boolean isAnimalProductInput() { return animalProductInput; }
        public String getOutputProductId() { return outputProductId; }
        public double getBaseDurationSec() { return baseDurationSec; }

        public Item createInputPrototype() {
            if (animalProductInput) {
                return new AnimalProductItem(inputCropOrProductName, 45);
            }
            return new HarvestedCropItem(inputCropOrProductName, 25);
        }

        public FactoryProductItem createOutputProduct() {
            return FactoryProductItem.createById(outputProductId);
        }

        public boolean matchesInventoryItem(Item item) {
            if (item == null) return false;
            if (animalProductInput) {
                return (item instanceof AnimalProductItem) && item.getName().equalsIgnoreCase(inputCropOrProductName);
            }
            return (item instanceof HarvestedCropItem)
                    && ((HarvestedCropItem) item).getCropName().equalsIgnoreCase(inputCropOrProductName);
        }
    }

    /** Trạng thái của 1 Dây chuyền công nghiệp (Phễu hàng đợi -> Máy xử lý -> Kho đầu ra). */
    public static final class ProductionLine {
        private final int lineIndex;
        private final String lineTitle;
        private final String lineSubtitle;
        private final List<LineRecipe> recipes;
        private String selectedRecipeId;
        private int speedLevel = 1; // 1 -> x1.0, 2 -> x2.0 ($500), 3 -> x3.0 ($1000)
        private final Deque<String> inputQueue = new ArrayDeque<>();
        private String activeRecipeId = null;
        private double progressSec = 0.0;
        private double currentDurationSec = 8.0;

        public ProductionLine(int lineIndex, String lineTitle, String lineSubtitle, List<LineRecipe> recipes) {
            this.lineIndex = lineIndex;
            this.lineTitle = lineTitle;
            this.lineSubtitle = lineSubtitle;
            this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
            this.selectedRecipeId = recipes.isEmpty() ? null : recipes.get(0).getRecipeId();
        }

        public int getLineIndex() { return lineIndex; }
        public String getLineTitle() { return lineTitle; }
        public String getLineSubtitle() { return lineSubtitle; }
        public List<LineRecipe> getRecipes() { return recipes; }
        public String getSelectedRecipeId() { return selectedRecipeId; }
        public void setSelectedRecipeId(String id) { this.selectedRecipeId = id; }
        public int getSpeedLevel() { return speedLevel; }
        public int getQueueSize() { return inputQueue.size(); }
        public Deque<String> getInputQueue() { return inputQueue; }
        public String getActiveRecipeId() { return activeRecipeId; }
        public boolean isRunning() { return activeRecipeId != null; }

        public double getSpeedMultiplier() {
            return speedLevel; // Lv1 = 1x, Lv2 = 2x, Lv3 = 3x
        }

        public int getUpgradeCost() {
            if (speedLevel == 1) return 500;
            if (speedLevel == 2) return 1000;
            return 0;
        }

        public double getProgressRatio() {
            if (activeRecipeId == null || currentDurationSec <= 0.001) return 0.0;
            return Math.max(0.0, Math.min(1.0, progressSec / currentDurationSec));
        }

        public double getRemainingSec() {
            if (activeRecipeId == null) return 0.0;
            return Math.max(0.0, (currentDurationSec - progressSec) / getSpeedMultiplier());
        }

        public LineRecipe findRecipe(String id) {
            if (id == null) return null;
            for (LineRecipe r : recipes) {
                if (r.getRecipeId().equals(id)) return r;
            }
            return null;
        }

        public LineRecipe getSelectedRecipe() {
            LineRecipe found = findRecipe(selectedRecipeId);
            return found != null ? found : (recipes.isEmpty() ? null : recipes.get(0));
        }
    }

    private final List<ProductionLine> lines = new ArrayList<>();
    /** Kho thành phẩm chung của Nhà Máy (productId -> số lượng). */
    private final Map<String, Integer> outputStorage = new LinkedHashMap<>();

    public FactoryManager() {
        // Dây chuyền 1: Ép & Đóng Chai
        List<LineRecipe> line1 = new ArrayList<>();
        line1.add(new LineRecipe(
            "rec_tomato_juice", "Cà chua", "Tomato", false,
            FactoryProductItem.ID_TOMATO_JUICE, 8.0
        ));
        lines.add(new ProductionLine(0, "DÂY CHUYỀN 1 — ÉP & ĐÓNG CHAI", "Nước giải khát đóng chai tự động", line1));

        // Dây chuyền 2: Chế Biến Thực Phẩm
        List<LineRecipe> line2 = new ArrayList<>();
        line2.add(new LineRecipe(
            "rec_potato_snack", "Khoai tây", "Potato", false,
            FactoryProductItem.ID_POTATO_SNACK, 8.0
        ));
        line2.add(new LineRecipe(
            "rec_butter_popcorn", "Ngô", "Corn", false,
            FactoryProductItem.ID_BUTTER_POPCORN, 9.0
        ));
        line2.add(new LineRecipe(
            "rec_canned_cheese", "Sữa Bò", "Fresh Milk", true,
            FactoryProductItem.ID_CANNED_CHEESE, 10.0
        ));
        lines.add(new ProductionLine(1, "DÂY CHUYỀN 2 — CHẾ BIẾN THỰC PHẨM", "Snack giòn, Bỏng ngô bơ & Phô mai hộp", line2));

        // Dây chuyền 3: Xay Xát & Tinh Chế
        List<LineRecipe> line3 = new ArrayList<>();
        line3.add(new LineRecipe(
            "rec_packed_rice", "Lúa nước", "Water Rice", false,
            FactoryProductItem.ID_PACKED_RICE, 10.0
        ));
        line3.add(new LineRecipe(
            "rec_sunflower_oil", "Hướng dương", "Sunflower", false,
            FactoryProductItem.ID_SUNFLOWER_OIL, 9.0
        ));
        lines.add(new ProductionLine(2, "DÂY CHUYỀN 3 — XAY XÁT & TINH CHẾ", "Đóng bao gạo trắng & Ép dầu thực vật", line3));
    }

    public List<ProductionLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Map<String, Integer> getOutputStorage() {
        return Collections.unmodifiableMap(outputStorage);
    }

    /** Chạy ngầm mỗi khung hình (cả khi đang ở ngoài đồng lẫn khi mở FactoryView). */
    public void update(double dtSec) {
        if (dtSec <= 0.0) return;
        for (ProductionLine line : lines) {
            if (line.activeRecipeId == null && !line.inputQueue.isEmpty()) {
                String nextRec = line.inputQueue.pollFirst();
                LineRecipe spec = line.findRecipe(nextRec);
                if (spec != null) {
                    line.activeRecipeId = nextRec;
                    line.progressSec = 0.0;
                    line.currentDurationSec = spec.getBaseDurationSec();
                }
            }
            if (line.activeRecipeId != null) {
                line.progressSec += dtSec * line.getSpeedMultiplier();
                while (line.activeRecipeId != null && line.progressSec >= line.currentDurationSec) {
                    double leftover = line.progressSec - line.currentDurationSec;
                    LineRecipe finished = line.findRecipe(line.activeRecipeId);
                    if (finished != null) {
                        outputStorage.merge(finished.getOutputProductId(), 1, Integer::sum);
                    }
                    if (!line.inputQueue.isEmpty()) {
                        String nextRec = line.inputQueue.pollFirst();
                        LineRecipe nextSpec = line.findRecipe(nextRec);
                        if (nextSpec != null) {
                            line.activeRecipeId = nextRec;
                            line.currentDurationSec = nextSpec.getBaseDurationSec();
                            line.progressSec = Math.min(leftover, line.currentDurationSec * 0.95);
                        } else {
                            line.activeRecipeId = null;
                            line.progressSec = 0.0;
                        }
                    } else {
                        line.activeRecipeId = null;
                        line.progressSec = 0.0;
                    }
                }
            }
        }
    }

    /** Đếm số lượng nguyên liệu phù hợp đang có trong túi người chơi cho một công thức. */
    public int countAvailableInput(Player player, LineRecipe recipe) {
        if (player == null || recipe == null) return 0;
        int sum = 0;
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            if (e.getValue() > 0 && recipe.matchesInventoryItem(e.getKey())) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    /** Nạp thêm nguyên liệu từ túi của người chơi vào phễu hàng đợi của dây chuyền (tối đa 20 món). */
    public int enqueueInput(Player player, int lineIndex, int requestedCount) {
        if (player == null || lineIndex < 0 || lineIndex >= lines.size() || requestedCount <= 0) return 0;
        ProductionLine line = lines.get(lineIndex);
        LineRecipe recipe = line.getSelectedRecipe();
        if (recipe == null) return 0;

        int spaceLeft = MAX_QUEUE_SIZE - line.inputQueue.size();
        if (spaceLeft <= 0) return 0;

        int added = 0;
        int limit = Math.min(requestedCount, spaceLeft);
        for (int k = 0; k < limit; k++) {
            Item matchItem = null;
            for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
                if (e.getValue() > 0 && recipe.matchesInventoryItem(e.getKey())) {
                    matchItem = e.getKey();
                    break;
                }
            }
            if (matchItem == null || !player.getInventory().removeItem(matchItem, 1)) {
                break;
            }
            line.inputQueue.addLast(recipe.getRecipeId());
            added++;
        }
        return added;
    }

    /** Nâng cấp tốc độ dây chuyền (Cấp 1 -> Cấp 2 giá $500; Cấp 2 -> Cấp 3 giá $1000). */
    public boolean upgradeLineSpeed(Player player, int lineIndex) {
        if (player == null || lineIndex < 0 || lineIndex >= lines.size()) return false;
        ProductionLine line = lines.get(lineIndex);
        if (line.speedLevel >= MAX_SPEED_LEVEL) return false;
        int cost = line.getUpgradeCost();
        if (!player.deductMoney(cost)) return false;
        line.speedLevel++;
        return true;
    }

    public int getTotalOutputCount() {
        int total = 0;
        for (int v : outputStorage.values()) {
            if (v > 0) total += v;
        }
        return total;
    }

    public int computeTotalOutputValue(MarketManager market) {
        int total = 0;
        for (Map.Entry<String, Integer> e : outputStorage.entrySet()) {
            int qty = e.getValue();
            if (qty <= 0) continue;
            FactoryProductItem prod = FactoryProductItem.createById(e.getKey());
            if (prod != null) {
                int unit = (market != null) ? market.getSellUnitPrice(prod) : prod.getPrice();
                total += unit * qty;
            }
        }
        return total;
    }

    /** Xuất bán toàn bộ kho thành phẩm Nhà Máy (áp dụng nhân 1.5x nếu trúng món SỐT GIÁ hôm nay). */
    public int sellAllOutputs(Player player, MarketManager market) {
        if (player == null) return 0;
        int earned = computeTotalOutputValue(market);
        if (earned <= 0) return 0;
        outputStorage.clear();
        player.addMoney(earned);
        return earned;
    }

    /** Chuyển toàn bộ thành phẩm từ kho Nhà Máy về túi đồ cá nhân (để ăn/uống hoặc tặng NPC +1 Tim). */
    public int collectAllToInventory(Player player) {
        if (player == null) return 0;
        int moved = 0;
        for (Map.Entry<String, Integer> e : new ArrayList<>(outputStorage.entrySet())) {
            int qty = e.getValue();
            if (qty <= 0) continue;
            FactoryProductItem prod = FactoryProductItem.createById(e.getKey());
            if (prod != null) {
                player.getInventory().addItem(prod, qty);
                moved += qty;
            }
        }
        outputStorage.clear();
        return moved;
    }

    public void save(Properties p) {
        if (p == null) return;
        for (int i = 0; i < lines.size(); i++) {
            ProductionLine ln = lines.get(i);
            String prefix = "factory.line." + i + ".";
            p.setProperty(prefix + "speed", Integer.toString(ln.speedLevel));
            p.setProperty(prefix + "selected", ln.selectedRecipeId != null ? ln.selectedRecipeId : "");
            p.setProperty(prefix + "active", ln.activeRecipeId != null ? ln.activeRecipeId : "");
            p.setProperty(prefix + "progress", Double.toString(ln.progressSec));
            p.setProperty(prefix + "duration", Double.toString(ln.currentDurationSec));
            p.setProperty(prefix + "queue", String.join(",", ln.inputQueue));
        }
        int outIdx = 0;
        for (Map.Entry<String, Integer> e : outputStorage.entrySet()) {
            if (e.getValue() > 0) {
                p.setProperty("factory.out." + (outIdx++), e.getKey() + "|" + e.getValue());
            }
        }
        p.setProperty("factory.out.count", Integer.toString(outIdx));
    }

    public void load(Properties p) {
        if (p == null) return;
        for (int i = 0; i < lines.size(); i++) {
            ProductionLine ln = lines.get(i);
            String prefix = "factory.line." + i + ".";
            ln.speedLevel = Math.max(1, Math.min(MAX_SPEED_LEVEL, Integer.parseInt(p.getProperty(prefix + "speed", "1"))));
            String sel = p.getProperty(prefix + "selected", "");
            if (!sel.isEmpty() && ln.findRecipe(sel) != null) {
                ln.selectedRecipeId = sel;
            }
            String act = p.getProperty(prefix + "active", "");
            ln.activeRecipeId = (!act.isEmpty() && ln.findRecipe(act) != null) ? act : null;
            ln.progressSec = Double.parseDouble(p.getProperty(prefix + "progress", "0.0"));
            ln.currentDurationSec = Double.parseDouble(p.getProperty(prefix + "duration", "8.0"));
            ln.inputQueue.clear();
            String qRaw = p.getProperty(prefix + "queue", "");
            if (!qRaw.isEmpty()) {
                for (String token : qRaw.split(",")) {
                    String t = token.trim();
                    if (!t.isEmpty() && ln.findRecipe(t) != null && ln.inputQueue.size() < MAX_QUEUE_SIZE) {
                        ln.inputQueue.addLast(t);
                    }
                }
            }
        }
        outputStorage.clear();
        int outCount = Integer.parseInt(p.getProperty("factory.out.count", "0"));
        for (int i = 0; i < outCount; i++) {
            String raw = p.getProperty("factory.out." + i);
            if (raw == null || raw.isEmpty()) continue;
            String[] parts = raw.split("\\|");
            if (parts.length >= 2) {
                int qty = Integer.parseInt(parts[1]);
                if (qty > 0 && FactoryProductItem.createById(parts[0]) != null) {
                    outputStorage.put(parts[0], qty);
                }
            }
        }
    }
}
