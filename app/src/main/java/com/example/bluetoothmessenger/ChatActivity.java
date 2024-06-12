package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_ADDRESS;
import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_NAME;
import static com.example.bluetoothmessenger.chat.ChatUtils.IMAGE_READ;
import static com.example.bluetoothmessenger.chat.ChatUtils.IMAGE_WRITE;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_READ;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_STATE_CHANGED;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_WRITE;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST_MESSAGE;
import static com.example.bluetoothmessenger.data.ChatMessage.IMAGE_MESSAGE;
import static com.example.bluetoothmessenger.data.ChatMessage.TEXT_MESSAGE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.ImageView;
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
import com.example.bluetoothmessenger.roomDB.ControllerDB;
import com.example.bluetoothmessenger.roomDB.MessageDB;
import com.github.dhaval2404.imagepicker.ImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    private final ControllerDB controllerDB = ControllerDB.getInstance();
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
        contact = new BluetoothContact(getIntent().getStringExtra(CONNECTED_DEVICE_NAME), getIntent().getStringExtra(CONNECTED_DEVICE_ADDRESS));
        AndroidBluetoothController.chatUtils.setHandler(handler);
        editText = findViewById(R.id.message_input);
        cameraButton = findViewById(R.id.camera_btn);
        sendButton = findViewById(R.id.send_btn);
        paintButton = findViewById(R.id.paint_btn);

        setChatAdapter();
        setPreviousMessagesIfExist();

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
            if (!message.isEmpty()) {
                AndroidBluetoothController.chatUtils.sendText(message);
                editText.setText("");
            }
        });

        paintButton.setOnClickListener(v -> startActivity(new Intent(this, PaintActivity.class)));

        cameraButton.setOnClickListener(v -> ImagePicker.with(this).crop().compress(1024).maxResultSize(1080, 1080).start());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
                    chatAdapter.addMessage(buffer, false, TEXT_MESSAGE);
                    break;
                case IMAGE_READ:
                    byte[] readImgBuffer = (byte[]) msg.obj;
                    chatAdapter.addMessage(readImgBuffer, false, IMAGE_MESSAGE);
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    chatAdapter.addMessage(writeBuf, true, TEXT_MESSAGE);
                    break;
                case IMAGE_WRITE:
                    byte[] writeImgBuffer = (byte[]) msg.obj;
                    chatAdapter.addMessage(writeImgBuffer, true, IMAGE_MESSAGE);
                    break;
                case TOAST_MESSAGE:
                    Toast.makeText(ChatActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setChatAdapter() {
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

            @Override
            public void onChanged() {
                chatRecyclerView.postDelayed(() -> chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1), 100);
            }
        });
    }

    private void setPreviousMessagesIfExist() {
        List<MessageDB> messagesFromDB = controllerDB.getMessagesFromUser(contact.getMACaddress());
        if (messagesFromDB != null) {
            List<ChatMessage> previousMessages = ChatMessage.convertFromMessageDB(messagesFromDB);
            chatAdapter.addPreviousMessages(previousMessages);
        }
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
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                byte[] buffer = uriToByteArray(uri.getPath());
                AndroidBluetoothController.chatUtils.sendImage(buffer);
            } else {
                Toast.makeText(this, "Error when loading image", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
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
        public void addMessage(byte[] message, boolean wroteByUser, String type) {
            messages.add(new ChatMessage(message, wroteByUser, type));
            notifyItemInserted(messages.size() - 1);
        }

        @SuppressLint("NotifyDataSetChanged")
        public void addPreviousMessages(List<ChatMessage> previousMessages) {
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
                FullScreenImageActivity.setImg(bitmap);
                Intent intent = new Intent(itemView.getContext(), FullScreenImageActivity.class);
                itemView.getContext().startActivity(intent);
            }
        }
    }
}