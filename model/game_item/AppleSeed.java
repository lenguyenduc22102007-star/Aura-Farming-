package model.game_item;

import model.crop.AppleTree;
import model.crop.Crop;

public class AppleSeed extends Seed {
    public AppleSeed() {
        super("Apple Seed", 210, 105, 99, 120, 5, 8);
    }

    @Override
    public Crop createCrop() {
        return new AppleTree();
    }

    @Override
    public String getStats() {
        return "Cây Táo lâu năm | Trồng 1 lần, ra quả $105 mỗi 3 ngày mãi mãi!";
    }
}
