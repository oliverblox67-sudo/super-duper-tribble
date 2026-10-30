#!/usr/bin/env python3
"""Genera le texture e il logo di The Gods (pixel art procedurale).

Uso:  python3 tools/generate_assets.py
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")

PALETTE = {
    ".": None,
    "O": (11, 37, 69, 255),      # contorno blu notte
    "S": (242, 198, 160, 255),   # conchiglia
    "D": (201, 138, 112, 255),   # ombra
    "H": (255, 241, 222, 255),   # riflesso
    "C": (47, 184, 255, 255),    # bagliore del mare
    "W": (190, 250, 255, 255),   # cuore luminoso
}

CONCH = [
    "................",
    "...........O....",
    "..........OHO...",
    ".........OHSDO..",
    "........OOSDDO..",
    ".......OHSOOO...",
    ".....OOHSSSDO...",
    "....OHHSSSSDDO..",
    "...OHSSOOSSDDO..",
    "..OCWSSSSOSDDO..",
    "..OCCWSSSSSDO...",
    ".OCWCCSSSDDDO...",
    ".OCCCCDDDDDO....",
    "..OOCCDDDOO.....",
    "....OOOOO.......",
    "................",
]


def pixel_art(rows, path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            c = PALETTE[ch]
            if c:
                img.putpixel((x, y), c)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def font(size):
    for f in ["/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
              "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]:
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default(size)


def logo(path, size=512):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    # fondale: gradiente abissale
    for y in range(size):
        t = y / size
        r = int(4 + 10 * (1 - t)); g = int(20 + 60 * (1 - t) ** 1.5); b = int(48 + 110 * (1 - t))
        d.line([(0, y), (size, y)], fill=(r, g, b, 255))
    cx, cy = size // 2, int(size * 0.44)

    # ali d'acqua: ventagli di penne
    wings = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    wd = ImageDraw.Draw(wings)
    for side in (-1, 1):
        for i in range(9):
            f = i / 8
            ang = math.radians(70 - f * 115)
            length = size * (0.42 - f * 0.18)
            bx, by = cx + side * size * 0.05, cy - size * 0.02
            tx = bx + side * math.cos(ang) * length
            ty = by - math.sin(ang) * length
            w = size * (0.045 - f * 0.012)
            nx, ny = -math.sin(ang) * w, -math.cos(ang) * w
            col = (int(30 + 140 * (1 - f)), int(150 + 90 * (1 - f)), 255, 170)
            wd.polygon([(bx - side * nx, by + ny), (bx + side * nx, by - ny), (tx, ty)], fill=col)
    wings = wings.filter(ImageFilter.GaussianBlur(1.2))
    glow = wings.filter(ImageFilter.GaussianBlur(14))
    img.alpha_composite(glow)
    img.alpha_composite(wings)

    # tridente
    tri = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    td = ImageDraw.Draw(tri)
    gold = (255, 236, 170, 255)
    sw = size * 0.018
    td.rectangle([cx - sw, cy - size * 0.12, cx + sw, cy + size * 0.33], fill=gold)
    td.rectangle([cx - size * 0.11, cy - size * 0.13, cx + size * 0.11, cy - size * 0.10], fill=gold)
    for dx, top in ((-0.105, 0.27), (0.0, 0.33), (0.105, 0.27)):
        x = cx + dx * size
        td.polygon([(x - sw * 1.3, cy - size * 0.11), (x + sw * 1.3, cy - size * 0.11), (x, cy - size * top)], fill=gold)
    tglow = tri.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(tglow)
    img.alpha_composite(tri)

    # titolo
    f = font(int(size * 0.13))
    text = "THE GODS"
    tw = d.textlength(text, font=f)
    ty = int(size * 0.77)
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text(((size - tw) / 2, ty), text, font=f, fill=(47, 184, 255, 255))
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(8)))
    ImageDraw.Draw(img).text(((size - tw) / 2, ty), text, font=f, fill=(235, 250, 255, 255))
    img.convert("RGB").save(path)


if __name__ == "__main__":
    pixel_art(CONCH, os.path.join(RES, "assets", "thegods", "textures", "item", "poseidon_conch.png"))
    logo(os.path.join(RES, "thegods_logo.png"))
    logo(os.path.join(ROOT, "docs", "logo.png"))
    print("asset generati")


# ---------------------------------------------------------------- struttura vuota per i GameTest
def empty_structure(path, size=(9, 6, 9), data_version=3955):
    """Scrive una struttura NBT vuota (solo aria) in formato gzip."""
    import gzip
    import struct

    def name(n):
        b = n.encode()
        return struct.pack(">H", len(b)) + b

    def tag_int(n, v):
        return b"\x03" + name(n) + struct.pack(">i", v)

    def tag_list_int(n, vals):
        return b"\x09" + name(n) + b"\x03" + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)

    def tag_list_empty(n):
        return b"\x09" + name(n) + b"\x00" + struct.pack(">i", 0)

    def tag_string(n, s):
        b = s.encode()
        return b"\x08" + name(n) + struct.pack(">H", len(b)) + b

    palette = b"\x09" + name("palette") + b"\x0a" + struct.pack(">i", 1) + tag_string("Name", "minecraft:air") + b"\x00"
    root = b"\x0a" + name("") + tag_int("DataVersion", data_version) + tag_list_int("size", list(size)) \
        + palette + tag_list_empty("blocks") + tag_list_empty("entities") + b"\x00"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, "wb") as f:
        f.write(root)


if __name__ == "__main__":
    empty_structure(os.path.join(RES, "data", "thegods", "structure", "empty.nbt"))
    print("struttura gametest generata")
