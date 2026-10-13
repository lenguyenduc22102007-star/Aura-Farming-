package model.game_item;

import model.crop.*;

public class SunflowerSeed extends Seed {
    public SunflowerSeed() {
        super("Sunflower Seed", 20, 35, 3, 70, 7, 9);
    }

    @Override
    public Crop createCrop() {
        return new Sunflower();
    }
}
