package com.example.bluetoothmessenger.data;

public class ChatMessage {
    public String message;
    public boolean wroteByUser;

    public ChatMessage(String message, boolean wroteByUser) {
        this.message = message;
        this.wroteByUser = wroteByUser;
    }

    public String getMessage() {
        return message;
    }
}
