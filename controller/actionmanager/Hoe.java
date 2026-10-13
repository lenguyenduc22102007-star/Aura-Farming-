package controller.actionmanager;

import model.crop.Crop;
import model.crop.GrowthState;
import model.soil.Cell;
import model.soil.CellState;

public class Hoe implements ToolStrategy {
    @Override
    public boolean executeOnTarget(Cell c) {
        if (c == null) {
            return false;
        }

        Crop crop = c.getCrop();
        if (crop != null && (crop.getCurrentState() == GrowthState.DEAD || crop.getCurrentState() == GrowthState.ROTTEN)) {
            c.clearCrop();
            c.changeState(CellState.UNTILLED);
            return true;
        }

        return c.till();
    }

    @Override
    public String getName() {
        return "Hoe";
    }
}
