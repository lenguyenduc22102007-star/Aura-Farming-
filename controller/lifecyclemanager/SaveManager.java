package controller.lifecyclemanager;

import controller.shopmanager.ItemManager;
import controller.shopmanager.ShopController;
import model.crop.Corn;
import model.crop.Crop;
import model.crop.GrowthState;
import model.crop.Potato;
import model.crop.Sunflower;
import model.crop.Tomato;
import model.crop.WaterRice;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.player_inventory.Player;
import model.soil.Beetle;
import model.soil.Cell;
import model.soil.CellState;
import model.soil.FarmMap;
import model.weather.Weather;
import model.weather.WeatherType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Persists a running game to disk so it can be resumed with "Continue" even
 * after the application was closed. Model state is handled here; view-only
 * state (player position, animals...) is added to the same Properties by the
 * view layer.
 */
public final class SaveManager {

    private static final String VERSION = "1";
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SmartFarm-SaveIO");
        t.setDaemon(true);
        return t;
    });

    private SaveManager() {}

    private static Path file() {
        return Paths.get(System.getProperty("user.home"), ".smartfarm", "savegame.properties");
    }

    public static boolean exists() {
        return Files.isRegularFile(file());
    }

    public static void delete() {
        try {
            Files.deleteIfExists(file());
        } catch (IOException ignored) {
            // nothing sensible to do
        }
    }

    // ---------------------------------------------------------------- write
    /** Collect the model state into p. */
    public static void captureModel(Properties p, GameManager gm, Player player, FarmMap map) {
        captureModel(p, gm, player, map, null);
    }

    /** Collect the model state (including custom farm name) into p. */
    public static void captureModel(Properties p, GameManager gm, Player player, FarmMap map, String farmName) {
        p.setProperty("version", VERSION);
        if (farmName != null && !farmName.trim().isEmpty()) {
            p.setProperty("farm.name", farmName.trim());
        }
        p.setProperty("day", Integer.toString(gm.getCurrentDay()));
        p.setProperty("weather", gm.getCurrentWeather().getType().name());
        p.setProperty("money", Integer.toString(player.getMoney()));
        p.setProperty("player.hunger", Double.toString(player.getHunger()));
        p.setProperty("player.thirst", Double.toString(player.getThirst()));
        p.setProperty("map.width", Integer.toString(map.getWidth()));
        p.setProperty("map.height", Integer.toString(map.getHeight()));

        int i = 0;
        for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
            p.setProperty("inv." + i++, e.getKey().getName() + "|" + e.getValue() + "|" + e.getKey().getPrice());
        }
        p.setProperty("inv.count", Integer.toString(i));

        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                Cell cell = map.getCell(r, c);
                StringBuilder sb = new StringBuilder();
                sb.append(cell.getCellState().name()).append(',')
                  .append(cell.getNutrientLevel()).append(',')
                  .append(cell.getMoistureLevel()).append(',')
                  .append(cell.getSunlightLevel()).append(',')
                  .append(cell.getPest() != null ? 1 : 0).append(',');
                Crop crop = cell.getCrop();
                if (crop == null) {
                    sb.append("-");
                } else {
                    sb.append(crop.getName()).append(',')
                      .append(crop.getCurrentStress()).append(',')
                      .append(crop.getCurrentGrowthProgress()).append(',')
                      .append(crop.getCurrentState().name()).append(',')
                      .append(crop.getDaysAtHarvest());
                }
                p.setProperty("cell." + r + "." + c, sb.toString());
            }
        }
    }

    public static synchronized void write(Properties p) {
        Path target = file();
        Path tmp = target.resolveSibling("savegame.tmp");
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "Smart Farm save");
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not save game: " + e);
        }
    }

    /** Ghi file save trên luồng nền riêng để không bao giờ gây khựng khung hình khi đang chơi. */
    public static void writeAsync(Properties p) {
        if (p == null) return;
        SAVE_EXECUTOR.submit(() -> write(p));
    }

    // ----------------------------------------------------------------- read
    /** @return the saved data, or null when there is no (readable) save. */
    public static Properties read() {
        if (!exists()) return null;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file())) {
            p.load(in);
        } catch (IOException e) {
            return null;
        }
        return VERSION.equals(p.getProperty("version")) ? p : null;
    }

    public static String readFarmName(Properties p) {
        if (p == null) return "Nông Trại Vui Vẻ";
        String n = p.getProperty("farm.name", "").trim();
        return n.isEmpty() ? "Nông Trại Vui Vẻ" : n;
    }

    /** Apply the model part of a save onto freshly-built session objects. */
    public static void applyModel(Properties p, GameManager gm, Player player, FarmMap map, ShopController shop) {
        gm.setCurrentDay(Integer.parseInt(p.getProperty("day", "1")));
        gm.setCurrentWeather(Weather.of(WeatherType.valueOf(p.getProperty("weather", "SUNNY"))));

        int money = Integer.parseInt(p.getProperty("money", "0"));
        if (money > player.getMoney()) player.addMoney(money - player.getMoney());
        else if (money < player.getMoney()) player.deductMoney(player.getMoney() - money);
        player.setHunger(Double.parseDouble(p.getProperty("player.hunger", "100.0")));
        player.setThirst(Double.parseDouble(p.getProperty("player.thirst", "100.0")));

        int invCount = Integer.parseInt(p.getProperty("inv.count", "0"));
        for (int i = 0; i < invCount; i++) {
            String[] f = p.getProperty("inv." + i).split("\\|");
            Item item = createItem(f[0], Integer.parseInt(f[2]), shop);
            if (item != null) player.getInventory().addItem(item, Integer.parseInt(f[1]));
        }

        for (int r = 0; r < map.getHeight(); r++) {
            for (int c = 0; c < map.getWidth(); c++) {
                String raw = p.getProperty("cell." + r + "." + c);
                if (raw == null) continue;
                String[] f = raw.split(",");
                Cell cell = map.getCell(r, c);
                cell.setLevels(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]));
                // Bảo vệ lối đi cỏ xanh giữa các luống 2x2: chỉ khôi phục trạng thái TILLED & cây trồng trên đúng ô đất DIRT
                if (cell.getTerrain() != model.soil.TerrainType.DIRT) {
                    cell.changeState(CellState.UNTILLED);
                    continue;
                }
                cell.changeState(CellState.valueOf(f[0]));
                if (f[4].equals("1")) cell.spawnPest(new Beetle());
                if (!f[5].equals("-")) {
                    Crop crop = createCrop(f[5]);
                    if (crop != null) {
                        crop.restore(Integer.parseInt(f[6]), Integer.parseInt(f[7]),
                                GrowthState.valueOf(f[8]), Integer.parseInt(f[9]));
                        // plantCrop requires a tilled cell; force the saved state around it
                        CellState saved = cell.getCellState();
                        cell.changeState(CellState.TILLED);
                        cell.plantCrop(crop);
                        cell.changeState(saved);
                    }
                }
            }
        }
    }

    private static Item createItem(String name, int price, ShopController shop) {
        if (name.endsWith(" (Crop)")) {
            return new HarvestedCropItem(name.substring(0, name.length() - " (Crop)".length()), price);
        }
        if (model.game_item.FishItem.DEFAULT_NAME.equalsIgnoreCase(name)
                || model.game_item.FishSpecies.findByDisplayName(name) != null) {
            return new model.game_item.FishItem(name, price);
        }
        if ("Fresh Egg".equalsIgnoreCase(name) || "Fresh Milk".equalsIgnoreCase(name) || "Farm Truffle".equalsIgnoreCase(name)) {
            return new model.game_item.AnimalProductItem(name, price);
        }
        model.game_item.CookedFoodItem cooked = model.game_item.CookedFoodItem.findByName(name);
        if (cooked != null) {
            return cooked;
        }
        for (ItemManager entry : shop.getCatalog()) {
            Item proto = entry.create();
            if (proto != null && proto.getName().equals(name)) return proto;
        }
        return null;
    }

    private static Crop createCrop(String name) {
        switch (name) {
            case "Corn":      return new Corn();
            case "Potato":    return new Potato();
            case "Sunflower": return new Sunflower();
            case "Tomato":    return new Tomato();
            case "Water Rice": return new WaterRice();
            default:          return null;
        }
    }
}
