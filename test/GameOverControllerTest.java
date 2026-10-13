package test;

import controller.lifecyclemanager.BankruptcyCondition;
import controller.lifecyclemanager.GameManager;
import controller.lifecyclemanager.GameOverController;
import controller.lifecyclemanager.GameStatus;
import model.game_item.TomatoSeed;
import model.player_inventory.Player;
import model.soil.Cell;
import model.soil.FarmMap;

import java.util.List;

public class GameOverControllerTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    public static void run() {
        // Bankruptcy Condition (LOST)
        BankruptcyCondition broke = new BankruptcyCondition(10);

        check(broke.evaluate(new Player(), new FarmMap(), 1) == GameStatus.RUNNING,
                "a player who can afford a seed is not bankrupt");

        Player noMoney = new Player();
        noMoney.deductMoney(noMoney.getMoney()); // -> 0
        check(broke.evaluate(noMoney, new FarmMap(), 1) == GameStatus.LOST,
                "broke + no seeds + empty farm must LOSE");

        Player noMoneyButSeed = new Player();
        noMoneyButSeed.deductMoney(noMoneyButSeed.getMoney());
        noMoneyButSeed.getInventory().addItem(new TomatoSeed(), 1);
        check(broke.evaluate(noMoneyButSeed, new FarmMap(), 1) == GameStatus.RUNNING,
                "owning a seed is still a way back");

        Player noMoneyLivingCrop = new Player();
        noMoneyLivingCrop.deductMoney(noMoneyLivingCrop.getMoney());
        FarmMap mapWithCrop = new FarmMap();
        Cell c = mapWithCrop.getCell(0, 0);
        c.till();
        c.plantCrop(new TomatoSeed().createCrop());
        check(broke.evaluate(noMoneyLivingCrop, mapWithCrop, 1) == GameStatus.RUNNING,
                "a living crop can still be harvested for income");

        // GameOverController integration
        GameManager gm = GameManager.getInstance();

        gm.reset();
        final GameStatus[] firedLose = {null};
        Player loser = new Player();
        loser.deductMoney(loser.getMoney());
        new GameOverController(gm, new FarmMap(), loser,
                List.of(new BankruptcyCondition(10)), s -> firedLose[0] = s, 1);
        gm.advanceDay();
        check(firedLose[0] == null, "loss must be delayed during the grace period");
        gm.advanceDay();
        check(firedLose[0] == GameStatus.LOST, "loss should be announced after the grace period");
        check(gm.getStatus() == GameStatus.LOST, "GameManager status should be LOST");

        int dayAtLoss = gm.getCurrentDay();
        gm.advanceDay();
        check(gm.getCurrentDay() == dayAtLoss, "advanceDay must be a no-op after the run ends");

        gm.reset();
        final GameStatus[] firedNow = {null};
        Player loser0 = new Player();
        loser0.deductMoney(loser0.getMoney());
        new GameOverController(gm, new FarmMap(), loser0,
                List.of(new BankruptcyCondition(10)), s -> firedNow[0] = s, 0);
        gm.advanceDay();
        check(firedNow[0] == GameStatus.LOST, "graceDays=0 should lose immediately");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: GameOverControllerTest");
    }
}
