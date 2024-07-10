package com.example.mhqltt;

public class DirectoryEntry {
    private byte[] name;
    private byte[] extendedName;
    private byte[] dateCreate;
    private byte[] dataPos;
    private byte[] size;
    private byte[] state;
    private byte[] password;
    private byte[] encrypt;

    public DirectoryEntry() {
        name = new byte[160];
        extendedName = new byte[5];
        dateCreate = new byte[4];
        dataPos = new byte[4];
        size = new byte[4];
        state = new byte[1];
        password = new byte[32];
        encrypt = new byte[1];
    }

    public DirectoryEntry(byte[] name, byte[] extendedName, byte[] dateCreate, byte[] dataPos, byte[] size, byte[] state, byte[] password, byte[] encrypt) {
        this.name = name;
        this.extendedName = extendedName;
        this.dateCreate = dateCreate;
        this.dataPos = dataPos;
        this.size = size;
        this.state = state;
        this.password = password;
        this.encrypt = encrypt;
    }

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public byte[] getExtendedName() {
        return extendedName;
    }

    public void setExtendedName(byte[] extendedName) {
        this.extendedName = extendedName;
    }

    public byte[] getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(byte[] dateCreate) {
        this.dateCreate = dateCreate;
    }

    public byte[] getDataPos() {
        return dataPos;
    }

    public void setDataPos(byte[] dataPos) {
        this.dataPos = dataPos;
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

    public byte[] getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(byte[] encrypt) {
        this.encrypt = encrypt;
    }
}
