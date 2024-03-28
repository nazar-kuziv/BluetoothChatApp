package com.example.bluetoothmessenger.data;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

@SuppressLint("MissingPermission")
public class BluetoothContactConverter {
    public static BluetoothContact toBluetoothContact(BluetoothDevice device) {
        return new BluetoothContact(device.getName(), device.getAddress());
    }
}

