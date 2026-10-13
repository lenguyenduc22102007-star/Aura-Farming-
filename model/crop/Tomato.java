package model.crop;

public class Tomato extends Crop {
    public Tomato() {
        super(new CropData(60, 80, 10), new StandardStateManager(3), new StandardStressManager(), new SunLovingGrowthManager());
    }

    @Override
    public String getName() {
        return "Tomato";
    }
}
