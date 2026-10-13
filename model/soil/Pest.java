package model.soil;

import model.crop.Crop;

public interface Pest {
    void attackTargetCrop(Crop crop);
}
