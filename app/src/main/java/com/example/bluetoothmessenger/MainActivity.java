package com.example.bluetoothmessenger;

import static com.example.bluetoothmessenger.chat.AndroidBluetoothController.BLUETOOTH_ENABLE_FOR_PAIRED;
import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_ADDRESS;
import static com.example.bluetoothmessenger.chat.ChatUtils.CONNECTED_DEVICE_NAME;
import static com.example.bluetoothmessenger.chat.ChatUtils.DEVICE_NAME_MESSAGE;
import static com.example.bluetoothmessenger.chat.ChatUtils.MESSAGE_STATE_CHANGED;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST;
import static com.example.bluetoothmessenger.chat.ChatUtils.TOAST_MESSAGE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothmessenger.chat.AndroidBluetoothController;
import com.example.bluetoothmessenger.chat.ChatUtils;
import com.example.bluetoothmessenger.data.BluetoothContact;
import com.example.bluetoothmessenger.roomDB.ControllerDB;
import com.example.bluetoothmessenger.roomDB.MessageDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private AndroidBluetoothController bluetoothController;
    private BluetoothContact contact;
    private ContactsAdapter contactsAdapter;
    private ControllerDB controllerDB;

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
        controllerDB = ControllerDB.getInstance(getApplicationContext());
        contactsAdapter = new ContactsAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.contacts);
        recyclerView.setAdapter(contactsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        contactsAdapter.setOnItemClickListener((name, macAddress) -> {
            Intent intent = new Intent(MainActivity.this, ChatHistoryActivity.class);
            intent.putExtra(CONNECTED_DEVICE_ADDRESS, macAddress);
            intent.putExtra(CONNECTED_DEVICE_NAME, name);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        bluetoothController = AndroidBluetoothController.getInstance(this);
        showListOfContacts();
        startListening();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.search_contact);
        SearchView searchView = (SearchView) menuItem.getActionView();
        Objects.requireNonNull(searchView).setQueryHint("Type here");

        Objects.requireNonNull(searchView).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                contactsAdapter.getFilter().filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                contactsAdapter.getFilter().filter(newText);
                return true;
            }
        });
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
        contactsAdapter.addList(controllerDB.getContactsList());
    }

    public void setNoContactsTextVisibility(int visibility) {
        TextView noContactsText = findViewById(R.id.no_contacts_text);
        noContactsText.setVisibility(visibility);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ENABLE_FOR_PAIRED) {
            if (resultCode != RESULT_OK) {
                enableBluetooth();
            } else {
                startListening();
            }
        }
    }

    private final Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_STATE_CHANGED:
                switch (msg.arg1) {
                    case ChatUtils.STATE_CONNECTED:
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra(CONNECTED_DEVICE_ADDRESS, contact.getMACaddress());
                        intent.putExtra(CONNECTED_DEVICE_NAME, contact.getName());
                        startActivity(intent);
                        Toast.makeText(MainActivity.this, "Connected to " + contact.getName(), Toast.LENGTH_SHORT).show();
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        break;
                }
                break;
            case DEVICE_NAME_MESSAGE:
                contact = new BluetoothContact(msg.getData().getString(CONNECTED_DEVICE_NAME), msg.getData().getString(CONNECTED_DEVICE_ADDRESS));
                Toast.makeText(MainActivity.this, "Connecting to " + contact.getName(), Toast.LENGTH_SHORT).show();
                break;
            case TOAST_MESSAGE:
                Toast.makeText(MainActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    });

    public static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> implements Filterable {
        private final MainActivity mainActivity;
        private final List<BluetoothContact> contacts;
        private final List<BluetoothContact> contactsFull;
        private BluetoothScanActivity.DevicesAdapter.OnItemClickListener onItemListener;

        public ContactsAdapter(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
            contacts = new ArrayList<>();
            contactsFull = new ArrayList<>();
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
                BluetoothContact contactFromFull = contacts.get(position);
                contacts.remove(contactFromFull);
                contactsFull.remove(contactFromFull);
                notifyDataSetChanged();
                setNoContactsTextVisibility();
                mainActivity.controllerDB.deleteContactFromDB(device.getMACaddress());
            });

            holder.editBtn.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                LayoutInflater inflater = LayoutInflater.from(mainActivity);
                View dialogView = inflater.inflate(R.layout.edit_contact_name_dialog, null);
                builder.setView(dialogView);

                EditText editContactName = dialogView.findViewById(R.id.new_contact_name);
                editContactName.setText(device.getName());

                builder.setTitle("Edit contact name").setPositiveButton("Save", (dialog, which) -> {
                    String newName = editContactName.getText().toString();
                    if (!newName.isEmpty()) {
                        for (BluetoothContact contactFromFull : contactsFull) {
                            if (contactFromFull.getMACaddress().equals(device.getMACaddress())) {
                                contactFromFull.setName(newName);
                                break;
                            }
                        }
                        device.setName(newName);
                        notifyItemChanged(position);
                        mainActivity.controllerDB.changeContactName(device.getMACaddress(), newName);
                    }
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();
            });
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        public void setNoContactsTextVisibility() {
            if (contacts.isEmpty()) {
                mainActivity.setNoContactsTextVisibility(View.VISIBLE);
            } else {
                mainActivity.setNoContactsTextVisibility(View.GONE);
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        public void addList(List<BluetoothContact> contacts) {
            this.clear();
            this.contacts.addAll(contacts);
            this.contactsFull.addAll(contacts);
            notifyDataSetChanged();
            setNoContactsTextVisibility();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void clear() {
            contacts.clear();
            contactsFull.clear();
            notifyDataSetChanged();
            setNoContactsTextVisibility();
        }

        public void setOnItemClickListener(BluetoothScanActivity.DevicesAdapter.OnItemClickListener listener) {
            onItemListener = listener;
        }

        @Override
        public Filter getFilter() {
            return contactFilter;
        }

        private final Filter contactFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<BluetoothContact> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(contactsFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (BluetoothContact contact : contactsFull) {
                        if (contact.getName().toLowerCase().startsWith(filterPattern)) {
                            filteredList.add(contact);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                contacts.clear();
                contacts.addAll((List<BluetoothContact>) results.values);
                notifyDataSetChanged();
                setNoContactsTextVisibility();
            }
        };

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

    @SuppressWarnings("unused")
    public void setUpDB() {
        byte[] message = "Hello".getBytes();
        new Thread(() -> {
            controllerDB.insertMessage(new MessageDB("00:11:22:33:FF:EE", "ATest", true, true, message, "00:00:00"));
            controllerDB.insertMessage(new MessageDB("01:12:23:34:FF:EF", "BTest", false, true, message, "00:01:00"));
            controllerDB.insertMessage(new MessageDB("02:13:24:35:FF:F0", "CTest", true, true, message, "00:02:00"));
            controllerDB.insertMessage(new MessageDB("03:14:25:36:FF:F1", "DTest", false, true, message, "00:03:00"));
            controllerDB.insertMessage(new MessageDB("04:15:26:37:FF:F2", "ETest", true, true, message, "00:04:00"));
            controllerDB.insertMessage(new MessageDB("05:16:27:38:FF:F3", "FTest", false, true, message, "00:05:00"));
            controllerDB.insertMessage(new MessageDB("06:17:28:39:FF:F4", "GTest", true, true, message, "00:06:00"));
            controllerDB.insertMessage(new MessageDB("07:18:29:40:FF:F5", "HTest", false, true, message, "00:07:00"));
            controllerDB.insertMessage(new MessageDB("08:19:30:41:FF:F6", "ITest", true, true, message, "00:08:00"));
            controllerDB.insertMessage(new MessageDB("09:20:31:42:FF:F7", "JTest", false, true, message, "00:09:00"));
            controllerDB.insertMessage(new MessageDB("10:21:32:43:FF:F8", "KTest", true, true, message, "00:10:00"));
            controllerDB.insertMessage(new MessageDB("11:22:33:44:FF:F9", "LTest", false, true, message, "00:11:00"));
            controllerDB.insertMessage(new MessageDB("12:23:34:45:FF:FA", "MTest", true, true, message, "00:12:00"));
            controllerDB.insertMessage(new MessageDB("13:24:35:46:FF:FB", "NTest", false, true, message, "00:13:00"));
            controllerDB.insertMessage(new MessageDB("14:25:36:47:FF:FC", "OTest", true, true, message, "00:14:00"));
            controllerDB.insertMessage(new MessageDB("15:26:37:48:FF:FD", "abcTest", false, true, message, "00:15:00"));
            controllerDB.insertMessage(new MessageDB("16:27:38:49:FF:FE", "AbTest", true, true, message, "00:16:00"));
            controllerDB.insertMessage(new MessageDB("17:28:39:50:FF:FF", "abTest", false, true, message, "00:17:00"));
            controllerDB.insertMessage(new MessageDB("18:29:40:51:FF:EF", "bTest", true, true, message, "00:18:00"));
            controllerDB.insertMessage(new MessageDB("19:30:41:52:FF:F0", "aTest", false, true, message, "00:19:00"));
        }).start();
    }
}