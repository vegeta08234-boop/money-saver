package com.spendlog.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Prefs {
    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("spendlog", Context.MODE_PRIVATE);
    }

    static JSONArray arr(Context c, String key) {
        try {
            return new JSONArray(sp(c).getString(key, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    static void putArr(Context c, String key, JSONArray a) {
        sp(c).edit().putString(key, a.toString()).apply();
    }

    /** Builds a JSON object from alternating key, value arguments. */
    static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                o.put((String) kv[i], kv[i + 1]);
            }
        } catch (JSONException ignored) {
            // keys are plain strings, nothing to handle
        }
        return o;
    }
}
