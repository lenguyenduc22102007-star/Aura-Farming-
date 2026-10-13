package view.assets;

import model.crop.*;
import model.game_item.*;

import java.util.HashMap;
import java.util.Map;

public final class CropAssetRegistry {
    private static final Map<Class<?>, String> KEYS = new HashMap<>();

    static {
        KEYS.put(Sunflower.class, "sunflower");
        KEYS.put(Potato.class, "potato");
        KEYS.put(Corn.class, "corn");
        KEYS.put(Tomato.class, "tomato");
        KEYS.put(WaterRice.class, "water_rice");
    }

    public static String getKey(Crop crop) {
        if (crop == null) {
            return null;
        }

        return KEYS.get(crop.getClass());
    }
}