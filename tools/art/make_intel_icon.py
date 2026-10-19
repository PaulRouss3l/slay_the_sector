"""Draws graphics/icons/intel/sts_descent.png (40x40): the Hub intel icon.

Dark round badge, cyan rim, three downward chevrons (the Descent) fading toward a Node dot.
Drawn at 8x and downsampled for anti-aliasing. Run: python3 tools/art/make_intel_icon.py
"""
import os
from PIL import Image, ImageDraw, ImageFilter

S = 8
N = 40 * S
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "graphics", "icons", "intel", "sts_descent.png")


def main():
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = N / 2
    r = N / 2 - 1.5 * S
    # Glow under the rim.
    glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([c - r, c - r, c + r, c + r], outline=(60, 200, 255, 200), width=3 * S)
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(2 * S)))
    d = ImageDraw.Draw(img)
    d.ellipse([c - r, c - r, c + r, c + r], fill=(10, 22, 34, 255), outline=(90, 215, 255, 255), width=int(1.5 * S))

    # Chevrons, brightest on top, sinking toward the Node.
    colors = [(255, 200, 80, 255), (255, 150, 60, 235), (230, 90, 50, 215)]
    w = 11 * S
    h = 5 * S
    t = int(2.2 * S)
    y = c - 12 * S
    for col in colors:
        d.line([(c - w, y), (c, y + h), (c + w, y)], fill=col, width=t, joint="curve")
        y += 7 * S
        w -= 2 * S
    # The Node waiting at the bottom.
    nr = 2.3 * S
    ny = c + 11.5 * S
    d.ellipse([c - nr, ny - nr, c + nr, ny + nr], fill=(255, 70, 60, 255))

    img.resize((40, 40), Image.LANCZOS).save(OUT)
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
