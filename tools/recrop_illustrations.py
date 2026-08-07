"""PNG 시안에서 폰 베젤을 제거하고 일러스트만 깔끔하게 추출합니다."""

from pathlib import Path
from typing import Tuple

from PIL import Image

PNG_DIR = Path(r"c:\Users\82108\Desktop\Bus\png")
OUT_DIR = Path(r"c:\Users\82108\Desktop\Bus\assets\images")
OUT_DIR.mkdir(parents=True, exist_ok=True)


def is_bezel(rgb: Tuple[int, int, int]) -> bool:
    r, g, b = rgb
    if r < 40 and g < 40 and b < 40:
        return True
    if abs(r - g) < 8 and abs(g - b) < 8 and r < 110:
        return True
    return False


def find_screen_bbox(im: Image.Image) -> Tuple[int, int, int, int]:
    rgb = im.convert("RGB")
    px = rgb.load()
    w, h = rgb.size

    # 좌/우 베젤 폭 탐색
    left = 0
    for x in range(w // 2):
        dark = sum(1 for y in range(0, h, 4) if is_bezel(px[x, y]))
        if dark > (h // 4) * 0.7:
            left = x + 1
        else:
            break

    right = w
    for x in range(w - 1, w // 2, -1):
        dark = sum(1 for y in range(0, h, 4) if is_bezel(px[x, y]))
        if dark > (h // 4) * 0.7:
            right = x
        else:
            break

    # 상/하 베젤
    top = 0
    for y in range(h // 3):
        dark = sum(1 for x in range(left, right, 4) if is_bezel(px[x, y]))
        cols = max(1, (right - left) // 4)
        if dark > cols * 0.7:
            top = y + 1
        else:
            break

    bottom = h
    for y in range(h - 1, h * 2 // 3, -1):
        dark = sum(1 for x in range(left, right, 4) if is_bezel(px[x, y]))
        cols = max(1, (right - left) // 4)
        if dark > cols * 0.7:
            bottom = y
        else:
            break

    return left, top, right, bottom


def trim_whitespace(im: Image.Image, threshold: int = 246, pad: int = 10) -> Image.Image:
    rgb = im.convert("RGB")
    px = rgb.load()
    w, h = rgb.size
    min_x, min_y, max_x, max_y = w, h, 0, 0
    found = False
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if r >= threshold and g >= threshold and b >= threshold:
                continue
            found = True
            min_x = min(min_x, x)
            min_y = min(min_y, y)
            max_x = max(max_x, x)
            max_y = max(max_y, y)
    if not found:
        return im
    box = (
        max(0, min_x - pad),
        max(0, min_y - pad),
        min(w, max_x + pad + 1),
        min(h, max_y + pad + 1),
    )
    return im.crop(box)


def crop_rel(
    screen: Image.Image,
    top: float,
    bottom: float,
    left: float = 0.0,
    right: float = 1.0,
) -> Image.Image:
    w, h = screen.size
    return screen.crop((int(w * left), int(h * top), int(w * right), int(h * bottom)))


def main() -> None:
    login_png = next(PNG_DIR.glob("DRI-00-01*.png"))
    consent_png = next(PNG_DIR.glob("DRI-00-02A*.png"))
    guide_png = next(PNG_DIR.glob("DRI-00-02B*.png"))
    complete_png = next(PNG_DIR.glob("DRI-00-02C*.png"))

    for label, path in [
        ("login", login_png),
        ("consent", consent_png),
        ("guide", guide_png),
        ("complete", complete_png),
    ]:
        im = Image.open(path).convert("RGBA")
        box = find_screen_bbox(im)
        print(label, "raw", im.size, "screen", box, "screen_size", (box[2] - box[0], box[3] - box[1]))

    login_screen = Image.open(login_png).convert("RGBA").crop(find_screen_bbox(Image.open(login_png)))
    consent_screen = Image.open(consent_png).convert("RGBA").crop(find_screen_bbox(Image.open(consent_png)))
    guide_screen = Image.open(guide_png).convert("RGBA").crop(find_screen_bbox(Image.open(guide_png)))
    complete_screen = Image.open(complete_png).convert("RGBA").crop(find_screen_bbox(Image.open(complete_png)))

    # 로그인 일러스트: 서브텍스트 아래 ~ 입력폼 직전 (버스 전체 포함)
    login_illust = trim_whitespace(crop_rel(login_screen, 0.34, 0.50, 0.0, 1.0))
    login_illust.save(OUT_DIR / "login_illustration.png")
    print("login_illustration", login_illust.size)

    # 위치정보 안내 일러스트
    consent_illust = trim_whitespace(crop_rel(consent_screen, 0.105, 0.275, 0.0, 1.0))
    consent_illust.save(OUT_DIR / "location_consent_illustration.png")
    print("location_consent_illustration", consent_illust.size)

    # 권한 안내 방패
    guide_illust = trim_whitespace(crop_rel(guide_screen, 0.105, 0.285, 0.08, 0.92))
    guide_illust.save(OUT_DIR / "permission_guide_illustration.png")
    print("permission_guide_illustration", guide_illust.size)

    # 권한 완료 하단 버스만 (상태카드/버튼 제외)
    bus = trim_whitespace(crop_rel(complete_screen, 0.705, 0.855, 0.0, 1.0))
    bus.save(OUT_DIR / "permission_complete_bus.png")
    print("permission_complete_bus", bus.size)


if __name__ == "__main__":
    main()
