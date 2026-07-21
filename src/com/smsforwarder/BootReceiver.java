package com.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ForwarderService.class);
            context.startForegroundService(serviceIntent);
            TelegramSender.send(context, "🔄 Phone rebooted — SMS Forwarder restarted");
        }
    }
}
