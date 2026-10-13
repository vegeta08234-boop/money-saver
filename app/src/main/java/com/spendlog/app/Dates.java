package com.spendlog.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class Dates {
    private Dates() {}

    static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static String today() {
        return key(Calendar.getInstance());
    }

    /** "yyyy-MM" of the given calendar. */
    static String ym(Calendar c) {
        return key(c).substring(0, 7);
    }

    static Calendar parse(String key) {
        String[] p = key.split("-");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0, 0);
        return c;
    }

    static Calendar firstOfMonth(Calendar c) {
        Calendar r = (Calendar) c.clone();
        r.set(Calendar.DAY_OF_MONTH, 1);
        return r;
    }

    static int daysInMonth(Calendar c) {
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** Moves a "yyyy-MM" string by the given number of months. */
    static String shiftYm(String ym, int delta) {
        int y = Integer.parseInt(ym.substring(0, 4));
        int m = Integer.parseInt(ym.substring(5, 7));
        int total = y * 12 + (m - 1) + delta;
        return String.format(Locale.US, "%04d-%02d", total / 12, total % 12 + 1);
    }

    static String monthLabel(Calendar c) {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.getTime());
    }

    static String dayLabel(String key) {
        return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parse(key).getTime());
    }
}
