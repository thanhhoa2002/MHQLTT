package com.example.mhqltt;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_SELECT = 1;
    private static final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 2;
    private ImageView imageView;
    private byte[] pic = null;
    private FileManager fileManager;
    private Header header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fileManager = new FileManager(this);
        header = null;

        // create Volume
        if (!fileManager.doesVolumeExist()) {
            header = fileManager.createVolume();
        }
        else {
            try {
                header = fileManager.readHeader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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
                File dir = getFilesDir();
                File file = new File(dir, ".NEW");
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(file, "rw");
                    byte[] pic = fileManager.readImageFileData(raf, 326, 3773713);
                    Bitmap bmp = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                    imageView.setImageBitmap(bmp);
                    List<DirectoryEntry> entries = fileManager.readAllEntries(raf);
                    fileManager.fullDeleteFile(raf, entries, 1);
                    for (int i = 0; i < entries.size(); ++i) {
                        if (entries.get(i) != null) {
                            if (Objects.equals(fileManager.byteArrayToString(entries.get(i).getState()), "1")) {
                                Log.d("ENTRY", entries.get(i).toString());
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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
            try {
                fileManager.writeImageFile(selectedImageUri, header);
            } catch (IOException e) {
                throw new RuntimeException(e);
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