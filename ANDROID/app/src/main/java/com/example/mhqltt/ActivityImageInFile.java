package com.example.mhqltt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
public class ActivityImageInFile extends AppCompatActivity {
    private FileManager fileManager;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<Bitmap> allImages;
    private List<Bitmap> displayedImages;
    private Bitmap selectedImage;
    private Button showImageButton, previousButton, nextButton;
    private int currentPage = 0;
    private static final int IMAGES_PER_PAGE = 18;

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

        allImages = new ArrayList<>();
        displayedImages = new ArrayList<>();

        imageAdapter = new ImageAdapter(this, displayedImages);
        recyclerView.setAdapter(imageAdapter);

        loadAllImages();

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
                    showImageDialog(ActivityImageInFile.this, selectedImage);
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    loadPage();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((currentPage + 1) * IMAGES_PER_PAGE < allImages.size()) {
                    currentPage++;
                    loadPage();
                }
            }
        });

        loadPage();
    }

    private void loadAllImages() {
        File dir = getFilesDir();
        File file = new File(dir, ".NEW");
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            List<DirectoryEntry> directoryEntries = fileManager.readAllEntries(raf);
            for (DirectoryEntry entry : directoryEntries) {
                if (entry != null) {
                    int pos = fileManager.byteArrayToInt(entry.getDataPos());
                    int size = fileManager.byteArrayToInt(entry.getSize());
                    byte[] data = fileManager.readImageFileData(raf, pos, size);
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                    allImages.add(bmp);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadPage() {
        displayedImages.clear();
        int start = currentPage * IMAGES_PER_PAGE;
        int end = Math.min(start + IMAGES_PER_PAGE, allImages.size());
        for (int i = start; i < end; i++) {
            displayedImages.add(allImages.get(i));
        }
        imageAdapter.notifyDataSetChanged();
    }

    private void showImageDialog(Context context, Bitmap bitmap) {
        // Tạo một AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate layout từ XML
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_image, null);

        // Tìm ImageView và Button trong layout
        ImageView imageView = dialogView.findViewById(R.id.dialogImageView);
        Button button = dialogView.findViewById(R.id.dialog_button);

        // Thiết lập hình ảnh và hành động cho Button
        imageView.setImageBitmap(bitmap);

        // Thiết lập view cho AlertDialog
        builder.setView(dialogView);

        // Tạo và hiển thị dialog
        AlertDialog dialog = builder.create();

        button.setOnClickListener(v -> {
            // Thực hiện hành động khi nhấn Button
            // Đóng dialog hoặc thực hiện hành động khác
            dialog.dismiss();
        });

        dialog.show();
    }
}



//public class ActivityImageInFile extends AppCompatActivity {
//    private FileManager fileManager;
//    private RecyclerView recyclerView;
//    private ImageAdapter imageAdapter;
//    private List<Bitmap> allImages;
//    private List<Bitmap> displayedImages;
//    private Bitmap selectedImage;
//    private Button showImageButton, previousButton, nextButton;
//    private int currentPage = 0;
//    private static final int IMAGES_PER_PAGE = 18;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_image_in_file);
//
//        recyclerView = findViewById(R.id.recyclerView);
//        showImageButton = findViewById(R.id.showImageButton);
//        previousButton = findViewById(R.id.previousButton);
//        nextButton = findViewById(R.id.nextButton);
//        fileManager = new FileManager(this);
//
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Set GridLayoutManager with 3 columns
//
//        allImages = new ArrayList<>();
//        displayedImages = new ArrayList<>();
//
//        imageAdapter = new ImageAdapter(this, displayedImages);
//        recyclerView.setAdapter(imageAdapter);
//
//        loadAllImages();
//
//        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(Bitmap image) {
//                selectedImage = image;
//            }
//        });
//
//        showImageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (selectedImage != null) {
//                    ImageDialog imageDialog = new ImageDialog(ActivityImageInFile.this, selectedImage);
//                    imageDialog.show();
//                }
//            }
//        });
//
//        previousButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentPage > 0) {
//                    currentPage--;
//                    loadPage();
//                }
//            }
//        });
//
//        nextButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if ((currentPage + 1) * IMAGES_PER_PAGE < allImages.size()) {
//                    currentPage++;
//                    loadPage();
//                }
//            }
//        });
//
//        loadPage();
//    }
//
//    private void loadAllImages() {
//        File dir = getFilesDir();
//        File file = new File(dir, ".NEW");
//        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
//            List<DirectoryEntry> directoryEntries = fileManager.readAllEntries(raf);
//            for (DirectoryEntry entry : directoryEntries) {
//                if (entry != null) {
//                    int pos = fileManager.byteArrayToInt(entry.getDataPos());
//                    int size = fileManager.byteArrayToInt(entry.getSize());
//                    byte[] data = fileManager.readImageFileData(raf, pos, size);
//                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    allImages.add(bmp);
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void loadPage() {
//        displayedImages.clear();
//        int start = currentPage * IMAGES_PER_PAGE;
//        int end = Math.min(start + IMAGES_PER_PAGE, allImages.size());
//        for (int i = start; i < end; i++) {
//            displayedImages.add(allImages.get(i));
//        }
//        imageAdapter.notifyDataSetChanged();
//    }
//    private void showImageDialog(Context context, Bitmap bitmap) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//
//        // Inflate layout từ XML
//        LayoutInflater inflater = LayoutInflater.from(context);
//        View dialogView = inflater.inflate(R.layout.dialog_image, null);
//
//
//        ImageView imageView = dialogView.findViewById(R.id.dialogImageView);
//        Button button = dialogView.findViewById(R.id.dialog_button);
//
//
//        imageView.setImageBitmap(bitmap);
//        button.setOnClickListener(v -> {
//
//            ((AlertDialog) v.getRootView().getContext()).dismiss();
//        });
//
//        // Thiết lập view cho AlertDialog
//        builder.setView(dialogView);
//
//        // Tạo và hiển thị dialog
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//}
