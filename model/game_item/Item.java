package model.game_item;

import java.util.Objects;

public abstract class Item {
    protected final String name;
    protected final int price;

    protected Item(String name, int price) {
        this.name = Objects.requireNonNull(name, "Item name must not be null");
        if (price < 0) {
            throw new IllegalArgumentException("Item price must be >= 0, was " + price);
        }
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public abstract String getStats();
}
