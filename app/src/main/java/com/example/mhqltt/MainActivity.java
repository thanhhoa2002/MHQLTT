package com.example.mhqltt;

import static com.example.mhqltt.FileManager.convertDateFormat;
import static com.example.mhqltt.FileManager.padding;
import static com.example.mhqltt.FileManager.stringToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToString;
import static com.example.mhqltt.FileManager.intToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToInt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import android.content.ContentResolver;
import android.database.Cursor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.provider.OpenableColumns;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_SELECT = 1;
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

        Header header = new Header();
        String typeString = ".new";
        String passwordString = "123456";
        int sizeInt = 10  ; // MB
        header = new Header(
                padding(stringToByteArray(typeString), 4),
                padding(intToByteArray(sizeInt), 4),
                padding(stringToByteArray(passwordString), 32));
        fileManager.createFile(header);
        Header header2 = fileManager.readHeader(".new");
        Log.d("TAG", byteArrayToString(header2.getType()));
        Log.d("TAG", byteArrayToString(header2.getPassword()));
        Log.d("TAG", String.valueOf(byteArrayToInt((header2.getSize()))));

        // Entry table test
        DirectoryEntry directoryEntry = new DirectoryEntry();
        String name= "Bao cao do an tot nghiep";
        String dateCreate= convertDateFormat("04/06/2002");
        String password= "Nguyen Thanh Phong aaa";
        String format= ".doc";
        int size = 10;
        int dataPosition= 291;
        int state=1;

        directoryEntry = new DirectoryEntry(
                padding(FileManager.stringToByteArray(name), 28),
                padding(FileManager.stringToByteArray(format), 5),
                padding(FileManager.stringToByteArray(dateCreate), 6),
                padding(FileManager.intToByteArray(dataPosition), 4),
                padding(FileManager.intToByteArray(size), 4),
                padding(FileManager.intToByteArray(state), 1),
                padding(FileManager.stringToByteArray(password), 32)
        );
//        outputDirectoryEntry(directoryEntry);

        fileManager.writeFileDirectoryEntry(directoryEntry,0);
        Log.d("TAG","Something");
        String filename = FileManager.byteArrayToString(directoryEntry.getName());

        DirectoryEntry directoryEntry2 = fileManager.readDirectoryEntry(filename);

//        if (DirectoryEntry2 != null) {
//            outputDirectoryEntry(DirectoryEntry2);
//        }

        DirectoryEntry directoryEntry1 = new DirectoryEntry();
        String name1= "Sector 2";
        String dateCreate1= "060624";
        String password1= "Le Anh Vinh ";
        String format1= ".pdf";
        int size1 = 1024;
        int dataPosition1=144;
        int state1=1;
        directoryEntry1 = new DirectoryEntry(
                padding(FileManager.stringToByteArray(name1), 32),
                padding(FileManager.stringToByteArray(format1), 5),
                padding(FileManager.stringToByteArray(dateCreate1), 2),
                padding(FileManager.intToByteArray(dataPosition1), 4),
                padding(FileManager.intToByteArray(size1), 4),
                padding(FileManager.intToByteArray(state1), 1),
                padding(FileManager.stringToByteArray(password1), 32)
        );
        fileManager.writeDirectoryEntry(directoryEntry1,filename);

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
                File file = new File(dir, filename);
                dataPos = fileManager.writeData(pic, file);
                byte[] outputArr = fileManager.readData(dataPos, file);
                Bitmap bitmap = BitmapUtil.bytesToBitmap(outputArr);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

//                int bitDepth = bitmap.getConfig() == Bitmap.Config.ARGB_8888 ? 32 : 16;
                Log.d("bitmap", "Width: " + width + ", Height: " + height );
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                pic = BitmapUtil.bitmapToBytes(bitmap);
                Log.d("MainActivity", "Bytes: " + pic.length);
                String fileName = getFileName(selectedImageUri);
                String dateCreate = getFileCreationDate(selectedImageUri);
                long fileSize = getFileSize(selectedImageUri);
                Log.d("MainActivity", "File Size: " + fileSize);
                Log.d("MainActivity", "Date Create: " + dateCreate);
                Log.d("MainActivity", "File Name: " + fileName);
                Log.d("bitmap", "File Name aaa: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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


    private String getFileName(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        if (documentFile != null && documentFile.getName() != null) {
            return documentFile.getName();
        }
        return null;
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

    private void outputDirectoryEntry(DirectoryEntry directoryEntry) {
        Log.d("TAG", "Name: " + byteArrayToString(directoryEntry.getName()));
        Log.d("TAG", "Format: " + byteArrayToString(directoryEntry.getFormat()));
        Log.d("TAG", "Size: " + byteArrayToInt(directoryEntry.getSize()));
        Log.d("TAG", "Date create: " + byteArrayToString(directoryEntry.getDateCreate()));
        Log.d("TAG", "Data position: " + byteArrayToInt(directoryEntry.getDataPosition()));
        Log.d("TAG", "Password: " + byteArrayToString(directoryEntry.getPassword()));
        Log.d("TAG", "State: " + byteArrayToInt(directoryEntry.getState()));
    }
}

