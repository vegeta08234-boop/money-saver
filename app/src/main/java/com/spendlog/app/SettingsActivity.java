package com.spendlog.app;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;

public class SettingsActivity extends BaseActivity {

    @Override
    String screenTitle() {
        return "Reminder, lock & colors";
    }

    @Override
    void render() {
        body.removeAllViews();
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);

        // Daily reminder
        LinearLayout rem = Ui.card(this);
        LinearLayout remRow = new LinearLayout(this);
        remRow.setOrientation(LinearLayout.HORIZONTAL);
        Ui.grow(remRow, Ui.text(this, "Daily reminder", 16, text, true));
        final Switch remSwitch = new Switch(this);
        remSwitch.setChecked(AppSettings.reminderOn(this));
        remRow.addView(remSwitch);
        rem.addView(remRow);
        rem.addView(Ui.text(this, "A nudge to log what you spent. It stays quiet on days you already added something.", 13, sub, false));

        if (AppSettings.reminderOn(this)) {
            Calendar t = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, AppSettings.reminderHour(this));
            t.set(Calendar.MINUTE, AppSettings.reminderMinute(this));
            TextView time = Ui.text(this, "Time: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(t.getTime()), 16, accent, true);
            time.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
            time.setOnClickListener(v -> new TimePickerDialog(this, (picker, h, m) -> {
                AppSettings.setReminder(this, true, h, m);
                Reminder.schedule(this);
                render();
            }, AppSettings.reminderHour(this), AppSettings.reminderMinute(this),
                    android.text.format.DateFormat.is24HourFormat(this)).show());
            rem.addView(time);
        }
        remSwitch.setOnCheckedChangeListener((b, on) -> {
            AppSettings.setReminder(this, on, AppSettings.reminderHour(this), AppSettings.reminderMinute(this));
            Reminder.schedule(this);
            if (on && Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
            }
            render();
        });
        Ui.add(body, rem, 12);

        // Color shift
        LinearLayout tint = Ui.card(this);
        LinearLayout tintRow = new LinearLayout(this);
        tintRow.setOrientation(LinearLayout.HORIZONTAL);
        Ui.grow(tintRow, Ui.text(this, "Colors shift with your budget", 16, text, true));
        final Switch tintSwitch = new Switch(this);
        tintSwitch.setChecked(AppSettings.tintOn(this));
        tintRow.addView(tintSwitch);
        tint.addView(tintRow);
        tint.addView(Ui.text(this, "Once half of your monthly budget is spent, the app's theme color and the widget slowly turn amber, then red as you reach it. The budget bar always changes color. This needs a monthly budget (More, then Budgets).", 13, sub, false));
        tintSwitch.setOnCheckedChangeListener((b, on) -> {
            AppSettings.setTint(this, on);
            SpendWidget.updateAll(this);
        });
        Ui.add(body, tint, 12);

        // App lock
        LinearLayout lock = Ui.card(this);
        LinearLayout lockRow = new LinearLayout(this);
        lockRow.setOrientation(LinearLayout.HORIZONTAL);
        Ui.grow(lockRow, Ui.text(this, "App lock", 16, text, true));
        final Switch lockSwitch = new Switch(this);
        lockSwitch.setChecked(AppSettings.lockOn(this));
        lockRow.addView(lockSwitch);
        lock.addView(lockRow);
        lock.addView(Ui.text(this, "Ask for your phone's screen lock (PIN, pattern or password) when you open Spend Log.", 13, sub, false));
        if (!Lock.deviceSecure(this)) {
            lock.addView(Ui.text(this, "Set a screen lock in your phone's settings first.", 13, getColor(R.color.danger), false));
        }
        lockSwitch.setOnCheckedChangeListener((b, on) -> {
            if (on && !Lock.deviceSecure(this)) {
                Ui.toast(this, "Set a screen lock in your phone's settings first");
                lockSwitch.setChecked(false);
                return;
            }
            AppSettings.setLock(this, on);
        });
        Ui.add(body, lock, 12);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7 && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Ui.toast(this, "Notifications are blocked for Spend Log, so the reminder cannot show");
        }
    }
}
