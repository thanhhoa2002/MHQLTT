import pytsk3
import os

def open_image(image_file):
    """Mở tệp ảnh và trả về một đối tượng TSK image."""
    if not os.path.isfile(image_file):
        print(f"Lỗi: Tệp {image_file} không tồn tại.")
        return None
    
    img_info = pytsk3.Img_Info(image_file)
    return img_info

def traverse_directory(fs_info, directory, parent_path=""):
    """Traverse the directory recursively and build a directory tree."""
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
    """Analyze an ext4 filesystem within the given image and find deleted files."""
    fs_info = pytsk3.FS_Info(img_info)
    root_dir = fs_info.open_dir(path="/")

    # Print the superblock information
    print("Kích thước block: ", fs_info.info.block_size)
    print("Tổng số block: ", fs_info.info.block_count)

    # Build the directory tree
    directory_tree = traverse_directory(fs_info, root_dir)
    
    return directory_tree

    

def print_directory_tree(directory_tree, indent=0):
    """Print the directory tree."""
    for key, value in directory_tree.items():
        if isinstance(value, dict):
            print("    " * indent + str(key))
            print_directory_tree(value, indent + 1)
        else:
            path, blocks = value
            block_info = ', '.join([f"(Block: [{block_addr}], Length: {block_len})" for block_addr, block_len in blocks])
            print("    " * indent + f"{key} {block_info}")

def extract_data_from_blocks(img_info, blocks):
    """Extract data from the specified blocks."""
    data = b""
    block_size = 4096  # Typical block size for ext4; adjust if necessary
    for block_addr, block_len in blocks:
        offset = block_addr * block_size
        length = block_len * block_size
        data += img_info.read(offset, length)
    return data

def find_file_in_tree(directory_tree, file_name):
    """Find the specified file in the directory tree."""
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


def main(image_path):
    img_info = open_image(image_path)
    

# Replace 'path_to_image_file.img' with the actual path to your .img file
main('D:\Disks\\30mar.img')