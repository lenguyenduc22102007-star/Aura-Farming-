package model.crop;

public class Sunflower extends Crop {
    public Sunflower() {
        super(new CropData(35, 70, 9), new StandardStateManager(4), new DroughtResistanceStressManager(), new SunLovingGrowthManager());
    }
    @Override
    public String getName() {
        return "Sunflower";
    }
}
