package model.player_inventory;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import utils.Constant;

public class Player {
    private final IntegerProperty money = new SimpleIntegerProperty(Constant.START_MONEY);
    private final Inventory inventory = new Inventory();

    public int getMoney() {
        return money.get();
    }

    /** Observable balance for JavaFX bindings (e.g. money label text). */
    public IntegerProperty moneyProperty() {
        return money;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void addMoney(int amount) {
        if (amount <= 0) return;

        try {
            money.set(Math.addExact(money.get(), amount));
        } catch (ArithmeticException overflow) {
            money.set(Integer.MAX_VALUE);
        }
    }

    public boolean deductMoney(int amount) {
        if (amount <= 0 || money.get() < amount) return false;
        money.set(money.get() - amount);
        return true;
    }
}