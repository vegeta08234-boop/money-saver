package com.spendlog.app;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import java.util.List;

public class RecurringActivity extends BaseActivity {

    @Override
    String screenTitle() {
        return "Recurring expenses";
    }

    @Override
    void render() {
        body.removeAllViews();
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);

        Button add = Ui.button(this, "+ Add recurring expense");
        add.setOnClickListener(v -> edit(null));
        Ui.add(body, add, 12);

        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Rent, subscriptions and bills. Each one adds itself to your entries every month on its day.", 13, sub, false));
        List<Recurring.Item> items = Recurring.load(this);
        if (items.isEmpty()) {
            card.addView(Ui.text(this, "Nothing set up yet.", 15, text, false));
        }
        for (final Recurring.Item it : items) {
            LinearLayout row = Ui.row(this);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(Ui.text(this, it.note.isEmpty() ? it.cat : it.note, 16, text, false));
            left.addView(Ui.text(this, it.cat + " · day " + it.day + " · next " + Dates.dayLabel(Recurring.nextDate(it)), 12, sub, false));
            Ui.grow(row, left);
            row.addView(Ui.text(this, Money.fmt(it.cents), 16, text, true));
            row.setOnClickListener(v -> edit(it));
            card.addView(row);
        }
        Ui.add(body, card, 12);
    }

    private void edit(final Recurring.Item existing) {
        final EditText amount = Ui.field(this, "Amount", Ui.DECIMAL,
                existing != null ? Ui.plain(existing.cents) : null);
        final Spinner cat = Ui.spinner(this, Categories.names(this), existing != null ? existing.cat : null);
        final EditText day = Ui.field(this, "Day of the month (1-31)", InputType.TYPE_CLASS_NUMBER,
                existing != null ? String.valueOf(existing.day) : null);
        final EditText note = Ui.field(this, "Name or note (optional)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                existing != null ? existing.note : null);

        LinearLayout form = Ui.form(this);
        form.addView(amount);
        form.addView(cat);
        form.addView(day);
        form.addView(note);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New recurring expense" : "Edit recurring expense")
                .setView(sv)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);
        if (existing != null) {
            b.setNeutralButton("Delete", (d, w) -> {
                Recurring.delete(this, existing.id);
                render();
            });
        }
        final AlertDialog d = b.create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long cents = Ui.parseCents(amount.getText());
            if (cents <= 0) {
                Ui.toast(this, "Enter an amount above zero");
                return;
            }
            int dd;
            try {
                dd = Integer.parseInt(day.getText().toString().trim());
            } catch (NumberFormatException e) {
                dd = 0;
            }
            if (dd < 1 || dd > 31) {
                Ui.toast(this, "Day must be between 1 and 31");
                return;
            }
            String category = (String) cat.getSelectedItem();
            if (category == null) {
                return;
            }
            String n = note.getText().toString().trim();
            Recurring.Item it;
            if (existing == null) {
                it = new Recurring.Item(System.currentTimeMillis(), cents, category, n, dd,
                        Recurring.initialLast(dd));
            } else {
                it = existing;
                if (it.day != dd) {
                    it.last = Recurring.initialLast(dd);
                }
                it.cents = cents;
                it.cat = category;
                it.note = n;
                it.day = dd;
            }
            Recurring.upsert(this, it);
            d.dismiss();
            render();
        }));
        d.show();
    }
}
