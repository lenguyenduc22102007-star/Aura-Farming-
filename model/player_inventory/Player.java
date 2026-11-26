package model.player_inventory;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import utils.Constant;

public class Player {
    private final IntegerProperty money = new SimpleIntegerProperty(Constant.START_MONEY);
    private final Inventory inventory = new Inventory();
    private final model.quest.QuestManager questManager = new model.quest.QuestManager();
    private double hunger = 100.0; // 0..100 (100 = no đầy)
    private double thirst = 100.0; // 0..100 (100 = đủ nước)

    public model.quest.QuestManager getQuestManager() {
        return questManager;
    }

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

    public double getHunger() {
        return hunger;
    }

    public void setHunger(double hunger) {
        this.hunger = clampStat(hunger);
    }

    public void addHunger(double amount) {
        this.hunger = clampStat(this.hunger + amount);
    }

    public double getThirst() {
        return thirst;
    }

    public void setThirst(double thirst) {
        this.thirst = clampStat(thirst);
    }

    public void addThirst(double amount) {
        this.thirst = clampStat(this.thirst + amount);
    }

    public boolean isExhausted() {
        return hunger <= 0.0 || thirst <= 0.0;
    }

    private static double clampStat(double v) {
        if (Double.isNaN(v)) return 100.0;
        return Math.max(0.0, Math.min(100.0, v));
    }
}