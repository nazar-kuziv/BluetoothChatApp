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
import com.example.bluetoothmessenger.roomDB.ControllerDB;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@SuppressLint({"MissingPermission", "StaticFieldLeak"})
public class AndroidBluetoothController {
    private static AndroidBluetoothController instance;
    private Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ControllerDB controllerDB = ControllerDB.getInstance();
    private final ArrayList<BluetoothContact> scannedDevices = new ArrayList<>();
    private final Object scannedDevicesLock = new Object();
    private final ArrayList<BluetoothContact> pairedDevices = new ArrayList<>();
    public static ChatUtils chatUtils;

    private AndroidBluetoothController(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            ((Activity) context).finish();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        context.registerReceiver(receiver, filter);
    }

    public static synchronized AndroidBluetoothController getInstance(Context context) {
        if (instance == null) {
            instance = new AndroidBluetoothController(context);
        } else {
            instance.context = context;
        }
        return instance;
    }

    public ArrayList<BluetoothContact> getPairedDevices() {
        return pairedDevices;
    }

    public void startDiscovery() {
        scannedDevices.clear();
        updatePairedDevices();
        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(context, "Discovery failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopDiscovery() {
        bluetoothAdapter.cancelDiscovery();
    }

    public void updatePairedDevices() {
        pairedDevices.clear();
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            String customName = controllerDB.getDeviceCustomName(device.getAddress());
            if (customName != null) {
                BluetoothContact customDevice = new BluetoothContact(customName, device.getAddress());
                pairedDevices.add(customDevice);
            } else {
                pairedDevices.add(BluetoothContactConverter.toBluetoothContact(device));
            }
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public boolean isLocationEnabled() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            return true;
        }
    }

    public boolean isVisible() {
        return bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
    }

    public boolean haveLocationPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            synchronized (scannedDevicesLock) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothContact device = BluetoothContactConverter.toBluetoothContact(Objects.requireNonNull(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
                    if (!scannedDevices.contains(device) && !pairedDevices.contains(device) && device.getName() != null) {
                        String customName = controllerDB.getDeviceCustomName(device.getMACaddress());
                        if (customName != null) {
                            device.setName(customName);
                        }
                        scannedDevices.add(device);
                        Intent resultIntent = new Intent(NEW_DEVICE_FOUND);
                        resultIntent.putExtra(DISCOVERED_DEVICE_ADDRESS, device.getMACaddress());
                        resultIntent.putExtra(DISCOVERED_DEVICE_NAME, device.getName());
                        context.sendBroadcast(resultIntent);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Intent resultIntent = new Intent(DISCOVERY_FINISHED);
                    context.sendBroadcast(resultIntent);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Intent resultIntent = new Intent(DISCOVERY_STARTED);
                    context.sendBroadcast(resultIntent);
                    Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    public static final int BLUETOOTH_ENABLE_FOR_PAIRED = 1;
    public static final int BLUETOOTH_ENABLE_FOR_SCAN = 2;
    public static final int BLUETOOTH_VISIBLE_ENABLE = 3;
    public static final int LOCATION_PERMISSION = 4;
    public static final int LOCATION_ENABLE = 5;
    public static final String NEW_DEVICE_FOUND = "NewDeviceFound";
    public static final String DISCOVERY_FINISHED = "DiscoveryFinished";
    public static final String DISCOVERY_STARTED = "DiscoveryStarted";
    public static final String DISCOVERED_DEVICE_NAME = "DiscoveredDeviceName";
    public static final String DISCOVERED_DEVICE_ADDRESS = "DiscoveredDeviceAddress";
}
