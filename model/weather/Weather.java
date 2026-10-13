package model.weather;

import model.soil.Cell;
import java.util.Objects;

public class Weather {
    private final WeatherType type;
    private final WeatherEffect effect;

    public Weather(WeatherType type, WeatherEffect effect) {
        this.type = Objects.requireNonNull(type, "weather type must not be null");
        this.effect = Objects.requireNonNull(effect, "weather effect must not be null");
    }

    public WeatherType getType() {
        return type;
    }

    public void applyWeatherEffect(Cell cell) {
        if (cell == null) return;
        effect.applyEffect(cell);
    }

    // return a weather object of a WeatherType
    public static Weather of(WeatherType type) {
        switch (type) {
            case RAINY:
                return new Weather(type, new RainyEffect());
            case DROUGHT:
                return new Weather(type, new DroughtEffect());
            case SUNNY:
            default:
                return new Weather(type, new SunnyEffect());
        }
    }
}
