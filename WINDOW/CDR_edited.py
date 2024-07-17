from PIL import Image
def sanitize_lsb(input_path, output_path):
    # Mở tệp ảnh
    image = Image.open(input_path)
    image = image.convert("RGB")  # Đảm bảo ảnh ở chế độ RGB
    pixels = image.load()

    # Lặp qua từng pixel và làm mờ bit thấp nhất
    for i in range(image.size[0]):
        for j in range(image.size[1]):
            r, g, b = pixels[i, j]
            # Làm mờ bit thấp nhất của mỗi kênh màu
            r = (r & 0xFE) | 0
            g = (g & 0xFE) | 0
            b = (b & 0xFE) | 0
            pixels[i, j] = (r, g, b)
    
    # Lưu ảnh đã được làm sạch
    image.save(output_path, "PNG")

# Sử dụng ví dụ
input_png_path = "imageTestGPS.JPG"
output_png_path = "output_sanitized.png"

sanitize_lsb(input_png_path, output_png_path)

print(f"Ảnh PNG đã được làm sạch và lưu tại: {output_png_path}")