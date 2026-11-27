import math
import os
import shutil
from collections import deque
from PIL import Image

"""
Xử lý 6 nhân vật NPC từ sheet_npcs_pool.png & sheet_npcs_bots.png:
1. Xóa sạch 100% nền hồng tím (#FF00FF) và viền mờ xung quanh.
2. Đưa về đúng khung 108x108 đồng bộ kích thước cơ thể 94px của Luffy (chân chạm y=106),
   riêng Chopper cao 64px nhỏ nhắn dễ thương (chân vẫn chạm y=106).
3. Tách khớp Vai & Hông (Tay trái, Tay phải, Chân trái, Chân phải, Thân & Đầu) để tạo trọn bộ
   6 frame chuyển động mượt mà cho MỖI nhân vật:
   - {name}.png (base)
   - {name}_idle_0.png, {name}_idle_1.png (2 frame đứng thở nhấp nhô tay/vai giống Luffy)
   - {name}_walk_0.png .. {name}_walk_3.png (4 frame bước đi vung tay & sải chân chéo ngược pha như Luffy)
"""

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_NPC_DIR = os.path.join(ROOT_DIR, "view", "assets", "npcs")
PROD_NPC_DIR = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "npcs")
SCRATCH_PREVIEW = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\scratch\verify_npc_walk_sheets.png"

CANVAS_SIZE = 108
FOOT_BASELINE_Y = 106


def clean_magenta_bg(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()

    def is_pure_bg(r, g, b, a):
        if a < 16:
            return True
        return r >= 210 and b >= 200 and g <= 65 and (r - g) >= 155 and (b - g) >= 145

    def is_bg_floodable(r, g, b, a):
        if a < 16:
            return True
        return r >= 160 and b >= 150 and g <= 110 and (r - g) >= 80 and (b - g) >= 75 and abs(r - b) <= 70

    visited = bytearray(w * h)
    q = deque()

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if x == 0 or x == w - 1 or y == 0 or y == h - 1 or is_pure_bg(r, g, b, a):
                if is_bg_floodable(r, g, b, a):
                    idx = y * w + x
                    if not visited[idx]:
                        visited[idx] = 1
                        q.append((x, y))

    while q:
        x, y = q.popleft()
        px[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < w and 0 <= ny < h:
                nidx = ny * w + nx
                if not visited[nidx]:
                    r, g, b, a = px[nx, ny]
                    if is_bg_floodable(r, g, b, a):
                        visited[nidx] = 1
                        q.append((nx, ny))

    for _ in range(3):
        to_clear = []
        to_despill = []
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                adj_trans = False
                for nx, ny in (
                    (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1),
                    (x - 1, y - 1), (x + 1, y - 1), (x - 1, y + 1), (x + 1, y + 1)
                ):
                    if not (0 <= nx < w and 0 <= ny < h) or px[nx, ny][3] == 0:
                        adj_trans = True
                        break
                if not adj_trans:
                    continue
                if (r - g) >= 50 and (b - g) >= 45 and abs(r - b) <= 70:
                    if g < 140 and r > 130 and b > 120:
                        to_clear.append((x, y))
                    else:
                        to_despill.append((x, y, min(r, g + 22), g, min(b, g + 22), a))
        for x, y in to_clear:
            px[x, y] = (0, 0, 0, 0)
        for x, y, nr, ng, nb, na in to_despill:
            px[x, y] = (nr, ng, nb, na)

    return im


def build_base_108(cell_im: Image.Image, target_h_px: int) -> Image.Image:
    w, h = cell_im.size
    px = cell_im.load()
    seen = bytearray(w * h)
    components = []

    for y in range(h):
        for x in range(w):
            idx = y * w + x
            if not seen[idx] and px[x, y][3] > 0:
                comp = []
                q = deque([(x, y)])
                seen[idx] = 1
                while q:
                    cx, cy = q.popleft()
                    comp.append((cx, cy))
                    for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                        if 0 <= nx < w and 0 <= ny < h:
                            nidx = ny * w + nx
                            if not seen[nidx] and px[nx, ny][3] > 0:
                                seen[nidx] = 1
                                q.append((nx, ny))
                components.append(comp)

    if components:
        max_len = max(len(c) for c in components)
        for comp in components:
            if len(comp) < max_len * 0.05:
                for cx, cy in comp:
                    px[cx, cy] = (0, 0, 0, 0)

    bbox = cell_im.getbbox()
    if not bbox:
        return Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))

    cropped = cell_im.crop(bbox)
    cw, ch = cropped.size
    scale = float(target_h_px) / float(ch)
    nw = max(1, int(round(cw * scale)))
    nh = target_h_px
    resized = cropped.resize((nw, nh), Image.Resampling.NEAREST)

    # Hậu kiểm sau resize để xóa sạch mọi điểm hồng nhạt còn sót ở rìa chân
    rpx = resized.load()
    for y in range(nh):
        for x in range(nw):
            r, g, b, a = rpx[x, y]
            if a > 0 and r > 180 and b > 170 and g < 165 and (r - g) > 60 and (b - g) > 55:
                rpx[x, y] = (0, 0, 0, 0)

    out = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    ox = (CANVAS_SIZE - nw) // 2
    oy = FOOT_BASELINE_Y - nh
    out.paste(resized, (ox, oy), resized)
    return out


