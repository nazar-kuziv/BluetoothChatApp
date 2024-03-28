package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.BLUETOOTH_ENABLE_FOR_PAIRED;
import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.BLUETOOTH_ENABLE_FOR_SCAN;
import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.BLUETOOTH_VISIBLE_ENABLE;
import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.LOCATION_ENABLE;
import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.LOCATION_PERMISSION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothmessenger.chat.AndroidBluetoothController;
import com.example.bluetoothmessenger.data.BluetoothContact;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;

import java.util.ArrayList;
import java.util.List;

@SuppressLint({"MissingPermission", "UnspecifiedRegisterReceiverFlag"})

public class BluetoothScan extends AppCompatActivity {
    private AndroidBluetoothController bluetoothController;
    private DevicedAdapter pairedDevicesAdapter, scannedDevicesAdapter;
    private TextView scannedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth_scan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        IntentFilter filter = new IntentFilter("New Device Found");
        registerReceiver(scannedDevicesReceiver, filter);
        showPairedDevices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        setTitle("Choose device");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.scan) {
            scanDevices();
            return true;
        } else if (itemId == R.id.action_back) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothController.finish();
        unregisterReceiver(scannedDevicesReceiver);
    }

    private void init() {
        RecyclerView pairedDevicesView = findViewById(R.id.paired_devices);
        RecyclerView scannedDevicesView = findViewById(R.id.scanned_devices);
        pairedDevicesAdapter = new DevicedAdapter();
        scannedDevicesAdapter = new DevicedAdapter();
        pairedDevicesView.setAdapter(pairedDevicesAdapter);
        scannedDevicesView.setAdapter(scannedDevicesAdapter);
        pairedDevicesView.setLayoutManager(new LinearLayoutManager(this));
        scannedDevicesView.setLayoutManager(new LinearLayoutManager(this));
        scannedDevices = findViewById(R.id.scanned_devices_text);
        bluetoothController = new AndroidBluetoothController(this);
    }

    private void showPairedDevices() {
        if (!bluetoothController.isBluetoothEnabled()) {
            enableBluetoothForPaired();
        } else {
            pairedDevicesAdapter.clear();
            bluetoothController.updatePairedDevices();
            ArrayList<BluetoothContact> pairedDevices = bluetoothController.getPairedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                pairedDevicesAdapter.clear();
                for (BluetoothContact device : pairedDevices) {
                    pairedDevicesAdapter.add(device.getName(), device.getMACaddress());
                }
            }
        }
    }

    private void scanDevices() {
        if(!bluetoothController.haveLocationPermissions()){
            askForLocationPermissions();
        }else if (!bluetoothController.isBluetoothEnabled()) {
            enableBluetoothForScan();
        } else if (!bluetoothController.isLocationEnabled()) {
            enableLocation();
        } else if (!bluetoothController.isVisible()) {
            enableVisibility();
        } else{
            scannedDevices.setVisibility(View.VISIBLE);
            bluetoothController.stopDiscovery();
            scannedDevicesAdapter.clear();
            showPairedDevices();
            bluetoothController.startDiscovery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ENABLE_FOR_PAIRED) {
            if (resultCode != RESULT_OK) {
                enableBluetoothForPaired();
            } else {
                showPairedDevices();
            }
        } else if (requestCode == BLUETOOTH_ENABLE_FOR_SCAN) {
            if (resultCode != RESULT_OK) {
                enableBluetoothForScan();
            } else {
                scanDevices();
            }
        } else if (requestCode == BLUETOOTH_VISIBLE_ENABLE) {
            if (resultCode == RESULT_CANCELED) {
                enableVisibility();
            } else {
                scanDevices();
            }
        } else if (requestCode == LOCATION_ENABLE) {
            if (resultCode == Activity.RESULT_OK) {
                scanDevices();
            } else {
                enableLocation();
            }
        }
    }

    public final BroadcastReceiver scannedDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("New Device Found".equals(action)) {
                scannedDevicesAdapter.add(intent.getStringExtra("name"), intent.getStringExtra("MACaddress"));
            }
        }
    };

    private void askForLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanDevices();
            } else {
                askForLocationPermissions();
            }
        }
    }

    private void enableLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener(this, (LocationSettingsResponse response) -> {
                })
                .addOnFailureListener(this, ex -> {
                    if (ex instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(this, LOCATION_ENABLE);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error
                        }
                    }
                });
    }

    private void enableBluetoothForPaired() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_FOR_PAIRED);
    }

    private void enableBluetoothForScan() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_FOR_SCAN);
    }

    private void enableVisibility() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, BLUETOOTH_VISIBLE_ENABLE);
    }

    public static class DevicedAdapter extends RecyclerView.Adapter<DevicedAdapter.ViewHolder> {

        private final List<BluetoothContact> devices;

        public DevicedAdapter() {
            devices = new ArrayList<>();
        }

        @NonNull
        @Override
        public DevicedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.bluetooth_device, parent, false);
            return new ViewHolder(contactView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothContact device = devices.get(position);
            holder.name.setText(device.getName());
            holder.MACaddress.setText(device.getMACaddress());
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void add(String name, String MACaddress) {
            devices.add(new BluetoothContact(name, MACaddress));
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void clear() {
            devices.clear();
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public TextView MACaddress;

            public ViewHolder(View itemView) {
                super(itemView);

                name = itemView.findViewById(R.id.name);
                MACaddress = itemView.findViewById(R.id.mac_address);
            }
        }
    }

}