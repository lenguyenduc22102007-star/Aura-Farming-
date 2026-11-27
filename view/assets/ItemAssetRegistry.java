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
        KEYS.put(OrangeSeed.class, "orange");
        KEYS.put(AppleSeed.class, "apple");
        KEYS.put(CoconutSeed.class, "coconut");

        KEYS.put(StandardFertilizer.class, "standard_fertilizer");
        KEYS.put(AutoSprinklerItem.class, "auto_sprinkler");

        KEYS.put(BabyChickenItem.class, "baby_chicken");
        KEYS.put(BabyPigItem.class, "baby_pig");
        KEYS.put(BabyCowItem.class, "baby_cow");
        KEYS.put(AnimalFeedItem.class, "animal_feed");
        KEYS.put(ChickenFeedItem.class, "corn");
        KEYS.put(CowFeedItem.class, "water_rice");
        KEYS.put(PigFeedItem.class, "potato");
        KEYS.put(FishItem.class, "river_fish");
    }

    public static String getKey(Item item) {
        if (item == null) {
            return null;
        }

        if (item instanceof HarvestedCropItem) {
            return ((HarvestedCropItem) item).getCropName().toLowerCase().replace(" ", "_");
        }
        if (item instanceof AnimalProductItem) {
            return item.getName().toLowerCase().replace(" ", "_");
        }
        if (item instanceof CookedFoodItem) {
            return ((CookedFoodItem) item).getRecipeId();
        }
        if (item instanceof ForageItem) {
            return ((ForageItem) item).getForageId();
        }
        if (item instanceof FactoryProductItem) {
            return ((FactoryProductItem) item).getProductId();
        }
        if (item instanceof OreItem) {
            return ((OreItem) item).getOreId();
        }

        return KEYS.get(item.getClass());
    }
}