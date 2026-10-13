import re

with open(r'c:\Users\Dell7560\Downloads\sourcecode\view\screen_controller\InGameController.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the occupied array for camp to prevent rocks from spawning on the flag
old_occ = 'for(int r = 14; r <= 20; r++) for(int c = 3; c <= 12; c++) occupied[r][c] = true;'
new_occ = 'for(int r = 13; r <= 21; r++) for(int c = 2; c <= 17; c++) occupied[r][c] = true;'
content = content.replace(old_occ, new_occ)

content = content.replace('if (Assets.PATH_TILE != null) {', 'if (Assets.DIRT_TILE != null) {')
content = content.replace('backgroundView.setImage(Assets.PATH_TILE);', 'backgroundView.setImage(Assets.DIRT_TILE);')

with open(r'c:\Users\Dell7560\Downloads\sourcecode\view\screen_controller\InGameController.java', 'w', encoding='utf-8') as f:
    f.write(content)
print('Patched successfully!')
