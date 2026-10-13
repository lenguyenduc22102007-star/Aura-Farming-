package model.weather;

import model.soil.Cell;

public interface WeatherEffect {
    void applyEffect(Cell cell);
}
