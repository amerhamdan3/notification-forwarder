package com.smsforwarder;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // Auto-start ForwarderService when notification listener connects
        try {
            Intent serviceIntent = new Intent(this, ForwarderService.class);
            startForegroundService(serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Dedup: track recently sent messages to prevent loops
    private static final LinkedHashMap<String, Long> recentMessages = new LinkedHashMap<String, Long>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 50;
        }
    };
    private static final long DEDUP_WINDOW_MS = 30000; // 30 seconds

    // Skip our own notifications and system noise
    private static final Set<String> SKIP_PACKAGES = new HashSet<>();
    static {
        SKIP_PACKAGES.add("com.smsforwarder");
        SKIP_PACKAGES.add("android");
        SKIP_PACKAGES.add("com.android.systemui");
        SKIP_PACKAGES.add("com.android.providers.downloads");
        SKIP_PACKAGES.add("org.telegram.messenger");
        SKIP_PACKAGES.add("org.telegram.messenger.web");
        SKIP_PACKAGES.add("org.thunderdog.challegram");
        // Note: com.android.messaging and com.google.android.apps.messaging
        // are NOT skipped so RCS messages get forwarded
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            if (SKIP_PACKAGES.contains(pkg)) return;

            // Skip ongoing/silent notifications
            Notification notification = sbn.getNotification();
            if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;
            if (notification.extras == null) return;

            CharSequence titleCs = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = notification.extras.getCharSequence(Notification.EXTRA_TEXT);

            String title = titleCs != null ? titleCs.toString() : "";
            String text = textCs != null ? textCs.toString() : "";

            // Skip empty notifications
            if (title.isEmpty() && text.isEmpty()) return;

            // Dedup: skip if we sent the same notification recently
            String dedupKey = pkg + "|" + title + "|" + text;
            long now = System.currentTimeMillis();
            synchronized (recentMessages) {
                Long lastSent = recentMessages.get(dedupKey);
                if (lastSent != null && (now - lastSent) < DEDUP_WINDOW_MS) return;
                recentMessages.put(dedupKey, now);
            }

            // Get app name
            String appName = getAppName(pkg);

            String message = "\uD83D\uDD14 <b>Notification</b> — " + escapeHtml(appName) + "\n";
            if (!title.isEmpty()) message += "<b>" + escapeHtml(title) + "</b>\n";
            if (!text.isEmpty()) message += escapeHtml(text);

            TelegramSender.send(this, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getAppName(String packageName) {
        try {
            return getPackageManager()
                    .getApplicationLabel(
                            getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
