import pytsk3
import struct
import inode
import extent
import filetype
import time

def get_file_extension(data):
    kind = filetype.guess(data)
    if kind is None:
        return "unknown"
    return kind.extension

def get_journal_start_block(img_info):
    fs_info = pytsk3.FS_Info(img_info)
    BLOCK_SIZE = fs_info.info.block_size
    SUPERBLOCK_OFFSET = 1024
    try:
        # Đọc siêu khối
        superblock_data = img_info.read(SUPERBLOCK_OFFSET, BLOCK_SIZE)
        # print(superblock_data)

        # Offset của trường journal_inode (số inode của nhật ký) trong siêu khối
        JOURNAL_INODE_OFFSET = 0x120
        journal_start_block = struct.unpack('<I', superblock_data[JOURNAL_INODE_OFFSET:JOURNAL_INODE_OFFSET+4])[0]
        # print(journal_inum)
        
        if journal_start_block:
            print(f"Journal start block: {journal_start_block}")
            return journal_start_block
        else:
            print("Không tìm thấy thông tin về nhật ký trong siêu khối.")
            return None
    except Exception as e:
        print(f"Lỗi không mong muốn khi truy xuất vị trí nhật ký: {e}")
        return None

def TAG_analysis(descriptor_tags,journal_descriptor_start, inode_inum, file_number, file_img_path, fs_info, img_info):
    TAG_SIZE = 16
    inode_block = inode.find_inode_block(img_info,inode_inum)
    BLOCK_SIZE = fs_info.info.block_size
    i = 0
    number_of_tag = 0
    while i < 255:
        if i != 1:
            tag_blocknr = struct.unpack('>I', descriptor_tags[(TAG_SIZE*i):(TAG_SIZE*i)+4])[0]
            if tag_blocknr == inode_block:
                # print("match")
                inode_number = inode_inum % 16 - 1
                # print(inode_number)
                if i <= 2:
                    block_temp = journal_descriptor_start + 1 + i # block chứa dữ liệu của TAG = 1 block descriptor + so block cua moi tag
                else:
                    block_temp = journal_descriptor_start + 1 + i - 1
                #print(block_temp)
                inode_finding_offset = block_temp*BLOCK_SIZE+inode_number*256
                #print(inode_finding_offset)
                inode_finding_data = img_info.read(inode_finding_offset+40, 60)
                #print(inode_finding_data)
                data_block = extent.extent_entries_analysis(inode_finding_data)
                #print(data_block[0][0])
                #print(data_block[0][0] * BLOCK_SIZE)
                if data_block[0][0] != 0:
                    data_offset = data_block[0][0] * BLOCK_SIZE
                    data_len = data_block[0][1] * BLOCK_SIZE
                    if(data_len < 1000000000000):
                        with open(file_img_path, 'rb') as file:
                            file.seek(data_offset)
                            data = file.read(data_len)
                        file_extension = get_file_extension(data)
                        #print(file_extension)
                        #print(data)
                        with open(f"file{file_number}.{file_extension}", 'wb') as file:
                            file.write(data)
                        print("file recovery into", f"file{file_number}.{file_extension}")
                        return -1

            start = (TAG_SIZE*i)+4
            stop = start+4
            
            tag_flag = struct.unpack('>I', descriptor_tags[(TAG_SIZE*i)+4:(TAG_SIZE*i)+8])[0]
            if tag_flag >= 8:
                #print("ending tag")
                break
            if tag_flag == 0 and i != 0:
                i -= 1
                #print("ending tag")
                break
            i += 1
        else:
            i += 1
            continue
    if i == 0: 
        number_of_tag = i + 1
    else:
        number_of_tag = i
    # print("number_of_tag: ", number_of_tag)
    return number_of_tag

