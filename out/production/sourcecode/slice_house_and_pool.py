import os
import shutil
from collections import deque
from PIL import Image

def is_bg_pink(r, g, b, a):
    if a < 10:
        return True
    # Pure magenta #FF00FF + purplish/pinkish haze around edges/shadows
    if r >= 150 and b >= 145 and g <= 125 and (r - g) >= 42 and (b - g) >= 38:
        return True
    if r >= 115 and b >= 115 and g <= 85 and (r - g) >= 36 and (b - g) >= 36:
        return True
    return False

def clean_magenta_flood(im: Image.Image, strip_wm: bool = True) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()

    if strip_wm:
        wm_x0 = int(w * 0.88)
        wm_y0 = int(h * 0.86)
        for y in range(wm_y0, h):
            for x in range(wm_x0, w):
                px[x, y] = (0, 0, 0, 0)

    # Flood-fill from image border so any pinkish ground shadow connected to background is cleanly removed
    visited = bytearray(w * h)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if is_bg_pink(*px[x, y]):
                idx = y * w + x
                if not visited[idx]:
                    visited[idx] = 1
                    q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if is_bg_pink(*px[x, y]):
                idx = y * w + x
                if not visited[idx]:
                    visited[idx] = 1
                    q.append((x, y))

    while q:
        x, y = q.popleft()
        px[x, y] = (0, 0, 0, 0)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h:
                idx = ny * w + nx
                if not visited[idx]:
                    r, g, b, a = px[nx, ny]
                    # Slightly wider tolerance for soft purple shadow connected to exterior magenta
                    if is_bg_pink(r, g, b, a) or (r > g + 22 and b > g + 22 and r > 90 and b > 90):
                        visited[idx] = 1
                        q.append((nx, ny))

    # Also remove any interior enclosed magenta pockets (e.g. under palm fronds or chair arms)
    for y in range(h):
        for x in range(w):
            if is_bg_pink(*px[x, y]):
                px[x, y] = (0, 0, 0, 0)

    # 1-px pinkish fringe despill pass
    to_clear = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if r > g + 28 and b > g + 28 and r > 105 and b > 105:
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] == 0:
                        to_clear.append((x, y))
                        break
    for x, y in to_clear:
        px[x, y] = (0, 0, 0, 0)
    return im

def find_n_islands(im: Image.Image, expected: int = 4):
    w, h = im.size
    px = im.load()
    col_has = [False] * w
    for x in range(w):
        cnt = 0
        for y in range(0, h, 4):
            if px[x, y][3] > 20:
                cnt += 1
        if cnt > 4:
            col_has[x] = True
    segs = []
    in_seg = False
    start = 0
    for x, val in enumerate(col_has):
        if val and not in_seg:
            in_seg = True
            start = x
        elif not val and in_seg:
            in_seg = False
            if x - start >= 24:
                segs.append((start, x))
    if in_seg and w - start >= 24:
        segs.append((start, w))
    while len(segs) > expected:
        best_i = min(range(len(segs) - 1), key=lambda i: segs[i + 1][0] - segs[i][1])
        segs[best_i] = (segs[best_i][0], segs[best_i + 1][1])
        segs.pop(best_i + 1)
    if len(segs) == expected:
        return segs
    fw = w / float(expected)
    return [(int(round(i * fw)), int(round((i + 1) * fw))) for i in range(expected)]

def fit_nearest(cropped: Image.Image, tw: int, th: int, anchor_bottom: bool = True) -> Image.Image:
    cw, ch = cropped.size
    s = min(tw / float(cw), th / float(ch))
    nw = max(1, int(round(cw * s)))
    nh = max(1, int(round(ch * s)))
    res = cropped.resize((nw, nh), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    px = (tw - nw) // 2
    py = (th - nh) if anchor_bottom else ((th - nh) // 2)
    canvas.paste(res, (px, py), res)
    return canvas

def main():
    props_dir = os.path.join("view", "assets", "props")
    tiled_obj_dir = os.path.join("view", "assets", "tiled", "objects")
    os.makedirs(props_dir, exist_ok=True)
    os.makedirs(tiled_obj_dir, exist_ok=True)

    # 1. sheet_main_house.png -> main_house.png (width = 224px, keep aspect ratio, NEAREST)
    house_im = clean_magenta_flood(Image.open("sheet_main_house.png"), strip_wm=True)
    hbbox = house_im.getbbox()
    if hbbox is None:
        raise RuntimeError("sheet_main_house.png is empty!")
    house_crop = house_im.crop(hbbox)
    hw, hh = house_crop.size
    out_hw = 224
    out_hh = max(1, int(round(hh * (out_hw / float(hw)))))
    house_final = house_crop.resize((out_hw, out_hh), Image.Resampling.NEAREST)
    house_path = os.path.join(props_dir, "main_house.png")
    house_final.save(house_path)
    shutil.copy2(house_path, os.path.join(tiled_obj_dir, "main_house.png"))
    print(f"sheet_main_house.png: bbox={hbbox}, crop={hw}x{hh} -> saved {out_hw}x{out_hh}")

    # 2. sheet_pool_resort.png (crop 4 clean regions above bottom blurred strip at y=1315)
    pool_raw = Image.open("sheet_pool_resort.png").convert("RGBA")
    pool_im = clean_magenta_flood(pool_raw.crop((0, 0, pool_raw.size[0], 1315)), strip_wm=False)

    # Prop 0: pool_water.png (preserve natural ratio, 168x238 fits PoolZone cleanly without overlapping OrchardZone)
    p0 = pool_im.crop((80, 240, 880, 1305))
    b0 = p0.getbbox()
    p0_crop = p0.crop(b0)
    pw_out = 168
    ph_out = max(1, int(round(p0_crop.size[1] * (pw_out / float(p0_crop.size[0])))))
    p0_final = p0_crop.resize((pw_out, ph_out), Image.Resampling.NEAREST)
    p0_final.save(os.path.join(props_dir, "pool_water.png"))
    shutil.copy2(os.path.join(props_dir, "pool_water.png"), os.path.join(tiled_obj_dir, "swimming_pool.png"))
    print(f"  prop 0 (pool_water.png): crop={p0_crop.size} -> {pw_out}x{ph_out}")

    # Prop 1..3: pool_chair.png (52x72), pool_umbrella.png (72x72), palm_tree.png (96x120)
    rest = [
        ((910, 440, 1330, 1240), "pool_chair.png",    "pool_lounger.png",  52, 72),
        ((1340, 420, 1980, 1200), "pool_umbrella.png", "pool_umbrella.png", 72, 72),
        ((1930, 60, 2745, 1315), "palm_tree.png",     "palm_tree.png",     96, 120),
    ]
    for idx, (box, prop_fn, tiled_fn, tw, th) in enumerate(rest, start=1):
        sub = pool_im.crop(box)
        bb = sub.getbbox()
        cr = sub.crop(bb)
        fin = fit_nearest(cr, tw, th, anchor_bottom=True)
        fin.save(os.path.join(props_dir, prop_fn))
        shutil.copy2(os.path.join(props_dir, prop_fn), os.path.join(tiled_obj_dir, tiled_fn))
        print(f"  prop {idx} ({prop_fn}): crop={cr.size} -> {tw}x{th}")

    print("ALL_HOUSE_AND_POOL_SLICED_OK")

if __name__ == "__main__":
    main()
