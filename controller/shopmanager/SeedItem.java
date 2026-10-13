package controller.shopmanager;

import exception.InvalidActionException;
import model.game_item.Item;
import model.game_item.Seed;

import java.util.Objects;
import java.util.function.Supplier;

public class SeedItem implements ItemManager {
    private final String name;
    private final Supplier<Seed> factory;

    public SeedItem(String name, Supplier<Seed> factory) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public String getItem() {
        return name;
    }

    @Override
    public Item create() {
        Seed seed;
        try {
            seed = factory.get();
        } catch(RuntimeException ex) {
            throw new InvalidActionException("Seed factory for '" + name + "' threw while producing an item", ex);
        }
        if (seed == null) {
            throw new InvalidActionException("Seed factory for '" + name + "' produced null");
        }
        return seed;
    }
}
