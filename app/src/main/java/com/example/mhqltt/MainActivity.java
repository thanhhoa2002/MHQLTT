package com.example.mhqltt;

import static com.example.mhqltt.FileManager.convertDateFormat;
import static com.example.mhqltt.FileManager.padding;
import static com.example.mhqltt.FileManager.stringToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToString;
import static com.example.mhqltt.FileManager.intToByteArray;
import static com.example.mhqltt.FileManager.byteArrayToInt;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission();
            }
        }

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
        int size = 20;
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
                Log.d("SIZE", String.valueOf(pic.length));
                Log.d("DONE", String.valueOf(dataPos));
                byte[] outputArr = fileManager.readData(dataPos, file);
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