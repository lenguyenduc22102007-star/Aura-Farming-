package test;

import controller.lifecyclemanager.DayObserver;
import controller.lifecyclemanager.GameManager;
import model.weather.WeatherType;

import java.util.ArrayList;
import java.util.List;

public class GameManagerTest {
    public static void run() {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        if (gm.getCurrentDay() != 1) {
            throw new AssertionError("day should be 1 after reset");
        }
        if (gm.getCurrentWeather() == null) {
            throw new AssertionError("weather should be SUNNY after reset");
        }
        if (gm.getCurrentWeather().getType() != WeatherType.SUNNY) {
            throw new AssertionError("weather should be sunny after reset");
        }

        if (GameManager.getInstance() != GameManager.getInstance()) {
            throw new AssertionError("getInstance must always return the same instance");
        }

        gm.reset();
        List<String> order = new ArrayList<>();
        gm.addObserver(() -> order.add("first"));
        gm.addObserver(() -> order.add("second"));
        gm.advanceDay();
        if (gm.getCurrentDay() != 2) {
            throw new AssertionError("day should advance to 2, was " + gm.getCurrentDay());
        }
        if (!order.equals(List.of("first", "second"))) {
            throw new AssertionError("observers must fire in registration order, was " + order);
        }

        gm.reset();
        int[] calls = {0};
        DayObserver once = () -> calls[0]++;
        gm.addObserver(once);
        gm.addObserver(once);
        gm.advanceDay();
        if (calls[0] != 1) {
            throw new AssertionError("duplicate observer should fire once, fired " + calls[0]);
        }
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: GameManagerTest");
    }
}
