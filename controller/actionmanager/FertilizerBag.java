package controller.actionmanager;

import model.game_item.Fertilizer;
import model.game_item.Item;
import model.player_inventory.Player;
import model.soil.Cell;

import java.util.Objects;

public class FertilizerBag implements ToolStrategy {
    private final String fertilizerName;
    private final Player player;

    public FertilizerBag(String fertilizerName, Player player) {
        this.fertilizerName = fertilizerName;
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    @Override
    public boolean executeOnTarget(Cell c) {
        if (c == null || !c.isFarmable()) {
            return false;
        }

        Item item = player.getInventory().findByName(fertilizerName);
        if (!(item instanceof Fertilizer fert)) {
            return false;
        }
        if (player.getInventory().getItemCount(fert) <= 0) {
            return false;
        }
        fert.addNutrientAmount(c);
        player.getInventory().removeItem(fert, 1);

        return true;
    }

    @Override
    public String getName() {
        return fertilizerName;
    }
}
