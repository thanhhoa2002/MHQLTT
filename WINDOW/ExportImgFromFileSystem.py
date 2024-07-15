def all_zeros(byte_string):
    """Kiểm tra xem toàn bộ ký tự trong chuỗi byte có phải là \x00 hay không"""
    return all(byte == 0 for byte in byte_string)
def process_file(file_path):
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
                    #print("Reached end of file")
                    break
                # Kiểm tra 3 byte đầu có phải là FF D8 FF không
                if Signature == b'\xFF\xD8\xFF':
                    #print("Found FF D8 FF at: ",file.tell())
                    imgNumberJPEG = imgNumberJPEG + 1
                    file.seek(-3, 1)
                    while True:
                        data = file.read(512)
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
                    file.seek(509, 1)
                 
        with open(file_path, 'rb') as file:
            while True:
                Signature = file.read(8)  # Read the first 8 bytes
                # Kiểm tra nếu không đọc được 3 byte (kết thúc file)
                if len(Signature) < 8:
                    print("Reached end of file")
                    break
                # Kiểm tra 3 byte đầu có phải là FF D8 FF không
                if Signature == b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A':  # PNG signature
                    #print("Found PNG signature")
                    imgNumberPNG = imgNumberPNG + 1
                    file.seek(-8, 1)  # Move the pointer back 8 bytes
                    while True:
                        data = file.read(512)
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
                    file.seek(504, 1)
    except FileNotFoundError:
        print(f"File not found: {file_path}")
    except Exception as e:
        print(f"An error occurred: {e}")

# Sử dụng hàm
process_file('F.img')

