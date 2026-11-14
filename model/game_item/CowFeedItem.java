package model.game_item;

public class CowFeedItem extends AnimalFeedItem {
    public CowFeedItem() {
        super("Cow Feed", 6);
    }

    @Override
    public String getStats() {
        return "Cỏ non & Rơm khô cho Bò (và các vật nuôi khác)";
    }
}
