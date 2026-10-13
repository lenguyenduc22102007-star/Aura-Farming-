package model.crop;

public class Corn extends Crop {
    public Corn() {
        super(new CropData(45, 85, 10), new StandardStateManager( 3), new StandardStressManager(), new StandardGrowthManager());
    }

    @Override
    public String getName() {
        return "Corn";
    }
}
