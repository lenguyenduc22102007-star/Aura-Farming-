package controller.actionmanager;

import model.game_item.Item;
import model.player_inventory.Player;
import model.soil.Cell;

import java.util.Objects;

public class FeedBag implements ToolStrategy {
    private final Player player;
    private String feedName;

    public FeedBag(Player player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    public void loadFeed(String feedName) {
        this.feedName = feedName;
    }

    public String getFeedName() {
        return feedName;
    }

    public Item getSelectedItem() {
        if (feedName != null) {
            Item item = player.getInventory().findByName(feedName);
            if (item != null && player.getInventory().getItemCount(item) > 0) {
                return item;
            }
        }
        return null;
    }

    @Override
    public boolean executeOnTarget(Cell c) {
        return false;
    }

    @Override
    public String getName() {
        return feedName != null ? feedName : "FeedBag";
    }
}
