package com.example.bluetoothmessenger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.bluetoothmessenger.data.BluetoothContact;
import com.example.bluetoothmessenger.roomDB.AppDatabase;
import com.example.bluetoothmessenger.roomDB.MessageDAO;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ContactsAdapter contactsAdapter;
    private MessageDAO messageDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        messageDAO = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "bluetooth-messenger-db").build().messageDAO();
        contactsAdapter = new ContactsAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.contacts);
        recyclerView.setAdapter(contactsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        showListOfContacts();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        setTitle("Find a device");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.scan) {
            Intent intent = new Intent(this, BluetoothScanActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void showListOfContacts() {
        new Thread(() -> {
            List<BluetoothContact> contacts = messageDAO.getUniqueInterlocutors();
            runOnUiThread(() -> contactsAdapter.addList(contacts));
        }).start();
    }

    public void deleteContactFromDB(String macAddress) {
        new Thread(() -> messageDAO.deleteAllMessagesFromUser(macAddress)).start();
    }

    public void changeContactName(String macAddress, String newName) {
        new Thread(() -> messageDAO.changeUserName(macAddress, newName)).start();
    }

    public static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
        private final MainActivity mainActivity;
        private final List<BluetoothContact> contacts;
        private BluetoothScanActivity.DevicesAdapter.OnItemClickListener onItemListener;

        public ContactsAdapter(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
            contacts = new ArrayList<>();
        }

        @NonNull
        @Override
        public MainActivity.ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.bluetooth_contact, parent, false);
            return new MainActivity.ContactsAdapter.ViewHolder(contactView);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothContact device = contacts.get(position);
            holder.name.setText(device.getName());
            holder.MACaddress.setText(device.getMACaddress());

            holder.itemView.setOnClickListener(v -> {
                if (onItemListener != null) {
                    onItemListener.onItemClick(device.getName(), device.getMACaddress());
                }
            });

            holder.deleteBtn.setOnClickListener(v -> {
                contacts.remove(position);
                notifyDataSetChanged();
                mainActivity.deleteContactFromDB(device.getMACaddress());
            });

            holder.editBtn.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                LayoutInflater inflater = LayoutInflater.from(mainActivity);
                View dialogView = inflater.inflate(R.layout.edit_contact_name_dialog, null);
                builder.setView(dialogView);

                EditText editContactName = dialogView.findViewById(R.id.new_contact_name);
                editContactName.setText(device.getName());

                builder.setTitle("Edit contact name")
                        .setPositiveButton("Save", (dialog, which) -> {
                            String newName = editContactName.getText().toString();
                            if (!newName.isEmpty()) {
                                device.setName(newName);
                                notifyItemChanged(position);
                                mainActivity.changeContactName(device.getMACaddress(), newName);
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();
            });
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void add(String name, String MACaddress) {
            contacts.add(new BluetoothContact(name, MACaddress));
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void addList(List<BluetoothContact> contacts) {
            this.clear();
            this.contacts.addAll(contacts);
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void clear() {
            contacts.clear();
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(BluetoothScanActivity.DevicesAdapter.OnItemClickListener listener) {
            onItemListener = listener;
        }

        public interface OnItemClickListener {
            void onItemClick(String name, String macAddress);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);

        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public TextView MACaddress;
            public ImageButton editBtn;
            public ImageButton deleteBtn;

            public ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                MACaddress = itemView.findViewById(R.id.mac_address);
                editBtn = itemView.findViewById(R.id.edit_btn);
                deleteBtn = itemView.findViewById(R.id.delete_btn);
            }

        }
    }
}