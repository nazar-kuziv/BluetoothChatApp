package com.example.bluetoothmessenger.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ChatMessage {
    private final String type;
    public String message;
    public Bitmap image;
    public boolean wroteByUser;

    public ChatMessage(byte[] message, boolean wroteByUser, String type) {
        this.type = type;
        this.wroteByUser = wroteByUser;
        if(type.equals(TEXT_MESSAGE)){
            this.message = new String(message);
            Log.e("MessageDB", "Text: " + this.message);
        }else if(type.equals(IMAGE_MESSAGE)){
            this.image = convertCompressedByteArrayToBitmap(message);
            Log.e("MessageDB", "Image: " + this.message);
        }
    }


    public boolean isTextMessage() {
        return type.equals(TEXT_MESSAGE);
    }

    public static Bitmap convertCompressedByteArrayToBitmap(byte[] src){
        return BitmapFactory.decodeByteArray(src, 0, src.length);
    }

    public static final int TEXT_MESSAGE_BYTE = 0;
    public static final int IMAGE_MESSAGE_BYTE = 1;
    public static final String TEXT_MESSAGE = "Text";
    public static final String IMAGE_MESSAGE = "Image";
}
