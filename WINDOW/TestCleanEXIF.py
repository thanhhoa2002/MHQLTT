from PIL import Image
import piexif
import os

def sanitize_image(input_path, output_path):
    # Mở một tệp ảnh
    image = Image.open(input_path)
    
    # Kiểm tra nếu tệp là JPEG để loại bỏ siêu dữ liệu
    if input_path.lower().endswith('.jpg') or input_path.lower().endswith('.jpeg'):
        # Lấy dữ liệu EXIF
        exif_data = piexif.load(image.info['exif']) if 'exif' in image.info else None
        if exif_data:
            # Tạo dữ liệu EXIF trống
            exif_bytes = piexif.dump({})
            # Lưu ảnh mà không có siêu dữ liệu EXIF
            image.save(output_path, "jpeg", exif=exif_bytes)
        else:
            image.save(output_path, "jpeg")
    else:
        # Lưu ảnh PNG mà không có siêu dữ liệu
        image.save(output_path, "png")

# Sử dụng ví dụ
input_jpg_path = "imageTestGPS.JPG"
output_jpg_path = "TEST.png"


sanitize_image(input_jpg_path, output_jpg_path)


print(f"Ảnh JPEG đã được làm sạch và lưu tại: {output_jpg_path}")

