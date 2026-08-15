import sys

def ascii_render(filename, w_out=60, h_out=25):
    with open(filename, "rb") as f:
        data = f.read()

    pixel_offset = int.from_bytes(data[10:14], 'little')
    width = int.from_bytes(data[18:22], 'little', signed=True)
    height = int.from_bytes(data[22:26], 'little', signed=True)
    bpp = int.from_bytes(data[28:30], 'little')
    row_stride = ((width * bpp + 31) // 32) * 4

    print(f"\n================ {filename} ({width}x{height}) ================")
    step_x = width / w_out
    step_y = height / h_out

    for gy in range(h_out):
        line = []
        y = int(gy * step_y)
        bmp_y = height - 1 - y
        for gx in range(w_out):
            x = int(gx * step_x)
            idx = pixel_offset + bmp_y * row_stride + x * (bpp // 8)
            b, g, r = data[idx], data[idx+1], data[idx+2]
            lum = 0.299*r + 0.587*g + 0.114*b
            if lum > 180:
                line.append("M")
            elif lum > 120:
                line.append("#")
            elif lum > 60:
                line.append(":")
            elif lum > 25:
                line.append(".")
            else:
                line.append(" ")
        print("".join(line))

for fn in ["crop_top.bmp", "crop_mid_left.bmp", "crop_mid_center.bmp", "crop_mid_right.bmp", "crop_bot_left.bmp", "crop_bot_center.bmp", "crop_bot_right.bmp"]:
    ascii_render(fn, 70, 20)
