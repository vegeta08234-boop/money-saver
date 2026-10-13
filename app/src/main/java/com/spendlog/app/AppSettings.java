package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;

import java.util.LinkedHashSet;
import java.util.Set;

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

    /* ---------- color shift as the budget fills ---------- */

    static boolean tintOn(Context c) {
        return Prefs.sp(c).getBoolean("tint", true);
    }

    static void setTint(Context c, boolean on) {
        Prefs.sp(c).edit().putBoolean("tint", on).apply();
    }

    /* ---------- auto-add payments ---------- */

    static boolean autoOn(Context c) {
        return Prefs.sp(c).getBoolean("auto_on", false);
    }

    static void setAuto(Context c, boolean on) {
        Prefs.sp(c).edit().putBoolean("auto_on", on).apply();
    }

    static Set<String> watched(Context c) {
        Set<String> out = new LinkedHashSet<>();
        JSONArray a = Prefs.arr(c, "watch_pkgs");
        for (int i = 0; i < a.length(); i++) {
            String s = a.optString(i, "");
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    static void setWatched(Context c, Set<String> pkgs) {
        JSONArray a = new JSONArray();
        for (String p : pkgs) {
            a.put(p);
        }
        Prefs.putArr(c, "watch_pkgs", a);
    }

    static String autoCat(Context c) {
        return Prefs.sp(c).getString("auto_cat", "Other");
    }

    static void setAutoCat(Context c, String name) {
        Prefs.sp(c).edit().putString("auto_cat", name).apply();
    }

    static boolean confirmOn(Context c) {
        return Prefs.sp(c).getBoolean("auto_confirm", true);
    }

    static void setConfirm(Context c, boolean on) {
        Prefs.sp(c).edit().putBoolean("auto_confirm", on).apply();
    }
}
