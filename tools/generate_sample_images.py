"""Usage: python3 tools/generate_sample_images.py app/src/main/res/drawable-nodpi

Generates original sample backgrounds for SowarApp (no third-party content).

Text is NOT drawn here: the app overlays the Arabic caption itself, so Arabic
letter shaping is handled correctly by Android.
"""
import math
import random
import sys
from PIL import Image, ImageDraw, ImageFilter

W, H = 720, 960
OUT = sys.argv[1]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(top, bottom, mid=None):
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        if mid:
            c = lerp(top, mid, t * 2) if t < 0.5 else lerp(mid, bottom, (t - 0.5) * 2)
        else:
            c = lerp(top, bottom, t)
        d.line([(0, y), (W, y)], fill=c)
    return img


def glow(img, cx, cy, r, color, alpha=140):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(r * 0.45))
    img.paste(layer, (0, 0), layer)


def overlay(img):
    return Image.new("RGBA", img.size, (0, 0, 0, 0))


def flower(d, cx, cy, r, petal, center, petals=6, rot=0.0):
    for i in range(petals):
        a = rot + i * 2 * math.pi / petals
        px, py = cx + math.cos(a) * r * 0.55, cy + math.sin(a) * r * 0.55
        pr = r * 0.48
        d.ellipse([px - pr, py - pr, px + pr, py + pr], fill=petal)
    cr = r * 0.3
    d.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=center)


def bokeh(img, rnd, n, colors):
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    for _ in range(n):
        x, y, r = rnd.randint(0, W), rnd.randint(0, H), rnd.randint(8, 40)
        c = rnd.choice(colors)
        d.ellipse([x - r, y - r, x + r, y + r], fill=c + (rnd.randint(30, 90),))
    lay = lay.filter(ImageFilter.GaussianBlur(4))
    img.paste(lay, (0, 0), lay)


def morning(seed):
    rnd = random.Random(seed)
    palettes = [
        ((255, 214, 165), (255, 140, 105), (255, 183, 130)),
        ((255, 236, 179), (255, 167, 38), None),
        ((179, 229, 252), (255, 204, 128), None),
        ((255, 224, 178), (240, 98, 146), (255, 171, 145)),
        ((200, 230, 201), (255, 213, 79), None),
    ]
    top, bottom, mid = palettes[seed % len(palettes)]
    img = gradient(top, bottom, mid)
    cx, cy = W // 2 + rnd.randint(-120, 120), int(H * 0.36)
    glow(img, cx, cy, 260, (255, 245, 200), 170)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    for i in range(16):
        a = i * math.pi / 8
        d.line([(cx + math.cos(a) * 130, cy + math.sin(a) * 130),
                (cx + math.cos(a) * 210, cy + math.sin(a) * 210)],
               fill=(255, 250, 220, 150), width=10)
    d.ellipse([cx - 110, cy - 110, cx + 110, cy + 110], fill=(255, 236, 150, 255))
    # rolling hills
    for k, col in enumerate([(255, 255, 255, 70), (255, 255, 255, 110)]):
        base = int(H * (0.72 + k * 0.1))
        pts = [(0, H)]
        for x in range(0, W + 1, 20):
            pts.append((x, base + math.sin(x / 120 + seed + k) * 30))
        pts.append((W, H))
        d.polygon(pts, fill=col)
    img.paste(lay, (0, 0), lay)
    bokeh(img, rnd, 25, [(255, 255, 255), (255, 240, 200)])
    return img


