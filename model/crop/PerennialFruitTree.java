package model.crop;

import model.soil.Cell;
import model.weather.Weather;

/**
 * Cây ăn quả lâu năm (Cam, Táo, Dừa) trồng trong Vườn Cây Ăn Quả mở khóa:
 * - Trồng 1 lần duy nhất, lớn thành cây trưởng thành.
 * - Khi thu hoạch KHÔNG bị xóa khỏi ô đất mà lui về trạng thái MATURE, và cứ sau đúng 3 ngày tự ra quả mới mãi mãi!
 */
public abstract class PerennialFruitTree extends Crop {

    private boolean matureEstablished = false;
    private int regrowDaysCounter = 0;

    protected PerennialFruitTree(CropData cropData) {
        super(
            cropData,
            new StandardStateManager(100), // Không bao giờ bị thối rữa (ROTTEN) trên cành
            new DroughtResistanceStressManager(),
            new StandardGrowthManager()
        );
    }

    @Override
    public void dailyUpdate(Cell hostCell, Weather weather) {
        if (matureEstablished && getCurrentState() == GrowthState.MATURE) {
            hostCell.updateMoistureAmount(-cropData.getWaterPerDay());
            regrowDaysCounter++;
            if (regrowDaysCounter >= 3) {
                regrowDaysCounter = 0;
                restore(0, 100, GrowthState.HARVEST, 0);
            }
            return;
        }
        if (getCurrentState() == GrowthState.HARVEST) {
            matureEstablished = true;
            // Giữ quả tươi trên cây không bị hỏng
            restore(0, 100, GrowthState.HARVEST, 0);
            return;
        }
        super.dailyUpdate(hostCell, weather);
        if (getCurrentState() == GrowthState.HARVEST) {
            matureEstablished = true;
            regrowDaysCounter = 0;
        }
    }

    /** Được gọi khi người chơi hái quả: giữ nguyên cây trưởng thành trên đất và đếm lại 3 ngày ra lứa quả tiếp theo. */
    public void onPerennialHarvested() {
        this.matureEstablished = true;
        this.regrowDaysCounter = 0;
        restore(0, 75, GrowthState.MATURE, 0);
    }

    @Override
    public void restore(int stress, int growthProgress, GrowthState state, int daysAtHarvest) {
        if (state == GrowthState.MATURE && growthProgress >= 75) {
            this.matureEstablished = true;
            this.regrowDaysCounter = Math.max(0, Math.min(2, daysAtHarvest));
        } else if (state == GrowthState.HARVEST) {
            this.matureEstablished = true;
            this.regrowDaysCounter = 0;
        }
        super.restore(stress, growthProgress, state, daysAtHarvest);
    }

    @Override
    public int getDaysAtHarvest() {
        // Tận dụng trường daysAtHarvest trong SaveManager để lưu số ngày đang chờ ra lứa quả tiếp theo (0..2)
        if (matureEstablished && getCurrentState() == GrowthState.MATURE) {
            return regrowDaysCounter;
        }
        return super.getDaysAtHarvest();
    }
}
