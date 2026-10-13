package com.spendlog.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int AMBER = 0xFFF59E0B;

    private Calendar viewMonth = Dates.firstOfMonth(Calendar.getInstance());
    private String chosenDate = Dates.today();

    private List<Expense> allCache = new ArrayList<>();
    private Stats statsCache;

    private ScrollView scroll;
    private View addCard;
    private EditText amount;
    private EditText note;
    private Spinner category;
    private Button dateBtn;
    private Button addBtn;
    private TextView themeBtn;
    private TextView moreBtn;
    private TextView prev;
    private TextView next;
    private TextView monthLabel;
    private TextView monthTotal;
    private TextView todayV;
    private TextView avgV;
    private TextView vsV;
    private TextView emptyV;
    private TextView filterInfo;
    private BarChartView chart;
    private LinearLayout budgetBox;
    private LinearLayout cats;
    private LinearLayout topBox;
    private LinearLayout entries;
    private EditText search;
    private Spinner filterCat;
    private EditText minAmt;
    private EditText maxAmt;

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
        moreBtn = findViewById(R.id.more_btn);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        monthLabel = findViewById(R.id.month_label);
        monthTotal = findViewById(R.id.month_total);
        todayV = findViewById(R.id.today_v);
        avgV = findViewById(R.id.avg_v);
        vsV = findViewById(R.id.vs_v);
        emptyV = findViewById(R.id.empty_v);
        filterInfo = findViewById(R.id.filter_info);
        chart = findViewById(R.id.chart);
        budgetBox = findViewById(R.id.budget_box);
        cats = findViewById(R.id.cats);
        topBox = findViewById(R.id.top_box);
        entries = findViewById(R.id.entries);
        search = findViewById(R.id.search);
        filterCat = findViewById(R.id.filter_cat);
        minAmt = findViewById(R.id.min_amt);
        maxAmt = findViewById(R.id.max_amt);

        fillSpinners();

        updateDateBtn();
        dateBtn.setOnClickListener(v -> pickDate());
        addBtn.setOnClickListener(v -> addExpense());
        themeBtn.setOnClickListener(v -> pickTheme());
        moreBtn.setOnClickListener(v -> showMore());
        prev.setOnClickListener(v -> shiftMonth(-1));
        next.setOnClickListener(v -> shiftMonth(1));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                rebuildEntries();
            }
        };
        search.addTextChangedListener(watcher);
        minAmt.addTextChangedListener(watcher);
        maxAmt.addTextChangedListener(watcher);
        filterCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rebuildEntries();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
        Lock.guard(this);
        applyTheme();
        Recurring.process(this);
        Reminder.schedule(this);
        PaymentListener.rebindIfNeeded(this);
        fillSpinners();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Lock.paused();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Lock.handleResult(this, requestCode, resultCode);
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

    /* ---------- spinners ---------- */

    private void fillSpinners() {
        List<String> names = Categories.names(this);

        String keepAdd = category.getSelectedItem() == null ? null : (String) category.getSelectedItem();
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(a);
        if (keepAdd != null && names.indexOf(keepAdd) >= 0) {
            category.setSelection(names.indexOf(keepAdd));
        }

        String keepFilter = filterCat.getSelectedItemPosition() > 0 ? (String) filterCat.getSelectedItem() : null;
        List<String> f = new ArrayList<>();
        f.add("All categories");
        f.addAll(names);
        ArrayAdapter<String> fa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, f);
        fa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterCat.setAdapter(fa);
        if (keepFilter != null && f.indexOf(keepFilter) >= 0) {
            filterCat.setSelection(f.indexOf(keepFilter));
        }
    }

    /* ---------- actions ---------- */

    private void shiftMonth(int delta) {
        viewMonth.add(Calendar.MONTH, delta);
        refresh();
    }

    private void pickDate() {
        Ui.pickDate(this, chosenDate, date -> {
            chosenDate = date;
            updateDateBtn();
        });
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

    private void showMore() {
        final String[] items = {"Auto-add payments", "Budgets", "Categories", "Recurring expenses", "Savings goals", "Reminder, lock & colors"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    Class<?> target;
                    switch (which) {
                        case 0:
                            target = PaymentsActivity.class;
                            break;
                        case 1:
                            target = BudgetActivity.class;
                            break;
                        case 2:
                            target = CategoriesActivity.class;
                            break;
                        case 3:
                            target = RecurringActivity.class;
                            break;
                        case 4:
                            target = GoalsActivity.class;
                            break;
                        default:
                            target = SettingsActivity.class;
                            break;
                    }
                    startActivity(new Intent(this, target));
                })
                .show();
    }

    private void addExpense() {
        long cents = Ui.parseCents(amount.getText());
        if (cents <= 0) {
            Toast.makeText(this, "Enter an amount greater than zero", Toast.LENGTH_SHORT).show();
            amount.requestFocus();
            return;
        }
        String cat = (String) category.getSelectedItem();
        if (cat == null) {
            return;
        }
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
        moreBtn.setTextColor(a);
        prev.setTextColor(a);
        next.setTextColor(a);
        chart.setColors(a, getColor(R.color.subtext), getColor(R.color.line));
    }

    /* ---------- screen ---------- */

    private void refresh() {
        applyTheme();
        allCache = ExpenseStore.load(this);
        statsCache = Stats.compute(allCache, viewMonth);
        Stats s = statsCache;
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

        buildBudget(s, ym);
        buildCategories(s);
        buildTop(s);
        rebuildEntries();
    }

    private void buildBudget(Stats s, String ym) {
        budgetBox.removeAllViews();
        int accent = Palette.accent(this);
        long budget = Budgets.monthly(this);
        View.OnClickListener open = v -> startActivity(new Intent(this, BudgetActivity.class));
        if (budget <= 0) {
            TextView t = text("Set a monthly budget ›", 14, accent, true);
            t.setOnClickListener(open);
            budgetBox.addView(t);
            return;
        }
        long left = budget - s.monthTotal;
        float frac = budget > 0 ? (float) s.monthTotal / budget : 0f;
        int color = BudgetColor.gradient(frac, Palette.isNight(this));
        Ui.addProgress(budgetBox, frac, color);

        String line = left >= 0
                ? Money.fmt(left) + " left of " + Money.fmt(budget)
                : "Over budget by " + Money.fmt(-left);
        TextView l = text(line, 14, left >= 0 ? getColor(R.color.text) : getColor(R.color.danger), true);
        l.setPadding(0, dp(6), 0, 0);
        budgetBox.addView(l);

        boolean current = Dates.ym(Calendar.getInstance()).equals(ym);
        if (current && left > 0) {
            int days = Stats.daysLeftInMonth();
            TextView d = text("About " + Money.fmt(left / Math.max(1, days)) + " a day for the next "
                    + days + (days == 1 ? " day" : " days"), 13, getColor(R.color.subtext), false);
            budgetBox.addView(d);
        }
        budgetBox.setOnClickListener(open);
    }

    private void buildCategories(Stats s) {
        cats.removeAllViews();
        List<Categories.Cat> known = Categories.load(this);
        Map<String, Long> totals = new HashMap<>(s.byCategory);
        for (Categories.Cat k : known) {
            if (!totals.containsKey(k.name) && Budgets.forCat(this, k.name) > 0) {
                totals.put(k.name, 0L);
            }
        }
        List<Map.Entry<String, Long>> rows = new ArrayList<>(totals.entrySet());
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
        long max = 1;
        for (Map.Entry<String, Long> r : rows) {
            max = Math.max(max, r.getValue());
        }
        for (Map.Entry<String, Long> r : rows) {
            long spent = r.getValue();
            long limit = Budgets.forCat(this, r.getKey());
            int color = Categories.colorOf(known, r.getKey());

            LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, dp(8), 0, 0);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(Ui.dot(this, color, 10));
            top.addView(text(r.getKey(), 14, getColor(R.color.text), false),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            String amt = limit > 0 ? Money.fmt(spent) + " of " + Money.fmt(limit) : Money.fmt(spent);
            int amtColor = getColor(R.color.text);
            if (limit > 0 && spent >= limit) {
                amtColor = getColor(R.color.danger);
            }
            top.addView(text(amt, 14, amtColor, true));
            block.addView(top);

            float frac;
            int barColor = color;
            if (limit > 0) {
                frac = (float) spent / limit;
                barColor = BudgetColor.gradient(frac, Palette.isNight(this));
            } else {
                frac = (float) spent / max;
            }
            Ui.addProgress(block, spent > 0 ? Math.max(0.02f, frac) : 0f, barColor);
            if (limit > 0 && spent >= limit) {
                block.addView(text("Over the limit by " + Money.fmt(spent - limit), 12, getColor(R.color.danger), false));
            } else if (limit > 0 && frac >= 0.8f) {
                block.addView(text(Money.fmt(limit - spent) + " left before the limit", 12, AMBER, false));
            }
            cats.addView(block);
        }
    }

    private LinearLayout twoCol(String left, String right, String sub) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.addView(text(left, 14, getColor(R.color.text), false),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(text(right, 14, getColor(R.color.text), true));
        row.addView(line);
        if (sub != null) {
            row.addView(text(sub, 12, getColor(R.color.subtext), false));
        }
        return row;
    }

    private TextView label(String s) {
        TextView t = text(s, 12, getColor(R.color.subtext), true);
        t.setPadding(0, dp(10), 0, dp(2));
        return t;
    }

    private void buildTop(Stats s) {
        topBox.removeAllViews();
        if (s.count == 0) {
            topBox.addView(text("Nothing to summarize yet.", 14, getColor(R.color.subtext), false));
            return;
        }
        Comparator<Map.Entry<String, Long>> desc = new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> x, Map.Entry<String, Long> y) {
                return Long.compare(y.getValue(), x.getValue());
            }
        };

        topBox.addView(label("BIGGEST DAYS"));
        List<Map.Entry<String, Long>> days = new ArrayList<>(s.byDate.entrySet());
        Collections.sort(days, desc);
        for (int i = 0; i < Math.min(3, days.size()); i++) {
            topBox.addView(twoCol(Dates.dayLabel(days.get(i).getKey()), Money.fmt(days.get(i).getValue()), null));
        }

        topBox.addView(label("TOP CATEGORIES"));
        List<Map.Entry<String, Long>> cs = new ArrayList<>(s.byCategory.entrySet());
        Collections.sort(cs, desc);
        for (int i = 0; i < Math.min(3, cs.size()); i++) {
            long v = cs.get(i).getValue();
            long pct = s.monthTotal > 0 ? Math.round(v * 100.0 / s.monthTotal) : 0;
            topBox.addView(twoCol(cs.get(i).getKey(), Money.fmt(v), pct + "% of the month"));
        }

        if (s.biggest != null) {
            topBox.addView(label("BIGGEST PURCHASE"));
            String name = s.biggest.cat + (s.biggest.note.isEmpty() ? "" : " · " + s.biggest.note);
            topBox.addView(twoCol(name, Money.fmt(s.biggest.cents), Dates.dayLabel(s.biggest.date)));
        }
    }

    private void rebuildEntries() {
        if (statsCache == null) {
            return;
        }
        buildEntries();
    }

    private boolean filtersActive() {
        return filterCat.getSelectedItemPosition() > 0
                || !search.getText().toString().trim().isEmpty()
                || Ui.parseCents(minAmt.getText()) >= 0
                || Ui.parseCents(maxAmt.getText()) >= 0;
    }

    private void buildEntries() {
        entries.removeAllViews();
        String ym = Dates.ym(viewMonth);

        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        String fc = filterCat.getSelectedItemPosition() > 0 ? (String) filterCat.getSelectedItem() : null;
        long min = Ui.parseCents(minAmt.getText());
        long max = Ui.parseCents(maxAmt.getText());

        int inMonth = 0;
        List<Expense> shown = new ArrayList<>();
        for (Expense e : allCache) {
            if (!e.date.startsWith(ym)) {
                continue;
            }
            inMonth++;
            if (fc != null && !e.cat.equals(fc)) {
                continue;
            }
            if (min >= 0 && e.cents < min) {
                continue;
            }
            if (max >= 0 && e.cents > max) {
                continue;
            }
            if (!q.isEmpty()) {
                String amt = String.format(Locale.US, "%.2f", e.cents / 100.0);
                boolean hit = e.note.toLowerCase(Locale.getDefault()).contains(q)
                        || e.cat.toLowerCase(Locale.getDefault()).contains(q)
                        || amt.contains(q);
                if (!hit) {
                    continue;
                }
            }
            shown.add(e);
        }

        boolean active = filtersActive();
        if (active && inMonth > 0) {
            long sum = 0;
            for (Expense e : shown) {
                sum += e.cents;
            }
            filterInfo.setText("Showing " + shown.size() + " of " + inMonth + " · " + Money.fmt(sum));
            filterInfo.setVisibility(View.VISIBLE);
        } else {
            filterInfo.setVisibility(View.GONE);
        }

        emptyV.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        emptyV.setText(active && inMonth > 0 ? "No entries match your filters." : "Nothing logged this month yet.");

        Collections.sort(shown, new Comparator<Expense>() {
            @Override
            public int compare(Expense a, Expense b) {
                int c = b.date.compareTo(a.date);
                return c != 0 ? c : Long.compare(b.id, a.id);
            }
        });

        Map<String, Long> dayTotals = new HashMap<>();
        for (Expense e : shown) {
            Long cur = dayTotals.get(e.date);
            dayTotals.put(e.date, (cur == null ? 0L : cur) + e.cents);
        }

        List<Categories.Cat> known = Categories.load(this);
        java.util.Set<Long> autos = AutoLog.autoIds(this);
        String lastDate = null;
        for (final Expense e : shown) {
            if (!e.date.equals(lastDate)) {
                lastDate = e.date;
                LinearLayout head = new LinearLayout(this);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setPadding(0, dp(14), 0, dp(2));
                head.addView(text(Dates.dayLabel(e.date), 13, getColor(R.color.subtext), true),
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                Long dt = dayTotals.get(e.date);
                head.addView(text(Money.fmt(dt == null ? 0 : dt), 13, getColor(R.color.subtext), true));
                entries.addView(head);
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            row.addView(Ui.dot(this, Categories.colorOf(known, e.cat), 10));

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(e.cat + (autos.contains(e.id) ? "  \u00B7  auto" : ""), 15, getColor(R.color.text), false));
            if (!e.note.isEmpty()) {
                left.addView(text(e.note, 13, getColor(R.color.subtext), false));
            }
            row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(text(Money.fmt(e.cents), 15, getColor(R.color.text), true));

            row.setOnClickListener(v -> editEntry(e));
            row.setOnLongClickListener(v -> {
                confirmDelete(e);
                return true;
            });
            entries.addView(row);
        }
    }

    /* ---------- edit / remove ---------- */

    private void editEntry(final Expense e) {
        final EditText amt = Ui.field(this, "Amount", Ui.DECIMAL, Ui.plain(e.cents));
        List<String> names = Categories.names(this);
        if (!names.contains(e.cat)) {
            names.add(e.cat);
        }
        final Spinner cat = Ui.spinner(this, names, e.cat);
        final String[] date = {e.date};
        final Button dateB = new Button(this);
        dateB.setAllCaps(false);
        dateB.setText("Date: " + Dates.dayLabel(date[0]));
        dateB.setOnClickListener(v -> Ui.pickDate(this, date[0], d -> {
            date[0] = d;
            dateB.setText("Date: " + Dates.dayLabel(d));
        }));
        final EditText n = Ui.field(this, "Note (optional)",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, e.note);

        LinearLayout form = Ui.form(this);
        form.addView(amt);
        form.addView(cat);
        form.addView(dateB);
        form.addView(n);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Edit entry")
                .setView(sv)
                .setPositiveButton("Save", null)
                .setNeutralButton("Remove", (dlg, w) -> confirmDelete(e))
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long cents = Ui.parseCents(amt.getText());
            if (cents <= 0) {
                Toast.makeText(this, "Enter an amount greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }
            String c = (String) cat.getSelectedItem();
            if (c == null) {
                return;
            }
            ExpenseStore.update(this, new Expense(e.id, cents, c, date[0], n.getText().toString().trim()));
            d.dismiss();
            refresh();
        }));
        d.show();
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

    /* ---------- helpers ---------- */

    private TextView text(String s, int sp, int color, boolean bold) {
        return Ui.text(this, s, sp, color, bold);
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }
}
