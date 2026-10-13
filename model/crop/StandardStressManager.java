package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import model.weather.WeatherType;
import utils.Constant;

/**
 * Baseline stress calculation: a crop is stressed by low moisture,
 * low nutrients, low sunlight and by drought weather. Returns the per-day delta.
 */
public class StandardStressManager implements StressManager {
    @Override
    public int calculateStress(Cell cell, Weather weather) {
        int stress = 0;
        // some basic conditions to calculate stress delta
        if (cell.getMoistureLevel() < 25)  stress += 3;
        if (cell.getNutrientLevel() < 20)  stress += 2;
        if(cell.getSunlightLevel() < 15)   stress += 1;
        if (weather != null && weather.getType() == WeatherType.DROUGHT) stress += 5;

        // ideal conditions let the crop recover by STRESS_DECAY (clamped >= 0 in Crop)
        if (cell.getMoistureLevel() > 60 && cell.getNutrientLevel() > 30 && cell.getSunlightLevel() > 30) {
            stress -= Constant.STRESS_DECAY;
        }
        return stress;
    }
}
