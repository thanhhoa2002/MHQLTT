package com.example.mhqltt;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

public class FileManager {
    private final Context context;
    static int dataSizeInSector = 507;

    public FileManager(Context context) {
        this.context = context;
    }

    public void createFile(Header header) {
        File dir = context.getFilesDir();
        File file = new File(dir, byteArrayToString(header.getType()));
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength((long) byteArrayToInt(header.getSize()) * 1048576);
            raf.seek(0);
            raf.write(header.getType());
            raf.write(header.getSize());
            raf.write(header.getPassword());
            raf.close();
            Log.d("File", "File created successfully with header");
        } catch (IOException e) {
            Log.e("File", "Error creating file", e);
        }
    }

    public Header readHeader(String filename) {
        Header header = new Header();
        File dir = context.getFilesDir();
        File file = new File(dir, filename);

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            byte[] type = new byte[4];
            byte[] size = new byte[4];
            byte[] password = new byte[32];

            raf.read(type, 0, 4);
            raf.read(size, 0, 4);
            raf.read(password, 0, 32);
            raf.close();

            header.setType(type);
            header.setSize(size);
            header.setPassword(password);
            return header;
        } catch (FileNotFoundException e) {
            Log.e("File", "File not found", e);
        } catch (IOException e) {
            Log.e("File", "Error reading file", e);
        }
        return null;
    }

    public void writeBytesToFile(byte[] bytes, String fileName) throws IOException {
        File dir = context.getFilesDir();
        File file = new File(dir, fileName);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.write(bytes);
        raf.close();
    }

    public byte[] readBytesFromFile(String fileName) throws IOException {
        File dir = context.getFilesDir();
        File file = new File(dir, fileName);
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long fileLength = raf.length();
        byte[] bytes = new byte[(int) fileLength];
        raf.read(bytes);
        raf.close();
        return bytes;
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

    public static boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        try {
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static String convertDateFormat(String date) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("ddMMyy");
        inputFormat.setLenient(false);

        try {
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return null;
        }
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

    public static void allocateSector(byte[] data) {

        for (int i = 0; i < 512; i++) {
            data[i] = 0;
        }
    }

    //cai tien neu co the
    public static boolean isNullByte(byte[] data, int size) {
        for (int i = 0; i < size; i++) {
            if (data[i] != 0)
                return false;
        }
        return true;
    }


    public int findEmptyDirectoryEntry(int posSectorAvailable, File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(512L * posSectorAvailable);
            for (int i = 0; i < 6; i++) {
                byte[] directoryEntry = new byte[80];
                raf.read(directoryEntry, 0, 80);
                if (isNullByte(directoryEntry, 80))
                    return i;
            }
            raf.close();


        } catch (FileNotFoundException e) {
            Log.e("File", "File not found", e);
        } catch (IOException e) {
            Log.e("File", "Error reading file", e);
        }

        return 6;
    }

    public int findEmptySector(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            raf.seek(512);
            for (int i = 1; i < fileLength / 512; i++) {
                byte[] sector = new byte[512];
                raf.read(sector, 0, 512);
                if (isNullByte(sector, 512))
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
        int sectorPos = 1;
        int oldSectorPos = 1;

        while (true) {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                int entryPos = findEmptyDirectoryEntry(sectorPos, file);
                if (entryPos == 6) {
                    byte[] temp = new byte[4];//vi tri tiep theo
                    raf.seek((512L * oldSectorPos + 6 * 80 + 28));
                    raf.read(temp, 0, 4);

                    if (byteArrayToInt(temp) != 0) {
                        sectorPos = byteArrayToInt(temp);
                        oldSectorPos = sectorPos;
                    }
                    else {
                        sectorPos = findEmptySector(file);
                        if (sectorPos == 0) {
                            raf.close();
                            break;
                        }
                        else {
                            raf.seek(512L * oldSectorPos + 6 * 80 + 28);
                            raf.write(intToByteArray(sectorPos));
                            oldSectorPos = sectorPos;
                        }
                    }
                    raf.close();
                } else {
                    raf.seek(512L * sectorPos + entryPos * 80L);
                    raf.write(directoryEntry.getName());
                    raf.write(directoryEntry.getFormat());
                    raf.write(directoryEntry.getDateCreate());
                    raf.write(directoryEntry.getDataPosition());
                    raf.write(directoryEntry.getSize());
                    raf.write(directoryEntry.getState());
                    raf.write(directoryEntry.getPassword());
                    raf.close();
                    break;
                }
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

