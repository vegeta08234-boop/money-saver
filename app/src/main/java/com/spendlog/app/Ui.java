package com.spendlog.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.List;

/** Small view-building helpers shared by the screens. */
final class Ui {
    interface Cents {
        void done(long cents);
    }

    interface DateCb {
        void done(String date);
    }

    static final int DECIMAL = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;

    private Ui() {}

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(t.getTypeface(), Typeface.BOLD);
        }
        return t;
    }

    static GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static View dot(Context c, int color, int sizeDp) {
        View v = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        v.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        lp.setMarginEnd(dp(c, 10));
        v.setLayoutParams(lp);
        return v;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundResource(R.drawable.bg_card);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, dp(c, 10), 0, dp(c, 10));
        return l;
    }

    /** Adds a child that fills the width, with a top margin in dp. */
    static void add(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(parent.getContext(), topDp);
        parent.addView(child, lp);
    }

    /** Adds a child that takes the remaining width of a horizontal row. */
    static void grow(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    }

    static Button button(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(Palette.accent(c), dp(c, 10)));
        return b;
    }

    static LinearLayout progress(Context c, float frac, int fill) {
        LinearLayout track = new LinearLayout(c);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setWeightSum(1000f);
        track.setBackground(rounded(c.getColor(R.color.line), dp(c, 4)));
        float f = Math.max(0f, Math.min(1f, frac));
        if (f > 0f) {
            View v = new View(c);
            v.setBackground(rounded(fill, dp(c, 4)));
            track.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, f * 1000f));
        }
        return track;
    }

    static void addProgress(LinearLayout parent, float frac, int fill) {
        LinearLayout p = progress(parent.getContext(), frac, fill);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(parent.getContext(), 8));
        lp.topMargin = dp(parent.getContext(), 8);
        parent.addView(p, lp);
    }

    /** Returns cents, or -1 when the text is empty or not a non-negative number. */
    static long parseCents(CharSequence in) {
        if (in == null) {
            return -1;
        }
        String t = in.toString().trim().replace(",", ".");
        if (t.isEmpty()) {
            return -1;
        }
        try {
            BigDecimal b = new BigDecimal(t);
            if (b.signum() < 0) {
                return -1;
            }
            return b.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    static String plain(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    static EditText field(Context c, String hint, int inputType, String initial) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextColor(c.getColor(R.color.text));
        e.setHintTextColor(c.getColor(R.color.subtext));
        if (initial != null) {
            e.setText(initial);
        }
        return e;
    }

    static LinearLayout form(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        l.setPadding(p, dp(c, 8), p, 0);
        return l;
    }

    static Spinner spinner(Context c, List<String> items, String selected) {
        Spinner s = new Spinner(c);
        ArrayAdapter<String> a = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        int i = selected == null ? -1 : items.indexOf(selected);
        if (i >= 0) {
            s.setSelection(i);
        }
        return s;
    }

    static void pickDate(Activity a, String current, DateCb cb) {
        Calendar c = Dates.parse(current);
        new DatePickerDialog(a, (picker, y, m, d) -> {
            Calendar n = Calendar.getInstance();
            n.clear();
            n.set(y, m, d, 12, 0, 0);
            cb.done(Dates.key(n));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    /** Asks for one money amount. With allowClear, an empty or zero answer clears it. */
    static void promptMoney(Activity a, String title, long current, boolean allowClear, Cents cb) {
        final EditText e = field(a, "Amount", DECIMAL, current > 0 ? plain(current) : null);
        LinearLayout box = form(a);
        box.addView(e, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);
        if (allowClear && current > 0) {
            b.setNeutralButton("Clear", (d, w) -> cb.done(0));
        }
        final AlertDialog d = b.create();
        d.setOnShowListener(x -> {
            e.requestFocus();
            if (d.getWindow() != null) {
                d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                long cents = parseCents(e.getText());
                boolean empty = e.getText().toString().trim().isEmpty();
                if (empty && allowClear) {
                    d.dismiss();
                    cb.done(0);
                    return;
                }
                if (cents < 0 || (cents == 0 && !allowClear)) {
                    toast(a, "Enter a valid amount");
                    return;
                }
                d.dismiss();
                cb.done(cents);
            });
        });
        d.show();
    }
}
