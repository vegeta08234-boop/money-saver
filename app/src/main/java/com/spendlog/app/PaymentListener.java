package com.spendlog.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Calendar;

/**
 * Reads payment notifications from the apps the user chose and adds the purchase as an expense.
 * It only looks at notifications from those apps, and nothing leaves the phone.
 */
public class PaymentListener extends NotificationListenerService {

    static final String[] WALLETS = {
            "com.google.android.apps.walletnfcrel",      // Google Wallet
            "com.google.android.apps.nbu.paisa.user",    // Google Pay (India)
            "com.samsung.android.spay"                   // Samsung Wallet
    };

    private static final String CHANNEL = "auto_added";

    static boolean isWallet(String pkg) {
        for (String w : WALLETS) {
            if (w.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasAccess(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName());
    }

    static void rebindIfNeeded(Context c) {
        if (AppSettings.autoOn(c) && hasAccess(c)) {
            try {
                NotificationListenerService.requestRebind(new ComponentName(c, PaymentListener.class));
            } catch (RuntimeException ignored) {
                // The system will bind the service on its own when it can.
            }
        }
    }

    static String appLabel(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            handle(getApplicationContext(), sbn);
        } catch (Exception ignored) {
            // A bad notification must never crash the listener.
        }
    }

    private static String str(CharSequence s) {
        return s == null ? "" : s.toString();
    }

    private static void handle(Context c, StatusBarNotification sbn) {
        if (sbn == null || !AppSettings.autoOn(c)) {
            return;
        }
        String pkg = sbn.getPackageName();
        if (pkg == null || pkg.equals(c.getPackageName()) || !AppSettings.watched(c).contains(pkg)) {
            return;
        }
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            return;
        }
        Bundle x = n.extras;
        if (x == null) {
            return;
        }
        String title = str(x.getCharSequence(Notification.EXTRA_TITLE));
        String body = str(x.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (body.isEmpty()) {
            body = str(x.getCharSequence(Notification.EXTRA_TEXT));
        }
        String app = appLabel(c, pkg);
        String snippet = (title + " | " + body).replace('\n', ' ');
        if (snippet.length() > 100) {
            snippet = snippet.substring(0, 100);
        }

        PaymentParser.Result r = PaymentParser.parse(title, body, isWallet(pkg));
        if (r.skip != null) {
            AutoLog.log(c, app, "Skipped: " + r.skip, snippet);
            return;
        }
        long now = System.currentTimeMillis();
        if (AutoLog.duplicate(c, r.cents, now)) {
            AutoLog.log(c, app, "Skipped: same amount was just added", snippet);
            return;
        }

        String cat = PaymentParser.guessCategory(Categories.names(c), r.merchant, AppSettings.autoCat(c));
        Calendar when = Calendar.getInstance();
        when.setTimeInMillis(sbn.getPostTime() > 0 ? sbn.getPostTime() : now);
        long id = now;
        ExpenseStore.add(c, new Expense(id, r.cents, cat, Dates.key(when), r.merchant));
        AutoLog.markAuto(c, id);
        AutoLog.log(c, app, "Added " + Money.fmt(r.cents) + " · " + cat, snippet);
        if (AppSettings.confirmOn(c)) {
            confirm(c, id, r.cents, cat, r.merchant);
        }
    }

    private static void confirm(Context c, long id, long cents, String cat, String merchant) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Payments added",
                    NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        int nid = (int) (id & 0x7fffffff);
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

        Intent open = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent undo = new Intent(c, UndoReceiver.class).putExtra("id", id);

        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Added " + Money.fmt(cents) + " · " + cat)
                .setContentText(merchant.isEmpty() ? "Tap to open Spend Log" : merchant + " · tap to open Spend Log")
                .setContentIntent(PendingIntent.getActivity(c, nid, open, flags))
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(c, R.drawable.ic_stat), "Undo",
                        PendingIntent.getBroadcast(c, nid, undo, flags)).build());
        nm.notify(nid, b.build());
    }
}
