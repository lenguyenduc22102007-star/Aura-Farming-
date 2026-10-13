package controller.actionmanager;

import model.crop.Crop;
import model.player_inventory.Player;
import model.soil.Cell;

import java.util.Objects;

public class Hand implements ToolStrategy {
    private final Player player;

    public Hand(Player player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    @Override
    public boolean executeOnTarget(Cell c) {
        if (c == null) {
            return false;
        }

        if (c.getPest() != null) {
            c.handlePest();
            return true;
        }
        Crop crop = c.getCrop();
        if (crop != null && crop.isHarvestable()) {
            Crop harvested =c.harvestCrop();
            if (harvested != null) {
                model.game_item.HarvestedCropItem item = new model.game_item.HarvestedCropItem(harvested.getName(), harvested.getCropData().getValue());
                player.getInventory().addItem(item, 1);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "Hand";
    }
}
