package model.game_item;

public class PigFeedItem extends AnimalFeedItem {
    public PigFeedItem() {
        super("Pig Feed", 5);
    }

    @Override
    public String getStats() {
        return "Cám dinh dưỡng cho Lợn (và các vật nuôi khác)";
    }
}
