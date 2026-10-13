package com.spendlog.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String[] CATEGORIES = {
            "Food", "Groceries", "Transport", "Bills", "Shopping", "Fun", "Health", "Other"
    };

    private Calendar viewMonth = Dates.firstOfMonth(Calendar.getInstance());
    private String chosenDate = Dates.today();

    private ScrollView scroll;
    private View addCard;
    private EditText amount;
    private EditText note;
    private Spinner category;
    private Button dateBtn;
    private Button addBtn;
    private TextView themeBtn;
    private TextView prev;
    private TextView next;
    private TextView monthLabel;
    private TextView monthTotal;
    private TextView todayV;
    private TextView avgV;
    private TextView vsV;
    private TextView emptyV;
    private BarChartView chart;
    private LinearLayout cats;
    private LinearLayout entries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scroll = findViewById(R.id.scroll);
        addCard = findViewById(R.id.add_card);
        amount = findViewById(R.id.amount);
        note = findViewById(R.id.note);
        category = findViewById(R.id.category);
        dateBtn = findViewById(R.id.date_btn);
        addBtn = findViewById(R.id.add_btn);
        themeBtn = findViewById(R.id.theme_btn);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        monthLabel = findViewById(R.id.month_label);
        monthTotal = findViewById(R.id.month_total);
        todayV = findViewById(R.id.today_v);
        avgV = findViewById(R.id.avg_v);
        vsV = findViewById(R.id.vs_v);
        emptyV = findViewById(R.id.empty_v);
        chart = findViewById(R.id.chart);
        cats = findViewById(R.id.cats);
        entries = findViewById(R.id.entries);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(adapter);

        updateDateBtn();
        dateBtn.setOnClickListener(v -> pickDate());
        addBtn.setOnClickListener(v -> addExpense());
        themeBtn.setOnClickListener(v -> pickTheme());
        prev.setOnClickListener(v -> shiftMonth(-1));
        next.setOnClickListener(v -> shiftMonth(1));

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        refresh();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("focus_add", false)) {
            intent.removeExtra("focus_add");
            viewMonth = Dates.firstOfMonth(Calendar.getInstance());
            chosenDate = Dates.today();
            updateDateBtn();
            scroll.post(() -> {
                scroll.smoothScrollTo(0, addCard.getTop());
                amount.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(amount, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    private void shiftMonth(int delta) {
        viewMonth.add(Calendar.MONTH, delta);
        refresh();
    }

    private void pickDate() {
        Calendar c = Dates.parse(chosenDate);
        new DatePickerDialog(this, (picker, y, m, d) -> {
            Calendar n = Calendar.getInstance();
            n.clear();
            n.set(y, m, d, 12, 0, 0);
            chosenDate = Dates.key(n);
            updateDateBtn();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateBtn() {
        String label = Dates.dayLabel(chosenDate);
        if (chosenDate.equals(Dates.today())) {
            label = "Today, " + label;
        }
        dateBtn.setText("Date: " + label);
    }

    private void pickTheme() {
        new AlertDialog.Builder(this)
                .setTitle("Color theme")
                .setSingleChoiceItems(Palette.NAMES, Palette.index(this), (dialog, which) -> {
                    Palette.setIndex(this, which);
                    dialog.dismiss();
                    applyTheme();
                    refresh();
                    SpendWidget.updateAll(this);
                })
                .show();
    }

    private long parseCents(String s) {
        try {
            String t = s.trim().replace(",", ".");
            if (t.isEmpty()) {
                return -1;
            }
            BigDecimal b = new BigDecimal(t);
            return b.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    private void addExpense() {
        long cents = parseCents(amount.getText().toString());
        if (cents <= 0) {
            Toast.makeText(this, "Enter an amount greater than zero", Toast.LENGTH_SHORT).show();
            amount.requestFocus();
            return;
        }
        String cat = CATEGORIES[category.getSelectedItemPosition()];
        Expense e = new Expense(System.currentTimeMillis(), cents, cat, chosenDate,
                note.getText().toString().trim());
        ExpenseStore.add(this, e);

        amount.setText("");
        note.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(amount.getWindowToken(), 0);
        }
        amount.clearFocus();
        note.clearFocus();

        viewMonth = Dates.firstOfMonth(Dates.parse(chosenDate));
        refresh();
        Toast.makeText(this, "Added " + Money.fmt(cents), Toast.LENGTH_SHORT).show();
    }

    private void applyTheme() {
        int a = Palette.accent(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(a);
        g.setCornerRadius(dp(10));
        addBtn.setBackground(g);
        addBtn.setTextColor(Color.WHITE);
        themeBtn.setTextColor(a);
        prev.setTextColor(a);
        next.setTextColor(a);
        chart.setColors(a, getColor(R.color.subtext), getColor(R.color.line));
    }

    private void refresh() {
        List<Expense> all = ExpenseStore.load(this);
        Stats s = Stats.compute(all, viewMonth);
        int accent = Palette.accent(this);
        String ym = Dates.ym(viewMonth);

        monthLabel.setText(Dates.monthLabel(viewMonth));
        monthTotal.setText(Money.fmt(s.monthTotal));
        todayV.setText(Money.fmt(s.todayTotal));
        avgV.setText(Money.fmt(s.dailyAvg));
        if (s.prevMonthTotal <= 0) {
            vsV.setText("—");
        } else {
            long pct = Math.round((s.monthTotal - s.prevMonthTotal) * 100.0 / s.prevMonthTotal);
            vsV.setText((pct > 0 ? "+" : "") + pct + "%");
        }

        Calendar now = Calendar.getInstance();
        int highlight = Dates.ym(now).equals(ym) ? now.get(Calendar.DAY_OF_MONTH) - 1 : -1;
        chart.setData(s.byDay, highlight);

        buildCategories(s, accent);
        buildEntries(all, s, ym);
    }

    private void buildCategories(Stats s, int accent) {
        cats.removeAllViews();
        List<Map.Entry<String, Long>> rows = new ArrayList<>(s.byCategory.entrySet());
        Collections.sort(rows, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> x, Map.Entry<String, Long> y) {
                return Long.compare(y.getValue(), x.getValue());
            }
        });
        if (rows.isEmpty()) {
            cats.addView(text("No spending yet.", 14, getColor(R.color.subtext), false));
            return;
        }
        long max = Math.max(1, rows.get(0).getValue());
        for (Map.Entry<String, Long> r : rows) {
            LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, dp(8), 0, 0);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(r.getKey(), 14, getColor(R.color.text), false);
            top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(text(Money.fmt(r.getValue()), 14, getColor(R.color.text), true));
            block.addView(top);

            LinearLayout bar = new LinearLayout(this);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setWeightSum(1000f);
            float frac = Math.max(0.02f, (float) r.getValue() / max);
            View fill = new View(this);
            GradientDrawable gf = new GradientDrawable();
            gf.setColor(accent);
            gf.setCornerRadius(dp(3));
            fill.setBackground(gf);
            bar.addView(fill, new LinearLayout.LayoutParams(0, dp(6), frac * 1000f));
            LinearLayout.LayoutParams bp =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            bp.topMargin = dp(4);
            block.addView(bar, bp);
            cats.addView(block);
        }
    }

    private void buildEntries(List<Expense> all, Stats s, String ym) {
        entries.removeAllViews();
        List<Expense> month = new ArrayList<>();
        for (Expense e : all) {
            if (e.date.startsWith(ym)) {
                month.add(e);
            }
        }
        emptyV.setVisibility(month.isEmpty() ? View.VISIBLE : View.GONE);
        Collections.sort(month, new Comparator<Expense>() {
            @Override
            public int compare(Expense a, Expense b) {
                int c = b.date.compareTo(a.date);
                return c != 0 ? c : Long.compare(b.id, a.id);
            }
        });

        String lastDate = null;
        for (final Expense e : month) {
            if (!e.date.equals(lastDate)) {
                lastDate = e.date;
                Long dayTotal = s.byDate.get(e.date);
                LinearLayout head = new LinearLayout(this);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setPadding(0, dp(14), 0, dp(2));
                head.addView(text(Dates.dayLabel(e.date), 13, getColor(R.color.subtext), true),
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                head.addView(text(Money.fmt(dayTotal == null ? 0 : dayTotal), 13, getColor(R.color.subtext), true));
                entries.addView(head);
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(e.cat, 15, getColor(R.color.text), false));
            if (!e.note.isEmpty()) {
                left.addView(text(e.note, 13, getColor(R.color.subtext), false));
            }
            row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(text(Money.fmt(e.cents), 15, getColor(R.color.text), true));

            row.setOnLongClickListener(v -> {
                confirmDelete(e);
                return true;
            });
            entries.addView(row);
        }
    }

    private void confirmDelete(final Expense e) {
        new AlertDialog.Builder(this)
                .setTitle("Remove this entry?")
                .setMessage(e.cat + " · " + Money.fmt(e.cents) + " · " + Dates.dayLabel(e.date))
                .setPositiveButton("Remove", (d, w) -> {
                    ExpenseStore.remove(this, e.id);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
