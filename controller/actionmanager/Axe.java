package controller.actionmanager;

import model.soil.Cell;

/**
 * Công cụ Rìu (Axe) dùng để chặt cây gỗ quanh nông trại thu hoạch Củi khô (Firewood).
 */
public class Axe implements ToolStrategy {
    @Override
    public boolean executeOnTarget(Cell target) {
        // Rìu tương tác trực tiếp với TreeObject trên bản đồ (không cuốc đất)
        return false;
    }

    @Override
    public String getName() {
        return "Axe";
    }
}
