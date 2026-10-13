package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class Categories {
    static final class Cat {
        String name;
        int color;

        Cat(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    static final int[] COLORS = {
            0xFF6366F1, 0xFF22C55E, 0xFFF97316, 0xFFF43F5E, 0xFF06B6D4,
            0xFFEAB308, 0xFFA855F7, 0xFF14B8A6, 0xFF64748B, 0xFFEC4899
    };
    private static final int FALLBACK = 0xFF94A3B8;
    private static final String KEY = "cats";

    private Categories() {}

    private static List<Cat> defaults() {
        List<Cat> l = new ArrayList<>();
        l.add(new Cat("Food", COLORS[2]));
        l.add(new Cat("Groceries", COLORS[1]));
        l.add(new Cat("Transport", COLORS[4]));
        l.add(new Cat("Bills", COLORS[8]));
        l.add(new Cat("Shopping", COLORS[9]));
        l.add(new Cat("Fun", COLORS[6]));
        l.add(new Cat("Health", COLORS[3]));
        l.add(new Cat("Other", COLORS[5]));
        return l;
    }

    static synchronized List<Cat> load(Context c) {
        List<Cat> out = new ArrayList<>();
        String raw = Prefs.sp(c).getString(KEY, null);
        if (raw != null) {
            try {
                JSONArray a = new JSONArray(raw);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    out.add(new Cat(o.getString("name"), o.getInt("color")));
                }
            } catch (JSONException ignored) {
                // fall through to defaults if nothing parsed
            }
        }
        return out.isEmpty() ? defaults() : out;
    }

    private static void save(Context c, List<Cat> l) {
        JSONArray a = new JSONArray();
        for (Cat k : l) {
            a.put(Prefs.obj("name", k.name, "color", k.color));
        }
        Prefs.sp(c).edit().putString(KEY, a.toString()).apply();
    }

    static List<String> names(Context c) {
        List<String> n = new ArrayList<>();
        for (Cat k : load(c)) {
            n.add(k.name);
        }
        return n;
    }

    static int colorOf(List<Cat> list, String name) {
        for (Cat k : list) {
            if (k.name.equals(name)) {
                return k.color;
            }
        }
        return FALLBACK;
    }

    static boolean exists(List<Cat> list, String name) {
        for (Cat k : list) {
            if (k.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    static synchronized void add(Context c, String name, int color) {
        List<Cat> l = load(c);
        l.add(new Cat(name, color));
        save(c, l);
        SpendWidget.updateAll(c);
    }

    static synchronized void update(Context c, String oldName, String newName, int color) {
        List<Cat> l = load(c);
        for (Cat k : l) {
            if (k.name.equals(oldName)) {
                k.name = newName;
                k.color = color;
            }
        }
        save(c, l);
        if (!oldName.equals(newName)) {
            ExpenseStore.renameCategory(c, oldName, newName);
            Recurring.renameCategory(c, oldName, newName);
            Budgets.rename(c, oldName, newName);
        }
        SpendWidget.updateAll(c);
    }

    /** Removes a category; its entries move to "Other" (or the first remaining category). */
    static synchronized boolean delete(Context c, String name) {
        List<Cat> l = load(c);
        if (l.size() <= 1) {
            return false;
        }
        List<Cat> rest = new ArrayList<>();
        for (Cat k : l) {
            if (!k.name.equals(name)) {
                rest.add(k);
            }
        }
        String target = rest.get(0).name;
        for (Cat k : rest) {
            if (k.name.equals("Other")) {
                target = k.name;
            }
        }
        save(c, rest);
        ExpenseStore.renameCategory(c, name, target);
        Recurring.renameCategory(c, name, target);
        Budgets.remove(c, name);
        SpendWidget.updateAll(c);
        return true;
    }
}
