package com.spendlog.app;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class CategoriesActivity extends BaseActivity {

    @Override
    String screenTitle() {
        return "Categories";
    }

    @Override
    void render() {
        body.removeAllViews();
        Button add = Ui.button(this, "+ Add category");
        add.setOnClickListener(v -> edit(null));
        Ui.add(body, add, 12);

        LinearLayout card = Ui.card(this);
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);
        card.addView(Ui.text(this, "Tap a category to rename it, change its color, or delete it.", 13, sub, false));
        for (final Categories.Cat k : Categories.load(this)) {
            LinearLayout row = Ui.row(this);
            row.addView(Ui.dot(this, k.color, 14));
            Ui.grow(row, Ui.text(this, k.name, 16, text, false));
            row.addView(Ui.text(this, "›", 20, sub, false));
            row.setOnClickListener(v -> edit(k));
            card.addView(row);
        }
        Ui.add(body, card, 12);
    }

    private void edit(final Categories.Cat existing) {
        final int[] chosen = {existing != null ? existing.color : Categories.COLORS[0]};
        final EditText name = Ui.field(this, "Category name",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                existing != null ? existing.name : null);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});

        LinearLayout form = Ui.form(this);
        form.addView(name);

        final List<View> swatches = new ArrayList<>();
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        for (int i = 0; i < Categories.COLORS.length; i++) {
            final int color = Categories.COLORS[i];
            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34));
            lp.setMarginEnd(Ui.dp(this, 10));
            sw.setLayoutParams(lp);
            sw.setTag(color);
            sw.setOnClickListener(v -> {
                chosen[0] = color;
                paintSwatches(swatches, chosen[0]);
            });
            swatches.add(sw);
            strip.addView(sw);
        }
        paintSwatches(swatches, chosen[0]);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(strip);
        form.addView(hs);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New category" : "Edit category")
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);
        if (existing != null) {
            b.setNeutralButton("Delete", (d, w) -> confirmDelete(existing));
        }
        final AlertDialog d = b.create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) {
                Ui.toast(this, "Enter a name");
                return;
            }
            List<Categories.Cat> all = Categories.load(this);
            boolean clash = Categories.exists(all, n)
                    && (existing == null || !existing.name.equalsIgnoreCase(n));
            if (clash) {
                Ui.toast(this, "You already have a category with that name");
                return;
            }
            if (existing == null) {
                Categories.add(this, n, chosen[0]);
            } else {
                Categories.update(this, existing.name, n, chosen[0]);
            }
            d.dismiss();
            render();
        }));
        d.show();
    }

    private void paintSwatches(List<View> swatches, int selected) {
        for (View v : swatches) {
            int color = (Integer) v.getTag();
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(color);
            if (color == selected) {
                g.setStroke(Ui.dp(this, 3), getColor(R.color.text));
            }
            v.setBackground(g);
        }
    }

    private void confirmDelete(final Categories.Cat k) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + k.name + "?")
                .setMessage("Entries in this category move to \"Other\" (or your first category). Its budget limit is removed.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (!Categories.delete(this, k.name)) {
                        Ui.toast(this, "Keep at least one category");
                    }
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
