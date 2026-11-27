import os
import shutil
from collections import deque
from PIL import Image

SRC_IMG = r"C:\Users\Dell7560\.gemini\antigravity\brain\a580a67f-c137-4355-a2ff-d3edc761c237\.user_uploaded\media_1795756048810_90a1a144.jpg"

def is_magenta_bg(r, g, b, a):
    if a < 10:
        return True
    if r >= 160 and b >= 150 and g <= 125 and (r - g) >= 48 and (b - g) >= 42:
        return True
    if r >= 120 and b >= 120 and g <= 85 and (r - g) >= 40 and (b - g) >= 40:
        return True
    return False

def clean_magenta(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()

    # Erase bottom-right sparkle watermark
    for y in range(int(h * 0.85), h):
        for x in range(int(w * 0.90), w):
            px[x, y] = (0, 0, 0, 0)

    # Flood-fill from borders + interior magenta pockets
    for y in range(h):
        for x in range(w):
            if is_magenta_bg(*px[x, y]):
                px[x, y] = (0, 0, 0, 0)

    # 2 passes of pinkish edge despill
    for _ in range(2):
        to_clear = []
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                if r > g + 26 and b > g + 26 and r > 100 and b > 100:
                    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] == 0:
                            to_clear.append((x, y))
                            break
        if not to_clear:
            break
        for x, y in to_clear:
            px[x, y] = (0, 0, 0, 0)
    return im

def find_valley(col_cnt, lo, hi):
    best_x = lo
    best_v = col_cnt[lo]
    for x in range(lo, hi + 1):
        if col_cnt[x] < best_v:
            best_v = col_cnt[x]
            best_x = x
    return best_x

def recolor_apples_to_oranges(im: Image.Image) -> Image.Image:
    out = im.copy()
    w, h = out.size
    px = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                continue
            # Detect bright red apple pixels (high R, much lower G & B)
            if r >= 135 and r > g + 45 and r > b + 45 and g < 115:
                nr = min(255, int(r * 1.06))
                ng = min(210, int(g * 1.45 + 72))
                nb = max(10, int(b * 0.55))
                px[x, y] = (nr, ng, nb, a)
    return out

def main():
    out_dir = os.path.join("view", "assets", "props")
    out_prod = os.path.join("out", "production", "sourcecode", "view", "assets", "props")
    os.makedirs(out_dir, exist_ok=True)
    os.makedirs(out_prod, exist_ok=True)

    im = clean_magenta(Image.open(SRC_IMG))
    w, h = im.size
    px = im.load()
    col_cnt = [sum(1 for y in range(h) if px[x, y][3] > 20) for x in range(w)]

    # Find exact vertical valleys separating the 4 trees:
    # Stage 0 (~0..17%), Stage 1 (~17..37%), Stage 2 (~37..65%), Stage 3 (~65..100%)
    cut1 = find_valley(col_cnt, int(w * 0.14), int(w * 0.20))
    cut2 = find_valley(col_cnt, int(w * 0.34), int(w * 0.40))
    cut3 = find_valley(col_cnt, int(w * 0.61), int(w * 0.67))
    cuts = [(0, cut1), (cut1, cut2), (cut2, cut3), (cut3, w)]
    print(f"Image {w}x{h} -> valleys: cut1={cut1}, cut2={cut2}, cut3={cut3}")

    raw_crops = []
    max_w = 1
    max_h = 1
    for i, (x0, x1) in enumerate(cuts):
        sub = im.crop((x0, 0, x1, h))
        bb = sub.getbbox()
        if bb is None:
            raise RuntimeError(f"Stage {i} is empty!")
        cr = sub.crop(bb)
        max_w = max(max_w, cr.size[0])
        max_h = max(max_h, cr.size[1])
        raw_crops.append(cr)
        print(f"  stage {i}: x=({x0}..{x1}), bbox={bb}, size={cr.size}")

    # Uniform scale across all 4 stages on 128x128 canvas anchored at bottom-center!
    # That way stage 0 (sapling) -> stage 1 (young) -> stage 2 (mature) -> stage 3 (harvest) grow naturally!
    target = 128
    pad = 2
    scale = min((target - pad * 2) / float(max_w), (target - pad * 2) / float(max_h))

    for i, cr in enumerate(raw_crops):
        nw = max(1, int(round(cr.size[0] * scale)))
        nh = max(1, int(round(cr.size[1] * scale)))
        res = cr.resize((nw, nh), Image.Resampling.NEAREST)
        canvas = Image.new("RGBA", (target, target), (0, 0, 0, 0))
        paste_x = (target - nw) // 2
        paste_y = target - pad - nh
        canvas.paste(res, (paste_x, paste_y), res)

        p_out = os.path.join(out_dir, f"apple_tree_{i}.png")
        canvas.save(p_out)
        shutil.copy2(p_out, os.path.join(out_prod, f"apple_tree_{i}.png"))

        if i == 3:
            orange_canvas = recolor_apples_to_oranges(canvas)
            o_out = os.path.join(out_dir, "orange_tree_3.png")
            orange_canvas.save(o_out)
            shutil.copy2(o_out, os.path.join(out_prod, "orange_tree_3.png"))

    print("ALL_FRUIT_TREE_STAGES_SLICED_OK")

if __name__ == "__main__":
    main()
