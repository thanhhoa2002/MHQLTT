def all_zeros(byte_string):
    """Kiểm tra xem toàn bộ ký tự trong chuỗi byte có phải là \x00 hay không"""
    return all(byte == 0 for byte in byte_string)
def extract_images(file, sector_size):
    imgNumberJPEG = 0
    imgNumberPNG = 0

    while True:
        signature = file.read(8)
        if len(signature) < 8:
            print("Reached end of file")
            break
        
        # Check for JPEG signature (FFD8FF)
        if signature[:3] == b'\xFF\xD8\xFF':
            print(f"Found JPEG signature at: {file.tell() - 8}")
            imgNumberJPEG += 1
            file.seek(-8, 1)
            img = b""
            while True:
                data = file.read(sector_size)
                img += data
                if img.endswith(b'\x00'):
                    pos_ff_d9 = img.rfind(b'\xFF\xD9')
                    if pos_ff_d9 != -1:
                        sub_string = img[pos_ff_d9 + 2:]
                        if all_zeros(sub_string):
                            with open(f"imageJPEG{imgNumberJPEG}.jpeg", 'wb') as imgFile:
                                imgFile.write(img)
                            break        
        # Check for PNG signature (89504E470D0A1A0A)
        elif signature == b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A':
            print(f"Found PNG signature at: {file.tell() - 8}")
            imgNumberPNG += 1
            file.seek(-8, 1)
            img = b""
            while True:
                data = file.read(sector_size)
                img += data
                if img.endswith(b'\x00'):
                    pos_iend = img.rfind(b'\x49\x45\x4E\x44\xAE\x42\x60\x82')
                    if pos_iend != -1:
                        sub_string = img[pos_iend + 8:]
                        if all_zeros(sub_string):
                            with open(f"imagePNG{imgNumberPNG}.png", 'wb') as imgFile:
                                imgFile.write(img)
                            break

        else:
            # If neither JPEG nor PNG signature, move to the next sector
            file.seek(sector_size-8, 1)

def process_file(file_path, sector_size):
    try:
        with open(file_path, 'rb') as file:
            extract_images(file, sector_size)
    except FileNotFoundError:
        print(f"File not found: {file_path}")
    except Exception as e:
        print(f"An error occurred: {e}")

# Sử dụng hàm
#process_file('D:/disks/30mar.img',4096)

