from pathlib import Path

from PIL import Image

png_dir = Path(r"c:\Users\82108\Desktop\Bus\png")
out_dir = Path(r"c:\Users\82108\Desktop\Bus\assets\images")
out_dir.mkdir(parents=True, exist_ok=True)

login = Image.open(next(png_dir.glob("DRI-00-01*.png"))).convert("RGBA")
splash = Image.open(next(png_dir.glob("DRI-00-00*.png"))).convert("RGBA")
print("login", login.size, "splash", splash.size)


def content_bbox(im: Image.Image, threshold: int = 250):
    px = im.load()
    w, h = im.size
    minx, miny, maxx, maxy = w, h, 0, 0
    step = 2
    for y in range(0, h, step):
        for x in range(0, w, step):
            r, g, b, a = px[x, y]
            if a < 10:
                continue
            if r > threshold and g > threshold and b > threshold:
                continue
            minx = min(minx, x)
            miny = min(miny, y)
            maxx = max(maxx, x)
            maxy = max(maxy, y)
    if maxx <= minx:
        return (0, 0, w, h)
    pad = 4
    return (max(0, minx - pad), max(0, miny - pad), min(w, maxx + pad), min(h, maxy + pad))


lb = content_bbox(login)
sb = content_bbox(splash)
print("login content", lb)
print("splash content", sb)

login_screen = login.crop(lb)
splash_screen = splash.crop(sb)
login_screen.save(out_dir / "ref_login_screen.png")
splash_screen.save(out_dir / "ref_splash_screen.png")
print("saved refs", login_screen.size, splash_screen.size)

w, h = login_screen.size
illust = login_screen.crop((int(w * 0.05), int(h * 0.22), int(w * 0.95), int(h * 0.52)))
illust.save(out_dir / "login_illustration.png")
print("illust", illust.size)

w2, h2 = splash_screen.size
splash_illust = splash_screen.crop((0, int(h2 * 0.55), w2, h2))
splash_illust.save(out_dir / "splash_illustration.png")
print("splash illust", splash_illust.size)
