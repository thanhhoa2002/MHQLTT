import struct

def extent_entries_analysis(data):
    ENTRY_SIZE = 12

    # Tính số lượng entries trong chuỗi byte
    num_entries = len(data) // ENTRY_SIZE
    #print(num_entries)

    results = []

    # Phân tích từng entry
    for i in range(1, num_entries):
        # Tính offset cho entry hiện tại
        offset = i * ENTRY_SIZE
        # Giải nén dữ liệu cho entry hiện tại
        ee_block, ee_len, ee_start_lo = struct.unpack('<III', data[offset:offset + ENTRY_SIZE])
        # Thêm vào kết quả
        results.append((ee_start_lo, ee_len))

    return results

# Ví dụ sử dụng
# example_data = bytes.fromhex("0A F3 01 00 04 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 02 82 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
# parsed_extents = extent_entries_analysis(example_data)
# print(parsed_extents)
