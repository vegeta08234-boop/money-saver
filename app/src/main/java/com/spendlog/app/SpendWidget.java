package com.spendlog.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpendWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, build(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(a)
                || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            updateAll(context);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, SpendWidget.class));
        for (int id : ids) {
            manager.updateAppWidget(id, build(context));
        }
    }

    private static RemoteViews build(Context c) {
        List<Expense> all = ExpenseStore.load(c);
        Map<String, Long> byDate = Stats.sumByDate(all);
        Calendar now = Calendar.getInstance();
        String ym = Dates.ym(now);

        long month = 0;
        for (Expense e : all) {
            if (e.date.startsWith(ym)) {
                month += e.cents;
            }
        }
        Long t = byDate.get(Dates.today());
        long today = t == null ? 0 : t;
        int accent = Palette.accent(c);

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
        rv.setTextViewText(R.id.w_label, Dates.monthLabel(now));
        rv.setTextViewText(R.id.w_month, Money.fmt(month));
        rv.setTextViewText(R.id.w_today, "Today " + Money.fmt(today));
        rv.setTextColor(R.id.w_add, accent);
        rv.setImageViewBitmap(R.id.w_bars, bars(c, byDate, accent));

        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 1, open, flags));

        Intent add = new Intent(c, MainActivity.class);
        add.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        add.putExtra("focus_add", true);
        rv.setOnClickPendingIntent(R.id.w_add, PendingIntent.getActivity(c, 2, add, flags));
        return rv;
    }

    private static Bitmap bars(Context c, Map<String, Long> byDate, int accent) {
        final int w = 600;
        final int h = 170;
        final int days = 7;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        long[] vals = new long[days];
        String[] labels = new String[days];
        SimpleDateFormat f = new SimpleDateFormat("EEE", Locale.getDefault());
        long max = 1;
        for (int i = 0; i < days; i++) {
            Calendar d = Calendar.getInstance();
            d.add(Calendar.DAY_OF_MONTH, i - (days - 1));
            Long v = byDate.get(Dates.key(d));
            vals[i] = v == null ? 0 : v;
            String name = f.format(d.getTime());
            labels[i] = name.isEmpty() ? "" : name.substring(0, 1);
            if (vals[i] > max) {
                max = vals[i];
            }
        }

        float labelH = 34f;
        float chartH = h - labelH - 4f;
        float baseY = 4f + chartH;
        float slot = (float) w / days;
        float bw = slot * 0.55f;

        p.setColor(c.getColor(R.color.line));
        canvas.drawRect(0, baseY, w, baseY + 2f, p);

        RectF r = new RectF();
        for (int i = 0; i < days; i++) {
            float left = i * slot + (slot - bw) / 2f;
            if (vals[i] > 0) {
                float bh = Math.max(6f, chartH * vals[i] / max);
                r.set(left, baseY - bh, left + bw, baseY);
                p.setColor(accent);
                p.setAlpha(i == days - 1 ? 255 : 150);
                canvas.drawRoundRect(r, 8f, 8f, p);
            }
        }

        p.setAlpha(255);
        p.setColor(c.getColor(R.color.subtext));
        p.setTextSize(26f);
        p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < days; i++) {
            canvas.drawText(labels[i], i * slot + slot / 2f, h - 6f, p);
        }
        return bmp;
    }
}
