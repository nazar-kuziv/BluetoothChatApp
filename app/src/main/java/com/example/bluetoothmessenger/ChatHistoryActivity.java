package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.BLUETOOTH_ENABLE_FOR_PAIRED;
import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_ADDRESS;
import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_NAME;
import static com.example.bluetoothmessenger.chat.ChatUtils.DEVICE_NAME_MESSAGE;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_STATE_CHANGED;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST_MESSAGE;
import static com.example.bluetoothmessenger.data.ChatMessage.IMAGE_MESSAGE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothmessenger.chat.AndroidBluetoothController;
import com.example.bluetoothmessenger.chat.ChatUtils;
import com.example.bluetoothmessenger.data.BluetoothContact;
import com.example.bluetoothmessenger.data.ChatMessage;
import com.example.bluetoothmessenger.roomDB.ControllerDB;
import com.example.bluetoothmessenger.roomDB.MessageDB;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryActivity extends AppCompatActivity {
    private AndroidBluetoothController bluetoothController;
    private final ControllerDB controllerDB = ControllerDB.getInstance();
    private BluetoothContact historyContact;
    private BluetoothContact bluetoothConnectionContact;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        historyContact = new BluetoothContact(getIntent().getStringExtra(CONNECTED_DEVICE_NAME), getIntent().getStringExtra(CONNECTED_DEVICE_ADDRESS));
        setChatAdapter();
        setPreviousMessages();
    }

    @Override
    protected void onResume() {
        bluetoothController = AndroidBluetoothController.getInstance(this);
        startListening();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_history_menu, menu);
        setTitle(historyContact.getName());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_back) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.connect_button) {
            if (!bluetoothController.isBluetoothEnabled()) {
                enableBluetooth();
            } else {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                AndroidBluetoothController.chatUtils.connect(bluetoothAdapter.getRemoteDevice(historyContact.getMACaddress()));
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void setChatAdapter() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatAdapter = new ChatAdapter();
        LinearLayoutManager manager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(manager);
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                chatRecyclerView.postDelayed(() -> chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1), 100);
            }
        });
    }

    private void setPreviousMessages() {
        List<MessageDB> messagesFromDB = controllerDB.getMessagesFromUser(historyContact.getMACaddress());
        if (messagesFromDB != null) {
            List<ChatMessage> previousMessages = ChatMessage.convertFromMessageDB(messagesFromDB);
            chatAdapter.addMessages(previousMessages);
        }
    }

    private void startListening() {
        if (!bluetoothController.isBluetoothEnabled()) {
            enableBluetooth();
        } else {
            if (AndroidBluetoothController.chatUtils == null) {
                AndroidBluetoothController.chatUtils = new ChatUtils(handler);
            } else {
                AndroidBluetoothController.chatUtils.finish();
                AndroidBluetoothController.chatUtils.setHandler(handler);
            }
            AndroidBluetoothController.chatUtils.startListening();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_FOR_PAIRED);
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1) {
                        case ChatUtils.STATE_CONNECTED:
                            Intent intent = new Intent(ChatHistoryActivity.this, ChatActivity.class);
                            intent.putExtra(CONNECTED_DEVICE_ADDRESS, bluetoothConnectionContact.getMACaddress());
                            intent.putExtra(CONNECTED_DEVICE_NAME, bluetoothConnectionContact.getName());
                            startActivity(intent);
                            Toast.makeText(ChatHistoryActivity.this, "Connected to " + bluetoothConnectionContact.getName(), Toast.LENGTH_SHORT).show();
                            finish();
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            break;
                    }
                    break;
                case DEVICE_NAME_MESSAGE:
                    bluetoothConnectionContact = new BluetoothContact(msg.getData().getString(CONNECTED_DEVICE_NAME), msg.getData().getString(CONNECTED_DEVICE_ADDRESS));
                    Toast.makeText(ChatHistoryActivity.this, "Connecting to " + historyContact.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(ChatHistoryActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        public ChatAdapter() {
            messages = new ArrayList<>();
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View chatView = inflater.inflate(R.layout.chat_message, parent, false);
            return new ChatViewHolder(chatView);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);

            if (message.sentByUser) {
                holder.rightChatLayout.setVisibility(LinearLayout.VISIBLE);
                holder.leftChatLayout.setVisibility(LinearLayout.GONE);
                if (message.isTextMessage()) {
                    holder.righChatImageview.setVisibility(ImageView.GONE);
                    holder.rightChatTextView.setVisibility(TextView.VISIBLE);
                    holder.rightChatTextView.setText(message.message);
                } else {
                    holder.rightChatTextView.setVisibility(TextView.GONE);
                    holder.righChatImageview.setVisibility(ImageView.VISIBLE);
                    holder.righChatImageview.setImageBitmap(message.image);
                }
            } else {
                holder.leftChatLayout.setVisibility(LinearLayout.VISIBLE);
                holder.rightChatLayout.setVisibility(LinearLayout.GONE);
                if (message.isTextMessage()) {
                    holder.leftChatImageview.setVisibility(ImageView.GONE);
                    holder.leftChatTextView.setVisibility(TextView.VISIBLE);
                    holder.leftChatTextView.setText(message.message);
                } else {
                    holder.leftChatTextView.setVisibility(TextView.GONE);
                    holder.leftChatImageview.setVisibility(ImageView.VISIBLE);
                    holder.leftChatImageview.setImageBitmap(message.image);
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void addMessages(List<ChatMessage> previousMessages) {
            messages.addAll(previousMessages);
            notifyDataSetChanged();

        }

        public static class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout leftChatLayout, rightChatLayout;
            TextView leftChatTextView, rightChatTextView;
            ImageView righChatImageview, leftChatImageview;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);

                leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
                rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
                leftChatTextView = itemView.findViewById(R.id.left_chat_textview);
                rightChatTextView = itemView.findViewById(R.id.right_chat_textview);
                righChatImageview = itemView.findViewById(R.id.right_chat_imageview);
                leftChatImageview = itemView.findViewById(R.id.left_chat_imageview);

                righChatImageview.setOnClickListener(v -> openFullScreenImage(((BitmapDrawable) righChatImageview.getDrawable()).getBitmap()));

                leftChatImageview.setOnClickListener(v -> openFullScreenImage(((BitmapDrawable) leftChatImageview.getDrawable()).getBitmap()));
            }

            private void openFullScreenImage(Bitmap bitmap) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] imageInByte = baos.toByteArray();
                Intent intent = new Intent(itemView.getContext(), FullScreenImageActivity.class);
                intent.putExtra(IMAGE_MESSAGE, imageInByte);
                itemView.getContext().startActivity(intent);
            }
        }
    }
}