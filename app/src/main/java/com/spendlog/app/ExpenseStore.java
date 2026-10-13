package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ExpenseStore {
    private static final String PREFS = "spendlog";
    private static final String KEY = "expenses";

    private ExpenseStore() {}

    static synchronized List<Expense> load(Context c) {
        List<Expense> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Expense(o.getLong("id"), o.getLong("cents"), o.getString("cat"),
                        o.getString("date"), o.optString("note", "")));
            }
        } catch (JSONException ignored) {
            // corrupt data: start from what we parsed so far
        }
        return out;
    }

    private static void save(Context c, List<Expense> list) {
        JSONArray a = new JSONArray();
        for (Expense e : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", e.id);
                o.put("cents", e.cents);
                o.put("cat", e.cat);
                o.put("date", e.date);
                o.put("note", e.note);
            } catch (JSONException ignored) {
                continue;
            }
            a.put(o);
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    static synchronized void add(Context c, Expense e) {
        List<Expense> list = load(c);
        list.add(e);
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
}
