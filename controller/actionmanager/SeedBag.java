package controller.actionmanager;

import model.crop.Crop;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import model.soil.Cell;

import java.util.Objects;

public class SeedBag implements ToolStrategy {
    private final Player player;
    private String seedName;

    public SeedBag(Player player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }
    public void loadSeed(String seedName) {
        this.seedName = seedName;
    }
    public String getSeedName() {
        return seedName;
    }

    @Override
    public boolean executeOnTarget(Cell c) {
        if (c == null || seedName == null) {
            return false;
        }

        Item item = player.getInventory().findByName(seedName);
        if (!(item instanceof Seed seed)) {
            return false;
        }
        if (player.getInventory().getItemCount(seed) <= 0) {
            return false;
        }
        Crop crop = seed.createCrop();
        if (crop == null) {
            return false;
        }
        if (!c.plantCrop(crop)) {
            return false;
        }
        player.getInventory().removeItem(seed, 1);

        return true;
    }

    @Override
    public String getName() {
        return seedName != null ? seedName : "SeedBag";
    }
}