def evening(seed):
    rnd = random.Random(seed)
    palettes = [
        ((26, 35, 126), (123, 31, 162), None),
        ((13, 27, 42), (65, 90, 119), None),
        ((49, 27, 146), (236, 64, 122), (94, 53, 177)),
        ((0, 37, 64), (0, 121, 107), None),
        ((40, 20, 60), (255, 112, 67), (120, 40, 100)),
    ]
    top, bottom, mid = palettes[seed % len(palettes)]
    img = gradient(top, bottom, mid)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    for _ in range(140):
        x, y, r = rnd.randint(0, W), rnd.randint(0, int(H * 0.7)), rnd.choice([1, 1, 2, 2, 3])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, rnd.randint(120, 255)))
    img.paste(lay, (0, 0), lay)
    mx, my = int(W * 0.68), int(H * 0.24)
    glow(img, mx, my, 170, (255, 248, 225), 110)
    moon = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    md = ImageDraw.Draw(moon)
    md.ellipse([mx - 90, my - 90, mx + 90, my + 90], fill=(255, 245, 210, 255))
    md.ellipse([mx - 50, my - 110, mx + 120, my + 70], fill=(0, 0, 0, 0))
    img.paste(moon, (0, 0), moon)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    pts = [(0, H)]
    for x in range(0, W + 1, 24):
        pts.append((x, int(H * 0.82) + math.sin(x / 90 + seed) * 22))
    pts.append((W, H))
    d.polygon(pts, fill=(0, 0, 0, 90))
    img.paste(lay, (0, 0), lay)
    return img


def flowers(seed):
    rnd = random.Random(seed)
    palettes = [
        ((252, 228, 236), (248, 187, 208)),
        ((243, 229, 245), (206, 147, 216)),
        ((255, 243, 224), (255, 204, 188)),
        ((232, 245, 233), (165, 214, 167)),
        ((227, 242, 253), (179, 157, 219)),
    ]
    petal_sets = [
        [(236, 64, 122), (244, 143, 177), (255, 255, 255)],
        [(171, 71, 188), (255, 255, 255), (240, 98, 146)],
        [(255, 112, 67), (255, 202, 40), (255, 255, 255)],
        [(255, 255, 255), (255, 241, 118), (129, 199, 132)],
        [(126, 87, 194), (100, 181, 246), (255, 255, 255)],
    ]
    top, bottom = palettes[seed % 5]
    img = gradient(top, bottom)
    bokeh(img, rnd, 30, [(255, 255, 255)])
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    # stems
    for _ in range(9):
        x = rnd.randint(60, W - 60)
        d.line([(x, H), (x + rnd.randint(-80, 80), rnd.randint(int(H * 0.45), int(H * 0.8)))],
               fill=(76, 140, 80, 200), width=6)
    for _ in range(14):
        cx, cy = rnd.randint(40, W - 40), rnd.randint(int(H * 0.4), H - 40)
        r = rnd.randint(45, 95)
        petal = rnd.choice(petal_sets[seed % 5]) + (230,)
        flower(d, cx, cy, r, petal, (255, 213, 79, 255), rnd.choice([5, 6, 8]), rnd.random())
    for _ in range(10):
        cx, cy = rnd.randint(20, W - 20), rnd.randint(0, int(H * 0.35))
        flower(d, cx, cy, rnd.randint(14, 28), (255, 255, 255, 120), (255, 255, 255, 160), 5, rnd.random())
    img.paste(lay, (0, 0), lay)
    return img


def star_points(cx, cy, r_out, r_in, n=8, rot=0.0):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + i * math.pi / n
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return pts


def wisdom(seed):
    palettes = [
        ((38, 50, 56), (69, 90, 100)),
        ((0, 77, 64), (0, 105, 92)),
        ((62, 39, 35), (121, 85, 72)),
        ((26, 35, 126), (57, 73, 171)),
        ((74, 20, 140), (106, 27, 154)),
    ]
    top, bottom = palettes[seed % 5]
    img = gradient(top, bottom)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    step = 120
    for y in range(-step, H + step, step):
        for x in range(-step, W + step, step):
            off = step // 2 if (y // step) % 2 else 0
            d.polygon(star_points(x + off, y, 46, 22, 8, math.pi / 8), outline=(255, 255, 255, 28))
    # framed panel for the quote
    d.rounded_rectangle([60, 260, W - 60, H - 260], radius=36, fill=(0, 0, 0, 70),
                        outline=(255, 215, 128, 160), width=3)
    img.paste(lay, (0, 0), lay)
    return img


