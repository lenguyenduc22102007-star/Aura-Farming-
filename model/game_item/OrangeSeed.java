package model.game_item;

import model.crop.Crop;
import model.crop.OrangeTree;

public class OrangeSeed extends Seed {
    public OrangeSeed() {
        super("Orange Seed", 180, 90, 99, 120, 5, 8);
    }

    @Override
    public Crop createCrop() {
        return new OrangeTree();
    }

    @Override
    public String getStats() {
        return "Cây Cam lâu năm | Trồng 1 lần, ra quả $90 mỗi 3 ngày mãi mãi!";
    }
}
