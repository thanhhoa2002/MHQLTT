package com.example.mhqltt;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class UIFolder extends AppCompatActivity {
    private static final int REQUEST_IMAGE_SELECT = 1;
    private FileManager fileManager;
    private Header header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_folder);
        fileManager = new FileManager(this);
        header = null;

        // create Volume
        if (!fileManager.doesVolumeExist()) {
            header = fileManager.createVolume();
        } else {
            try {
                header = fileManager.readHeader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Button viewImageButton = findViewById(R.id.view_image);
        viewImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UIFolder.this, ActivityView.class);
                startActivity(intent);
            }
        });

//         Button to select an image
        Button addImageButton = findViewById(R.id.add_image);
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_IMAGE_SELECT);
            }
        });

        Button deleteImageButton = findViewById(R.id.delete_image);
        deleteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UIFolder.this, ActivityDelete.class);
                startActivity(intent);
            }
        });

        Button restoreImageButton = findViewById(R.id.restore_image);
        restoreImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(UIFolder.this, ActivityRestore.class);
                startActivity(intent);
            }
        });
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
        }
    }
}
