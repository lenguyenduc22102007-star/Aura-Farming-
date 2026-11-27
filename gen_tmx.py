import os
import shutil
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

"""
Quy hoạch mặt bằng level1.tmx:
1. Khu FarmZone: 12 luống đất 2x2 ngăn cách bởi lối đi cỏ xanh 1 ô.
2. Khu bên kia cầu (phía Nam / Đông-Tây đại lộ):
   - Pool Zone (Bể bơi nghỉ dưỡng): Hồ bơi hình chữ nhật viền gạch trắng/xanh nước trong vắt,
     3 ghế dài tắm nắng, 2 dù che nắng rực rỡ và 4 cây dừa nhiệt đới.
   - Casino (Sòng Bạc): Tòa nhà Casino lớn với mái vòm nhung đỏ, cột vàng, biển hiệu đèn vàng/đỏ và thảm đỏ cửa vào.
"""

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
TMX_PATH = os.path.join(ROOT_DIR, "view", "assets", "tiled", "level1.tmx")
TSX_OBJECTS_PATH = os.path.join(ROOT_DIR, "view", "assets", "tiled", "objects.tsx")
OBJ_IMG_DIR = os.path.join(ROOT_DIR, "view", "assets", "tiled", "objects")
OUT_TILED_DIR = os.path.join(ROOT_DIR, "out", "production", "sourcecode", "view", "assets", "tiled")

GID_GRASS = 1
GID_DIRT = 3
GID_PATH = 5