def jumuah(seed):
    palettes = [
        ((0, 77, 64), (27, 94, 32)),
        ((13, 71, 161), (0, 96, 100)),
        ((27, 94, 32), (130, 119, 23)),
        ((0, 60, 50), (0, 121, 107)),
        ((49, 27, 146), (0, 77, 64)),
    ]
    top, bottom = palettes[seed % 5]
    img = gradient(top, bottom)
    glow(img, W // 2, int(H * 0.3), 260, (255, 224, 130), 70)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    gold = (255, 213, 79)
    cx, cy = W // 2, int(H * 0.3)
    d.polygon(star_points(cx, cy, 170, 120, 8), outline=gold + (200,))
    d.polygon(star_points(cx, cy, 150, 105, 8, math.pi / 8), outline=gold + (140,))
    d.ellipse([cx - 80, cy - 80, cx + 80, cy + 80], fill=gold + (230,))
    d.ellipse([cx - 45, cy - 95, cx + 105, cy + 55], fill=(0, 0, 0, 0))
    # arches along the bottom
    for i in range(5):
        x0 = i * W // 5
        x1 = x0 + W // 5
        d.rectangle([x0 + 12, int(H * 0.82), x1 - 12, H], fill=(255, 255, 255, 40))
        d.pieslice([x0 + 12, int(H * 0.82) - (x1 - x0) // 2 + 12, x1 - 12, int(H * 0.82) + (x1 - x0) // 2 - 12],
                   180, 360, fill=(255, 255, 255, 40))
    img.paste(lay, (0, 0), lay)
    # lanterns: drawn on a fresh layer so the moon cut-out above doesn't affect them
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    for lx in (110, W - 110):
        d.line([(lx, 0), (lx, 120)], fill=gold + (200,), width=3)
        d.polygon([(lx - 30, 160), (lx, 120), (lx + 30, 160), (lx + 22, 230), (lx - 22, 230)], fill=gold + (210,))
    img.paste(lay, (0, 0), lay)
    return img


def occasions(seed):
    rnd = random.Random(seed)
    palettes = [
        ((255, 236, 179), (255, 112, 67)),
        ((225, 190, 231), (94, 53, 177)),
        ((178, 235, 242), (0, 151, 167)),
        ((255, 205, 210), (198, 40, 40)),
        ((26, 35, 126), (0, 0, 0)),
    ]
    top, bottom = palettes[seed % 5]
    img = gradient(top, bottom)
    lay = overlay(img)
    d = ImageDraw.Draw(lay)
    colors = [(255, 213, 79), (240, 98, 146), (79, 195, 247), (129, 199, 132), (255, 255, 255), (186, 104, 200)]
    for _ in range(160):
        x, y = rnd.randint(0, W), rnd.randint(0, H)
        c = rnd.choice(colors) + (220,)
        if rnd.random() < 0.5:
            d.rectangle([x, y, x + rnd.randint(6, 14), y + rnd.randint(14, 26)], fill=c)
        else:
            r = rnd.randint(4, 9)
            d.ellipse([x - r, y - r, x + r, y + r], fill=c)
    for _ in range(5):
        bx, by = rnd.randint(80, W - 80), rnd.randint(int(H * 0.55), H - 160)
        c = rnd.choice(colors)
        d.line([(bx, by + 70), (bx + rnd.randint(-30, 30), H)], fill=(255, 255, 255, 180), width=2)
        d.ellipse([bx - 52, by - 70, bx + 52, by + 70], fill=c + (235,))
        d.ellipse([bx - 30, by - 50, bx - 10, by - 20], fill=(255, 255, 255, 120))
    img.paste(lay, (0, 0), lay)
    return img


GENERATORS = {
    "morning": morning,
    "evening": evening,
    "flowers": flowers,
    "wisdom": wisdom,
    "jumuah": jumuah,
    "occasions": occasions,
}

for name, gen in GENERATORS.items():
    for i in range(1, 6):
        gen(i * 7 + len(name)).convert("RGB").save(f"{OUT}/img_{name}_{i}.webp", "WEBP", quality=82, method=6)
print("done")
