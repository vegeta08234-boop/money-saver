package com.spendlog.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/** Daily "log today's spending" reminder. */
final class Reminder {
    static final String ACTION = "com.spendlog.app.REMIND";
    private static final String CHANNEL = "reminder";

    private Reminder() {}

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 10, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Sets or cancels the daily alarm to match the saved setting. Safe to call often. */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.cancel(alarmIntent(c));
        if (!AppSettings.reminderOn(c)) {
            return;
        }
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, AppSettings.reminderHour(c));
        t.set(Calendar.MINUTE, AppSettings.reminderMinute(c));
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) {
            t.add(Calendar.DAY_OF_MONTH, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent(c));
    }

    /** Shows the reminder unless something was already logged today. */
    static void notifyNow(Context c) {
        long today = 0;
        List<Expense> all = ExpenseStore.load(c);
        String d = Dates.today();
        for (Expense e : all) {
            if (e.date.equals(d)) {
                today += e.cents;
            }
        }
        if (today > 0) {
            return;
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Daily reminder",
                    NotificationManager.IMPORTANCE_DEFAULT));
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        Intent open = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("focus_add", true);
        PendingIntent pi = PendingIntent.getActivity(c, 11, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Log today's spending")
                .setContentText("Add anything you spent today to Spend Log.")
                .setContentIntent(pi)
                .setAutoCancel(true);
        nm.notify(1, b.build());
    }
}
