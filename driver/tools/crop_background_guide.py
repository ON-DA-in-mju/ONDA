from pathlib import Path

from PIL import Image

png = next(Path(r"c:\Users\82108\Desktop\Bus\png").glob("DRI-01-03G*.png"))
im = Image.open(png).convert("RGBA")
print("size", im.size)
w, h = im.size
px = im.load()


def is_dark(x: int, y: int) -> bool:
    r, g, b, a = px[x, y]
    return a > 200 and r < 40 and g < 40 and b < 40


left = 0
for x in range(w):
    if sum(1 for y in range(h // 3, 2 * h // 3, 4) if not is_dark(x, y)) > 20:
        left = x
        break
right = w - 1
for x in range(w - 1, -1, -1):
    if sum(1 for y in range(h // 3, 2 * h // 3, 4) if not is_dark(x, y)) > 20:
        right = x
        break

content = im.crop((left, 0, right + 1, h))
cw, ch = content.size
print("content", content.size)

# Illustration band below title bar, above headline
ill = content.crop((int(cw * 0.05), int(ch * 0.11), int(cw * 0.95), int(ch * 0.335)))
out = Path(
    r"c:\Users\82108\Desktop\Bus\android-native\app\src\main\res\drawable-nodpi"
    r"\background_guide_illustration.png"
)
ill.save(out)
print("saved", out, ill.size)

# Sample teal icon colors near first card
for yf in (0.42, 0.48, 0.54, 0.60):
    for xf in (0.14, 0.18):
        x, y = int(cw * xf), int(ch * yf)
        print(xf, yf, content.getpixel((x, y)))
