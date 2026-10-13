package model.crop;

import model.soil.Cell;
import model.weather.Weather;

/**
 * Strategy: how much growth progress (0..100 scale) a crop gains in a single day.
 */
public interface GrowthManager {
    int calculateGrowthProgress(Cell currentCell, Weather currentWeather);
}
