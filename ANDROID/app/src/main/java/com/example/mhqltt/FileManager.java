package com.example.mhqltt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import android.database.Cursor;
import android.content.ContentResolver;
import android.provider.OpenableColumns;

import javax.crypto.spec.SecretKeySpec;

public class FileManager {
    private final Context context;
    int entrySize = 256;
    int sectorSize = 8192;
    int numberOfEntries = sectorSize * 320 / entrySize;
    private UriFileHelper uriFileHelper;


    public FileManager(Context context) {
        this.context = context;
        uriFileHelper = new UriFileHelper(context);
    }

    public void getCurrentDateTimeInBytes(byte[] date, byte[] time) {
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

        String password = "";
        // input password and hash
        String ownerSign = Build.MODEL;
        // input owner sign

        byte[] date = new byte[4];
        byte[] time = new byte[3];
        getCurrentDateTimeInBytes(date, time);

        header.setType(stringToByteArray(".NEW"));
        header.setSize(intToByteArray(2608)); // 25165824
        header.setPassword(padding(stringToByteArray(password),32));
        header.setDateCreate(date);
        header.setDateModify(date);
        header.setTimeCreate(time);
        header.setTimeModify(time);
        header.setOwnerSign(padding(stringToByteArray(ownerSign), 10));

        return header;
    }

    public void updateDateTimeModifyHeader(Header header, RandomAccessFile raf) throws IOException {
        raf.seek(44);
        raf.write(header.getDateModify());
        raf.seek(51);
        raf.write(header.getTimeModify());
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

    private void writeHeader(RandomAccessFile raf, Header header) throws IOException {
        raf.seek(0); // Di chuyển con trỏ file tới vị trí đầu tiên

        raf.write(header.getType());
        raf.write(header.getSize());
        raf.write(header.getPassword());
        raf.write(header.getDateCreate());
        raf.write(header.getDateModify());
        raf.write(header.getTimeCreate());
        raf.write(header.getTimeModify());
        raf.write(header.getOwnerSign());
    }

    public Header createVolume() {
        Header header = createHeader();

        File dir = context.getFilesDir();
        File file = new File(dir, byteArrayToString(header.getType()));

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength((long)byteArrayToInt(header.getSize()) * 1024);

            // Header
            writeHeader(raf, header);

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

    public void writeImageFile(Uri imageUri, Header header) throws IOException {
        String imagePath = uriFileHelper.getRealPathFromURI(imageUri);
        File imageFile = new File(imagePath);
        byte[] cache = readFileToBytes(imageFile);
        int cacheSectorSize = (cache.length + sectorSize - 1) / sectorSize;

        File dir = context.getFilesDir();
        File file = new File(dir, byteArrayToString(header.getType()));
        int dataPos = (int) (file.length() / sectorSize);

        // Write data
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(cache);

            byte[] padding = new byte[cacheSectorSize * sectorSize - cache.length];
            fos.write(padding);
        }

        // Write entry and update header
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            DirectoryEntry entry = imageUriToEntry(imageUri, dataPos);
            writeImageFileDirectoryEntry(raf, entry);

            // Update header time
            byte[] date = new byte[4];
            byte[] time = new byte[3];
            getCurrentDateTimeInBytes(date, time);
            header.setDateModify(date);
            header.setTimeModify(time);
            updateDateTimeModifyHeader(header, raf);
        }
    }

