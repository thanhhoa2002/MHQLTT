package com.example.mhqltt;

//import static com.example.mhqltt.FileManager.convertDateFormat;
import static com.example.mhqltt.FileManager.padding;
import static com.example.mhqltt.FileManager.stringToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToString;
import static com.example.mhqltt.FileManager.intToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToInt;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
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

    private Context context;
    UriFileHelper uriFileHelper = new UriFileHelper(context);

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
            String imagePath = uriFileHelper.getRealPathFromURI(selectedImageUri);
            Log.d("PATH", imagePath);
            if (imagePath != null) {
                File imageFile = new File(imagePath);
                try {
                    pic = fileManager.readFileToBytes(imageFile);
                    Log.d("PIC", "Bytes: " + pic.length);
                    String fileName = uriFileHelper.getFileName(selectedImageUri);
                    String creationDate = uriFileHelper.getFileCreationDate(selectedImageUri);
                    long fileSize = uriFileHelper.getFileSize(selectedImageUri);
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
}