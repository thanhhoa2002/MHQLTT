import base64
from PIL import Image

def extract_bits_from_image(image_path):
    image = Image.open(image_path)
    pixels = image.load()
    extracted_bits = ''

    for x in range(image.width):
        r, g, b = pixels[x, 0]
        extracted_bits += bin(r)[-1]  # LSB of red channel
        extracted_bits += bin(g)[-1]  # LSB of green channel
        extracted_bits += bin(b)[-1]  # LSB of blue channel

    return extracted_bits

def bits_to_bytes(bits):
    chars = []
    for i in range(0, len(bits), 8):
        byte = bits[i:i + 8]
        if len(byte) < 8:
            break
        chars.append(chr(int(byte, 2)))
    return ''.join(chars)

def clean_byte_string(byte_string):
    # Remove non-ASCII characters and ensure the string is valid for base64 decoding
    return ''.join(c for c in byte_string if ord(c) < 128)

def decode_message_from_bits(bits):
    byte_string = bits_to_bytes(bits)
    cleaned_byte_string = clean_byte_string(byte_string)
    try:
        decoded_message = base64.b64decode(cleaned_byte_string.encode('ascii')).decode('utf-8')
    except (base64.binascii.Error, UnicodeDecodeError, ValueError) as e:
        raise ValueError(f"Decoding failed: {e}")
    return decoded_message

def extract_hidden_message(image_path):
    extracted_bits = extract_bits_from_image(image_path)
    message = decode_message_from_bits(extracted_bits)
    return message

# Usage
image_path = "output_sanitized.png"
try:
    hidden_message = extract_hidden_message(image_path)
    print(hidden_message)
except ValueError as e:
    print(f"An error occurred: {e}")