import os
import shutil
import xml.etree.ElementTree as ET

"""
Quy hoạch lại mặt bằng FarmZone trong view/assets/tiled/level1.tmx thành lưới luống 2x2 gọn gàng,
ngăn cách nhau bởi 1 hàng/cột cỏ xanh (gid=1) làm lối đi ở giữa — đồng thời bảo toàn 100% toàn bộ
bản đồ 40x52, con sông, cây cầu, nhà cửa, đống lửa, NPC Zoro, Pet Shop và 6 GameplayZones hiện có.
"""

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
TMX_PATH = os.path.join(ROOT_DIR, "view", "assets", "tiled", "level1.tmx")
OUT_TMX_PATH = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "tiled", "level1.tmx")

# Theo terrain_atlas.png & FarmMap.java: gid 1 = GRASS (cỏ xanh), gid 3 = DIRT (đất trồng)
GID_GRASS = 1
GID_DIRT = 3


def reorganize_farm_zone(tmx_path: str) -> None:
    tree = ET.parse(tmx_path)
    root = tree.getroot()

    width = int(root.attrib["width"])
    height = int(root.attrib["height"])
    tile_w = int(root.attrib.get("tilewidth", "40"))
    tile_h = int(root.attrib.get("tileheight", "40"))

    # Đọc chính xác khung FarmZone từ GameplayZones (fallback: rows 20..30, cols 2..16)
    fz_r0, fz_r1 = 20, 30
    fz_c0, fz_c1 = 2, 16
    fz_x0, fz_y0, fz_x1, fz_y1 = 80.0, 800.0, 680.0, 1240.0
    for og in root.findall("objectgroup"):
        if og.attrib.get("name") == "GameplayZones":
            for obj in og.findall("object"):
                if obj.attrib.get("name") == "FarmZone":
                    fx = float(obj.attrib["x"])
                    fy = float(obj.attrib["y"])
                    fw = float(obj.attrib["width"])
                    fh = float(obj.attrib["height"])
                    fz_x0, fz_y0, fz_x1, fz_y1 = fx, fy, fx + fw, fy + fh
                    # Vùng các ô nằm trọn bên trong hàng rào FarmZone (3 hàng x 4 cột luống 2x2)
                    fz_c0 = max(0, int((fx + tile_w * 0.5) // tile_w))
                    fz_c1 = min(width - 1, int((fx + fw - tile_w * 0.5) // tile_w))
                    fz_r0 = max(0, int((fy + tile_h * 0.1) // tile_h))
                    fz_r1 = min(height - 1, int((fy + fh - tile_h * 0.1) // tile_h))
                    break

    # 1. Cập nhật lớp Terrain: chia thành lưới 3 hàng x 4 cột các luống đất vuông 2x2 viền cỏ 1 ô
    terrain_layer = root.find("layer")
    data_elem = terrain_layer.find("data")
    tile_elems = data_elem.findall("tile")

    dirt_count = 0
    grass_walk_count = 0
    if tile_elems:
        for r in range(fz_r0, fz_r1 + 1):
            dr = r - fz_r0
            in_bed_row = (dr % 3) < 2 and (dr // 3) < 3
            for c in range(fz_c0, fz_c1 + 1):
                dc = c - fz_c0
                in_bed_col = (dc % 3) < 2 and (dc // 3) < 4
                idx = r * width + c
                cur_gid = int(tile_elems[idx].attrib.get("gid", "1"))
                # Không đè lên đường đá (5..10, 13..22) hoặc nước (11..12)
                if cur_gid in (11, 12) or (5 <= cur_gid <= 10) or (13 <= cur_gid <= 22):
                    continue
                if in_bed_row and in_bed_col:
                    tile_elems[idx].attrib["gid"] = str(GID_DIRT)
                    dirt_count += 1
                else:
                    tile_elems[idx].attrib["gid"] = str(GID_GRASS)
                    grass_walk_count += 1
    else:
        # Hỗ trợ cả định dạng CSV nếu file TMX dùng CSV
        raw = (data_elem.text or "").strip()
        rows = [[int(v) for v in ln.strip().rstrip(",").split(",") if v.strip()]
                for ln in raw.splitlines() if ln.strip()]
        for r in range(fz_r0, min(fz_r1 + 1, len(rows))):
            dr = r - fz_r0
            in_bed_row = (dr % 3) < 2 and (dr // 3) < 3
            for c in range(fz_c0, min(fz_c1 + 1, len(rows[r]))):
                dc = c - fz_c0
                in_bed_col = (dc % 3) < 2 and (dc // 3) < 4
                if in_bed_row and in_bed_col:
                    rows[r][c] = GID_DIRT
                    dirt_count += 1
                else:
                    rows[r][c] = GID_GRASS
                    grass_walk_count += 1
        data_elem.text = "\n" + ",\n".join(",".join(map(str, row)) for row in rows) + "\n"

    # 2. Dọn sạch các cụm wheat_patch phủ kín cũ bên trong FarmZone để lộ rõ 12 luống đất 2x2 viền cỏ xanh
    removed_wheat = 0
    for og in root.findall("objectgroup"):
        if og.attrib.get("name") == "Objects":
            for obj in list(og.findall("object")):
                name = (obj.attrib.get("name") or "").lower()
                if "wheat" in name:
                    ox = float(obj.attrib.get("x", "0"))
                    oy = float(obj.attrib.get("y", "0"))
                    if fz_x0 <= ox <= fz_x1 and fz_y0 <= oy <= fz_y1:
                        og.remove(obj)
                        removed_wheat += 1

    tree.write(tmx_path, encoding="utf-8", xml_declaration=True)
    if os.path.isdir(os.path.dirname(OUT_TMX_PATH)):
        shutil.copy2(tmx_path, OUT_TMX_PATH)

    print(
        f"Updated {tmx_path}: FarmZone rows {fz_r0}..{fz_r1}, cols {fz_c0}..{fz_c1} -> "
        f"12 plots (3x4 grid of 2x2 dirt = {dirt_count} dirt tiles, {grass_walk_count} grass walkway tiles, "
        f"cleared {removed_wheat} old wheat_patch decals)."
    )


if __name__ == "__main__":
    reorganize_farm_zone(TMX_PATH)
