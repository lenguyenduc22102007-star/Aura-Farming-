import xml.etree.ElementTree as ET
from xml.dom import minidom

width = 40
height = 40
tile_size = 40

# Terrain mapping
# 1 = Grass, 2 = Dirt, 3 = Path, 4 = Water
terrain = [[1 for _ in range(width)] for _ in range(height)]

# Create River at rows 32-35
for r in range(32, 36):
    for c in range(width):
        terrain[r][c] = 4

# Create Paths (3)
# Vertical path center
for r in range(0, height):
    if terrain[r][20] != 4:
        terrain[r][20] = 3
        terrain[r][19] = 3

# Horizontal path
for c in range(0, width):
    if terrain[18][c] != 4:
        terrain[18][c] = 3
        terrain[17][c] = 3

# Create Dirt (2) for farm zone (left side, rows 20-30, cols 2-16)
for r in range(20, 31):
    for c in range(2, 17):
        terrain[r][c] = 2

# Build the CSV string for data
csv_lines = []
for row in terrain:
    csv_lines.append(",".join(map(str, row)))
csv_data = ",\n".join(csv_lines)

# Create XML
map_elem = ET.Element('map', {'version': '1.10', 'tiledversion': '1.12.2', 'orientation': 'orthogonal', 'renderorder': 'right-down', 'width': str(width), 'height': str(height), 'tilewidth': '40', 'tileheight': '40', 'infinite': '0', 'nextlayerid': '4', 'nextobjectid': '100'})

ET.SubElement(map_elem, 'tileset', {'firstgid': '1', 'source': 'terrain.tsx'})
ET.SubElement(map_elem, 'tileset', {'firstgid': '5', 'source': 'objects.tsx'})

layer_elem = ET.SubElement(map_elem, 'layer', {'id': '1', 'name': 'Terrain', 'width': str(width), 'height': str(height)})
data_elem = ET.SubElement(layer_elem, 'data', {'encoding': 'csv'})
data_elem.text = '\n' + csv_data + '\n'

objects_group = ET.SubElement(map_elem, 'objectgroup', {'id': '2', 'name': 'Objects'})

def add_obj(oid, name, gid, c, r, w, h):
    ET.SubElement(objects_group, 'object', {'id': str(oid), 'name': name, 'gid': str(gid), 'x': str(c*40), 'y': str(r*40), 'width': str(w), 'height': str(h)})

# Add Objects
obj_id = 1
# Bridge over river
add_obj(obj_id, 'bridge', 5+7, 18, 37, 200, 190); obj_id+=1

# Main house at top center
add_obj(obj_id, 'house_main', 5+0, 16, 7, 320, 260); obj_id+=1

# Barn at top right
add_obj(obj_id, 'barn', 5+2, 28, 9, 340, 280); obj_id+=1

# Small house
add_obj(obj_id, 'house_small', 5+1, 5, 8, 120, 220); obj_id+=1

# Trees and rocks randomly
import random
random.seed(42)
for i in range(40):
    c = random.randint(1, 38)
    r = random.randint(1, 30)
    # avoid paths and zones roughly
    if r > 16 and c < 18: continue
    if terrain[r][c] == 1 and not (14<=c<=26 and r<10):
        if random.random() > 0.3:
            add_obj(obj_id, 'tree', 5+8, c, r, 110, 140); obj_id+=1
        else:
            add_obj(obj_id, 'rock', 5+9, c, r, 110, 100); obj_id+=1

# Fences
add_obj(obj_id, 'fence', 5+10, 25, 12, 180, 70); obj_id+=1
add_obj(obj_id, 'fence', 5+10, 25, 15, 180, 70); obj_id+=1

# Campfire
add_obj(obj_id, 'campfire', 5+5, 5, 14, 160, 140); obj_id+=1

# Flags
add_obj(obj_id, 'flag', 5+12, 18, 14, 90, 160); obj_id+=1
add_obj(obj_id, 'flag', 5+12, 22, 14, 90, 160); obj_id+=1

zones_group = ET.SubElement(map_elem, 'objectgroup', {'id': '3', 'name': 'GameplayZones'})
def add_zone(oid, name, x, y, w, h):
    obj = ET.SubElement(zones_group, 'object', {'id': str(oid), 'name': name, 'type': 'zone', 'x': str(x), 'y': str(y), 'width': str(w), 'height': str(h)})
    props = ET.SubElement(obj, 'properties')
    ET.SubElement(props, 'property', {'name': 'action', 'value': name})

# Add Zones
add_zone(100, 'FarmZone', 80, 800, 600, 440)
add_zone(101, 'AnimalZone', 900, 800, 600, 440)
add_zone(102, 'DigZone', 80, 320, 400, 200)
add_zone(103, 'BuildZone', 900, 320, 400, 200)

xml_str = ET.tostring(map_elem, encoding='utf-8')
parsed = minidom.parseString(xml_str)
pretty_xml = parsed.toprettyxml(indent='  ')
# Remove blank lines
pretty_xml = "\n".join([line for line in pretty_xml.split("\n") if line.strip()])

with open(r'c:\Users\Dell7560\Downloads\sourcecode\view\assets\tiled\level1.tmx', 'w', encoding='utf-8') as f:
    f.write(pretty_xml)
print('Generated level1.tmx successfully.')
