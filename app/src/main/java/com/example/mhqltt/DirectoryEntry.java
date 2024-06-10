package com.example.mhqltt;

public class DirectoryEntry {
    private byte[] name;
    private byte[] format;
    private byte[] dateCreate;
    private byte[] dataPosition;
    private byte[] size;
    private byte[] state;
    private byte[] password;

    public DirectoryEntry() {
        name = new byte[28];
        format = new byte[5];
        dateCreate = new byte[6];
        dataPosition = new byte[4];
        size = new byte[4];
        state = new byte[1];
        password = new byte[32];
    }

    public DirectoryEntry(byte[] name, byte[] format, byte[] dateCreate, byte[] dataPosition, byte[] size, byte[] state, byte[] password) {
        this.name = name;
        this.format = format;
        this.dateCreate = dateCreate;
        this.dataPosition = dataPosition;
        this.size = size;
        this.state = state;
        this.password = password;
    }

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public byte[] getFormat() {
        return format;
    }

    public void setFormat(byte[] format) {
        this.format = format;
    }

    public byte[] getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(byte[] dateCreate) {
        this.dateCreate = dateCreate;
    }

    public byte[] getDataPosition() {
        return dataPosition;
    }

    public void setDataPosition(byte[] dataPosition) {
        this.dataPosition = dataPosition;
    }

    public byte[] getSize() {
        return size;
    }

    public void setSize(byte[] size) {
        this.size = size;
    }

    public byte[] getState() {
        return state;
    }

    public void setState(byte[] state) {
        this.state = state;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }
}
