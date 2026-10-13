package controller.shopmanager;

import exception.InvalidActionException;
import model.game_item.Item;

import java.util.Objects;
import java.util.function.Supplier;

public class AnimalShopItem implements ItemManager {
    private final String name;
    private final Supplier<Item> factory;

    public AnimalShopItem(String name, Supplier<Item> factory) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public String getItem() {
        return name;
    }

    @Override
    public Item create() {
        Item item;
        try {
            item = factory.get();
        } catch(RuntimeException ex) {
            throw new InvalidActionException("Factory for '" + name + "' threw while producing an item", ex);
        }
        if (item == null) {
            throw new InvalidActionException("Factory for '" + name + "' produced null");
        }
        return item;
    }
}
