package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import model.weather.WeatherType;

/**
 * Stress strategy for drought-tolerant species (e.g. Potato, Sunflower).
 * Ignores drought; only suffers severe water lack and loves sunlight.
 */
public class DroughtResistanceStressManager implements StressManager {
    @Override
    public int calculateStress(Cell cell, Weather weather) {
        int stress = 0;
        if (cell.getMoistureLevel() < 10 || cell.getMoistureLevel() > 80) stress += 10;
        if (cell.getNutrientLevel() < 20) stress += 10;
        if (weather != null && weather.getType() == WeatherType.DROUGHT) stress += 2;  // not fear drought
        if (weather != null && weather.getType() == WeatherType.SUNNY) stress -= 10;  // loves sunlight
        if(cell.getSunlightLevel() > 60)  stress -= 5;
        if (cell.getNutrientLevel() > 50) stress -= 5;
        return stress;
    }
}
