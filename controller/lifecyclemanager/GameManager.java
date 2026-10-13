package controller.lifecyclemanager;

import model.weather.Weather;
import model.weather.WeatherType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameManager {
    private static final class Holder {
        static final GameManager INSTANCE = new GameManager();
    }
    public static GameManager getInstance() {
        return Holder.INSTANCE;
    }
    private GameManager() {
        reset();
    }

    private int currentDay;
    private Weather currentWeather;
    private GameStatus status;
    private final List<DayObserver> observers = new ArrayList<>();

    public void addObserver(DayObserver observer) {
        Objects.requireNonNull(observer, "Observer must not be null");
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public int getCurrentDay() {
        return currentDay;
    }
    public Weather getCurrentWeather() {
        return currentWeather;
    }
    public void setCurrentWeather(Weather currentWeather) {
        this.currentWeather = currentWeather;
    }

    public GameStatus getStatus() {
        return status;
    }
    public void setStatus(GameStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void reset() {
        observers.clear();
        currentDay = 1;
        currentWeather = Weather.of(WeatherType.SUNNY);
        status = GameStatus.RUNNING;
    }

    public void advanceDay() {
        if (status != GameStatus.RUNNING) {
            return;
        }
        List<DayObserver> snapshot = new ArrayList<>(observers);
        for (DayObserver obs : snapshot) {
            obs.dayEnded();
        }
        currentDay++;
    }
}
