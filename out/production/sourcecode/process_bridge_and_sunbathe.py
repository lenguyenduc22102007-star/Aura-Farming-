import os
import shutil
from collections import deque
from PIL import Image, ImageDraw

"""
Script Python tự động xử lý 2 ảnh mới:
1. Xóa sạch nền hồng tím (#FF00FF), viền hồng mờ và dấu sao góc dưới phải của ảnh Cầu mới -> lưu đè vào
   view/assets/tiled/objects/bridge.png (200x190, chuẩn TMX) và đồng bộ sang out/production/sourcecode.
2. Tách chuẩn sprite pixel-art Luffy nằm tắm nắng (nền trong suốt hoàn toàn) -> lưu vào
   view/assets/player/luffy_sunbathe.png và đồng bộ sang out/production/sourcecode.
"""

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
SUNBATHE_SRC = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\.user_uploaded\media_1795739111696_c85c3d7e.jpg"
BRIDGE_SRC   = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\.user_uploaded\media_1795739754478_f1c3f222.jpg"

BRIDGE_OUT = os.path.join(ROOT_DIR, "view", "assets", "tiled", "objects", "bridge.png")
BRIDGE_PROD = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "tiled", "objects", "bridge.png")

LUFFY_SUNBATHE_OUT = os.path.join(ROOT_DIR, "view", "assets", "player", "luffy_sunbathe.png")
LUFFY_SUNBATHE_PROD = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "player", "luffy_sunbathe.png")

PREVIEW_OUT = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\scratch\verify_bridge_and_sunbathe.png"


