package model.game_item;

public class ChickenFeedItem extends AnimalFeedItem {
    public ChickenFeedItem() {
        super("Chicken Feed", 4);
    }

    @Override
    public String getStats() {
        return "Thóc & Ngô dinh dưỡng cho Gà (và các vật nuôi khác)";
    }
}
