package model.weather;

import model.soil.Cell;

/** Rainy day: + moisture, less sunlight. */
public class RainyEffect implements WeatherEffect {
    @Override
    public void applyEffect(Cell cell) {
        cell.updateMoistureAmount(15);
        cell.updateSunlightAmount(-15);
    }
}
