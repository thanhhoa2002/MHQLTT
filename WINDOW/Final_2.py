import ReadPartition_test
import ExportImgFromFileSystem
import ConvertGPS
import TestCleanEXIF
import CDR_edited
import hide_message_in_image
import extract_hidden_in_message
import recover_ext4
import os

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
        print("9. Phục hồi file từ file image EXT4")
        print("0. Thoát")
        choice = input("Chọn một tuỳ chọn: ")
        
        if choice == "1":
            image_path = input("Nhập đường dẫn đến file image EXT4: ")
            img_info = ReadPartition_test.open_image(image_path)
            if not img_info == None:
                directory_tree = ReadPartition_test.analyze_ext4(img_info)
                ReadPartition_test.print_directory_tree(directory_tree)
        
        elif choice == "2":
            image_path = input("Nhập đường dẫn đến file image EXT4: ")
            img_info = ReadPartition_test.open_image(image_path)
            
            if not img_info == None:
                
                directory_tree = ReadPartition_test.analyze_ext4(img_info)
                ReadPartition_test.print_directory_tree(directory_tree)
                file_name = input("Nhập tên tệp cần truy xuất: ")
                file_info = ReadPartition_test.find_file_in_tree(directory_tree, file_name)
                if file_info:
                    path, blocks = file_info
                    data = ReadPartition_test.extract_data_from_blocks(img_info, blocks)
                    with open(file_name, 'wb') as f:
                        f.write(data)
                    print(f"\nĐã trích xuất dữ liệu vào '{file_name}'")
                else:
                    print(f"\nKhông tìm thấy tệp '{file_name}' trong cây thư mục.")
        
        elif choice == "3":
            file_path = input("Nhập đường dẫn đến tệp file image cần phục hồi: ")
            if os.path.isfile(file_path):
                sector_size = int(input("Nhập kích thước sector: "))
                ExportImgFromFileSystem.process_file(file_path, sector_size)
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")
        
        elif choice == "4":
            image_path = input("Nhập đường dẫn đến tệp JPEG: ")
            if os.path.isfile(image_path):
                ConvertGPS.convert_gps(image_path)
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")
        
        elif choice == "5":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            if os.path.isfile(input_path):
                output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã xoá thông tin EXIF: ")
                TestCleanEXIF.sanitize_image(input_path, output_path)
                print(f"Đã xoá thông tin EXIF của ảnh và lưu tại '{output_path}'")
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")
        
        elif choice == "6":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            if os.path.isfile(input_path):
                output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã tái cấu trúc: ")
                CDR_edited.sanitize_lsb(input_path, output_path)
                print(f"Đã tái cấu trúc ảnh và lưu tại '{output_path}'")
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")
        
        elif choice == "7":
            input_path = input("Nhập đường dẫn đến tệp hình ảnh gốc: ")
            if os.path.isfile(input_path):
                message = input("Nhập thông điệp cần ẩn: ")
                output_path = input("Nhập đường dẫn lưu tệp hình ảnh đã ẩn thông điệp: ")
                hide_message_in_image.hide_message_in_image(input_path, message, output_path)
                print(f"Đã ẩn thông điệp trong ảnh và lưu tại '{output_path}'")
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")
        
        elif choice == "8":
            image_path = input("Nhập đường dẫn đến tệp hình ảnh: ")
            if os.path.isfile(image_path):
                try:
                    hidden_message = extract_hidden_in_message.extract_hidden_message(image_path)
                    print("Thông điệp ẩn:", hidden_message)
                except ValueError as e:
                    print(f"An error occurred: {e}")
            else:
                print(f"Lỗi: Tệp {file_path} không tồn tại.")

        elif choice == "9":
            file_img_path = input("Nhập đường dẫn đến tệp file image EXT4 cần phục hồi: ")
            recover_ext4.main_recovery(file_img_path)  
        
        elif choice == "0":
            print("Thoát chương trình.")
            break
        
        else:
            print("Lựa chọn không hợp lệ, vui lòng chọn lại.")
        
        input("Ấn nút Enter để tiếp tục")
        os.system('cls')


if __name__ == "__main__":
    menu()