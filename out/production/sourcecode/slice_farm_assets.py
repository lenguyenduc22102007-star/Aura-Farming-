import os
import shutil
from PIL import Image

def is_magenta_bg(r, g, b, a):
    if a < 10:
        return True
    if r >= 165 and b >= 165 and g <= 130 and (r - g) >= 55 and (b - g) >= 55:
        return True
    if r >= 130 and b >= 130 and g <= 95 and (r - g) >= 48 and (b - g) >= 48:
        return True
    return False

def clean_magenta(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if is_magenta_bg(r, g, b, a):
                px[x, y] = (0, 0, 0, 0)

    # Pass 2: remove pinkish anti-alias fringe pixels touching transparency
    for _ in range(2):
        to_clear = []
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                if r > g + 30 and b > g + 30 and r > 110 and b > 110:
                    touches_bg = False
                    for dy in (-1, 0, 1):
                        ny = y + dy
                        if ny < 0 or ny >= h:
                            continue
                        for dx in (-1, 0, 1):
                            nx = x + dx
                            if 0 <= nx < w and px[nx, ny][3] == 0:
                                touches_bg = True
                                break
                        if touches_bg:
                            break
                    if touches_bg:
                        to_clear.append((x, y))
        if not to_clear:
            break
        for x, y in to_clear:
            px[x, y] = (0, 0, 0, 0)
    return im

def find_5_prop_boxes(im: Image.Image):
    """Detect the 5 prop columns cleanly, merging sub-clusters (like individual wildflowers) inside the 5th zone."""
    w, h = im.size
    px = im.load()
    col_has = [False] * w
    for x in range(w):
        cnt = 0
        for y in range(0, h, 4):
            if px[x, y][3] > 20:
                cnt += 1
        if cnt > 3:
            col_has[x] = True
    raw_segs = []
    in_seg = False
    start = 0
    for x, val in enumerate(col_has):
        if val and not in_seg:
            in_seg = True
            start = x
        elif not val and in_seg:
            in_seg = False
            if x - start >= 12:
                raw_segs.append((start, x))
    if in_seg and w - start >= 12:
        raw_segs.append((start, w))

    # Merge tiny gaps (< 12px) first; if > 5 segments (wildflowers sub-clusters), merge the closest adjacent segments
    merged = []
    for s, e in raw_segs:
        if merged and (s - merged[-1][1]) < 12:
            merged[-1] = (merged[-1][0], e)
        else:
            merged.append((s, e))

    while len(merged) > 5:
        # Merge the two closest adjacent segments (almost always the wildflower sub-flowers on the right)
        best_i = min(range(len(merged) - 1), key=lambda i: merged[i + 1][0] - merged[i][1])
        merged[best_i] = (merged[best_i][0], merged[best_i + 1][1])
        merged.pop(best_i + 1)

    if len(merged) == 5:
        return merged
    fw = w / 5.0
    return [(int(round(i * fw)), int(round((i + 1) * fw))) for i in range(5)]

def fit_nearest_on_canvas(cropped: Image.Image, target_w: int, target_h: int, anchor_bottom: bool = True) -> Image.Image:
    cw, ch = cropped.size
    scale = min(target_w / float(cw), target_h / float(ch))
    nw = max(1, int(round(cw * scale)))
    nh = max(1, int(round(ch * scale)))
    resized = cropped.resize((nw, nh), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    px = (target_w - nw) // 2
    py = (target_h - nh) if anchor_bottom else ((target_h - nh) // 2)
    canvas.paste(resized, (px, py), resized)
    return canvas

def main():
    props_dir = os.path.join("view", "assets", "props")
    tiled_obj_dir = os.path.join("view", "assets", "tiled", "objects")
    os.makedirs(props_dir, exist_ok=True)
    os.makedirs(tiled_obj_dir, exist_ok=True)

    # 1. Process sheet_farm_gate.png (crop above blurred bottom floor y=1720)
    gate_raw = Image.open("sheet_farm_gate.png").convert("RGBA")
    gate_raw = gate_raw.crop((0, 0, gate_raw.size[0], 1720))
    gate_im = clean_magenta(gate_raw)
    gbbox = gate_im.getbbox()
    if gbbox is None:
        raise RuntimeError("sheet_farm_gate.png is empty!")
    gate_crop = gate_im.crop(gbbox)
    gw, gh = gate_crop.size
    out_gw = 144
    out_gh = max(1, int(round(gh * (out_gw / float(gw)))))
    gate_final = gate_crop.resize((out_gw, out_gh), Image.Resampling.NEAREST)
    gate_path = os.path.join(props_dir, "farm_gate.png")
    gate_final.save(gate_path)
    shutil.copy2(gate_path, os.path.join(tiled_obj_dir, "farm_gate.png"))
    print(f"sheet_farm_gate.png: bbox={gbbox}, crop={gw}x{gh} -> saved {out_gw}x{out_gh}")

    # 2. Process sheet_farm_props.png (crop above blurred bottom floor y=1055)
    props_raw = Image.open("sheet_farm_props.png").convert("RGBA")
    props_raw = props_raw.crop((0, 0, props_raw.size[0], 1055))
    props_im = clean_magenta(props_raw)
    segs = find_5_prop_boxes(props_im)
    print(f"sheet_farm_props.png: 5 segs={segs}")

    specs = [
        ("farm_plot.png",    96, 96, False),  # 1. Luống đất có 4 cọc gỗ (cho cụm 2x2 hoặc ô trồng cây)
        ("fence_wood.png",   48, 48, True),   # 2. Đoạn hàng rào gỗ ngang
        ("lantern_post.png", 48, 72, True),   # 3. Cột đèn lồng gỗ
        ("garden_rock.png",  48, 48, True),   # 4. Cụm đá vườn
        ("wildflowers.png",  48, 48, True),   # 5. Cụm hoa dại & cỏ 3 lá
    ]

    for idx, (fname, tw, th, anchor_bot) in enumerate(specs):
        x0, x1 = segs[idx]
        sub = props_im.crop((x0, 0, x1, props_im.size[1]))
        bbox = sub.getbbox()
        if bbox is None:
            raise RuntimeError(f"Prop {idx} ({fname}) is empty!")
        cropped = sub.crop(bbox)
        if fname == "farm_plot.png":
            final_im = cropped.resize((tw, th), Image.Resampling.NEAREST)
        else:
            final_im = fit_nearest_on_canvas(cropped, tw, th, anchor_bottom=anchor_bot)
        out_path = os.path.join(props_dir, fname)
        final_im.save(out_path)
        shutil.copy2(out_path, os.path.join(tiled_obj_dir, fname))
        print(f"  prop {idx} ({fname}): seg=({x0}..{x1}), bbox={bbox}, size={cropped.size} -> {tw}x{th}")

    # Also sync fence_wood.png -> fence_h.png so existing TMX fences around farm automatically match!
    shutil.copy2(os.path.join(props_dir, "fence_wood.png"), os.path.join(tiled_obj_dir, "fence_h.png"))
    print("ALL_FARM_ASSETS_CLEAN_OK")

if __name__ == "__main__":
    main()
