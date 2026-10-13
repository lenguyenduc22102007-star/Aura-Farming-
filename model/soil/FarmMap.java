package model.soil;

import model.weather.Weather;
import utils.Constant;

public class FarmMap {
    private final int width;
    private final int height;
    private final Cell[][] grid;

    public FarmMap() {
        // Mặc định bản đồ rộng 25x25 ô để đủ không gian khám phá
        this(Constant.DEFAULT_FARM_COLS, Constant.DEFAULT_FARM_ROWS);
    }

    public FarmMap(int width, int height) {
        this(width, height, null);
    }

    public FarmMap(int width, int height, int[][] terrainGrid) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("FarmMap dimensions must be > 0 (width=" + width + ", height=" + height + ")");
        }
        this.width = width;
        this.height = height;
        this.grid = new Cell[height][width];

        // Quy hoạch bản đồ dựa trên Tiled Map
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = new Cell(c, r);
                
                if (terrainGrid != null && r < terrainGrid.length && c < terrainGrid[0].length) {
                    int tileId = terrainGrid[r][c];
                    // Map tile IDs from Tiled
                    if (tileId == 2 || tileId == 4 || tileId == 5) {
                        grid[r][c].setTerrain(TerrainType.PATH); // dirt paths
                    } else if (tileId == 3) {
                        grid[r][c].setTerrain(TerrainType.DIRT); // "soil" from Tiled = Tillable Soil
                    } else if (tileId == 11 || tileId == 12) {
                        grid[r][c].setTerrain(TerrainType.WATER);
                    } else {
                        grid[r][c].setTerrain(TerrainType.GRASS);
                    }
                } else {
                    grid[r][c].setTerrain(TerrainType.GRASS); // Mặc định là cỏ
                }
            }
        }
    }

    public Cell getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) return null;
        return grid[row][col];
    }

    public Cell[][] getGrid() {
        return grid;
    }

    /** Apply the daily tick (weather, crop growth, pests, evaporation) to every cell. Used by FarmController. */
    public void updateAllCells(Weather weather) {
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c].dailyUpdate(weather);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}