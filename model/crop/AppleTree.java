package model.crop;

public class AppleTree extends PerennialFruitTree {
    public AppleTree() {
        super(new CropData(105, 120, 8));
    }

    @Override
    public String getName() {
        return "Apple";
    }
}
