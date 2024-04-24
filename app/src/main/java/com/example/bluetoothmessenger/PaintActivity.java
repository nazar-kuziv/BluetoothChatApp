package com.example.bluetoothmessenger;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bluetoothmessenger.chat.AndroidBluetoothController;
import com.kyanogen.signatureview.SignatureView;

import java.io.ByteArrayOutputStream;

import yuku.ambilwarna.AmbilWarnaDialog;

public class PaintActivity extends AppCompatActivity {
    SignatureView signatureView;
    ImageButton eraseButton, colorButton, sendButton;
    SeekBar brushSizeBar;
    TextView brushSizeText;
    int defaultColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_paint);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        signatureView = findViewById(R.id.signature_view);
        eraseButton = findViewById(R.id.eraser_btn);
        colorButton = findViewById(R.id.color_btn);
        sendButton = findViewById(R.id.send_btn);
        brushSizeBar = findViewById(R.id.brush_size_bar);
        brushSizeText = findViewById(R.id.brush_size_text);
        defaultColor = ContextCompat.getColor(this, R.color.black);
        brushSizeBar.setProgress(5);
        brushSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress< 1)
                    progress = 1;
                signatureView.setPenSize(progress);
                brushSizeText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        eraseButton.setOnClickListener(v -> signatureView.clearCanvas());

        colorButton.setOnClickListener(v -> openColorPicker());

        sendButton.setOnClickListener(v -> {
            if(!signatureView.isBitmapEmpty()){
                Bitmap imgBitmap = signatureView.getSignatureBitmap();
                byte[] imgBytes = convertBitmapToByteArrayCompressed(imgBitmap);
                AndroidBluetoothController.chatUtils.sendImage(imgBytes);
                onBackPressed();
                Log.e("Image", "We are sending an image" + imgBytes.length);
            }else{
                Toast.makeText(this, "Please draw something", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        setTitle("Draw your picture");
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

    private byte[] convertBitmapToByteArrayCompressed(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 40, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        return byteArray;
    }

    private void openColorPicker() {
        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                defaultColor = color;
                signatureView.setPenColor(color);
            }
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }
        });
        ambilWarnaDialog.show();
    }

}