package model.game_item;

/**
 * Vòi Tưới Tự Động (AutoSprinkler): chế tạo từ 2 Quặng Sắt + 1 Quặng Vàng.
 * Đặt lên ruộng bằng phím [V] (hoặc trong Sổ Quản Lý) — mỗi sáng tự động tưới ướt 8 ô xung quanh!
 */
public class AutoSprinklerItem extends Item {

    public static final String ITEM_NAME = "Vòi Tưới Tự Động";

    public AutoSprinklerItem() {
        super(ITEM_NAME, 350);
    }

    @Override
    public String getStats() {
        return "Bấm [V] trên ruộng để đặt | Mỗi sáng tự tưới ướt 8 ô xung quanh!";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof AutoSprinklerItem);
    }

    @Override
    public int hashCode() {
        return ITEM_NAME.hashCode();
    }
}
