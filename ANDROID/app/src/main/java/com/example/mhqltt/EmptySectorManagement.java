package com.example.mhqltt;

public class EmptySectorManagement {
    private int startPos;
    private int size;

    public EmptySectorManagement() {
        startPos = 0;
        size = 0;
    }

    public EmptySectorManagement(int startPos, int size) {
        this.startPos = startPos;
        this.size = size;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getEnd() {
        return startPos + size;
    }
}
