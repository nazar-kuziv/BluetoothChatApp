package com.example.bluetoothmessenger.chat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class ChatUtils {
    private int state;
    private Handler handler;
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;

    public ChatUtils(Handler handler) {
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    public synchronized void startListening() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void finish() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (Exception e) {
                Log.e("ConnectThread", "Socket's create() method failed", e);
            }
            this.socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (Exception e2) {
                    Log.e("ConnectThread", "Could not close the client socket", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connected(device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e("ConnectThread", "Could not close the client socket", e);
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (Exception e) {
                Log.e("AcceptThread", "Socket's listen() method failed", e);
            }
            this.serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (Exception e) {
                Log.e("AcceptThread", "Socket's accept() method failed", e);
                try {
                    serverSocket.close();
                } catch (Exception e1) {
                    Log.e("AcceptThread", "Could not close the server socket", e1);
                }
            }
            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (Exception e) {
                            Log.e("AcceptThread", "Could not close the server socket", e);
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (Exception e) {
                Log.e("AcceptThread", "Could not close the server socket", e);
            }
        }
    }

    private synchronized void connectionFailed() {
        Message msg = handler.obtainMessage(TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Connection failed");
        msg.setData(bundle);
        handler.sendMessage(msg);
        ChatUtils.this.startListening();
    }

    private synchronized void connected(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        Message msg = handler.obtainMessage(DEVICE_NAME_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        bundle.putString(DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public static final int STATE_NONE = 1;
    public static final int STATE_LISTEN = 2;
    public static final int STATE_CONNECTING = 3;
    public static final int STATE_CONNECTED = 4;
    public static final int MESSAGE_STATE_CHANGED = 5;
    public static final int MESSAGE_READ = 6;
    public static final int MESSAGE_WRITE = 7;
    public static final int DEVICE_NAME_MESSAGE = 8;
    public static final int TOAST_MESSAGE = 9;
    public static final String TOAST = "toast";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    private static final UUID APP_UUID = UUID.fromString("7211ebe8-08cd-460a-8e6b-de55aef84723");
    private static final String APP_NAME = "BluetoothChatApp";
}
