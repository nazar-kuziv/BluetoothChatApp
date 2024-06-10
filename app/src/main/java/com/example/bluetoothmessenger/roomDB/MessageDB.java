package com.example.bluetoothmessenger.roomDB;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
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
}
