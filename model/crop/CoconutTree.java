package model.crop;

public class CoconutTree extends PerennialFruitTree {
    public CoconutTree() {
        super(new CropData(125, 140, 7));
    }

    @Override
    public String getName() {
        return "Coconut";
    }
}
