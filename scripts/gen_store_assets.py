"""Mockup Play Store assets for Picture Widget: feature graphic + placeholder
screenshots + listing copy. Not final art, not part of the app build.
"""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "store")
os.makedirs(STORE, exist_ok=True)

# Palette (matches app / marketing site)
CREAM = (251, 250, 247)
CREAM_DARK = (245, 241, 229)
SURFACE = (255, 255, 255)
TEXT = (28, 26, 23)
MUTED = (95, 90, 82)
ACCENT = (154, 79, 44)
ACCENT_DARK = (122, 61, 32)
BORDER = (231, 225, 216)
BROWN = (58, 42, 30)

FONT_DIR = "C:/Windows/Fonts"


def font(name, size):
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


def torn_polygon(x, y, w, h, jitter=6, seed=0):
    rnd = random.Random(seed)
    pts = []
    step = 14
    # top edge
    for px in range(0, w + 1, step):
        pts.append((x + px, y + rnd.randint(-jitter, jitter)))
    # right edge
    for py in range(0, h + 1, step):
        pts.append((x + w + rnd.randint(-jitter, jitter), y + py))
    # bottom edge
    for px in range(w, -1, -step):
        pts.append((x + px, y + h + rnd.randint(-jitter, jitter)))
    # left edge
    for py in range(h, -1, -step):
        pts.append((x + rnd.randint(-jitter, jitter), y + py))
    return pts


def paste_rotated(base, layer, cx, cy, angle):
    rotated = layer.rotate(angle, expand=True, resample=Image.BICUBIC)
    w, h = rotated.size
    base.paste(rotated, (int(cx - w / 2), int(cy - h / 2)), rotated)


def torn_photo(w, h, seed, colors):
    pad = 30
    im = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    poly = torn_polygon(pad, pad, w, h, jitter=7, seed=seed)
    sd.polygon(poly, fill=(30, 20, 12, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(8))
    im.paste(shadow, (6, 10), shadow)
    d.polygon(poly, fill=CREAM)
    # simple layered "scene" fill inside a slight inset of the torn poly
    inset = torn_polygon(pad + 6, pad + 6, w - 12, h - 12, jitter=5, seed=seed + 1)
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).polygon(inset, fill=255)
    scene = Image.new("RGBA", im.size, (0, 0, 0, 0))
    sc = ImageDraw.Draw(scene)
    top, horizon, ground = colors
    sc.rectangle([pad, pad, pad + w, pad + h * 0.55], fill=top)
    sc.rectangle([pad, pad + h * 0.4, pad + w, pad + h * 0.7], fill=horizon)
    sc.rectangle([pad, pad + h * 0.6, pad + w, pad + h], fill=ground)
    im.paste(scene, (0, 0), Image.composite(scene, Image.new("RGBA", im.size, (0, 0, 0, 0)), mask))
    d.line(poly + [poly[0]], fill=BROWN, width=3, joint="curve")
    return im


def status_bar(d, w, y=0, dark_icons=True):
    col = TEXT if dark_icons else (255, 255, 255)
    f = font("segoeui.ttf", 26)
    d.text((36, y + 16), "9:41", font=f, fill=col)
    d.text((w - 140, y + 16), "●●●", font=font("segoeui.ttf", 18), fill=col)


def rounded(d, box, r, **kw):
    d.rounded_rectangle(box, radius=r, **kw)


# ---------------------------------------------------------------------------
# Screenshot 1: Editor with torn-paper collage + clock + toolbar
# ---------------------------------------------------------------------------
W, H = 1080, 1920
im = Image.new("RGB", (W, H), CREAM)
d = ImageDraw.Draw(im)
status_bar(d, W)

# top app bar
d.rectangle([0, 0, W, 130], fill=SURFACE)
d.line([0, 130, W, 130], fill=BORDER, width=2)
d.text((36, 60), "←", font=font("segoeui.ttf", 40), fill=TEXT)
d.text((W / 2 - 130, 55), "Edit collage", font=font("segoeuib.ttf", 40), fill=TEXT)
d.line([(W - 100, 78), (W - 84, 96), (W - 50, 56)], fill=ACCENT, width=7, joint="curve")

board = torn_photo(420, 520, seed=1, colors=[(240, 205, 168), (196, 120, 76), (92, 56, 34)])
paste_rotated(im, board, 330, 620, -8)
board2 = torn_photo(380, 300, seed=5, colors=[(214, 176, 138), (168, 96, 60), (74, 46, 30)])
paste_rotated(im, board2, 760, 420, 10)
board3 = torn_photo(360, 460, seed=9, colors=[(232, 196, 150), (176, 104, 58), (66, 40, 26)])
paste_rotated(im, board3, 700, 1060, -6)

