"""Draws graphics/icons/intel/sts_descent.png (40x40): the Hub intel icon.

Dark round badge, cyan rim, gold "STS" lettering over a small downward chevron (the Descent).
Drawn at 8x and downsampled for anti-aliasing. Run: python3 tools/art/make_intel_icon.py
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = 8
N = 40 * S
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "graphics", "icons", "intel", "sts_descent.png")


def main():
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    c = N / 2
    r = N / 2 - 1.5 * S
    # Glow under the rim.
    glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([c - r, c - r, c + r, c + r], outline=(60, 200, 255, 200), width=3 * S)
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(2 * S)))
    d = ImageDraw.Draw(img)
    d.ellipse([c - r, c - r, c + r, c + r], fill=(10, 22, 34, 255), outline=(90, 215, 255, 255), width=int(1.5 * S))

    # "STS": a vertical gold-to-orange gradient masked by the text, with a dark outline.
    font = ImageFont.truetype(FONT, 13 * S)
    ty = c - 3 * S
    outline = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(outline).text((c, ty), "STS", font=font, anchor="mm", fill=(0, 0, 0, 255),
                                 stroke_width=int(1.2 * S), stroke_fill=(0, 0, 0, 255))
    img = Image.alpha_composite(img, outline)
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).text((c, ty), "STS", font=font, anchor="mm", fill=255)
    grad = Image.new("RGBA", (N, N))
    gd = ImageDraw.Draw(grad)
    top, bottom = ty - 6.5 * S, ty + 6.5 * S
    for y in range(N):
        k = min(1.0, max(0.0, (y - top) / (bottom - top)))
        gd.line([(0, y), (N, y)], fill=(255, int(215 - 85 * k), int(90 - 40 * k), 255))
    img.paste(grad, (0, 0), mask)

    # Small chevron under the letters: the way down.
    d = ImageDraw.Draw(img)
    w, h, y = 5 * S, 3 * S, c + 6 * S
    d.line([(c - w, y), (c, y + h), (c + w, y)], fill=(230, 90, 50, 255), width=int(1.8 * S), joint="curve")

    img.resize((40, 40), Image.LANCZOS).save(OUT)
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
