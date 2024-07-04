package com.example.mhqltt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
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
    private List<Bitmap> displayedImages;
    private Bitmap selectedImage;
    private Button showImageButton, previousButton, nextButton;
    private int currentPage = 0;
    private static final int IMAGES_PER_PAGE = 15;
    private List<DirectoryEntry> directoryEntries = null;
    private LruCache<String, Bitmap> bitmapCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_in_file);

        recyclerView = findViewById(R.id.recyclerView);
        showImageButton = findViewById(R.id.showImageButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        fileManager = new FileManager(this);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Set GridLayoutManager with 3 columns

        displayedImages = new ArrayList<>();

        imageAdapter = new ImageAdapter(this, displayedImages);
        recyclerView.setAdapter(imageAdapter);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        bitmapCache = new LruCache<>(cacheSize);

        File dir = getFilesDir();
        File file = new File(dir, ".NEW");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            directoryEntries = fileManager.readAllEntries(raf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    loadCurrentPageImages();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((currentPage + 1) * IMAGES_PER_PAGE < fileManager.countFileInList(directoryEntries)) {
                    currentPage++;
                    loadCurrentPageImages();
                }
            }
        });

        loadCurrentPageImages();
    }

    private void loadCurrentPageImages() {
        displayedImages.clear();
        new LoadImagesTask().execute(currentPage);
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadImagesTask extends AsyncTask<Integer, Void, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(Integer... params) {
            int page = params[0];
            List<Bitmap> images = new ArrayList<>();
            File dir = getFilesDir();
            File file = new File(dir, ".NEW");

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                int count = 0;
                int startIndex = page * IMAGES_PER_PAGE;
                int endIndex = startIndex + IMAGES_PER_PAGE;

                for (DirectoryEntry entry : directoryEntries) {
                    if (entry != null) {
                        if (count >= startIndex && count < endIndex) {
                            String key = fileManager.byteArrayToString(entry.getDataPos()) + "_" + fileManager.byteArrayToString(entry.getSize());
                            Bitmap bmp = bitmapCache.get(key);

                            if (bmp == null) {
                                int pos = fileManager.byteArrayToInt(entry.getDataPos());
                                int size = fileManager.byteArrayToInt(entry.getSize());
                                byte[] data = fileManager.readImageFileData(raf, pos, size);
                                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                bitmapCache.put(key, bmp);
                            }

                            images.add(bmp);
                        }
                        count++;
                        if (count >= endIndex) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return images;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            displayedImages.addAll(bitmaps);
            imageAdapter.notifyDataSetChanged();
        }
    }
}