def process_bridge(src_path: str) -> Image.Image:
    im = Image.open(src_path).convert("RGBA")
    w, h = im.size
    px = im.load()

    # 1. Xóa dấu sao watermark mờ ở góc dưới phải (nằm hoàn toàn trên nền hồng)
    for y in range(int(h * 0.88), h):
        for x in range(int(w * 0.88), w):
            px[x, y] = (255, 0, 255, 255)

    # 2. Flood-fill xóa toàn bộ nền hồng tím (#FF00FF & hồng JPEG) từ 4 cạnh ảnh
    def is_magenta(r, g, b, a):
        if a < 16:
            return True
        return r >= 155 and b >= 145 and g <= 115 and (r - g) >= 75 and (b - g) >= 65 and abs(r - b) <= 75

    visited = bytearray(w * h)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if is_magenta(*px[x, y]):
                idx = y * w + x
                if not visited[idx]:
                    visited[idx] = 1
                    q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if is_magenta(*px[x, y]):
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
                if not visited[nidx] and is_magenta(*px[nx, ny]):
                    visited[nidx] = 1
                    q.append((nx, ny))

    # 3. Khử sạch 2 lớp viền hồng mờ bám sát cạnh cây cầu (anti-aliased fringe)
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
                if (r - g) >= 42 and (b - g) >= 38 and abs(r - b) <= 80:
                    if r > 125 and b > 115 and g < 130:
                        to_clear.append((x, y))
                    else:
                        # Giảm sắc tím/hồng nhiễm lên viền gỗ nâu tối
                        to_despill.append((x, y, min(r, g + 28), g, min(b, g + 16), a))
        for x, y in to_clear:
            px[x, y] = (0, 0, 0, 0)
        for x, y, nr, ng, nb, na in to_despill:
            px[x, y] = (nr, ng, nb, na)

    # 4. Crop gọn theo bounding box thực của cây cầu vòm gỗ, giữ đúng tỉ lệ tự nhiên (chiều ngang gỗ ~68% = x=32..168 khớp chuẩn mặt đường đá & thành cầu)
    bbox = im.getbbox()
    print("Bridge cleaned bbox:", bbox, "of", (w, h))
    cropped = im.crop(bbox)
    cw, ch = cropped.size
    target_h = 190
    target_w = min(142, max(128, int(round(cw * (target_h / float(ch))))))
    scaled = cropped.resize((target_w, target_h), Image.Resampling.LANCZOS)

    # Khóa alpha sắc nét (khử bán trong suốt & mọi vệt ám hồng ở viền gỗ)
    spx = scaled.load()
    for y in range(target_h):
        for x in range(target_w):
            r, g, b, a = spx[x, y]
            if a < 120:
                spx[x, y] = (0, 0, 0, 0)
            else:
                if (r - g) >= 40 and (b - g) >= 35 and b > 90:
                    b = min(b, g + 14)
                spx[x, y] = (r, g, b, 255)

    out = Image.new("RGBA", (200, 190), (0, 0, 0, 0))
    ox = (200 - target_w) // 2
    out.paste(scaled, (ox, 0), scaled)

    # Thêm bóng tiếp giáp mềm dưới 2 đầu bậc gỗ & chân cột để 2 đầu cầu chập tự nhiên vào mặt đường đá
    opx = out.load()
    cx_mid = 100
    plank_ys = [y for y in range(190) if opx[cx_mid, y][3] > 180]
    if plank_ys:
        top_py, bot_py = plank_ys[0], plank_ys[-1]
        # Tìm mép ngang ván gỗ giữa 2 trụ đầu cầu bắc & nam
        def plank_x_span(py):
            xs = [x for x in range(200) if opx[x, py][3] > 180]
            return (xs[0] + 10, xs[-1] - 10) if len(xs) > 24 else (ox + 12, ox + target_w - 12)

        nx0, nx1 = plank_x_span(min(189, top_py + 4))
        sx0, sx1 = plank_x_span(max(0, bot_py - 4))
        # Bóng đất mềm ngay trước bậc ván Bắc (y = top_py-4 .. top_py-1)
        for dy, alpha in [(-1, 105), (-2, 72), (-3, 42), (-4, 18)]:
            yy = top_py + dy
            if 0 <= yy < 190:
                for xx in range(nx0, nx1 + 1):
                    if opx[xx, yy][3] == 0:
                        opx[xx, yy] = (38, 24, 16, alpha)
        # Bóng đất mềm ngay dưới bậc ván Nam (y = bot_py+1 .. bot_py+5)
        for dy, alpha in [(1, 120), (2, 85), (3, 52), (4, 26), (5, 10)]:
            yy = bot_py + dy
            if 0 <= yy < 190:
                for xx in range(sx0, sx1 + 1):
                    if opx[xx, yy][3] == 0:
                        opx[xx, yy] = (36, 22, 14, alpha)

    print(f"Packed arched bridge: {target_w}x{target_h} at x={ox}..{ox + target_w} (frac={ox/200:.3f}..{(ox+target_w)/200:.3f})")

    out.save(BRIDGE_OUT)
    if os.path.isdir(os.path.dirname(BRIDGE_PROD)):
        shutil.copy2(BRIDGE_OUT, BRIDGE_PROD)
    print("Saved bridge:", BRIDGE_OUT)
    return out


