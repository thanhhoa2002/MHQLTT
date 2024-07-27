package com.example.mhqltt;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.IOException;
import java.io.OutputStream;

public class Steganography {
    private static int[] encodeMessageToBits(String message) {
        byte[] encodedMessage = Base64.encode(message.getBytes(), Base64.DEFAULT);
        int[] bitArray = new int[encodedMessage.length * 8];
        for (int i = 0; i < encodedMessage.length; i++) {
            for (int j = 0; j < 8; j++) {
                bitArray[i * 8 + j] = (encodedMessage[i] >> (7 - j)) & 1;
            }
        }
        return bitArray;
    }

    private static int modifyLSB(int color, int bit) {
        return (color & 0xFE) | bit;
    }

    public static void hideMessageInImage(ContentResolver contentResolver, Uri imageUri, String message) throws IOException {
        int[] bitArray = encodeMessageToBits(message);

        Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri));
        if (bitmap == null) {
            throw new IOException("Không thể giải mã tệp ảnh.");
        }

        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (mutableBitmap == null) {
            throw new IOException("Không thể tạo bản sao có thể chỉnh sửa của ảnh.");
        }

        int width = mutableBitmap.getWidth();
        int height = mutableBitmap.getHeight();
        int i = 0;

        for (int x = 0; x < width && i < bitArray.length; x++) {
            int pixel = mutableBitmap.getPixel(x, 0);
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            if (i < bitArray.length) r = modifyLSB(r, bitArray[i++]);
            if (i < bitArray.length) g = modifyLSB(g, bitArray[i++]);
            if (i < bitArray.length) b = modifyLSB(b, bitArray[i++]);

            int newPixel = (0xFF << 24) | (r << 16) | (g << 8) | b;
            mutableBitmap.setPixel(x, 0, newPixel);
        }

        // Lưu bitmap đã chỉnh sửa lại vào MediaStore
        saveBitmapToMediaStore(contentResolver, mutableBitmap, imageUri);
    }

    private static void saveBitmapToMediaStore(ContentResolver contentResolver, Bitmap bitmap, Uri originalUri) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png"); // Loại MIME PNG
        values.put(MediaStore.Images.Media.IS_PENDING, true);

        Uri newUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (newUri == null) {
            throw new IOException("Không thể tạo Uri mới cho ảnh đã chỉnh sửa.");
        }

        try (OutputStream out = contentResolver.openOutputStream(newUri)) {
            if (out != null) {
                boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                if (!success) {
                    throw new IOException("Không thể lưu ảnh đã chỉnh sửa.");
                }
            } else {
                throw new IOException("Không thể mở output stream cho URI.");
            }
        }

        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, false);
        contentResolver.update(newUri, values, null, null);

        // Xóa ảnh gốc nếu cần thiết
        deleteOriginalImage(contentResolver, originalUri);
    }

    private static void deleteOriginalImage(ContentResolver contentResolver, Uri originalUri) {
        try {
            int rowsDeleted = contentResolver.delete(originalUri, null, null);
            if (rowsDeleted > 0) {
                // Ảnh gốc đã được xóa thành công
                System.out.println("Original image deleted successfully.");
            } else {
                // Không xóa được ảnh gốc
                System.out.println("Could not delete the original image.");
            }
        } catch (Exception e) {
            // Xử lý ngoại lệ nếu không thể xóa ảnh gốc
            e.printStackTrace();
            System.out.println("Error deleting original image: " + e.getMessage());
        }
    }

    private static String extractBitsFromImage(ContentResolver contentResolver, Uri imageUri) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri));
        if (bitmap == null) {
            throw new IOException("Không thể giải mã tệp ảnh.");
        }

        StringBuilder extractedBits = new StringBuilder();

        for (int x = 0; x < bitmap.getWidth(); x++) {
            int pixel = bitmap.getPixel(x, 0);
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            extractedBits.append(r & 1);
            extractedBits.append(g & 1);
            extractedBits.append(b & 1);
        }

        return extractedBits.toString();
    }

    private static String bitsToBytes(String bits) {
        StringBuilder chars = new StringBuilder();
        for (int i = 0; i < bits.length(); i += 8) {
            String byteStr = bits.substring(i, Math.min(bits.length(), i + 8));
            if (byteStr.length() < 8) break;
            int byteValue = Integer.parseInt(byteStr, 2);
            chars.append((char) byteValue);
        }
        return chars.toString();
    }

    private static String cleanByteString(String byteString) {
        StringBuilder cleanedByteString = new StringBuilder();
        for (char c : byteString.toCharArray()) {
            if (c < 128) {
                cleanedByteString.append(c);
            }
        }
        return cleanedByteString.toString();
    }

    private static String decodeMessageFromBits(String bits) {
        String byteString = bitsToBytes(bits);
        String cleanedByteString = cleanByteString(byteString);
        byte[] decodedBytes = Base64.decode(cleanedByteString, Base64.DEFAULT);
        return new String(decodedBytes);
    }

    public static String extractHiddenMessage(ContentResolver contentResolver, Uri imageUri) throws IOException {
        String extractedBits = extractBitsFromImage(contentResolver, imageUri);
        return decodeMessageFromBits(extractedBits);
    }
}
