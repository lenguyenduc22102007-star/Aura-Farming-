package test;

import controller.democontroller.DemoController;
import controller.lifecyclemanager.GameManager;
import model.crop.Crop;
import model.game_item.TomatoSeed;
import model.soil.Cell;
import model.soil.CellState;
import model.soil.FarmMap;
import model.weather.Weather;
import model.weather.WeatherType;

public class DemoControllerTest {
    private static void check(boolean pass, String msg) {
        if (!pass) {
            throw new AssertionError(msg);
        }
    }

    private static DemoController fresh(FarmMap map) {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        return new DemoController(gm, map);
    }

    private static Cell plantedCell(FarmMap map, int row, int col) {
        Cell cell = map.getCell(row, col);
        cell.till();
        cell.plantCrop(new TomatoSeed().createCrop());
        cell.updateMoistureAmount(100);
        cell.updateNutrientAmount(100);
        cell.updateSunlightAmount(100);
        return cell;
    }

    public static void run() {
        FarmMap map = new FarmMap();
        DemoController demo = fresh(map);
        Cell sample = map.getCell(0, 0);
        int sunBefore = sample.getSunlightLevel();
        int moistBefore = sample.getMoistureLevel();

        Weather w = demo.forceWeather(WeatherType.SUNNY);
        check(w.getType() == WeatherType.SUNNY, "forceWeather returns the chosen weather");
        check(GameManager.getInstance().getCurrentWeather().getType() == WeatherType.SUNNY,
                "forceWeather updates the GameManager's current weather");
        check(sample.getSunlightLevel() > sunBefore, "sunny weather raises sunlight immediately");
        check(sample.getMoistureLevel() < moistBefore, "sunny weather lowers moisture immediately");

        FarmMap map2 = new FarmMap();
        DemoController demo2 = fresh(map2);
        demo2.tillAll();
        check(map2.getCell(0, 0).getCellState() == CellState.TILLED, "tillAll tills untilled cells");

        FarmMap map3 = new FarmMap();
        DemoController demo3 = fresh(map3);
        check(!demo3.spawnDemoPest(), "no crops -> nothing to infest");

        Cell cropCell = plantedCell(map3, 1, 1);
        check(demo3.spawnDemoPest(), "a crop-bearing cell can be infested");
        check(cropCell.getPest() != null, "spawnDemoPest places a pest on the crop cell");

        FarmMap map4 = new FarmMap();
        DemoController demo4 = fresh(map4);
        check(!demo4.forceFastGrowthCrop(null), "null cell is a safe false");
        check(!demo4.forceFastGrowthCrop(map4.getCell(0, 0)), "an empty cell cannot be fast-grown");

        Cell grow = plantedCell(map4, 2, 2);
        Crop crop = grow.getCrop();
        int before = crop.getCurrentGrowthProgress();
        check(demo4.forceFastGrowthCrop(grow), "fast-growing a planted crop succeeds");
        check(crop.getCurrentGrowthProgress() > before, "fast-grow advances the crop's progress");

        FarmMap map5 = new FarmMap();
        DemoController demo5 = fresh(map5);
        Cell c = plantedCell(map5, 0, 0);
        Crop crop2 = c.getCrop();
        int before2 = crop2.getCurrentGrowthProgress();
        demo5.fastForwardGrowthMap();
        check(crop2.getCurrentGrowthProgress() > before2, "fastForwardGrowthMap advances planted crops");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: DemoControllerTest");
    }
}
