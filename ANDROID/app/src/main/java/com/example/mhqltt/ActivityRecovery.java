package com.example.mhqltt;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivityRecovery extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Uri> photoUris;
    private PhotoAdapter photoAdapter;
    private FileManager fileManager;
    private Header header;
    private FileLibrary fileLibrary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns in the grid

        photoUris = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, photoUris);
        recyclerView.setAdapter(photoAdapter);
        fileManager = new FileManager(this);
        header = null;

        fileLibrary = new FileLibrary(this);

        // Create Volume
        if (!fileManager.doesVolumeExist()) {
            header = fileManager.createVolume();
        } else {
            try {
                header = fileManager.readHeader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        photoAdapter.setOnItemClickListener(this::showImageRecoveryDialog);

        displayAllImages();
    }

    private void displayAllImages() {
        File directory = Environment.getExternalStorageDirectory();
        List<Uri> imageUris = getAllImageUris(directory);

        // Display the list of images in RecyclerView
        photoAdapter.updatePhotoUris(imageUris);
    }

    private List<Uri> getAllImageUris(File directory) {
        List<Uri> imageUris = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    imageUris.addAll(getAllImageUris(file));
                } else {
                    String filePath = file.getAbsolutePath();
                    if (isImageFile(filePath)) {
                        imageUris.add(Uri.fromFile(file));
                    }
                }
            }
        }
        return imageUris;
    }

    private boolean isImageFile(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif");
    }

    // Method to display dialog when an image is selected
    private void showImageRecoveryDialog(Bitmap bitmap, Uri imageUri) {
        if (imageUri != null) {
            ImageDialogRecovery dialog = new ImageDialogRecovery(this, bitmap, imageUri);
            dialog.show();
            Button buttonRecovery = dialog.findViewById(R.id.recoverybutton);
            Button info = dialog.findViewById(R.id.informationImage);

            info.setOnClickListener(view -> {
                Log.d("TAG", "Image URI: " + imageUri);
                String displayName = "recovered_image";
                String mimeType = "image/jpeg";
                long dateAdded = 0;

                String scheme = imageUri.getScheme();
                if (scheme.equals("content")) {
                    String[] projection = {
                            MediaStore.Images.Media.DISPLAY_NAME,
                            MediaStore.Images.Media.DATE_ADDED,
                            MediaStore.Images.Media.MIME_TYPE
                    };

                    try (Cursor cursor = getContentResolver().query(imageUri, projection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                            int dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
                            int mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);

                            displayName = cursor.getString(displayNameIndex);
                            dateAdded = cursor.getLong(dateAddedIndex);
                            mimeType = cursor.getString(mimeTypeIndex);


                            Toast.makeText(this, "Tên: " + displayName + "\nNgày tạo: " + new Date(dateAdded * 1000) + "\nMIME: " + mimeType, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Không thể lấy thông tin từ URI", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("TAG", "Error querying content resolver", e);
                    }
                } else if (scheme.equals("file")) {
                    File file = new File(imageUri.getPath());
                    if (file.exists()) {
                        displayName = file.getName();
                        dateAdded = file.lastModified() / 1000; // Convert milliseconds to seconds


                        if (this instanceof FragmentActivity) {
                            FragmentActivity activity = (FragmentActivity) this;
                            String date= String.valueOf(new Date(dateAdded * 1000));
                            ImageInfoDialogFragment dialogFragment = ImageInfoDialogFragment.newInstance("Tên: "+displayName, "Ngày tạo: "+ date, "");
                            dialogFragment.show(activity.getSupportFragmentManager(), "image_info");
                        } else {
                            throw new IllegalStateException("Context must be an instance of FragmentActivity");
                        }
                    } else {
                        Log.e("TAG", "File does not exist: " + imageUri.getPath());
                        Toast.makeText(this, "Tệp không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("TAG", "Unsupported URI scheme: " + scheme);
                    Toast.makeText(this, "URI scheme không hỗ trợ: " + scheme, Toast.LENGTH_SHORT).show();
                }
            });

            buttonRecovery.setOnClickListener(view -> {
                try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                    if (inputStream != null) {
                        String displayName = "recovered_image";
                        String mimeType = "image/jpeg";
                        fileLibrary.saveImageToLibrary(inputStream, displayName, mimeType);
                        Toast.makeText(this, "Đã phục hồi", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("TAG", "Failed to open InputStream for URI: " + imageUri);
                    }
                } catch (IOException e) {
                    Log.e("TAG", "Error writing image file", e);
                }
            });

            dialog.show();
        } else {
            Log.d("TAG", "Image URI is null");
        }
    }



}
