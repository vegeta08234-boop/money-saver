package com.spendlog.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the "Undo" button on the "Added $x" notification. */
public class UndoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent == null ? 0L : intent.getLongExtra("id", 0L);
        if (id == 0L) {
            return;
        }
        ExpenseStore.remove(context, id);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel((int) (id & 0x7fffffff));
        }
    }
}
