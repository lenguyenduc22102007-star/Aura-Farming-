package model.game_item;

import model.soil.Cell;

import java.util.Objects;

public abstract class Fertilizer extends Item {
    protected final int nutrientAmount;

    public Fertilizer(String name, int price, int nutrientAmount){
        super(name, price);

        if (nutrientAmount < 0) {
            throw new IllegalArgumentException("Fertilizer nutrient amount must be >= 0, was " + nutrientAmount);
        }

        this.nutrientAmount = nutrientAmount;
    }

    public int getNutrientAmount() {
        return nutrientAmount;
    }

    @Override
    public String getStats() {
        return "BOOST: +" + nutrientAmount + " NUTRIENT  USE: 1/CELL";
    }

    public void addNutrientAmount(Cell cell) {
        Objects.requireNonNull(cell, "Cell must not be null");
        cell.updateNutrientAmount(nutrientAmount);
    }
}
