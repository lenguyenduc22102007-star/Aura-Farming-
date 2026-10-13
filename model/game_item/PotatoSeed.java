package model.game_item;

import model.crop.*;

public class PotatoSeed extends Seed {
    public PotatoSeed() {
        super("Potato Seed", 12, 25, 4, 90, 8, 12);
    }

    @Override
    public Crop createCrop() {
        return new Potato();
    }
}
