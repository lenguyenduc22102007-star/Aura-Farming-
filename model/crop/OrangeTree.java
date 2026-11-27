package model.crop;

public class OrangeTree extends PerennialFruitTree {
    public OrangeTree() {
        super(new CropData(90, 120, 8));
    }

    @Override
    public String getName() {
        return "Orange";
    }
}
