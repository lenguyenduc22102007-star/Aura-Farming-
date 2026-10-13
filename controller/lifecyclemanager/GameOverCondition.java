package controller.lifecyclemanager;

import model.player_inventory.Player;
import model.soil.FarmMap;

public interface GameOverCondition {
    GameStatus evaluate(Player player, FarmMap farmMap, int currentDay);
}
