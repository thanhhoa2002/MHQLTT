package com.example.mhqltt;

import java.nio.ByteBuffer;

public class Header {
    private byte[] type;
    private byte[] size;
    private byte[] password;

    public Header() {
        type = new byte[4];
        size = new byte[4];
        password = new byte[32];
    }

    public Header(byte[] type, byte[] size, byte[] password) {
        this.type = type;
        this.size = size;
        this.password = password;
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
}
