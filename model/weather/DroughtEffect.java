package model.weather;

import model.soil.Cell;

/** Drought: heavy moisture loss, intense sun. */
public class DroughtEffect implements WeatherEffect {
    @Override
    public void applyEffect(Cell cell) {
        cell.updateMoistureAmount(-20);
        cell.updateSunlightAmount(25);
    }
}
