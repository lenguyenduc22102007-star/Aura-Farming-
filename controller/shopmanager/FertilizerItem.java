package controller.shopmanager;

import exception.InvalidActionException;
import model.game_item.Fertilizer;
import model.game_item.Item;

import java.util.Objects;
import java.util.function.Supplier;

public class FertilizerItem implements ItemManager {
    private final String name;
    private final Supplier<Fertilizer> factory;

    public FertilizerItem(String name, Supplier<Fertilizer> factory) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public String getItem() {
        return name;
    }

    @Override
    public Item create() {
        Fertilizer fertilizer;
        try {
            fertilizer = factory.get();
        } catch(RuntimeException ex) {
            throw new InvalidActionException("Fertilizer factory for '" + name + "' threw while producing an item", ex);
        }
        if (fertilizer == null) {
            throw new InvalidActionException("Fertilizer factory for '" + name + "' produced null");
        }
        return fertilizer;
    }
}