# clock overlay
clock_font = font("segoeuib.ttf", 96)
d.text((70, 1400), "10:24", font=clock_font, fill=ACCENT_DARK)

# bottom toolbar
d.rectangle([0, H - 220, W, H], fill=SURFACE)
d.line([0, H - 220, W, H - 220], fill=BORDER, width=2)
labels = ["Front", "Back", "Copy", "Delete"]
for i, lb in enumerate(labels):
    cx = 160 + i * 260
    cy = H - 115
    d.ellipse([cx - 45, cy - 45, cx + 45, cy + 45], outline=ACCENT, width=3)
    if lb == "Front":
        d.polygon([(cx, cy - 20), (cx - 18, cy + 8), (cx + 18, cy + 8)], fill=ACCENT)
    elif lb == "Back":
        d.polygon([(cx, cy + 20), (cx - 18, cy - 8), (cx + 18, cy - 8)], fill=ACCENT)
    elif lb == "Copy":
        d.rounded_rectangle([cx - 18, cy - 18, cx + 10, cy + 10], radius=4, outline=ACCENT, width=4)
        d.rounded_rectangle([cx - 8, cy - 8, cx + 20, cy + 20], radius=4, outline=ACCENT, width=4, fill=SURFACE)
    elif lb == "Delete":
        d.line([(cx - 18, cy - 18), (cx + 18, cy + 18)], fill=ACCENT, width=6)
        d.line([(cx - 18, cy + 18), (cx + 18, cy - 18)], fill=ACCENT, width=6)
    d.text((cx - 40, H - 60), lb, font=font("segoeui.ttf", 22), fill=MUTED)

# floating action button
rounded(d, [W - 320, 1180, W - 40, 1260], 40, fill=ACCENT_DARK)
d.text((W - 300, 1200), "Add to home screen", font=font("segoeuib.ttf", 26), fill=(255, 255, 255))

im.save(os.path.join(STORE, "screenshot-1-editor.png"))

# ---------------------------------------------------------------------------
# Screenshot 2: Home screen with pinned widget
# ---------------------------------------------------------------------------
im2 = Image.new("RGB", (W, H), (32, 40, 46))
d2 = ImageDraw.Draw(im2)
# faux wallpaper gradient
for yy in range(H):
    t = yy / H
    r = int(24 + t * 30)
    g = int(30 + t * 34)
    b = int(38 + t * 46)
    d2.line([(0, yy), (W, yy)], fill=(r, g, b))
status_bar(d2, W, dark_icons=False)

# widget card
wx, wy, ww, wh = 90, 420, W - 180, 640
rounded(d2, [wx, wy, wx + ww, wy + wh], 44, fill=CREAM)
d2.text((wx + 40, wy + 30), "10:24", font=font("segoeuib.ttf", 64), fill=ACCENT_DARK)
mini = torn_photo(ww - 140, wh - 220, seed=3, colors=[(232, 196, 150), (176, 104, 58), (66, 40, 26)])
mini = mini.resize((int(mini.width * 0.72), int(mini.height * 0.72)))
im2.paste(mini, (wx + 60, wy + 150), mini)

# app icon grid below widget
for row in range(3):
    for col in range(4):
        cx = 140 + col * 230
        cy = 1220 + row * 220
        rounded(d2, [cx, cy, cx + 130, cy + 130], 30, fill=(255, 255, 255, 40) if False else (230, 224, 214))
d2.text((90, H - 90), "Long-press to add the Picture Widget", font=font("segoeui.ttf", 26), fill=(230, 224, 214))

im2.save(os.path.join(STORE, "screenshot-2-homescreen.png"))

# ---------------------------------------------------------------------------
# Screenshot 3: Layers + background picker sheet
# ---------------------------------------------------------------------------
im3 = Image.new("RGB", (W, H), CREAM)
d3 = ImageDraw.Draw(im3)
status_bar(d3, W)
d3.rectangle([0, 0, W, 130], fill=SURFACE)
d3.line([0, 130, W, 130], fill=BORDER, width=2)
d3.text((36, 55), "←", font=font("segoeui.ttf", 40), fill=TEXT)
d3.text((W / 2 - 100, 55), "Board settings", font=font("segoeuib.ttf", 38), fill=TEXT)

bg_board = torn_photo(500, 560, seed=2, colors=[(214, 176, 138), (168, 96, 60), (74, 46, 30)])
paste_rotated(im3, bg_board, W / 2, 480, 4)

sheet_y = 900
rounded(d3, [0, sheet_y, W, H], 0, fill=SURFACE)
d3.rounded_rectangle([0, sheet_y, W, sheet_y + 60], radius=30, fill=SURFACE)
d3.rounded_rectangle([W / 2 - 60, sheet_y + 22, W / 2 + 60, sheet_y + 34], radius=8, fill=BORDER)

