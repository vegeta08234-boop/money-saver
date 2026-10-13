package com.spendlog.app;

import android.content.Context;

final class AppSettings {
    private AppSettings() {}

    static boolean lockOn(Context c) {
        return Prefs.sp(c).getBoolean("lock", false);
    }

    static void setLock(Context c, boolean on) {
        Prefs.sp(c).edit().putBoolean("lock", on).apply();
    }

    static boolean reminderOn(Context c) {
        return Prefs.sp(c).getBoolean("rem_on", false);
    }

    static int reminderHour(Context c) {
        return Prefs.sp(c).getInt("rem_h", 20);
    }

    static int reminderMinute(Context c) {
        return Prefs.sp(c).getInt("rem_m", 0);
    }

    static void setReminder(Context c, boolean on, int hour, int minute) {
        Prefs.sp(c).edit().putBoolean("rem_on", on).putInt("rem_h", hour).putInt("rem_m", minute).apply();
    }
}
