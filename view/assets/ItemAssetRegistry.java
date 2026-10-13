package view.assets;

import model.crop.*;
import model.game_item.*;

import java.util.HashMap;
import java.util.Map;

public final class ItemAssetRegistry {
    private static final Map<Class<?>, String> KEYS = new HashMap<>();

    static {
        KEYS.put(TomatoSeed.class, "tomato");
        KEYS.put(PotatoSeed.class, "potato");
        KEYS.put(CornSeed.class, "corn");
        KEYS.put(SunflowerSeed.class, "sunflower");
        KEYS.put(WaterRiceSeed.class, "water_rice");

        KEYS.put(StandardFertilizer.class, "standard_fertilizer");

        KEYS.put(BabyChickenItem.class, "baby_chicken");
        KEYS.put(BabyPigItem.class, "baby_pig");
        KEYS.put(BabyCowItem.class, "baby_cow");
        KEYS.put(AnimalFeedItem.class, "animal_feed");
    }

    public static String getKey(Item item) {
        if (item == null) {
            return null;
        }

        if (item instanceof HarvestedCropItem) {
            return ((HarvestedCropItem) item).getCropName().toLowerCase().replace(" ", "_");
        }

        return KEYS.get(item.getClass());
    }
}