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
    private boolean zoroPestProtection = false;

    public EnvironmentController(GameManager gameManager, FarmMap farmMap) {
        this(gameManager, farmMap, new Random());
    }

    public void setZoroPestProtection(boolean enabled) {
        this.zoroPestProtection = enabled;
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
        int effectiveChance = zoroPestProtection
                ? Math.max(1, (int) Math.round(Constant.PEST_SPAWN_CHANCE * 0.20)) // Zoro >= 3 Tim: giảm 80% sâu bệnh!
                : Constant.PEST_SPAWN_CHANCE;
        if (rng.nextInt(100) >= effectiveChance) {
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
