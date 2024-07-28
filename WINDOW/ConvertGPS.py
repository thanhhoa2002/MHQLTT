from PIL import Image #install pillow
from PIL.ExifTags import TAGS, GPSTAGS
from geopy.geocoders import Nominatim  # install geopy
from geopy.exc import GeocoderTimedOut, GeocoderServiceError
import time

def reverse_geocode(lat, lon, retries=3, delay=10):
    geolocator = Nominatim(user_agent="vinhphatphonghoa", timeout=40)  
    for i in range(retries):
        try:
            location = geolocator.reverse((lat, lon), exactly_one=True)
            return location.address if location else "Không tìm thấy địa chỉ cho tọa độ này."
        except (GeocoderServiceError):
            print(f"Thử lại lần {i + 1}/{retries} sau {delay} giây...")
            time.sleep(delay)
    return "Lỗi kết nối hoặc dịch vụ không khả dụng."

def get_exif_data(image_path):  #vị trí + thời gian chụp
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

def convert_gps(image_path):
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

# # Đường dẫn của tệp JPEG
# image_path = "TESTJPGwithEXIF.JPG"

# # Chạy chương trình chính
# convert_gps(image_path)