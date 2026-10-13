package controller.actionmanager;

import controller.lifecyclemanager.BaseController;
import controller.lifecyclemanager.GameManager;
import model.player_inventory.Player;
import model.soil.Cell;

public class PlayerController extends BaseController {
    private final Player player;
    private ToolStrategy currentTool;

    public PlayerController(Player player, GameManager gameManager) {
        super(gameManager);
        this.player = player;
        this.currentTool = new Hand(player);
    }

    public Player getPlayer() {
        return player;
    }
    public ToolStrategy getCurrentTool() {
        return currentTool;
    }

    public void useTool(ToolStrategy tool) {
        if (tool != null) {
            this.currentTool = tool;
        }
    }

    public boolean interactWithCell(Cell cell) {
        if (currentTool == null || cell == null) {
            return false;
        }
        return currentTool.executeOnTarget(cell);
    }
}
