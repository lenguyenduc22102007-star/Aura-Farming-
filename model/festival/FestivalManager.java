package model.festival;

import java.util.Properties;

/**
 * Quản lý Lễ Hội Định Kỳ Mỗi 7 Ngày (Day 7, 14, 21, 28...):
 * - Tuần lẻ (Day 7, 21, 35...): 🎣 Giải Thi Câu Cá Bên Sông — câu đủ 5 con cá trong ngày nhận ngay +$1.000!
 * - Tuần chẵn (Day 14, 28, 42...): 🎆 Đêm Pháo Hoa Tại Khu Bể Bơi — từ 18:00, 6 NPC tụ hội quanh Bể Bơi,
 *   pháo hoa rực rỡ liên tục; người chơi ghé tham dự nhận +$800 và +0.5 Tim cho toàn bộ 6 NPC!
 */
public class FestivalManager {

    public enum FestivalType {
        NONE,
        FISHING_CONTEST,
        FIREWORKS_NIGHT
    }

    public static final int FISHING_TARGET = 5;
    public static final int FISHING_REWARD = 1000;
    public static final int FIREWORKS_REWARD = 800;

    private int activeFestivalDay = -1;
    private int fishCaughtToday = 0;
    private boolean fishingRewardClaimed = false;
    private boolean fireworksRewardClaimed = false;

    public FestivalType getFestivalForDay(int day) {
        if (day <= 0 || day % 7 != 0) {
            return FestivalType.NONE;
        }
        int weekIndex = day / 7;
        return (weekIndex % 2 == 1) ? FestivalType.FISHING_CONTEST : FestivalType.FIREWORKS_NIGHT;
    }

    /** Gọi vào đầu mỗi ngày mới để đồng bộ trạng thái lễ hội. */
    public void onDayStart(int day) {
        if (this.activeFestivalDay != day) {
            this.activeFestivalDay = day;
            this.fishCaughtToday = 0;
            this.fishingRewardClaimed = false;
            this.fireworksRewardClaimed = false;
        }
    }

    /** Ghi nhận 1 con cá vừa câu được; trả về true đúng vào khoảnh khắc hoàn thành mốc 5 con cá nhận +$1.000! */
    public boolean recordFishCaught(int currentDay) {
        onDayStart(currentDay);
        if (getFestivalForDay(currentDay) != FestivalType.FISHING_CONTEST || fishingRewardClaimed) {
            return false;
        }
        fishCaughtToday++;
        if (fishCaughtToday >= FISHING_TARGET) {
            fishingRewardClaimed = true;
            return true;
        }
        return false;
    }

    /** Kiểm tra và trao thưởng tham gia Đêm Pháo Hoa tại Khu Bể Bơi (từ 18:00 trở đi, 1 lần/ngày lễ hội). */
    public boolean tryClaimFireworksParticipation(int currentDay, int currentHour) {
        onDayStart(currentDay);
        if (getFestivalForDay(currentDay) != FestivalType.FIREWORKS_NIGHT) return false;
        if (currentHour < 18 || fireworksRewardClaimed) return false;
        fireworksRewardClaimed = true;
        return true;
    }

    public boolean isFireworksShowActive(int currentDay, int currentHour) {
        return getFestivalForDay(currentDay) == FestivalType.FIREWORKS_NIGHT && currentHour >= 18;
    }

    public int getFishCaughtToday() {
        return fishCaughtToday;
    }

    public boolean isFishingRewardClaimed() {
        return fishingRewardClaimed;
    }

    public boolean isFireworksRewardClaimed() {
        return fireworksRewardClaimed;
    }

    /** Nội dung biểu ngữ thông báo lễ hội nổi bật trên đầu màn hình vào ngày chia hết cho 7. */
    public String getTopBannerText(int currentDay, int currentHour) {
        onDayStart(currentDay);
        FestivalType type = getFestivalForDay(currentDay);
        if (type == FestivalType.FISHING_CONTEST) {
            if (fishingRewardClaimed) {
                return "🏆 LỄ HỘI NGÀY " + currentDay + " — ĐÃ VÔ ĐỊCH GIẢI CÂU CÁ BÊN SÔNG (+$" + FISHING_REWARD + ")!";
            }
            return "🎣 LỄ HỘI NGÀY " + currentDay + ": GIẢI THI CÂU CÁ BÊN SÔNG — Đã câu: "
                    + fishCaughtToday + "/" + FISHING_TARGET + " con (Thưởng +$" + FISHING_REWARD + ")";
        }
        if (type == FestivalType.FIREWORKS_NIGHT) {
            if (fireworksRewardClaimed) {
                return "🎆 ĐÊM PHÁO HOA NGÀY " + currentDay + " — Đang bắn rực rỡ tại Bể Bơi! (Đã nhận +$" + FIREWORKS_REWARD + " & +0.5 ❤ toàn bộ NPC)";
            }
            if (currentHour >= 18) {
                return "🎆 ĐÊM PHÁO HOA ĐÃ BẮT ĐẦU! Hãy qua cầu tới Khu Bể Bơi ngay để nhận +$" + FIREWORKS_REWARD + " & +0.5 ❤ toàn bộ NPC!";
            }
            return "🎆 LỄ HỘI NGÀY " + currentDay + ": ĐÊM PHÁO HOA TẠI KHU BỂ BƠI — Khai mạc lúc 18:00 (Thưởng +$" + FIREWORKS_REWARD + " & +0.5 ❤ NPC)";
        }
        return null;
    }

    public void save(Properties p) {
        if (p == null) return;
        p.setProperty("festival.day", Integer.toString(activeFestivalDay));
        p.setProperty("festival.fishCaught", Integer.toString(fishCaughtToday));
        p.setProperty("festival.fishClaimed", Boolean.toString(fishingRewardClaimed));
        p.setProperty("festival.fireworksClaimed", Boolean.toString(fireworksRewardClaimed));
    }

    public void load(Properties p) {
        if (p == null) return;
        this.activeFestivalDay = Integer.parseInt(p.getProperty("festival.day", "-1"));
        this.fishCaughtToday = Integer.parseInt(p.getProperty("festival.fishCaught", "0"));
        this.fishingRewardClaimed = Boolean.parseBoolean(p.getProperty("festival.fishClaimed", "false"));
        this.fireworksRewardClaimed = Boolean.parseBoolean(p.getProperty("festival.fireworksClaimed", "false"));
    }
}
