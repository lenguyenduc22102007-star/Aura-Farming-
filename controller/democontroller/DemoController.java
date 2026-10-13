package controller.democontroller;

import controller.lifecyclemanager.BaseController;
import controller.lifecyclemanager.GameManager;
import model.crop.Crop;
import model.soil.Beetle;
import model.soil.Cell;
import model.soil.CellState;
import model.soil.FarmMap;
import model.weather.Weather;
import model.weather.WeatherType;

import java.util.Random;

public class DemoController extends BaseController {
    private final FarmMap farmMap;

    public DemoController(GameManager gameManager, FarmMap farmMap) {
        super(gameManager);
        this.farmMap = farmMap;
    }

    public Weather forceWeather(WeatherType type) {
        Weather w = Weather.of(type);
        gameManager.setCurrentWeather(w);
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                w.applyWeatherEffect(c);
            }
        }
        return w;
    }

    public boolean forceFastGrowthCrop(Cell cell) {
        if (cell == null || cell.getCrop() == null) {
            return false;
        }
        Crop crop = cell.getCrop();
        for (int i = 0; i < 5; i++) {
            crop.dailyUpdate(cell, gameManager.getCurrentWeather());
        }
        return true;
    }

    public void fastForwardGrowthMap() {
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                if (c.getCrop() != null) {
                    forceFastGrowthCrop(c);
                }
            }
        }
    }

    public boolean spawnDemoPest() {
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                if (c.getCrop() != null && c.getPest() == null) {
                    c.spawnPest(new Beetle());
                    return true;
                }
            }
        }
        return false;
    }

    public void tillAll() {
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                if (c.getCellState() == CellState.UNTILLED) {
                    c.till();
                }
            }
        }
    }
}
