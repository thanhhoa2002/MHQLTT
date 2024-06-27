package com.example.mhqltt;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class ActivityImageInFile extends AppCompatActivity {
    private FileManager fileManager;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<Bitmap> imageList;
    private Bitmap selectedImage;
    private Button showImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_in_file);
        recyclerView = findViewById(R.id.recyclerView);
        showImageButton = findViewById(R.id.showImageButton);
        fileManager = new FileManager(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Set GridLayoutManager with 3 columns
        File dir = getFilesDir();
        File file = new File(dir, ".NEW");
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            List<DirectoryEntry> directoryEntries = fileManager.readAllEntries(raf);
            Log.d("TAG", "2");
            imageList = new ArrayList<>();
            for (int i = 0; i < directoryEntries.size(); i++) {
                if (directoryEntries.get(i) != null) {
                    int pos = fileManager.byteArrayToInt(directoryEntries.get(i).getDataPos());
                    int size = fileManager.byteArrayToInt(directoryEntries.get(i).getSize());
                    byte[] data = fileManager.readImageFileData(raf, pos, size);
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                    imageList.add(bmp);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        imageAdapter = new ImageAdapter(this, imageList);
        recyclerView.setAdapter(imageAdapter);

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Bitmap image) {
                selectedImage = image;
            }
        });

        showImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImage != null) {
                    ImageDialog imageDialog = new ImageDialog(ActivityImageInFile.this, selectedImage);
                    imageDialog.show();
                }
            }
        });
    }
}