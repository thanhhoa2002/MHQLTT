package com.example.mhqltt;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.Set;

public class FileManager {
    private final Context context;


    public FileManager(Context context) {
        this.context = context;
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
        header.setSize(intToByteArray(25165824));
        header.setPassword(stringToByteArray(password));
        header.setDateCreate(date);
        header.setDateModify(date);
        header.setTimeCreate(time);
        header.setTimeModify(time);
        header.setOwnerSign(stringToByteArray(ownerSign));

        return header;
    }

    public void createFile() {
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
    }

    //test
    public void writeFileDirectoryEntry(DirectoryEntry directoryEntry, int pos) {
        File dir = context.getFilesDir();
        File file = new File(dir, byteArrayToString(directoryEntry.getName()));
        Log.d("TAG", "test11");
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
//            raf.setLength(Integer.parseInt(byteArrayToString(DirectoryEntry.getSize())));
            raf.setLength(byteArrayToInt(directoryEntry.getSize()) * 1048576L);

            raf.seek(pos); // con tro
            raf.write(directoryEntry.getName());
            raf.write(directoryEntry.getFormat());
            raf.write(directoryEntry.getDateCreate());

            raf.write(directoryEntry.getDataPosition());
            raf.write(directoryEntry.getSize());
            raf.write(directoryEntry.getState());
            raf.write(directoryEntry.getPassword());
            raf.close();
            Log.d("File", "File created successfully with entry table");
        } catch (IOException e) {
            Log.e("File", "Error creating file", e);
        }
    }

    public DirectoryEntry readDirectoryEntry(String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            byte[] name = new byte[28];
            byte[] format = new byte[5];
            byte[] dateCreate = new byte[6];
            byte[] dataPosition = new byte[4];
            byte[] size = new byte[4];
            byte[] state = new byte[1];
            byte[] password = new byte[32];

            raf.read(name, 0, 28);
            raf.read(format, 0, 5);
            raf.read(dateCreate, 0, 6);
            raf.read(dataPosition, 0, 4);
            raf.read(size, 0, 4);
            raf.read(state, 0, 1);
            raf.read(password, 0, 32);

            raf.close();

            return new DirectoryEntry(name, format, dateCreate, dataPosition, size, state, password);
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
}

