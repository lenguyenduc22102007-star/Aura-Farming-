package model.game_item;

public class BabyChickenItem extends Item {
    public BabyChickenItem() {
        super("Baby Chicken", 20);
    }

    @Override
    public String getStats() {
        return "Nuôi lớn sẽ có giá trị bán cao (150$)";
    }
}
