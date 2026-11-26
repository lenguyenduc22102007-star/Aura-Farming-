package model.quest;

import model.quest.Quest.ObjectiveType;

/**
 * Quản lý chuỗi 1000 nhiệm vụ nối tiếp tự động (#1 -> #1000).
 * Nhiệm vụ được sinh tất định (deterministic) theo questIndex để luôn nhất quán khi lưu/tải game.
 * Các mốc chia hết cho 10 (#10, #20, ..., #1000) là Nhiệm vụ Đặc biệt thưởng x3 tiền!
 */
public class QuestManager {

    public static final int MAX_QUESTS = 1000;

    private static final String[] CROP_KEYS = {
        "Tomato", "Potato", "Corn", "Sunflower", "Water Rice"
    };
    private static final String[] CROP_VI_NAMES = {
        "Cà chua", "Khoai tây", "Ngô", "Hướng dương", "Lúa nước"
    };

    private int currentQuestIndex = 1;     // 1 .. 1000 (1001 = đã hoàn thành trọn bộ 1000 nhiệm vụ)
    private int currentQuestProgress = 0;
    private Quest cachedQuest = null;

    public QuestManager() {
        this.cachedQuest = generateQuest(1);
    }

    public int getCurrentQuestIndex() {
        return currentQuestIndex;
    }

    public int getCurrentQuestProgress() {
        return currentQuestProgress;
    }

    public boolean isAllCompleted() {
        return currentQuestIndex > MAX_QUESTS;
    }

    public Quest getCurrentQuest() {
        if (isAllCompleted()) {
            return null;
        }
        if (cachedQuest == null || cachedQuest.getIndex() != currentQuestIndex) {
            cachedQuest = generateQuest(currentQuestIndex);
        }
        return cachedQuest;
    }

    public boolean isCurrentQuestReadyToClaim() {
        Quest q = getCurrentQuest();
        return q != null && currentQuestProgress >= q.getTargetAmount();
    }

    public void restoreState(int questIndex, int questProgress) {
        if (questIndex < 1) questIndex = 1;
        if (questIndex > MAX_QUESTS + 1) questIndex = MAX_QUESTS + 1;
        this.currentQuestIndex = questIndex;
        this.cachedQuest = isAllCompleted() ? null : generateQuest(this.currentQuestIndex);
        if (this.cachedQuest != null) {
            this.currentQuestProgress = Math.max(0, Math.min(this.cachedQuest.getTargetAmount(), questProgress));
        } else {
            this.currentQuestProgress = 0;
        }
    }

    /** Ghi nhận thu hoạch cây trồng. @return true nếu tiến độ nhiệm vụ thay đổi. */
    public boolean onCropHarvested(String cropName) {
        Quest q = getCurrentQuest();
        if (q == null || q.getType() != ObjectiveType.HARVEST_CROP) return false;
        if (currentQuestProgress >= q.getTargetAmount()) return false;

        String filter = q.getCropFilter();
        if (filter != null && !filter.isEmpty()) {
            if (cropName == null || !cropName.equalsIgnoreCase(filter)) {
                return false;
            }
        }
        currentQuestProgress = Math.min(q.getTargetAmount(), currentQuestProgress + 1);
        return true;
    }

    /** Ghi nhận câu được 1 con cá. */
    public boolean onFishCaught() {
        return incrementIfType(ObjectiveType.CATCH_FISH, 1);
    }

    /** Ghi nhận nấu xong 1 món ăn tại đống lửa. */
    public boolean onFoodCooked() {
        return incrementIfType(ObjectiveType.COOK_FOOD, 1);
    }

    /** Ghi nhận thu hoạch sản phẩm vật nuôi (trứng, sữa, nấm truffle) hoặc bán thú trưởng thành. */
    public boolean onAnimalHarvested() {
        return incrementIfType(ObjectiveType.ANIMAL_PRODUCT, 1);
    }

    /** Ghi nhận người chơi kiếm thêm tiền ($). */
    public boolean onMoneyEarned(int amount) {
        if (amount <= 0) return false;
        return incrementIfType(ObjectiveType.EARN_MONEY, amount);
    }

    private boolean incrementIfType(ObjectiveType expected, int delta) {
        Quest q = getCurrentQuest();
        if (q == null || q.getType() != expected || delta <= 0) return false;
        if (currentQuestProgress >= q.getTargetAmount()) return false;
        currentQuestProgress = Math.min(q.getTargetAmount(), currentQuestProgress + delta);
        return true;
    }

