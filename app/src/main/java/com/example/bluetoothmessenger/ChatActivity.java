package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.ChatUtils.DEVICE_NAME;
import static com.example.bluetoothmessenger.chat.ChatUtils.DEVICE_NAME_MESSAGE;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_READ;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_STATE_CHANGED;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_WRITE;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST_MESSAGE;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bluetoothmessenger.chat.AndroidBluetoothController;
import com.example.bluetoothmessenger.chat.ChatUtils;
import com.example.bluetoothmessenger.data.BluetoothContact;

public class ChatActivity extends AppCompatActivity {
    private BluetoothContact contact;
    private ChatUtils chatUtils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        contact = new BluetoothContact(getIntent().getStringExtra("name"), getIntent().getStringExtra("macAddress"));
        chatUtils = AndroidBluetoothController.chatUtils;
        chatUtils.setHandler(handler);
        Toast.makeText(this, contact.getName() + " " + contact.getMACaddress(), Toast.LENGTH_SHORT).show();
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1) {
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected to " + contact.getName());
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_LISTEN:
                        case ChatUtils.STATE_NONE:
                            setState("Not connected");
                            break;
                    }
                case MESSAGE_READ:
                    break;
                case MESSAGE_WRITE:
                    break;
                case DEVICE_NAME_MESSAGE:
                    contact = new BluetoothContact(msg.getData().getString(DEVICE_NAME), contact.getMACaddress());
                    Toast.makeText(ChatActivity.this, contact.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(ChatActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        setTitle(contact.getName());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_back) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        if(chatUtils != null) {
            chatUtils.finish();
        }
        super.onDestroy();
    }

    private void setState(CharSequence state) {
        getSupportActionBar().setSubtitle(state);
    }
}