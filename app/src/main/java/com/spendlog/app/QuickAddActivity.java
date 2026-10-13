package com.spendlog.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/** A small popup opened from the widget: pick a category there, type the amount here. */
public class QuickAddActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        final String cat = getIntent().getStringExtra("cat");
        if (cat == null) {
            finish();
            return;
        }
        setFinishOnTouchOutside(true);

        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);
        int accent = Palette.accent(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, p, p, p);
        box.setMinimumWidth(Ui.dp(this, 280));

        box.addView(Ui.text(this, "Add to " + cat, 18, text, true));
        box.addView(Ui.text(this, "Today, " + Dates.dayLabel(Dates.today()), 13, sub, false));

        final EditText amount = Ui.field(this, "Amount", Ui.DECIMAL, null);
        amount.setTextSize(26);
        Ui.add(box, amount, 8);
        final EditText note = Ui.field(this, "Note (optional)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, null);
        Ui.add(box, note, 0);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        cancel.setTextColor(accent);
        cancel.setBackground(null);
        cancel.setOnClickListener(v -> finish());
        Button add = Ui.button(this, "Add");
        add.setOnClickListener(v -> {
            long cents = Ui.parseCents(amount.getText());
            if (cents <= 0) {
                Ui.toast(this, "Enter an amount above zero");
                return;
            }
            ExpenseStore.add(this, new Expense(System.currentTimeMillis(), cents, cat, Dates.today(),
                    note.getText().toString().trim()));
            Ui.toast(this, "Added " + Money.fmt(cents) + " to " + cat);
            finish();
        });
        buttons.addView(cancel);
        buttons.addView(add);
        Ui.add(box, buttons, 8);

        setContentView(box);
        amount.requestFocus();
    }
}
