"""One-off generator for Picture Widget launcher + Play Store icon assets.
Source: ref/icon-idk-gpt.png. Not part of the app build.
"""
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "ref", "icon-idk-gpt.png")
RES = os.path.join(ROOT, "app", "src", "main", "res")
STORE = os.path.join(ROOT, "store")

src = Image.open(SRC).convert("RGBA")

# Adaptive icon foreground: full-bleed baked artwork, one per density bucket.
DENSITIES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}
for folder, size in DENSITIES.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)
    resized = src.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(out_dir, "ic_launcher_foreground.png"))

# Play Store listing icon: 512x512, PNG.
os.makedirs(STORE, exist_ok=True)
store_icon = src.resize((512, 512), Image.LANCZOS)
store_icon.convert("RGB").save(os.path.join(STORE, "play-store-icon-512.png"))

# Masked preview (circle / squircle / rounded-square) so we can eyeball
# launcher cropping before shipping.
PREVIEW = 320
base = src.resize((PREVIEW, PREVIEW), Image.LANCZOS)

def masked(shape):
    mask = Image.new("L", (PREVIEW, PREVIEW), 0)
    d = ImageDraw.Draw(mask)
    if shape == "circle":
        d.ellipse((0, 0, PREVIEW, PREVIEW), fill=255)
    elif shape == "squircle":
        # approximate squircle via high-radius rounded rect
        d.rounded_rectangle((0, 0, PREVIEW, PREVIEW), radius=int(PREVIEW * 0.32), fill=255)
    elif shape == "rounded_square":
        d.rounded_rectangle((0, 0, PREVIEW, PREVIEW), radius=int(PREVIEW * 0.16), fill=255)
    out = Image.new("RGBA", (PREVIEW, PREVIEW), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out

pad = 24
labels = ["circle", "squircle", "rounded_square"]
sheet = Image.new("RGBA", (PREVIEW * 3 + pad * 4, PREVIEW + pad * 2), (235, 230, 220, 255))
for i, shape in enumerate(labels):
    tile = masked(shape)
    x = pad + i * (PREVIEW + pad)
    sheet.paste(tile, (x, pad), tile)
sheet.convert("RGB").save(os.path.join(STORE, "icon-mask-preview.png"))

print("done")
