package model.game_item;

public class BabyCowItem extends Item {
    public BabyCowItem() {
        super("Baby Cow", 100);
    }

    @Override
    public String getStats() {
        return "Nuôi lớn sẽ có giá trị bán cao (500$)";
    }
}
