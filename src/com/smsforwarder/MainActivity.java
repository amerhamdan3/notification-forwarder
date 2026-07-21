package com.smsforwarder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.Manifest;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 100;
    private TextView statusText;
    private EditText tokenInput;
    private EditText chatIdInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1a1a2e"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 80);

        // Title
        TextView title = new TextView(this);
        title.setText("📨 SMS, Call & Notification Forwarder");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        // Spacer
        layout.addView(spacer(40));

        // --- Telegram configuration ---
        TextView configLabel = new TextView(this);
        configLabel.setText("Telegram configuration");
        configLabel.setTextSize(15);
        configLabel.setTextColor(Color.WHITE);
        layout.addView(configLabel);

        TextView tokenLabel = new TextView(this);
        tokenLabel.setText("Bot token (from @BotFather)");
        tokenLabel.setTextSize(13);
        tokenLabel.setTextColor(Color.parseColor("#aaaaaa"));
        tokenLabel.setPadding(0, 20, 0, 6);
        layout.addView(tokenLabel);

        tokenInput = new EditText(this);
        tokenInput.setHint("123456789:ABC...");
        tokenInput.setTextColor(Color.WHITE);
        tokenInput.setHintTextColor(Color.parseColor("#666666"));
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        tokenInput.setSingleLine(true);
        layout.addView(tokenInput);

        TextView chatLabel = new TextView(this);
        chatLabel.setText("Chat ID (your numeric Telegram ID or channel)");
        chatLabel.setTextSize(13);
        chatLabel.setTextColor(Color.parseColor("#aaaaaa"));
        chatLabel.setPadding(0, 20, 0, 6);
        layout.addView(chatLabel);

        chatIdInput = new EditText(this);
        chatIdInput.setHint("123456789");
        chatIdInput.setTextColor(Color.WHITE);
        chatIdInput.setHintTextColor(Color.parseColor("#666666"));
        chatIdInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        chatIdInput.setSingleLine(true);
        layout.addView(chatIdInput);

        // Prefill with any saved values
        tokenInput.setText(TelegramSender.getBotToken(this));
        chatIdInput.setText(TelegramSender.getChatId(this));

        layout.addView(spacer(16));

        Button saveBtn = new Button(this);
        saveBtn.setText("Save Telegram Settings");
        saveBtn.setTextSize(16);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = tokenInput.getText().toString().trim();
                String chatId = chatIdInput.getText().toString().trim();
                if (token.isEmpty() || chatId.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "Enter both a bot token and a chat ID",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                TelegramSender.saveConfig(MainActivity.this, token, chatId);
                Toast.makeText(MainActivity.this, "Settings saved",
                        Toast.LENGTH_SHORT).show();
                updateStatus();
            }
        });
        layout.addView(saveBtn);

        layout.addView(spacer(40));

        // Status
        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setTextColor(Color.parseColor("#aaaaaa"));
        statusText.setPadding(0, 20, 0, 20);
        layout.addView(statusText);

        layout.addView(spacer(30));

        // Grant Permissions Button
        Button permBtn = new Button(this);
        permBtn.setText("Grant SMS & Call Permissions");
        permBtn.setTextSize(16);
        permBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_NUMBERS,
                        Manifest.permission.POST_NOTIFICATIONS
                }, PERMISSION_REQUEST);
            }
        });
        layout.addView(permBtn);

        layout.addView(spacer(20));

        // Notification Access Button
        Button notifBtn = new Button(this);
        notifBtn.setText("Enable Notification Access");
        notifBtn.setTextSize(16);
        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            }
        });
        layout.addView(notifBtn);

        layout.addView(spacer(20));

        // Start Service Button
        Button startBtn = new Button(this);
        startBtn.setText("Start Forwarder Service");
        startBtn.setTextSize(16);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this, ForwarderService.class);
                startForegroundService(serviceIntent);
                updateStatus();
            }
        });
        layout.addView(startBtn);

        layout.addView(spacer(20));

        // Test Button
        Button testBtn = new Button(this);
        testBtn.setText("Send Test Message to Telegram");
        testBtn.setTextSize(16);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TelegramSender.isConfigured(MainActivity.this)) {
                    Toast.makeText(MainActivity.this,
                            "Save your Telegram settings first",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                TelegramSender.send(MainActivity.this,
                        "✅ <b>SMS Forwarder</b> is working!\nTest message from your phone.");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Test Sent")
                        .setMessage("Check your Telegram for the test message!")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        layout.addView(testBtn);

        scroll.addView(layout);
        setContentView(scroll);

        // Auto-start forwarder service only once configured
        if (TelegramSender.isConfigured(this)) {
            Intent serviceIntent = new Intent(this, ForwarderService.class);
            startForegroundService(serviceIntent);
        }

        updateStatus();
    }

    private View spacer(int heightPx) {
        View spacer = new View(this);
        spacer.setMinimumHeight(heightPx);
        return spacer;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();

        // Check Telegram configuration
        boolean configured = TelegramSender.isConfigured(this);
        sb.append(configured ? "✅" : "❌").append(" Telegram Configured\n");

        // Check SMS permission
        boolean hasSms = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(hasSms ? "✅" : "❌").append(" SMS Permission\n");

        // Check call permission
        boolean hasCall = checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(hasCall ? "✅" : "❌").append(" Call Permission\n");

        // Check notification listener
        boolean hasNotif = isNotificationListenerEnabled();
        sb.append(hasNotif ? "✅" : "❌").append(" Notification Access\n");

        // Check internet
        boolean hasNet = checkSelfPermission(Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(hasNet ? "✅" : "❌").append(" Internet Permission\n");

        sb.append("\n📱 Forwarding to Telegram Bot");

        statusText.setText(sb.toString());
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && cn.getPackageName().equals(getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        updateStatus();
    }
}
