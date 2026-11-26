package model.quest;

import model.game_item.FishSpecies;
import model.quest.Quest.ObjectiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Quản lý chuỗi 1000 nhiệm vụ nối tiếp tự động (#1 -> #1000).
 * Dùng seed cố định theo questIndex để mỗi số thứ tự luôn sinh ra cùng 1 nhiệm vụ nhất quán.
 * Các mốc chia hết cho 10 (#10, #20, #30 ... #1000) là Nhiệm vụ Đặc biệt (nhãn vàng, thưởng x3 tiền).
 */
public class QuestManager {

    public static final int MAX_QUESTS = 1000;

    private static final String[] CROP_KEYS = {
        "Tomato", "Sunflower", "Potato", "Corn", "Water Rice"
    };
    private static final String[] CROP_VI_NAMES = {
        "Cà chua", "Hướng dương", "Khoai tây", "Ngô", "Lúa nước"
    };
    private static final String[] ANIMAL_FILTERS = {
        null, "Egg", "Truffle", "Milk"
    };
    private static final String[] ANIMAL_VI_DESC = {
        "sản phẩm vật nuôi (Gà/Lợn/Bò)",
        "sản phẩm từ Gà (Trứng / Gà lớn)",
        "sản phẩm từ Lợn (Nấm Truffle / Lợn lớn)",
        "sản phẩm từ Bò (Sữa tươi / Bò lớn)"
    };

    private int currentQuestIndex = 1;     // 1 .. 1000 (1001 = đã hoàn tất trọn bộ 1000 nhiệm vụ)
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

    /** Xem trước count nhiệm vụ kế tiếp (#N+1, #N+2...) để hiển thị trong Cuốn Sổ Quản Lý. */
    public List<Quest> peekUpcomingQuests(int count) {
        List<Quest> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            int nextIdx = currentQuestIndex + i;
            if (nextIdx <= MAX_QUESTS) {
                list.add(generateQuest(nextIdx));
            }
        }
        return list;
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

    /** Ghi nhận thu hoạch cây trồng. */
    public boolean onCropHarvested(String cropName) {
        Quest q = getCurrentQuest();
        if (q == null || q.getType() != ObjectiveType.HARVEST_CROP) return false;
        if (currentQuestProgress >= q.getTargetAmount()) return false;

        String filter = q.getSubFilter();
        if (filter != null && !filter.isEmpty()) {
            if (cropName == null || !cropName.equalsIgnoreCase(filter)) {
                return false;
            }
        }
        currentQuestProgress = Math.min(q.getTargetAmount(), currentQuestProgress + 1);
        return true;
    }

    /** Ghi nhận câu cá (hỗ trợ cả câu cá bất kỳ và câu cá hiếm). */
    public boolean onFishCaught(FishSpecies species) {
        Quest q = getCurrentQuest();
        if (q == null || q.getType() != ObjectiveType.CATCH_FISH) return false;
        if (currentQuestProgress >= q.getTargetAmount()) return false;
        if (q.isRareFishOnly() && (species == null || !species.isRareOrHigher())) {
            return false;
        }
        currentQuestProgress = Math.min(q.getTargetAmount(), currentQuestProgress + 1);
        return true;
    }

    public boolean onFishCaught() {
        return onFishCaught(null);
    }

    /** Ghi nhận nấu xong 1 món ăn tại đống lửa. */
    public boolean onFoodCooked() {
        return incrementIfType(ObjectiveType.COOK_FOOD, 1);
    }

    /** Ghi nhận thu hoạch sản phẩm vật nuôi (typeKey: "Egg", "Truffle", "Milk" hoặc null). */
    public boolean onAnimalHarvested(String animalKey) {
        Quest q = getCurrentQuest();
        if (q == null || q.getType() != ObjectiveType.ANIMAL_PRODUCT) return false;
        if (currentQuestProgress >= q.getTargetAmount()) return false;

        String filter = q.getSubFilter();
        if (filter != null && !filter.isEmpty() && animalKey != null && !animalKey.isEmpty()) {
            if (!animalKey.toLowerCase().contains(filter.toLowerCase())) {
                return false;
            }
        }
        currentQuestProgress = Math.min(q.getTargetAmount(), currentQuestProgress + 1);
        return true;
    }

    public boolean onAnimalHarvested() {
        return onAnimalHarvested(null);
    }

    /** Ghi nhận kiếm tiền bán hàng / thu hoạch ($). */
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

    /** Nhận thưởng nhiệm vụ hiện tại và tự động mở ngay Nhiệm vụ #N+1. */
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
     * Thuật toán sinh nhiệm vụ tất định dùng seed cố định theo questIndex (1..1000).
     * Xoay vòng đa dạng 5 loại mục tiêu, độ khó & thưởng tăng dần, mốc chia hết cho 10 thưởng x3 tiền!
     */
    public static Quest generateQuest(int idx) {
        int clamped = Math.max(1, Math.min(MAX_QUESTS, idx));
        long fixedSeed = clamped * 6364136223846793005L + 1442695040888963407L;
        Random rng = new Random(fixedSeed);

        boolean special = (clamped % 10 == 0);
        int tier = (clamped - 1) / 10; // 0 .. 99

        // Xoay vòng 5 loại mục tiêu đa dạng, kết hợp biến thể ngẫu nhiên tất định theo seed
        ObjectiveType[] allTypes = ObjectiveType.values();
        int typeSlot = special ? ((clamped / 10 + rng.nextInt(2)) % allTypes.length) : ((clamped - 1) % allTypes.length);
        ObjectiveType type = allTypes[typeSlot];

        String title = special ? ("★ ĐẶC BIỆT #" + clamped) : ("NHIỆM VỤ #" + clamped);
        String subFilter = null;
        String desc;
        int target;
        int baseReward;

        switch (type) {
            case HARVEST_CROP: {
                boolean specificCrop = !special && (clamped <= 3 || rng.nextBoolean());
                int baseCount = 2 + Math.min(35, tier / 2) + rng.nextInt(2);
                if (special) baseCount = Math.max(6, (int) Math.round(baseCount * 1.5));
                if (specificCrop) {
                    int cIdx = (clamped == 1) ? 0 : rng.nextInt(CROP_KEYS.length);
                    subFilter = CROP_KEYS[cIdx];
                    target = baseCount;
                    desc = "Thu hoạch " + target + " " + CROP_VI_NAMES[cIdx];
                } else {
                    target = baseCount + 1;
                    desc = "Thu hoạch " + target + " nông sản bất kỳ";
                }
                baseReward = 95 + clamped * 14 + target * 6;
                break;
            }
            case CATCH_FISH: {
                boolean rareOnly = (clamped >= 12) && (special || rng.nextInt(3) == 0);
                if (rareOnly) {
                    subFilter = "RARE";
                    target = Math.max(1, 1 + Math.min(8, tier / 10));
                    if (special) target += 1;
                    desc = "Câu " + target + " con cá hiếm trên sông";
                    baseReward = 140 + clamped * 16 + target * 25;
                } else {
                    target = 2 + Math.min(18, tier / 4) + rng.nextInt(2);
                    if (special) target += 2;
                    desc = "Câu " + target + " con cá bất kỳ";
                    baseReward = 110 + clamped * 15 + target * 10;
                }
                break;
            }
            case COOK_FOOD: {
                target = 1 + Math.min(10, tier / 8) + (tier >= 4 ? rng.nextInt(2) : 0);
                if (special) target += 1;
                desc = "Nấu " + target + " món ăn tại đống lửa";
                baseReward = 120 + clamped * 16 + target * 18;
                break;
            }
            case ANIMAL_PRODUCT: {
                int aChoice = (clamped < 15 || special) ? 0 : rng.nextInt(ANIMAL_FILTERS.length);
                subFilter = ANIMAL_FILTERS[aChoice];
                target = 1 + Math.min(12, tier / 6) + (aChoice == 0 && tier >= 3 ? 1 : 0);
                if (special) target += 2;
                desc = "Thu hoạch " + target + " " + ANIMAL_VI_DESC[aChoice];
                baseReward = 125 + clamped * 16 + target * 15;
                break;
            }
            case EARN_MONEY:
            default: {
                int rawTarget = 150 + tier * 90 + rng.nextInt(3) * 50;
                if (special) rawTarget = (int) Math.round(rawTarget * 1.6);
                target = Math.max(100, (rawTarget / 50) * 50);
                desc = "Kiếm tổng cộng $" + target + " tiền bán hàng";
                baseReward = 135 + clamped * 18;
                break;
            }
        }

        int finalReward = special ? (baseReward * 3) : baseReward;
        finalReward = ((finalReward + 4) / 5) * 5;

        return new Quest(clamped, type, subFilter, title, desc, target, finalReward, special);
    }
}
