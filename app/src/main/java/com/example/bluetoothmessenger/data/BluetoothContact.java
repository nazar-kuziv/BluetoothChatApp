package com.example.bluetoothmessenger.data;

import java.io.Serializable;
import java.util.Objects;

public class BluetoothContact implements Serializable {
    private String name;
    private String MACaddress;

    public BluetoothContact(String name, String MACaddress) {
        this.name = name;
        this.MACaddress = MACaddress;
    }

    public String getMACaddress() {
        return MACaddress;
    }

    //Never use setMACaddress, it has been added only because of the need of the RoomDB
    @SuppressWarnings("unused")
    public void setMACaddress(String MACaddress) {
        this.MACaddress = MACaddress;
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
