package com.example.mhqltt;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
