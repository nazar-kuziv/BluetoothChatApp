package com.example.bluetoothmessenger.roomDB;

import android.content.Context;

import androidx.room.Room;

import com.example.bluetoothmessenger.data.BluetoothContact;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ControllerDB {
    private static ControllerDB instance;
    private final MessageDAO messageDAO;

    private ControllerDB(Context context) {
        messageDAO = Room.databaseBuilder(context, AppDatabase.class, "bluetooth-messenger-db").build().messageDAO();
    }

    public static synchronized ControllerDB getInstance(Context context) {
        if (instance == null) {
            instance = new ControllerDB(context);
        }
        return instance;
    }

    public static synchronized ControllerDB getInstance() {
        if (instance == null) {
            throw new IllegalStateException(ControllerDB.class.getSimpleName() + " is not initialized, call getInstance(Context context) first.");
        }
        return instance;
    }

    public String getDeviceCustomName(String deviceMACaddress) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> callable = () -> messageDAO.getUserNameByMacAddress(deviceMACaddress);

        Future<String> future = executor.submit(callable);
        String deviceName = null;
        try {
            deviceName = future.get();
        } catch (InterruptedException | ExecutionException ignored) {
            // Ignore exceptions
        } finally {
            executor.shutdown();
        }

        return deviceName;
    }

    public List<BluetoothContact> getContactsList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<List<BluetoothContact>> callable = messageDAO::getUniqueInterlocutors;

        Future<List<BluetoothContact>> future = executor.submit(callable);
        List<BluetoothContact> contacts = null;
        try {
            contacts = future.get();
        } catch (InterruptedException | ExecutionException ignored) {
            // Ignore exceptions
        } finally {
            executor.shutdown();
        }

        return contacts;
    }

    public void deleteContactFromDB(String macAddress) {
        new Thread(() -> messageDAO.deleteAllMessagesFromUser(macAddress)).start();
    }

    public void changeContactName(String macAddress, String newName) {
        new Thread(() -> messageDAO.changeUserName(macAddress, newName)).start();
    }

    public void insertMessage(MessageDB messageDB) {
        new Thread(() -> messageDAO.insert(messageDB)).start();
    }
}

