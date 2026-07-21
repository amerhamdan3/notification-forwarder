package com.notificationforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String format = bundle.getString("format");

        StringBuilder fullMessage = new StringBuilder();
        String sender = "";

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            sender = sms.getDisplayOriginatingAddress();
            fullMessage.append(sms.getMessageBody());
        }

        String text = "\uD83D\uDCE9 <b>New SMS</b>\n"
                + "<b>From:</b> " + escapeHtml(sender) + "\n"
                + "<b>Message:</b>\n" + escapeHtml(fullMessage.toString());

        TelegramSender.send(context, text);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
