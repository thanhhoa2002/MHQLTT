package com.example.mhqltt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ActivityView extends AppCompatActivity {
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

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/NoPadding";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

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

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ActivityView.this, "Selected: " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();

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
                    showImageDialog(ActivityView.this, selectedEntry);
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
    }


    private void showMonthYearPickerDialog(List<DirectoryEntry> entries) {
        MonthYearPickerDialog pd = new MonthYearPickerDialog(ActivityView.this,
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
                            if (count >= startIndex && count < endIndex) {
                                String key = fileManager.byteArrayToString(entry.getDataPos()) + "_" + fileManager.byteArrayToString(entry.getSize());
                                Bitmap bmp = bitmapCache.get(key);

                                if (bmp == null) {
                                    int pos = fileManager.byteArrayToInt(entry.getDataPos());
                                    int size = fileManager.byteArrayToInt(entry.getSize());
                                    if (Arrays.equals(entry.getEncrypt(), fileManager.stringToByteArray("0"))) {
                                        byte[] data = fileManager.readImageFileData(raf, pos, size);
                                        bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        bitmapCache.put(key, bmp);
                                    } else {
                                        bmp = BitmapFactory.decodeResource(ActivityView.this.getResources(), R.drawable.encrypt);
                                        bitmapCache.put(key, bmp);
                                    }
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
        if (Arrays.equals(entry.getEncrypt(), fileManager.stringToByteArray("0"))) {
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
                showBitmapDialog(context, bitmap, entry);
            }
        } else {
            // Show encrypted image
            showImageDialogWithResource(context, R.drawable.encrypt, entry);
        }
    }

    private void showImageDialogWithResource(Context context, int resourceId, DirectoryEntry entry) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        showBitmapDialog(context, bitmap, entry);
    }

    private void showBitmapDialog(Context context, Bitmap bitmap, DirectoryEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_encrypt, null);

        ImageView imageView = dialogView.findViewById(R.id.dialogImageView);
        Button encryptButton = dialogView.findViewById(R.id.encrypt_button);
        Button decryptButton = dialogView.findViewById(R.id.decrypt_button);
        Button infImage= dialogView.findViewById(R.id.informationImage);

        imageView.setImageBitmap(bitmap);

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        infImage.setOnClickListener(v -> {
            // Lấy thông tin từ DirectoryEntry
            String name = "Tên ảnh: " + fileManager.byteArrayToString(entry.getName());
            String extension = "Loại: " + fileManager.byteArrayToString(entry.getExtendedName());
            int[] date1 = new int[4];

            for(int i=0;i<4;i++)
            {
                date1[i]=entry.getDateCreate()[i];
            }
            int day = date1[0];
            int month1 = date1[1];
            int year1 = (date1[2] & 0xFF) << 8 | (date1[3] & 0xFF);
            String creationDate="Ngày tạo: "+day+"/"+month1+"/"+year1;
            // Tạo và hiển thị ImageInfoDialogFragment
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                ImageInfoDialogFragment dialogFragment = ImageInfoDialogFragment.newInstance(name, extension, creationDate);
                dialogFragment.show(activity.getSupportFragmentManager(), "image_info");
            } else {
                throw new IllegalStateException("Context must be an instance of FragmentActivity");
            }
        });

        if (Arrays.equals(entry.getEncrypt(), fileManager.stringToByteArray("0"))) {
            decryptButton.setVisibility(View.GONE);
            encryptButton.setOnClickListener(v -> showPasswordInputDialog(context, password -> {
                File file = new File(getFilesDir(), ".NEW");
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    fileManager.encryptFile(raf, directoryEntries, directoryEntries.indexOf(entry), getKeyFromPassword(password));
                    dialog.dismiss();
                    loadCurrentPageImages();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } else {
            encryptButton.setVisibility(View.GONE);
            decryptButton.setOnClickListener(v -> showPasswordInputDialog(context, password -> {
                try {
                    if (Arrays.equals(entry.getPassword(), getKeyFromPassword(password).getEncoded())) {
                        File file = new File(getFilesDir(), ".NEW");
                        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                            fileManager.decryptFile(raf, directoryEntries, directoryEntries.indexOf(entry), getKeyFromPassword(password));
                            dialog.dismiss();
                            loadCurrentPageImages();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        dialog.show();
    }

    private void showPasswordInputDialog(Context context, PasswordCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_password_input, null);

        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        builder.setView(dialogView);

        AlertDialog passwordDialog = builder.create();

        confirmButton.setOnClickListener(v -> {
            String password = passwordInput.getText().toString();
            if (!password.isEmpty()) {
                callback.onPasswordEntered(password);
                passwordDialog.dismiss();
            } else {
                Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show();
            }
        });

        passwordDialog.show();
    }

    private static SecretKeySpec getKeyFromPassword(String password) throws Exception {
        // Hash the password using SHA-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes("UTF-8"));

        // Use the full 32 bytes (256 bits) for the AES key
        return new SecretKeySpec(key, ALGORITHM);
    }

    private interface PasswordCallback {
        void onPasswordEntered(String password);
    }
}
