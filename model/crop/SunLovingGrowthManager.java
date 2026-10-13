package model.crop;

import model.soil.Cell;
import model.weather.Weather;
import model.weather.WeatherType;
import utils.Constant;

public class SunLovingGrowthManager implements GrowthManager {
    @Override
    public int calculateGrowthProgress(Cell cell, Weather weather) {
        int growth = Constant.GROWTH_RATE;

        if (weather != null) {
            if (weather.getType() == WeatherType.RAINY) {
                growth -= 10;
            }
            else if (weather.getType() == WeatherType.SUNNY) {
                growth += 8;
            }
        }

        // loves sunlight
        if (cell.getSunlightLevel() >= 50) {
            growth += 5;
        }
        else if (cell.getSunlightLevel() < 20) {
            growth -= 3;
        }

        // this type only needs a little of water
        if (cell.getMoistureLevel() >= 60) {
            growth -= 5;
        }
        else if (cell.getMoistureLevel() >= 25) {
            growth += 2;
        }

        return Math.max(0, growth);
    }
}