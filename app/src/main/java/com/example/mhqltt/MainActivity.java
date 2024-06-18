package com.example.mhqltt;

//import static com.example.mhqltt.FileManager.convertDateFormat;
import static com.example.mhqltt.FileManager.padding;
import static com.example.mhqltt.FileManager.stringToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToString;
import static com.example.mhqltt.FileManager.intToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToInt;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_SELECT = 1;
    private static final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 2;
    private ImageView imageView;
    private byte[] pic = null;
    private FileManager fileManager;

    int dataPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        fileManager = new FileManager(this);
        fileManager.createFile();
        String filename="/.NEW";
        DirectoryEntry directoryEntry= fileManager.createDirectoryEntry();
        fileManager.writeDirectoryEntry(directoryEntry,filename);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission();
            }
        }


        // Button to select an image
        Button selectImageButton = findViewById(R.id.select_image_button);
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_IMAGE_SELECT);
            }
        });

        // Button to display the image
        imageView = findViewById(R.id.imageView);
        Button convertButton = findViewById(R.id.convert_button);
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SIZE", String.valueOf(pic.length));
                Log.d("DONE", String.valueOf(dataPos));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_MANAGE_EXTERNAL_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            String imagePath = getRealPathFromURI(selectedImageUri);
            Log.d("PATH", imagePath);
            if (imagePath != null) {
                File imageFile = new File(imagePath);
                try {
                    pic = readFileToBytes(imageFile);
                    Log.d("PIC", "Bytes: " + pic.length);
                    String fileName = getFileName(selectedImageUri);
                    String creationDate = getFileCreationDate(selectedImageUri);
                    long fileSize = getFileSize(selectedImageUri);
                    Log.d("MainActivity", "File Name: " + fileName);
                    Log.d("MainActivity", "Creation Date: " + creationDate);
                    Log.d("MainActivity", "File Size: " + fileSize + " bytes");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Quyền truy cập đã được cấp
                    Log.d("TAG", "onActivityResult: granted");
                } else {
                    // Quyền truy cập không được cấp
                    Log.d("TAG", "onActivityResult: ");
                }
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }

    private byte[] readFileToBytes(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
        return byteArrayOutputStream.toByteArray();
    }


    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileCreationDate(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {MediaStore.Images.Media.DATE_TAKEN};
        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                if (dateIndex != -1) {
                    long dateTaken = cursor.getLong(dateIndex);
                    Date date = new Date(dateTaken);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    return formatter.format(date);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown date";
    }

    private long getFileSize(Uri uri) {
        long fileSize = -1;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }
        }
        return fileSize;
    }

}