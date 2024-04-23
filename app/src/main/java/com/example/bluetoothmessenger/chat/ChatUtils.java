package com.example.bluetoothmessenger.chat;

import static com.example.bluetoothmessenger.data.ChatMessage.IMAGE_MESSAGE_BYTE;
import static com.example.bluetoothmessenger.data.ChatMessage.TEXT_MESSAGE_BYTE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class ChatUtils {
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ListeningThread listeningThread;
    private CommunicationThread communicationThread;
    private Handler handler;
    private int state;

    public ChatUtils(Handler handler) {
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
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
        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }
        if (listeningThread != null) {
            listeningThread.cancel();
            listeningThread = null;
        }
        listeningThread = new ListeningThread();
        listeningThread.start();
        setState(STATE_LISTEN);
    }

    public synchronized void finish() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (listeningThread != null) {
            listeningThread.cancel();
            listeningThread = null;
        }
        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }
        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        setState(STATE_CONNECTING);
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }

        communicationThread = new CommunicationThread(socket);

        communicationThread.start();

        Message msg = handler.obtainMessage(DEVICE_NAME_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTED_DEVICE_NAME, device.getName());
        bundle.putString(CONNECTED_DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    private synchronized void connectionFailed() {
        Message msg = handler.obtainMessage(TOAST_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Connection failed");
        msg.setData(bundle);
        handler.sendMessage(msg);
        ChatUtils.this.startListening();
    }

    public void sendImage(byte[] img) {
        if (communicationThread != null){
            communicationThread.startSendingImage();
            int subArraySize = 1024;
            byte[] imgSize = String.valueOf(img.length).getBytes();
            Log.e("Image size", String.valueOf(img.length));
            byte[] imgSizeModified = new byte[imgSize.length + 1];
            imgSizeModified[0] = IMAGE_MESSAGE_BYTE;
            System.arraycopy(imgSize, 0, imgSizeModified, 1, imgSize.length);
            communicationThread.write(imgSizeModified);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("Image", "Error occurred when sleeping", e);
            }
            for (int i = 0; i < img.length; i += subArraySize) {
                int numberOfBytes = Math.min(subArraySize, img.length - i);
                byte[] currentPackage = new byte[numberOfBytes];
                System.arraycopy(img, i, currentPackage, 0, numberOfBytes);
                communicationThread.write(currentPackage);
            }
            communicationThread.stopSendingImage();
            handler.obtainMessage(IMAGE_WRITE, img.length, -1, img).sendToTarget();
        }
    }

    public void sendText(String message){
        if(communicationThread != null){
            byte[] messageBytes = message.getBytes();
            byte[] sendMessage = new byte[messageBytes.length + 1];
            sendMessage[0] = TEXT_MESSAGE_BYTE;
            System.arraycopy(messageBytes, 0, sendMessage, 1, messageBytes.length);
            communicationThread.write(sendMessage);
        }
    }

    public synchronized void connectionLost() {
        if(communicationThread != null){
            Message msg = handler.obtainMessage(TOAST_MESSAGE);
            Bundle bundle = new Bundle();
            bundle.putString(TOAST, "Connection lost");
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
        ChatUtils.this.finish();
    }

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public ListeningThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (Exception e) {
                Log.e("ListeningThread", "Socket's listen() method failed", e);
            }
            this.serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (Exception e) {
                Log.e("ListeningThread", "Socket's accept() method failed", e);
                try {
                    serverSocket.close();
                } catch (Exception e1) {
                    Log.e("ListeningThread", "Could not close the server socket", e1);
                }
            }
            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (Exception e) {
                            Log.e("ListeningThread", "Could not close the server socket", e);
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (Exception e) {
                Log.e("ListeningThread", "Could not close the server socket", e);
            }
        }
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

            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e("ConnectThread", "Could not close the client socket", e);
            }
        }
    }

    private class CommunicationThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] img;
        private boolean areWeRetrievingImage = false;
        private boolean areWeSendingImage = false;
        private int imgSize = 0;

        private CommunicationThread(BluetoothSocket socket){
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (Exception e){
                Log.e("CommunicationThread", "Error occurred when creating input and output streams", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                Log.e("Image", String.valueOf(areWeRetrievingImage));
                try {
                    bytes = inputStream.read(buffer);
                    if(areWeRetrievingImage){
                        Log.e("Image Length", String.valueOf(img.length));
                        Log.e("Image Size", String.valueOf(imgSize));
                        System.arraycopy(buffer, 0, img, img.length - imgSize, bytes);
                        imgSize -= bytes;
                        if(imgSize <= 0){
                            handler.obtainMessage(IMAGE_READ, img.length, -1, img).sendToTarget();
                            areWeRetrievingImage = false;
                        }
                    }else if(buffer[0] == IMAGE_MESSAGE_BYTE) {
                        areWeRetrievingImage = true;
                        String test = new String(buffer, 1, bytes - 1);
                        imgSize = Integer.parseInt(test);
                        img = new byte[imgSize];
                    }else if(buffer[0] == TEXT_MESSAGE_BYTE) {
                        byte[] textMessage = new byte[bytes - 1];
                        System.arraycopy(buffer, 1, textMessage, 0, bytes - 1);
                        handler.obtainMessage(MESSAGE_READ, bytes - 1, -1, textMessage).sendToTarget();
                    }
                } catch (Exception e) {
                    Log.e("CommunicationThread", "Error occurred when reading from input stream", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                if(!areWeSendingImage)
                    handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }catch (Exception e){
                Log.e("CommunicationThread", "Error occurred when writing to output stream", e);
            }
        }

        public void cancel(){
            try{
                socket.close();
            }catch (Exception e){
                Log.e("CommunicationThread", "Error occurred when closing the connected socket", e);
            }
        }

        public void startSendingImage(){
            areWeSendingImage = true;
        }
        public void stopSendingImage(){
            areWeSendingImage = false;
        }
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
    public static final int IMAGE_READ = 10;
    public static final int IMAGE_WRITE = 11;
    public static final String TOAST = "Toast";
    public static final String CONNECTED_DEVICE_NAME = "ConnectedDeviceName";
    public static final String CONNECTED_DEVICE_ADDRESS = "ConnectedDeviceAddress";
    private static final UUID APP_UUID = UUID.fromString("7211ebe8-08cd-460a-8e6b-de55aef84723");
    private static final String APP_NAME = "BluetoothChatApp";
}