# Thông số giải phẫu khớp vai & khớp hông chuẩn trên khung 108x108 cho từng nhân vật
RIG_SPECS = {
    "nami": {
        "shoulder_y": 46, "arm_bot_y": 69,
        "arm_L_max_x": 45, "sh_L": (44, 47),
        "arm_R_min_x": 62, "sh_R": (62, 47),
        "hip_y": 70, "mid_x": 54,
        "hip_L": (49, 71), "hip_R": (59, 71),
    },
    "hancock": {
        "shoulder_y": 45, "arm_bot_y": 73,
        "arm_L_max_x": 36, "sh_L": (42, 46),
        "arm_R_min_x": 62, "sh_R": (62, 46),
        "hip_y": 71, "mid_x": 54,
        "hip_L": (51, 72), "hip_R": (57, 72),
    },
    "zoro": {
        "shoulder_y": 42, "arm_bot_y": 69,
        "arm_L_max_x": 44, "sh_L": (43, 44),
        "arm_R_min_x": 63, "sh_R": (64, 44),
        "hip_y": 74, "mid_x": 54,
        "hip_L": (48, 75), "hip_R": (60, 75),
    },
    "sanji": {
        "shoulder_y": 41, "arm_bot_y": 58,
        "arm_L_max_x": 44, "sh_L": (44, 43),
        "arm_R_min_x": 62, "sh_R": (62, 43),
        "hip_y": 69, "mid_x": 53,
        "hip_L": (48, 70), "hip_R": (59, 70),
    },
    "chopper": {
        "shoulder_y": 80, "arm_bot_y": 95,
        "arm_L_max_x": 46, "sh_L": (45, 81),
        "arm_R_min_x": 62, "sh_R": (62, 81),
        "hip_y": 94, "mid_x": 54,
        "hip_L": (49, 95), "hip_R": (59, 95),
    },
    "usopp": {
        "shoulder_y": 42, "arm_bot_y": 68,
        "arm_L_max_x": 44, "sh_L": (44, 44),
        "arm_R_min_x": 62, "sh_R": (62, 44),
        "hip_y": 70, "mid_x": 54,
        "hip_L": (48, 71), "hip_R": (60, 71),
    },
}


