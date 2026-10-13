package model.game_item;

public class AnimalFeedItem extends Item {
    public AnimalFeedItem() {
        super("Animal Feed", 5);
    }

    @Override
    public String getStats() {
        return "Cho thú ăn mỗi ngày để chúng lớn lên";
    }
}
