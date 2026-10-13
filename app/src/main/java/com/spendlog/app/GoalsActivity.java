package com.spendlog.app;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

public class GoalsActivity extends BaseActivity {

    private static final int DONE = 0xFF15803D;

    @Override
    String screenTitle() {
        return "Savings goals";
    }

    @Override
    void render() {
        body.removeAllViews();
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);

        Button add = Ui.button(this, "+ New goal");
        add.setOnClickListener(v -> edit(null));
        Ui.add(body, add, 12);

        List<Goals.Goal> goals = Goals.load(this);
        if (goals.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.text(this, "Set a target, then add money to it as you save. Tap a goal to add money, edit or delete it.", 14, sub, false));
            Ui.add(body, empty, 12);
        }
        for (final Goals.Goal g : goals) {
            LinearLayout card = Ui.card(this);
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            Ui.grow(top, Ui.text(this, g.name, 16, text, true));
            int pct = g.target > 0 ? (int) Math.min(100, g.saved * 100 / g.target) : 0;
            top.addView(Ui.text(this, pct + "%", 16, g.saved >= g.target ? DONE : text, true));
            card.addView(top);
            card.addView(Ui.text(this, Money.fmt(g.saved) + " saved of " + Money.fmt(g.target)
                    + (g.saved >= g.target ? " · goal reached" : " · " + Money.fmt(g.target - g.saved) + " to go"), 13, sub, false));
            Ui.addProgress(card, g.target > 0 ? (float) g.saved / g.target : 0f, g.saved >= g.target ? DONE : accent);
            card.setOnClickListener(v -> options(g));
            Ui.add(body, card, 12);
        }
    }

    private void options(final Goals.Goal g) {
        String[] items = {"Add money", "Take money out", "Edit goal", "Delete goal"};
        new AlertDialog.Builder(this)
                .setTitle(g.name)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        Ui.promptMoney(this, "Add to " + g.name, 0, false, cents -> {
                            g.saved += cents;
                            Goals.upsert(this, g);
                            render();
                        });
                    } else if (which == 1) {
                        Ui.promptMoney(this, "Take out of " + g.name, 0, false, cents -> {
                            g.saved = Math.max(0, g.saved - cents);
                            Goals.upsert(this, g);
                            render();
                        });
                    } else if (which == 2) {
                        edit(g);
                    } else {
                        confirmDelete(g);
                    }
                })
                .show();
    }

    private void confirmDelete(final Goals.Goal g) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + g.name + "?")
                .setMessage("The goal and its saved amount are removed.")
                .setPositiveButton("Delete", (d, w) -> {
                    Goals.delete(this, g.id);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void edit(final Goals.Goal existing) {
        final EditText name = Ui.field(this, "Goal name (e.g. New laptop)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                existing != null ? existing.name : null);
        final EditText target = Ui.field(this, "Target amount", Ui.DECIMAL,
                existing != null ? Ui.plain(existing.target) : null);
        LinearLayout form = Ui.form(this);
        form.addView(name);
        form.addView(target);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New goal" : "Edit goal")
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            long cents = Ui.parseCents(target.getText());
            if (n.isEmpty()) {
                Ui.toast(this, "Enter a name");
                return;
            }
            if (cents <= 0) {
                Ui.toast(this, "Enter a target above zero");
                return;
            }
            long id = existing == null ? System.currentTimeMillis() : existing.id;
            long saved = existing == null ? 0 : existing.saved;
            Goals.upsert(this, new Goals.Goal(id, n, cents, saved));
            d.dismiss();
            render();
        }));
        d.show();
    }
}
