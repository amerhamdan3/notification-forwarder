package com.notificationforwarder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 100;

    // Palette
    private static final int BG          = 0xFF0F0F1E;
    private static final int CARD        = 0xFF1C1C33;
    private static final int CARD_INPUT  = 0xFF262645;
    private static final int ACCENT      = 0xFF6C5CE7;
    private static final int ACCENT_DK   = 0xFF4B3FC4;
    private static final int TEXT        = 0xFFF5F5FA;
    private static final int TEXT_MUTED  = 0xFF9A9AB5;
    private static final int OK_GREEN    = 0xFF2ECC71;
    private static final int WARN_RED    = 0xFFFF6B6B;

    private TextView statusText;
    private EditText tokenInput;
    private EditText chatIdInput;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(36), dp(20), dp(36));

        // ---------------- Header ----------------
        TextView title = new TextView(this);
        title.setText("Notification Forwarder");
        title.setTextSize(26);
        title.setTextColor(TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Forward SMS, calls & notifications to your Telegram");
        subtitle.setTextSize(14);
        subtitle.setTextColor(TEXT_MUTED);
        subtitle.setPadding(0, dp(6), 0, 0);
        root.addView(subtitle);

        root.addView(gap(24));

        // ---------------- Config card ----------------
        LinearLayout configCard = card();
        configCard.addView(cardHeading("⚙️  Telegram Setup"));

        configCard.addView(fieldLabel("Bot token (from @BotFather)"));
        tokenInput = input("123456789:ABC...");
        configCard.addView(tokenInput);

        configCard.addView(fieldLabel("Chat ID"));
        chatIdInput = input("Your numeric Telegram ID");
        configCard.addView(chatIdInput);

        tokenInput.setText(TelegramSender.getBotToken(this));
        chatIdInput.setText(TelegramSender.getChatId(this));

        configCard.addView(gap(16));
        Button saveBtn = primaryButton("Save Settings");
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onSave(); }
        });
        configCard.addView(saveBtn);
        root.addView(configCard);

        root.addView(gap(16));

        // ---------------- Status card ----------------
        LinearLayout statusCard = card();
        statusCard.addView(cardHeading("📊  Status"));
        statusText = new TextView(this);
        statusText.setTextSize(15);
        statusText.setTextColor(TEXT);
        statusText.setLineSpacing(dp(6), 1f);
        statusText.setPadding(0, dp(8), 0, 0);
        statusCard.addView(statusText);
        root.addView(statusCard);

        root.addView(gap(16));

        // ---------------- Actions card ----------------
        LinearLayout actionsCard = card();
        actionsCard.addView(cardHeading("🔐  Setup Steps"));

        actionsCard.addView(gap(4));
        Button permBtn = secondaryButton("1 · Grant SMS & Call Permissions");
        permBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
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
        actionsCard.addView(permBtn);

        actionsCard.addView(gap(10));
        Button notifBtn = secondaryButton("2 · Enable Notification Access");
        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        actionsCard.addView(notifBtn);

        actionsCard.addView(gap(10));
        Button startBtn = secondaryButton("3 · Start Forwarder Service");
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startForegroundService(new Intent(MainActivity.this, ForwarderService.class));
                updateStatus();
            }
        });
        actionsCard.addView(startBtn);
        root.addView(actionsCard);

        root.addView(gap(16));

        // ---------------- Test button ----------------
        Button testBtn = primaryButton("✈  Send Test Message");
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onTest(); }
        });
        root.addView(testBtn);

        scroll.addView(root);
        setContentView(scroll);

        if (TelegramSender.isConfigured(this)) {
            startForegroundService(new Intent(this, ForwarderService.class));
        }
        updateStatus();
    }

    // ---------------- Actions ----------------

    private void onSave() {
        String token = tokenInput.getText().toString().trim();
        String chatId = chatIdInput.getText().toString().trim();
        if (token.isEmpty() || chatId.isEmpty()) {
            toast("Enter both a bot token and a chat ID");
            return;
        }
        TelegramSender.saveConfig(this, token, chatId);
        toast("Settings saved");
        updateStatus();
    }

    private void onTest() {
        if (!TelegramSender.isConfigured(this)) {
            toast("Save your Telegram settings first");
            return;
        }
        TelegramSender.send(this,
                "✅ <b>Notification Forwarder</b> is working!\nTest message from your phone.");
        new AlertDialog.Builder(this)
                .setTitle("Test Sent")
                .setMessage("Check your Telegram for the test message!")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        statusText.setText(TextUtils.concat(
                statusRow("Telegram configured", TelegramSender.isConfigured(this)),
                statusRow("SMS permission",
                        granted(Manifest.permission.RECEIVE_SMS)),
                statusRow("Call permission",
                        granted(Manifest.permission.READ_PHONE_STATE)),
                statusRow("Notification access", isNotificationListenerEnabled()),
                statusRow("Internet permission",
                        granted(Manifest.permission.INTERNET))
        ));
    }

    private boolean granted(String perm) {
        return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    private CharSequence statusRow(String label, boolean ok) {
        android.text.SpannableString s =
                new android.text.SpannableString((ok ? "● " : "● ") + label + "\n");
        s.setSpan(new android.text.style.ForegroundColorSpan(ok ? OK_GREEN : WARN_RED),
                0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            for (String name : flat.split(":")) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && cn.getPackageName().equals(getPackageName())) return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        updateStatus();
    }

    // ---------------- UI builders ----------------

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(18));
        c.setBackground(bg);
        return c;
    }

    private TextView cardHeading(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, dp(4));
        return t;
    }

    private TextView fieldLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(TEXT_MUTED);
        t.setPadding(0, dp(14), 0, dp(6));
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(0xFF6A6A85);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_INPUT);
        bg.setCornerRadius(dp(12));
        e.setBackground(bg);
        e.setLayoutParams(matchWidth());
        return e;
    }

    private Button primaryButton(String text) {
        return styledButton(text, ACCENT, ACCENT_DK, Color.WHITE);
    }

    private Button secondaryButton(String text) {
        return styledButton(text, CARD_INPUT, 0xFF33335A, TEXT);
    }

    private Button styledButton(String text, int fill, int pressed, int textColor) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(fill);
        normal.setCornerRadius(dp(14));
        GradientDrawable down = new GradientDrawable();
        down.setColor(pressed);
        down.setCornerRadius(dp(14));

        android.graphics.drawable.StateListDrawable sld =
                new android.graphics.drawable.StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, down);
        sld.addState(new int[]{}, normal);
        b.setBackground(sld);

        LinearLayout.LayoutParams lp = matchWidth();
        lp.height = dp(52);
        b.setLayoutParams(lp);
        return b;
    }

    private View gap(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return v;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
