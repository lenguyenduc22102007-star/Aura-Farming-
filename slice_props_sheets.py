import os
from PIL import Image

def is_magenta_bg(r, g, b, a):
    if a < 10:
        return True
    if r >= 170 and b >= 170 and g <= 125 and (r - g) >= 65 and (b - g) >= 65:
        return True
    if r >= 135 and b >= 135 and g <= 95 and (r - g) >= 55 and (b - g) >= 55:
        return True
    return False

def clean_magenta_and_watermark(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()
    # Erase bottom-right AI sparkle watermark region before bbox detection
    wm_x0 = int(w * 0.90)
    wm_y0 = int(h * 0.84)
    for y in range(wm_y0, h):
        for x in range(wm_x0, w):
            px[x, y] = (0, 0, 0, 0)

    # Pass 1: remove magenta background & halo
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if is_magenta_bg(r, g, b, a):
                px[x, y] = (0, 0, 0, 0)

    # Pass 2: remove pinkish fringe pixels touching transparency
    to_clear = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if r > g + 35 and b > g + 35 and r > 120 and b > 120:
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
    for x, y in to_clear:
        px[x, y] = (0, 0, 0, 0)
    return im

def find_column_segments(im: Image.Image, expected: int = 6):
    w, h = im.size
    px = im.load()
    col_has = [False] * w
    for x in range(w):
        for y in range(0, h, 2):
            if px[x, y][3] > 20:
                col_has[x] = True
                break
    segs = []
    in_seg = False
    start = 0
    for x, val in enumerate(col_has):
        if val and not in_seg:
            in_seg = True
            start = x
        elif not val and in_seg:
            in_seg = False
            if x - start >= 30:
                segs.append((start, x))
    if in_seg and w - start >= 30:
        segs.append((start, w))

    if len(segs) == expected:
        return segs
    # Fallback: equal 6-column division
    fw = w / float(expected)
    return [(int(round(i * fw)), int(round((i + 1) * fw))) for i in range(expected)]

def find_pole_anchor_x(cropped: Image.Image) -> float:
    """Find average X of the flagpole near the bottom 18% of the sprite so the pole never jitters."""
    w, h = cropped.size
    px = cropped.load()
    xs = []
    y_start = int(h * 0.80)
    for y in range(y_start, h):
        for x in range(w):
            if px[x, y][3] > 20:
                xs.append(x)
    return (sum(xs) / len(xs)) if xs else (w * 0.5)

def slice_6_frames(src_path: str, out_dir: str, prefix: str, target_w: int, target_h: int, anchor_pole: bool = False):
    os.makedirs(out_dir, exist_ok=True)
    im = clean_magenta_and_watermark(Image.open(src_path))
    w, h = im.size
    segs = find_column_segments(im, 6)
    print(f"Processing {src_path} ({w}x{h}) -> {len(segs)} segs: {segs}")

    raw_crops = []
    max_cw = 1
    max_ch = 1
    for i, (x0, x1) in enumerate(segs):
        sub = im.crop((x0, 0, x1, h))
        bbox = sub.getbbox()
        if bbox is None:
            raise RuntimeError(f"Frame {i} in {src_path} is empty!")
        cropped = sub.crop(bbox)
        cw, ch = cropped.size
        max_cw = max(max_cw, cw)
        max_ch = max(max_ch, ch)
        raw_crops.append(cropped)
        print(f"  frame {i}: seg=({x0}..{x1}), bbox={bbox}, size={cw}x{ch}")

    pad_x = 2
    pad_top = 2
    pad_bot = 1
    avail_w = target_w - pad_x * 2
    avail_h = target_h - pad_top - pad_bot
    scale = min(avail_w / max_cw, avail_h / max_ch)

    # For flag, compute target X where the pole should sit (left-biased if flag waves right)
    pole_ratios = [find_pole_anchor_x(c) / max(1, c.size[0]) for c in raw_crops]
    pole_anchor_target_x = int(round(target_w * 0.28)) if (anchor_pole and sum(pole_ratios) / 6.0 < 0.42) else (target_w // 2)

    for i, cropped in enumerate(raw_crops):
        cw, ch = cropped.size
        nw = max(1, int(round(cw * scale)))
        nh = max(1, int(round(ch * scale)))
        resized = cropped.resize((nw, nh), Image.Resampling.NEAREST)

        canvas = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
        if anchor_pole:
            pole_x_in_resized = int(round(find_pole_anchor_x(cropped) * scale))
            paste_x = max(1, min(target_w - nw - 1, pole_anchor_target_x - pole_x_in_resized))
        else:
            paste_x = (target_w - nw) // 2
        paste_y = target_h - pad_bot - nh
        canvas.paste(resized, (paste_x, paste_y), resized)

        out_path = os.path.join(out_dir, f"{prefix}_{i}.png")
        canvas.save(out_path)
    print(f"  Saved 6 frames to {out_dir}/{prefix}_0..5.png")

if __name__ == "__main__":
    out_dir = os.path.join("view", "assets", "props")
    slice_6_frames("sheet_flag_wave.png", out_dir, "flag", 48, 64, anchor_pole=True)
    slice_6_frames("sheet_campfire_anim.png", out_dir, "campfire", 48, 48, anchor_pole=False)
    print("ALL_PROPS_SLICED_CLEAN_OK")
