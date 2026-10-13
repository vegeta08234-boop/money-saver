package com.spendlog.app;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

final class Budgets {
    private Budgets() {}

    static long monthly(Context c) {
        return Prefs.sp(c).getLong("budget", 0);
    }

    static void setMonthly(Context c, long cents) {
        Prefs.sp(c).edit().putLong("budget", Math.max(0, cents)).apply();
        SpendWidget.updateAll(c);
    }

    private static JSONObject catMap(Context c) {
        try {
            return new JSONObject(Prefs.sp(c).getString("cat_budgets", "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void saveMap(Context c, JSONObject o) {
        Prefs.sp(c).edit().putString("cat_budgets", o.toString()).apply();
    }

    static long forCat(Context c, String name) {
        return catMap(c).optLong(name, 0);
    }

    static synchronized void setCat(Context c, String name, long cents) {
        JSONObject o = catMap(c);
        try {
            if (cents <= 0) {
                o.remove(name);
            } else {
                o.put(name, cents);
            }
        } catch (JSONException ignored) {
            return;
        }
        saveMap(c, o);
    }

    static synchronized void rename(Context c, String from, String to) {
        JSONObject o = catMap(c);
        if (o.has(from)) {
            long v = o.optLong(from, 0);
            o.remove(from);
            try {
                o.put(to, v);
            } catch (JSONException ignored) {
                return;
            }
            saveMap(c, o);
        }
    }

    static synchronized void remove(Context c, String name) {
        JSONObject o = catMap(c);
        o.remove(name);
        saveMap(c, o);
    }
}
