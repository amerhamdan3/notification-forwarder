package com.notificationforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ForwarderService extends Service {
    private static final String CHANNEL_ID = "sms_forwarder_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Notification Forwarder", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps forwarding active");
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Forwarder Active")
                .setContentText("Forwarding SMS, calls & notifications to Telegram")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
