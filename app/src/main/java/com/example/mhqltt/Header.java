package com.example.mhqltt;

import java.nio.ByteBuffer;

public class Header {
    private byte[] type;
    private byte[] size; //KB = *1024 //25.165.824 = 24GB
    private byte[] password;
    private byte[] dateCreate;
    private byte[] dateModify;
    private byte[] timeCreate;
    private byte[] timeModify;
    private byte[] ownerSign;

    public Header(byte[] type, byte[] size, byte[] password, byte[] dateCreate, byte[] dateModify, byte[] timeCreate, byte[] timeModify, byte[] ownerSign) {
        this.type = type;
        this.size = size;
        this.password = password;
        this.dateCreate = dateCreate;
        this.dateModify = dateModify;
        this.timeCreate = timeCreate;
        this.timeModify = timeModify;
        this.ownerSign = ownerSign;
    }

    public Header() {
        type = new byte[4];
        size = new byte[4];
        password = new byte[32];
        dateCreate = new byte[4];
        dateModify = new byte[4];
        timeCreate = new byte[3];
        timeModify = new byte[3];
        ownerSign = new byte[10];
    }

    public byte[] getType() {
        return type;
    }

    public void setType(byte[] type) {
        this.type = type;
    }

    public byte[] getSize() {
        return size;
    }

    public void setSize(byte[] size) {
        this.size = size;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public byte[] getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(byte[] dateCreate) {
        this.dateCreate = dateCreate;
    }

    public byte[] getDateModify() {
        return dateModify;
    }

    public void setDateModify(byte[] dateModify) {
        this.dateModify = dateModify;
    }

    public byte[] getTimeCreate() {
        return timeCreate;
    }

    public void setTimeCreate(byte[] timeCreate) {
        this.timeCreate = timeCreate;
    }

    public byte[] getTimeModify() {
        return timeModify;
    }

    public void setTimeModify(byte[] timeModify) {
        this.timeModify = timeModify;
    }

    public byte[] getOwnerSign() {
        return ownerSign;
    }

    public void setOwnerSign(byte[] ownerSign) {
        this.ownerSign = ownerSign;
    }
}
