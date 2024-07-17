from PIL import Image
import piexif
import os
import pytsk3
import sys
import base64
import time
from bitarray import bitarray
from geopy.geocoders import Nominatim
from PIL.ExifTags import TAGS, GPSTAGS
from geopy.exc import GeocoderTimedOut, GeocoderServiceError

def sanitize_image(input_path, output_path):
    image = Image.open(input_path)
    if input_path.lower().endswith('.jpg') or input_path.lower().endswith('.jpeg'):
        exif_data = piexif.load(image.info['exif']) if 'exif' in image.info else None
        if (exif_data):
            exif_bytes = piexif.dump({})
            image.save(output_path, "jpeg", exif=exif_bytes)
        else:
            image.save(output_path, "jpeg")
    else:
        image.save(output_path, "png")

def sanitize_lsb(input_path, output_path):
    image = Image.open(input_path)
    image = image.convert("RGB")
    pixels = image.load()
    for i in range(image.size[0]):
        for j in range(image.size[1]):
            r, g, b = pixels[i, j]
            r = (r & 0xFE) | 0
            g = (g & 0xFE) | 0
            b = (b & 0xFE) | 0
            pixels[i, j] = (r, g, b)
    image.save(output_path, "PNG")

def open_image(image_file):
    img_info = pytsk3.Img_Info(image_file)
    return img_info

def traverse_directory(fs_info, directory, parent_path=""):
    directory_tree = {}
    for entry in directory:
        if entry.info.name.name in [b'.', b'..']:
            continue
        entry_name = entry.info.name.name.decode('utf-8')
        entry_path = f"{parent_path}/{entry_name}"
        if entry.info.meta and entry.info.meta.type == pytsk3.TSK_FS_META_TYPE_DIR:
            try:
                sub_directory = fs_info.open_dir(inode=entry.info.meta.addr)
                directory_tree[entry_name] = traverse_directory(fs_info, sub_directory, entry_path)
            except IOError:
                directory_tree[entry_name] = "<Unable to open directory>"
        else:
            blocks = []
            for attr in entry:
                if hasattr(attr.info, 'type') and attr.info.type == pytsk3.TSK_FS_ATTR_TYPE_DEFAULT:
                    for run in attr:
                        if run.addr != 0:
                            blocks.append((run.addr, run.len))
            directory_tree[entry_name] = (entry_path, blocks)
    return directory_tree

def analyze_ext4(img_info):
    fs_info = pytsk3.FS_Info(img_info)
    root_dir = fs_info.open_dir(path="/")
    directory_tree = traverse_directory(fs_info, root_dir)
    return directory_tree

def print_directory_tree(directory_tree, indent=0):
    for key, value in directory_tree.items():
        if isinstance(value, dict):
            print("    " * indent + str(key))
            print_directory_tree(value, indent + 1)
        else:
            path, blocks = value
            block_info = ', '.join([f"(Block: [{block_addr}], Length: {block_len})" for block_addr, block_len in blocks])
            print("    " * indent + f"{key} {block_info}")

def extract_data_from_blocks(img_info, blocks):
    data = b""
    block_size = 4096
    for block_addr, block_len in blocks:
        offset = block_addr * block_size
        length = block_len * block_size
        data += img_info.read(offset, length)
    return data

def find_file_in_tree(directory_tree, file_name):
    for key, value in directory_tree.items():
        if isinstance(value, dict):
            result = find_file_in_tree(value, file_name)
            if result:
                return result
        else:
            path, blocks = value
            if key == file_name:
                return path, blocks
    return None

