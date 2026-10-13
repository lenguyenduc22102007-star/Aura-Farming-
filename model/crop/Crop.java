package model.crop;

import exception.SmartFarmException;
import model.soil.Cell;
import model.weather.Weather;
import utils.Constant;

import java.util.Objects;

public abstract class Crop {
    private int currentStress;
    private int currentGrowthProgress;
    private GrowthState currentState;

    protected final CropData cropData;
    protected final StateManager  stateManager;
    protected final StressManager stressManager;
    protected final GrowthManager growthManager;

    private int daysAtHarvest = 0;

    public Crop(CropData cropData,
                StateManager  stateManager,
                StressManager stressManager,
                GrowthManager growthManager) {
        this.cropData = Objects.requireNonNull(cropData, "cropData must not be null");
        this.stateManager  = Objects.requireNonNull(stateManager,  "stateManager must not be null");
        this.stressManager = Objects.requireNonNull(stressManager, "stressManager must not be null");
        this.growthManager = Objects.requireNonNull(growthManager, "growthManager must not be null");
        this.currentStress = 0;
        this.currentGrowthProgress = 0;
        this.currentState = GrowthState.SEED;
    }

    // Getters
    public int  getCurrentGrowthProgress() {
        return currentGrowthProgress;

    }

    public GrowthState getCurrentState() {
        return currentState;
    }

    public CropData getCropData() {
        return cropData;
    }

    public boolean isHarvestable() {
        return currentState == GrowthState.HARVEST;
    }

    public boolean isDead() {
        return currentState == GrowthState.DEAD || currentState == GrowthState.ROTTEN;
    }

    //  Daily simulation
    public void dailyUpdate(Cell hostCell, Weather weather) {
        // this cell must not be null - has a crop on it
        Objects.requireNonNull(hostCell, "hostCell must not be null in dailyUpdate");
        if (isDead()) return;

        //  accumulate stress and check death
        int stressDelta = stressManager.calculateStress(hostCell, weather);
        currentStress = safeCheckingValue(currentStress + stressDelta, 0, cropData.getMaxStress());
        if (currentStress >= cropData.getMaxStress()) {
            currentState = GrowthState.DEAD;
            return;
        }

        //  accumulate growth, but only if not already at peak
        if (currentState != GrowthState.HARVEST) {
            int growthDelta = growthManager.calculateGrowthProgress(hostCell, weather);

            // stress slows down growth progress
            int maxStress = cropData.getMaxStress();
            double stressFactor = 1.0 - (currentStress / (double) maxStress);
            growthDelta = (int) Math.round(growthDelta * stressFactor);
            currentGrowthProgress = safeCheckingValue(currentGrowthProgress + growthDelta, 0, Constant.MAX_GROWTH);

            // update growth state
            GrowthState next = stateManager.updateState(currentGrowthProgress);
            if (next == null) {
                throw new SmartFarmException(
                        "StateManager (" + stateManager.getClass().getName()
                                + ") returned null state for growth progress " + currentGrowthProgress);
            }
            currentState = next;
        }

        //  consume soil resources because the crop drank/ate
        hostCell.updateMoistureAmount(-cropData.getWaterPerDay());
        hostCell.updateNutrientAmount(-Constant.CROP_NUTRIENT_USE);

        // detect if it is rotten
        if (currentState == GrowthState.HARVEST) {
            daysAtHarvest++;
            if (daysAtHarvest > stateManager.getHarvestPatience()) {
                currentState = GrowthState.ROTTEN;
            }
        }
    }

    // used to update stress when a cell is attack by pest
    public void addStress(int amount) {
        if (isDead()) return;
        currentStress = safeCheckingValue(currentStress + amount, 0, cropData.getMaxStress());
        if (currentStress >= cropData.getMaxStress()) currentState = GrowthState.DEAD;
    }

    public abstract String getName();

    private int safeCheckingValue(int v, int low, int high){
        if(v > high) return high;
        if(v < low) return low;
        return v;
    }
}
