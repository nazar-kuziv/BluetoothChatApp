package com.example.bluetoothmessenger.roomDB;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class MessageDB {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "interlocutor_mac_address")
    public String interlocutorMACaddress;
    @ColumnInfo(name = "interlocutor_name")
    public String interlocutorName;
    @ColumnInfo(name = "sent_by_user")
    public boolean sentByUser;
    @ColumnInfo(name = "text_message")
    public boolean textMessage;
    @ColumnInfo(name = "message",typeAffinity = ColumnInfo.BLOB)
    public byte[] message;
    @ColumnInfo(name = "time")
    public String time;

    @Ignore
    public MessageDB(String interlocutorMACaddress, String interlocutorName, boolean sentByUser, boolean textMessage, byte[] message, String time) {
        this.interlocutorMACaddress = interlocutorMACaddress;
        this.interlocutorName = interlocutorName;
        this.sentByUser = sentByUser;
        this.textMessage = textMessage;
        this.message = message;
        this.time = time;
    }

    public MessageDB() {
    }
}
