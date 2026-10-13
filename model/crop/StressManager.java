package model.crop;

import model.soil.Cell;
import model.weather.Weather;

/**
 * Strategy: how much stress a crop accumulates each day given the
 * cell's conditions and the current weather. Different species can
 * cope with different conditions, so they swap in different strategies.
 */
public interface StressManager {
    int calculateStress(Cell currentCell, Weather currentWeather);
}
