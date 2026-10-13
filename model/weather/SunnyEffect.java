package model.weather;

import model.soil.Cell;

/** Sunny day: + sunlight, slight moisture loss. */
public class SunnyEffect implements WeatherEffect {
    @Override
    public void applyEffect(Cell cell){
        cell.updateSunlightAmount(15);
        cell.updateMoistureAmount(-10);
    }
}