package test;

import controller.actionmanager.Hoe;
import controller.actionmanager.PlayerController;
import controller.lifecyclemanager.GameManager;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.CellState;
import model.soil.FarmMap;
import utils.Constant;

public class PlayerControllerTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    private static PlayerController fresh() {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        return new PlayerController(new Player(), gm);
    }

    public static void run() {
        PlayerController pc = fresh();
        check("Hand".equals(pc.getCurrentTool().getName()), "default tool should be Hand");

        Hoe hoe = new Hoe();
        pc.useTool(hoe);
        check(pc.getCurrentTool() == hoe, "useTool should set the active tool");
        pc.useTool(null);
        check(pc.getCurrentTool() == hoe, "a null tool must be ignored");

        Cell c = new Cell(0, 0);
        PlayerController pc2 = fresh();
        pc2.useTool(hoe);

        check(pc2.interactWithCell(c) && c.getCellState() == CellState.TILLED, "Hoe should succeed and till the cell");
        check(!fresh().interactWithCell(null), "null cell must be a safe return");

        FarmMap zonedMap = new FarmMap(4, 4);
        zonedMap.applyFarmZone(Constant.CELL_SIZE, Constant.CELL_SIZE,
                Constant.CELL_SIZE * 2, Constant.CELL_SIZE * 2);
        check(!pc2.interactWithCell(zonedMap.getCell(0, 0)),
                "Hoe interaction through PlayerController must fail outside FarmZone");
        check(pc2.interactWithCell(zonedMap.getCell(1, 1)),
                "Hoe interaction through PlayerController must succeed inside FarmZone");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: PlayerControllerTest");
    }
}
