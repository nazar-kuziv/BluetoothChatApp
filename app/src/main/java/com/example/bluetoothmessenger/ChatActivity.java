package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_READ;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_STATE_CHANGED;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_WRITE;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST_MESSAGE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.github.dhaval2404.imagepicker.ImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    private BluetoothContact contact;
    private ImageButton cameraButton;
    private ImageButton sendButton;
    private ImageButton paintButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText editText;

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
//        contact = new BluetoothContact(getIntent().getStringExtra(CONNECTED_DEVICE_NAME), getIntent().getStringExtra(CONNECTED_DEVICE_ADDRESS));
//        AndroidBluetoothController.chatUtils.setHandler(handler);
        //Delete this 2 lines:
        contact = new BluetoothContact("Test", "Test");
        AndroidBluetoothController.chatUtils = new ChatUtils(handler);
        //
        editText = findViewById(R.id.message_input);
        cameraButton = findViewById(R.id.camera_btn);
        sendButton = findViewById(R.id.send_btn);
        paintButton = findViewById(R.id.paint_btn);

        setChatAdapter();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (text.isEmpty()) {
                    cameraButton.setVisibility(ImageButton.VISIBLE);
                    paintButton.setVisibility(ImageButton.VISIBLE);
                    sendButton.setVisibility(ImageButton.GONE);
                } else {
                    cameraButton.setVisibility(ImageButton.GONE);
                    paintButton.setVisibility(ImageButton.GONE);
                    sendButton.setVisibility(ImageButton.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sendButton.setOnClickListener(v -> {
            String message = editText.getText().toString();
            if(!message.isEmpty()){
                AndroidBluetoothController.chatUtils.write(message.getBytes());
                editText.setText("");
            }
        });

        paintButton.setOnClickListener(v -> startActivity(new Intent(this, PaintActivity.class)));

        cameraButton.setOnClickListener(v -> ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start());
    }

    @Override
    protected void onPause() {
        super.onPause();
        AndroidBluetoothController.chatUtils.connectionLost();
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1) {
                        case ChatUtils.STATE_LISTEN:
                        case ChatUtils.STATE_NONE:
                            setState("Not connected");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) msg.obj;
                    String inputBuffer = new String(buffer, 0, msg.arg1);
                    chatAdapter.addMessage(inputBuffer, false);
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    chatAdapter.addMessage(new String(writeBuf), true);
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(ChatActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setChatAdapter(){
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatAdapter = new ChatAdapter();
        LinearLayoutManager manager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(manager);
        chatRecyclerView.setAdapter(chatAdapter);

        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                chatRecyclerView.postDelayed(() -> {
                    int lastVisibleItemPosition = manager.findLastVisibleItemPosition();
                    if (lastVisibleItemPosition == -1 || positionStart >= lastVisibleItemPosition) {
                        manager.scrollToPositionWithOffset(positionStart, 0);
                    }
                }, 100);
            }
        });
    }


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

    private void setState(CharSequence state) {
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            Uri uri = data.getData();
            Log.e("URI", uriToByteArray(uri.getPath()).toString());
        }else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, "Error when loading image", Toast.LENGTH_LONG).show();
        }
    }

    private byte[] uriToByteArray(String uri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(uri)) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
        return baos.toByteArray();
    }

    public static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
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
            if(message.wroteByUser) {
                holder.rightChatLayout.setVisibility(LinearLayout.VISIBLE);
                holder.leftChatLayout.setVisibility(LinearLayout.GONE);
                holder.rightChatTextView.setText(message.message);
            } else {
                holder.leftChatLayout.setVisibility(LinearLayout.VISIBLE);
                holder.rightChatLayout.setVisibility(LinearLayout.GONE);
                holder.leftChatTextView.setText(message.message);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void addMessage(String message, boolean wroteByUser) {
            messages.add(new ChatMessage(message, wroteByUser));
            notifyItemInserted(messages.size() - 1);
        }

        public static class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout leftChatLayout, rightChatLayout;
            TextView leftChatTextView, rightChatTextView;
            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);

                leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
                rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
                leftChatTextView = itemView.findViewById(R.id.left_chat_textview);
                rightChatTextView = itemView.findViewById(R.id.right_chat_textview);
            }
        }
    }
}