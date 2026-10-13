package controller.farmcontroller;

import controller.lifecyclemanager.BaseController;
import controller.lifecyclemanager.DayObserver;
import controller.lifecyclemanager.GameManager;
import model.soil.FarmMap;

public class FarmController extends BaseController implements DayObserver {
    private final FarmMap farmMap;

    public FarmController(GameManager gameManager, FarmMap farmMap) {
        super(gameManager);
        this.farmMap = farmMap;
        gameManager.addObserver(this);
    }

    public void processGrowth() {
        farmMap.updateAllCells(gameManager.getCurrentWeather());
    }

    @Override
    public void dayEnded() {
        processGrowth();
    }
}
