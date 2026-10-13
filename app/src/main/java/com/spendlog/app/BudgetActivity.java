package com.spendlog.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class BudgetActivity extends BaseActivity {

    @Override
    String screenTitle() {
        return "Budgets";
    }

    @Override
    void render() {
        body.removeAllViews();
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);

        // Monthly budget
        LinearLayout monthly = Ui.card(this);
        monthly.addView(Ui.text(this, "Monthly budget", 16, text, true));
        long m = Budgets.monthly(this);
        monthly.addView(Ui.text(this, m > 0 ? Money.fmt(m) : "Not set", 30, text, true));
        monthly.addView(Ui.text(this, "How much you plan to spend each month. The app and widget show what is left and a daily allowance.", 13, sub, false));
        monthly.setOnClickListener(v -> Ui.promptMoney(this, "Monthly budget", Budgets.monthly(this), true, cents -> {
            Budgets.setMonthly(this, cents);
            render();
        }));
        Ui.add(body, monthly, 12);

        // Per-category limits
        LinearLayout limits = Ui.card(this);
        limits.addView(Ui.text(this, "Category limits", 16, text, true));
        limits.addView(Ui.text(this, "Tap a category to set a monthly limit. You get a warning color at 80% and when you go over.", 13, sub, false));
        List<Categories.Cat> cats = Categories.load(this);
        for (final Categories.Cat k : cats) {
            LinearLayout row = Ui.row(this);
            row.addView(Ui.dot(this, k.color, 12));
            Ui.grow(row, Ui.text(this, k.name, 15, text, false));
            long lim = Budgets.forCat(this, k.name);
            TextView val = Ui.text(this, lim > 0 ? Money.fmt(lim) : "No limit", 15, lim > 0 ? text : sub, lim > 0);
            row.addView(val);
            row.setOnClickListener(v -> Ui.promptMoney(this, "Limit for " + k.name,
                    Budgets.forCat(this, k.name), true, cents -> {
                        Budgets.setCat(this, k.name, cents);
                        render();
                    }));
            limits.addView(row);
        }
        Ui.add(body, limits, 12);
    }
}
