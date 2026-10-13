package com.spendlog.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PaymentsActivity extends BaseActivity {

    @Override
    String screenTitle() {
        return "Auto-add payments";
    }

    @Override
    void render() {
        body.removeAllViews();
        int text = getColor(R.color.text);
        int sub = getColor(R.color.subtext);
        boolean access = PaymentListener.hasAccess(this);
        boolean on = AppSettings.autoOn(this);

        // Master switch
        LinearLayout main = Ui.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        Ui.grow(head, Ui.text(this, "Add payments automatically", 16, text, true));
        final Switch sw = new Switch(this);
        sw.setChecked(on);
        head.addView(sw);
        main.addView(head);
        main.addView(Ui.text(this, "When you pay with your phone, or a bank app sends a purchase alert, Spend Log reads the amount from that notification and adds it for you. Nothing leaves this phone, and only the apps you pick below are read.", 13, sub, false));

        if (on) {
            String status = access ? "Notification access is on." : "Notification access is off, so nothing can be added yet.";
            TextView st = Ui.text(this, status, 14, access ? text : getColor(R.color.danger), true);
            st.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
            main.addView(st);
            if (!access) {
                Button allow = Ui.button(this, "Allow notification access");
                allow.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
                Ui.add(main, allow, 4);
                main.addView(Ui.text(this, "If Spend Log is greyed out or says \"Restricted setting\" (Android 13 and newer, for apps installed from a file): open the app info below, tap the three dots at the top right, choose Allow restricted settings, then come back and try again.", 12, sub, false));
                Button info = new Button(this);
                info.setAllCaps(false);
                info.setText("Open app info");
                info.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()))));
                Ui.add(main, info, 4);
            }
        }
        sw.setOnCheckedChangeListener((b, checked) -> {
            AppSettings.setAuto(this, checked);
            if (checked) {
                if (AppSettings.watched(this).isEmpty()) {
                    AppSettings.setWatched(this, installedWallets());
                }
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
                }
                if (!PaymentListener.hasAccess(this)) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
            }
            render();
        });
        Ui.add(body, main, 12);

        if (!on) {
            return;
        }

        // Apps to watch
        LinearLayout apps = Ui.card(this);
        apps.addView(Ui.text(this, "Apps to read", 16, text, true));
        apps.addView(Ui.text(this, "Add your wallet and bank apps. Tap one to stop reading it.", 13, sub, false));
        final Set<String> watched = AppSettings.watched(this);
        if (watched.isEmpty()) {
            apps.addView(Ui.text(this, "None yet.", 14, text, false));
        }
        for (final String pkg : watched) {
            LinearLayout row = Ui.row(this);
            Ui.grow(row, Ui.text(this, PaymentListener.appLabel(this, pkg), 15, text, false));
            row.addView(Ui.text(this, "Remove", 13, getColor(R.color.danger), true));
            row.setOnClickListener(v -> {
                Set<String> next = new LinkedHashSet<>(AppSettings.watched(this));
                next.remove(pkg);
                AppSettings.setWatched(this, next);
                render();
            });
            apps.addView(row);
        }
        Button add = Ui.button(this, "+ Add an app");
        add.setOnClickListener(v -> pickApp());
        Ui.add(apps, add, 8);
        Ui.add(body, apps, 12);

        // Options
        LinearLayout opts = Ui.card(this);
        opts.addView(Ui.text(this, "Options", 16, text, true));

        LinearLayout catRow = Ui.row(this);
        Ui.grow(catRow, Ui.text(this, "Category when unsure", 15, text, false));
        catRow.addView(Ui.text(this, AppSettings.autoCat(this), 15, accent, true));
        catRow.setOnClickListener(v -> pickCategory());
        opts.addView(catRow);
        opts.addView(Ui.text(this, "Spend Log guesses from the shop name (a coffee shop becomes Food, for example) and uses this when it cannot tell.", 12, sub, false));

        LinearLayout confRow = Ui.row(this);
        Ui.grow(confRow, Ui.text(this, "Tell me when one is added", 15, text, false));
        final Switch conf = new Switch(this);
        conf.setChecked(AppSettings.confirmOn(this));
        conf.setOnCheckedChangeListener((b, c) -> AppSettings.setConfirm(this, c));
        confRow.addView(conf);
        opts.addView(confRow);
        opts.addView(Ui.text(this, "A quiet notification with an Undo button. The same amount seen twice within two minutes is only added once.", 12, sub, false));
        Ui.add(body, opts, 12);

        // Recent alerts
        LinearLayout log = Ui.card(this);
        log.addView(Ui.text(this, "Recent alerts", 16, text, true));
        log.addView(Ui.text(this, "What Spend Log saw from your chosen apps and what it did. Kept on this phone only.", 12, sub, false));
        List<AutoLog.Entry> recent = AutoLog.recent(this);
        if (recent.isEmpty()) {
            log.addView(Ui.text(this, "Nothing yet. The next payment alert will show up here.", 14, text, false));
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (AutoLog.Entry e : recent) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, Ui.dp(this, 10), 0, 0);
            boolean added = e.status.startsWith("Added");
            item.addView(Ui.text(this, e.status, 14, added ? text : sub, added));
            item.addView(Ui.text(this, e.app + " · " + df.format(new Date(e.time)), 12, sub, false));
            if (!e.snippet.isEmpty() && !added) {
                item.addView(Ui.text(this, e.snippet, 12, sub, false));
            }
            log.addView(item);
        }
        if (!recent.isEmpty()) {
            Button clear = new Button(this);
            clear.setAllCaps(false);
            clear.setText("Clear this list");
            clear.setOnClickListener(v -> {
                AutoLog.clearLog(this);
                render();
            });
            Ui.add(log, clear, 8);
        }
        Ui.add(body, log, 12);
    }

    private Set<String> installedWallets() {
        Set<String> out = new LinkedHashSet<>();
        PackageManager pm = getPackageManager();
        for (String w : PaymentListener.WALLETS) {
            try {
                pm.getApplicationInfo(w, 0);
                out.add(w);
            } catch (PackageManager.NameNotFoundException ignored) {
                // not installed on this phone
            }
        }
        return out;
    }

    private void pickApp() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        Set<String> have = AppSettings.watched(this);
        Map<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> labelToPkg = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || have.contains(pkg)) {
                continue;
            }
            String label = ri.loadLabel(pm).toString();
            if (byLabel.containsKey(label)) {
                label = label + " (" + pkg + ")";
            }
            byLabel.put(label, pkg);
        }
        final List<String> labels = new ArrayList<>(byLabel.keySet());
        Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
        for (String l : labels) {
            labelToPkg.put(l, byLabel.get(l));
        }
        if (labels.isEmpty()) {
            Ui.toast(this, "No other apps found");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Read which app?")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    Set<String> next = new LinkedHashSet<>(AppSettings.watched(this));
                    next.add(labelToPkg.get(labels.get(which)));
                    AppSettings.setWatched(this, next);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickCategory() {
        final List<String> names = Categories.names(this);
        int cur = Math.max(0, names.indexOf(AppSettings.autoCat(this)));
        new AlertDialog.Builder(this)
                .setTitle("Category when unsure")
                .setSingleChoiceItems(names.toArray(new String[0]), cur, (d, which) -> {
                    AppSettings.setAutoCat(this, names.get(which));
                    d.dismiss();
                    render();
                })
                .show();
    }
}
