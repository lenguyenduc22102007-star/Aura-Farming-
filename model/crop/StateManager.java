package model.crop;

/**
 * Strategy: decides which lifecycle stage a crop is in
 * given its current growth progress (0..100).
 */
public interface StateManager {
    GrowthState updateState(int currentGrowthProgress);

    /** How many days a crop can sit in HARVEST before rotting. */
    default int getHarvestPatience() {
        return 3;
    }
}