def rotate_part_around_pivot(part: Image.Image, pivot: tuple, angle_deg: float, dx: int = 0, dy: int = 0) -> Image.Image:
    """Xoay bộ phận tay/chân quanh khớp vai/hông bằng supersampling 2x rồi lấy mẫu NEAREST sắc nét."""
    if abs(angle_deg) < 0.01 and dx == 0 and dy == 0:
        return part.copy()

    s = 2
    big = part.resize((CANVAS_SIZE * s, CANVAS_SIZE * s), Image.Resampling.NEAREST)
    px_piv = (pivot[0] * s, pivot[1] * s)
    rotated_big = big.rotate(-angle_deg, resample=Image.Resampling.NEAREST, center=px_piv)
    small = rotated_big.resize((CANVAS_SIZE, CANVAS_SIZE), Image.Resampling.NEAREST)

    if dx == 0 and dy == 0:
        return small

    shifted = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    shifted.paste(small, (int(round(dx)), int(round(dy))), small)
    return shifted


def segment_character(base: Image.Image, cfg: dict):
    """Tách nhân vật thành 5 lớp: torso_head, arm_L, arm_R, leg_L, leg_R (có trám viền hông/sườn mượt mà)."""
    bpx = base.load()
    torso = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    arm_L = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    arm_R = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    leg_L = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    leg_R = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))

    tpx = torso.load()
    alpx = arm_L.load()
    arpx = arm_R.load()
    llpx = leg_L.load()
    lrpx = leg_R.load()

    sh_y = cfg["shoulder_y"]
    ab_y = cfg["arm_bot_y"]
    al_max = cfg["arm_L_max_x"]
    ar_min = cfg["arm_R_min_x"]
    hip_y = cfg["hip_y"]
    mid_x = cfg["mid_x"]

    for y in range(CANVAS_SIZE):
        for x in range(CANVAS_SIZE):
            col = bpx[x, y]
            if col[3] == 0:
                continue
            if y >= hip_y:
                # Vùng chồng khớp hông 2px giữ cả ở thân dưới để không hở khe khi xoay đùi
                if y <= hip_y + 2 and (mid_x - 7 <= x <= mid_x + 7):
                    tpx[x, y] = col
                if x < mid_x:
                    llpx[x, y] = col
                else:
                    lrpx[x, y] = col
                # Với nhân vật khép chân sát nhau (như Hancock), cho phép 1px biên giữa có viền tối ở cả 2 chân
                if x == mid_x:
                    llpx[x - 1, y] = (26, 18, 20, 255) if llpx[x - 1, y][3] == 0 else llpx[x - 1, y]
            elif sh_y <= y < ab_y and x <= al_max:
                alpx[x, y] = col
                # Trám 2px sát sườn trái để khi tay vung ra không bị hở sườn/tóc
                if x >= al_max - 1 and bpx[min(CANVAS_SIZE - 1, al_max + 2), y][3] > 0:
                    tpx[x, y] = bpx[min(CANVAS_SIZE - 1, al_max + 2), y]
            elif sh_y <= y < ab_y and x >= ar_min:
                arpx[x, y] = col
                if x <= ar_min + 1 and bpx[max(0, ar_min - 2), y][3] > 0:
                    tpx[x, y] = bpx[max(0, ar_min - 2), y]
            else:
                tpx[x, y] = col

    return torso, arm_L, arm_R, leg_L, leg_R


