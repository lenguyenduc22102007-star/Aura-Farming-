package utils;

/**
 * Global game constants used across the Smart Farm simulator.
 * Holds tunable game parameters that drive simulation behavior.
 */
public final class Constant {

    private Constant() {}

    // --- Farm grid ---
    public static final int DEFAULT_FARM_ROWS = 40;   // FarmMap's zone layout is designed for 40x40
    public static final int DEFAULT_FARM_COLS = 40;
    public static final int CELL_SIZE         = 64;

    // --- Player economy ---
    public static final int START_MONEY       = 200;
    public static final int FERTILIZER_PRICE  = 25;

    public static final int GAME_OVER_GRACE_DAYS = 2;

    // --- Simulation tuning ---
    public static final int GROWTH_RATE        = 20;  // baseline progress added per day
    public static final int STRESS_DECAY       = 15;  // stress recovered per day under good conditions
    public static final int MOISTURE_EVAPORATION = 10; // baseline moisture lost per day (all cells)
    public static final int NUTRIENT_REGEN_RATE  = 5; // nutrient recovered per day on empty soil
    public static final int CROP_NUTRIENT_USE = 8;

    // --- Cell resource bounds ---
    public static final int MAX_NUTRIENT      = 100;
    public static final int MAX_MOISTURE      = 100;
    public static final int MAX_SUNLIGHT      = 100;
    public static final int DEFAULT_NUTRIENT  = 50;   // starting + regen target for soil nutrient

    // --- Crop progress range ---
    public static final int MAX_GROWTH        = 100;

    // --- Random event probabilities (0..100) ---
    public static final int PEST_SPAWN_CHANCE = 8;
    public static final int RAIN_CHANCE       = 30;
    public static final int DROUGHT_CHANCE    = 20;
    public static final int SUNNY_CHANCE      = 50;

    // --- Tool costs (water usage) ---
    public static final int WATER_PER_USE     = 30;  // amount of moisture increased when watering
}
