package test;

import controller.lifecyclemanager.GameManager;
import controller.shopmanager.ItemManager;
import controller.shopmanager.ShopController;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;

public class ShopControllerTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    private static ShopController fresh(Player player) {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        return new ShopController(player, gm);
    }

    private static ItemManager firstSeed(ShopController shop) {
        for (ItemManager im : shop.getCatalog()) {
            if (im.create() instanceof Seed) return im;
        }
        throw new AssertionError("catalog has no seed entry");
    }

    public static void run() {
        ShopController shop = fresh(new Player());
        check(!shop.getCatalog().isEmpty(), "catalog should not be empty");

        Player buyer = new Player();
        ShopController shop2 = fresh(buyer);
        ItemManager seedEntry = firstSeed(shop2);
        int price = seedEntry.create().getPrice();
        int before = buyer.getMoney();

        check(shop2.buyItem(seedEntry, 1), "buying an affordable seed succeeds");
        check(buyer.getMoney() == before - price, "money is reduced by the price");
        Item bought = buyer.getInventory().findByName(seedEntry.getItem());
        check(bought != null && buyer.getInventory().getItemCount(bought) == 1,
                "the bought seed lands in the inventory");
        check(shop2.getLastMessage().startsWith("Bought"), "a success message is recorded");

        check(!shop2.buyItem(null, 1), "null entry is rejected");
        check(!shop2.buyItem(seedEntry, 0), "non-positive quantity is rejected");

        Player broke = new Player();
        ShopController shop3 = fresh(broke);
        broke.deductMoney(broke.getMoney()); // -> 0
        check(!shop3.buyItem(firstSeed(shop3), 1), "cannot buy without money");
        check(shop3.getLastMessage().contains("Not enough money"), "an insufficient-funds message is recorded");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: ShopControllerTest");
    }
}
