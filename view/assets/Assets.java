package view.assets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.crop.Crop;
import model.crop.GrowthState;
import model.game_item.Item;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Assets {
    // TOOL ICONS
    public static Image HOE_ICON;
    public static Image WATER_ICON;
    public static Image SEED_ICON;
    public static Image HAND_ICON;
    public static Image FERTILIZER_ICON;

    // WEATHER ICONS
    public static Image SUN_ICON;
    public static Image RAIN_ICON;
    public static Image DRY_ICON;

    // PEST
    public static Image BEETLE_ICON;

    // CROP STATE
    public static Image SEED_STATE;
    public static Image SEEDLING_STATE;
    public static Image MATURE_STATE;
    public static Image DEAD_STATE;
    public static Image ROTTEN_STATE;

    // SHOP ITEM ICONS
    private static final Map<String, Image> SHOP_ICONS = new HashMap<>();

    // OBJECTS (BẢN ĐỒ VÀ CÔNG TRÌNH)
    public static Image GRASS_TILE;
    public static Image PATH_TILE;
    public static Image DIRT_TILE;
    public static Image WATER_TILE;
    public static Image HOUSE_TILE;
    public static Image FACTORY_TILE;
    public static Image TOWER_TILE;
    public static Image TREE_TILE;
    public static Image CAMP_TILE;
    public static Image PLAYER_IMAGE;

    public static Image SCENERY_BANGHE;
    public static Image SCENERY_BUICAY1;
    public static Image SCENERY_BUICAY2;
    public static Image SCENERY_DANHO;
    public static Image SCENERY_DATO;
    public static Image SCENERY_GOCCAY;
    public static Image SCENERY_LUATRAI;
    public static Image SCENERY_LUATRAI2;
    public static Image SCENERY_RAONGHIENG;
    public static Image SCENERY_RAOTHANG;
    public static Image SCENERY_THANCAY;

    public static Image FLAG_TILE;
    public static Image CRAFT_STATION_TILE;

    // ANIMAL SPRITES
    public static Image PIG_SPRITE;
    public static Image COW_SPRITE;
    public static Image CHICKEN_SPRITE;

    // LOAD ALL ASSETS
    public static void load() {
        System.out.println("Loading assets...");

        // TOOLS
        HOE_ICON = safeLoadImage("/view/assets/tools/hoe.png");
        WATER_ICON = safeLoadImage("/view/assets/tools/watering_can.png");
        SEED_ICON = safeLoadImage("/view/assets/tools/seed.png");
        HAND_ICON = safeLoadImage("/view/assets/tools/hand.png");
        FERTILIZER_ICON = safeLoadImage("/view/assets/tools/fertilizer.png");

        // WEATHER
        SUN_ICON = safeLoadImage("/view/assets/weather/sun.png");
        RAIN_ICON = safeLoadImage("/view/assets/weather/rain.png");
        DRY_ICON = safeLoadImage("/view/assets/weather/dry.png");

        // SHOP
        loadShopIcon("corn");
        loadShopIcon("standard_fertilizer");
        loadShopIcon("potato");
        loadShopIcon("sunflower");
        loadShopIcon("tomato");
        loadShopIcon("water_rice");
        loadShopIcon("baby_chicken");
        loadShopIcon("baby_pig");
        loadShopIcon("baby_cow");
        loadShopIcon("animal_feed");

        // PEST
        BEETLE_ICON = safeLoadImage("/view/assets/pest/beetle.png");

        // SEED STATE
        SEED_STATE      = safeLoadImage("/view/assets/crop_state/seed.png");
        SEEDLING_STATE  = safeLoadImage("/view/assets/crop_state/seedling.png");
        MATURE_STATE    = safeLoadImage("/view/assets/crop_state/mature.png");
        DEAD_STATE      = safeLoadImage("/view/assets/crop_state/dead.png");
        ROTTEN_STATE    = safeLoadImage("/view/assets/crop_state/rotten.png");

        // TILES & BUILDINGS
        GRASS_TILE   = safeLoadImage("/view/assets/grass_tile.png");
        PATH_TILE    = safeLoadImage("/view/assets/path_tile.png");
        DIRT_TILE    = safeLoadImage("/view/assets/dirt_tile.png");
        HOUSE_TILE   = safeLoadImage("/view/assets/house_tile.png");
        FACTORY_TILE = safeLoadImage("/view/assets/factory_tile.png");
        TOWER_TILE   = safeLoadImage("/view/assets/tower_tile.png");
        TREE_TILE    = safeLoadImage("/view/assets/tree_tile.png");
        CAMP_TILE    = safeLoadImage("/view/assets/camp_tile.png");
        PLAYER_IMAGE = safeLoadImage("/view/assets/player.jpg");

        // NEW SCENERY OBJECTS
        SCENERY_BANGHE   = safeLoadImage("/view/assets/banghe_tile.png");
        SCENERY_BUICAY1  = safeLoadImage("/view/assets/buicay1_tile.png");
        SCENERY_BUICAY2  = safeLoadImage("/view/assets/buicay2_tile.png");
        SCENERY_DANHO    = safeLoadImage("/view/assets/danho_tile.png");
        SCENERY_DATO     = safeLoadImage("/view/assets/dato_tile.png");
        SCENERY_GOCCAY   = safeLoadImage("/view/assets/goccay_tile.png");
        SCENERY_LUATRAI  = safeLoadImage("/view/assets/luatrai_tile.png");
        SCENERY_LUATRAI2 = safeLoadImage("/view/assets/luatrai2_tile.png");
        SCENERY_RAONGHIENG = safeLoadImage("/view/assets/raonghieng_tile.png");
        SCENERY_RAOTHANG   = safeLoadImage("/view/assets/raothang_tile.png");
        SCENERY_THANCAY    = safeLoadImage("/view/assets/thancay_tile.png");
        FLAG_TILE          = safeLoadImage("/view/assets/cothaydoi_tile.png");
        CRAFT_STATION_TILE = safeLoadImage("/view/assets/khoangdat_tile.png");

        // ANIMAL SPRITES
        PIG_SPRITE         = safeLoadImage("/view/assets/animal_pig.png");
        COW_SPRITE         = safeLoadImage("/view/assets/animal_cow.png");
        CHICKEN_SPRITE     = safeLoadImage("/view/assets/animal_chicken.png");

        System.out.println("Assets loaded successfully.");
    }

    // HÀM LOAD ẢNH AN TOÀN (Sẽ báo chính xác file nào bị thiếu)
    private static Image safeLoadImage(String path) {
        InputStream stream = Assets.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("LỖI MẤT FILE: Không tìm thấy ảnh tại -> " + path);
            return null; // Trả về null để game không văng, giúp bạn xem được Terminal
        }
        return new Image(stream);
    }

    private static void loadShopIcon(String name) {
        SHOP_ICONS.put(name.toLowerCase(), safeLoadImage("/view/assets/shop/" + name + ".png"));
    }

    public static Image getShopIcon(String key) {
        return SHOP_ICONS.get(key);
    }
    
    public static Image getShopIcon(Item item) {
        if (item instanceof model.game_item.AnimalFeedItem) {
            return SEED_ICON != null ? SEED_ICON : FERTILIZER_ICON;
        }
        String key = ItemAssetRegistry.getKey(item);
        Image icon = getShopIcon(key);
        if (icon == null && item instanceof model.game_item.Seed) {
            return SEED_ICON;
        }
        return icon;
    }

    // LOAD IMAGE (Hàm cũ)
    private static Image loadImage(String path) {
        return new Image(Assets.class.getResourceAsStream(path));
    }

    public static Image getCropSprite(Crop crop) {
        if (crop == null) {
            return null;
        }

        String cropKey = CropAssetRegistry.getKey(crop);
        GrowthState state = crop.getCurrentState();

        switch (state) {
            case DEAD:
                return DEAD_STATE;
            case ROTTEN:
                return ROTTEN_STATE;
            case SEED:
                return SEED_STATE;
            case SEEDLING:
                return SEEDLING_STATE;
            case MATURE:
                return MATURE_STATE;
            case HARVEST:
                return getShopIcon(cropKey);
            default:
                return null;
        }
    }

    // CREATE IMAGE VIEW
    public static ImageView imageView(Image image, double size) {
        // Nếu ảnh null (bị lỗi thiếu file), hệ thống không tạo View để tránh lỗi
        if (image == null) {
            return new ImageView();
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        return view;
    }
}