"""Generate a 512x512 dice icon for Google Play Store."""
import struct
import zlib
import os

SIZE = 512
pixels = [[0, 0, 0, 0] for _ in range(SIZE * SIZE)]  # RGBA

def set_pixel(x, y, r, g, b, a=255):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        pixels[y * SIZE + x] = [r, g, b, a]

def blend_pixel(x, y, r, g, b, a):
    """Alpha blend onto existing pixel."""
    if 0 <= x < SIZE and 0 <= y < SIZE:
        idx = y * SIZE + x
        dst = pixels[idx]
        src_a = a / 255.0
        dst_a = dst[3] / 255.0
        out_a = src_a + dst_a * (1 - src_a)
        if out_a > 0:
            nr = int((r * src_a + dst[0] * dst_a * (1 - src_a)) / out_a)
            ng = int((g * src_a + dst[1] * dst_a * (1 - src_a)) / out_a)
            nb = int((b * src_a + dst[2] * dst_a * (1 - src_a)) / out_a)
            pixels[idx] = [nr, ng, nb, int(out_a * 255)]

def fill_rounded_rect(x1, y1, x2, y2, radius, r, g, b, a=255):
    """Fill a rounded rectangle with anti-aliasing."""
    for y in range(max(0, y1), min(SIZE, y2)):
        for x in range(max(0, x1), min(SIZE, x2)):
            # Check if inside rounded corners
            dx, dy = 0, 0
            if x < x1 + radius and y < y1 + radius:
                dx = x1 + radius - x
                dy = y1 + radius - y
            elif x >= x2 - radius and y < y1 + radius:
                dx = x - (x2 - radius - 1)
                dy = y1 + radius - y
            elif x < x1 + radius and y >= y2 - radius:
                dx = x1 + radius - x
                dy = y - (y2 - radius - 1)
            elif x >= x2 - radius and y >= y2 - radius:
                dx = x - (x2 - radius - 1)
                dy = y - (y2 - radius - 1)

            dist = (dx * dx + dy * dy) ** 0.5
            if dist <= radius - 1:
                set_pixel(x, y, r, g, b, a)
            elif dist <= radius:
                # Anti-alias edge
                alpha = int(a * (radius - dist))
                blend_pixel(x, y, r, g, b, alpha)

def fill_circle(cx, cy, radius, r, g, b, a=255):
    """Fill a circle with anti-aliasing."""
    for y in range(max(0, int(cy - radius - 1)), min(SIZE, int(cy + radius + 2))):
        for x in range(max(0, int(cx - radius - 1)), min(SIZE, int(cx + radius + 2))):
            dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if dist <= radius - 0.5:
                set_pixel(x, y, r, g, b, a)
            elif dist <= radius + 0.5:
                alpha = int(a * (radius + 0.5 - dist))
                blend_pixel(x, y, r, g, b, max(0, alpha))

def fill_gradient_bg():
    """Fill background with purple gradient."""
    for y in range(SIZE):
        for x in range(SIZE):
            t = (x + y) / (2 * SIZE)
            r = int(106 * (1 - t) + 74 * t)  # #6A1B9A -> #4A148C
            g = int(27 * (1 - t) + 20 * t)
            b = int(154 * (1 - t) + 140 * t)
            set_pixel(x, y, r, g, b)

# Draw
print("Filling gradient background...")
fill_gradient_bg()

# Dice shadow
print("Drawing dice shadow...")
fill_rounded_rect(72, 72, 452, 452, 36, 0, 0, 0, 40)

# Dice body (white rounded rectangle)
print("Drawing dice body...")
fill_rounded_rect(60, 60, 440, 440, 36, 255, 255, 255)

# Dice border
fill_rounded_rect(60, 60, 440, 440, 36, 200, 200, 200)

# Inner face (slightly inset)
fill_rounded_rect(72, 72, 428, 428, 28, 255, 255, 255)

# Purple dots (6 pattern - 2 columns, 3 rows)
print("Drawing dots...")
dot_positions = [
    (160, 150), (340, 150),  # top row
    (160, 250), (340, 250),  # middle row
    (160, 350), (340, 350),  # bottom row
]

for cx, cy in dot_positions:
    fill_circle(cx, cy, 38, 106, 27, 154)  # #6A1B9A

# Save as PNG
print("Saving PNG...")

def create_png(width, height, pixels):
    def chunk(chunk_type, data):
        c = chunk_type + data
        crc = struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)
        return struct.pack('>I', len(data)) + c + crc

    header = b'\x89PNG\r\n\x1a\n'
    ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))

    raw = b''
    for y in range(height):
        raw += b'\x00'  # filter none
        for x in range(width):
            p = pixels[y * width + x]
            raw += bytes(p)

    idat = chunk(b'IDAT', zlib.compress(raw, 9))
    iend = chunk(b'IEND', b'')
    return header + ihdr + idat + iend

output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'res')
png_data = create_png(SIZE, SIZE, pixels)

# Save to drawable
drawable_dir = os.path.join(output_dir, 'drawable')
os.makedirs(drawable_dir, exist_ok=True)
with open(os.path.join(drawable_dir, 'ic_launcher_playstore.png'), 'wb') as f:
    f.write(png_data)

print(f"Done! Saved to {os.path.join(drawable_dir, 'ic_launcher_playstore.png')}")
print(f"File size: {len(png_data)} bytes")
