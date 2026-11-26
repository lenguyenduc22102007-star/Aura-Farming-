package model.game_item;

/**
 * Sản phẩm thu hoạch từ vật nuôi (Trứng gà, Sữa bò, Nấm cục lợn), rơi ra đất và tự hút vào hành trang.
 */
public class AnimalProductItem extends Item {

    public AnimalProductItem(String name, int price) {
        super(name, price);
    }

    public static AnimalProductItem forAnimalType(int animalType) {
        switch (animalType) {
            case 0:  return new AnimalProductItem("Fresh Egg", 25);
            case 1:  return new AnimalProductItem("Fresh Milk", 45);
            case 3:  return new AnimalProductItem("Farm Truffle", 40);
            default: return new AnimalProductItem("Fresh Egg", 25);
        }
    }

    @Override
    public String getStats() {
        return "Sản phẩm vật nuôi | Bán: " + price + "$";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnimalProductItem)) return false;
        AnimalProductItem that = (AnimalProductItem) o;
        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
