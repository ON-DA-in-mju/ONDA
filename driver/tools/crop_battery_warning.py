from pathlib import Path

from PIL import Image

png = next(Path(r"c:\Users\82108\Desktop\Bus\png").glob("DRI-01-03H*.png"))
im = Image.open(png).convert("RGBA")
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

# Bus illustration on right of operation card (~y 28%-42%, x 55%-92%)
ill = content.crop((int(cw * 0.52), int(ch * 0.265), int(cw * 0.93), int(ch * 0.42)))
out = Path(
    r"c:\Users\82108\Desktop\Bus\android-native\app\src\main\res\drawable-nodpi"
    r"\battery_warning_illustration.png"
)
ill.save(out)
print("saved", out, ill.size)
