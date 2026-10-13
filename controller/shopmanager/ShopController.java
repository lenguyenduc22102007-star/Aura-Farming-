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

    public ShopController(Player player, GameManager gameManager) {
        super(gameManager);
        this.player = player;
        seedCatalog();
    }

    private void seedCatalog() {
        catalog.add(new SeedItem("Tomato Seed", TomatoSeed::new));
        catalog.add(new SeedItem("Sunflower Seed", SunflowerSeed::new));
        catalog.add(new SeedItem("Potato Seed", PotatoSeed::new));
        catalog.add(new SeedItem("Corn Seed", CornSeed::new));
        catalog.add(new SeedItem("Water Rice Seed", WaterRiceSeed::new));
        catalog.add(new FertilizerItem("Standard Fertilizer", StandardFertilizer::new));
        
        // Thú vật non
        catalog.add(new AnimalShopItem("Baby Chicken", BabyChickenItem::new));
        catalog.add(new AnimalShopItem("Baby Pig", BabyPigItem::new));
        catalog.add(new AnimalShopItem("Baby Cow", BabyCowItem::new));
        
        // Đồ ăn cho thú
        catalog.add(new AnimalShopItem("Animal Feed", AnimalFeedItem::new));
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

        int totalCost;
        try {
            totalCost = Math.multiplyExact(prototype.getPrice(), quantity);
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
