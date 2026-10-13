package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class Goals {
    static final class Goal {
        long id;
        String name;
        long target;
        long saved;

        Goal(long id, String name, long target, long saved) {
            this.id = id;
            this.name = name;
            this.target = target;
            this.saved = saved;
        }
    }

    private static final String KEY = "goals";

    private Goals() {}

    static synchronized List<Goal> load(Context c) {
        List<Goal> out = new ArrayList<>();
        JSONArray a = Prefs.arr(c, KEY);
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                out.add(new Goal(o.getLong("id"), o.getString("name"), o.getLong("target"), o.getLong("saved")));
            } catch (JSONException ignored) {
                // skip a bad row
            }
        }
        return out;
    }

    private static void save(Context c, List<Goal> list) {
        JSONArray a = new JSONArray();
        for (Goal g : list) {
            a.put(Prefs.obj("id", g.id, "name", g.name, "target", g.target, "saved", g.saved));
        }
        Prefs.putArr(c, KEY, a);
    }

    static synchronized void upsert(Context c, Goal goal) {
        List<Goal> list = load(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == goal.id) {
                list.set(i, goal);
                found = true;
            }
        }
        if (!found) {
            list.add(goal);
        }
        save(c, list);
    }

    static synchronized void delete(Context c, long id) {
        List<Goal> list = load(c);
        List<Goal> keep = new ArrayList<>();
        for (Goal g : list) {
            if (g.id != id) {
                keep.add(g);
            }
        }
        save(c, keep);
    }
}
