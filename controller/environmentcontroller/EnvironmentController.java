package controller.environmentcontroller;

import controller.lifecyclemanager.BaseController;
import controller.lifecyclemanager.DayObserver;
import controller.lifecyclemanager.GameManager;
import model.soil.Beetle;
import model.soil.Cell;
import model.soil.FarmMap;
import model.weather.Weather;
import model.weather.WeatherType;
import utils.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnvironmentController extends BaseController implements DayObserver {
    private final FarmMap farmMap;
    private final Random rng;

    public EnvironmentController(GameManager gameManager, FarmMap farmMap) {
        this(gameManager, farmMap, new Random());
    }

    public EnvironmentController(GameManager gameManager, FarmMap farmMap, Random rng) {
        super(gameManager);
        this.farmMap = farmMap;
        this.rng = rng;
        gameManager.addObserver(this);
    }

    public Weather generateWeather() {
        int roll = rng.nextInt(100);
        WeatherType chosen;

        if (roll < Constant.DROUGHT_CHANCE) {
            chosen = WeatherType.DROUGHT;
        } else if (roll < Constant.DROUGHT_CHANCE + Constant.RAIN_CHANCE) {
            chosen = WeatherType.RAINY;
        } else {
            chosen = WeatherType.SUNNY;
        }

        Weather w = Weather.of(chosen);
        gameManager.setCurrentWeather(w);
        return w;
    }

    public void spawnPest() {
        if (rng.nextInt(100) >= Constant.PEST_SPAWN_CHANCE) {
            return;
        }

        List<Cell> candidates = new ArrayList<>();
        for (Cell[] row : farmMap.getGrid()) {
            for (Cell c : row) {
                if (c.getCrop() != null && c.getPest() == null) {
                    candidates.add(c);
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.get(rng.nextInt(candidates.size())).spawnPest(new Beetle());
    }

    @Override
    public void dayEnded() {
        generateWeather();
        spawnPest();
    }
}
