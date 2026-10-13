package controller.lifecyclemanager;

import model.crop.Crop;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.FarmMap;

import java.util.Map;

public final class BankruptcyCondition implements GameOverCondition {
    private final int cheapestSeedPrice;

    public BankruptcyCondition(int cheapestSeedPrice) {
        this.cheapestSeedPrice = cheapestSeedPrice;
    }

    @Override
    public GameStatus evaluate(Player player, FarmMap farmMap, int currentDay) {
        if (player.getMoney() >= cheapestSeedPrice) {
            return GameStatus.RUNNING;
        }
        if (ownsAnySeed(player)) {
            return GameStatus.RUNNING;
        }
        if (hasLivingCrop(farmMap)) {
            return GameStatus.RUNNING;
        }
        return GameStatus.LOST;
    }

    private boolean ownsAnySeed(Player player) {
        for (Map.Entry<Item, Integer> entry : player.getInventory().getItems().entrySet()) {
            if (entry.getKey() instanceof Seed && entry.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLivingCrop(FarmMap farmMap) {
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                Crop crop = c.getCrop();
                if (crop != null && !crop.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }
}
