package com.spendlog.app;

import android.content.Context;
import android.graphics.Color;

import java.util.Calendar;

/** Green when the budget is fresh, amber as it fills, red when it is used up. */
final class BudgetColor {
    private BudgetColor() {}

    /** Share of this month's budget already spent (1.0 = all of it), or -1 when no budget is set. */
    static float ratio(Context c) {
        long budget = Budgets.monthly(c);
        if (budget <= 0) {
            return -1f;
        }
        String ym = Dates.ym(Calendar.getInstance());
        long total = 0;
        for (Expense e : ExpenseStore.load(c)) {
            if (e.date.startsWith(ym)) {
                total += e.cents;
            }
        }
        return (float) total / budget;
    }

    /** Smooth green (0%), through yellow and amber, to red (100% and over). */
    static int gradient(float ratio, boolean night) {
        float r = Math.max(0f, Math.min(1f, ratio));
        float hue = 140f * (1f - r);
        return Color.HSVToColor(new float[]{hue, 0.82f, night ? 0.95f : 0.80f});
    }

    /** Moves the app's accent color toward the warning color once half the budget is used. */
    static int tint(Context c, int base) {
        if (!AppSettings.tintOn(c)) {
            return base;
        }
        float r = ratio(c);
        if (r < 0.5f) {
            return base;
        }
        float t = Math.min(1f, (r - 0.5f) / 0.5f);
        return blend(base, gradient(r, Palette.isNight(c)), t);
    }

    static int blend(int a, int b, float t) {
        float u = 1f - t;
        return Color.argb(255,
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }
}
