package com.spendlog.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A scrolling screen with a back link and a title. Subclasses fill {@code body} in render(). */
abstract class BaseActivity extends Activity {
    protected LinearLayout body;
    protected int accent;

    abstract String screenTitle();

    abstract void render();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        accent = Palette.accent(this);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setFitsSystemWindows(true);
        sv.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, Ui.dp(this, 32));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = Ui.text(this, "‹ Back", 16, accent, true);
        back.setPadding(0, Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        back.setOnClickListener(v -> finish());
        head.addView(back);
        root.addView(head);

        root.addView(Ui.text(this, screenTitle(), 24, getColor(R.color.text), true));

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        root.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accent = Palette.accent(this);
        Lock.guard(this);
        render();
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
}
