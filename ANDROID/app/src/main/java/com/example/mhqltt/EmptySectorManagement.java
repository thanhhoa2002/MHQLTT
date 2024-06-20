package com.example.mhqltt;

public class EmptySectorManagement {
    private byte[] startPos;
    private byte[] size;

    public EmptySectorManagement() {
        startPos = new byte[4];
        size = new byte[4];
    }

    public EmptySectorManagement(byte[] startPos, byte[] size) {
        this.startPos = startPos;
        this.size = size;
    }

    public byte[] getStartPos() {
        return startPos;
    }

    public void setStartPos(byte[] startPos) {
        this.startPos = startPos;
    }

    public byte[] getSize() {
        return size;
    }

    public void setSize(byte[] size) {
        this.size = size;
    }
}