def process_luffy_sunbathe(src_path: str) -> Image.Image:
    im = Image.open(src_path).convert("RGBA")
    w, h = im.size
    px = im.load()

    # Luffy pixel-art nằm gọn trong vùng x=150..605, y=35..265 với viền đen khép kín sắc nét
    # Bước 1: Tạo mặt nạ "khung ngoài" bằng cách flood-fill từ 4 biên qua các màu nền (cỏ xanh, gối vàng, gỗ nâu mờ, sọc xanh-trắng của ghế)
    def is_luffy_outline(r, g, b):
        return r < 42 and g < 40 and b < 42

    def is_luffy_skin_or_shorts(r, g, b):
        # Da rám nắng ấm (R > G > B rõ rệt, R >= 135, B < 105) — loại trừ gỗ ghế tối mờ (R < 120) & gối vàng (G > 165)
        is_skin = (138 <= r <= 235 and 72 <= g <= 165 and 35 <= b <= 115 and (r - b) >= 65 and (r - g) >= 28)
        # Vết sẹo chữ X đỏ nâu trên ngực
        is_scar = (120 <= r <= 185 and 40 <= g <= 85 and 30 <= b <= 70 and (r - g) >= 50)
        # Quần jean xanh đậm của Luffy (B > R + 25, B < 165 — khác sọc ghế cyan sáng G > 145, B > 195)
        is_shorts = (b >= r + 20 and 20 <= r <= 95 and 40 <= g <= 118 and 65 <= b <= 170)
        return is_skin or is_scar or is_shorts

    # Tìm tất cả pixel viền đen khép kín thuộc nhân vật Luffy (chỉ trong hộp chứa Luffy)
    outline_mask = bytearray(w * h)
    for y in range(40, 268):
        for x in range(155, 600):
            r, g, b, _ = px[x, y]
            if is_luffy_outline(r, g, b):
                outline_mask[y * w + x] = 1

    # Giãn nhẹ viền đen 1px để bít khe JPEG nhỏ
    barrier = bytearray(outline_mask)
    for y in range(41, 267):
        for x in range(156, 599):
            if outline_mask[y * w + x]:
                barrier[y * w + (x - 1)] = 1
                barrier[y * w + (x + 1)] = 1
                barrier[(y - 1) * w + x] = 1
                barrier[(y + 1) * w + x] = 1

    # Flood-fill từ toàn bộ ngoài hộp Luffy qua các màu nền (cỏ xanh, gối vàng, gỗ nâu ghế, sọc xanh-trắng)
    outside = bytearray(w * h)
    q = deque()
    for y in range(h):
        for x in range(w):
            if x < 155 or x >= 600 or y < 40 or y >= 268:
                outside[y * w + x] = 1
                if (x in (154, 600) and 40 <= y < 268) or (y in (39, 268) and 155 <= x < 600):
                    q.append((x, y))

    while q:
        x, y = q.popleft()
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 155 <= nx < 600 and 40 <= ny < 268:
                nidx = ny * w + nx
                if not outside[nidx] and not barrier[nidx]:
                    r, g, b, _ = px[nx, ny]
                    # Không dừng lại ở vùng gối vàng bên trái đầu Luffy (x < 235) hoặc nền gỗ tối ngoài bàn chân
                    is_yellow_pillow = (nx < 235 and r > 150 and g > 110 and b < 95)
                    is_olive_grass = (g >= r - 8 and g > b + 35 and b < 100)
                    if is_yellow_pillow or is_olive_grass or not is_luffy_skin_or_shorts(r, g, b):
                        outside[nidx] = 1
                        q.append((nx, ny))

    # Xuất Luffy nguyên vẹn sắc nét, xóa sạch mọi pixel gối vàng / sọc nệm / cỏ xanh lọt khe
    out_im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out_im.load()
    for y in range(40, 268):
        for x in range(155, 600):
            idx = y * w + x
            if not outside[idx]:
                r, g, b, a = px[x, y]
                is_cyan_mattress = (b > 165 and g > 135 and r < 160)
                is_white_mattress = (r > 210 and g > 214 and b > 210 and y > 175 and x < 425)
                is_green_grass = (g >= r - 6 and g > b + 35 and b < 95)
                is_left_pillow = (x < 228 and y > 125 and r > 140 and g > 105 and b < 95)
                is_bottom_wood = (y > 234 and r < 115 and g < 80 and b < 55 and not is_luffy_outline(r, g, b))
                if is_cyan_mattress or is_white_mattress or is_green_grass or is_left_pillow or is_bottom_wood:
                    continue
                opx[x, y] = (r, g, b, 255)

    # Cắt gọt nhẹ 1px rìa vàng/nâu-mờ dính sát ngoài viền đen của Luffy
    for _ in range(2):
        trim = []
        for y in range(41, 267):
            for x in range(156, 599):
                r, g, b, a = opx[x, y]
                if a == 0 or is_luffy_outline(r, g, b):
                    continue
                adj_t = (opx[x - 1, y][3] == 0 or opx[x + 1, y][3] == 0 or opx[x, y - 1][3] == 0 or opx[x, y + 1][3] == 0)
                if adj_t:
                    # Nếu pixel ngoài rìa là màu vàng gối, xanh cỏ, xanh cyan nệm hoặc nâu ghế mờ thì bỏ đi
                    if (g > b + 35 and r < 185 and b < 90) or (b > 160 and g > 130) or (x < 230 and r > 120 and g > 95 and b < 85):
                        trim.append((x, y))
        for tx, ty in trim:
            opx[tx, ty] = (0, 0, 0, 0)

    # Giữ thành phần liên thông lớn nhất (Luffy) để loại bỏ mọi chấm vụn
    seen = bytearray(w * h)
    comps = []
    for y in range(40, 268):
        for x in range(155, 600):
            idx = y * w + x
            if not seen[idx] and opx[x, y][3] > 0:
                comp = []
                cq = deque([(x, y)])
                seen[idx] = 1
                while cq:
                    cx, cy = cq.popleft()
                    comp.append((cx, cy))
                    for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                        if 0 <= nx < w and 0 <= ny < h:
                            nidx = ny * w + nx
                            if not seen[nidx] and opx[nx, ny][3] > 0:
                                seen[nidx] = 1
                                cq.append((nx, ny))
                comps.append(comp)
    if comps:
        biggest = max(len(c) for c in comps)
        for comp in comps:
            if len(comp) < biggest * 0.20:
                for cx, cy in comp:
                    opx[cx, cy] = (0, 0, 0, 0)

    # Crop sát Luffy và đưa về khung 112x68 ngang chuẩn tỉ lệ nằm trọn trên ghế tắm nắng (93x58 in-game)
    bb = out_im.getbbox()
    print("Luffy sunbathe bbox:", bb)
    cropped = out_im.crop(bb)
    cw, ch = cropped.size
    target_w = 104
    scale = target_w / float(cw)
    nw = target_w
    nh = max(1, int(round(ch * scale)))
    resized = cropped.resize((nw, nh), Image.Resampling.NEAREST)

    canvas = Image.new("RGBA", (112, 68), (0, 0, 0, 0))
    ox = (112 - nw) // 2
    oy = (68 - nh) // 2
    canvas.paste(resized, (ox, oy), resized)

    canvas.save(LUFFY_SUNBATHE_OUT)
    if os.path.isdir(os.path.dirname(LUFFY_SUNBATHE_PROD)):
        shutil.copy2(LUFFY_SUNBATHE_OUT, LUFFY_SUNBATHE_PROD)
    print("Saved luffy sunbathe:", LUFFY_SUNBATHE_OUT, canvas.size)
    return canvas


