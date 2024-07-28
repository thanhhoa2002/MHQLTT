import pytsk3
import struct
import os

def open_image(image_file):
    """Mở tệp ảnh và trả về một đối tượng TSK image."""
    if not os.path.isfile(image_file):
        print(f"Lỗi: Tệp {image_file} không tồn tại.")
        return None
    
    img_info = pytsk3.Img_Info(image_file)
    return img_info

def find_deleted_files_by_hardlink(fs_info):
    """Find deleted files based on the number of hard links (nlink) and the block containing the inode."""
    deleted_files = []
    for inode in range(fs_info.info.first_inum, fs_info.info.last_inum + 1):
        try:
            entry = fs_info.open_meta(inode)
            if entry.info.meta:
                nlink = entry.info.meta.nlink

                if nlink == 0:
                    if entry.info.meta.type == pytsk3.TSK_FS_META_TYPE_REG:
                        inode_addr = entry.info.meta.addr  # The metadata address of the inode
                        deleted_files.append({
                            'inode': inode,
                        })
        except IOError:
            continue
    return deleted_files

def find_inode_block(img_info, inode_inum):
    fs_info = pytsk3.FS_Info(img_info)
    superblock_offset = 1024
    superblock = fs_info.info
    block_size = superblock.block_size
    superblock_size = 1024
    # print(superblock_size)

    superblock_data = img_info.read(superblock_offset,superblock_size)
    # print(superblock_data)
    
    inode_per_group_offset = 40
    inode_per_group = struct.unpack('<I', superblock_data[inode_per_group_offset:inode_per_group_offset+4])[0]
    # print(inode_per_group)
    
    inode_in_group = inode_inum // inode_per_group
    inode_per_block = block_size // 256
    # print(inode_per_block)
    # print(inode_in_group)

    block_group_descriptor_offset = block_size
    block_group_descriptor_size = 64
    block_group_descriptor_data = img_info.read(block_group_descriptor_offset+block_group_descriptor_size*inode_in_group,block_group_descriptor_size)
    # print(block_group_descriptor_data)
    lower_32_bits = struct.unpack('<I', block_group_descriptor_data[8:8+4])[0]
    # print(lower_32_bits)
    upper_32_bits = struct.unpack('<I', block_group_descriptor_data[40:40+4])[0]
    # print(upper_32_bits)
    combined_64_bit = (upper_32_bits << 32) | lower_32_bits
    # print(combined_64_bit)

    inode_in_block = combined_64_bit + inode_inum // inode_per_block
    # print(inode_in_block)
    return inode_in_block 

# def main(image_path):
#     img_info = open_image(image_path)
#     if img_info is None:
#         return

#     fs_info = pytsk3.FS_Info(img_info)

#     print("Kích thước block: ", fs_info.info.block_size)
#     print("Tổng số block: ", fs_info.info.block_count)

#     deleted_file = find_deleted_files_by_hardlink(fs_info) # đầu ra là 1 chuỗi gồm vị trí của các inode đã bị xoá
#     print(deleted_file)

#     find_inode_block(img_info ,deleted_file[1]['inode']) #input: vị trí của inode cần tìm + output: block mà inode hiện tại đang nằm trong đó

    

# main('D:/Disks/testDel3+4/30mar_4.img')
# print("=======================")
#main("D:/Disks/30mar.img")

