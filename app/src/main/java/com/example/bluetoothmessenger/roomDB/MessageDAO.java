package com.example.bluetoothmessenger.roomDB;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.bluetoothmessenger.data.BluetoothContact;

import java.util.List;

@Dao
public interface MessageDAO {
    @Query("SELECT * FROM MessageDB WHERE interlocutor_mac_address = :macAddress ORDER BY time ASC")
    List<MessageDB> getAllMessagesForUser(String macAddress);
    @Query("DELETE FROM MessageDB WHERE interlocutor_mac_address = :macAddress")
    void deleteAllMessagesFromUser(String macAddress);
    @Query("SELECT interlocutor_mac_address AS MACaddress, interlocutor_name AS name " +
            "FROM MessageDB " +
            "WHERE time = (SELECT MAX(time) FROM MessageDB AS subQuery WHERE subQuery.interlocutor_mac_address = MessageDB.interlocutor_mac_address) " +
            "ORDER BY time DESC")
    List<BluetoothContact> getUniqueInterlocutors();
    @Query("UPDATE MessageDB SET interlocutor_name = :newName WHERE interlocutor_mac_address = :macAddress")
    void changeUserName(String macAddress, String newName);
    @Insert
    void insert(MessageDB messageDB);
}
