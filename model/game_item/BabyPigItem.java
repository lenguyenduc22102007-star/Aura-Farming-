package model.game_item;

public class BabyPigItem extends Item {
    public BabyPigItem() {
        super("Baby Pig", 50);
    }

    @Override
    public String getStats() {
        return "Nuôi lớn sẽ có giá trị bán cao (300$)";
    }
}
