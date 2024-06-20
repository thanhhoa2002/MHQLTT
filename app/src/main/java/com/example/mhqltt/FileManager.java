package com.example.mhqltt;

import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import android.database.Cursor;
import android.content.ContentResolver;
import android.provider.OpenableColumns;

public class FileManager {
    private final Context context;
    int entrySize = 256;
    int numberOfEntries = 320;
    int sectorSize = 8192;
    private UriFileHelper uriFileHelper;


    public FileManager(Context context) {
        this.context = context;
        uriFileHelper = new UriFileHelper(context);
    }

    public static void getCurrentDateTimeInBytes(byte[] date, byte[] time) {
        Calendar calendar = Calendar.getInstance();

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Tháng bắt đầu từ 0
        int year = calendar.get(Calendar.YEAR);

        byte[] yearBytes = intToByteArray(year);

        date[0] = (byte) day;
        date[1] = (byte) month;
        date[2] = yearBytes[2];
        date[3] = yearBytes[3];

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        time[0] = (byte) hour;
        time[1] = (byte) minute;
        time[2] = (byte) second;
    }

    public static int bytesToYear(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    public Header createHeader() {
        Header header = new Header();

        String password = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        // input password and hash
        String ownerSign = "aaaaaaaaaa";
        // input owner sign

        byte[] date = new byte[4];
        byte[] time = new byte[3];
        getCurrentDateTimeInBytes(date, time);

        header.setType(stringToByteArray(".NEW"));
        header.setSize(intToByteArray(2608)); // 25165824
        header.setPassword(stringToByteArray(password));
        header.setDateCreate(date);
        header.setDateModify(date);
        header.setTimeCreate(time);
        header.setTimeModify(time);
        header.setOwnerSign(stringToByteArray(ownerSign));

        return header;
    }

    public Header readHeader() throws IOException {
        Header header = new Header();

        File dir = context.getFilesDir();
        File file = new File(dir, ".NEW");

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer;

            buffer = new byte[4];
            raf.read(buffer);
            header.setType(buffer);

            buffer = new byte[4];
            raf.read(buffer);
            header.setSize(buffer);

            buffer = new byte[32];
            raf.read(buffer);
            header.setPassword(buffer);

            buffer = new byte[4];
            raf.read(buffer);
            header.setDateCreate(buffer);

            buffer = new byte[4];
            raf.read(buffer);
            header.setDateModify(buffer);

            buffer = new byte[3];
            raf.read(buffer);
            header.setTimeCreate(buffer);

            buffer = new byte[3];
            raf.read(buffer);
            header.setTimeModify(buffer);

            buffer = new byte[10];
            raf.read(buffer);
            header.setOwnerSign(buffer);
        } catch (IOException e) {
            throw new IOException("Error reading the header from the file.", e);
        }

        return header;
    }

