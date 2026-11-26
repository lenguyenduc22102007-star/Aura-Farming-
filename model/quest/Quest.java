package model.quest;

/**
 * Đại diện cho 1 nhiệm vụ trong chuỗi 1000 nhiệm vụ nối tiếp tự động (#1 -> #1000).
 */
public class Quest {

    public enum ObjectiveType {
        HARVEST_CROP,    // Thu hoạch nông sản (Tomato, Sunflower, Potato, Corn, Water Rice)
        CATCH_FISH,      // Câu cá trên sông (bất kỳ X con cá hoặc cá hiếm)
        COOK_FOOD,       // Nấu ăn tại đống lửa
        ANIMAL_PRODUCT,  // Thu hoạch sản phẩm vật nuôi (Gà, Lợn, Bò)
        EARN_MONEY       // Kiếm tổng cộng X tiền bán hàng ($)
    }

    private final int index; // 1 .. 1000
    private final ObjectiveType type;
    private final String subFilter; // null = bất kỳ; hoặc tên cây ("Tomato"...), "RARE" cho cá hiếm, hoặc "Gà"/"Lợn"/"Bò"
    private final String title;
    private final String description;
    private final int targetAmount;
    private final int rewardMoney;
    private final boolean specialMilestone;

    public Quest(int index,
                 ObjectiveType type,
                 String subFilter,
                 String title,
                 String description,
                 int targetAmount,
                 int rewardMoney,
                 boolean specialMilestone) {
        this.index = index;
        this.type = type;
        this.subFilter = subFilter;
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

    public String getSubFilter() {
        return subFilter;
    }

    public String getCropFilter() {
        return (type == ObjectiveType.HARVEST_CROP) ? subFilter : null;
    }

    public boolean isRareFishOnly() {
        return type == ObjectiveType.CATCH_FISH && "RARE".equalsIgnoreCase(subFilter);
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
