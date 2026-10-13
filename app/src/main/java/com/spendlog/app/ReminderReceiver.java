package com.spendlog.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Reminder.schedule(context);
        } else if (Reminder.ACTION.equals(action)) {
            Reminder.notifyNow(context);
        }
    }
}