def generate_resort_and_casino_sprites() -> dict:
    """Tạo bộ ảnh pixel-art sắc nét cho Bể bơi, Ghế tắm nắng, Dù bãi biển, Cây dừa và Sòng bạc Casino."""
    os.makedirs(OBJ_IMG_DIR, exist_ok=True)

    # 1. swimming_pool.png (120x70 px -> phóng NEAREST x2 thành 240x140)
    pw, ph = 120, 70
    pool = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    d = ImageDraw.Draw(pool)
    # Bóng đổ nhẹ bên ngoài thành bể
    d.rounded_rectangle([2, 4, pw - 1, ph - 1], radius=7, fill=(28, 52, 68, 120))
    # Thành gạch trắng kem bên ngoài
    d.rounded_rectangle([0, 0, pw - 3, ph - 3], radius=6, fill=(242, 247, 252, 255), outline=(42, 68, 92, 255), width=2)
    # Đường gạch xanh ngọc viền quanh hồ + kẻ ô gạch lát
    d.rounded_rectangle([5, 5, pw - 8, ph - 8], radius=4, fill=(112, 206, 242, 255), outline=(56, 138, 186, 255), width=1)
    for gx in range(8, pw - 8, 8):
        d.line([(gx, 1), (gx, 5)], fill=(204, 220, 234, 255))
        d.line([(gx, ph - 8), (gx, ph - 4)], fill=(204, 220, 234, 255))
    for gy in range(8, ph - 8, 8):
        d.line([(1, gy), (5, gy)], fill=(204, 220, 234, 255))
        d.line([(pw - 8, gy), (pw - 4, gy)], fill=(204, 220, 234, 255))
    # Lòng hồ nước xanh dương trong vắt phân tầng độ sâu
    d.rectangle([8, 8, pw - 11, ph - 11], fill=(26, 168, 236, 255))
    d.rectangle([8, 8, pw - 11, 14], fill=(18, 132, 202, 255))  # Thành trong tạo chiều sâu 3D
    d.rectangle([12, 16, pw - 15, ph - 15], fill=(48, 196, 250, 255))
    # Vân caustics sáng đáy hồ
    for rx, ry, rw in [(18, 22, 18), (52, 28, 22), (82, 20, 16), (28, 44, 24), (68, 46, 20)]:
        d.line([(rx, ry), (rx + rw, ry)], fill=(158, 238, 255, 220), width=2)
        d.line([(rx + 4, ry + 3), (rx + rw - 3, ry + 3)], fill=(220, 252, 255, 190), width=1)
    # Thang inox góc trái hồ bơi
    d.rectangle([16, 3, 18, 16], fill=(210, 224, 236, 255))
    d.rectangle([24, 3, 26, 16], fill=(210, 224, 236, 255))
    d.line([(17, 8), (25, 8)], fill=(245, 250, 255, 255))
    d.line([(17, 12), (25, 12)], fill=(245, 250, 255, 255))
    pool_big = pool.resize((240, 140), Image.Resampling.NEAREST)
    pool_big.save(os.path.join(OBJ_IMG_DIR, "swimming_pool.png"))

    # 2. pool_lounger.png (30x19 px -> 60x38: ghế dài tắm nắng gỗ tếch + nệm sọc trắng xanh/cam)
    lw, lh = 30, 19
    lounger = Image.new("RGBA", (lw, lh), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lounger)
    ld.rounded_rectangle([1, 12, lw - 2, lh - 1], radius=2, fill=(24, 38, 24, 95))  # bóng
    # Khung gỗ nâu ấm & chân ghế
    ld.rectangle([3, 13, 6, 17], fill=(98, 56, 26, 255))
    ld.rectangle([lw - 7, 13, lw - 4, 17], fill=(98, 56, 26, 255))
    ld.rectangle([2, 8, lw - 3, 13], fill=(146, 88, 42, 255), outline=(58, 30, 12, 255))
    # Tựa đầu nâng cao bên trái + nệm trắng sọc kem/cam
    ld.polygon([(2, 8), (9, 3), (11, 8)], fill=(248, 244, 232, 255), outline=(58, 30, 12, 255))
    ld.rectangle([9, 6, lw - 4, 11], fill=(252, 248, 238, 255), outline=(58, 30, 12, 255))
    for sx in range(11, lw - 5, 4):
        ld.rectangle([sx, 7, sx + 1, 10], fill=(56, 178, 232, 255))
    ld.rectangle([4, 5, 8, 8], fill=(255, 196, 84, 255))  # gối nằm vàng êm
    lounger_big = lounger.resize((60, 38), Image.Resampling.NEAREST)
    lounger_big.save(os.path.join(OBJ_IMG_DIR, "pool_lounger.png"))

    # 3. pool_umbrella.png (38x40 px -> 76x80: dù che nắng 8 múi đỏ-trắng-vàng rực rỡ)
    uw, uh = 38, 40
    umb = Image.new("RGBA", (uw, uh), (0, 0, 0, 0))
    ud = ImageDraw.Draw(umb)
    ud.ellipse([6, 31, uw - 6, 39], fill=(20, 36, 24, 95))
    # Thân cột gỗ
    ud.rectangle([18, 14, 20, 36], fill=(132, 82, 40, 255), outline=(48, 26, 12, 255))
    # Tán dù sọc đỏ cam & kem trắng nổi bật
    ud.pieslice([2, 3, uw - 3, 28], start=180, end=360, fill=(238, 72, 58, 255), outline=(52, 22, 18, 255))
    ud.pieslice([8, 3, uw - 9, 28], start=180, end=360, fill=(255, 248, 232, 255))
    ud.pieslice([14, 3, uw - 15, 28], start=180, end=360, fill=(255, 184, 46, 255))
    ud.line([(2, 15), (uw - 3, 15)], fill=(52, 22, 18, 255))
    # Diềm lượn sóng mép dù
    for bx in range(3, uw - 5, 5):
        col = (238, 72, 58, 255) if ((bx // 5) % 2 == 0) else (255, 248, 232, 255)
        ud.rectangle([bx, 15, bx + 3, 17], fill=col)
    ud.rectangle([18, 1, 20, 3], fill=(255, 214, 68, 255))
    umb_big = umb.resize((76, 80), Image.Resampling.NEAREST)
    umb_big.save(os.path.join(OBJ_IMG_DIR, "pool_umbrella.png"))

    # 4. palm_tree.png (48x68 px -> 96x136: cây dừa nhiệt đới có quả dừa nâu và tán lá xanh mướt)
    tw, th = 48, 68
    palm = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    pd = ImageDraw.Draw(palm)
    pd.ellipse([13, 59, 35, 67], fill=(20, 38, 20, 95))
    # Thân cây dừa hơi cong có đốt vỏ dừa
    trunk_pts = [(22, 63), (21, 52), (22, 41), (24, 30), (25, 21)]
    for i in range(len(trunk_pts) - 1):
        x0, y0 = trunk_pts[i]
        x1, y1 = trunk_pts[i + 1]
        pd.line([(x0, y0), (x1, y1)], fill=(48, 28, 14, 255), width=7)
        pd.line([(x0, y0), (x1, y1)], fill=(154, 102, 56, 255), width=5)
        pd.line([(x0 - 1, y0), (x1 - 1, y1)], fill=(188, 134, 78, 255), width=2)
    for ry in range(25, 62, 5):
        pd.line([(19, ry), (26, ry)], fill=(104, 64, 32, 255), width=1)
    # Tán lá dừa xòe rộng nhiều lớp xanh đậm -> xanh tươi
    fronds = [
        [(25, 20), (10, 14), (2, 22), (14, 20)],
        [(25, 20), (39, 14), (46, 22), (35, 20)],
        [(25, 19), (12, 7), (4, 11), (16, 14)],
        [(25, 19), (37, 7), (44, 11), (33, 14)],
        [(25, 21), (13, 24), (6, 33), (18, 26)],
        [(25, 21), (36, 24), (43, 33), (31, 26)],
        [(25, 18), (20, 4), (25, 2), (29, 6)]
    ]
    for poly in fronds:
        pd.polygon(poly, fill=(44, 148, 52, 255), outline=(22, 68, 26, 255))
    for poly in fronds[:4]:
        mx = (poly[0][0] + poly[1][0]) // 2
        my = (poly[0][1] + poly[1][1]) // 2
        pd.line([poly[0], (mx, my)], fill=(116, 214, 88, 255), width=2)
    # Chùm 3 quả dừa nâu dưới tán lá
    for cx, cy in [(22, 21), (27, 22), (24, 24)]:
        pd.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(96, 54, 24, 255), outline=(38, 20, 8, 255))
    palm_big = palm.resize((96, 136), Image.Resampling.NEAREST)
    palm_big.save(os.path.join(OBJ_IMG_DIR, "palm_tree.png"))

    # 5. casino_building.png (120x98 px -> 240x196: Tòa nhà Sòng Bạc hoàng gia rực rỡ)
    cw, ch = 120, 98
    cas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cas)
    # Bóng nền & Bậc thềm đá hoa cương + Thảm đỏ hoàng gia
    cd.rounded_rectangle([4, 82, cw - 4, ch - 2], radius=4, fill=(24, 20, 18, 120))
    cd.rectangle([8, 32, cw - 9, 88], fill=(138, 32, 36, 255), outline=(42, 16, 14, 255), width=2)
    # Tầng mái vòm & diềm vàng kim
    cd.polygon([(4, 34), (16, 16), (cw - 17, 16), (cw - 5, 34)], fill=(86, 20, 28, 255), outline=(38, 12, 14, 255))
    cd.rectangle([6, 31, cw - 7, 36], fill=(242, 192, 62, 255), outline=(72, 42, 10, 255))
    # 4 cột trụ vàng chạm khắc mặt tiền
    for col_x in [14, 34, cw - 41, cw - 21]:
        cd.rectangle([col_x, 36, col_x + 6, 84], fill=(244, 208, 104, 255), outline=(92, 58, 14, 255))
        cd.rectangle([col_x + 1, 37, col_x + 2, 83], fill=(255, 242, 178, 255))
    # Ô cửa sổ vòm phát sáng vàng ấm hai cánh
    for wx in [23, cw - 32]:
        cd.rounded_rectangle([wx, 46, wx + 8, 66], radius=3, fill=(255, 224, 112, 255), outline=(64, 32, 12, 255))
        cd.line([(wx + 4, 46), (wx + 4, 66)], fill=(120, 68, 22, 255))
    # Biển hiệu lớn trên nóc (Bảng đen viền vàng đính bóng đèn đỏ/vàng + chữ CASINO pixel)
    cd.rounded_rectangle([22, 3, cw - 23, 25], radius=4, fill=(32, 14, 22, 255), outline=(255, 204, 56, 255), width=2)
    for bx in range(25, cw - 24, 6):
        bulb_c = (255, 232, 82, 255) if ((bx // 6) % 2 == 0) else (255, 76, 76, 255)
        cd.point((bx, 5), fill=bulb_c)
        cd.point((bx, 23), fill=bulb_c)
    # Vẽ chữ pixel "CASINO" sắc nét giữa biển hiệu (y=9..19)
    casino_font = {
        "C": ["1111", "1000", "1000", "1000", "1111"],
        "A": ["0110", "1001", "1111", "1001", "1001"],
        "S": ["1111", "1000", "1111", "0001", "1111"],
        "I": ["111", "010", "010", "010", "111"],
        "N": ["1001", "1101", "1011", "1001", "1001"],
        "O": ["1111", "1001", "1001", "1001", "1111"],
    }
    cursor_x = 32
    for ch_let in "CASINO":
        pat = casino_font[ch_let]
        for py_i, row_s in enumerate(pat):
            for px_i, bit in enumerate(row_s):
                if bit == "1":
                    cd.rectangle(
                        [cursor_x + px_i * 2, 9 + py_i * 2, cursor_x + px_i * 2 + 1, 9 + py_i * 2 + 1],
                        fill=(255, 222, 74, 255)
                    )
        cursor_x += len(pat[0]) * 2 + 2
    # Mái hiên sọc đỏ - trắng & Cửa đôi rực sáng ở chính giữa
    cd.rectangle([42, 48, cw - 43, 55], fill=(224, 48, 48, 255), outline=(58, 16, 16, 255))
    for ax in range(44, cw - 44, 6):
        cd.rectangle([ax, 49, ax + 2, 54], fill=(255, 248, 236, 255))
    cd.rectangle([46, 55, cw - 47, 86], fill=(54, 24, 14, 255), outline=(242, 196, 68, 255), width=2)
    cd.rectangle([49, 58, 58, 85], fill=(255, 214, 102, 255))
    cd.rectangle([61, 58, 70, 85], fill=(255, 214, 102, 255))
    # Thảm đỏ bậc tam cấp dẫn ra đường
    cd.polygon([(45, 86), (cw - 46, 86), (cw - 40, 96), (39, 96)], fill=(198, 28, 38, 255), outline=(242, 196, 68, 255))

    cas_big = cas.resize((240, 196), Image.Resampling.NEAREST)
    cas_big.save(os.path.join(OBJ_IMG_DIR, "casino_building.png"))

    # 6. ore_rock.png (32x28 px -> 64x56: Tảng đá mỏ quặng lấp lánh mạch vàng/đồng/đá quý)
    rw, rh = 32, 28
    rock = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rock)
    rd.ellipse([2, 19, rw - 3, rh - 1], fill=(20, 26, 20, 105))
    rd.polygon([(4, 22), (2, 14), (8, 5), (18, 3), (27, 7), (30, 16), (27, 23)],
               fill=(104, 110, 120, 255), outline=(34, 36, 42, 255))
    rd.polygon([(7, 20), (6, 13), (11, 7), (19, 6), (24, 11), (22, 19)],
               fill=(136, 144, 156, 255))
    # Mạch quặng vàng, đồng và tinh thể đá quý lấp lánh trên thân đá
    for ox, oy, oc in [
        (10, 11, (255, 212, 64, 255)), (11, 12, (255, 238, 128, 255)),
        (19, 10, (82, 226, 255, 255)), (20, 11, (186, 250, 255, 255)),
        (14, 16, (232, 132, 68, 255)), (15, 17, (255, 176, 108, 255)),
        (22, 16, (255, 206, 58, 255))
    ]:
        rd.rectangle([ox, oy, ox + 1, oy + 1], fill=oc)
    rock_big = rock.resize((64, 56), Image.Resampling.NEAREST)
    rock_big.save(os.path.join(OBJ_IMG_DIR, "ore_rock.png"))

    # 7. mine_anvil.png (32x28 px -> 64x56: Đe rèn đúc Vòi Tưới Tự Động tại cửa khu mỏ)
    aw, ah = 32, 28
    anv = Image.new("RGBA", (aw, ah), (0, 0, 0, 0))
    ad = ImageDraw.Draw(anv)
    ad.ellipse([4, 20, aw - 4, ah - 1], fill=(20, 24, 20, 110))
    ad.rectangle([8, 17, 23, 24], fill=(122, 74, 36, 255), outline=(42, 22, 10, 255))
    ad.polygon([(3, 10), (28, 10), (25, 14), (20, 14), (21, 17), (10, 17), (11, 14), (6, 14)],
               fill=(82, 90, 104, 255), outline=(28, 32, 38, 255))
    ad.line([(5, 11), (26, 11)], fill=(184, 198, 214, 255), width=1)
    anv_big = anv.resize((64, 56), Image.Resampling.NEAREST)
    anv_big.save(os.path.join(OBJ_IMG_DIR, "mine_anvil.png"))

    # Đồng bộ các asset tách từ sheet_farm_gate / sheet_farm_props / sheet_main_house / sheet_pool_resort vào objects/
    props_dir = os.path.join(ROOT_DIR, "view", "assets", "props")
    prop_copies = {
        "farm_gate.png":      "farm_gate.png",
        "farm_gate_side.png": "farm_gate_side.png",
        "farm_plot.png":     "farm_plot.png",
        "fence_wood.png":    "fence_wood.png",
        "lantern_post.png":      "lantern_post.png",
        "lantern_post_left.png": "lantern_post_left.png",
        "lantern_post_down.png": "lantern_post_down.png",
        "garden_rock.png":   "garden_rock.png",
        "wildflowers.png":   "wildflowers.png",
        "main_house.png":    "main_house.png",
        "pool_water.png":    "swimming_pool.png",
        "pool_chair.png":    "pool_lounger.png",
        "pool_umbrella.png": "pool_umbrella.png",
        "palm_tree.png":     "palm_tree.png",
    }
    for src_fn, dst_fn in prop_copies.items():
        src_p = os.path.join(props_dir, src_fn)
        if os.path.isfile(src_p):
            shutil.copy2(src_p, os.path.join(OBJ_IMG_DIR, dst_fn))
    fence_wood_src = os.path.join(props_dir, "fence_wood.png")
    if os.path.isfile(fence_wood_src):
        shutil.copy2(fence_wood_src, os.path.join(OBJ_IMG_DIR, "fence_h.png"))

    return {
        "swimming_pool": ("objects/swimming_pool.png", 168, 238),
        "pool_lounger": ("objects/pool_lounger.png", 52, 72),
        "pool_umbrella": ("objects/pool_umbrella.png", 72, 72),
        "palm_tree": ("objects/palm_tree.png", 96, 120),
        "casino_building": ("objects/casino_building.png", 240, 196),
        "ore_rock": ("objects/ore_rock.png", 64, 56),
        "mine_anvil": ("objects/mine_anvil.png", 64, 56),
        "farm_gate": ("objects/farm_gate.png", 144, 118),
        "farm_gate_side": ("objects/farm_gate_side.png", 80, 300),
        "fence_wood": ("objects/fence_wood.png", 48, 48),
        "lantern_post": ("objects/lantern_post.png", 48, 72),
        "lantern_post_left": ("objects/lantern_post_left.png", 48, 72),
        "lantern_post_down": ("objects/lantern_post_down.png", 48, 72),
        "garden_rock": ("objects/garden_rock.png", 48, 48),
        "wildflowers": ("objects/wildflowers.png", 48, 48),
    }


def register_objects_in_tsx(tsx_path: str, sprite_specs: dict, first_gid: int = 25) -> dict:
    """Đảm bảo 5 sprite mới được khai báo trong objects.tsx và trả về bảng name -> gid."""
    tree = ET.parse(tsx_path)
    root = tree.getroot()

    existing_by_type = {}
    max_id = -1
    for tile_el in root.findall("tile"):
        tid = int(tile_el.attrib["id"])
        if tid > max_id:
            max_id = tid
        for prop in tile_el.findall("./properties/property"):
            if prop.attrib.get("name") == "object_type":
                existing_by_type[prop.attrib.get("value")] = tid

    gid_map = {}
    for obj_name, (rel_src, w, h) in sprite_specs.items():
        if obj_name in existing_by_type:
            tid = existing_by_type[obj_name]
            # Cập nhật lại kích thước nếu thay đổi
            for tile_el in root.findall("tile"):
                if int(tile_el.attrib["id"]) == tid:
                    img_el = tile_el.find("image")
                    if img_el is not None:
                        img_el.attrib["source"] = rel_src
                        img_el.attrib["width"] = str(w)
                        img_el.attrib["height"] = str(h)
        else:
            max_id += 1
            tid = max_id
            tile_el = ET.SubElement(root, "tile", {"id": str(tid)})
            props_el = ET.SubElement(tile_el, "properties")
            ET.SubElement(props_el, "property", {"name": "object_type", "value": obj_name})
            ET.SubElement(tile_el, "image", {"source": rel_src, "width": str(w), "height": str(h)})
        gid_map[obj_name] = first_gid + tid

    root.attrib["tilecount"] = str(max_id + 1)
    tree.write(tsx_path, encoding="utf-8", xml_declaration=True)
    return gid_map


def reorganize_map(tmx_path: str, gid_map: dict) -> None:
    tree = ET.parse(tmx_path)
    root = tree.getroot()

    width = int(root.attrib["width"])
    height = int(root.attrib["height"])
    tile_w = int(root.attrib.get("tilewidth", "40"))
    tile_h = int(root.attrib.get("tileheight", "40"))

    # Đọc khung FarmZone
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
                    fz_c0 = max(0, int((fx + tile_w * 0.5) // tile_w))
                    fz_c1 = min(width - 1, int((fx + fw - tile_w * 0.5) // tile_w))
                    fz_r0 = max(0, int((fy + tile_h * 0.1) // tile_h))
                    fz_r1 = min(height - 1, int((fy + fh - tile_h * 0.1) // tile_h))
                    break

    terrain_layer = root.find("layer")
    data_elem = terrain_layer.find("data")
    tile_elems = data_elem.findall("tile")

    def set_tile_gid(r_idx: int, c_idx: int, new_gid: int, rows_grid=None):
        if not (0 <= r_idx < height and 0 <= c_idx < width):
            return
        if tile_elems:
            tile_elems[r_idx * width + c_idx].attrib["gid"] = str(new_gid)
        elif rows_grid is not None:
            rows_grid[r_idx][c_idx] = new_gid

    rows_csv = None
    if not tile_elems:
        raw = (data_elem.text or "").strip()
        rows_csv = [
            [int(v) for v in ln.strip().rstrip(",").split(",") if v.strip()]
            for ln in raw.splitlines() if ln.strip()
        ]

    # 1. Bảo toàn lưới 12 luống đất 2x2 viền cỏ xanh 1 ô trong FarmZone
    dirt_count = 0
    for r in range(fz_r0, fz_r1 + 1):
        dr = r - fz_r0
        in_bed_row = (dr % 3) < 2 and (dr // 3) < 3
        for c in range(fz_c0, fz_c1 + 1):
            dc = c - fz_c0
            in_bed_col = (dc % 3) < 2 and (dc // 3) < 4
            cur_gid = int(tile_elems[r * width + c].attrib.get("gid", "1")) if tile_elems else rows_csv[r][c]
            if cur_gid in (11, 12) or (5 <= cur_gid <= 10) or (13 <= cur_gid <= 22):
                continue
            if in_bed_row and in_bed_col:
                set_tile_gid(r, c, GID_DIRT, rows_csv)
                dirt_count += 1
            else:
                set_tile_gid(r, c, GID_GRASS, rows_csv)

    # 2. Lát đường đá lối vào khu Bể Bơi (bên Tây Nam cầu: row 42, cols 7..11) & Sòng Bạc Casino (bên Đông Nam cầu: row 42, cols 27..29)
    for c in range(6, 13):
        set_tile_gid(42, c, GID_PATH, rows_csv)
    for c in range(27, 30):
        set_tile_gid(42, c, GID_PATH, rows_csv)

    # 2b. Quy hoạch VƯỜN CÂY ĂN QUẢ LÂU NĂM MỞ RỘNG (OrchardZone: rows 45..51, cols 3..17 -> 8 ô trồng cây lớn giãn cách 3 ô = 120px chống đè tán!)
    for r in range(45, 52):
        for c in range(3, 17):
            set_tile_gid(r, c, GID_GRASS, rows_csv)
    # 2 hàng x 4 cột = 8 vị trí trồng cây ăn quả rộng rãi (cách nhau 3 ô = 120px cả ngang lẫn dọc, không bao giờ đè tán!)
    for r in (47, 50):
        for c in (5, 8, 11, 14):
            set_tile_gid(r, c, GID_DIRT, rows_csv)
    # Lối đá nhỏ dẫn từ phía Bắc vào Cổng Vườn Cây Ăn Quả (cols 9..10, rows 44..46)
    for r in (44, 45, 46):
        set_tile_gid(r, 9, GID_PATH, rows_csv)
        set_tile_gid(r, 10, GID_PATH, rows_csv)

    # 2c. Mặt bằng KHU MỎ ĐÁ MINI (MineZone: rows 47..50, cols 23..33 — đường đá dẫn xuống mỏ ở col 25)
    for r in range(46, 51):
        for c in range(23, 34):
            set_tile_gid(r, c, GID_GRASS, rows_csv)
    set_tile_gid(46, 25, GID_PATH, rows_csv)

    if rows_csv is not None:
        data_elem.text = "\n" + ",\n".join(",".join(map(str, row)) for row in rows_csv) + "\n"

    # 3. Dọn các công trường cũ / vật thể chồng lấn ở khu vực Bể bơi, Casino, Vườn Cây Ăn Quả & Khu Mỏ Đá
    managed_names = {
        "swimming_pool", "pool_lounger", "pool_umbrella", "palm_tree", "casino_building",
        "orchard_fence_h", "orchard_fence_v", "orchard_sign", "mine_rock", "mine_anvil", "mine_sign",
        "rustic_farm_gate", "farm_bed_frame", "ground_clovers", "ground_daisies",
        "ground_yellow_flowers", "ground_tuft", "ground_pebbles", "farm_decor_bush", "farm_decor_rock",
        "farm_gate", "farm_gate_side", "lantern_post_road", "garden_rock_decor", "wildflowers_decor",
        "lantern_post_orchard", "lantern_post_orchard_left", "garden_rock_orchard", "wildflowers_orchard",
        "orchard_gate", "fence_orchard_perm_h", "fence_orchard_perm_v"
    }
    for og in root.findall("objectgroup"):
        if og.attrib.get("name") == "Objects":
            for obj in list(og.findall("object")):
                name = (obj.attrib.get("name") or "").lower()
                ox = float(obj.attrib.get("x", "0"))
                oy = float(obj.attrib.get("y", "0"))
                # Thay thế biển hiệu cũ ở góc trên phải khu ruộng (x=555, y=688) bằng Cổng Chào Gỗ & Đá nằm dọc
                if name == "signpost_left" and 530 <= ox <= 620 and 640 <= oy <= 740:
                    og.remove(obj)
                    continue
                # Dịch cột đèn id=145 (x=612, y=668) ra sát lề đường phải một chút để không đụng vòm cổng dọc
                if name == "streetlamp" and 595 <= ox <= 635 and 640 <= oy <= 700:
                    obj.attrib["x"] = "646"
                    continue
                # Căn chuẩn cây cầu vòm chập giữa 2 đầu đường đá (cols 17..20, tâm x=760; sông y=1280..1480)
                if name == "bridge":
                    obj.attrib["x"] = "637"
                    obj.attrib["y"] = "1508"
                    obj.attrib["width"] = "248"
                    obj.attrib["height"] = "276"
                    continue
                # Dọn wheat_patch cũ trong FarmZone
                if "wheat" in name and fz_x0 <= ox <= fz_x1 and fz_y0 <= oy <= fz_y1:
                    og.remove(obj)
                    continue
                # Dọn object quản lý tự động để script idempotent mỗi lần chạy lại
                if name in managed_names or "construction_site" in name or name == "npc_zoro":
                    og.remove(obj)
                    continue
                # Dọn vật cản nằm trùng mặt bằng Pool, Casino, OrchardZone (x=190..605, y=1840..2090) hoặc MineZone (x=910..1380, y=1840..2090)
                if (110 <= ox <= 565 and 1510 <= oy <= 1720) or (990 <= ox <= 1275 and 1500 <= oy <= 1720):
                    if name in ("well", "dirt_spot_md", "flower_patch", "streetlamp"):
                        og.remove(obj)
                        continue
                if (120 <= ox <= 690 and 1800 <= oy <= 2095) or (910 <= ox <= 1380 and 1840 <= oy <= 2095):
                    og.remove(obj)
                    continue

            # Bổ sung quần thể BỂ BƠI NGHỈ DƯỠNG (Pool Zone: x=115..565, y=1520..1705) & SÒNG BẠC CASINO (x=940..1310, y=1510..1710)
            new_objects = [
                # --- KHU BỂ BƠI RESORT (POOL ZONE - Phía Tây bên kia cầu) ---
                ("swimming_pool", gid_map["swimming_pool"], 168, 1766, 168, 238),
                # 3 chiếc ghế dài tắm nắng (POOL_CHAIR) xếp ngay ngắn cạnh hồ bơi
                ("pool_lounger",  gid_map["pool_lounger"],  364, 1648, 52, 72),
                ("pool_lounger",  gid_map["pool_lounger"],  426, 1648, 52, 72),
                ("pool_lounger",  gid_map["pool_lounger"],  426, 1732, 52, 72),
                # 2 dù che nắng sọc xanh trắng (POOL_UMBRELLA) che phía trên ghế tắm nắng
                ("pool_umbrella", gid_map["pool_umbrella"], 392, 1626, 72, 72),
                ("pool_umbrella", gid_map["pool_umbrella"], 470, 1706, 72, 72),
                # 3 cây dừa nhiệt đới (PALM_TREE) bao quanh khu resort
                ("palm_tree",     gid_map["palm_tree"],     74,  1618, 96, 120),
                ("palm_tree",     gid_map["palm_tree"],     74,  1762, 96, 120),
                ("palm_tree",     gid_map["palm_tree"],     526, 1676, 96, 120),

                # --- SÒNG BẠC LỚN (CASINO - Phía Đông bên kia cầu) ---
                ("casino_building", gid_map["casino_building"], 1012, 1708, 240, 196),
                ("palm_tree",       gid_map["palm_tree"],       928,  1704, 90,  128),
                ("palm_tree",       gid_map["palm_tree"],       1256, 1704, 90,  128),

                # --- KHU VƯỜN CÂY ĂN QUẢ MỞ RỘNG (x=136..664, y=1836..2068) CÓ CỔNG CHÀO + HÀNG RÀO VĨNH VIỄN ---
                # Cổng Chào Vườn Cây Ăn Quả ở giữa cạnh Bắc (x=330, y=1864 -> lối đi rộng giữa x=364..440)
                ("farm_gate", gid_map["farm_gate"], 330, 1864, 144, 118),
                # Rào khóa tạm ngay cửa cổng (chỉ biến mất khi mua mở khóa $3.000)
                ("orchard_fence_h", gid_map["fence_wood"], 358, 1858, 48, 44),
                ("orchard_fence_h", gid_map["fence_wood"], 406, 1858, 48, 44),
                ("orchard_sign",    58,                    302, 1854, 34, 42),

                # Hàng rào gỗ vĩnh viễn bao quanh khu vườn (Bắc trái & phải cổng, Nam, Tây, Đông)
                ("fence_orchard_perm_h", gid_map["fence_wood"], 142, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 190, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 238, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 286, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 470, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 518, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 566, 1846, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 614, 1846, 48, 44),

                ("fence_orchard_perm_h", gid_map["fence_wood"], 142, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 190, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 238, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 286, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 334, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 382, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 430, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 478, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 526, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 574, 2070, 48, 44),
                ("fence_orchard_perm_h", gid_map["fence_wood"], 618, 2070, 48, 44),

                ("fence_orchard_perm_v", 43, 134, 1916, 20, 72),
                ("fence_orchard_perm_v", 43, 134, 1988, 20, 72),
                ("fence_orchard_perm_v", 43, 134, 2060, 20, 72),
                ("fence_orchard_perm_v", 43, 656, 1916, 20, 72),
                ("fence_orchard_perm_v", 43, 656, 1988, 20, 72),
                ("fence_orchard_perm_v", 43, 656, 2060, 20, 72),

                # --- KHU KHAI KHOÁNG MINI (7 Tảng Đá Mỏ Quặng + Đe Đúc Vòi Tưới Tự Động) ---
                ("mine_anvil", gid_map["mine_anvil"], 986,  1886, 56, 48),
                ("mine_sign",  58,                    942,  1882, 34, 42),
                ("mine_rock",  gid_map["ore_rock"],   1072, 1916, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1166, 1906, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1264, 1920, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1036, 1998, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1134, 2012, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1232, 2002, 62, 54),
                ("mine_rock",  gid_map["ore_rock"],   1318, 1966, 62, 54),

                # --- CỔNG CHÀO NÔNG TRẠI XOAY NGANG (FARM_GATE_SIDE) KHỚP LỐI VÀO KHU VƯỜN ĐẦU TIÊN ---
                ("farm_gate_side", gid_map["farm_gate_side"], 562, 804, 50, 186),

                # --- 2 CỘT ĐÈN LỒNG GỖ (LANTERN_POST) DỌC CON ĐƯỜNG ĐÁ BÊN PHẢI ---
                ("lantern_post_road", gid_map["lantern_post"], 652, 856, 48, 72),
                ("lantern_post_road", gid_map["lantern_post"], 652, 1018, 48, 72),

                # --- CỤM ĐÁ VƯỜN (GARDEN_ROCK) VEN HÀNG RÀO & ĐƯỜNG ĐÁ (KHÔNG CHẶN LỐI ĐI / Ô ĐẤT) ---
                ("garden_rock_decor", gid_map["garden_rock"], 424, 622, 42, 42),
                ("garden_rock_decor", gid_map["garden_rock"], 612, 996, 44, 44),
                ("garden_rock_decor", gid_map["garden_rock"], 112, 622, 40, 40),
            ]
            # Rải hoa dại & cỏ 3 lá (WILDFLOWERS) tự nhiên trên các khoảng cỏ trống giữa 12 luống đất & chân rào
            wildflower_spots = [
                (200, 704, 40, 40),
                (320, 704, 40, 40),
                (440, 704, 40, 40),
                (140, 756, 40, 40),
                (260, 756, 40, 40),
                (380, 756, 40, 40),
                (200, 824, 40, 40),
                (320, 824, 40, 40),
                (440, 824, 40, 40),
                (140, 876, 40, 40),
                (260, 876, 40, 40),
                (380, 876, 40, 40),
                (500, 876, 40, 40),
                (200, 944, 40, 40),
                (320, 944, 40, 40),
                (440, 944, 40, 40),
                (614, 816, 42, 42),
                (614, 926, 42, 42),
            ]
            for wx, wy, ww, wh in wildflower_spots:
                new_objects.append(("wildflowers_decor", gid_map["wildflowers"], wx, wy, ww, wh))

            # Trang trí VƯỜN CÂY ĂN QUẢ MỞ RỘNG (x=136..664, y=1840..2068):
            # 2 cột đèn lồng gỗ hai bên cổng + đá vườn + hoa dại & cỏ 3 lá rải đều giữa các khoảng cỏ 120px
            orchard_extras = [
                ("lantern_post_orchard",      gid_map["lantern_post"],      264, 1852, 48, 72),
                ("lantern_post_orchard_left", gid_map["lantern_post_left"], 494, 1852, 48, 72),
                ("garden_rock_orchard",  gid_map["garden_rock"],  606, 1892, 42, 42),
                ("garden_rock_orchard",  gid_map["garden_rock"],  154, 2028, 42, 42),
                ("wildflowers_orchard",  gid_map["wildflowers"],  262, 1948, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  382, 1948, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  502, 1948, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  262, 2036, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  382, 2036, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  502, 2036, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  158, 1952, 40, 40),
                ("wildflowers_orchard",  gid_map["wildflowers"],  604, 1986, 40, 40),
            ]
            new_objects.extend(orchard_extras)
            base_obj_id = 1300
            for idx, (oname, ogid, ox, oy, ow, oh) in enumerate(new_objects):
                ET.SubElement(og, "object", {
                    "id": str(base_obj_id + idx),
                    "name": oname,
                    "gid": str(ogid),
                    "x": str(ox),
                    "y": str(oy),
                    "width": str(ow),
                    "height": str(oh),
                })

        elif og.attrib.get("name") == "GameplayZones":
            for obj in list(og.findall("object")):
                if obj.attrib.get("name") in ("PoolZone", "CasinoZone", "OrchardZone", "MineZone"):
                    og.remove(obj)
            ET.SubElement(og, "object", {
                "id": "506", "name": "PoolZone", "type": "zone",
                "x": "110", "y": "1520", "width": "490", "height": "195"
            })
            ET.SubElement(og, "object", {
                "id": "507", "name": "CasinoZone", "type": "zone",
                "x": "995", "y": "1505", "width": "275", "height": "215"
            })
            ET.SubElement(og, "object", {
                "id": "508", "name": "OrchardZone", "type": "zone",
                "x": "136", "y": "1836", "width": "528", "height": "234"
            })
            ET.SubElement(og, "object", {
                "id": "509", "name": "MineZone", "type": "zone",
                "x": "935", "y": "1850", "width": "445", "height": "205"
            })

    root.attrib["nextobjectid"] = "1400"
    tree.write(tmx_path, encoding="utf-8", xml_declaration=True)

    # Đồng bộ sang thư mục out/production/sourcecode nếu tồn tại
    if os.path.isdir(OUT_TILED_DIR):
        shutil.copy2(tmx_path, os.path.join(OUT_TILED_DIR, "level1.tmx"))
        shutil.copy2(TSX_OBJECTS_PATH, os.path.join(OUT_TILED_DIR, "objects.tsx"))
        out_obj_dir = os.path.join(OUT_TILED_DIR, "objects")
        os.makedirs(out_obj_dir, exist_ok=True)
        for fn in os.listdir(OBJ_IMG_DIR):
            if fn.endswith(".png"):
                shutil.copy2(os.path.join(OBJ_IMG_DIR, fn), os.path.join(out_obj_dir, fn))

    print(
        f"Updated {tmx_path}: FarmZone ({dirt_count} dirt tiles), PoolZone (pool + 3 loungers + 2 umbrellas + 4 palms), "
        f"and CasinoZone (casino_building + 2 palms)."
    )


if __name__ == "__main__":
    specs = generate_resort_and_casino_sprites()
    gids = register_objects_in_tsx(TSX_OBJECTS_PATH, specs)
    reorganize_map(TMX_PATH, gids)