d3.text((60, sheet_y + 60), "Layers", font=font("segoeuib.ttf", 34), fill=TEXT)
layer_names = ["Photo cutout 3", "Photo cutout 2", "Clock", "Photo cutout 1"]
for i, name in enumerate(layer_names):
    ly = sheet_y + 130 + i * 90
    rounded(d3, [50, ly, W - 50, ly + 74], 18, fill=CREAM_DARK)
    rounded(d3, [70, ly + 12, 120, ly + 62], 12, fill=ACCENT)
    d3.text((150, ly + 18), name, font=font("segoeui.ttf", 30), fill=TEXT)
    d3.text((W - 150, ly + 16), "↑", font=font("segoeui.ttf", 30), fill=MUTED)
    d3.text((W - 100, ly + 16), "↓", font=font("segoeui.ttf", 30), fill=MUTED)

d3.text((60, sheet_y + 500), "Background", font=font("segoeuib.ttf", 34), fill=TEXT)
swatches = [
    ("None", CREAM_DARK),
    ("Paper", (223, 200, 168)),
    ("Cork", (176, 128, 78)),
    ("Wood", (120, 78, 46)),
    ("Linen", (214, 206, 188)),
]
for i, (name, col) in enumerate(swatches):
    sx = 70 + i * 195
    sy = sheet_y + 570
    rounded(d3, [sx, sy, sx + 150, sy + 150], 24, fill=col, outline=BORDER, width=3)
    d3.text((sx + 20, sy + 160), name, font=font("segoeui.ttf", 24), fill=MUTED)

im3.save(os.path.join(STORE, "screenshot-3-layers.png"))

# ---------------------------------------------------------------------------
# Feature graphic 1024x500
# ---------------------------------------------------------------------------
FW, FH = 1024, 500
fg = Image.new("RGB", (FW, FH), CREAM)
fd = ImageDraw.Draw(fg)
fd.rectangle([0, 0, FW, FH], fill=CREAM)

src_path = os.path.join(ROOT, "ref", "icon-idk-gpt.png")
icon = Image.open(src_path).convert("RGBA").resize((420, 420), Image.LANCZOS)
fg.paste(icon, (-70, 40), icon)

fd.text((420, 130), "Picture Widget", font=font("segoeuib.ttf", 72), fill=TEXT)
fd.text((424, 220), "Photos, torn into paper-cutout collages", font=font("segoeui.ttf", 34), fill=MUTED)
fd.text((424, 280), "for your home screen.", font=font("segoeui.ttf", 34), fill=MUTED)

fg.save(os.path.join(STORE, "feature-graphic-1024x500.png"))

# ---------------------------------------------------------------------------
# Listing copy
# ---------------------------------------------------------------------------
short_desc = "Turn photos into hand-torn paper cutouts and pin a collage to your home screen"

full_desc = """Picture Widget turns your photos into hand-torn paper cutouts and arranges them as a collage, right on your home screen.

Pick a few photos and each one is cut out on-device, with no server and no upload. Every cutout gets an irregular, hand-torn paper edge. Arrange the pieces into a collage: drag, pinch, rotate, resize, and layer them however you like. Add a live clock that is ticked by the system, so it never costs battery. Then pin the finished collage to your home screen as a resizable widget.

FEATURES
- On-device photo cutout: your photos are processed entirely on your phone, never uploaded anywhere
- Hand-torn paper styling for a tactile, collage-book look
- Full manual control: drag, pinch, rotate, resize, reorder layers
- Backgrounds: transparent, your own photo, gradients, or generated paper, cork, wood and linen textures
- A live clock overlay you can place anywhere on the board
- Text pieces, so you can add captions or labels to your collage
- Prebuilt templates to start from
- As many home screen widgets as you like, each with its own collage
- Light and dark themes, with a choice of accent colours

PRIVACY
Picture Widget does not request internet access and does not collect, transmit, or store any personal data. Everything happens on your device: background removal, paper cutout rendering, and collage layout. See the privacy policy for details.

Questions or feedback? Use the in-app Feedback option, or email bikash13763@gmail.com."""

with open(os.path.join(STORE, "listing-copy.txt"), "w", encoding="utf-8") as f:
    f.write("SHORT DESCRIPTION (%d/80 chars)\n" % len(short_desc))
    f.write(short_desc + "\n\n")
    f.write("FULL DESCRIPTION (%d/4000 chars)\n" % len(full_desc))
    f.write(full_desc + "\n")

print("short_desc len:", len(short_desc))
print("full_desc len:", len(full_desc))
print("done")
