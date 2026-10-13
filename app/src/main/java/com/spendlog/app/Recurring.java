package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class Recurring {
    static final class Item {
        long id;
        long cents;
        String cat;
        String note;
        int day;
        String last; // yyyy-MM of the last month already added

        Item(long id, long cents, String cat, String note, int day, String last) {
            this.id = id;
            this.cents = cents;
            this.cat = cat;
            this.note = note == null ? "" : note;
            this.day = day;
            this.last = last;
        }
    }

    private static final String KEY = "recurring";

    private Recurring() {}

    static synchronized List<Item> load(Context c) {
        List<Item> out = new ArrayList<>();
        JSONArray a = Prefs.arr(c, KEY);
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                out.add(new Item(o.getLong("id"), o.getLong("cents"), o.getString("cat"),
                        o.optString("note", ""), o.getInt("day"), o.getString("last")));
            } catch (JSONException ignored) {
                // skip a bad row
            }
        }
        return out;
    }

    private static void save(Context c, List<Item> list) {
        JSONArray a = new JSONArray();
        for (Item it : list) {
            a.put(Prefs.obj("id", it.id, "cents", it.cents, "cat", it.cat, "note", it.note,
                    "day", it.day, "last", it.last));
        }
        Prefs.putArr(c, KEY, a);
    }

    /** First automatic entry is the next occurrence of the day that has not passed yet. */
    static String initialLast(int day) {
        Calendar now = Calendar.getInstance();
        String cur = Dates.ym(now);
        return day <= now.get(Calendar.DAY_OF_MONTH) ? cur : Dates.shiftYm(cur, -1);
    }

    static synchronized void upsert(Context c, Item item) {
        List<Item> list = load(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == item.id) {
                list.set(i, item);
                found = true;
            }
        }
        if (!found) {
            list.add(item);
        }
        save(c, list);
    }

    static synchronized void delete(Context c, long id) {
        List<Item> keep = new ArrayList<>();
        for (Item it : load(c)) {
            if (it.id != id) {
                keep.add(it);
            }
        }
        save(c, keep);
    }

    static synchronized void renameCategory(Context c, String from, String to) {
        List<Item> list = load(c);
        for (Item it : list) {
            if (it.cat.equals(from)) {
                it.cat = to;
            }
        }
        save(c, list);
    }

    private static String dateFor(String ym, int day) {
        int dim = Dates.daysInMonth(Dates.parse(ym + "-01"));
        return ym + "-" + String.format(Locale.US, "%02d", Math.min(day, dim));
    }

    /** The date of the next automatic entry, for display. */
    static String nextDate(Item it) {
        return dateFor(Dates.shiftYm(it.last, 1), it.day);
    }

    /** Adds every entry that has come due since the last run. Safe to call often. */
    static synchronized void process(Context c) {
        List<Item> items = load(c);
        if (items.isEmpty()) {
            return;
        }
        String curYm = Dates.ym(Calendar.getInstance());
        String today = Dates.today();
        long base = System.currentTimeMillis();
        List<Expense> toAdd = new ArrayList<>();
        boolean changed = false;

        for (Item it : items) {
            String m = Dates.shiftYm(it.last, 1);
            int guard = 0;
            while (m.compareTo(curYm) <= 0 && guard++ < 240) {
                String date = dateFor(m, it.day);
                if (date.compareTo(today) > 0) {
                    break;
                }
                toAdd.add(new Expense(base + toAdd.size(), it.cents, it.cat, date, it.note));
                it.last = m;
                changed = true;
                m = Dates.shiftYm(m, 1);
            }
        }
        if (changed) {
            save(c, items);
            ExpenseStore.addAllQuiet(c, toAdd);
        }
    }
}