    public DirectoryEntry imageUriToEntry(Uri imageUri, int dataPos) {
        DirectoryEntry entry = new DirectoryEntry();

        String[] fileName = splitFileName(uriFileHelper.getFileName(imageUri));
        String dateCreate = uriFileHelper.getFileCreationDate(imageUri);
        long fileSize = uriFileHelper.getFileSize(imageUri);

        Log.d("SIZE", String.valueOf(fileSize));

        entry.setName(padding(stringToByteArray(fileName[0]), 160));
        entry.setExtendedName(padding(stringToByteArray(fileName[1]), 5));
        entry.setDateCreate(stringDateToByteArray(dateCreate));
        entry.setDataPos(intToByteArray(dataPos));
        entry.setSize(intToByteArray((int)fileSize));
        entry.setState(stringToByteArray("0"));
        entry.setPassword(padding(stringToByteArray(""), 32));
        entry.setEncrypt(stringToByteArray("0"));

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
            raf.write(directoryEntry.getEncrypt());

            Log.d("File", "Write entry table successfully");
        } catch (IOException e) {
            Log.e("File", "Write entry table fail", e);
        }
    }

    private long findNextEmptyEntrySlot(RandomAccessFile raf) throws IOException {
        raf.seek(sectorSize * 6L);

        byte[] buffer = new byte[entrySize];

        for (int i = 0; i < numberOfEntries; ++i) {
            raf.read(buffer);
            if (isBufferEmpty(buffer)) {
                return i;
            }
        }
        return -1;
    }

    public byte[] readImageFileData(RandomAccessFile raf, int dataPos, int dataSize) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(raf.getFD()))) {
            raf.seek((long) dataPos * sectorSize);

            byte[] cache = new byte[dataSize];
            int bytesRead = bis.read(cache, 0, dataSize);
            if (bytesRead < dataSize) {
                throw new IOException("Unable to read the required amount of data.");
            }

            return cache;
        }
    }

    public DirectoryEntry readFileEntry(RandomAccessFile raf, int orderOfEntry) throws IOException {
        int totalSize = 160 + 5 + 4 + 4 + 4 + 1 + 32 + 1;
        byte[] buffer = new byte[totalSize];

        raf.seek( sectorSize * 6L + (long) orderOfEntry * entrySize);
        raf.readFully(buffer);

        if (isBufferEmpty(buffer)) {
            return null;
        }

        int offset = 0;
        byte[] name = new byte[160];
        System.arraycopy(buffer, offset, name, 0, 160);
        offset += 160;

        byte[] extendedName = new byte[5];
        System.arraycopy(buffer, offset, extendedName, 0, 5);
        offset += 5;

        byte[] dateCreate = new byte[4];
        System.arraycopy(buffer, offset, dateCreate, 0, 4);
        offset += 4;

        byte[] dataPos = new byte[4];
        System.arraycopy(buffer, offset, dataPos, 0, 4);
        offset += 4;

        byte[] size = new byte[4];
        System.arraycopy(buffer, offset, size, 0, 4);
        offset += 4;

        byte[] state = new byte[1];
        System.arraycopy(buffer, offset, state, 0, 1);
        offset += 1;

        byte[] password = new byte[32];
        System.arraycopy(buffer, offset, password, 0, 32);
        offset += 32;

        byte[] encrypt = new byte[1];
        System.arraycopy(buffer, offset, encrypt, 0, 1);

        return new DirectoryEntry(name, extendedName, dateCreate, dataPos, size, state, password, encrypt);
    }

    public List<DirectoryEntry> readAllEntries(RandomAccessFile raf) throws IOException {
        List<DirectoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < numberOfEntries; ++i) {
            DirectoryEntry entry = readFileEntry(raf, i);
            entries.add(entry);
        }
        return entries;
    }

    public int countFileInList(List<DirectoryEntry> entries){
        int count = 0;
        for (DirectoryEntry entry : entries) {
            if (entry != null) {
                ++count;
            }
        }
        return count;
    }

    public void tempDeleteFile(RandomAccessFile raf, List<DirectoryEntry> entries, int orderOfEntry) throws IOException {
        raf.seek(sectorSize * 6L + (long) orderOfEntry * entrySize + 177);
        raf.write(stringToByteArray("1"));

        DirectoryEntry entry = readFileEntry(raf, orderOfEntry);
        entries.set(orderOfEntry, entry);
    }

    public void restoreTempFile(RandomAccessFile raf, List<DirectoryEntry> entries, int orderOfEntry) throws IOException {
        raf.seek(sectorSize * 6L + (long) orderOfEntry * entrySize + 177);
        raf.write(stringToByteArray("0"));

        DirectoryEntry entry = readFileEntry(raf, orderOfEntry);
        entries.set(orderOfEntry, entry);
    }

    public EmptySectorManagement fullDeleteFile(RandomAccessFile raf, List<DirectoryEntry> entries, int orderOfEntry) throws IOException {
        // Initialize EmptySectorManagement
        EmptySectorManagement esm = new EmptySectorManagement();

        // Calculate base position only once
        long basePos = sectorSize * 6L + (long) orderOfEntry * entrySize;

        byte[] temp = new byte[4];

        // Read start position
        raf.seek(basePos + 169);
        raf.readFully(temp);
        esm.setStartPos(byteArrayToInt(temp));

        // Read size
        raf.seek(basePos + 173);
        raf.readFully(temp);
        esm.setSize(roundUp(byteArrayToInt(temp), 8192));

        // Create and write padding
        byte[] tempStr = new byte[entrySize];
        Arrays.fill(tempStr, (byte) 0);  // Assuming padding fills with zeros
        raf.seek(basePos);
        raf.write(tempStr);

        // Set the directory entry to null
        entries.set(orderOfEntry, null);

        return esm;
    }

    public void deleteDataFile(RandomAccessFile raf, EmptySectorManagement esm) throws IOException {
        byte[] buffer = new byte[esm.getSize() * sectorSize];
        Arrays.fill(buffer, (byte) 0);

        raf.seek((long) esm.getStartPos() * sectorSize);
        raf.write(buffer);
    }

    public List<EmptySectorManagement> emptyAreaProcessing(List<EmptySectorManagement> lesm, EmptySectorManagement esm) {
        List<EmptySectorManagement> result = new ArrayList<>();

        int newStart = esm.getStartPos();
        int newEnd = esm.getEnd();

        // Iterate over the list only once
        for (int i = 0; i < lesm.size(); i++) {
            EmptySectorManagement current = lesm.get(i);

            if (current.getEnd() < newStart) {
                result.add(current);
            } else if (current.getStartPos() <= newEnd) {
                newStart = Math.min(newStart, current.getStartPos());
                newEnd = Math.max(newEnd, current.getEnd());
            } else {
                result.add(new EmptySectorManagement(newStart, newEnd - newStart));
                result.addAll(lesm.subList(i, lesm.size()));
                return result;
            }
        }

        result.add(new EmptySectorManagement(newStart, newEnd - newStart));
        return result;
    }


    public List<EmptySectorManagement> readEmptyArea(RandomAccessFile raf) throws IOException {
        List<EmptySectorManagement> lesm = new ArrayList<>();
        byte[] temp = new byte[8];
        long limit = sectorSize * 6L;
        long currentPosition = sectorSize;

        raf.seek(currentPosition);

        while (currentPosition + 8 <= limit) {
            if (raf.read(temp) != 8) {
                break; // End of file or read error
            }

            ByteBuffer buffer = ByteBuffer.wrap(temp);
            int startPos = buffer.getInt();
            int size = buffer.getInt();

            if (startPos == 0 && size == 0) {
                break; // End of relevant data
            }

            lesm.add(new EmptySectorManagement(startPos, size));

            currentPosition += 8;
        }

        return lesm;
    }

    public void writeEmptyArea(RandomAccessFile raf, List<EmptySectorManagement> lesm) throws IOException {
        raf.seek(sectorSize);

        ByteBuffer buffer = ByteBuffer.allocate(lesm.size() * 8);
        for (EmptySectorManagement esm : lesm) {
            buffer.put(intToByteArray(esm.getStartPos()));
            buffer.put(intToByteArray(esm.getSize()));
        }

        raf.write(buffer.array());

        int remainingBytes = (int) (sectorSize * 5L - lesm.size() * 8);
        if (remainingBytes > 0) {
            raf.write(new byte[remainingBytes]);
        }
    }

    public void encryptFile(RandomAccessFile raf, List<DirectoryEntry> entries, int orderOfEntry, SecretKeySpec keySpec) throws IOException {
        long basePos = sectorSize * 6L + (long) orderOfEntry * entrySize;

        byte[] temp = new byte[4];

        // Read start position
        raf.seek(basePos + 169);
        raf.readFully(temp);
        long startPos = byteArrayToInt(temp);

        // Read size
        raf.seek(basePos + 173);
        raf.readFully(temp);
        int size = roundUp(byteArrayToInt(temp), 8192);

        byte[] buffer = new byte[sectorSize];
        byte[] encryptedChunk;
        for (long pos = startPos * sectorSize; pos < (startPos + size) * sectorSize; pos += sectorSize) {
            raf.seek(pos);
            int bytesRead = raf.read(buffer, 0, sectorSize);
            if (bytesRead > 0) {
                try {
                    Log.d("AES", "buf"+buffer.length);
                    encryptedChunk = AES.encrypt(Arrays.copyOf(buffer, bytesRead), keySpec);
                    raf.seek(pos);
                    raf.write(encryptedChunk, 0, encryptedChunk.length);
                    Log.d("AES", "en"+encryptedChunk.length);
                } catch (Exception e) {
                    throw new IOException("Error encrypting chunk", e);
                }
            }
        }

        byte[] keyBytes = keySpec.getEncoded();
        raf.seek(basePos + 178);
        raf.write(keyBytes);

        raf.write(stringToByteArray("1"));

        DirectoryEntry entry = readFileEntry(raf, orderOfEntry);
        entries.set(orderOfEntry, entry);
    }

    public void decryptFile(RandomAccessFile raf, List<DirectoryEntry> entries, int orderOfEntry, SecretKeySpec keySpec) throws IOException {
        long basePos = sectorSize * 6L + (long) orderOfEntry * entrySize;

        byte[] temp = new byte[4];

        // Read start position
        raf.seek(basePos + 169);
        raf.readFully(temp);
        long startPos = byteArrayToInt(temp);

        // Read size
        raf.seek(basePos + 173);
        raf.readFully(temp);
        int size = roundUp(byteArrayToInt(temp), 8192);


        byte[] buffer = new byte[sectorSize];
        byte[] decryptedChunk;
        for (long pos = startPos * sectorSize; pos < (startPos + size) * sectorSize; pos += sectorSize) {
            raf.seek(pos);
            int bytesRead = raf.read(buffer, 0, sectorSize);
            if (bytesRead > 0) {
                try {
                    decryptedChunk = AES.decrypt(Arrays.copyOf(buffer, bytesRead), keySpec);
//                    Log.d("AES", "de"+filedecryptedChunk.length);
                    raf.seek(pos);
                    raf.write(decryptedChunk, 0, decryptedChunk.length);
                    Log.d("AES", "de"+decryptedChunk.length);
                } catch (Exception e) {
                    throw new IOException("Error encrypting chunk", e);
                }
            }
        }

        byte[] tempBuffer = new byte[32];
        raf.seek(basePos + 178);
        raf.write(tempBuffer);

        raf.seek(basePos + 210);
        raf.write(stringToByteArray("0"));

        DirectoryEntry entry = readFileEntry(raf, orderOfEntry);
        entries.set(orderOfEntry, entry);
    }

    public void reconstructFolder() {
        File dir = context.getFilesDir();
        File oldFile = new File(dir, ".NEW");
        File newFile = new File(dir, ".NEW_TEMP");

        try {
            RandomAccessFile rafOld = new RandomAccessFile(oldFile, "r");
            RandomAccessFile rafNew = new RandomAccessFile(newFile, "rw");

            Header header = readHeader();

            header.setSize(intToByteArray(2608));
            rafNew.setLength((long)byteArrayToInt(header.getSize()) * 1024);
            byte[] date = new byte[4];
            byte[] time = new byte[3];
            getCurrentDateTimeInBytes(date, time);
            header.setDateModify(date);
            header.setTimeModify(time);

            writeHeader(rafNew, header);

            List<DirectoryEntry> entries = readAllEntries(rafOld);

            for (DirectoryEntry entry : entries) {
                if (entry != null && Arrays.equals(entry.getState(), stringToByteArray("0")) && Arrays.equals(entry.getEncrypt(), stringToByteArray("0"))) {
                    int dataPos = byteArrayToInt(entry.getDataPos());
                    int dataSize = byteArrayToInt(entry.getSize());
                    byte[] data = readImageFileData(rafOld, dataPos, dataSize);

                    int newDataPos = (int) (rafNew.length() / sectorSize);

                    try (FileOutputStream fos = new FileOutputStream(newFile, true)) {
                        fos.write(data);

                        int cacheSectorSize = (data.length + sectorSize - 1) / sectorSize;
                        byte[] padding = new byte[cacheSectorSize * sectorSize - data.length];
                        fos.write(padding);
                    }

                    entry.setDataPos(intToByteArray(newDataPos));
                    writeImageFileDirectoryEntry(rafNew, entry);
                }
            }

            rafOld.close();
            rafNew.close();

            if (oldFile.delete()) {
                newFile.renameTo(oldFile);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] padding(byte[] data, int size) {
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

    public String byteArrayToString(byte[] bytes) {
        return new String(bytes).trim();
    }

    public byte[] stringToByteArray(String str) {
        return str.getBytes();
    }

    public byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public int byteArrayToInt(byte[] bytes) {
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        return wrapped.getInt();
    }

    private boolean isBufferEmpty(byte[] buffer) {
        for (byte b : buffer) {
            if (b != 0) {
                return false;
            }
        }
        return true;
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

    public byte[] stringDateToByteArray(String dateString) {
        String[] dateParts = dateString.split("-");
        byte[] date = new byte[4];

        int year = Integer.parseInt(dateParts[2]);
        byte[] yearBytes = intToByteArray(year);

        date[0] = (byte)Integer.parseInt(dateParts[0]);
        date[1] = (byte)Integer.parseInt(dateParts[1]);
        date[2] = yearBytes[2];
        date[3] = yearBytes[3];

        return date;
    }

    private int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }

    public void filterEntriesByMonthYear(List<DirectoryEntry> entries, int year, int month){
        List<DirectoryEntry> filteredEntries = new ArrayList<>();
        byte[] dateCreates;
        for(int i=0;i<entries.size();i++){
            dateCreates = new byte[4];
            if(entries.get(i)!=null){
                for (int k = 0; k < 4; k++) {
                    dateCreates[k] = entries.get(i).getDateCreate()[k];
                }
                int monthEntry = dateCreates[1];
                int yearEntry = (dateCreates[2] & 0xFF) << 8 | (dateCreates[3] & 0xFF);
                Log.d("TAG","month"+ monthEntry);
                if(monthEntry==month+1 && yearEntry==year)
                {
                    Log.d("TAG","vao day");
                    filteredEntries.add(entries.get(i));
                }
            }
        }
        entries.clear();
        entries.addAll(filteredEntries);

    }


    public void sortEntriesBasedOnDateCreate(List<DirectoryEntry> entries,int temp) {
        boolean swapped;
        byte[] dateCreates;
        byte[] dateCreates1;

        for (int i = 0; i < entries.size() - 1; i++) {
            swapped = false;
            for (int j = 0; j < entries.size() - i - 1; j++) {
                if (entries.get(j) == null || entries.get(j + 1) == null) {
                    continue; // Skip null entries
                }

                dateCreates = new byte[4];
                dateCreates1 = new byte[4];

                for (int k = 0; k < 4; k++) {
                    dateCreates[k] = entries.get(j).getDateCreate()[k];
                    dateCreates1[k] = entries.get(j + 1).getDateCreate()[k];
                }

                ByteArrayDateComparator comparator = new ByteArrayDateComparator();
                if(temp==2)
                {
                    if (comparator.compare(dateCreates, dateCreates1) > 0) {
                        // Swap DirectoryEntry objects
                        DirectoryEntry tempEntry = entries.get(j);
                        entries.set(j, entries.get(j + 1));
                        entries.set(j + 1, tempEntry);
                        swapped = true;
                    }
                }
                else{
                    if (comparator.compare(dateCreates, dateCreates1) < 0) {
                        // Swap DirectoryEntry objects
                        DirectoryEntry tempEntry = entries.get(j);
                        entries.set(j, entries.get(j + 1));
                        entries.set(j + 1, tempEntry);
                        swapped = true;
                    }
                }
            }

            if (!swapped) {
                break;
            }
        }
    }

    public class ByteArrayDateComparator implements Comparator<byte[]> {
        @Override
        public int compare(byte[] date1, byte[] date2) {
            // Giả sử các mảng có độ dài 4
            // day: date[0], month: date[1], year: (date[2] << 8) + date[3]

            int day1 = date1[0];
            int month1 = date1[1];
            int year1 = (date1[2] & 0xFF) << 8 | (date1[3] & 0xFF);

            int day2 = date2[0];
            int month2 = date2[1];
            int year2 = (date2[2] & 0xFF) << 8 | (date2[3] & 0xFF);

            // So sánh năm trước
            if (year1 != year2) {
                return Integer.compare(year1, year2);
            }
            // So sánh tháng nếu năm bằng nhau
            if (month1 != month2) {
                return Integer.compare(month1, month2);
            }
            // So sánh ngày nếu tháng và năm bằng nhau
            return Integer.compare(day1, day2);
        }
    }

    public Bitmap decodeSampledBitmapFromData(byte[] data, int reqWidth, int reqHeight) {
        // Bước 1: Đọc kích thước ảnh mà không tải nó vào bộ nhớ
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Bước 2: Tính toán inSampleSize để giảm kích thước ảnh
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Bước 3: Đọc ảnh thật với inSampleSize đã tính toán
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Kích thước ảnh gốc
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Tính toán tỷ lệ mẫu
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Tăng inSampleSize cho đến khi ảnh giảm xuống dưới kích thước yêu cầu
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}