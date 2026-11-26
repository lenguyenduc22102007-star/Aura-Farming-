package model.quest;

/**
 * Đại diện cho 1 nhiệm vụ trong chuỗi 1000 nhiệm vụ nối tiếp tự động (#1 -> #1000).
 */
public class Quest {

    public enum ObjectiveType {
        HARVEST_CROP,    // Thu hoạch nông sản
        CATCH_FISH,      // Câu cá trên sông
        COOK_FOOD,       // Nấu ăn tại đống lửa
        ANIMAL_PRODUCT,  // Thu hoạch vật nuôi (trứng/sữa/nấm hoặc bán thú lớn)
        EARN_MONEY       // Kiếm tổng số tiền ($)
    }

    private final int index; // 1 .. 1000
    private final ObjectiveType type;
    private final String cropFilter; // null = mọi nông sản, hoặc "Tomato", "Potato", "Corn", "Sunflower", "Water Rice"
    private final String title;
    private final String description;
    private final int targetAmount;
    private final int rewardMoney;
    private final boolean specialMilestone;

    public Quest(int index,
                 ObjectiveType type,
                 String cropFilter,
                 String title,
                 String description,
                 int targetAmount,
                 int rewardMoney,
                 boolean specialMilestone) {
        this.index = index;
        this.type = type;
        this.cropFilter = cropFilter;
        this.title = title;
        this.description = description;
        this.targetAmount = Math.max(1, targetAmount);
        this.rewardMoney = Math.max(10, rewardMoney);
        this.specialMilestone = specialMilestone;
    }

    public int getIndex() {
        return index;
    }

    public ObjectiveType getType() {
        return type;
    }

    public String getCropFilter() {
        return cropFilter;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getRewardMoney() {
        return rewardMoney;
    }

    public boolean isSpecialMilestone() {
        return specialMilestone;
    }
}
