package com.notificationforwarder;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TelegramSender {
    public static final String PREFS_NAME = "sms_forwarder_prefs";
    public static final String KEY_BOT_TOKEN = "bot_token";
    public static final String KEY_CHAT_ID = "chat_id";

    /** Returns the saved bot token, or "" if not configured. */
    public static String getBotToken(Context context) {
        return prefs(context).getString(KEY_BOT_TOKEN, "");
    }

    /** Returns the saved chat id, or "" if not configured. */
    public static String getChatId(Context context) {
        return prefs(context).getString(KEY_CHAT_ID, "");
    }

    /** True once the user has entered both a bot token and a chat id. */
    public static boolean isConfigured(Context context) {
        return !getBotToken(context).isEmpty() && !getChatId(context).isEmpty();
    }

    public static void saveConfig(Context context, String botToken, String chatId) {
        prefs(context).edit()
                .putString(KEY_BOT_TOKEN, botToken.trim())
                .putString(KEY_CHAT_ID, chatId.trim())
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Sends a message to the Telegram chat configured by the user.
     * No-op if the app has not been configured yet.
     */
    public static void send(Context context, final String message) {
        final String botToken = getBotToken(context);
        final String chatId = getChatId(context);
        if (botToken.isEmpty() || chatId.isEmpty()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                    String params = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                            + "&text=" + URLEncoder.encode(message, "UTF-8")
                            + "&parse_mode=HTML";

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(params.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
