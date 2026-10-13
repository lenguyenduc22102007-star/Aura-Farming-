package test;

import controller.environmentcontroller.EnvironmentController;
import controller.lifecyclemanager.GameManager;
import model.soil.FarmMap;
import model.weather.Weather;

public class EnvironmentControllerTest {
    public static void run() {
        GameManager gm = GameManager.getInstance();
        gm.reset();
        EnvironmentController env = new EnvironmentController(gm, new FarmMap());
        Weather w = env.generateWeather();
        if (w == null) {
            throw new AssertionError("generateWeather should return a weather");
        }
        if (w.getType() == null) {
            throw new AssertionError("the weather type should be set");
        }
        if (w != gm.getCurrentWeather()) {
            throw new AssertionError("generated weather should be the GameManager's current weather");
        }

        gm.reset();
        EnvironmentController env2 = new EnvironmentController(gm, new FarmMap());
        env2.dayEnded();
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASSED: EnvironmentControllerTest");
    }
}