def process_file(file_path,sector_size):
    imgNumberJPEG = 0
    imgNumberPNG = 0
    try:
        with open(file_path, 'rb') as file:
            while True:
                # Đọc 3 byte
                Signature = file.read(3)
                img = b""
                # Kiểm tra nếu không đọc được 3 byte (kết thúc file)
                if len(Signature) < 3:
                    print("Reached end of file")
                    break
                # Kiểm tra 3 byte đầu có phải là FF D8 FF không
                if Signature == b'\xFF\xD8\xFF':
                    print("Found FF D8 FF at: ",file.tell())
                    imgNumberJPEG = imgNumberJPEG + 1
                    file.seek(-3, 1)
                    while True:
                        data = file.read(sector_size)
                        img = img + data
                        if img.endswith(b'\x00'):
                            pos_ff_d9 = img.rfind(b'\xFF\xD9')
                            sub_string = img[pos_ff_d9 + 2:-1]
                            if all_zeros(sub_string):
                                with open(f"imageJPEG{imgNumberJPEG}.jpeg", 'wb') as imgFile:
                                    imgFile.write(img)
                                    break
                            
                else:
                    # Dịch chuyển con trỏ thêm 509 byte
                    file.seek(sector_size-3, 1)
                    
        with open(file_path, 'rb') as file:
            while True:
                Signature = file.read(8)  # Read the first 8 bytes
                # Kiểm tra nếu không đọc được 3 byte (kết thúc file)
                if len(Signature) < 8:
                    print("Reached end of file")
                    break
                # Kiểm tra 3 byte đầu có phải là FF D8 FF không
                if Signature == b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A':  # PNG signature
                    print("Found PNG signature")
                    imgNumberPNG = imgNumberPNG + 1
                    file.seek(-8, 1)  # Move the pointer back 8 bytes
                    while True:
                        data = file.read(sector_size)
                        img = img + data
                        if img.endswith(b'\x00'):
                            pos_iend = img.rfind(b'\x49\x45\x4E\x44\xAE\x42\x60\x82')  # IEND chunk
                            sub_string = img[pos_iend + 8:-1]
                            if all_zeros(sub_string):
                                with open(f"imagePNG{imgNumberPNG}.png", 'wb') as imgFile:
                                    imgFile.write(img)
                                    break
                else:
                    # Dịch chuyển con trỏ thêm 509 byte
                    file.seek(sector_size-8, 1)
    except FileNotFoundError:
        print(f"File not found: {file_path}")
    except Exception as e:
        print(f"An error occurred: {e}")

def all_zeros(byte_string):
    return all(byte == 0 for byte in byte_string)

def reverse_geocode(lat, lon, retries=3, delay=2):
    geolocator = Nominatim(user_agent="myGeocoder", timeout=10)  # Thời gian chờ là 10 giây
    for i in range(retries):
        try:
            location = geolocator.reverse((lat, lon), exactly_one=True)
            return location.address if location else "Không tìm thấy địa chỉ cho tọa độ này."
        except (GeocoderTimedOut, GeocoderServiceError):
            print(f"Thử lại lần {i + 1}/{retries} sau {delay} giây...")
            time.sleep(delay)
    return "Lỗi kết nối hoặc dịch vụ không khả dụng."

def get_exif_data(image_path):
    image = Image.open(image_path)
    exif_data = image._getexif()
    gps_data = {}
    if exif_data is not None:
        for tag, value in exif_data.items():
            tag_name = TAGS.get(tag, tag)
            if tag_name == "GPSInfo":
                for gps_tag in value:
                    gps_tag_name = GPSTAGS.get(gps_tag, gps_tag)
                    gps_data[gps_tag_name] = value[gps_tag]
    return gps_data

def get_decimal_from_dms(dms_tuple):
    degrees, minutes, seconds = dms_tuple
    return degrees + minutes / 60.0 + seconds / 3600.0

def gps_data_to_coordinates(gps_data):
    if gps_data:
        lat_dms = gps_data.get("GPSLatitude")
        lat_ref = gps_data.get("GPSLatitudeRef")
        lon_dms = gps_data.get("GPSLongitude")
        lon_ref = gps_data.get("GPSLongitudeRef")
        if lat_dms and lon_dms and lat_ref and lon_ref:
            lat = get_decimal_from_dms(lat_dms)
            lon = get_decimal_from_dms(lon_dms)
            if lat_ref == "S":
                lat = -lat
            if lon_ref == "W":
                lon = -lon
            return lat, lon
    return None

def extract_bits_from_image(image_path):
    image = Image.open(image_path)
    pixels = image.load()
    extracted_bits = ''

    for x in range(image.width):
        r, g, b = pixels[x, 0]
        extracted_bits += bin(r)[-1]
        extracted_bits += bin(g)[-1]
        extracted_bits += bin(b)[-1]

    return extracted_bits

def bits_to_bytes(bits):
    chars = []
    for i in range(0, len(bits), 8):
        byte = bits[i:i + 8]
        if len(byte) < 8:
            break
        chars.append(chr(int(byte, 2)))
    return ''.join(chars)

def clean_byte_string(byte_string):
    return ''.join(c for c in byte_string if ord(c) < 128)

def decode_message_from_bits(bits):
    byte_string = bits_to_bytes(bits)
    cleaned_byte_string = clean_byte_string(byte_string)
    try:
        decoded_message = base64.b64decode(cleaned_byte_string.encode('ascii')).decode('utf-8')
    except (base64.binascii.Error, UnicodeDecodeError, ValueError) as e:
        raise ValueError(f"Decoding failed: {e}")
    return decoded_message

def extract_hidden_message(image_path):
    extracted_bits = extract_bits_from_image(image_path)
    message = decode_message_from_bits(extracted_bits)
    return message

def encode_message_to_bits(message):
    encoded_message = base64.b64encode(message.encode('utf-8'))
    ba = bitarray()
    ba.frombytes(encoded_message)
    return [int(i) for i in ba]

