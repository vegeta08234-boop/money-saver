package com.spendlog.app;

import android.content.Context;
import android.content.res.Configuration;

final class Palette {
    static final String[] NAMES = {"Indigo", "Green", "Orange", "Rose", "Graphite"};
    private static final int[] LIGHT = {0xFF4F46E5, 0xFF15803D, 0xFFC2410C, 0xFFBE123C, 0xFF374151};
    private static final int[] DARK = {0xFF6366F1, 0xFF16A34A, 0xFFEA580C, 0xFFF43F5E, 0xFF6B7280};
    private static final String PREFS = "spendlog";

    private Palette() {}

    static int index(Context c) {
        int i = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("theme", 0);
        return (i < 0 || i >= NAMES.length) ? 0 : i;
    }

    static void setIndex(Context c, int i) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("theme", i).apply();
    }

    static boolean isNight(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    static int accent(Context c) {
        int i = index(c);
        return isNight(c) ? DARK[i] : LIGHT[i];
    }
}