    public Header createVolume() {
        Header header = createHeader();

        File dir = context.getFilesDir();
        File file = new File(dir, byteArrayToString(header.getType()));

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength((long)byteArrayToInt(header.getSize()) * 1024);

            // Header
            raf.seek(0);

            raf.write(header.getType());
            raf.write(header.getSize());
            raf.write(header.getPassword());
            raf.write(header.getDateCreate());
            raf.write(header.getDateModify());
            raf.write(header.getTimeCreate());
            raf.write(header.getTimeModify());
            raf.write(header.getOwnerSign());

            raf.close();
            Log.d("File", "File created successfully");
        } catch (IOException e) {
            Log.e("File", "Error creating file", e);
        }
        return header;
    }

    public boolean doesVolumeExist() {
        File dir = context.getFilesDir();

        File file = new File(dir, ".NEW");

        return file.exists();
    }

    public int writeImageFile(Uri imageUri, RandomAccessFile raf, int emptySector) throws IOException {
        // write data
        String imagePath = uriFileHelper.getRealPathFromURI(imageUri);
        File imageFile = new File(imagePath);
        byte[] cache = readFileToBytes(imageFile);

        raf.seek((long)emptySector * sectorSize);
        raf.write(cache);

        // write entry
        DirectoryEntry entry = imageUriToEntry(imageUri, emptySector);
        writeImageFileDirectoryEntry(raf, entry);


        return emptySector + (cache.length / sectorSize + 1);
    }

    public DirectoryEntry imageUriToEntry(Uri imageUri, int dataPos) {
        DirectoryEntry entry = new DirectoryEntry();

        String[] fileName = splitFileName(uriFileHelper.getFileName(imageUri));
        String dateCreate = uriFileHelper.getFileCreationDate(imageUri);
        long fileSize = uriFileHelper.getFileSize(imageUri);

        entry.setName(padding(stringToByteArray(fileName[0]), 160));
        entry.setExtendedName(padding(stringToByteArray(fileName[1]), 5));
        entry.setDateCreate(stringDateToByteArray(dateCreate));
        entry.setDataPos(intToByteArray(dataPos));
        entry.setSize(intToByteArray((int)fileSize / 1024));
        entry.setState(stringToByteArray("0"));
        entry.setPassword(padding(stringToByteArray(""), 32));

        return entry;
    }

    public void writeImageFileDirectoryEntry(RandomAccessFile raf, DirectoryEntry directoryEntry) {
        try {
            // Find the next empty slot
            long nextEmptySlot = findNextEmptyEntrySlot(raf);

            // Seek to the found empty slot
            raf.seek(sectorSize * 6 + entrySize * nextEmptySlot);
            Log.d("TEST", "slot: " + (sectorSize * 6 + entrySize * nextEmptySlot));

            // Write the directory entry
            raf.write(directoryEntry.getName());
            raf.write(directoryEntry.getExtendedName());
            raf.write(directoryEntry.getDateCreate());
            raf.write(directoryEntry.getDataPos());
            raf.write(directoryEntry.getSize());
            raf.write(directoryEntry.getState());
            raf.write(directoryEntry.getPassword());

            Log.d("File", "Write entry table successfully");
        } catch (IOException e) {
            Log.e("File", "Write entry table fail", e);
        }
    }

    private long findNextEmptyEntrySlot(RandomAccessFile raf) throws IOException {
        raf.seek(sectorSize * 6);

        byte[] buffer = new byte[entrySize];

        for (int i = 0; i < numberOfEntries; ++i) {
            raf.read(buffer);
            if (isBufferEmpty(buffer)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBufferEmpty(byte[] buffer) {
        for (byte b : buffer) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }


    public DirectoryEntry readDirectoryEntry(String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            byte[] name = new byte[160];
            byte[] extendedName = new byte[5];
            byte[] dateCreate = new byte[4];
            byte[] dataPos = new byte[4];
            byte[] size = new byte[4];
            byte[] state = new byte[1];
            byte[] password = new byte[32];


            raf.read(name, 0, 160);
            raf.read(extendedName, 0, 5);
            raf.read(dateCreate, 0, 4);
            raf.read(dataPos, 0, 4);
            raf.read(size, 0, 4);
            raf.read(state, 0, 1);
            raf.read(password, 0, 32);

            raf.close();

            return new DirectoryEntry(name, extendedName, dateCreate, dataPos, size, state, password);
        } catch (FileNotFoundException e) {
            Log.e("File", "File not found", e);
        } catch (IOException e) {
            Log.e("File", "Error reading file", e);
        }
        return null;
    }

    public static byte[] padding(byte[] data, int size) {
        int sizeData = data.length;
        byte[] dataPadding = new byte[size];
        for (int i = 0; i < size; i++) {
            if (i < sizeData)
                dataPadding[i] = data[i];
            else {
                byte Str = 0;
                dataPadding[i] = Str;
            }
        }
        return dataPadding;
    }

    public static String byteArrayToString(byte[] bytes) {
        return new String(bytes).trim();
    }

    public static byte[] stringToByteArray(String str) {
        return str.getBytes();
    }

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int byteArrayToInt(byte[] bytes) {
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        return wrapped.getInt();
    }

    public byte[] readFileToBytes(File file) throws IOException {
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

    public static String[] splitFileName(String fileName) {
        // Get the last index of the dot
        int lastDotIndex = fileName.lastIndexOf('.');

        // Check if the dot is found and is not the first character
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String name = fileName.substring(0, lastDotIndex);
            String extension = fileName.substring(lastDotIndex + 1); // Without dot
            return new String[]{name, extension};
        } else {
            // Return the original file name if no dot is found
            return new String[]{fileName, ""};
        }
    }

    public static byte[] stringDateToByteArray(String dateString) {
        String[] dateParts = dateString.split("-");
        byte[] date = new byte[4];

        int year = Integer.parseInt(dateParts[2]);
        byte[] yearBytes = intToByteArray(year);

        date[0] = (byte)Integer.parseInt(dateParts[0]);
        date[1] = (byte)Integer.parseInt(dateParts[1]);
        date[2] = yearBytes[0];
        date[3] = yearBytes[1];

        return date;
    }
}