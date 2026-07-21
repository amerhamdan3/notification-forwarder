package com.notificationforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {
    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;
    private static String incomingNumber = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.PHONE_STATE".equals(intent.getAction())) return;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (number != null) {
                incomingNumber = number;
                String text = "\uD83D\uDCDE <b>Incoming Call</b>\n"
                        + "<b>From:</b> " + escapeHtml(number);
                TelegramSender.send(context, text);
            }
        } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)
                && lastState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            // Went from ringing to idle = missed call
            if (!incomingNumber.isEmpty()) {
                String text = "\u260E\uFE0F <b>Missed Call</b>\n"
                        + "<b>From:</b> " + escapeHtml(incomingNumber);
                TelegramSender.send(context, text);
            }
        }

        lastState = state;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