def build_preview(bridge_im: Image.Image, luffy_im: Image.Image) -> None:
    os.makedirs(os.path.dirname(PREVIEW_OUT), exist_ok=True)
    prev = Image.new("RGBA", (620, 260), (42, 128, 178, 255))
    d = ImageDraw.Draw(prev)
    # Nền cỏ xanh bên trái cho ghế tắm nắng + Luffy
    d.rectangle([0, 0, 310, 260], fill=(108, 152, 62, 255))

    lounger_p = os.path.join(ROOT_DIR, "view", "assets", "tiled", "objects", "pool_lounger.png")
    if os.path.isfile(lounger_p):
        lg = Image.open(lounger_p).convert("RGBA").resize((186, 116), Image.Resampling.NEAREST)
        prev.paste(lg, (60, 86), lg)
    luf_big = luffy_im.resize((224, 136), Image.Resampling.NEAREST)
    prev.paste(luf_big, (42, 42), luf_big)

    # Bên phải: cây cầu mới đặt trên nền sông xanh dương để kiểm tra viền cắt hồng
    prev.paste(bridge_im, (365, 35), bridge_im)
    prev.save(PREVIEW_OUT)
    print("Saved preview:", PREVIEW_OUT)


if __name__ == "__main__":
    b_im = process_bridge(BRIDGE_SRC)
    l_im = process_luffy_sunbathe(SUNBATHE_SRC)
    build_preview(b_im, l_im)