    /**
     * Nhận thưởng nhiệm vụ hiện tại và tự động chuyển sang nhiệm vụ #N+1.
     * @return đối tượng Quest vừa hoàn thành (hoặc null nếu chưa đủ điều kiện).
     */
    public Quest claimAndAdvance() {
        Quest completed = getCurrentQuest();
        if (completed == null || currentQuestProgress < completed.getTargetAmount()) {
            return null;
        }
        currentQuestIndex++;
        currentQuestProgress = 0;
        cachedQuest = isAllCompleted() ? null : generateQuest(currentQuestIndex);
        return completed;
    }

    /**
     * Sinh tự động nhiệm vụ nhất quán từ questIndex (1..1000).
     * Xoay vòng 5 nhóm mục tiêu, độ khó và tiền thưởng tăng dần; mốc chia hết cho 10 thưởng x3 tiền!
     */
    public static Quest generateQuest(int idx) {
        int clamped = Math.max(1, Math.min(MAX_QUESTS, idx));
        boolean special = (clamped % 10 == 0);
        int tier = (clamped - 1) / 10; // 0 .. 99

        ObjectiveType type;
        if (special) {
            // Mốc đặc biệt xoay vòng giữa các thử thách lớn hấp dẫn
            int spMode = (clamped / 10) % 5;
            switch (spMode) {
                case 1:  type = ObjectiveType.HARVEST_CROP;   break;
                case 2:  type = ObjectiveType.CATCH_FISH;     break;
                case 3:  type = ObjectiveType.EARN_MONEY;     break;
                case 4:  type = ObjectiveType.COOK_FOOD;      break;
                default: type = ObjectiveType.ANIMAL_PRODUCT; break;
            }
        } else {
            int rot = (clamped - 1) % 5;
            switch (rot) {
                case 0:  type = ObjectiveType.HARVEST_CROP;   break;
                case 1:  type = ObjectiveType.CATCH_FISH;     break;
                case 2:  type = ObjectiveType.COOK_FOOD;      break;
                case 3:  type = ObjectiveType.ANIMAL_PRODUCT; break;
                default: type = ObjectiveType.EARN_MONEY;     break;
            }
        }

        String title = special ? ("★ ĐẶC BIỆT #" + clamped) : ("NHIỆM VỤ #" + clamped);
        String cropFilter = null;
        String desc;
        int target;
        int baseReward;

        switch (type) {
            case HARVEST_CROP: {
                // Luân phiên giữa thu hoạch cây cụ thể và thu hoạch nông sản bất kỳ
                boolean anyCrop = special || (clamped % 2 == 0);
                int baseCount = 3 + Math.min(32, tier / 2);
                if (special) baseCount = Math.max(6, (int) Math.round(baseCount * 1.4));
                if (anyCrop) {
                    target = baseCount;
                    desc = "Thu hoạch " + target + " nông sản bất kỳ";
                } else {
                    int cIdx = ((clamped / 5) + tier) % CROP_KEYS.length;
                    cropFilter = CROP_KEYS[cIdx];
                    target = Math.max(2, baseCount - 1);
                    desc = "Thu hoạch " + target + " " + CROP_VI_NAMES[cIdx];
                }
                baseReward = 90 + clamped * 14;
                break;
            }
            case CATCH_FISH: {
                target = 2 + Math.min(18, tier / 4);
                if (special) target += 2;
                desc = "Câu " + target + " con cá trên sông";
                baseReward = 110 + clamped * 15;
                break;
            }
            case COOK_FOOD: {
                target = 1 + Math.min(10, tier / 8);
                if (special) target += 1;
                desc = "Nấu " + target + " món ăn tại đống lửa";
                baseReward = 120 + clamped * 16;
                break;
            }
            case ANIMAL_PRODUCT: {
                target = 1 + Math.min(12, tier / 6);
                if (special) target += 2;
                desc = "Thu hoạch vật nuôi " + target + " lần";
                baseReward = 125 + clamped * 16;
                break;
            }
            case EARN_MONEY:
            default: {
                int step = 150 + tier * 85;
                if (special) step = (int) Math.round(step * 1.5);
                target = (step / 50) * 50;
                desc = "Kiếm tổng cộng $" + target;
                baseReward = 130 + clamped * 18;
                break;
            }
        }

        int finalReward = special ? (baseReward * 3) : baseReward;
        // Làm tròn đẹp đến bội số của 5$
        finalReward = ((finalReward + 4) / 5) * 5;

        return new Quest(clamped, type, cropFilter, title, desc, target, finalReward, special);
    }
}