def modify_lsb(color, bit):
    color_bin = bin(color)
    new_color_bin = color_bin[:-1] + str(bit)
    return int(new_color_bin, 2)

def hide_message_in_image(image_path, message, output_path):
    bit_array = encode_message_to_bits(message)
    im = Image.open(image_path)
    im = im.convert("RGB")
    width, height = im.size
    pixels = im.load()
    i = 0

    for x in range(width):
        if i >= len(bit_array):
            break
        r, g, b = pixels[x, 0]
        new_r = modify_lsb(r, bit_array[i]) if i < len(bit_array) else r
        i += 1
        new_g = modify_lsb(g, bit_array[i]) if i < len(bit_array) else g
        i += 1
        new_b = modify_lsb(b, bit_array[i]) if i < len(bit_array) else b
        i += 1
        pixels[x, 0] = (new_r, new_g, new_b)

    im.save(output_path)

def menu():
    while True:
        print("\nMenu:")
        print("1. Xuất cây thư mục")
        print("2. Truy xuất file (theo tên)")
        print("3. Phục hồi file ảnh PNG, JPEG trên mọi loại hệ thống tập tin")
        print("4. Trích xuất vị trí cụ thể của ảnh (nếu có)")
        print("5. Xoá thông tin EXIF của ảnh")
        print("6. Tái cấu trúc ảnh")
        print("7. Ẩn thông điệp trong ảnh(chỉ PNG)")
        print("8. Đọc thông điệp ẩn trong ảnh(chỉ PNG)")
        print("0. Thoát")
        
        choice = input("Chọn một tuỳ chọn: ")
        
        if choice == "1":
            image_path = input("Nhập đường dẫn đến file image EXT4: ")
            img_info = open_image(image_path)
            directory_tree = analyze_ext4(img_info)
            print("Cây thư mục:")
            print_directory_tree(directory_tree)
        
        elif choice == "2":
            image_path = input("Nhập đường dẫn đến file image EXT4: ")
            file_name = input("Nhập tên tệp cần truy xuất: ")
            img_info = open_image(image_path)
            directory_tree = analyze_ext4(img_info)
            file_info = find_file_in_tree(directory_tree, file_name)
            if file_info:
                path, blocks = file_info
                data = extract_data_from_blocks(img_info, blocks)
                with open(file_name, 'wb') as f:
                    f.write(data)
                print(f"\nĐã trích xuất dữ liệu vào '{file_name}'")
            else:
                print(f"\nKhông tìm thấy tệp '{file_name}' trong cây thư mục.")
        
        elif choice == "3":
            file_path = input("Nhập đường dẫn đến tệp file image EXT4 cần phục hồi: ")


            sector_size = int(input("Nhập kích thước sector: "))

            process_file(file_path, sector_size)
        
        elif choice == "4":
            image_path = input("Nhập đường dẫn đến tệp JPEG: ")
            # Lấy dữ liệu GPS từ tệp JPEG
            gps_data = get_exif_data(image_path)
            
            # Chuyển đổi dữ liệu GPS thành tọa độ
            coordinates = gps_data_to_coordinates(gps_data)
            
            if coordinates:
                lat, lon = coordinates
                print("Latitude:", lat)
                print("Longitude:", lon)
                
                # Lấy địa chỉ từ tọa độ
                location = reverse_geocode(lat, lon)
                
                # In địa chỉ
                print(location)
            else:
                print("Không có dữ liệu GPS trong tệp JPEG này.")
        
        elif choice == "5":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã xoá thông tin EXIF: ")
            sanitize_image(input_path, output_path)
            print(f"Đã xoá thông tin EXIF của ảnh và lưu tại '{output_path}'")
        
        elif choice == "6":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã tái cấu trúc: ")
            sanitize_lsb(input_path, output_path)
            print(f"Đã tái cấu trúc ảnh và lưu tại '{output_path}'")
        
        elif choice == "7":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            message = input("Nhập thông điệp cần ẩn: ")
            output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã ẩn thông điệp: ")
            hide_message_in_image(input_path, message, output_path)
            print(f"Đã ẩn thông điệp trong ảnh và lưu tại '{output_path}'")
        
        elif choice == "8":
            image_path = input("Nhập đường dẫn đến tệp hình ảnh: ")
            try:
                hidden_message = extract_hidden_message(image_path)
                print("Thông điệp ẩn:", hidden_message)
            except ValueError as e:
                print(f"An error occurred: {e}")
        
        elif choice == "0":
            print("Thoát chương trình.")
            break
        
        else:
            print("Lựa chọn không hợp lệ, vui lòng chọn lại.")

        input("Ấn nút bất kỳ để tiếp tục")

        

if __name__ == "__main__":
    menu()