def compose_pose(parts, cfg: dict,
                 arm_L_ang: float, arm_R_ang: float,
                 leg_L_ang: float, leg_R_ang: float,
                 leg_L_shift: tuple = (0, 0), leg_R_shift: tuple = (0, 0),
                 body_dy: int = 0, back_leg_is_left: bool = True) -> Image.Image:
    torso, arm_L, arm_R, leg_L, leg_R = parts

    r_arm_L = rotate_part_around_pivot(arm_L, cfg["sh_L"], arm_L_ang, 0, body_dy)
    r_arm_R = rotate_part_around_pivot(arm_R, cfg["sh_R"], arm_R_ang, 0, body_dy)
    r_leg_L = rotate_part_around_pivot(leg_L, cfg["hip_L"], leg_L_ang, leg_L_shift[0], leg_L_shift[1] + body_dy)
    r_leg_R = rotate_part_around_pivot(leg_R, cfg["hip_R"], leg_R_ang, leg_R_shift[0], leg_R_shift[1] + body_dy)
    r_torso = rotate_part_around_pivot(torso, (cfg["mid_x"], cfg["hip_y"]), 0.0, 0, body_dy)

    frame = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))

    # Vẽ tay sau (arm_L) -> chân sau -> thân/đầu -> chân trước -> tay trước (arm_R) (vì nhân vật hướng mặt hơi chếch phải)
    frame.alpha_composite(r_arm_L)
    if back_leg_is_left:
        frame.alpha_composite(r_leg_L)
        frame.alpha_composite(r_torso)
        frame.alpha_composite(r_leg_R)
    else:
        frame.alpha_composite(r_leg_R)
        frame.alpha_composite(r_torso)
        frame.alpha_composite(r_leg_L)
    frame.alpha_composite(r_arm_R)

    # Kẹp bàn chân không vượt quá y=107 để không bị lệch mặt đất
    fpx = frame.load()
    for y in range(FOOT_BASELINE_Y + 1, CANVAS_SIZE):
        for x in range(CANVAS_SIZE):
            fpx[x, y] = (0, 0, 0, 0)

    return frame


def generate_6_frames(name: str, base_108: Image.Image) -> dict:
    cfg = RIG_SPECS[name]
    parts = segment_character(base_108, cfg)
    is_chopper = (name == "chopper")
    leg_sw = 26.0 if is_chopper else 22.0
    arm_sw = 26.0 if is_chopper else 22.0
    stride_x = 2 if is_chopper else 3

    # Idle 0: Tư thế gốc chuẩn
    idle_0 = base_108.copy()
    # Idle 1: Nhịp thở nhấp nhô nhẹ thân trên (-1px) + hai cánh tay đung đưa nhẹ mở ra
    idle_1 = compose_pose(
        parts, cfg,
        arm_L_ang=4.5, arm_R_ang=-4.5,
        leg_L_ang=0.0, leg_R_ang=0.0,
        leg_L_shift=(0, 1), leg_R_shift=(0, 1),
        body_dy=-1, back_leg_is_left=True
    )

    # Walk 0: Chân phải bước dài ra trước (-leg_sw), chân trái đạp ra sau (+leg_sw); Tay trái vung trước (-arm_sw), tay phải đánh ra sau (+arm_sw)
    walk_0 = compose_pose(
        parts, cfg,
        arm_L_ang=-arm_sw, arm_R_ang=arm_sw,
        leg_L_ang=leg_sw, leg_R_ang=-leg_sw,
        leg_L_shift=(-stride_x, -2), leg_R_shift=(stride_x, -2),
        body_dy=1, back_leg_is_left=True
    )

    # Walk 1: Pha thu chân chuyển bước (chân phải trụ thẳng, chân trái co gối nhấc cao bước qua)
    walk_1 = compose_pose(
        parts, cfg,
        arm_L_ang=-7.0, arm_R_ang=7.0,
        leg_L_ang=6.0, leg_R_ang=-4.0,
        leg_L_shift=(0, -3), leg_R_shift=(0, 0),
        body_dy=-1, back_leg_is_left=True
    )

    # Walk 2: Chân trái bước dài ra trước (-leg_sw), chân phải đạp ra sau (+leg_sw); Tay phải vung trước (-arm_sw), tay trái đánh ra sau (+arm_sw)
    walk_2 = compose_pose(
        parts, cfg,
        arm_L_ang=arm_sw, arm_R_ang=-arm_sw,
        leg_L_ang=-leg_sw, leg_R_ang=leg_sw,
        leg_L_shift=(stride_x, -2), leg_R_shift=(-stride_x, -2),
        body_dy=1, back_leg_is_left=False
    )

    # Walk 3: Pha thu chân chuyển bước ngược lại (chân trái trụ thẳng, chân phải co gối nhấc cao)
    walk_3 = compose_pose(
        parts, cfg,
        arm_L_ang=7.0, arm_R_ang=-7.0,
        leg_L_ang=-4.0, leg_R_ang=6.0,
        leg_L_shift=(0, 0), leg_R_shift=(0, -3),
        body_dy=-1, back_leg_is_left=False
    )

    return {
        f"{name}.png": idle_0,
        f"{name}_idle_0.png": idle_0,
        f"{name}_idle_1.png": idle_1,
        f"{name}_walk_0.png": walk_0,
        f"{name}_walk_1.png": walk_1,
        f"{name}_walk_2.png": walk_2,
        f"{name}_walk_3.png": walk_3,
    }


