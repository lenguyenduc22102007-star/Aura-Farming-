package model.game_item;

public class HarvestedCropItem extends Item {
    private final String cropName;

    public HarvestedCropItem(String cropName, int value) {
        super(cropName + " (Crop)", value);
        this.cropName = cropName;
    }

    public String getCropName() {
        return cropName;
    }

    @Override
    public String getStats() {
        return "Bán: " + price + "$ | Làm thức ăn pet";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HarvestedCropItem)) return false;
        HarvestedCropItem that = (HarvestedCropItem) o;
        return cropName.equals(that.cropName);
    }

    @Override
    public int hashCode() {
        return cropName.hashCode();
    }
}
