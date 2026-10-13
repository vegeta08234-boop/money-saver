package com.spendlog.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bookkeeping for auto-added payments: which entries came from a notification, a short log, and duplicate checks. */
final class AutoLog {
    static final class Entry {
        long time;
        String app;
        String status;
        String snippet;
    }

    private static final int MAX_IDS = 400;
    private static final int MAX_LOG = 20;
    private static final long DUP_WINDOW_MS = 120_000L;

    private AutoLog() {}

    static synchronized Set<Long> autoIds(Context c) {
        Set<Long> out = new HashSet<>();
        JSONArray a = Prefs.arr(c, "auto_ids");
        for (int i = 0; i < a.length(); i++) {
            out.add(a.optLong(i));
        }
        return out;
    }

    static synchronized void markAuto(Context c, long id) {
        JSONArray a = Prefs.arr(c, "auto_ids");
        a.put(id);
        Prefs.putArr(c, "auto_ids", tail(a, MAX_IDS));
    }

    static synchronized void log(Context c, String app, String status, String snippet) {
        JSONArray a = Prefs.arr(c, "auto_log");
        a.put(Prefs.obj("t", System.currentTimeMillis(), "a", app, "s", status, "x", snippet));
        Prefs.putArr(c, "auto_log", tail(a, MAX_LOG));
    }

    /** Newest first. */
    static synchronized List<Entry> recent(Context c) {
        List<Entry> out = new ArrayList<>();
        JSONArray a = Prefs.arr(c, "auto_log");
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) {
                continue;
            }
            Entry e = new Entry();
            e.time = o.optLong("t");
            e.app = o.optString("a");
            e.status = o.optString("s");
            e.snippet = o.optString("x");
            out.add(e);
        }
        return out;
    }

    static synchronized void clearLog(Context c) {
        Prefs.putArr(c, "auto_log", new JSONArray());
    }

    /**
     * True when the same amount was already auto-added in the last two minutes. This also stops a wallet
     * and a bank app from adding the same purchase twice.
     */
    static synchronized boolean duplicate(Context c, long cents, long now) {
        JSONArray a = Prefs.arr(c, "auto_seen");
        JSONArray keep = new JSONArray();
        boolean dup = false;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null || now - o.optLong("t") > DUP_WINDOW_MS) {
                continue;
            }
            keep.put(o);
            if (o.optLong("c") == cents) {
                dup = true;
            }
        }
        if (!dup) {
            keep.put(Prefs.obj("c", cents, "t", now));
        }
        Prefs.putArr(c, "auto_seen", keep);
        return dup;
    }

    private static JSONArray tail(JSONArray a, int max) {
        if (a.length() <= max) {
            return a;
        }
        JSONArray out = new JSONArray();
        for (int i = a.length() - max; i < a.length(); i++) {
            out.put(a.opt(i));
        }
        return out;
    }
}
