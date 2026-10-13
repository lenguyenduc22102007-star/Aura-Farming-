package controller.actionmanager;

import model.soil.Cell;
import utils.Constant;

public class WateringCan implements ToolStrategy {
    @Override
    public boolean executeOnTarget(Cell c) {
        if (c == null || !c.isFarmable()) {
            return false;
        }
        c.updateMoistureAmount(Constant.WATER_PER_USE);
        return true;
    }

    @Override
    public String getName() {
        return "Watering Can";
    }
}
