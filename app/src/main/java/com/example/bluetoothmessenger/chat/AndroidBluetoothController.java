package com.example.bluetoothmessenger.chat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.bluetoothmessenger.data.BluetoothContact;
import com.example.bluetoothmessenger.data.BluetoothContactConverter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@SuppressLint("MissingPermission")
public class AndroidBluetoothController{
    public static final int BLUETOOTH_ENABLE_FOR_PAIRED = 1;
    public static final int BLUETOOTH_ENABLE_FOR_SCAN = 2;
    public static final int BLUETOOTH_VISIBLE_ENABLE = 3;
    public static final int LOCATION_PERMISSION = 4;
    public static final int LOCATION_ENABLE = 5;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothContact> scannedDevices = new ArrayList<>();
    private final ArrayList<BluetoothContact> pairedDevices = new ArrayList<>();

    public AndroidBluetoothController(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            ((Activity) context).finish();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(receiver, filter);
    }

    public ArrayList<BluetoothContact> getPairedDevices() {
        return pairedDevices;
    }

    public void startDiscovery() {
        scannedDevices.clear();
        updatePairedDevices();
        if(!bluetoothAdapter.startDiscovery()){
            Toast.makeText(context, "Discovery failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopDiscovery() {
        bluetoothAdapter.cancelDiscovery();
    }

    public void finish() {
        bluetoothAdapter.cancelDiscovery();
        context.unregisterReceiver(receiver);
    }

    public void updatePairedDevices() {
        pairedDevices.clear();
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            pairedDevices.add(BluetoothContactConverter.toBluetoothContact(device));
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public boolean isLocationEnabled() {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isVisible() {
        return bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
    }

    public boolean haveLocationPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothContact device = BluetoothContactConverter.toBluetoothContact(Objects.requireNonNull(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
                if (!scannedDevices.contains(device) && !pairedDevices.contains(device) && device.getName() != null) {
                        scannedDevices.add(device);
                        Intent resultIntent = new Intent("New Device Found");
                        resultIntent.putExtra("name", device.getName());
                        resultIntent.putExtra("MACaddress", device.getMACaddress());
                        context.sendBroadcast(resultIntent);
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
            }
        }
    };



}
