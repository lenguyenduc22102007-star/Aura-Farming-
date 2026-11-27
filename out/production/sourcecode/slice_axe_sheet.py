import os
import shutil
from PIL import Image, ImageDraw

"""
Tự động xóa nền hồng tím (#FF00FF), xóa dấu sao góc dưới phải, tách 4 frame Luffy bổ rìu từ sheet_axe.png,
căn thẳng chân ở đáy khung 72x72 (khớp tuyệt đối chiều cao Luffy của luffy_hoe_0..3.png),
resize NEAREST giữ nét pixel sắc, lưu vào view/assets/player_actions/luffy_axe_0..3.png.
"""

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_PATH = os.path.join(ROOT_DIR, "sheet_axe.png")
OUT_DIR = os.path.join(ROOT_DIR, "view", "assets", "player_actions")
PROD_DIR = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "player_actions")
PREVIEW_PATH = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\scratch\verify_luffy_axe.png"


def is_magenta(r, g, b, a):
    if a < 16:
        return True
    return r >= 150 and b >= 140 and g <= 120 and (r - g) >= 70 and (b - g) >= 60 and abs(r - b) <= 85


def clean_sheet(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()

    # 1. Xóa dấu sao watermark góc dưới phải
    for y in range(int(h * 0.84), h):
        for x in range(int(w * 0.90), w):
            px[x, y] = (0, 0, 0, 0)

    # 2. Xóa toàn bộ nền hồng tím (#FF00FF) kể cả các khe kín giữa 2 tay / 2 chân
    for y in range(h):
        for x in range(w):
            if is_magenta(*px[x, y]):
                px[x, y] = (0, 0, 0, 0)

    # 3. Khử sạch 2 lớp viền hồng mờ (anti-aliased magenta fringe) bám sát nét viền đen
    for _ in range(2):
        to_clear = []
        to_despill = []
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                adj_t = False
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if not (0 <= nx < w and 0 <= ny < h) or px[nx, ny][3] == 0:
                        adj_t = True
                        break
                if not adj_t:
                    continue
                if (r - g) >= 38 and (b - g) >= 34 and abs(r - b) <= 90:
                    if r > 115 and b > 110 and g < 135:
                        to_clear.append((x, y))
                    else:
                        to_despill.append((x, y, min(r, g + 26), g, min(b, g + 20), a))
        for x, y in to_clear:
            px[x, y] = (0, 0, 0, 0)
        for x, y, nr, ng, nb, na in to_despill:
            px[x, y] = (nr, ng, nb, na)

    return im


def find_four_frame_splits(im: Image.Image):
    """Tìm chính xác 3 đường rãnh dọc ít pixel nhất ngăn cách 4 frame ngang."""
    w, h = im.size
    px = im.load()
    col_counts = [0] * w
    for x in range(w):
        cnt = 0
        for y in range(h):
            if px[x, y][3] > 20:
                cnt += 1
        col_counts[x] = cnt

    # Tìm 3 điểm cắt tối ưu quanh 1/4, 2/4, 3/4 chiều ngang
    cuts = [0]
    for k in (1, 2, 3):
        center = int(w * k / 4.0)
        lo = max(1, center - int(w * 0.06))
        hi = min(w - 1, center + int(w * 0.06))
        # Ưu tiên cột có 0 pixel nằm gần giữa khoảng trống nhất
        min_v = min(col_counts[lo:hi + 1])
        zero_cols = [x for x in range(lo, hi + 1) if col_counts[x] == min_v]
        cut_x = zero_cols[len(zero_cols) // 2]
        cuts.append(cut_x)
    cuts.append(w)
    print("Detected frame X cuts:", cuts)
    return [(cuts[i], cuts[i + 1]) for i in range(4)]


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(PROD_DIR, exist_ok=True)

    raw = Image.open(SRC_PATH)
    cleaned = clean_sheet(raw)
    w, h = cleaned.size

    spans = find_four_frame_splits(cleaned)
    crops = []
    bboxes = []
    for idx, (x0, x1) in enumerate(spans):
        sub = cleaned.crop((x0, 0, x1, h))
        bb = sub.getbbox()
        if bb is None:
            raise RuntimeError(f"Empty frame {idx}")
        cropped = sub.crop(bb)
        crops.append(cropped)
        bboxes.append(bb)
        print(f"Frame {idx}: span=({x0}..{x1}), tight bbox={bb}, size={cropped.size}")

    # Dùng chung 1 tỉ lệ scale cho cả 4 frame dựa theo frame cao nhất & rộng nhất để Luffy đồng bộ kích thước chuẩn luffy_hoe (56px cao, nằm gọn trong 72x72)
    max_cw = max(c.size[0] for c in crops)
    max_ch = max(c.size[1] for c in crops)
    # Khớp chiều cao cơ thể ~56-58px (tối đa 66px chiều rộng / 64px chiều cao trong khung 72x72)
    scale = min(68.0 / float(max_cw), 60.0 / float(max_ch))
    print(f"Uniform scale={scale:.5f} (max_src={max_cw}x{max_ch} -> max_dst={max_cw*scale:.1f}x{max_ch*scale:.1f})")

    frames_out = []
    for idx, cropped in enumerate(crops):
        cw, ch = cropped.size
        nw = max(1, int(round(cw * scale)))
        nh = max(1, int(round(ch * scale)))
        # Resize NEAREST giữ đúng nét pixel sắc theo yêu cầu
        resized = cropped.resize((nw, nh), Image.Resampling.NEAREST)

        # Khóa alpha sắc nét tuyệt đối
        rpx = resized.load()
        for y in range(nh):
            for x in range(nw):
                r, g, b, a = rpx[x, y]
                if a < 128:
                    rpx[x, y] = (0, 0, 0, 0)
                else:
                    rpx[x, y] = (r, g, b, 255)

        canvas = Image.new("RGBA", (72, 72), (0, 0, 0, 0))
        # Căn thẳng chân nhân vật ở đáy khung (y = 71 giống luffy_hoe_0..3.png) & căn giữa ngang
        px_x = (72 - nw) // 2
        px_y = 71 - nh
        canvas.paste(resized, (px_x, px_y), resized)

        out_path = os.path.join(OUT_DIR, f"luffy_axe_{idx}.png")
        canvas.save(out_path)
        shutil.copy2(out_path, os.path.join(PROD_DIR, f"luffy_axe_{idx}.png"))
        frames_out.append(canvas)
        print(f"Saved {out_path} -> placed at ({px_x},{px_y}) size=({nw}x{nh}) bbox={canvas.getbbox()}")

    # Xuất ảnh preview 4x trên nền caro để kiểm tra trực quan
    zoom = 4
    prev = Image.new("RGBA", (72 * 4 * zoom, 72 * zoom), (40, 52, 62, 255))
    pd = ImageDraw.Draw(prev)
    for y in range(0, prev.height, 16):
        for x in range(0, prev.width, 16):
            if ((x // 16) + (y // 16)) % 2 == 0:
                pd.rectangle([x, y, x + 15, y + 15], fill=(54, 68, 80, 255))
    for i, fr in enumerate(frames_out):
        big = fr.resize((72 * zoom, 72 * zoom), Image.Resampling.NEAREST)
        prev.alpha_composite(big, (i * 72 * zoom, 0))
        pd.line([(i * 72 * zoom, 0), (i * 72 * zoom, 72 * zoom)], fill=(255, 220, 80, 180), width=2)
    prev.save(PREVIEW_PATH)
    print("Saved preview:", PREVIEW_PATH)


if __name__ == "__main__":
    main()
