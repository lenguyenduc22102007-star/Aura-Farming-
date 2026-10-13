package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import utils.Constant;

public class StandardGrowthManager implements GrowthManager {
    @Override
    public int calculateGrowthProgress(Cell cell, Weather weather) {
        // for general crop, if severe lack of water and sunlight -> growth rate = 0
        if (cell.getMoistureLevel() < 10 || cell.getSunlightLevel() < 10) {
            return 0;
        }

        int growth = Constant.GROWTH_RATE;

        // Bonus if soil is nutritious
        if (cell.getNutrientLevel() >= 50) {
            growth += 5;
        }

        // Penalty if moisture or sunlight or nutrient level is modest
        if (cell.getMoistureLevel() < 30) growth -= 2;
        if (cell.getSunlightLevel() < 30) growth -= 2;
        if (cell.getNutrientLevel() < 30) growth -= 2;

        return Math.max(0, growth);  // avoid growth rate < 0
    }
}
