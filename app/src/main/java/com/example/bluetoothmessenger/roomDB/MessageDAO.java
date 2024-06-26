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
            "FROM MessageDB AS m1 " +
            "WHERE time = (SELECT MAX(time) FROM MessageDB AS m2 WHERE m2.interlocutor_mac_address = m1.interlocutor_mac_address) " +
            "GROUP BY interlocutor_mac_address " +
            "ORDER BY time DESC")
    List<BluetoothContact> getUniqueInterlocutors();
    @Query("SELECT interlocutor_name FROM MessageDB WHERE interlocutor_mac_address = :macAddress LIMIT 1")
    String getUserNameByMacAddress(String macAddress);
    @Query("UPDATE MessageDB SET interlocutor_name = :newName WHERE interlocutor_mac_address = :macAddress")
    void changeUserName(String macAddress, String newName);
    @Insert
    void insert(MessageDB messageDB);
}
