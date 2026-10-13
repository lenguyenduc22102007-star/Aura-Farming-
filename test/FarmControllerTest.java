package test;

import controller.farmcontroller.FarmController;
import controller.lifecyclemanager.GameManager;
import model.crop.*;
import model.soil.Cell;
import model.soil.FarmMap;

public class FarmControllerTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    private static Crop plantCrop(FarmMap map) {
        Cell c = map.getCell(0, 0);
        c.till();
        Crop crop = new model.crop.Tomato();
        c.plantCrop(crop);
        c.updateMoistureAmount(100);
        c.updateNutrientAmount(100);
        c.updateSunlightAmount(100);
        return crop;
    }

    public static void run() {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        FarmMap map = new FarmMap();
        Crop crop = plantCrop(map);
        int before = crop.getCurrentGrowthProgress();
        new FarmController(gm, map).processGrowth();
        check(crop.getCurrentGrowthProgress() > before, "processGrowth should advance growth (before = " + before + ")");

        gm.reset();
        FarmMap map2 = new FarmMap();
        new FarmController(gm, map2);
        Crop crop2 = plantCrop(map2);
        gm.advanceDay();
        check(crop2.getCurrentGrowthProgress() > 0, "dayEnded should grow the crop");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: FarmControllerTest");
    }
}
