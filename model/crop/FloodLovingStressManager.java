package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import model.weather.WeatherType;

/**
 * Stress strategy for water-loving species (e.g. Water Rice).
 * Loves rain, suffers heavily under drought.
 */
public class FloodLovingStressManager implements StressManager {
    @Override
    public int calculateStress(Cell cell, Weather weather) {
        int stress = 0;
        if (cell.getMoistureLevel() < 40)  stress += 15;
        if (cell.getNutrientLevel() < 20)  stress += 10;
        if (weather != null && weather.getType() == WeatherType.DROUGHT) stress += 20;
        if (weather != null && weather.getType() == WeatherType.RAINY)   stress -= 15;  // loves rain
        if(cell.getMoistureLevel() > 70) stress -= 15;
        return stress;
    }
}
