import bitarray #https://www.boiteaklou.fr/Steganography-Least-Significant-Bit.html#resources
import base64
from PIL import Image

# Function to encode a message into a bit array
def encode_message_to_bits(message):
    encoded_message = base64.b64encode(message.encode('utf-8'))
    ba = bitarray.bitarray()
    ba.frombytes(encoded_message)
    return [int(i) for i in ba]

# Function to modify the least significant bit of a color channel
def modify_lsb(color, bit):
    color_bin = bin(color)
    new_color_bin = color_bin[:-1] + str(bit)
    return int(new_color_bin, 2)

# Main function to hide a message in an image
def hide_message_in_image(image_path, message, output_path):
    bit_array = encode_message_to_bits(message)
    im = Image.open(image_path)
    im = im.convert("RGB")  # Ensure image is in RGB mode
    width, height = im.size
    pixels = im.load()
    i = 0

    for x in range(width):
        if i >= len(bit_array):
            break
        r, g, b = pixels[x, 0]
        new_r = modify_lsb(r, bit_array[i]) if i < len(bit_array) else r
        i += 1
        new_g = modify_lsb(g, bit_array[i]) if i < len(bit_array) else g
        i += 1
        new_b = modify_lsb(b, bit_array[i]) if i < len(bit_array) else b
        i += 1
        pixels[x, 0] = (new_r, new_g, new_b)

    im.save(output_path)

#Usage
# message = 'Phong1234567'
# input_image_path = 'spongebob.png'
# output_image_path = 'lsb_spongebob.png'
# hide_message_in_image(input_image_path, message, output_image_path)