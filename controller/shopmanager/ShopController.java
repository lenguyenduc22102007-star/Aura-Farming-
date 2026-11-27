package controller.shopmanager;

import controller.lifecyclemanager.BaseController;
import controller.lifecyclemanager.GameManager;
import model.game_item.*;
import model.player_inventory.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShopController extends BaseController {
    private final List<ItemManager> catalog = new ArrayList<>();
    private String lastMessage = "";
    private final Player player;
    private boolean namiDiscountActive = false;
    private boolean orchardUnlocked = false;

    public ShopController(Player player, GameManager gameManager) {
        super(gameManager);
        this.player = player;
        seedCatalog();
    }

    public void setNamiDiscountActive(boolean active) {
        this.namiDiscountActive = active;
    }

    public boolean isNamiDiscountActive() {
        return namiDiscountActive;
    }

    public void setOrchardUnlocked(boolean unlocked) {
        this.orchardUnlocked = unlocked;
    }

    public boolean isOrchardUnlocked() {
        return orchardUnlocked;
    }

    public static boolean isOrchardTreeSeed(Item proto) {
        return proto instanceof OrangeSeed || proto instanceof AppleSeed || proto instanceof CoconutSeed;
    }

    public int getEffectiveUnitPrice(Item prototype) {
        if (prototype == null) return 0;
        int base = prototype.getPrice();
        if (namiDiscountActive && base > 1) {
            return Math.max(1, (int) Math.round(base * 0.85));
        }
        return base;
    }

    private void seedCatalog() {
        catalog.add(new SeedItem("Tomato Seed", TomatoSeed::new));
        catalog.add(new SeedItem("Sunflower Seed", SunflowerSeed::new));
        catalog.add(new SeedItem("Potato Seed", PotatoSeed::new));
        catalog.add(new SeedItem("Corn Seed", CornSeed::new));
        catalog.add(new SeedItem("Water Rice Seed", WaterRiceSeed::new));

        // Cây ăn quả lâu năm (mở khóa khi mua Vườn Cây Ăn Quả $3.000)
        catalog.add(new SeedItem("Orange Seed", OrangeSeed::new));
        catalog.add(new SeedItem("Apple Seed", AppleSeed::new));
        catalog.add(new SeedItem("Coconut Seed", CoconutSeed::new));

        catalog.add(new FertilizerItem("Standard Fertilizer", StandardFertilizer::new));
        catalog.add(new AnimalShopItem("Củi khô (Firewood)", ForageItem::firewood));
        
        // Thú vật non
        catalog.add(new AnimalShopItem("Baby Chicken", BabyChickenItem::new));
        catalog.add(new AnimalShopItem("Baby Pig", BabyPigItem::new));
        catalog.add(new AnimalShopItem("Baby Cow", BabyCowItem::new));
        
        // Đồ ăn cho thú
        catalog.add(new AnimalShopItem("Animal Feed", AnimalFeedItem::new));
        catalog.add(new AnimalShopItem("Chicken Feed", ChickenFeedItem::new));
        catalog.add(new AnimalShopItem("Cow Feed", CowFeedItem::new));
        catalog.add(new AnimalShopItem("Pig Feed", PigFeedItem::new));
    }
    public List<ItemManager> getCatalog() {
        return Collections.unmodifiableList(catalog);
    }
    public String getLastMessage() {
        return lastMessage;
    }

    public boolean buyItem(ItemManager entry, int quantity) {
        if (entry == null || quantity <= 0) {
            lastMessage = "Invalid purchase request.";
            return false;
        }

        Item prototype;
        try {
            prototype = entry.create();
        } catch (RuntimeException e) {
            lastMessage = "This item is currently unavailable.";
            return false;
        }
        if (prototype == null) {
            lastMessage = "This item is currently unavailable.";
            return false;
        }
        if (isOrchardTreeSeed(prototype) && !orchardUnlocked) {
            lastMessage = "🔒 Hãy mua mở khóa Vườn Cây Ăn Quả ($3.000) trên bản đồ trước!";
            return false;
        }

        int unitCost = getEffectiveUnitPrice(prototype);
        int totalCost;
        try {
            totalCost = Math.multiplyExact(unitCost, quantity);
        } catch (ArithmeticException overflow) {
            lastMessage = "That quantity is too large to purchase.";
            return false;
        }

        if (!player.deductMoney(totalCost)) {
            lastMessage = "Not enough money for " + prototype.getName() + ".";
            return false;
        }

        Item canonical = player.getInventory().findByName(prototype.getName());
        if (canonical == null) {
            canonical = prototype;
        }
        player.getInventory().addItem(canonical, quantity);
        lastMessage = "Bought " + quantity + " x " + prototype.getName() + " for $" + totalCost + ".";
        return true;
    }
}
