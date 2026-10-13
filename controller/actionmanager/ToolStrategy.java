package controller.actionmanager;

import model.soil.Cell;

public interface ToolStrategy {
    boolean executeOnTarget(Cell target);
    String getName();
}
