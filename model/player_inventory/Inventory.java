package model.player_inventory;

import model.game_item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Inventory {
    private final Map<Item, Integer> items = new LinkedHashMap<>();

    public Map<Item, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void addItem(Item item, int quantity) {
        if(item == null || quantity <= 0) return;

        int current = items.getOrDefault(item, 0);

        try {
            items.put(item, Math.addExact(current, quantity));
        } catch (ArithmeticException overflow) {
            items.put(item, Integer.MAX_VALUE);
        }
    }

    public boolean removeItem(Item item, int quantity) {
        if (item == null || quantity <= 0) return false;

        int current = items.getOrDefault(item, 0);

        if(current < quantity) return false;

        int left = current - quantity;
        if(left == 0) {
            items.remove(item);
        }
        else {
            items.put(item, left);
        }
        return true;
    }

    public int getItemCount(Item item) {
        return items.getOrDefault(item, 0);
    }

    public Item findByName(String name) {
        if(name == null) return null;

        for (Item i : items.keySet()) {
            if (i.getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return null;
    }
}
