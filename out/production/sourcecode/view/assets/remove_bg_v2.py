import sys
from PIL import Image

def remove_background(image_path):
    try:
        img = Image.open(image_path).convert("RGBA")
        datas = img.getdata()
        width, height = img.size

        # Get corner colors
        corners = [
            img.getpixel((0, 0)),
            img.getpixel((width - 1, 0)),
            img.getpixel((0, height - 1)),
            img.getpixel((width - 1, height - 1))
        ]
        
        # Collect background colors (typically the most common corner color)
        # Or just use all corners that have high alpha
        bg_colors = [c for c in corners if c[3] > 200]
        
        # Add pure white and near white just in case
        bg_colors.append((255, 255, 255, 255))
        bg_colors.append((240, 240, 240, 255)) # Light gray

        threshold = 40
        
        newData = []
        for item in datas:
            is_bg = False
            for bg in bg_colors:
                if (abs(item[0] - bg[0]) < threshold and 
                    abs(item[1] - bg[1]) < threshold and 
                    abs(item[2] - bg[2]) < threshold):
                    is_bg = True
                    break
            
            if is_bg:
                newData.append((255, 255, 255, 0))
            else:
                newData.append(item)

        img.putdata(newData)
        img.save(image_path, "PNG")
        print(f"Successfully processed {image_path}")
    except Exception as e:
        print(f"Error processing {image_path}: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        for arg in sys.argv[1:]:
            remove_background(arg)
    else:
        print("Please provide image paths.")
