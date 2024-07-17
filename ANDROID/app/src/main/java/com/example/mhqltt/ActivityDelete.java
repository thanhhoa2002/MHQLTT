package com.example.mhqltt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivityDelete extends AppCompatActivity {
    private FileManager fileManager;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<Bitmap> displayedImages;
    private List<DirectoryEntry> displayedEntries;
    private DirectoryEntry selectedEntry;
    private Button showImageButton, previousButton, nextButton;
    private int currentPage = 0;
    private static final int IMAGES_PER_PAGE = 15;
    private List<DirectoryEntry> directoryEntries = null;
    private LruCache<String, Bitmap> bitmapCache;
    private List<EmptySectorManagement> lesm;

    Spinner spinnerSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fileshow);
        spinnerSort = findViewById(R.id.spinner);

        recyclerView = findViewById(R.id.recyclerView);
        showImageButton = findViewById(R.id.showImageButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        fileManager = new FileManager(this);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Set GridLayoutManager with 3 columns

        displayedImages = new ArrayList<>();
        displayedEntries = new ArrayList<>();

        imageAdapter = new ImageAdapter(this, displayedImages, displayedEntries);
        recyclerView.setAdapter(imageAdapter);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        bitmapCache = new LruCache<>(cacheSize);

//        File dir = getFilesDir();
//        File file = new File(dir, ".NEW");
//        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
//            directoryEntries = fileManager.readAllEntries(raf);
//            lesm = fileManager.readEmptyArea(raf);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ActivityDelete.this, "Selected: " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();

                if (position == 0) {
                    File dir = getFilesDir();
                    File file = new File(dir, ".NEW");
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        directoryEntries = fileManager.readAllEntries(raf);
                        lesm = fileManager.readEmptyArea(raf);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    loadCurrentPageImages();
                } else if (position == 1 || position == 2) {
                    currentPage = 0;
                    File dir = getFilesDir();
                    File file = new File(dir, ".NEW");
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        directoryEntries = fileManager.readAllEntries(raf);
                        fileManager.sortEntriesBasedOnDateCreate(directoryEntries, position);
                        lesm = fileManager.readEmptyArea(raf);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    loadCurrentPageImages();
                } else if (position == 3) {
                    currentPage = 0;
                    int year=0;
                    int month=0;
                    File dir = getFilesDir();
                    File file = new File(dir, ".NEW");
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        directoryEntries = fileManager.readAllEntries(raf);
                        // Assuming you have a method to filter entries by selected date
                        showMonthYearPickerDialog(directoryEntries);
                        lesm = fileManager.readEmptyArea(raf);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
//                    loadCurrentPageImages();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Bitmap image, DirectoryEntry entry) {
                selectedEntry = entry;
            }
        });

        showImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedEntry != null) {
                    showImageDialog(ActivityDelete.this, selectedEntry);
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

//        loadCurrentPageImages();
    }


    private void showMonthYearPickerDialog(List<DirectoryEntry> entries) {
        MonthYearPickerDialog pd = new MonthYearPickerDialog(ActivityDelete.this,
                new MonthYearPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int year, int month) {

                        fileManager.filterEntriesByMonthYear(entries,year, month);
                        loadCurrentPageImages();
                    }
                });
        pd.show();
    }
    @Override
    public void onBackPressed() {
        File dir = getFilesDir();
        File file = new File(dir, ".NEW");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            fileManager.writeEmptyArea(raf, lesm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        super.onBackPressed();
    }

    private void loadCurrentPageImages() {
        displayedImages.clear();
        displayedEntries.clear();
        new LoadImagesTask().execute(currentPage);
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadImagesTask extends AsyncTask<Integer, Void, List<Bitmap>> {
        private List<DirectoryEntry> entriesForPage = new ArrayList<>();

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
                        if (Arrays.equals(entry.getState(), fileManager.stringToByteArray("0"))) {
                            if (Arrays.equals(entry.getEncrypt(), fileManager.stringToByteArray("0"))) {
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
                                    entriesForPage.add(entry);
                                }
                                count++;
                                if (count >= endIndex) {
                                    break;
                                }
                            }
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
            displayedEntries.addAll(entriesForPage);
            imageAdapter.notifyDataSetChanged();
        }
    }

    private void showImageDialog(Context context, DirectoryEntry entry) {
        File dir = getFilesDir();
        File file = new File(dir, ".NEW");
        Bitmap bitmap = null;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int pos = fileManager.byteArrayToInt(entry.getDataPos());
            int size = fileManager.byteArrayToInt(entry.getSize());
            byte[] data = fileManager.readImageFileData(raf, pos, size);
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_delete, null);

            ImageView imageView = dialogView.findViewById(R.id.dialogImageView);
            Button tempDeleteButton = dialogView.findViewById(R.id.temp_delete_button);
            Button fullDeleteButton = dialogView.findViewById(R.id.full_delete_button);
            Button fullDataDeleteButton = dialogView.findViewById(R.id.full_data_delete_button);

            imageView.setImageBitmap(bitmap);

            builder.setView(dialogView);

            AlertDialog dialog = builder.create();

            tempDeleteButton.setOnClickListener(v -> {
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    fileManager.tempDeleteFile(raf, directoryEntries, directoryEntries.indexOf(entry));
                    dialog.dismiss();
                    loadCurrentPageImages(); // Refresh the current page to reflect changes
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            fullDeleteButton.setOnClickListener(v -> {
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    EmptySectorManagement esm = fileManager.fullDeleteFile(raf, directoryEntries, directoryEntries.indexOf(entry));
                    lesm = fileManager.emptyAreaProcessing(lesm, esm);
                    dialog.dismiss();
                    loadCurrentPageImages(); // Refresh the current page to reflect changes
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            fullDataDeleteButton.setOnClickListener(v -> {
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    EmptySectorManagement esm = fileManager.fullDeleteFile(raf, directoryEntries, directoryEntries.indexOf(entry));
                    fileManager.deleteDataFile(raf, esm);
                    lesm = fileManager.emptyAreaProcessing(lesm, esm);
                    dialog.dismiss();
                    loadCurrentPageImages(); // Refresh the current page to reflect changes
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            dialog.show();
        }
    }
}