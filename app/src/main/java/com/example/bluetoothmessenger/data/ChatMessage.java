package com.example.bluetoothmessenger.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.bluetoothmessenger.roomDB.MessageDB;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private final String type;
    public String message;
    public Bitmap image;
    public boolean sentByUser;

    public ChatMessage(byte[] message, boolean sentByUser, String type) {
        this.type = type;
        this.sentByUser = sentByUser;
        if (type.equals(TEXT_MESSAGE)) {
            this.message = new String(message);
            Log.e("Message", "Text: " + this.message);
        } else if (type.equals(IMAGE_MESSAGE)) {
            this.image = convertCompressedByteArrayToBitmap(message);
            Log.e("Message", "Image: " + this.message);
        }
    }


    public boolean isTextMessage() {
        return type.equals(TEXT_MESSAGE);
    }

    public static Bitmap convertCompressedByteArrayToBitmap(byte[] src) {
        return BitmapFactory.decodeByteArray(src, 0, src.length);
    }

    public static List<ChatMessage> convertFromMessageDB(List<MessageDB> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (MessageDB messageDB : messages) {
            if (messageDB.textMessage) {
                chatMessages.add(new ChatMessage(messageDB.message, messageDB.sentByUser, TEXT_MESSAGE));
            } else {
                chatMessages.add(new ChatMessage(messageDB.message, messageDB.sentByUser, IMAGE_MESSAGE));
            }
        }
        return chatMessages;
    }

    public static final int TEXT_MESSAGE_BYTE = 0;
    public static final int IMAGE_MESSAGE_BYTE = 1;
    public static final String TEXT_MESSAGE = "Text";
    public static final String IMAGE_MESSAGE = "Image";
}
