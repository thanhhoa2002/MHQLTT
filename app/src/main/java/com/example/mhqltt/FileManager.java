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

import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;


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

            raf.seek(pos*8192); // con tro
            raf.write(directoryEntry.getName());
            raf.write(directoryEntry.getExtendedName());
            raf.write(directoryEntry.getDateCreate());

            raf.write(directoryEntry.getDataPos());
            raf.write(directoryEntry.getSize());
            raf.write(directoryEntry.getState());
            raf.write(directoryEntry.getPassword());
            raf.close();
            Log.d("File", "File created successfully with entry table");
        } catch (IOException e) {
            Log.e("File", "Error creating file", e);
        }
    }

    public static boolean isNullByte(byte[] data, int size) {
        for (int i = 0; i < size; i++) {
            if (data[i] != 0)
                return false;
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
    public int findEmptyDirectoryEntry(int posSectorAvailable, String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(8192L * posSectorAvailable);
            for (int i = 0; i < 32; i++) {
                byte[] directoryEntry = new byte[256];
                raf.read(directoryEntry, 0, 256);
                if (isNullByte(directoryEntry, 256))
                    return i;
            }
            raf.close();


        } catch (FileNotFoundException e) {
            Log.e("File", "File not found", e);
        } catch (IOException e) {
            Log.e("File", "Error reading file", e);
        }

        return 32;
    }

    public int findEmptySector(String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            raf.seek(512);
            for (int i = 1; i < fileLength / 8192; i++) {
                byte[] sector = new byte[8192];
                raf.read(sector, 0, 8192);
                if (isNullByte(sector, 8192))
                    return i;
            }
            raf.close();
            Log.d("File", "File is out of space");

        } catch (FileNotFoundException e) {
            Log.e("File", "File not found", e);
        } catch (IOException e) {
            Log.e("File", "Error reading file", e);
        }

        return 0;
    }

    public void writeDirectoryEntry(DirectoryEntry directoryEntry, String filename) {
        File dir = context.getFilesDir();
        File file = new File(dir, filename);

        while (true) {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public int writeData(byte[] data, File file) {
        int sectorPos = findEmptySector(file);
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            int offset = 0;
            int sectorPosTemp = sectorPos;
            while (offset < data.length) {
                raf.seek(512L * sectorPosTemp);
                int writeLength = Math.min(dataSizeInSector, data.length - offset);
                raf.write(data, offset, writeLength);
                offset += writeLength;

                if (offset < data.length) {
                    raf.seek(512L * sectorPosTemp + 508);
                    sectorPosTemp = findEmptySector(file);
                    raf.write(intToByteArray(sectorPosTemp));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sectorPos;
    }

    public byte[] readData(int sectorPos, File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            ArrayList<Byte> byteArrayList = new ArrayList<>();
            int sectorPosTemp = sectorPos;

            while (true) {
                byte[] dataCache = new byte[dataSizeInSector];
                raf.seek(512L * sectorPosTemp);
                raf.read(dataCache, 0, dataSizeInSector);

                for (int i = 0; i < dataSizeInSector; ++i) {
                    byteArrayList.add(dataCache[i]);
                }

                byte[] nextSectorPos = new byte[4];
                raf.seek(512L * sectorPosTemp + 508);
                raf.read(nextSectorPos, 0, 4);

                if (byteArrayToInt(nextSectorPos) == 0) {
                    break;
                }

                sectorPosTemp = byteArrayToInt(nextSectorPos);
            }

            byte[] result = new byte[byteArrayList.size()];
            for (int i = 0; i < byteArrayList.size(); i++) {
                result[i] = byteArrayList.get(i);
            }
            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}



