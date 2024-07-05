from PIL import Image #install pillow
from PIL.ExifTags import TAGS, GPSTAGS
from geopy.geocoders import Nominatim #install geopy

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
    degrees = dms_tuple[0]
    minutes = dms_tuple[1]
    seconds = dms_tuple[2]
    decimal = degrees + minutes / 60.0 + seconds / 3600.0
    return decimal

def gps_data_to_coordinates(gps_data):
    if gps_data is None:
        return None
    lat_dms = gps_data.get("GPSLatitude")
    lat_ref = gps_data.get("GPSLatitudeRef")
    lon_dms = gps_data.get("GPSLongitude")
    lon_ref = gps_data.get("GPSLongitudeRef")
    altitude = gps_data.get("GPSAltitude")
    if lat_dms and lon_dms and lat_ref and lon_ref:
        lat = get_decimal_from_dms(lat_dms)
        lon = get_decimal_from_dms(lon_dms)
        if lat_ref == "S":
            lat = -lat
        if lon_ref == "W":
            lon = -lon
        return lat, lon, altitude
    else:
        return None
    
def lat_long_to_location_details(latitude, longitude):
    geolocator = Nominatim(user_agent="geoapiExercises")
    location = geolocator.reverse((latitude, longitude), exactly_one=True)
    address = location.raw['address']
    
    # Extracting various address components
    country = address.get('country', '')
    city = address.get('city', address.get('town', address.get('village', '')))
    district = address.get('suburb', address.get('borough', ''))
    state = address.get('state', '')
    county = address.get('county', '')
    postcode = address.get('postcode', '')
    road = address.get('road', '')
    house_number = address.get('house_number', '')
    neighborhood = address.get('neighbourhood', '')
    
    return {
        'country': country,
        'city': city,
        'district': district,
        'state': state,
        'county': county,
        'postcode': postcode,
        'road': road,
        'house_number': house_number,
        'neighborhood': neighborhood
    }
# Đường dẫn của tệp JPEG
image_path = "gps_exif_example.jpg"

# Lấy dữ liệu GPS từ tệp JPEG
gps_data = get_exif_data(image_path)

# Chuyển đổi dữ liệu GPS thành tọa độ
coordinates = gps_data_to_coordinates(gps_data)

if coordinates is not None:
    lat, lon, altitude = coordinates
    print("Latitude:", lat)
    print("Longitude:", lon)
    if altitude is not None:
        print("Altitude:", altitude)
    else:
        print("No altitude data available.")
else:
    print("Không có dữ liệu GPS trong tệp JPEG này.")

location_details = lat_long_to_location_details(lat, lon)
print('Quốc gia: ',location_details['country'])
print('Thành phố: ',location_details['city'])
print('Quận: ',location_details['district'])