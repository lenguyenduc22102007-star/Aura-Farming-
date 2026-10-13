package model.game_item;

import model.crop.Crop;

public abstract class Seed extends Item {
    protected final int yieldValue;
    protected final int daysAvailableAfterRipe;
    protected final int maxStress;
    protected final int growDays;
    protected final int waterPerDay;

    public Seed(String name, int price, int yieldValue, int daysAvailableAfterRipe, int maxStress, int growDays, int waterPerDay) {
        super(name, price);
        if (yieldValue < 0 || daysAvailableAfterRipe < 0 || maxStress < 0 || growDays < 0 || waterPerDay < 0) {
            throw new IllegalArgumentException("Seed numerical attributes must be >= 0");
        }
        this.yieldValue = yieldValue;
        this.daysAvailableAfterRipe = daysAvailableAfterRipe;
        this.maxStress = maxStress;
        this.growDays =growDays;
        this.waterPerDay = waterPerDay;
    }

    public int getYieldValue() {
        return yieldValue;
    }

    public int getGrowDays() {
        return growDays;
    }

    public int getWaterPerDay() {
        return waterPerDay;
    }

    @Override
    public String getStats(){
        return String.format("GROWS: %d DAYS  WATER: %d/D  SELL: $%d", growDays, waterPerDay, yieldValue);
    }

    public abstract Crop createCrop();
}