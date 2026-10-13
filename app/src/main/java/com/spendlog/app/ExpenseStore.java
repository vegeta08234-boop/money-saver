package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ExpenseStore {
    private static final String KEY = "expenses";

    private ExpenseStore() {}

    static synchronized List<Expense> load(Context c) {
        List<Expense> out = new ArrayList<>();
        JSONArray a = Prefs.arr(c, KEY);
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                out.add(new Expense(o.getLong("id"), o.getLong("cents"), o.getString("cat"),
                        o.getString("date"), o.optString("note", "")));
            } catch (JSONException ignored) {
                // skip a bad row
            }
        }
        return out;
    }

    private static void save(Context c, List<Expense> list) {
        JSONArray a = new JSONArray();
        for (Expense e : list) {
            a.put(Prefs.obj("id", e.id, "cents", e.cents, "cat", e.cat, "date", e.date, "note", e.note));
        }
        Prefs.putArr(c, KEY, a);
    }

    static synchronized void add(Context c, Expense e) {
        List<Expense> list = load(c);
        list.add(e);
        save(c, list);
        SpendWidget.updateAll(c);
    }

    /** Adds several entries without refreshing the widget (used by the recurring processor). */
    static synchronized void addAllQuiet(Context c, List<Expense> more) {
        List<Expense> list = load(c);
        list.addAll(more);
        save(c, list);
    }

    static synchronized void update(Context c, Expense e) {
        List<Expense> list = load(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == e.id) {
                list.set(i, e);
            }
        }
        save(c, list);
        SpendWidget.updateAll(c);
    }

    static synchronized void remove(Context c, long id) {
        List<Expense> list = load(c);
        for (Iterator<Expense> it = list.iterator(); it.hasNext(); ) {
            if (it.next().id == id) {
                it.remove();
            }
        }
        save(c, list);
        SpendWidget.updateAll(c);
    }

    static synchronized void renameCategory(Context c, String from, String to) {
        List<Expense> list = load(c);
        for (int i = 0; i < list.size(); i++) {
            Expense e = list.get(i);
            if (e.cat.equals(from)) {
                list.set(i, new Expense(e.id, e.cents, to, e.date, e.note));
            }
        }
        save(c, list);
        SpendWidget.updateAll(c);
    }
}
