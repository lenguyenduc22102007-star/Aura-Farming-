package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import model.weather.WeatherType;
import utils.Constant;

public class FloodLovingGrowthManager implements GrowthManager {
    @Override
    public int calculateGrowthProgress(Cell cell, Weather weather) {
        // this type really need water to grow
        if (cell.getMoistureLevel() < 20 || (weather != null && weather.getType() == WeatherType.DROUGHT)) {
            return 0;
        }

        int growth = Constant.GROWTH_RATE;
        // loves water
        if (cell.getMoistureLevel() >= 60) {
            growth += 10;
        }
        if (weather != null && weather.getType() == WeatherType.RAINY) {
            growth += 5;
        }

        // bonus and penalty
        if (cell.getNutrientLevel() >= 30) {
            growth += 2;
        }
        else growth -= 5;

        return Math.max(0, growth);
    }
}