def journal_descriptor_analysis(img_info, journal_descriptor_start, inode_inum, file_number, file_img_path):
    fs_info = pytsk3.FS_Info(img_info)
    BLOCK_SIZE = fs_info.info.block_size
    inode_block = inode.find_inode_block(img_info,inode_inum)
    journal_descriptor_start_offset = journal_descriptor_start * BLOCK_SIZE

    journal_descriptor_data = img_info.read(journal_descriptor_start_offset,BLOCK_SIZE)
    #print(journal_descriptor_data)

    journal_descriptor_signature = struct.unpack('8s', journal_descriptor_data[:8])[0]
    # print(journal_descriptor_signature)

    if journal_descriptor_signature != b'\xc0;9\x98\x00\x00\x00\x01':
        #print("Wrong signature")
        return None

    descriptor_tags = journal_descriptor_data[12:]
    #print(descriptor_tags)

    number_of_tag = TAG_analysis(descriptor_tags, journal_descriptor_start, inode_inum, file_number, file_img_path, fs_info, img_info)
    return number_of_tag
    
def recovery(fs_info, img_info, journal_start_block, deleted_files, file_img_path):
    BLOCK_COUNT = fs_info.info.block_count
    BLOCK_SIZE = fs_info.info.block_size
    timeout_seconds=120
    start_time = time.time()  # Lưu thời điểm bắt đầu

    for i, file_info in enumerate(deleted_files):
        print(f"Searching journal for inode {file_info['inode']} file")
        inode_inum = file_info["inode"]
        journal_descriptor_first_start = journal_start_block + 1
        journal_descriptor_start = journal_descriptor_first_start
        number_of_tag = 0
        k = 0
        while journal_descriptor_start < BLOCK_COUNT:
            if k == 2000:
                print("File not found")
                break
            # Kiểm tra xem thời gian chạy có vượt quá timeout không
            if time.time() - start_time > timeout_seconds:
                print("File not found")
                break

            number_of_tag = journal_descriptor_analysis(img_info, journal_descriptor_start, inode_inum, i, file_img_path)
            if number_of_tag is not None and number_of_tag != -1:
                journal_descriptor_start = number_of_tag + 1 + 1 + journal_descriptor_start
            else:
                if number_of_tag == -1:
                    break
                number_of_tag = 0
                journal_descriptor_start += 1
                signature = img_info.read(journal_descriptor_start * BLOCK_SIZE, 8)
                inode_block = inode.find_inode_block(img_info, inode_inum)
                #print("Position: ", journal_descriptor_start)

                if signature == b'\xc0;9\x98\x00\x00\x00\x01':
                    descriptor_tags = img_info.read(journal_descriptor_start * BLOCK_SIZE + 12, BLOCK_SIZE - 12)
                    number_of_tag = TAG_analysis(descriptor_tags, journal_descriptor_start, inode_inum, i, file_img_path, fs_info, img_info) #TAG_analysis(descriptor_tags, journal_descriptor_start, inode_inum, file_number, file_img_path, fs_info)
                    if number_of_tag is None:
                        break
            k += 1


# # Ví dụ sử dụng:
# file_img_path = 'D:/Disks/testDel3+4/30mar_4.img'
# #img_info = pytsk3.Img_Info('D:/Disks/testDel3+4/testDel2_4.img')
# img_info = pytsk3.Img_Info('D:/Disks/testDel3+4/30mar_4.img')
# fs_info = pytsk3.FS_Info(img_info)
# journal_start_block = get_journal_start_block(img_info) #tìm vị trí journal

# deleted_files = inode.find_deleted_files_by_hardlink(fs_info)
# print("Deleted inode: ",deleted_files)


# recovery(fs_info, img_info, journal_start_block, deleted_files, file_img_path)        
            


def main_recovery(file_img_path):
    #img_info = pytsk3.Img_Info('D:/Disks/testDel3+4/testDel2_4.img') # 'D:/Disks/testDel3+4/30mar_4.img'
    img_info = pytsk3.Img_Info(file_img_path)
    fs_info = pytsk3.FS_Info(img_info)
    journal_start_block = get_journal_start_block(img_info) #tìm vị trí journal
    deleted_files = inode.find_deleted_files_by_hardlink(fs_info)
    print("Deleted inode: ",deleted_files)
    recovery(fs_info, img_info, journal_start_block, deleted_files, file_img_path)     
    pass



# file_img_path = ('D:/Disks/28july_2.img')
# main_recovery(file_img_path)