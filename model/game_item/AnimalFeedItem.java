package model.game_item;

public class AnimalFeedItem extends Item {
    public AnimalFeedItem() {
        super("Animal Feed", 5);
    }

    public AnimalFeedItem(String name, int price) {
        super(name, price);
    }

    @Override
    public String getStats() {
        return "Cho thú ăn mỗi ngày để chúng lớn lên";
    }
}
