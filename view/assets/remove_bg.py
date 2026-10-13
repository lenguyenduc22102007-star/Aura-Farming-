import sys
from PIL import Image

def remove_background(image_path):
    try:
        img = Image.open(image_path).convert("RGBA")
        datas = img.getdata()

        # Get the color of the top-left pixel to use as the background color to remove
        bg_color = datas[0]
        
        # We will remove pixels that are very close to bg_color
        # Threshold for similarity
        threshold = 30
        
        newData = []
        for item in datas:
            # item is (R, G, B, A)
            if (abs(item[0] - bg_color[0]) < threshold and 
                abs(item[1] - bg_color[1]) < threshold and 
                abs(item[2] - bg_color[2]) < threshold):
                # Replace with transparent
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
