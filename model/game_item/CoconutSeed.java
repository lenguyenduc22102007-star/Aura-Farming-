package model.game_item;

import model.crop.CoconutTree;
import model.crop.Crop;

public class CoconutSeed extends Seed {
    public CoconutSeed() {
        super("Coconut Seed", 250, 125, 99, 140, 5, 7);
    }

    @Override
    public Crop createCrop() {
        return new CoconutTree();
    }

    @Override
    public String getStats() {
        return "Cây Dừa lâu năm | Trồng 1 lần, ra quả $125 mỗi 3 ngày mãi mãi!";
    }
}