def main() -> None:
    os.makedirs(OUT_NPC_DIR, exist_ok=True)
    char_entries = []

    pool_sheet = os.path.join(ROOT_DIR, "sheet_npcs_pool.png")
    if os.path.isfile(pool_sheet):
        pool_im = clean_magenta_bg(Image.open(pool_sheet))
        pw, ph = pool_im.size
        for i, name in enumerate(["nami", "hancock"]):
            sub = pool_im.crop(((pw * i) // 2, 0, (pw * (i + 1)) // 2, ph))
            char_entries.append((name, build_base_108(sub, target_h_px=94)))

    bots_sheet = os.path.join(ROOT_DIR, "sheet_npcs_bots.png")
    if os.path.isfile(bots_sheet):
        bots_im = clean_magenta_bg(Image.open(bots_sheet))
        bw, bh = bots_im.size
        for i, (name, th) in enumerate([("zoro", 94), ("sanji", 94), ("chopper", 64), ("usopp", 94)]):
            sub = bots_im.crop(((bw * i) // 4, 0, (bw * (i + 1)) // 4, bh))
            char_entries.append((name, build_base_108(sub, target_h_px=th)))

    all_frames_by_char = []
    for name, base_108 in char_entries:
        frames = generate_6_frames(name, base_108)
        for fn, img in frames.items():
            img.save(os.path.join(OUT_NPC_DIR, fn))
        all_frames_by_char.append((name, frames))
        print(f"Generated 7 sprites (108x108) for {name}")

    if os.path.isdir(os.path.dirname(PROD_NPC_DIR)):
        os.makedirs(PROD_NPC_DIR, exist_ok=True)
        for fn in os.listdir(OUT_NPC_DIR):
            if fn.endswith(".png"):
                shutil.copy2(os.path.join(OUT_NPC_DIR, fn), os.path.join(PROD_NPC_DIR, fn))

    # Lưu bảng kiểm tra trực quan so sánh trực tiếp với Luffy (hàng 0 = Luffy, hàng 1..6 = 6 NPC)
    os.makedirs(os.path.dirname(SCRATCH_PREVIEW), exist_ok=True)
    sheet = Image.new("RGBA", (6 * 116 + 16, 7 * 116 + 16), (48, 86, 48, 255))
    luffy_files = [
        "luffy_idle_down_0.png", "luffy_idle_side_0.png",
        "luffy_run_side_0.png", "luffy_run_side_1.png", "luffy_run_side_2.png", "luffy_run_side_3.png"
    ]
    for ci, lfn in enumerate(luffy_files):
        lp = os.path.join(ROOT_DIR, "view", "assets", "player", lfn)
        if os.path.isfile(lp):
            lim = Image.open(lp).convert("RGBA")
            sheet.paste(lim, (8 + ci * 116, 8), lim)

    for ri, (name, frames) in enumerate(all_frames_by_char):
        keys = [
            f"{name}_idle_0.png", f"{name}_idle_1.png",
            f"{name}_walk_0.png", f"{name}_walk_1.png", f"{name}_walk_2.png", f"{name}_walk_3.png"
        ]
        for ci, k in enumerate(keys):
            fim = frames[k]
            sheet.paste(fim, (8 + ci * 116, 8 + (ri + 1) * 116), fim)

    sheet.save(SCRATCH_PREVIEW)
    print("Saved visual verification sheet:", SCRATCH_PREVIEW)


if __name__ == "__main__":
    main()
