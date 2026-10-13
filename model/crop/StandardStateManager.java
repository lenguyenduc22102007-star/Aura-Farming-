package model.crop;

import utils.Constant;

/**
 * Default StateManager: linearly subdivides the 0..100 growth bar.
 *   0..29   → SEED
 *   30..59  → SEEDLING
 *   60..99  → MATURE
 *   100     → HARVEST
 */
public class StandardStateManager implements StateManager {
    private final int daysAvailableAfterRipe; // how long HARVEST stays valid

    public StandardStateManager(int daysAvailableAfterRipe) {
        this.daysAvailableAfterRipe = daysAvailableAfterRipe;
    }

    @Override
    public GrowthState updateState(int currentGrowthProgress) {
        if (currentGrowthProgress < 30) return GrowthState.SEED;
        if (currentGrowthProgress < 60) return GrowthState.SEEDLING;
        if (currentGrowthProgress < Constant.MAX_GROWTH) return GrowthState.MATURE;
        return GrowthState.HARVEST;
    }

    @Override
    public int getHarvestPatience(){
        return daysAvailableAfterRipe;
    }
}
