package view.assets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
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
    public static Image WATER_HOTBAR_ICON; // chỉ dùng cho ô bình tưới trên thanh công cụ
    public static Image SEED_ICON;
    public static Image HAND_ICON;
    public static Image FERTILIZER_ICON;
    public static Image BAG_ICON;

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

    // 4-STAGE PIXEL CROP SPRITES (48x48, smooth = false: 0=Mầm, 1=Cây con, 2=Cây lớn/Nụ, 3=Chín 100%)
    public static Image[] TOMATO_STAGES;
    public static Image[] SUNFLOWER_STAGES;
    public static Image[] POTATO_STAGES;
    public static Image[] CORN_STAGES;
    public static Image[] RICE_STAGES;
    public static Image[] ORANGE_STAGES;
    public static Image[] APPLE_STAGES;
    public static Image[] COCONUT_STAGES;

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
    public static Image[] FLAG_FRAMES;
    public static Image[] CAMPFIRE_FRAMES;
    public static Image CAMPFIRE_EXTINGUISHED;

    // FARM PROPS (Load sắc nét smooth=false từ /view/assets/props/)
    public static Image FARM_GATE;
    public static Image FARM_GATE_SIDE;
    public static Image FARM_GATE_SIDE_BACK;
    public static Image FARM_GATE_SIDE_FRONT;
    public static Image FARM_PLOT;
    public static Image FENCE_WOOD;
    public static Image LANTERN_POST;
    public static Image LANTERN_POST_LEFT;
    public static Image LANTERN_POST_DOWN;
    public static Image GARDEN_ROCK;
    public static Image WILDFLOWERS;
    public static Image MAIN_HOUSE;
    public static Image POOL_WATER;
    public static Image POOL_CHAIR;
    public static Image POOL_UMBRELLA;
    public static Image PALM_TREE;
    public static Image PROP_CHICKEN_COOP;
    public static Image PROP_STRAW_NEST;
    public static Image PROP_MUD_PUDDLE;
    public static Image PROP_WOOD_TROUGH;
    public static Image PROP_HAY_BALE;
    public static Image PROP_WATER_TROUGH;
    public static Image SIGN_CHICKEN;
    public static Image SIGN_PIG;
    public static Image SIGN_COW;

    // ANIMAL SPRITES
    public static Image PIG_SPRITE;
    public static Image COW_SPRITE;
    public static Image CHICKEN_SPRITE;

    // RIVER & FISHING ASSETS
    public static Image BOAT_IMG;
    public static Image FISH_ICON;
    public static Image EGG_ICON;
    public static Image MILK_ICON;
    public static Image TRUFFLE_ICON;

    // COOKED FOOD ICONS (Món ăn nấu tại đống lửa trại)
    public static Image FOOD_EGG_ICON;
    public static Image FOOD_PORK_ICON;
    public static Image FOOD_STEAK_ICON;
    public static Image FOOD_HOTPOT_ICON;
    public static Image FOOD_FRIED_FISH_ICON;

    // FORAGING ICONS (Vật phẩm tự nhiên quanh khu trại: Củi khô, Nấm dại, Thảo mộc)
    public static Image FIREWOOD_ICON;
    public static Image WILD_MUSHROOM_ICON;
    public static Image HERB_ICON;
    public static Image AXE_ICON;
    public static Image STUMP_IMAGE;

    // ICON BÌNH TƯỚI NHẮC NHỞ MÀU XANH DƯƠNG SẮC NÉT (24x24 nổi trên ô cây thiếu nước)
    public static Image WATER_NEED_ICON;

    // 3 TRẠNG THÁI MÀU ĐẤT RÕ RỆT: Chưa cuốc (vàng cát phẳng) -> Đã cuốc khô (nâu rãnh cày) -> Đã cuốc ướt (nâu đậm ẩm)
    public static Image UNPLOWED_BED_TILE;
    public static Image TILLED_DRY_TILE;
    public static Image TILLED_WET_TILE;

    // 6 ẢNH NPC BỂ BƠI & BOT ĐI DẠO (108x108 chuẩn tỉ lệ Luffy 94px, Chopper 64px, smooth = false)
    public static Image NPC_NAMI;
    public static Image NPC_HANCOCK;
    public static Image NPC_ZORO;
    public static Image NPC_SANJI;
    public static Image NPC_CHOPPER;
    public static Image NPC_USOPP;

    public static Image[] NPC_NAMI_IDLE, NPC_NAMI_WALK;
    public static Image[] NPC_HANCOCK_IDLE, NPC_HANCOCK_WALK;
    public static Image[] NPC_ZORO_IDLE, NPC_ZORO_WALK;
    public static Image[] NPC_SANJI_IDLE, NPC_SANJI_WALK;
    public static Image[] NPC_CHOPPER_IDLE, NPC_CHOPPER_WALK;
    public static Image[] NPC_USOPP_IDLE, NPC_USOPP_WALK;

    // PLAYER ACTION SPRITE FRAMES (72x72 side/action + 108x108 8-direction run/idle, smooth = false)
    public static Image LUFFY_SUNBATHE;
    public static Image[] LUFFY_HOE_FRAMES;
    public static Image[] LUFFY_HOE_RUN_FRAMES;
    public static Image[] LUFFY_AXE_FRAMES;
    public static Image[] LUFFY_WATER_FRAMES;
    public static Image[] LUFFY_WATER_RUN_FRAMES;
    public static Image[] LUFFY_PICK_LOW;
    public static Image[] LUFFY_PICK_HIGH;
    public static Map<String, Image[]> LUFFY_HOE_RUN_BY_DIR;
    public static Map<String, Image> LUFFY_HOE_IDLE_BY_DIR;
    public static Map<String, Image[]> LUFFY_WATER_RUN_BY_DIR;
    public static Map<String, Image> LUFFY_WATER_IDLE_BY_DIR;

    // LOAD ALL ASSETS
    public static void load() {
        System.out.println("Loading assets...");

        // TOOLS (Load sắc nét pixel-art với smooth=false, phóng nguyên 64x64 không bị nhòe)
        HOE_ICON = safeLoadPixelImage("/view/assets/tools/hoe.png", 64, 64);
        WATER_ICON = safeLoadPixelImage("/view/assets/tools/watering_can.png", 64, 64);
        WATER_HOTBAR_ICON = safeLoadPixelImage("/view/assets/tools/watering_can_hotbar.png", 64, 64);
        SEED_ICON = safeLoadPixelImage("/view/assets/tools/seed.png", 64, 64);
        HAND_ICON = safeLoadPixelImage("/view/assets/tools/hand.png", 64, 64);
        FERTILIZER_ICON = safeLoadPixelImage("/view/assets/tools/fertilizer.png", 64, 64);
        BAG_ICON = safeLoadPixelImage("/view/assets/tools/bag.png", 64, 64);

        // WEATHER
        SUN_ICON = safeLoadPixelImage("/view/assets/weather/sun.png", 64, 64);
        RAIN_ICON = safeLoadPixelImage("/view/assets/weather/rain.png", 64, 64);
        DRY_ICON = safeLoadPixelImage("/view/assets/weather/dry.png", 64, 64);

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

        // 4-STAGE CROP SPRITES (smooth = false, phóng sắc nét từ 48x48)
        TOMATO_STAGES    = loadCropStages("tomato");
        SUNFLOWER_STAGES = loadCropStages("sunflower");
        POTATO_STAGES    = loadCropStages("potato");
        CORN_STAGES      = loadCropStages("corn");
        RICE_STAGES      = loadCropStages("rice");

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

        // RIVER & FISHING (Ưu tiên boat.png nếu có, kế tiếp river/boat_full.png, cuối cùng fallback pixel art)
        BOAT_IMG = loadBoatImageWithFallback();
        FISH_ICON = createPixelFishIcon();
        EGG_ICON = createPixelEggIcon();
        MILK_ICON = createPixelMilkIcon();
        TRUFFLE_ICON = createPixelTruffleIcon();
        SHOP_ICONS.put("river_fish", FISH_ICON);
        SHOP_ICONS.put("fresh_egg", EGG_ICON);
        SHOP_ICONS.put("fresh_milk", MILK_ICON);
        SHOP_ICONS.put("farm_truffle", TRUFFLE_ICON);

        FOOD_EGG_ICON = createPixelFriedEggIcon();
        FOOD_PORK_ICON = createPixelGrilledPorkIcon();
        FOOD_STEAK_ICON = createPixelMilkSteakIcon();
        FOOD_HOTPOT_ICON = createPixelHotpotIcon();
        FOOD_FRIED_FISH_ICON = safeLoadPixelImage("/view/assets/shop/fried_fish.png", 64, 64);
        WATER_NEED_ICON = createPixelWaterNeedIcon();
        UNPLOWED_BED_TILE = createUnplowedBedTile();
        TILLED_DRY_TILE = createTilledSoilTile(false);
        TILLED_WET_TILE = createTilledSoilTile(true);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_EGG_FRY, FOOD_EGG_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_PORK_GRILL, FOOD_PORK_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_MILK_STEAK, FOOD_STEAK_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_FARM_HOTPOT, FOOD_HOTPOT_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_FRIED_FISH_COMMON, FOOD_FRIED_FISH_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_FRIED_FISH_RARE, FOOD_FRIED_FISH_ICON);
        SHOP_ICONS.put(model.game_item.CookedFoodItem.ID_FRIED_FISH_EPIC, FOOD_FRIED_FISH_ICON);

        FIREWOOD_ICON = createPixelFirewoodIcon();
        WILD_MUSHROOM_ICON = createPixelWildMushroomIcon();
        HERB_ICON = createPixelHerbIcon();
        AXE_ICON = createPixelAxeIcon();
        STUMP_IMAGE = safeLoadImage("/view/assets/tiled/objects/stump.png");
        if (STUMP_IMAGE == null) {
            STUMP_IMAGE = safeLoadImage("/view/assets/tiled/objects/goccay_tile.png");
        }
        SHOP_ICONS.put(model.game_item.ForageItem.ID_FIREWOOD, FIREWOOD_ICON);
        SHOP_ICONS.put(model.game_item.ForageItem.ID_WILD_MUSHROOM, WILD_MUSHROOM_ICON);
        SHOP_ICONS.put(model.game_item.ForageItem.ID_HERB, HERB_ICON);

        // FACTORY PRODUCTS, ORES, AUTO SPRINKLER & PERENNIAL ORCHARD FRUITS
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_TOMATO_JUICE,   createBottleIcon(Color.rgb(228, 52, 46), Color.rgb(255, 126, 112)));
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_POTATO_SNACK,   createSnackBagIcon(Color.rgb(242, 184, 48), Color.rgb(214, 58, 42)));
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_BUTTER_POPCORN, createPopcornBoxIcon());
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_CANNED_CHEESE,  createCheeseWheelIcon());
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_PACKED_RICE,    createRiceSackIcon());
        SHOP_ICONS.put(model.game_item.FactoryProductItem.ID_SUNFLOWER_OIL,  createBottleIcon(Color.rgb(246, 198, 44), Color.rgb(255, 238, 132)));

        SHOP_ICONS.put(model.game_item.OreItem.ID_COPPER_ORE, createOreIcon(Color.rgb(214, 114, 56), Color.rgb(252, 172, 114)));
        SHOP_ICONS.put(model.game_item.OreItem.ID_IRON_ORE,   createOreIcon(Color.rgb(156, 170, 188), Color.rgb(226, 236, 248)));
        SHOP_ICONS.put(model.game_item.OreItem.ID_GOLD_ORE,   createOreIcon(Color.rgb(245, 192, 42), Color.rgb(255, 242, 138)));
        SHOP_ICONS.put(model.game_item.OreItem.ID_GEMSTONE,   createGemIcon());
        SHOP_ICONS.put("auto_sprinkler", createSprinklerIcon());

        Image orangeIcon  = createRoundFruitIcon(Color.rgb(248, 136, 28), Color.rgb(255, 194, 88));
        Image appleIcon   = createRoundFruitIcon(Color.rgb(224, 44, 44),  Color.rgb(255, 132, 132));
        Image coconutIcon = createRoundFruitIcon(Color.rgb(132, 82, 42),  Color.rgb(186, 132, 78));
        SHOP_ICONS.put("orange",  orangeIcon);
        SHOP_ICONS.put("apple",   appleIcon);
        SHOP_ICONS.put("coconut", coconutIcon);

        APPLE_STAGES = new Image[4];
        for (int i = 0; i < 4; i++) {
            APPLE_STAGES[i] = safeLoadPixelImage("/view/assets/props/apple_tree_" + i + ".png", 128, 128);
        }
        Image orangeHarvestImg = safeLoadPixelImage("/view/assets/props/orange_tree_3.png", 128, 128);
        ORANGE_STAGES = new Image[] {
            APPLE_STAGES[0], APPLE_STAGES[1], APPLE_STAGES[2],
            (orangeHarvestImg != null ? orangeHarvestImg : APPLE_STAGES[3])
        };
        Image coconutPalm = safeLoadPixelImage("/view/assets/props/palm_tree.png", 96, 120);
        COCONUT_STAGES = new Image[] {
            APPLE_STAGES[0], APPLE_STAGES[1], APPLE_STAGES[2],
            (coconutPalm != null ? coconutPalm : APPLE_STAGES[3])
        };

        // NPC POOL & ROAMING BOTS (108x108 chuẩn tỉ lệ Luffy + đủ frame Idle cử động thở & Walk 4 frame vung tay sải chân, smooth = false)
        NPC_NAMI    = safeLoadPixelImage("/view/assets/npcs/nami.png", 108, 108);
        NPC_HANCOCK = safeLoadPixelImage("/view/assets/npcs/hancock.png", 108, 108);
        NPC_ZORO    = safeLoadPixelImage("/view/assets/npcs/zoro.png", 108, 108);
        NPC_SANJI   = safeLoadPixelImage("/view/assets/npcs/sanji.png", 108, 108);
        NPC_CHOPPER = safeLoadPixelImage("/view/assets/npcs/chopper.png", 108, 108);
        NPC_USOPP   = safeLoadPixelImage("/view/assets/npcs/usopp.png", 108, 108);

        NPC_NAMI_IDLE    = loadNpcFrames("nami", "idle", 2, NPC_NAMI);
        NPC_NAMI_WALK    = loadNpcFrames("nami", "walk", 4, NPC_NAMI);
        NPC_HANCOCK_IDLE = loadNpcFrames("hancock", "idle", 2, NPC_HANCOCK);
        NPC_HANCOCK_WALK = loadNpcFrames("hancock", "walk", 4, NPC_HANCOCK);
        NPC_ZORO_IDLE    = loadNpcFrames("zoro", "idle", 2, NPC_ZORO);
        NPC_ZORO_WALK    = loadNpcFrames("zoro", "walk", 4, NPC_ZORO);
        NPC_SANJI_IDLE   = loadNpcFrames("sanji", "idle", 2, NPC_SANJI);
        NPC_SANJI_WALK   = loadNpcFrames("sanji", "walk", 4, NPC_SANJI);
        NPC_CHOPPER_IDLE = loadNpcFrames("chopper", "idle", 2, NPC_CHOPPER);
        NPC_CHOPPER_WALK = loadNpcFrames("chopper", "walk", 4, NPC_CHOPPER);
        NPC_USOPP_IDLE   = loadNpcFrames("usopp", "idle", 2, NPC_USOPP);
        NPC_USOPP_WALK   = loadNpcFrames("usopp", "walk", 4, NPC_USOPP);

        // PLAYER ACTION & RUN FRAMES (Cuốc đứng/bổ 4 frame, chạy cầm cuốc 4 frame, tưới 3 frame, chạy cầm bình 4 frame; smooth = false)
        LUFFY_SUNBATHE = safeLoadPixelImage("/view/assets/player/luffy_sunbathe.png", 49, 111);
        LUFFY_HOE_FRAMES = new Image[4];
        LUFFY_HOE_RUN_FRAMES = new Image[4];
        LUFFY_AXE_FRAMES = new Image[4];
        for (int i = 0; i < 4; i++) {
            LUFFY_HOE_FRAMES[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_hoe_" + i + ".png", 144, 144);
            LUFFY_HOE_RUN_FRAMES[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_hoe_run_" + i + ".png", 144, 144);
            LUFFY_AXE_FRAMES[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_axe_" + i + ".png", 144, 144);
        }
        LUFFY_WATER_FRAMES = new Image[3];
        for (int i = 0; i < 3; i++) {
            LUFFY_WATER_FRAMES[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_water_" + i + ".png", 144, 144);
        }
        LUFFY_WATER_RUN_FRAMES = new Image[4];
        for (int i = 0; i < 4; i++) {
            LUFFY_WATER_RUN_FRAMES[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_water_run_" + i + ".png", 144, 144);
        }
        LUFFY_PICK_LOW = new Image[4];
        LUFFY_PICK_HIGH = new Image[4];
        for (int i = 0; i < 4; i++) {
            LUFFY_PICK_LOW[i]  = safeLoadPixelImage("/view/assets/player/luffy_pick_low_" + i + ".png", 0, 0);
            LUFFY_PICK_HIGH[i] = safeLoadPixelImage("/view/assets/player/luffy_pick_high_" + i + ".png", 0, 0);
        }

        // ANIMATED MAP PROPS (Cờ bay theo gió 6 frame 48x64 & Lửa trại cháy bập bùng 6 frame 48x48, smooth=false)
        FLAG_FRAMES = new Image[6];
        CAMPFIRE_FRAMES = new Image[6];
        for (int i = 0; i < 6; i++) {
            FLAG_FRAMES[i] = safeLoadPixelImage("/view/assets/props/flag_" + i + ".png", 48, 64);
            CAMPFIRE_FRAMES[i] = safeLoadPixelImage("/view/assets/props/campfire_" + i + ".png", 48, 48);
        }
        CAMPFIRE_EXTINGUISHED = safeLoadPixelImage("/view/assets/props/campfire_extinguished.png", 96, 96);

        FARM_GATE            = safeLoadPixelImage("/view/assets/props/farm_gate.png",            144, 118);
        FARM_GATE_SIDE       = safeLoadPixelImage("/view/assets/props/farm_gate_side.png",        80, 300);
        FARM_GATE_SIDE_BACK  = safeLoadPixelImage("/view/assets/props/farm_gate_side_back.png",   80, 300);
        FARM_GATE_SIDE_FRONT = safeLoadPixelImage("/view/assets/props/farm_gate_side_front.png",  80, 300);
        FARM_PLOT    = safeLoadPixelImage("/view/assets/props/farm_plot.png",     96,  96);
        FENCE_WOOD   = safeLoadPixelImage("/view/assets/props/fence_wood.png",    48,  48);
        LANTERN_POST      = safeLoadPixelImage("/view/assets/props/lantern_post.png",      48,  72);
        LANTERN_POST_LEFT = safeLoadPixelImage("/view/assets/props/lantern_post_left.png", 48,  72);
        LANTERN_POST_DOWN = safeLoadPixelImage("/view/assets/props/lantern_post_down.png", 48,  72);
        GARDEN_ROCK   = safeLoadPixelImage("/view/assets/props/garden_rock.png",   48,  48);
        WILDFLOWERS   = safeLoadPixelImage("/view/assets/props/wildflowers.png",   48,  48);
        MAIN_HOUSE    = safeLoadPixelImage("/view/assets/props/main_house.png",   224, 225);
        POOL_WATER    = safeLoadPixelImage("/view/assets/props/pool_water.png",   168, 238);
        POOL_CHAIR    = safeLoadPixelImage("/view/assets/props/pool_chair.png",    52,  72);
        POOL_UMBRELLA = safeLoadPixelImage("/view/assets/props/pool_umbrella.png", 72,  72);
        PALM_TREE     = safeLoadPixelImage("/view/assets/props/palm_tree.png",     96, 120);
        PROP_CHICKEN_COOP = safeLoadPixelImage("/view/assets/props/prop_chicken_coop.png", 0, 0);
        PROP_STRAW_NEST   = safeLoadPixelImage("/view/assets/props/prop_straw_nest.png",   0, 0);
        PROP_MUD_PUDDLE   = safeLoadPixelImage("/view/assets/props/prop_mud_puddle.png",   0, 0);
        PROP_WOOD_TROUGH  = safeLoadPixelImage("/view/assets/props/prop_wood_trough.png",  0, 0);
        PROP_HAY_BALE     = safeLoadPixelImage("/view/assets/props/prop_hay_bale.png",     0, 0);
        PROP_WATER_TROUGH = safeLoadPixelImage("/view/assets/props/prop_water_trough.png", 0, 0);
        SIGN_CHICKEN      = safeLoadPixelImage("/view/assets/props/sign_chicken.png",      0, 0);
        SIGN_PIG          = safeLoadPixelImage("/view/assets/props/sign_pig.png",          0, 0);
        SIGN_COW          = safeLoadPixelImage("/view/assets/props/sign_cow.png",          0, 0);

        // 8-DIRECTION TOOL RUN & IDLE MAPS (side dùng sheet 72x72 gốc; down/up/down_right/up_right dùng sprite 108x108)
        LUFFY_HOE_RUN_BY_DIR = new HashMap<>();
        LUFFY_HOE_IDLE_BY_DIR = new HashMap<>();
        LUFFY_WATER_RUN_BY_DIR = new HashMap<>();
        LUFFY_WATER_IDLE_BY_DIR = new HashMap<>();

        LUFFY_HOE_RUN_BY_DIR.put("side", LUFFY_HOE_RUN_FRAMES);
        LUFFY_HOE_IDLE_BY_DIR.put("side", LUFFY_HOE_FRAMES[0]);
        LUFFY_WATER_RUN_BY_DIR.put("side", LUFFY_WATER_RUN_FRAMES);
        LUFFY_WATER_IDLE_BY_DIR.put("side", LUFFY_WATER_FRAMES[0]);

        String[] toolDirs = {"down", "up", "down_right", "up_right"};
        for (String dir : toolDirs) {
            Image[] hoeDirRun = new Image[4];
            Image[] waterDirRun = new Image[4];
            for (int i = 0; i < 4; i++) {
                hoeDirRun[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_hoe_run_" + dir + "_" + i + ".png", 108, 108);
                waterDirRun[i] = safeLoadPixelImage("/view/assets/player_actions/luffy_water_run_" + dir + "_" + i + ".png", 108, 108);
            }
            LUFFY_HOE_RUN_BY_DIR.put(dir, hoeDirRun);
            LUFFY_HOE_IDLE_BY_DIR.put(dir, safeLoadPixelImage("/view/assets/player_actions/luffy_hoe_idle_" + dir + ".png", 108, 108));
            LUFFY_WATER_RUN_BY_DIR.put(dir, waterDirRun);
            LUFFY_WATER_IDLE_BY_DIR.put(dir, safeLoadPixelImage("/view/assets/player_actions/luffy_water_idle_" + dir + ".png", 108, 108));
        }

        System.out.println("Assets loaded successfully.");
    }

    /**
     * Load ảnh thuyền từ /view/assets/boat.png -> /view/assets/river/boat_full.png.
     * Nếu không tìm thấy file thì tự vẽ ảnh pixel-art thuyền gỗ dự phòng để không bao giờ gây NullPointerException.
     */
    private static Image loadBoatImageWithFallback() {
        String[] candidates = {
            "/view/assets/boat.png",
            "/view/assets/river/boat.png",
            "/view/assets/river/boat_full.png"
        };
        for (String path : candidates) {
            InputStream stream = Assets.class.getResourceAsStream(path);
            if (stream != null) {
                return new Image(stream);
            }
        }
        return createFallbackPixelBoat();
    }

    private static Image createFallbackPixelBoat() {
        int w = 96, h = 84;
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        Color outline = Color.rgb(46, 26, 18);
        Color woodHi  = Color.rgb(214, 158, 96);
        Color woodMd  = Color.rgb(164, 102, 54);
        Color woodLo  = Color.rgb(112, 64, 34);
        Color sailW   = Color.rgb(245, 248, 255);
        Color sailSh  = Color.rgb(196, 210, 236);

        // Cột buồm (x=36..41, y=8..54)
        for (int y = 8; y <= 54; y++) {
            for (int x = 36; x <= 41; x++) {
                pw.setColor(x, y, (x == 36 || x == 41 || y == 8) ? outline : (x <= 38 ? woodHi : woodMd));
            }
        }
        // Cánh buồm trắng hơi căng gió (x=42..66, y=14..46)
        for (int y = 14; y <= 46; y++) {
            double t = (y - 14.0) / 32.0;
            int bulge = (int) Math.round(Math.sin(t * Math.PI) * 18);
            int rightX = 46 + bulge;
            int leftX = 42 + (int) Math.round(Math.sin(t * Math.PI) * 4);
            for (int x = leftX; x <= rightX; x++) {
                boolean border = (y == 14 || y == 46 || x == leftX || x == rightX);
                pw.setColor(x, y, border ? outline : (y > 35 ? sailSh : sailW));
            }
        }
        // Thân thuyền gỗ (y=52..78)
        for (int y = 52; y <= 78; y++) {
            int insetL = (y > 68) ? (y - 68) : 0;
            int insetR = (y > 62) ? (y - 62) : 0;
            int x1 = 10 + insetL;
            int x2 = 86 - insetR;
            for (int x = x1; x <= x2; x++) {
                boolean border = (y == 52 || y == 78 || x == x1 || x == x2);
                Color c = border ? outline : (y <= 55 ? woodHi : (((y - 52) % 6 == 0) ? woodLo : woodMd));
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    /** Vẽ icon cá sông pixel-art sắc nét cho hành trang & hiệu ứng cá nhảy khi câu trúng. */
    private static Image createPixelFishIcon() {
        String[] art = {
            "................",
            "......oooo......",
            "....ooBBBBoo....",
            "...oBBLLLLBBo...",
            ".ooBBLWKBLLBBo..",
            "oBBBBBKKLLLLBBo.",
            "oBBBBBBBBLLLBo..",
            ".ooBBBBBBLLBo...",
            "...oBBLLLLBo....",
            "....ooBBBBoo....",
            "......oooo......",
            "................"
        };
        int scale = 3;
        int h = art.length;
        int w = art[0].length();
        WritableImage img = new WritableImage(w * scale, h * scale);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char ch = art[y].charAt(x);
                Color c = null;
                if (ch == 'o') c = Color.rgb(24, 42, 62);
                else if (ch == 'B') c = Color.rgb(66, 154, 214);
                else if (ch == 'L') c = Color.rgb(162, 224, 250);
                else if (ch == 'W') c = Color.rgb(248, 252, 255);
                else if (ch == 'K') c = Color.rgb(20, 26, 36);
                if (c != null) {
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            pw.setColor(x * scale + dx, y * scale + dy, c);
                        }
                    }
                }
            }
        }
        return img;
    }

    private static Image createPixelEggIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "...oWWWWo...",
            "..oWWLLWWo..",
            "..oWLLLLWo..",
            ".oWWLLLLWWo.",
            ".oWWLLLLSSo.",
            ".oWWWLLSSSo.",
            "..oWWSSSSo..",
            "...ooSSoo...",
            ".....oo....."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(66, 46, 32);
            if (ch == 'W') return Color.rgb(255, 250, 238);
            if (ch == 'L') return Color.rgb(246, 232, 206);
            if (ch == 'S') return Color.rgb(218, 194, 158);
            return null;
        });
    }

    private static Image createPixelMilkIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "....oRRo....",
            "....oooo....",
            "...oWWWWo...",
            "..oWWLLWWo..",
            "..oWBBBBWo..",
            "..oWBBBBWo..",
            "..oWWLLWWo..",
            "..oWWSSSSo..",
            "...oooooo..."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(42, 48, 66);
            if (ch == 'R') return Color.rgb(220, 74, 68);
            if (ch == 'W') return Color.rgb(248, 252, 255);
            if (ch == 'L') return Color.rgb(214, 234, 248);
            if (ch == 'B') return Color.rgb(92, 168, 224);
            if (ch == 'S') return Color.rgb(176, 206, 228);
            return null;
        });
    }

    private static Image createPixelTruffleIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "..ooBBBBoo..",
            ".oBBLLBBBBo.",
            ".oBLLBBBBSo.",
            "oBBBBBSSSSBo",
            "oBBBBSSSSBBo",
            ".oBBBSSSSBo.",
            "..ooSSSSoo..",
            "....oooo...."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(36, 24, 18);
            if (ch == 'B') return Color.rgb(112, 74, 48);
            if (ch == 'L') return Color.rgb(164, 118, 80);
            if (ch == 'S') return Color.rgb(74, 46, 30);
            return null;
        });
    }

    private static Image createPixelFriedEggIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "..ooWWWWoo..",
            ".oWWYYYYWWo.",
            "oWWYYLLYYWWo",
            "oWWYYYYYYWWo",
            "oWWWYYYYWWSo",
            ".oWWWWWWWSo.",
            "..ooSSSSoo..",
            "....oooo...."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(54, 34, 20);
            if (ch == 'W') return Color.rgb(255, 252, 242);
            if (ch == 'S') return Color.rgb(220, 208, 186);
            if (ch == 'Y') return Color.rgb(255, 192, 34);
            if (ch == 'L') return Color.rgb(255, 236, 134);
            return null;
        });
    }

    private static Image createPixelGrilledPorkIcon() {
        return renderPixelPattern(new String[] {
            "...oooooo...",
            "..oBBBBBBoo.",
            ".oBMMKMMBMBo",
            "oBMMKMMKMMBo",
            "oBMMMMMMMMBo",
            "oBMMKMMKMMSo",
            ".oBBMMMMSSo.",
            "..ooSSSSoo..",
            "....oooo...."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(42, 22, 14);
            if (ch == 'B') return Color.rgb(156, 76, 36);
            if (ch == 'M') return Color.rgb(214, 114, 62);
            if (ch == 'K') return Color.rgb(78, 34, 18);
            if (ch == 'S') return Color.rgb(116, 52, 26);
            return null;
        });
    }

    private static Image createPixelMilkSteakIcon() {
        return renderPixelPattern(new String[] {
            ".ooo....ooo.",
            "oWWWo..oBBBo",
            "oWLLo.oBMMBo",
            "oWLLo.oBMKBo",
            "oWWWo.oBMMBo",
            "oSSSo..oSSo.",
            ".ooo....oo..",
            "oooooooooooo",
            ".oooooooooo."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(44, 28, 22);
            if (ch == 'W') return Color.rgb(250, 252, 255);
            if (ch == 'L') return Color.rgb(208, 230, 246);
            if (ch == 'B') return Color.rgb(122, 54, 30);
            if (ch == 'M') return Color.rgb(186, 86, 48);
            if (ch == 'K') return Color.rgb(232, 152, 92);
            if (ch == 'S') return Color.rgb(154, 162, 174);
            return null;
        });
    }

    private static Image createPixelHotpotIcon() {
        return renderPixelPattern(new String[] {
            "...w...w....",
            "....w...w...",
            "..oooooooo..",
            ".oRRGYYGRRo.",
            "ooRRRRRRRRoo",
            "oPPDDDDDDPPo",
            ".oPDDDDDDPo.",
            "..oDDDDDDo..",
            "...oooooo..."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(34, 24, 22);
            if (ch == 'w') return Color.rgb(245, 242, 234);
            if (ch == 'R') return Color.rgb(232, 82, 42);
            if (ch == 'Y') return Color.rgb(255, 206, 62);
            if (ch == 'G') return Color.rgb(104, 198, 72);
            if (ch == 'P') return Color.rgb(92, 86, 98);
            if (ch == 'D') return Color.rgb(62, 56, 68);
            return null;
        });
    }

    /** Icon Củi khô pixel-art (bó củi buộc dây thừng vàng, sắc nét 36x36). */
    private static Image createPixelFirewoodIcon() {
        return renderPixelPattern(new String[] {
            "............",
            "...oooo.....",
            "..oLLBBo....",
            ".oBMBBBBo...",
            ".oBBYMBBBoo.",
            "ooBBYMMBBLLo",
            "oLLBYMBBMBBo",
            "oBMMYBBBBBBo",
            ".oBBYMMBBBo.",
            "..ooYooooo..",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(42, 22, 12);
            if (ch == 'B') return Color.rgb(122, 70, 34);
            if (ch == 'M') return Color.rgb(162, 98, 48);
            if (ch == 'L') return Color.rgb(218, 174, 114);
            if (ch == 'Y') return Color.rgb(236, 188, 72);
            return null;
        });
    }

    /** Icon Nấm dại pixel-art (mũ đỏ chấm kem trắng, cuống trắng ngà). */
    private static Image createPixelWildMushroomIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "..ooRRRRoo..",
            ".oRWRRRWRRo.",
            "oRRRRWRRRRRo",
            "oRRWRRRRWRRo",
            "ooRRRRRRRRoo",
            "..oooooooo..",
            "...oCCCCo...",
            "...oCWCCo...",
            "...oCCCCo...",
            "..ooCCCCoo..",
            "..oooooooo.."
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(46, 20, 16);
            if (ch == 'R') return Color.rgb(216, 56, 44);
            if (ch == 'W') return Color.rgb(255, 248, 228);
            if (ch == 'C') return Color.rgb(236, 218, 182);
            return null;
        });
    }

    /** Icon Thảo mộc pixel-art (nhánh lá xanh tươi điểm hoa vàng nhỏ). */
    private static Image createPixelHerbIcon() {
        return renderPixelPattern(new String[] {
            "......oo....",
            "....ooGYo...",
            "...oGGLLo...",
            "..oGLGGo.oo.",
            ".ooGGGo.oGLo",
            "oGLLGo.oGGLo",
            ".ooGGo.oGGo.",
            "...oGooGGo..",
            "....oGGoo...",
            "....oSGo....",
            "...oSSo.....",
            "...ooo......"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(20, 54, 24);
            if (ch == 'G') return Color.rgb(54, 164, 62);
            if (ch == 'L') return Color.rgb(138, 226, 98);
            if (ch == 'S') return Color.rgb(74, 128, 48);
            if (ch == 'Y') return Color.rgb(255, 222, 86);
            return null;
        });
    }

    /** Icon Rìu chặt cây pixel-art sắc nét (lưỡi thép sáng + cán gỗ nâu ấm, 48x48). */
    private static Image createPixelAxeIcon() {
        return renderPixelPattern(new String[] {
            "......oooo......",
            ".....oSSLLo.....",
            "....oSSLLLLo....",
            "...oSSDDLLLo....",
            "...oSSDBBLLo....",
            "....ooDBBSo.....",
            "......oBBo......",
            ".....oBMBoo.....",
            "....oBMBo.......",
            "...oBMBo........",
            "..oBMBo.........",
            ".oBMBo..........",
            ".oBBo...........",
            "..oo............",
            "................",
            "................"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(34, 24, 18);
            if (ch == 'L') return Color.rgb(232, 242, 250);
            if (ch == 'S') return Color.rgb(162, 178, 194);
            if (ch == 'D') return Color.rgb(98, 112, 128);
            if (ch == 'B') return Color.rgb(138, 82, 40);
            if (ch == 'M') return Color.rgb(186, 118, 62);
            return null;
        });
    }

    private static Image createBottleIcon(Color main, Color hi) {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "....oCCo....",
            "....oooo....",
            "...oMMMMo...",
            "..oMHHMMMo..",
            "..oMHMMMMo..",
            "..oMMMMMMo..",
            "..oWWWWWWo..",
            "..oMMMMMMo..",
            "..oMMMMMMo..",
            "...oooooo...",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(32, 22, 18);
            if (ch == 'C') return Color.rgb(214, 168, 96);
            if (ch == 'M') return main;
            if (ch == 'H') return hi;
            if (ch == 'W') return Color.rgb(252, 248, 236);
            return null;
        });
    }

    private static Image createSnackBagIcon(Color bagCol, Color stripeCol) {
        return renderPixelPattern(new String[] {
            ".oooooooooo.",
            ".oSSSSSSSSo.",
            "..oBBBBBBBo.",
            "..oBBWWBBBo.",
            "..oBWWWWBBo.",
            "..oBBWWBBBo.",
            "..oSSSSSSSSo",
            "..oBBBBBBBo.",
            ".oSSSSSSSSo.",
            ".oooooooooo.",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(34, 22, 16);
            if (ch == 'B') return bagCol;
            if (ch == 'S') return stripeCol;
            if (ch == 'W') return Color.rgb(255, 248, 212);
            return null;
        });
    }

    private static Image createPopcornBoxIcon() {
        return renderPixelPattern(new String[] {
            "..oo.oo.oo..",
            ".oYYoYYoYYo.",
            "oYYYYYYYYYYo",
            ".oooooooooo.",
            ".oRWRWRWRWo.",
            ".oRWRWRWRWo.",
            "..oRWRWRWo..",
            "..oRWRWRWo..",
            "..oRWRWRWo..",
            "...oooooo...",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(38, 20, 14);
            if (ch == 'Y') return Color.rgb(255, 224, 92);
            if (ch == 'R') return Color.rgb(220, 48, 42);
            if (ch == 'W') return Color.rgb(252, 248, 240);
            return null;
        });
    }

    private static Image createCheeseWheelIcon() {
        return renderPixelPattern(new String[] {
            "............",
            "...oooooo...",
            ".ooLLLLLLoo.",
            "oLLLLLLLLLLo",
            "oooooooooooo",
            "oYYDYYYYDYYo",
            "oYYYYYYYYYYo",
            "oYYYYDYYYYYo",
            ".oooooooooo.",
            "............",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(44, 28, 12);
            if (ch == 'L') return Color.rgb(255, 236, 124);
            if (ch == 'Y') return Color.rgb(245, 192, 42);
            if (ch == 'D') return Color.rgb(198, 136, 22);
            return null;
        });
    }

    private static Image createRiceSackIcon() {
        return renderPixelPattern(new String[] {
            "...oooooo...",
            "..oLLLLLLo..",
            "...oRRRRo...",
            "..oSSSSSSo..",
            ".oSSGGGGSSo.",
            ".oSSGGGGSSo.",
            ".oSSSSSSSSo.",
            ".oSSSSSSSSo.",
            "..oooooooo..",
            "............",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(42, 28, 16);
            if (ch == 'L') return Color.rgb(236, 214, 172);
            if (ch == 'S') return Color.rgb(212, 184, 136);
            if (ch == 'R') return Color.rgb(206, 48, 42);
            if (ch == 'G') return Color.rgb(86, 172, 68);
            return null;
        });
    }

    private static Image createOreIcon(Color ore, Color shine) {
        return renderPixelPattern(new String[] {
            "............",
            "...oooooo...",
            "..oRRRRRRo..",
            ".oRROSHRRRo.",
            "oRRROOORRRRo",
            "oRROSHRRRRRo",
            "oRRRRROORRRo",
            ".oRRRRRRRRo.",
            "..oooooooo..",
            "............",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(28, 30, 36);
            if (ch == 'R') return Color.rgb(104, 112, 124);
            if (ch == 'O') return ore;
            if (ch == 'S' || ch == 'H') return shine;
            return null;
        });
    }

    private static Image createGemIcon() {
        return renderPixelPattern(new String[] {
            "............",
            "...oooooo...",
            "..oCCCCCCo..",
            ".oCWWCCCCCo.",
            "oCCWCCCCCCCo",
            ".oCCCCCCCCo.",
            "..oCCCCCCo..",
            "...oCCCCo...",
            "....oooo....",
            "............",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(18, 42, 68);
            if (ch == 'C') return Color.rgb(56, 198, 252);
            if (ch == 'W') return Color.rgb(224, 250, 255);
            return null;
        });
    }

    private static Image createSprinklerIcon() {
        return renderPixelPattern(new String[] {
            "............",
            ".oooooooooo.",
            "oBBBBWWBBBBo",
            ".ooooYYoooo.",
            "....oYYo....",
            "....oYYo....",
            "....oYYo....",
            "..ooSSSSoo..",
            ".oSSSSSSSSo.",
            ".oooooooooo.",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(24, 32, 42);
            if (ch == 'B') return Color.rgb(56, 189, 248);
            if (ch == 'W') return Color.rgb(224, 242, 254);
            if (ch == 'Y') return Color.rgb(250, 204, 21);
            if (ch == 'S') return Color.rgb(100, 116, 139);
            return null;
        });
    }

    private static Image createRoundFruitIcon(Color body, Color hi) {
        return renderPixelPattern(new String[] {
            ".....oo.....",
            "....oGGo....",
            "...oooooo...",
            "..oBBHHBBo..",
            ".oBBHBBBBBo.",
            ".oBBBBBBBBo.",
            ".oBBBBBBBBo.",
            "..oBBBBBBo..",
            "...oooooo...",
            "............",
            "............",
            "............"
        }, 3, ch -> {
            if (ch == 'o') return Color.rgb(38, 22, 14);
            if (ch == 'G') return Color.rgb(74, 182, 62);
            if (ch == 'B') return body;
            if (ch == 'H') return hi;
            return null;
        });
    }

    /** 4 giai đoạn cây ăn quả lâu năm (Mầm -> Cây non -> Cây tán lá lớn -> Cây sai trĩu quả). */
    private static Image[] createFruitTreeStages(Color fruitColor) {
        Image s0 = renderPixelPattern(new String[] {
            "................",
            "................",
            "................",
            ".......oo.......",
            "......oGGo......",
            ".....oGGLLo.....",
            "......oTTo......",
            "......oTTo......",
            ".....ooTToo.....",
            "................"
        }, 4, ch -> {
            if (ch == 'o') return Color.rgb(24, 42, 20);
            if (ch == 'G') return Color.rgb(62, 168, 56);
            if (ch == 'L') return Color.rgb(132, 224, 92);
            if (ch == 'T') return Color.rgb(132, 82, 40);
            return null;
        });
        Image s1 = renderPixelPattern(new String[] {
            "................",
            ".....oooooo.....",
            "....oGGLLGGo....",
            "...oGLLLLGGGo...",
            "....oGGGGGGo....",
            "......oTTo......",
            "......oTTo......",
            "......oTTo......",
            ".....ooTToo.....",
            "................"
        }, 4, ch -> {
            if (ch == 'o') return Color.rgb(24, 42, 20);
            if (ch == 'G') return Color.rgb(54, 158, 52);
            if (ch == 'L') return Color.rgb(122, 214, 84);
            if (ch == 'T') return Color.rgb(132, 82, 40);
            return null;
        });
        Image s2 = renderPixelPattern(new String[] {
            "....oooooooo....",
            "..ooGGLLLLGGoo..",
            ".oGGLLLLLLLLGGo.",
            ".oGLLLLGGLLLLGo.",
            "..oGGGGGGGGGGoo.",
            "....ooTTTTTo....",
            "......oTTTo.....",
            "......oTTTo.....",
            ".....ooTTToo....",
            "................"
        }, 4, ch -> {
            if (ch == 'o') return Color.rgb(22, 40, 18);
            if (ch == 'G') return Color.rgb(46, 148, 48);
            if (ch == 'L') return Color.rgb(106, 204, 76);
            if (ch == 'T') return Color.rgb(128, 76, 36);
            return null;
        });
        Image s3 = renderPixelPattern(new String[] {
            "....oooooooo....",
            "..ooGFFLLLGGoo..",
            ".oGGLFFLLLFFGGo.",
            ".oGLLGGGGFFLLGo.",
            "..oGGFFGGGGGGoo.",
            "....ooTTTTTo....",
            "......oTTTo.....",
            "......oTTTo.....",
            ".....ooTTToo....",
            "................"
        }, 4, ch -> {
            if (ch == 'o') return Color.rgb(22, 40, 18);
            if (ch == 'G') return Color.rgb(46, 148, 48);
            if (ch == 'L') return Color.rgb(106, 204, 76);
            if (ch == 'T') return Color.rgb(128, 76, 36);
            if (ch == 'F') return fruitColor;
            return null;
        });
        return new Image[] { s0, s1, s2, s3 };
    }

    /** Icon bình tưới nước pixel màu xanh dương nổi bật (24x24) để nhắc người chơi tưới cây. */
    private static Image createPixelWaterNeedIcon() {
        return renderPixelPattern(new String[] {
            "....oooo....",
            "...oBBBB....",
            "..oB...oo.oo",
            ".oBoooooooLo",
            ".oBBLBBBBBo.",
            ".oBBLLBBBo..",
            ".oBBLBBBBBo.",
            ".oBBBBBBBDo.",
            "..oooooooo..",
            "...........W",
            "..........WW",
            "...........W"
        }, 2, ch -> {
            if (ch == 'o') return Color.rgb(18, 48, 86);
            if (ch == 'B') return Color.rgb(46, 172, 255);
            if (ch == 'L') return Color.rgb(168, 232, 255);
            if (ch == 'D') return Color.rgb(24, 118, 206);
            if (ch == 'W') return Color.rgb(122, 224, 255);
            return null;
        });
    }

    /** Ô đất luống chưa cuốc (UNTILLED): đất tơi ấm tự nhiên, có hạt đất & độ sâu nhẹ, khớp khung luống viền đất nhô. */
    private static Image createUnplowedBedTile() {
        return renderPixelPattern(new String[] {
            "dLLLLdLLLLdLLLLd",
            "LLsLLLLLLdLLLLsL",
            "LLLLLdLLLLLLsLLL",
            "LdLLLLLLsLLLLdLL",
            "LLLLsLLLLLLdLLLL",
            "LLdLLLLdLLLLLLsL",
            "LLLLLLsLLLLdLLLL",
            "dLLsLLLLLLsLLLdL",
            "LLLLLLdLLLLLLLLL",
            "LLdLLLLLLsLLdLLL",
            "LLLLsLLLLLLLLsLL",
            "LsLLLLdLLLLdLLLL",
            "LLLLLLLLsLLLLLLd",
            "LLdLLsLLLLLLsLLL",
            "LLLLLLLLdLLLLdLL",
            "dLLsLLLLLLdLLLLd"
        }, 4, ch -> {
            if (ch == 'L') return Color.rgb(168, 118, 72);
            if (ch == 's') return Color.rgb(186, 136, 86);
            if (ch == 'd') return Color.rgb(138, 90, 50);
            return null;
        });
    }

    /**
     * Ô đất ĐÃ CUỐC (TILLED) tơi xốp giàu dinh dưỡng, rãnh cày tự nhiên:
     * - wet=false (Khô): đất mùn nâu sẫm ấm, hạt đất tơi nổi rõ.
     * - wet=true  (Ướt): đất ẩm đen nâu sâu (~30% tối hơn), bóng ẩm rõ rệt.
     */
    private static Image createTilledSoilTile(boolean wet) {
        Color ridge  = wet ? Color.rgb(86, 48, 22)  : Color.rgb(126, 74, 36);
        Color mid    = wet ? Color.rgb(70, 38, 16)  : Color.rgb(106, 60, 28);
        Color furrow = wet ? Color.rgb(48, 24, 10)  : Color.rgb(78, 42, 18);
        Color speck  = wet ? Color.rgb(104, 62, 32) : Color.rgb(148, 92, 48);

        return renderPixelPattern(new String[] {
            "MRRRsRRRRMRRsRRM",
            "MMMMMMMMMMMMMMMM",
            "MFFFFFFFFFFFFFFM",
            "RRsRRRRRRRRRsRRR",
            "MMMMMMsMMMMMMMMM",
            "FFFFFFFFFFFFFFFF",
            "RRRRRsRRRRRsRRRR",
            "MMMMMMMMMMsMMMMM",
            "FFFFFFFFFFFFFFFF",
            "RRRsRRRRRRRRRRsR",
            "MMMsMMMMMMMMMMMM",
            "FFFFFFFFFFFFFFFF",
            "RRRRRRRsRRRRRsRR",
            "MMMMMMMMMMMMMMMM",
            "MFFFFFFFFFFFFFFM",
            "MRRsRRRRMRRRsRRM"
        }, 4, ch -> {
            if (ch == 'R') return ridge;
            if (ch == 'M') return mid;
            if (ch == 'F') return furrow;
            if (ch == 's') return speck;
            return null;
        });
    }

    private static Image renderPixelPattern(String[] art, int scale, java.util.function.Function<Character, Color> palette) {
        int h = art.length;
        int w = art[0].length();
        WritableImage img = new WritableImage(w * scale, h * scale);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = palette.apply(art[y].charAt(x));
                if (c != null) {
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            pw.setColor(x * scale + dx, y * scale + dy, c);
                        }
                    }
                }
            }
        }
        return img;
    }

    // HÀM LOAD ẢNH PIXEL SẮC NÉT (smooth = false -> giữ cạnh pixel rõ ràng, không bao giờ mờ nhòe)
    private static Image safeLoadPixelImage(String path, double width, double height) {
        java.net.URL url = Assets.class.getResource(path);
        if (url == null) {
            System.err.println("LỖI MẤT FILE: Không tìm thấy ảnh tại -> " + path);
            return null;
        }
        return new Image(url.toExternalForm(), width, height, true, false);
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
        SHOP_ICONS.put(name.toLowerCase(), safeLoadPixelImage("/view/assets/shop/" + name + ".png", 64, 64));
    }

    public static Image getShopIcon(String key) {
        return SHOP_ICONS.get(key);
    }
    
    public static Image getShopIcon(Item item) {
        String key = ItemAssetRegistry.getKey(item);
        Image icon = getShopIcon(key);
        if (icon != null) {
            return icon;
        }
        if (item instanceof model.game_item.AnimalFeedItem) {
            return SEED_ICON != null ? SEED_ICON : FERTILIZER_ICON;
        }
        if (item instanceof model.game_item.Seed) {
            return SEED_ICON;
        }
        return null;
    }

    // LOAD IMAGE (Hàm cũ)
    private static Image loadImage(String path) {
        return new Image(Assets.class.getResourceAsStream(path));
    }

    private static Image[] loadNpcFrames(String npcPrefix, String kind, int count, Image fallback) {
        Image[] arr = new Image[count];
        for (int i = 0; i < count; i++) {
            Image loaded = safeLoadPixelImage("/view/assets/npcs/" + npcPrefix + "_" + kind + "_" + i + ".png", 108, 108);
            arr[i] = (loaded != null) ? loaded : fallback;
        }
        return arr;
    }

    private static Image[] loadCropStages(String cropPrefix) {
        Image[] arr = new Image[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = safeLoadPixelImage("/view/assets/crops/" + cropPrefix + "_" + i + ".png", 96, 96);
        }
        return arr;
    }

    public static Image getCropSprite(Crop crop) {
        if (crop == null) {
            return null;
        }

        GrowthState state = crop.getCurrentState();
        if (state == GrowthState.DEAD) return DEAD_STATE;
        if (state == GrowthState.ROTTEN) return ROTTEN_STATE;

        String cropKey = CropAssetRegistry.getKey(crop);
        Image[] stages = null;
        if ("tomato".equals(cropKey))         stages = TOMATO_STAGES;
        else if ("sunflower".equals(cropKey)) stages = SUNFLOWER_STAGES;
        else if ("potato".equals(cropKey))    stages = POTATO_STAGES;
        else if ("corn".equals(cropKey))      stages = CORN_STAGES;
        else if ("water_rice".equals(cropKey)) stages = RICE_STAGES;
        else if ("orange".equals(cropKey))     stages = ORANGE_STAGES;
        else if ("apple".equals(cropKey))      stages = APPLE_STAGES;
        else if ("coconut".equals(cropKey))    stages = COCONUT_STAGES;

        if (stages != null) {
            int pct = crop.getCurrentGrowthProgress();
            int stageIdx;
            if (state == GrowthState.HARVEST || pct >= 100) {
                stageIdx = 3;      // 100%: Đủ chín sẵn sàng thu hoạch
            } else if (pct >= 60) {
                stageIdx = 2;      // 60% - 99%: Cây lớn / nụ hoa / trổ đòng
            } else if (pct >= 25) {
                stageIdx = 1;      // 25% - 59%: Cây con có lá
            } else {
                stageIdx = 0;      // 0% - 24%: Mầm non
            }
            if (stages[stageIdx] != null) {
                return stages[stageIdx];
            }
        }

        switch (state) {
            case SEED:     return SEED_STATE;
            case SEEDLING: return SEEDLING_STATE;
            case MATURE:   return MATURE_STATE;
            case HARVEST:  return getShopIcon(cropKey);
            default:       return null;
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