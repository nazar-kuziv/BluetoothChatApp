package com.example.bluetoothmessenger.data;

import java.util.Objects;

public class BluetoothContact {
    private String name;
    private final String MACaddress;

    public BluetoothContact(String name, String address) {
        this.name = name;
        this.MACaddress = address;
    }

    public String getMACaddress() {
        return MACaddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BluetoothContact that = (BluetoothContact) o;
        return Objects.equals(MACaddress, that.getMACaddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(MACaddress);
    }
}
