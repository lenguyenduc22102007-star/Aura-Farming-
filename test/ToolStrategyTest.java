package test;

import controller.actionmanager.*;
import model.crop.Crop;
import model.game_item.StandardFertilizer;
import model.game_item.TomatoSeed;
import model.player_inventory.Player;
import model.soil.Beetle;
import model.soil.Cell;
import model.soil.CellState;
import utils.Constant;

public class ToolStrategyTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    public static void run() {
        Cell c1 = new Cell(0, 0);
        check(new Hoe().executeOnTarget(c1) && c1.getCellState() == CellState.TILLED, "Hoe tills untilled soil");

        Cell c2 = new Cell(0, 0);
        c2.till();
        Crop crop = new TomatoSeed().createCrop();
        c2.plantCrop(crop);
        crop.addStress(crop.getCropData().getMaxStress());
        check(new Hoe().executeOnTarget(c2) && c2.getCrop() == null && c2.getCellState() == CellState.UNTILLED, "Hoe clears DEAD crop to UNTILLED");
        check(!new Hoe().executeOnTarget(null), "Hoe on null must return false");

        Cell c3 = new Cell(0, 0);
        int expectedWater = Math.min(Constant.MAX_MOISTURE, c3.getMoistureLevel() + Constant.WATER_PER_USE);
        check(new WateringCan().executeOnTarget(c3) && c3.getMoistureLevel() == expectedWater, "WateringCan adds water");

        Player p1 = new Player();
        TomatoSeed seed1 = new TomatoSeed();
        p1.getInventory().addItem(seed1, 2);
        SeedBag bag1 = new SeedBag(p1);
        bag1.loadSeed("Tomato Seed");
        Cell c4 = new Cell(0, 0);
        c4.till();
        check(bag1.executeOnTarget(c4) && c4.getCrop() != null && p1.getInventory().getItemCount(seed1) == 1, "SeedBag plants & consumes 1");

        Cell c5 = new Cell(0, 0); c5.till();
        check(!new SeedBag(new Player()).executeOnTarget(c5) && c5.getCrop() == null, "Empty SeedBag must not plant");

        Cell c6 = new Cell(0, 0);
        check(!bag1.executeOnTarget(c6) && p1.getInventory().getItemCount(seed1) == 1, "Fails on untilled, keeps seed");

        Player p4 = new Player();
        StandardFertilizer fert = new StandardFertilizer();
        p4.getInventory().addItem(fert, 1);
        FertilizerBag fertilizerBag = new FertilizerBag("Standard Fertilizer", p4);
        Cell c7 = new Cell(0, 0);
        c7.updateNutrientAmount(-c7.getNutrientLevel());
        check(fertilizerBag.executeOnTarget(c7) && c7.getNutrientLevel() == fert.getNutrientAmount() && p4.getInventory().getItemCount(fert) == 0, "FertilizerBag adds nutrient & consumes");

        Cell c8 = new Cell(0, 0);
        int beforeNutrient = c8.getNutrientLevel();
        check(!new FertilizerBag("Standard Fertilizer", new Player()).executeOnTarget(c8) && c8.getNutrientLevel() == beforeNutrient, "Empty FertilizerBag failed");

        Cell c9 = new Cell(0, 0);
        c9.spawnPest(new Beetle());
        check(new Hand(new Player()).executeOnTarget(c9) && c9.getPest() == null, "Hand should remove a pest");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: ToolStrategyTest");
    }
}
