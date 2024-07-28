import filetype

def get_file_extension(data):
    kind = filetype.guess(data)
    if kind is None:
        return "unknown"
    return kind.extension

# Ví dụ sử dụng:
# file_path = 'example.bin'
# with open(file_path, 'rb') as f:
#     data = f.read()
#     file_extension = get_file_extension(data)
#     print(f"Phần mở rộng tệp: {file_extension}")