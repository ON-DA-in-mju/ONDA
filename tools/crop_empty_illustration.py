from pathlib import Path
from PIL import Image

png_dir = Path(r"c:\Users\82108\Desktop\Bus\png")
files = list(png_dir.glob("DRI-01-00B*.png"))
if not files:
    raise SystemExit("PNG not found")

img = Image.open(files[0]).convert("RGBA")
w, h = img.size
print("size", w, h)

# Crop empty-state illustration inside the card (relative to design frame)
left = int(w * 0.16)
right = int(w * 0.84)
top = int(h * 0.295)
bottom = int(h * 0.475)
crop = img.crop((left, top, right, bottom))

out_android = Path(
    r"c:\Users\82108\Desktop\Bus\android-native\app\src\main\res\drawable-nodpi\today_empty_illustration.png"
)
out_assets = Path(r"c:\Users\82108\Desktop\Bus\assets\images\today_empty_illustration.png")
crop.save(out_android)
crop.save(out_assets)
print("saved", crop.size, out_android)
